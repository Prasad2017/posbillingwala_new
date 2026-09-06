package com.pos_billingwala.Extra;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.DiningSessionResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.TableStatus;

/**
 * Dine-in settlement helpers: partial pay, print status, session close rules.
 */
public final class DineInSettlementHelper {

    public static final String PRINT_PENDING = "PENDING";
    public static final String PRINT_PRINTED = "PRINTED";
    public static final String PRINT_FAILED = "FAILED";

    private DineInSettlementHelper() {
    }

    public static float sessionPaid(DiningSessionResponse session) {
        if (session == null || session.getPaidAmount() == null || session.getPaidAmount().trim().isEmpty()) {
            return 0f;
        }
        try {
            return Float.parseFloat(session.getPaidAmount().trim());
        } catch (Exception e) {
            return 0f;
        }
    }

    public static float remaining(float tableTotal, DiningSessionResponse session) {
        return Math.max(0f, tableTotal - sessionPaid(session));
    }

    public static boolean isFullyPaid(float tableTotal, DiningSessionResponse session) {
        return remaining(tableTotal, session) <= 0.05f;
    }

    /**
     * After a successful full payment, close the dining session so the table becomes AVAILABLE.
     * Print failure must not reopen the session.
     */
    public static void closeSessionAfterFullPayment(POSBillingWalaDatabase db, String tableNumber) {
        if (db == null || tableNumber == null) {
            return;
        }
        DiningSessionResponse session = db.getOpenDiningSessionForTable(tableNumber);
        if (session != null) {
            db.closeDiningSession(session.getSessionId());
        }
    }

    public static void markBillRequested(POSBillingWalaDatabase db, String tableNumber) {
        DiningSessionResponse session = DineInTableHelper.openOrGetSession(db, tableNumber, 0);
        if (session != null) {
            db.updateDiningSessionStatus(session.getSessionId(), TableStatus.BILL_REQUESTED);
        }
    }

    public static void markPaymentPending(POSBillingWalaDatabase db, String tableNumber) {
        DiningSessionResponse session = db.getOpenDiningSessionForTable(tableNumber);
        if (session != null) {
            db.updateDiningSessionStatus(session.getSessionId(), TableStatus.PAYMENT_PENDING);
        }
    }

    public static void markPartiallyPaid(POSBillingWalaDatabase db, String tableNumber) {
        DiningSessionResponse session = db.getOpenDiningSessionForTable(tableNumber);
        if (session != null) {
            db.updateDiningSessionStatus(session.getSessionId(), TableStatus.PARTIALLY_PAID);
        }
    }

    public static boolean invoiceNeedsPrintRetry(InvoiceResponse invoice) {
        if (invoice == null) {
            return false;
        }
        String status = invoice.getBillPrintStatus();
        return status != null && PRINT_FAILED.equalsIgnoreCase(status.trim());
    }
}
