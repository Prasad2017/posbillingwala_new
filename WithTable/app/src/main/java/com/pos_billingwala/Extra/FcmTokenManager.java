package com.pos_billingwala.Extra;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessaging;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Retrofit.Api;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Registers the device FCM token with the server after login.
 */
public final class FcmTokenManager {

    private static final String TAG = "FcmTokenManager";

    private FcmTokenManager() {
    }

    public static void registerIfLoggedIn(@NonNull Context context) {
        Context app = context.getApplicationContext();
        String userId = Common.getSavedUserData(app, "userId");
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        registerToken(app, task.getResult());
                    }
                });
    }

    public static void registerToken(@NonNull Context context, @NonNull String token) {
        Context app = context.getApplicationContext();
        String userId = Common.getSavedUserData(app, "userId");
        if (userId == null || userId.trim().isEmpty()) {
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
                // Best-effort — no user-facing error
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.w(TAG, "FCM token upload failed", t);
            }
        });
    }

    public static void clearOnLogout(@NonNull Context context) {
        Context app = context.getApplicationContext();
        String userId = Common.getSavedUserData(app, "userId");
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        String androidId = Settings.Secure.getString(app.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.trim().isEmpty()) {
            return;
        }
        Api.getClient(app).registerFcmToken(userId, androidId, "").enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
            }
        });
    }

    public static void requestPermissionIfNeeded(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        // Permission is requested from Home fragment for sync; push uses same POST_NOTIFICATIONS grant.
    }
}
