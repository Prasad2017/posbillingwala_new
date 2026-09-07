package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;

import java.util.List;

/**
 * Shop-level toggles from {@code company} / {@code company_printer_setting}.
 * Mirrors existing on/off string behaviour used by billing and print paths.
 */
public final class CompanyFeatureToggles {

    private CompanyFeatureToggles() {
    }

    public static boolean isOn(String raw) {
        if (raw == null) {
            return false;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return false;
        }
        return value.equalsIgnoreCase("on")
                || value.equals("1")
                || value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("yes");
    }

    public static boolean isGstOn(Context context) {
        return isGstOn(db(context));
    }

    public static boolean isGstOn(POSBillingWalaDatabase db) {
        CompanyResponse company = firstCompany(db);
        return company != null && isOn(company.getGstStatus());
    }

    public static boolean isTablesOn(Context context) {
        return isTablesOn(db(context));
    }

    public static boolean isTablesOn(POSBillingWalaDatabase db) {
        CompanyResponse company = firstCompany(db);
        return company != null && isOn(company.getTableStatus());
    }

    /**
     * Matches {@link DineInTableHelper#isKotEnabled}: missing printer row / blank → on.
     */
    public static boolean isKotOn(Context context) {
        return isKotOn(db(context));
    }

    public static boolean isKotOn(POSBillingWalaDatabase db) {
        if (db == null) {
            return true;
        }
        List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
        if (list == null || list.isEmpty()) {
            return true;
        }
        String enabled = list.get(0).getKotEnable();
        if (enabled == null || enabled.trim().isEmpty()) {
            return true;
        }
        return isOn(enabled);
    }

    /**
     * BOT default off when missing/blank so restaurant shops stay on kitchen-only KOT.
     */
    public static boolean isBotOn(Context context) {
        return isBotOn(db(context));
    }

    public static boolean isBotOn(POSBillingWalaDatabase db) {
        if (db == null) {
            return false;
        }
        List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
        if (list == null || list.isEmpty()) {
            return false;
        }
        return isOn(list.get(0).getBotEnable());
    }

    public static CompanyResponse firstCompany(POSBillingWalaDatabase db) {
        if (db == null) {
            return null;
        }
        List<CompanyResponse> list = db.getCompanyDetails();
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private static POSBillingWalaDatabase db(Context context) {
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return null;
        }
        return new POSBillingWalaDatabase(ctx);
    }
}
