package com.posbillingwala.owner.Extra;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.posbillingwala.owner.Activity.MainActivity;

import java.util.Map;

public class OwnerFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "OwnerFcmService";

    @Override
    public void onNewToken(@NonNull String token) {
        String userId = MainActivity.userId;
        if (userId == null || userId.isEmpty()) {
            userId = Common.getSavedUserData(getApplicationContext(), "userId");
        }
        if (userId != null && !userId.isEmpty()) {
            FcmTokenManager.registerToken(getApplicationContext(), userId, token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        String title = message.getNotification() != null ? message.getNotification().getTitle() : null;
        String body = message.getNotification() != null ? message.getNotification().getBody() : null;
        if ((title == null || title.isEmpty()) && data != null) title = data.get("title");
        if ((body == null || body.isEmpty()) && data != null) {
            body = data.containsKey("body") ? data.get("body") : data.get("message");
        }
        Log.d(TAG, "FCM received");
        PushNotificationHelper.showFromPayload(getApplicationContext(), title, body, data);
    }
}
