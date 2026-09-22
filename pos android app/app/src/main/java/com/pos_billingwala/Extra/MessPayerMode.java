package com.pos_billingwala.Extra;

import android.content.Context;

/**
 * Mess-level payer mode: user (default) or institute.
 * Cached locally; synced via mess_shop_setting_* APIs.
 */
public final class MessPayerMode {

    public static final String MODE_USER = "user";
    public static final String MODE_INSTITUTE = "institute";
    private static final String PREF_KEY = "mess_payer_mode";

    private MessPayerMode() {
    }

    public static String normalize(String mode) {
        if (mode != null && MODE_INSTITUTE.equalsIgnoreCase(mode.trim())) {
            return MODE_INSTITUTE;
        }
        return MODE_USER;
    }

    public static boolean isInstitutePay(Context context) {
        return MODE_INSTITUTE.equals(get(context));
    }

    public static String get(Context context) {
        if (context == null) {
            return MODE_USER;
        }
        return normalize(Common.getSavedUserData(context, PREF_KEY));
    }

    public static void setLocal(Context context, String mode) {
        if (context == null) {
            return;
        }
        Common.saveUserData(context, PREF_KEY, normalize(mode));
    }
}
