package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.Model.LoginResponse;

/**
 * Persist server-issued auth tokens for API requests.
 * Tokens are long-lived so offline POS can sync without re-login.
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

    public static void saveFromLogin(Context context, LoginResponse response) {
        if (context == null || response == null) {
            return;
        }
        save(context, response.getAuthToken(), response.getTokenExpiresAt());
    }

    public static String getToken(Context context) {
        if (context == null) {
            return "";
        }
        String token = Common.getSavedUserData(context, "authToken");
        return token != null ? token : "";
    }

    /** True when a token is stored (offline OK even if near expiry — refresh when online). */
    public static boolean hasStoredToken(Context context) {
        String token = getToken(context);
        return token != null && !token.trim().isEmpty();
    }

    public static void clear(Context context) {
        if (context == null) {
            return;
        }
        Common.saveUserData(context, "authToken", "");
        Common.saveUserData(context, "tokenExpiresAt", "");
    }
}
