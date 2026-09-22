package com.pos_billingwala.Extra;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Registers / clears the device FCM token with the Billingwala server.
 * Uses the Firebase project from WithTable/app/google-services.json (pos-billingwala).
 */
public final class FcmTokenManager {

    private static final String TAG = "FcmTokenManager";

    private FcmTokenManager() {
    }

    public static void registerIfLoggedIn(@NonNull Context context) {
        Context app = context.getApplicationContext();
        String userId = Common.getSavedUserData(app, "userId");
        if (userId == null || userId.trim().isEmpty()) {
            Log.d(TAG, "Skip FCM register — not logged in");
            return;
        }
        PushNotificationHelper.ensureChannel(app);
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Log.w(TAG, "FCM getToken failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d(TAG, "FCM token ready, uploading to server");
                    registerToken(app, token);
                });
    }

    public static void registerToken(@NonNull Context context, @NonNull String token) {
        Context app = context.getApplicationContext();
        String userId = Common.getSavedUserData(app, "userId");
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        if (token.trim().isEmpty()) {
            return;
        }

        String androidId = Settings.Secure.getString(app.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.trim().isEmpty()) {
            return;
        }

        Call<AllApiResponse> call = Api.getClient(app).registerFcmToken(userId, androidId, token);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && "1".equals(response.body().status)) {
                    Log.d(TAG, "FCM token registered on server");
                } else {
                    Log.w(TAG, "FCM token register rejected by server");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.w(TAG, "FCM token upload failed", t);
            }
        });
    }

    /**
     * Clears server-side token for this device, then deletes the local Firebase token
     * so the next login gets a fresh registration.
     */
    public static void clearOnLogout(@NonNull Context context) {
        Context app = context.getApplicationContext();
        String userId = Common.getSavedUserData(app, "userId");
        String androidId = Settings.Secure.getString(app.getContentResolver(), Settings.Secure.ANDROID_ID);

        if (userId != null && !userId.trim().isEmpty()
                && androidId != null && !androidId.trim().isEmpty()) {
            Api.getClient(app).registerFcmToken(userId, androidId, "").enqueue(new Callback<AllApiResponse>() {
                @Override
                public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                    Log.d(TAG, "FCM token cleared on server");
                }

                @Override
                public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                    Log.w(TAG, "FCM clear on server failed", t);
                }
            });
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Tasks.await(FirebaseMessaging.getInstance().deleteToken());
                Log.d(TAG, "Local FCM token deleted");
            } catch (Exception e) {
                Log.w(TAG, "Local FCM token delete failed", e);
            }
        });
    }

    public static void requestPermissionIfNeeded(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        // POST_NOTIFICATIONS is requested from Home / sync flow.
    }
}
