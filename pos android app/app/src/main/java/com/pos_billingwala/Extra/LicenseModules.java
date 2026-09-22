package com.pos_billingwala.Extra;

import android.content.Context;
import android.view.View;

import com.pos_billingwala.Activity.MainActivity;

/**
 * License feature flags for POS modules (Fast Billing / Dine-In / Takeaway / Mess).
 * Offline Home must keep showing modules from last successful login / signed payload.
 */
public final class LicenseModules {

    private LicenseModules() {
    }

    public static boolean isEnabled(String flag) {
        if (flag == null) {
            return false;
        }
        String value = flag.trim();
        if (value.isEmpty()) {
            return false;
        }
        if (value.equals("1")
                || value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("on")
                || value.equalsIgnoreCase("yes")) {
            return true;
        }
        try {
            return Integer.parseInt(value) != 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static void setVisible(View view, boolean enabled) {
        if (view == null) {
            return;
        }
        view.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    /**
     * Persist module flags from a login / licence-check response.
     * Null or blank values are skipped so an offline refresh cannot wipe a previously saved module.
     */
    public static void saveModuleFlags(Context context, String fastBilling, String takeAway,
                                       String dineIn, String mess, String totalSaleData,
                                       String todaySaleData) {
        if (context == null) {
            return;
        }
        saveIfPresent(context, "fastBilling", fastBilling);
        saveIfPresent(context, "takeAway", takeAway);
        saveIfPresent(context, "dineIn", dineIn);
        saveIfPresent(context, "mess", mess);
        saveIfPresent(context, "totalSaleData", totalSaleData);
        saveIfPresent(context, "todaySaleData", todaySaleData);
        applySavedFlagsToSession(context);
    }

    /**
     * Fill empty prefs from the last signed license payload (works fully offline).
     * Never turns a saved-enabled module off — only restores missing flags.
     */
    public static boolean hydrateMissingFlagsFromPayload(Context context) {
        if (context == null) {
            return false;
        }
        LicenseValidator.SignedPayload payload = LicenseValidator.peekVerifiedPayload(context);
        if (payload == null) {
            return false;
        }
        boolean changed = false;
        changed |= restoreIfMissing(context, "fastBilling", payload.fastBilling == 1);
        changed |= restoreIfMissing(context, "takeAway", payload.takeAway == 1);
        changed |= restoreIfMissing(context, "dineIn", payload.dineIn == 1);
        changed |= restoreIfMissing(context, "mess", payload.mess == 1);
        if (changed) {
            applySavedFlagsToSession(context);
        }
        return changed;
    }

    public static void applySavedFlagsToSession(Context context) {
        if (context == null) {
            return;
        }
        MainActivity.fastBilling = Common.getSavedUserData(context, "fastBilling");
        MainActivity.takeAway = Common.getSavedUserData(context, "takeAway");
        MainActivity.dineIn = Common.getSavedUserData(context, "dineIn");
        MainActivity.mess = Common.getSavedUserData(context, "mess");
        MainActivity.totalSaleData = Common.getSavedUserData(context, "totalSaleData");
        MainActivity.todaySaleData = Common.getSavedUserData(context, "todaySaleData");
    }

    private static void saveIfPresent(Context context, String key, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        Common.saveUserData(context, key, isEnabled(raw) ? "1" : "0");
    }

    private static boolean restoreIfMissing(Context context, String key, boolean payloadEnabled) {
        if (!payloadEnabled) {
            return false;
        }
        String current = Common.getSavedUserData(context, key);
        if (isEnabled(current)) {
            return false;
        }
        Common.saveUserData(context, key, "1");
        return true;
    }
}
