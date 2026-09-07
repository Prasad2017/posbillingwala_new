package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.PrinterSettingResponse;

import java.util.List;

/**
 * Universal printer facade over live Bluetooth / Woosim bill + KOT stack.
 * Phase 16: wrap settings; print failure must never wipe a saved bill.
 */
public final class UniversalPrinterEngine {

    private UniversalPrinterEngine() {
    }

    public static PrinterSettingResponse firstSettings(POSBillingWalaDatabase db) {
        if (db == null) {
            return null;
        }
        List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public static boolean hasBillPrinter(POSBillingWalaDatabase db) {
        PrinterSettingResponse s = firstSettings(db);
        return s != null && s.getBluetoothAddress() != null && !s.getBluetoothAddress().trim().isEmpty();
    }

    public static boolean hasKotPrinter(POSBillingWalaDatabase db) {
        PrinterSettingResponse s = firstSettings(db);
        if (s == null) {
            return false;
        }
        String kot = s.getBluetoothKOTAddress();
        return kot != null && !kot.trim().isEmpty();
    }

    public static boolean isKotEnabled(POSBillingWalaDatabase db) {
        return RestaurantFoodModule.canKot(db);
    }

    public static boolean isBotEnabled(Context context, POSBillingWalaDatabase db) {
        return BarRestaurantModule.canBot(context, db);
    }

    public static boolean hasBotPrinter(POSBillingWalaDatabase db) {
        return BarRestaurantModule.hasDedicatedBotPrinter(db)
                || hasKotPrinter(db);
    }

    public static String moduleSummary(Context context) {
        POSBillingWalaDatabase db = context != null ? new POSBillingWalaDatabase(context) : null;
        return "Bill BT: " + (hasBillPrinter(db) ? "configured" : "missing")
                + " · KOT BT: " + (hasKotPrinter(db) ? "configured" : "missing/same")
                + " · BOT BT: " + (BarRestaurantModule.hasDedicatedBotPrinter(db) ? "dedicated" : "same as KOT")
                + "\nKOT: " + (isKotEnabled(db) ? "on" : "off")
                + " · BOT: " + (isBotEnabled(context, db) ? "on" : "off")
                + "\nRule: print failure never deletes saved invoice"
                + "\nCustom-order photo: on bill when | Photo:… present"
                + "\nSettings: CompanyPrinterSetting (KOT + BOT enable/prefix/MAC)";
    }
}
