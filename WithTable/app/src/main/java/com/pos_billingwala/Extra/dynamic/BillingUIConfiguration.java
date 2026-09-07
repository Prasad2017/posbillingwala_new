package com.pos_billingwala.Extra.dynamic;

import android.content.Context;

import com.pos_billingwala.Extra.DynamicUiEngine;
import com.pos_billingwala.Extra.dynamicui.UIConfiguration;

/**
 * Public billing UI configuration facade (Phase 06).
 */
public final class BillingUIConfiguration {

    public final UIConfiguration ui;

    private BillingUIConfiguration(UIConfiguration ui) {
        this.ui = ui;
    }

    public static BillingUIConfiguration resolve(Context context) {
        return new BillingUIConfiguration(DynamicUiEngine.resolve(context));
    }

    /** Snapshot at cart open — callers should keep this instance for the cart lifetime. */
    public static BillingUIConfiguration snapshotForCart(Context context) {
        ConfigApplyGuard.setCartActive(context, true);
        return resolve(context);
    }

    public static void releaseCart(Context context) {
        ConfigApplyGuard.setCartActive(context, false);
    }

    public boolean isFieldVisible(String fieldCode) {
        return ui != null && ui.isBillingFieldVisible(fieldCode);
    }

    public boolean isSectionVisible(String sectionCode) {
        return ui != null && ui.isBillingSectionVisible(sectionCode);
    }
}
