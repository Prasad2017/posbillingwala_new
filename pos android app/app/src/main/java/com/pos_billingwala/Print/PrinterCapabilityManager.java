package com.pos_billingwala.Print;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Cutter capability cache.
 *
 * ESC/POS does NOT provide a universal, reliable cutter-query on every model.
 * Therefore capability is:
 * <ul>
 *   <li>SUPPORTED — known model / explicit profile</li>
 *   <li>NOT_SUPPORTED — user disabled auto-cut or profile says no</li>
 *   <li>UNKNOWN — default; use profile/settings fallback (do NOT treat as OFF)</li>
 * </ul>
 */
public final class PrinterCapabilityManager {

    public enum CutterCapability {
        SUPPORTED,
        NOT_SUPPORTED,
        UNKNOWN
    }

    private static final String PREFS = "printer_capability_prefs";
    private static final String KEY_AUTO_CUT = "auto_cut_enabled";
    private static final String KEY_CUT_TYPE = "cut_type";

    private PrinterCapabilityManager() {
    }

    public static boolean isAutoCutEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_CUT, true);
    }

    public static void setAutoCutEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_CUT, enabled).apply();
    }

    public static EscPosCutHelper.CutType cutType(Context context) {
        String raw = prefs(context).getString(KEY_CUT_TYPE, "default");
        if ("partial".equalsIgnoreCase(raw)) {
            return EscPosCutHelper.CutType.PARTIAL;
        }
        if ("full".equalsIgnoreCase(raw)) {
            return EscPosCutHelper.CutType.FULL;
        }
        return EscPosCutHelper.CutType.DEFAULT;
    }

    public static void setCutType(Context context, EscPosCutHelper.CutType type) {
        String value = "default";
        if (type == EscPosCutHelper.CutType.FULL) {
            value = "full";
        } else if (type == EscPosCutHelper.CutType.PARTIAL) {
            value = "partial";
        }
        prefs(context).edit().putString(KEY_CUT_TYPE, value).apply();
    }

    /**
     * Probe is intentionally conservative — without a reliable status response
     * the result stays UNKNOWN (or NOT_SUPPORTED when auto-cut is off).
     */
    public static CutterCapability probe(Context context) {
        if (!isAutoCutEnabled(context)) {
            return CutterCapability.NOT_SUPPORTED;
        }
        return CutterCapability.UNKNOWN;
    }

    /**
     * Safe cut gate:
     * AutoCut OFF → never cut
     * SUPPORTED → cut
     * NOT_SUPPORTED → never cut
     * UNKNOWN → honour auto-cut setting (profile/user fallback)
     */
    public static boolean shouldCut(Context context) {
        CutterCapability capability = probe(context);
        boolean autoCut = isAutoCutEnabled(context);
        if (!autoCut) {
            return false;
        }
        switch (capability) {
            case SUPPORTED:
                return true;
            case NOT_SUPPORTED:
                return false;
            case UNKNOWN:
            default:
                return true;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
