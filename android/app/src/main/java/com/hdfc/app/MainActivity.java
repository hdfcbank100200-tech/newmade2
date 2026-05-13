package com.hdfc.app;

import android.Manifest;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import androidx.core.app.ActivityCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onStart() {
        super.onStart();
        // CREATE A BRIDGE TO ASK FOR REAL PERMISSIONS
        WebView webView = getBridge().getWebView();
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void requestRealPermissions() {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.CALL_PHONE
                }, 101);
            }
        }, "AndroidBridge");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            boolean allGranted = true;
            for (int res : grantResults) {
                if (res != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            // TELL THE WEBSITE THE RESULT
            final String status = allGranted ? "GRANTED" : "DENIED";
            getBridge().getWebView().post(new Runnable() {
                @Override
                public void run() {
                    getBridge().getWebView().loadUrl("javascript:handlePermissionResult('" + status + "')");
                }
            });
        }
    }
}
