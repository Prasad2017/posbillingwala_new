package com.pos_billingwala.Extra;

import java.util.List;
import java.util.Locale;

/**
 * Pure validation for fixed Combo masters. No Android / DB dependency.
 */
public final class ComboValidator {

    public static final String ERR_NAME = "name";
    public static final String ERR_PRICE = "price";
    public static final String ERR_ITEMS = "items";
    public static final String ERR_PRODUCT = "product";
    public static final String ERR_PRODUCT_INACTIVE = "product_inactive";
    public static final String ERR_PORTION_REQUIRED = "portion_required";
    public static final String ERR_PORTION_MISMATCH = "portion_mismatch";
    public static final String ERR_QUANTITY = "quantity";

    private ComboValidator() {
    }

    public static String validateCombo(String name, String sellingPrice, int itemCount) {
        if (name == null || name.trim().isEmpty()) {
            return ERR_NAME;
        }
        if (!isValidSellingPrice(sellingPrice)) {
            return ERR_PRICE;
        }
        if (itemCount < 1) {
            return ERR_ITEMS;
        }
        return null;
    }

    public static String validateComboItem(String productId, boolean productExists, boolean productActive,
                                           boolean productHasPortions, String portionId,
                                           boolean portionBelongsToProduct, String quantity) {
        if (productId == null || productId.trim().isEmpty() || !productExists) {
            return ERR_PRODUCT;
        }
        if (!productActive) {
            return ERR_PRODUCT_INACTIVE;
        }
        boolean portionSet = portionId != null && !portionId.trim().isEmpty();
        if (productHasPortions && !portionSet) {
            return ERR_PORTION_REQUIRED;
        }
        if (portionSet && !portionBelongsToProduct) {
            return ERR_PORTION_MISMATCH;
        }
        if (!isValidQuantity(quantity)) {
            return ERR_QUANTITY;
        }
        return null;
    }

    public static boolean isValidSellingPrice(String price) {
        Double value = parsePositiveNumber(price);
        return value != null && value > 0d;
    }

    public static boolean isValidQuantity(String quantity) {
        Double value = parsePositiveNumber(quantity);
        return value != null && value > 0d;
    }

    public static Double parsePositiveNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String formatComponentLine(String productName, String portionName, String quantity) {
        String name = productName != null ? productName.trim() : "";
        String qty = (quantity != null && !quantity.trim().isEmpty()) ? quantity.trim() : "1";
        if (portionName != null && !portionName.trim().isEmpty()) {
            return "  - " + name + " / " + portionName.trim() + " x " + qty;
        }
        return "  - " + name + " x " + qty;
    }

    public static String buildComponentSnapshot(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        return builder.toString();
    }

    public static String comboItemDisplayName(String productName, String portionName, String quantity) {
        String name = productName != null ? productName.trim() : "";
        String qty = (quantity != null && !quantity.trim().isEmpty()) ? quantity.trim() : "1";
        if (portionName != null && !portionName.trim().isEmpty()) {
            return String.format(Locale.US, "%s — %s × %s", name, portionName.trim(), qty);
        }
        return String.format(Locale.US, "%s × %s", name, qty);
    }
}
