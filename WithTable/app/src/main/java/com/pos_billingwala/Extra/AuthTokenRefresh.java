package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.BuildConfig;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Silent POS token refresh for offline devices that are already licence-bound.
 * Does not ask for MPIN again — only licence key + device id.
 */
public final class AuthTokenRefresh {

    private static final Object LOCK = new Object();
    private static long lastAttemptMs = 0L;

    private AuthTokenRefresh() {
    }

    /**
     * @return true if a fresh token was saved
     */
    public static boolean tryRefresh(Context context) {
        if (context == null) {
            return false;
        }
        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            // Avoid hammering the server
            if (now - lastAttemptMs < 5000L) {
                return AuthTokens.hasStoredToken(context);
            }
            lastAttemptMs = now;

            String licenceKey = Common.getSavedUserData(context, "LicenceKey");
            if (licenceKey == null || licenceKey.trim().isEmpty()) {
                licenceKey = Common.getSavedUserData(context, "licenceKey");
            }
            String deviceId = null;
            try {
                deviceId = android.provider.Settings.Secure.getString(
                        context.getContentResolver(),
                        android.provider.Settings.Secure.ANDROID_ID);
            } catch (Exception ignored) {
            }
            if (deviceId == null || deviceId.trim().isEmpty()) {
                deviceId = Common.getSavedUserData(context, "androidId");
            }
            if (deviceId == null || deviceId.trim().isEmpty()) {
                deviceId = Common.getSavedUserData(context, "android_device_id");
            }
            if (licenceKey == null || licenceKey.trim().isEmpty()
                    || deviceId == null || deviceId.trim().isEmpty()) {
                return false;
            }

            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build();

                FormBody body = new FormBody.Builder()
                        .add("app_licence_key", licenceKey.trim())
                        .add("android_device_id", deviceId.trim())
                        .build();

                String oldToken = AuthTokens.getToken(context);
                Request.Builder req = new Request.Builder()
                        .url(BuildConfig.API_BASE_URL + "refreshAuthToken.php")
                        .post(body);
                if (oldToken != null && !oldToken.isEmpty()) {
                    req.header("Authorization", "Bearer " + oldToken);
                }

                try (Response response = client.newCall(req.build()).execute()) {
                    ResponseBody respBody = response.body();
                    if (respBody == null) {
                        return false;
                    }
                    String json = respBody.string();
                    JSONObject obj = new JSONObject(json);
                    if (!"1".equals(obj.optString("status")) && !"true".equalsIgnoreCase(obj.optString("status"))) {
                        return false;
                    }
                    String token = obj.optString("authToken", "");
                    String expires = obj.optString("tokenExpiresAt", "");
                    if (token.isEmpty()) {
                        return false;
                    }
                    AuthTokens.save(context, token, expires);
                    return true;
                }
            } catch (Exception ignored) {
                return false;
            }
        }
    }
}
