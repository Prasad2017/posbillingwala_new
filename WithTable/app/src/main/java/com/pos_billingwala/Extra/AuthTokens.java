package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.Model.LoginResponse;

/**
 * P5-3: Persist server-issued auth tokens for API requests.
 */
public final class AuthTokens {

    private AuthTokens() {
    }

    public static void saveFromLogin(Context context, LoginResponse response) {
        if (context == null || response == null) {
            return;
        }
        if (response.getAuthToken() != null && !response.getAuthToken().isEmpty()) {
            Common.saveUserData(context, "authToken", response.getAuthToken());
        }
        if (response.getTokenExpiresAt() != null && !response.getTokenExpiresAt().isEmpty()) {
            Common.saveUserData(context, "tokenExpiresAt", response.getTokenExpiresAt());
        }
    }

    public static void clear(Context context) {
        if (context == null) {
            return;
        }
        Common.saveUserData(context, "authToken", "");
        Common.saveUserData(context, "tokenExpiresAt", "");
    }
}
