package com.pos_billingwala.Extra;

import android.content.Context;
import android.view.View;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;

/**
 * Resolves whether a feature is active:
 * <b>template ∩ licence ∩ company/printer toggles</b>.
 * Default restaurant template + existing toggles = prior live behaviour.
 */
public final class FeatureEngine {

    private FeatureEngine() {
    }

    public static boolean isEnabled(Context context, String featureFlag) {
        return isEnabled(context, featureFlag, null);
    }

    public static boolean isEnabled(Context context, String featureFlag, POSBillingWalaDatabase db) {
        if (featureFlag == null || featureFlag.trim().isEmpty()) {
            return false;
        }
        Context ctx = AppContexts.or(context);
        BusinessTemplate template = BusinessTemplateRegistry.resolve(ctx);
        if (!template.supports(featureFlag)) {
            return false;
        }
        if (!isLicenceAllowed(featureFlag)) {
            return false;
        }
        return isCompanyAllowed(ctx, featureFlag, db);
    }

    public static void setVisible(Context context, View view, String featureFlag) {
        LicenseModules.setVisible(view, isEnabled(context, featureFlag));
    }

    public static BusinessTemplate currentTemplate(Context context) {
        return BusinessTemplateRegistry.resolve(AppContexts.or(context));
    }

    /**
     * Why a template-supported flag is off (for Settings readout). Empty if enabled.
     */
    public static String disabledReason(Context context, String featureFlag) {
        return disabledReason(context, featureFlag, null);
    }

    public static String disabledReason(Context context, String featureFlag, POSBillingWalaDatabase db) {
        if (featureFlag == null || featureFlag.trim().isEmpty()) {
            return "invalid";
        }
        Context ctx = AppContexts.or(context);
        BusinessTemplate template = BusinessTemplateRegistry.resolve(ctx);
        if (!template.supports(featureFlag)) {
            return "template off";
        }
        if (!isLicenceAllowed(featureFlag)) {
            return "licence off";
        }
        if (!isCompanyAllowed(ctx, featureFlag, db)) {
            switch (featureFlag) {
                case FeatureFlags.GST:
                    return "GST off in shop details";
                case FeatureFlags.TABLES:
                    return "tables off in shop details";
                case FeatureFlags.KOT:
                    return "KOT off in printer settings";
                case FeatureFlags.BOT:
                    return "BOT off in printer settings";
                default:
                    return "company off";
            }
        }
        return "";
    }

    private static boolean isLicenceAllowed(String featureFlag) {
        switch (featureFlag) {
            case FeatureFlags.FAST_BILLING:
                return LicenseModules.isEnabled(MainActivity.fastBilling);
            case FeatureFlags.DINE_IN:
                return LicenseModules.isEnabled(MainActivity.dineIn);
            case FeatureFlags.TAKE_AWAY:
                return LicenseModules.isEnabled(MainActivity.takeAway);
            case FeatureFlags.MESS:
                return LicenseModules.isEnabled(MainActivity.mess);
            case FeatureFlags.TOTAL_SALE_DATA:
                return LicenseModules.isEnabled(MainActivity.totalSaleData);
            case FeatureFlags.TODAY_SALE_DATA:
                return LicenseModules.isEnabled(MainActivity.todaySaleData);
            default:
                return true;
        }
    }

    private static boolean isCompanyAllowed(Context context, String featureFlag, POSBillingWalaDatabase db) {
        switch (featureFlag) {
            case FeatureFlags.GST:
                return db != null ? CompanyFeatureToggles.isGstOn(db) : CompanyFeatureToggles.isGstOn(context);
            case FeatureFlags.TABLES:
                return db != null ? CompanyFeatureToggles.isTablesOn(db) : CompanyFeatureToggles.isTablesOn(context);
            case FeatureFlags.KOT:
                return db != null ? CompanyFeatureToggles.isKotOn(db) : CompanyFeatureToggles.isKotOn(context);
            case FeatureFlags.BOT:
                return db != null ? CompanyFeatureToggles.isBotOn(db) : CompanyFeatureToggles.isBotOn(context);
            default:
                return true;
        }
    }
}
