package com.pos_billingwala.Extra;

/**
 * Canonical billing modes. {@link #wireValue} is the legacy SQLite / Intent string —
 * never rename these values (reports, sync, and carts depend on them).
 */
public enum BillingMode {

    FAST("fast_billing", FeatureFlags.FAST_BILLING, true, "Fast Billing"),
    TABLE("table_wise", FeatureFlags.DINE_IN, true, "Dine-In"),
    TAKEAWAY("take_away", FeatureFlags.TAKE_AWAY, true, "Takeaway"),
    /**
     * Parallel mess domain (membership / tokens) — not the CreatePos cart → BluetoothPrint path.
     */
    MESS("mess", FeatureFlags.MESS, false, "Mess");

    private final String wireValue;
    private final String featureFlag;
    private final boolean usesCreatePosCart;
    private final String displayName;

    BillingMode(String wireValue, String featureFlag, boolean usesCreatePosCart, String displayName) {
        this.wireValue = wireValue;
        this.featureFlag = featureFlag;
        this.usesCreatePosCart = usesCreatePosCart;
        this.displayName = displayName;
    }

    public String getWireValue() {
        return wireValue;
    }

    public String getFeatureFlag() {
        return featureFlag;
    }

    public boolean usesCreatePosCart() {
        return usesCreatePosCart;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse legacy {@code cartOrderStatus} / {@code invoiceType}. Unknown → {@link #FAST}.
     */
    public static BillingMode fromWire(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return FAST;
        }
        String value = raw.trim();
        for (BillingMode mode : values()) {
            if (mode.wireValue.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return FAST;
    }

    /**
     * Invoice type written by the cart save path. Mess never goes through this path;
     * unknown / mess → {@link #FAST} wire value (matches prior BluetoothPrint fallback).
     */
    public static String invoiceTypeForCartSave(String cartOrderStatus) {
        BillingMode mode = fromWire(cartOrderStatus);
        if (mode == MESS || !mode.usesCreatePosCart) {
            return FAST.wireValue;
        }
        return mode.wireValue;
    }
}
