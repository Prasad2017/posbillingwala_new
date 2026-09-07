package com.pos_billingwala.Extra;

import android.content.Context;

/**
 * Dynamic settings + reports facade over UserSetting / ReportsHub.
 * Phase 19: map entry points; report PIN remains soft gate (not full RBAC).
 */
public final class DynamicSettingsReports {

    private DynamicSettingsReports() {
    }

    public static String settingsEntry() {
        return "Fragment.UserSetting";
    }

    public static String reportsEntry() {
        return "Fragment.ReportsHub (+ ReportSetting PIN)";
    }

    public static boolean isInventoryVisible(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.INVENTORY);
    }

    public static String moduleSummary(Context context) {
        return "Settings: " + settingsEntry()
                + "\nReports: " + reportsEntry()
                + "\nBusiness template: Settings → Store → Business Template"
                + "\nInventory row: " + (isInventoryVisible(context) ? "visible" : "hidden")
                + "\nKPIs: total/today sale FeatureFlags";
    }
}
