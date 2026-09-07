package com.pos_billingwala.Extra.dynamic;

import android.content.Context;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.AppContexts;
import com.pos_billingwala.Extra.Common;

/**
 * Blocks configuration apply while cart, payment, or printing is active (Phase 12).
 */
public final class ConfigApplyGuard {

    private static final String PREF_CART_ACTIVE = "dynamicConfigCartActive";
    private static final String PREF_PAYMENT_ACTIVE = "dynamicConfigPaymentActive";
    private static final String PREF_PRINT_ACTIVE = "dynamicConfigPrintActive";

    private ConfigApplyGuard() {
    }

    public static void setCartActive(Context context, boolean active) {
        save(context, PREF_CART_ACTIVE, active);
    }

    public static void setPaymentActive(Context context, boolean active) {
        save(context, PREF_PAYMENT_ACTIVE, active);
    }

    public static void setPrintActive(Context context, boolean active) {
        save(context, PREF_PRINT_ACTIVE, active);
    }

    public static boolean isBusy(Context context) {
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return false;
        }
        if ("1".equals(Common.getSavedUserData(ctx, PREF_CART_ACTIVE))
                || "1".equals(Common.getSavedUserData(ctx, PREF_PAYMENT_ACTIVE))
                || "1".equals(Common.getSavedUserData(ctx, PREF_PRINT_ACTIVE))) {
            return true;
        }
        // Live session signals used by MainActivity / CreatePos
        String invoiceRunning = MainActivity.invoiceRunningStatus;
        return invoiceRunning != null && !invoiceRunning.trim().isEmpty();
    }

    public static boolean canApplyConfiguration(Context context) {
        return !isBusy(context);
    }

    public static String blockReason(Context context) {
        if (!isBusy(context)) {
            return "";
        }
        Context ctx = AppContexts.or(context);
        if (ctx != null && "1".equals(Common.getSavedUserData(ctx, PREF_PRINT_ACTIVE))) {
            return "printing";
        }
        if (ctx != null && "1".equals(Common.getSavedUserData(ctx, PREF_PAYMENT_ACTIVE))) {
            return "payment";
        }
        return "cart";
    }

    private static void save(Context context, String key, boolean active) {
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return;
        }
        Common.saveUserData(ctx, key, active ? "1" : "0");
    }
}
