package com.pos_billingwala.Model;

/**
 * Visual / operational status for a physical dine-in table.
 * A table is not a bill — status reflects the active dining session (if any).
 */
public final class TableStatus {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String RUNNING = "RUNNING";
    public static final String BILL_REQUESTED = "BILL_REQUESTED";
    public static final String PAYMENT_PENDING = "PAYMENT_PENDING";
    public static final String PARTIALLY_PAID = "PARTIALLY_PAID";
    public static final String RESERVED = "RESERVED";
    public static final String BLOCKED = "BLOCKED";

    private TableStatus() {
    }

    public static String displayLabel(String status) {
        if (status == null) {
            return "Available";
        }
        switch (status) {
            case RUNNING:
                return "Running";
            case BILL_REQUESTED:
                return "Bill Requested";
            case PAYMENT_PENDING:
                return "Payment Pending";
            case PARTIALLY_PAID:
                return "Partially Paid";
            case RESERVED:
                return "Reserved";
            case BLOCKED:
                return "Blocked";
            case AVAILABLE:
            default:
                return "Available";
        }
    }

    public static int colorRes(String status) {
        if (status == null) {
            return com.pos_billingwala.R.color.table_status_available;
        }
        switch (status) {
            case RUNNING:
                return com.pos_billingwala.R.color.table_status_running;
            case BILL_REQUESTED:
                return com.pos_billingwala.R.color.table_status_bill_requested;
            case PAYMENT_PENDING:
            case PARTIALLY_PAID:
                return com.pos_billingwala.R.color.table_status_payment;
            case RESERVED:
                return com.pos_billingwala.R.color.table_status_reserved;
            case BLOCKED:
                return com.pos_billingwala.R.color.table_status_blocked;
            case AVAILABLE:
            default:
                return com.pos_billingwala.R.color.table_status_available;
        }
    }

    /** Legend entries shown on the floor screen (status + color). */
    public static String[][] legendEntries() {
        return new String[][]{
                {AVAILABLE, displayLabel(AVAILABLE)},
                {RUNNING, displayLabel(RUNNING)},
                {BILL_REQUESTED, displayLabel(BILL_REQUESTED)},
                {PAYMENT_PENDING, "Payment"},
                {RESERVED, displayLabel(RESERVED)},
                {BLOCKED, displayLabel(BLOCKED)},
        };
    }
}
