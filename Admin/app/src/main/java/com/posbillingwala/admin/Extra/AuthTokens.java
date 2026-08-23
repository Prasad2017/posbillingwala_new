package com.posbillingwala.admin.Extra;

import android.content.Context;

import com.posbillingwala.admin.Model.AllApiResponse;

/**
 * P5-3: Persist server-issued auth tokens for API requests.
 */
public final class AuthTokens {

    private AuthTokens() {
    }

    public static void save(Context context, String authToken, String tokenExpiresAt) {
        if (context == null) {
            return;
        }
        if (authToken != null && !authToken.isEmpty()) {
            Common.saveUserData(context, "authToken", authToken);
        }
        if (tokenExpiresAt != null && !tokenExpiresAt.isEmpty()) {
            Common.saveUserData(context, "tokenExpiresAt", tokenExpiresAt);
        }
    }

    public static void saveFromLogin(Context context, AllApiResponse response) {
        if (response == null) {
            return;
        }
        save(context, response.getAuthToken(), response.getTokenExpiresAt());
    }

    public static void clear(Context context) {
        if (context == null) {
            return;
        }
        Common.saveUserData(context, "authToken", "");
        Common.saveUserData(context, "tokenExpiresAt", "");
    }
}
