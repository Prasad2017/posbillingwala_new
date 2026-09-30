package com.pos_billingwala.Extra;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.DiningSessionResponse;
import com.pos_billingwala.Model.KotResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Model.ProductCartResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * KOT / order-round helpers on top of existing cart + printer settings.
 * Delta-only: each KOT contains items not yet assigned to a prior KOT.
 */
public final class DineInKotHelper {

    private DineInKotHelper() {
    }

    public static boolean isAutoPrint(POSBillingWalaDatabase db) {
        PrinterSettingResponse s = firstSetting(db);
        if (s == null || s.getKotAutoPrint() == null || s.getKotAutoPrint().trim().isEmpty()) {
            return false;
        }
        return isFlagOn(s.getKotAutoPrint());
    }

    public static boolean isPreviewEnabled(POSBillingWalaDatabase db) {
        PrinterSettingResponse s = firstSetting(db);
        if (s == null || s.getKotPreview() == null || s.getKotPreview().trim().isEmpty()) {
            return true;
        }
        return isFlagOn(s.getKotPreview());
    }

    /** on / 1 / true enable the flag. off / 0 / false / no disable it. */
    public static boolean isFlagOn(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase(java.util.Locale.ROOT);
        return !v.isEmpty()
                && !("off".equals(v) || "0".equals(v) || "false".equals(v) || "no".equals(v));
    }

    /** Same as Flutter: auto print or preview off skips the on-screen ticket. */
    public static boolean shouldSkipKotPreview(POSBillingWalaDatabase db) {
        return isAutoPrint(db) || !isPreviewEnabled(db);
    }

    public static int kotCopies(POSBillingWalaDatabase db) {
        PrinterSettingResponse s = firstSetting(db);
        if (s == null || s.getKotCopies() == null) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(s.getKotCopies().trim()));
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * Creates a KOT for currently unprinted cart lines on the table.
     * Returns null when there are no new items or KOT is disabled.
     */
    public static KotResponse createKotForTable(POSBillingWalaDatabase db, String tableNumber) {
        if (db == null || tableNumber == null || !DineInTableHelper.isKotEnabled(db)) {
            return null;
        }
        DiningSessionResponse session = DineInTableHelper.openOrGetSession(db, tableNumber, 0);
        if (session == null || session.getSessionId() == null) {
            return null;
        }
        List<ProductCartResponse> unprinted =
                db.getUnprintedCartProductList(tableNumber, DineInTableHelper.CART_ORDER_TABLE);
        if (unprinted == null || unprinted.isEmpty()) {
            return null;
        }
        return db.createKotForUnprintedItems(
                tableNumber,
                DineInTableHelper.CART_ORDER_TABLE,
                session.getSessionId(),
                DineInTableHelper.kotPrefix(db));
    }

    public static List<ProductCartResponse> loadKotLines(POSBillingWalaDatabase db, KotResponse kot) {
        if (db == null || kot == null || kot.getKotId() == null) {
            return new ArrayList<>();
        }
        return db.getKotItemsAsCartLines(kot.getKotId());
    }

    private static PrinterSettingResponse firstSetting(POSBillingWalaDatabase db) {
        if (db == null) {
            return null;
        }
        List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
}
