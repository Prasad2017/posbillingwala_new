package com.pos_billingwala.Extra;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Handles FCM push: licence expiry reminders and admin promotional messages.
 */
public class PosFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "PosFcmService";

    @Override
    public void onNewToken(@NonNull String token) {
        FcmTokenManager.registerToken(getApplicationContext(), token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        String title = message.getNotification() != null ? message.getNotification().getTitle() : null;
        String body = message.getNotification() != null ? message.getNotification().getBody() : null;

        if (title == null && data.containsKey("title")) {
            title = data.get("title");
        }
        if (body == null && data.containsKey("body")) {
            body = data.get("body");
        }

        Log.d(TAG, "Push received type=" + (data != null ? data.get("type") : "unknown"));
        PushNotificationHelper.showFromPayload(getApplicationContext(), title, body, data);
    }
}
