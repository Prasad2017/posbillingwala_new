package com.pos_billingwala.PaymentDisplay;

import android.content.Context;

import com.pos_billingwala.Extra.Common;

/** SharedPreferences for Payment Display (auto-show, duration, last URL/bill). */
public final class PaymentDisplayPrefs {

    public static final int DEFAULT_QR_DURATION_SEC = 5 * 60;
    public static final int MIN_QR_DURATION_SEC = 60;
    public static final int MAX_QR_DURATION_SEC = 30 * 60;

    private static final String KEY_AUTO = "payment_display_auto_show";
    private static final String KEY_DURATION = "payment_display_qr_duration_sec";
    private static final String KEY_URL = "payment_display_last_url";
    private static final String KEY_BILL = "payment_display_last_bill_json";

    private PaymentDisplayPrefs() {
    }

    public static boolean isAutoDisplayEnabled(Context context) {
        String v = Common.getSavedUserData(context, KEY_AUTO);
        return v == null || v.isEmpty() || "1".equals(v) || "true".equalsIgnoreCase(v);
    }

    public static void setAutoDisplayEnabled(Context context, boolean enabled) {
        Common.saveUserData(context, KEY_AUTO, enabled ? "1" : "0");
    }

    public static int getQrDurationSeconds(Context context) {
        String v = Common.getSavedUserData(context, KEY_DURATION);
        try {
            int sec = Integer.parseInt(v);
            return Math.max(MIN_QR_DURATION_SEC, Math.min(MAX_QR_DURATION_SEC, sec));
        } catch (Exception e) {
            return DEFAULT_QR_DURATION_SEC;
        }
    }

    public static void setQrDurationSeconds(Context context, int seconds) {
        int sec = Math.max(MIN_QR_DURATION_SEC, Math.min(MAX_QR_DURATION_SEC, seconds));
        Common.saveUserData(context, KEY_DURATION, String.valueOf(sec));
    }

    public static String getLastLocalUrl(Context context) {
        return Common.getSavedUserData(context, KEY_URL);
    }

    public static void setLastLocalUrl(Context context, String url) {
        Common.saveUserData(context, KEY_URL, url != null ? url : "");
    }

    public static String getLastBillJson(Context context) {
        return Common.getSavedUserData(context, KEY_BILL);
    }

    public static void setLastBillJson(Context context, String json) {
        Common.saveUserData(context, KEY_BILL, json != null ? json : "");
    }

    public static void clearLastBill(Context context) {
        Common.saveUserData(context, KEY_BILL, "");
    }
}
