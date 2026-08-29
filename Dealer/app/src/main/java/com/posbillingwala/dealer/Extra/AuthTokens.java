package com.posbillingwala.dealer.Extra;

import android.content.Context;

import com.posbillingwala.dealer.Model.AllApiResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Persist dealer auth tokens. Login once — reuse until expiry.
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

    public static String getToken(Context context) {
        if (context == null) {
            return "";
        }
        String token = Common.getSavedUserData(context, "authToken");
        return token != null ? token : "";
    }

    public static boolean hasValidSession(Context context) {
        if (context == null) {
            return false;
        }
        String token = getToken(context);
        if (token.trim().isEmpty()) {
            return false;
        }
        String userId = Common.getSavedUserData(context, "userId");
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        return !isTokenExpired(context);
    }

    public static boolean isTokenExpired(Context context) {
        if (context == null) {
            return true;
        }
        String expiresAt = Common.getSavedUserData(context, "tokenExpiresAt");
        if (expiresAt == null || expiresAt.trim().isEmpty()) {
            return false;
        }
        Date expiry = parseExpiry(expiresAt.trim());
        if (expiry == null) {
            return false;
        }
        return expiry.getTime() <= System.currentTimeMillis();
    }

    private static Date parseExpiry(String expiresAt) {
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                sdf.setLenient(false);
                return sdf.parse(expiresAt);
            } catch (ParseException ignored) {
            }
        }
        try {
            long epoch = Long.parseLong(expiresAt);
            if (epoch < 1_000_000_000_000L) {
                epoch *= 1000L;
            }
            return new Date(epoch);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static void clear(Context context) {
        if (context == null) {
            return;
        }
        Common.saveUserData(context, "authToken", "");
        Common.saveUserData(context, "tokenExpiresAt", "");
        Common.saveUserData(context, "userId", "");
        Common.saveUserData(context, "userName", "");
        Common.saveUserData(context, "contactNumber", "");
        android.content.SharedPreferences pref = context.getSharedPreferences("user", android.content.Context.MODE_PRIVATE);
        pref.edit().clear().apply();
    }
}
