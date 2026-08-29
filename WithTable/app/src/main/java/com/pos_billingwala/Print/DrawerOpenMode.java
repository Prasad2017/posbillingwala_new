package com.pos_billingwala.Print;

/**
 * When the cash drawer should open after a successful bill print.
 */
public enum DrawerOpenMode {
    CASH_ONLY,
    ALWAYS,
    NEVER;

    public static DrawerOpenMode fromStored(String value) {
        if (value == null || value.trim().isEmpty()) {
            return CASH_ONLY;
        }
        String v = value.trim().toUpperCase().replace(' ', '_');
        if ("ALWAYS".equals(v)) {
            return ALWAYS;
        }
        if ("NEVER".equals(v) || "OFF".equals(v)) {
            return NEVER;
        }
        return CASH_ONLY;
    }

    public String toStored() {
        switch (this) {
            case ALWAYS:
                return "ALWAYS";
            case NEVER:
                return "NEVER";
            case CASH_ONLY:
            default:
                return "CASH_ONLY";
        }
    }

    public boolean shouldOpen(String paymentMode) {
        switch (this) {
            case ALWAYS:
                return true;
            case NEVER:
                return false;
            case CASH_ONLY:
            default:
                return paymentMode != null && paymentMode.trim().equalsIgnoreCase("Cash");
        }
    }
}
