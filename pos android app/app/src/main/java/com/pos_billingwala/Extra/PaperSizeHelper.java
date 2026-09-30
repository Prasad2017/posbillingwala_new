package com.pos_billingwala.Extra;

/**
 * Maps stored paper-size labels to layout families.
 * Legacy: "2-Inch" (58mm), "3-Inch" (80mm).
 * Extended: "60mm", "78mm".
 */
public final class PaperSizeHelper {
    private PaperSizeHelper() {}

    public static String normalize(String raw) {
        if (raw == null) {
            return "2-Inch";
        }
        String n = raw.trim().toLowerCase().replace(" ", "");
        if (n.isEmpty()) {
            return "2-Inch";
        }
        if (n.contains("60")) {
            return "60mm";
        }
        if (n.contains("78")) {
            return "78mm";
        }
        if (n.contains("58") || n.equals("2") || n.contains("2-inch") || n.contains("2inch")) {
            return "2-Inch";
        }
        if (n.contains("80") || n.equals("3") || n.contains("3-inch") || n.contains("3inch")) {
            return "3-Inch";
        }
        if (n.contains("3")) {
            return "3-Inch";
        }
        return "2-Inch";
    }

    /** Wide layout family: 78mm / 80mm (3-Inch). */
    public static boolean isWide(String raw) {
        String n = normalize(raw);
        return "3-Inch".equalsIgnoreCase(n) || "78mm".equalsIgnoreCase(n);
    }

    /** Narrow layout family: 58mm / 60mm (2-Inch). */
    public static boolean isNarrow(String raw) {
        return !isWide(raw);
    }

    /** Printable width in mm for the stored label (48 / 50 / 70 / 72). */
    public static int printableWidthMm(String raw) {
        return PrinterPaperProfile.of(raw).effectivePrintWidthMm();
    }
}
