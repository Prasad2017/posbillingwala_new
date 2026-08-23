package com.pos_billingwala.Model;

/**
 * Resolves frozen bill-line display name and price from snapshot columns.
 * Legacy rows without snapshots fall back to existing productName / price fields.
 */
public final class BillLineSnapshot {

    private BillLineSnapshot() {
    }

    public static String displayName(String productName, String snapshotProductName, String portionName) {
        String base = hasText(snapshotProductName) ? snapshotProductName : productName;
        if (!hasText(base)) {
            base = "";
        }
        if (hasText(portionName)) {
            return base + " (" + portionName.trim() + ")";
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
