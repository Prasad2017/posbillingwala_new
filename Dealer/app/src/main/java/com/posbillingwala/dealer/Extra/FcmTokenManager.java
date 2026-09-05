package com.posbillingwala.dealer.Extra;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;
import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.Retrofit.Api;

import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** FCM for Dealer app — Firebase project pos-billingwala. */
public final class FcmTokenManager {
    private static final String TAG = "DealerFcm";

    private FcmTokenManager() {}

    public static void registerIfLoggedIn(@NonNull Context context) {
        Context app = context.getApplicationContext();
        String userId = MainActivity.userId;
        if (userId == null || userId.trim().isEmpty()) {
            userId = Common.getSavedUserData(app, "userId");
        }
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        PushNotificationHelper.ensureChannel(app);
        final String uid = userId;
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                registerToken(app, uid, task.getResult());
            }
        });
    }

    public static void registerToken(@NonNull Context context, @NonNull String userId, @NonNull String token) {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.isEmpty() || token.isEmpty()) return;
        Api.getClient().registerFcmToken(userId, androidId, token).enqueue(new Callback<AllApiResponse>() {
            @Override public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                Log.d(TAG, "token registered");
            }
            @Override public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.w(TAG, "token register failed", t);
            }
        });
    }

    public static void clearOnLogout(@NonNull Context context) {
        Context app = context.getApplicationContext();
        String userId = MainActivity.userId;
        if (userId == null || userId.isEmpty()) {
            userId = Common.getSavedUserData(app, "userId");
        }
        String androidId = Settings.Secure.getString(app.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (userId != null && !userId.isEmpty() && androidId != null) {
            Api.getClient().registerFcmToken(userId, androidId, "").enqueue(new Callback<AllApiResponse>() {
                @Override public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {}
                @Override public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {}
            });
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            try { Tasks.await(FirebaseMessaging.getInstance().deleteToken()); } catch (Exception ignored) {}
        });
    }
}
