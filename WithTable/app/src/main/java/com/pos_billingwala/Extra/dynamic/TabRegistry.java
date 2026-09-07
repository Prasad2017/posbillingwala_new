package com.pos_billingwala.Extra.dynamic;

import android.content.Context;
import android.view.View;

import com.pos_billingwala.Extra.FeatureEngine;
import com.pos_billingwala.Extra.FeatureFlags;
import com.pos_billingwala.Extra.LicenseModules;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.BusinessTemplate;
import com.pos_billingwala.Extra.dynamicui.UiCodes;

/**
 * Tab / master-section registry + guard (Dynamic Phase 03).
 */
public final class TabRegistry {

    public static final String TAB_CATEGORY = "TAB_CATEGORY";
    public static final String TAB_SUBCATEGORY = "TAB_SUBCATEGORY";
    public static final String TAB_PORTION = "TAB_PORTION";
    public static final String TAB_PRODUCT = "TAB_PRODUCT";
    public static final String TAB_COMBO = "TAB_COMBO";
    public static final String TAB_TABLE = "TAB_TABLE";
    public static final String TAB_APPOINTMENT = "TAB_APPOINTMENT";
    public static final String TAB_DEPOSIT = "TAB_DEPOSIT";
    public static final String TAB_CSV = "TAB_CSV";

    private TabRegistry() {
    }

    public static boolean isVisible(Context context, String tabCode) {
        if (tabCode == null) {
            return false;
        }
        switch (tabCode) {
            case TAB_PORTION:
                return FeatureEngine.isEnabled(context, FeatureFlags.PORTIONS);
            case TAB_COMBO:
                return FeatureEngine.isEnabled(context, FeatureFlags.COMBOS);
            case TAB_TABLE:
                BusinessTemplate template = FeatureEngine.currentTemplate(context);
                return template != null && template.supports(FeatureFlags.TABLES)
                        && LicenseModules.isEnabled(MainActivity.dineIn);
            case TAB_APPOINTMENT:
                return FeatureEngine.isEnabled(context, FeatureFlags.APPOINTMENTS);
            case TAB_DEPOSIT:
                return FeatureEngine.isEnabled(context, FeatureFlags.CUSTOM_ORDERS);
            case TAB_CATEGORY:
            case TAB_SUBCATEGORY:
            case TAB_PRODUCT:
            case TAB_CSV:
                return true;
            default:
                return ScreenRegistry.isVisible(context, tabCode);
        }
    }

    public static void apply(Context context, View view, String tabCode) {
        TabGuard.setVisible(context, view, tabCode);
    }

    /** Map MasterData rows to screen codes for diagnostics. */
    public static String screenCodeForTab(String tabCode) {
        if (TAB_PORTION.equals(tabCode)) {
            return UiCodes.PORTIONS;
        }
        if (TAB_COMBO.equals(tabCode)) {
            return UiCodes.COMBOS;
        }
        if (TAB_TABLE.equals(tabCode)) {
            return UiCodes.TABLE_MANAGEMENT;
        }
        if (TAB_APPOINTMENT.equals(tabCode)) {
            return UiCodes.APPOINTMENTS;
        }
        if (TAB_DEPOSIT.equals(tabCode)) {
            return UiCodes.CUSTOM_ORDERS;
        }
        if (TAB_PRODUCT.equals(tabCode)) {
            return UiCodes.PRODUCTS;
        }
        return UiCodes.MASTER_DATA;
    }
}
