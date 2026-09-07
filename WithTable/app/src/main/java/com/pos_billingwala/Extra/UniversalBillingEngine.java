package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.ProductCartResponse;

/**
 * Universal billing facade over existing cart / invoice modes.
 * Does not replace {@code BluetoothPrint.saveInvoice} — adapters only.
 * Legacy wire strings stay {@link BillingMode#getWireValue()}.
 */
public final class UniversalBillingEngine {

    private UniversalBillingEngine() {
    }

    public static BillingMode resolveMode(String cartOrderStatusOrInvoiceType) {
        return BillingMode.fromWire(cartOrderStatusOrInvoiceType);
    }

    public static boolean canStart(Context context, BillingMode mode) {
        if (mode == null) {
            return false;
        }
        return FeatureEngine.isEnabled(context, mode.getFeatureFlag());
    }

    public static boolean canStart(Context context, String cartOrderStatus) {
        return canStart(context, BillingMode.fromWire(cartOrderStatus));
    }

    /**
     * Value stored on {@code invoice.invoiceType} for CreatePos cart checkout.
     */
    public static String invoiceTypeForCartSave(String cartOrderStatus) {
        return BillingMode.invoiceTypeForCartSave(cartOrderStatus);
    }

    public static boolean isTableWise(String cartOrderStatusOrInvoiceType) {
        return BillingMode.fromWire(cartOrderStatusOrInvoiceType) == BillingMode.TABLE;
    }

    public static boolean isTakeAway(String cartOrderStatusOrInvoiceType) {
        return BillingMode.fromWire(cartOrderStatusOrInvoiceType) == BillingMode.TAKEAWAY;
    }

    public static boolean isFastBilling(String cartOrderStatusOrInvoiceType) {
        return BillingMode.fromWire(cartOrderStatusOrInvoiceType) == BillingMode.FAST;
    }

    public static boolean isMess(String cartOrderStatusOrInvoiceType) {
        return BillingMode.fromWire(cartOrderStatusOrInvoiceType) == BillingMode.MESS;
    }

    public static boolean usesCreatePosCart(String cartOrderStatus) {
        return BillingMode.fromWire(cartOrderStatus).usesCreatePosCart();
    }

    public static String normalizeLineType(String cartItemType) {
        return CartItemType.normalize(cartItemType);
    }

    public static boolean isComboLine(ProductCartResponse line) {
        return line != null && CartItemType.isCombo(line.getCartItemType());
    }

    public static String displayLineName(ProductCartResponse line) {
        return line == null ? "" : line.getDisplayLineName();
    }

    public static String resolvedLinePrice(ProductCartResponse line) {
        return line == null ? "0" : line.getResolvedLinePrice();
    }

    /**
     * Shop GST on/off — same source as print/save (company row).
     */
    public static boolean isGstOn(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.GST);
    }

    public static boolean isGstOn(POSBillingWalaDatabase db) {
        return FeatureEngine.isEnabled(null, FeatureFlags.GST, db);
    }

    /**
     * Grand total: (subtotal − discount) + packing [+ GST when on].
     * Mirrors BluetoothPrint.saveInvoice arithmetic.
     */
    public static float computeGrandTotal(float subTotal, float discountAmount, float packingAmount,
                                          float gstAmount, boolean gstOn) {
        float base = (subTotal - discountAmount) + packingAmount;
        return gstOn ? base + gstAmount : base;
    }
}
