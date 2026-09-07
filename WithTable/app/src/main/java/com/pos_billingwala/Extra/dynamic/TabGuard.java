package com.pos_billingwala.Extra.dynamic;

import android.content.Context;
import android.view.View;

import com.pos_billingwala.Extra.LicenseModules;

/**
 * Applies tab visibility without crashing on null views.
 */
public final class TabGuard {

    private TabGuard() {
    }

    public static void setVisible(Context context, View view, String tabCode) {
        if (view == null) {
            return;
        }
        LicenseModules.setVisible(view, TabRegistry.isVisible(context, tabCode));
    }
}
