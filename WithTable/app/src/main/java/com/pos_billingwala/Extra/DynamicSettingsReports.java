package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.Extra.dynamic.SettingsRegistry;
import com.pos_billingwala.Extra.dynamicui.UiCodes;

/**
 * Dynamic settings + reports facade over UserSetting / ReportsHub.
 * Phase 19 + Dynamic Phase 09/11: SettingsRegistry + DynamicUiEngine.
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
        return SettingsRegistry.isSectionVisible(context, UiCodes.ST_INVENTORY);
    }

    public static boolean isSectionVisible(Context context, String sectionCode) {
        return SettingsRegistry.isSectionVisible(context, sectionCode);
    }

    public static String moduleSummary(Context context) {
        return "Settings: " + settingsEntry()
                + "\nReports: " + reportsEntry()
                + "\nBusiness template: Settings → Store → Business Template"
                + "\nInventory row: " + (isInventoryVisible(context) ? "visible" : "hidden")
                + "\nScale: " + (SettingsRegistry.isSectionVisible(context, UiCodes.ST_SCALE) ? "on" : "off")
                + "\nMaster: " + (SettingsRegistry.isSectionVisible(context, UiCodes.ST_MASTER) ? "on" : "off")
                + "\nKPIs / reports: DynamicUiEngine + SettingsRegistry";
    }
}
