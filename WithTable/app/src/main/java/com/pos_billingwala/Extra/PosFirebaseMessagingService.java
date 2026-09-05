package com.pos_billingwala.Extra;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Receives FCM from Firebase project {@code pos-billingwala}
 * (WithTable/app/google-services.json).
 *
 * Message types:
 * <ul>
 *   <li>{@code promotional} / Admin Send Push — show notification tray</li>
 *   <li>{@code license_expiring} — show notification tray</li>
 *   <li>{@code mess.token.created} — enqueue local print job only (no tray)</li>
 * </ul>
 */
public class PosFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "PosFcmService";

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "onNewToken — re-register with server");
        FcmTokenManager.registerToken(getApplicationContext(), token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        String type = null;
        if (data != null) {
            type = data.get("type");
            if (type == null || type.trim().isEmpty()) {
                type = data.get("event");
            }
        }

        Log.d(TAG, "FCM received type=" + (type != null ? type : "unknown")
                + " from=" + message.getFrom()
                + " hasNotification=" + (message.getNotification() != null));

        // Mess Common QR — silent queue only (never print in this callback)
        if ("mess.token.created".equals(type)) {
            MessMealTokenPrintWorker.enqueueFromFcm(
                    getApplicationContext(),
                    data != null ? data.get("tokenId") : null,
                    data != null ? data.get("tokenNumber") : null,
                    data != null ? data.get("registrationNo") : null,
                    data != null ? data.get("mealSession") : null,
                    data != null ? data.get("date") : null,
                    data != null ? data.get("createdAt") : null,
                    data != null ? data.get("printStatus") : null
            );
            return;
        }

        // Admin promotional + licence expiry (+ any other typed alert)
        String title = message.getNotification() != null ? message.getNotification().getTitle() : null;
        String body = message.getNotification() != null ? message.getNotification().getBody() : null;

        if ((title == null || title.trim().isEmpty()) && data != null && data.containsKey("title")) {
            title = data.get("title");
        }
        if ((body == null || body.trim().isEmpty()) && data != null && data.containsKey("body")) {
            body = data.get("body");
        }
        // Admin API sends message text in notification body; also accept "message" data key
        if ((body == null || body.trim().isEmpty()) && data != null && data.containsKey("message")) {
            body = data.get("message");
        }

        PushNotificationHelper.showFromPayload(getApplicationContext(), title, body, data);
    }
}
