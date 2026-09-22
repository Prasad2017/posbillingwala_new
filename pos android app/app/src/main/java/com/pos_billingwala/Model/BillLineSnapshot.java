package com.pos_billingwala.Model;

/**
 * Resolves frozen bill-line display name and price from snapshot columns.
 * Legacy rows without snapshots fall back to existing productName / price fields.
 */
public final class BillLineSnapshot {

    private BillLineSnapshot() {
    }

    public static String displayName(String productName, String snapshotProductName, String portionName) {
        return displayName(productName, snapshotProductName, portionName, null);
    }

    public static String displayName(String productName, String snapshotProductName, String portionName,
                                     String comboComponents) {
        String base = hasText(snapshotProductName) ? snapshotProductName : productName;
        if (!hasText(base)) {
            base = "";
        }
        if (hasText(portionName)) {
            base = base + " (" + portionName.trim() + ")";
        }
        if (hasText(comboComponents)) {
            return base + "\n" + comboComponents.trim();
        }
        return base;
    }

    public static String linePrice(String snapshotLinePrice, String primaryFallback, String secondaryFallback) {
        if (hasText(snapshotLinePrice)) {
            return snapshotLinePrice;
        }
        if (hasText(primaryFallback)) {
            return primaryFallback;
        }
        return hasText(secondaryFallback) ? secondaryFallback : "0";
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
