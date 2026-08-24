package com.pos_billingwala.Extra;

public final class CartItemType {

    public static final String PRODUCT = "PRODUCT";
    public static final String COMBO = "COMBO";

    private CartItemType() {
    }

    public static boolean isCombo(String itemType) {
        return itemType != null && COMBO.equalsIgnoreCase(itemType.trim());
    }

    public static String normalize(String itemType) {
        return isCombo(itemType) ? COMBO : PRODUCT;
    }
}
