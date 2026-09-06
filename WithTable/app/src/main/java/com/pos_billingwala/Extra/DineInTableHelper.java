package com.pos_billingwala.Extra;

import android.text.TextUtils;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.DiningSessionResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.PosTableResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.Model.TableStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Dine-in table / session helpers on top of the existing cart + invoice engine.
 * Does not replace billing, tax, or payment logic.
 */
public final class DineInTableHelper {

    public static final String CART_ORDER_TABLE = "table_wise";

    private DineInTableHelper() {
    }

    public static String formatElapsed(long startedAtMillis) {
        if (startedAtMillis <= 0L) {
            return "";
        }
        long minutes = Math.max(0L, TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - startedAtMillis));
        if (minutes < 60L) {
            return minutes + " min";
        }
        long hours = minutes / 60L;
        long rem = minutes % 60L;
        return hours + "h " + rem + "m";
    }

    public static String formatAmount(float amount) {
        String currency = MainActivity.currencyName != null ? MainActivity.currencyName : "₹";
        return currency + " " + String.format(Locale.US, "%.2f", amount);
    }

    public static boolean isKotEnabled(POSBillingWalaDatabase db) {
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
        return "on".equalsIgnoreCase(enabled.trim());
    }

    public static String kotPrefix(POSBillingWalaDatabase db) {
        if (db == null) {
            return "KOT-";
        }
        List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
        if (list == null || list.isEmpty()) {
            return "KOT-";
        }
        String prefix = list.get(0).getKotPrefix();
        if (prefix == null || prefix.trim().isEmpty()) {
            return "KOT-";
        }
        return prefix;
    }

    public static float computeCartTotal(POSBillingWalaDatabase db, String tableNumber) {
        if (db == null || tableNumber == null) {
            return 0f;
        }
        List<CompanyResponse> companyResponseList = db.getCompanyDetails();
        List<ProductCartResponse> productCartResponseList = db.getCartProductList(tableNumber, CART_ORDER_TABLE);
        if (productCartResponseList == null || productCartResponseList.isEmpty()) {
            return 0f;
        }
        String discountType = "";
        String packingChargeType = "";
        float totalPerProductAmount = 0f, discountAmount = 0f, packingAmount = 0f;
        float totalCGST = 0f, totalSGST = 0f, totalGST = 0f;
        for (int i = 0; i < productCartResponseList.size(); i++) {
            ProductCartResponse line = productCartResponseList.get(i);
            float productPrice = ReportCursorHelper.parseAmount(line.getProductOldPrice());
            float productQuantity = ReportCursorHelper.parseAmount(line.getProductQuantity());
            if (line.getProductCGST() != null && !line.getProductCGST().isEmpty()) {
                totalCGST += ReportCursorHelper.parseAmount(line.getProductCGST());
            }
            if (line.getProductSGST() != null && !line.getProductSGST().isEmpty()) {
                totalSGST += ReportCursorHelper.parseAmount(line.getProductSGST());
            }
            discountAmount = ReportCursorHelper.parseAmount(line.getCartDiscount());
            discountType = productCartResponseList.get(0).getCartDiscountType();
            packingAmount = ReportCursorHelper.parseAmount(productCartResponseList.get(0).getCartPackingCharge());
            packingChargeType = productCartResponseList.get(0).getCartPackingChargeType();
            float totalPerProductGST = (productPrice * ((totalCGST + totalSGST) / 100));
            totalGST += (productPrice * ((totalCGST + totalSGST) / 100)) * productQuantity;
            totalPerProductAmount = totalPerProductAmount + ((productPrice + totalPerProductGST) * productQuantity);
        }
        float subTotalAmt = totalPerProductAmount - totalGST;
        if (discountType != null && discountType.equalsIgnoreCase("Amount")) {
            // amount as entered
        } else if (discountAmount != 0f) {
            discountAmount = subTotalAmt / (100 / discountAmount);
        } else {
            discountAmount = 0f;
        }
        packingAmount = ReportCursorHelper.packingRupees(
                productCartResponseList.get(0).getCartPackingCharge(),
                packingChargeType,
                String.valueOf(subTotalAmt));
        float shopCGST = 0f, shopSGST = 0f;
        if (companyResponseList != null && !companyResponseList.isEmpty()) {
            CompanyResponse company = companyResponseList.get(0);
            try {
                if (company.getShopCGST() != null && !company.getShopCGST().trim().isEmpty()) {
                    shopCGST = subTotalAmt * (Float.parseFloat(company.getShopCGST().trim()) / 100);
                }
                if (company.getShopSGST() != null && !company.getShopSGST().trim().isEmpty()) {
                    shopSGST = subTotalAmt * (Float.parseFloat(company.getShopSGST().trim()) / 100);
                }
            } catch (Exception ignored) {
            }
        }
        float totalAmount = totalPerProductAmount - discountAmount + packingAmount + shopCGST + shopSGST;
        return (float) Math.ceil(totalAmount);
    }

    public static List<PosTableResponse> buildFloorTableList(POSBillingWalaDatabase db) {
        db.ensureDineInMastersSeeded();
        List<PosTableResponse> tables = db.getActivePosTables();
        if (tables == null) {
            tables = new ArrayList<>();
        }
        for (PosTableResponse table : tables) {
            enrichTableRuntime(db, table);
        }
        return tables;
    }

    public static void enrichTableRuntime(POSBillingWalaDatabase db, PosTableResponse table) {
        if (table == null || db == null) {
            return;
        }
        String tableNumber = table.getTableNumber();
        if (TableStatus.BLOCKED.equalsIgnoreCase(safe(table.getStatusOverride()))
                || TableStatus.RESERVED.equalsIgnoreCase(safe(table.getStatusOverride()))) {
            table.setDisplayStatus(table.getStatusOverride().trim().toUpperCase(Locale.US));
            return;
        }

        List<ProductCartResponse> cart = db.getCartProductList(tableNumber, CART_ORDER_TABLE);
        boolean hasCart = cart != null && !cart.isEmpty();
        table.setHasCartItems(hasCart);

        List<InvoiceResponse> unpaid = db.checkTablePaymentMode(tableNumber);
        boolean hasUnpaid = unpaid != null && !unpaid.isEmpty();
        if (hasUnpaid) {
            table.setUnpaidInvoiceNumber(unpaid.get(0).getInvoiceNumber());
            try {
                table.setCurrentAmount(ReportCursorHelper.parseAmount(unpaid.get(0).getTotalAmount()));
            } catch (Exception ignored) {
            }
        }

        DiningSessionResponse session = db.getOpenDiningSessionForTable(tableNumber);
        if (session != null) {
            table.setSessionId(session.getSessionId());
            table.setJoinedTableLabel(session.dineInHeaderLabel());
            try {
                table.setGuestCount(Integer.parseInt(safe(session.getGuestCount()).isEmpty() ? "0" : session.getGuestCount()));
            } catch (Exception ignored) {
                table.setGuestCount(0);
            }
            try {
                table.setSessionStartedAt(Long.parseLong(session.getStartedAt()));
            } catch (Exception ignored) {
                table.setSessionStartedAt(0L);
            }
            String status = safe(session.getSessionStatus());
            if (!status.isEmpty()) {
                table.setDisplayStatus(status);
            }
        }

        if (hasCart) {
            table.setCurrentAmount(computeCartTotal(db, tableNumber));
            if (session != null) {
                float paid = DineInSettlementHelper.sessionPaid(session);
                float rem = DineInSettlementHelper.remaining(table.getCurrentAmount(), session);
                table.setRemainingAmount(rem);
                if (paid > 0.05f && rem > 0.05f) {
                    table.setDisplayStatus(TableStatus.PARTIALLY_PAID);
                }
            }
            if (session == null) {
                // Cart-only running bill (session created lazily on open/hold)
                table.setDisplayStatus(TableStatus.RUNNING);
                if (table.getSessionStartedAt() <= 0L) {
                    // Prefer earliest cart row time is unavailable — use now only for display fallback
                    table.setSessionStartedAt(0L);
                }
            } else if (TextUtils.isEmpty(table.getDisplayStatus())
                    || TableStatus.AVAILABLE.equals(table.getDisplayStatus())) {
                table.setDisplayStatus(TableStatus.RUNNING);
            }
        } else if (hasUnpaid) {
            if (TextUtils.isEmpty(table.getDisplayStatus())
                    || TableStatus.AVAILABLE.equals(table.getDisplayStatus())
                    || TableStatus.RUNNING.equals(table.getDisplayStatus())) {
                table.setDisplayStatus(TableStatus.PAYMENT_PENDING);
            }
            try {
                InvoiceResponse inv = unpaid.get(0);
                float total = ReportCursorHelper.parseAmount(inv.getTotalAmount());
                table.setRemainingAmount(total);
                if (inv.getBillPrintStatus() == null) {
                    // map may not include billPrintStatus from checkTablePaymentMode — lookup
                    InvoiceResponse full = db.getInvoiceByNumber(inv.getInvoiceNumber());
                    if (full != null && DineInSettlementHelper.invoiceNeedsPrintRetry(full)) {
                        table.setPrintRetryAvailable(true);
                    }
                } else if (DineInSettlementHelper.invoiceNeedsPrintRetry(inv)) {
                    table.setPrintRetryAvailable(true);
                }
            } catch (Exception ignored) {
            }
        } else {
            // No cart items and no unpaid bill → table is free
            InvoiceResponse failedPrint = db.getLatestFailedPrintInvoiceForTable(tableNumber);
            if (failedPrint != null) {
                table.setPrintRetryAvailable(true);
                table.setUnpaidInvoiceNumber(failedPrint.getInvoiceNumber());
            }
            if (session != null && failedPrint == null) {
                // Abandon empty open session so floor does not stay "Running"
                db.updateDiningSessionStatus(session.getSessionId(), "CLOSED");
                table.setSessionId(null);
                table.setJoinedTableLabel(null);
                table.setGuestCount(0);
                table.setSessionStartedAt(0L);
            }
            table.setDisplayStatus(TableStatus.AVAILABLE);
            table.setCurrentAmount(0f);
            table.setRemainingAmount(0f);
        }
    }

    public static DiningSessionResponse openOrGetSession(POSBillingWalaDatabase db, String tableNumber, int guestCount) {
        if (db == null || tableNumber == null || tableNumber.trim().isEmpty()) {
            return null;
        }
        DiningSessionResponse existing = db.getOpenDiningSessionForTable(tableNumber);
        if (existing != null) {
            return existing;
        }
        return db.createDiningSession(tableNumber.trim(), guestCount);
    }

    public static String dineInHeaderForTable(POSBillingWalaDatabase db, String tableNumber) {
        DiningSessionResponse session = db != null ? db.getOpenDiningSessionForTable(tableNumber) : null;
        if (session != null) {
            return session.dineInHeaderLabel();
        }
        return "DINE-IN • T" + (tableNumber == null ? "?" : tableNumber.trim());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
