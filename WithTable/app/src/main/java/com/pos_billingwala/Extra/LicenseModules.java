package com.pos_billingwala.Extra;

import android.view.View;

/**
 * License feature flags for POS modules (Fast Billing / Dine-In / Takeaway / Mess).
 */
public final class LicenseModules {

    private LicenseModules() {
    }

    public static boolean isEnabled(String flag) {
        return flag != null && flag.trim().equalsIgnoreCase("1");
    }

    public static void setVisible(View view, boolean enabled) {
        if (view == null) {
            return;
        }
        view.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }
}
