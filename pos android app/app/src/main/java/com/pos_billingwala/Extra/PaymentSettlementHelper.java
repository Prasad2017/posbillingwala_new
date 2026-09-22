package com.pos_billingwala.Extra;

import java.util.Locale;

/**
 * Canonical payment modes and cash/UPI amount rules for split settlement.
 */
public final class PaymentSettlementHelper {

    public static final String MODE_CASH = "Cash";
    public static final String MODE_UPI = "UPI";
    public static final String MODE_BANK = "Bank";
    public static final String MODE_SPLIT = "Cash+UPI";
    public static final float MATCH_TOLERANCE = 0.05f;

    private PaymentSettlementHelper() {
    }

    public static String canonicalMode(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        String compact = value.replace(" ", "").toLowerCase(Locale.US);
        if (compact.contains("cash") && (compact.contains("upi") || compact.contains("online"))) {
            return MODE_SPLIT;
        }
        if (compact.contains("cash+") || compact.equals("split")) {
            return MODE_SPLIT;
        }
        if (value.equalsIgnoreCase("Bank") || compact.contains("bank")) {
            return MODE_BANK;
        }
        if (compact.contains("upi") || compact.contains("online")) {
            return MODE_UPI;
        }
        if (compact.contains("cash") || value.equalsIgnoreCase("नकद") || value.equalsIgnoreCase("रोख")) {
            return MODE_CASH;
        }
        return value;
    }

    public static boolean isSplit(String mode) {
        return MODE_SPLIT.equalsIgnoreCase(canonicalMode(mode));
    }

    public static boolean isUpi(String mode) {
        return MODE_UPI.equalsIgnoreCase(canonicalMode(mode));
    }

    public static boolean isCash(String mode) {
        return MODE_CASH.equalsIgnoreCase(canonicalMode(mode));
    }

    public static float parseAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0f;
        }
        try {
            return Float.parseFloat(raw.trim().replace(",", ""));
        } catch (Exception e) {
            return 0f;
        }
    }

    public static String formatAmount(float amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    public static boolean amountsMatch(float cash, float upi, float total) {
        return Math.abs((cash + upi) - total) <= MATCH_TOLERANCE;
    }

    public static Tender resolve(String paymentMode, float total, String cashRaw, String upiRaw) {
        Tender tender = new Tender();
        tender.mode = canonicalMode(paymentMode);
        float cash = parseAmount(cashRaw);
        float upi = parseAmount(upiRaw);
        if (tender.mode.isEmpty()) {
            tender.cashAmount = formatAmount(0f);
            tender.upiAmount = formatAmount(0f);
            return tender;
        }
        if (isSplit(tender.mode)) {
            tender.cashAmount = formatAmount(cash);
            tender.upiAmount = formatAmount(upi);
            return tender;
        }
        if (isUpi(tender.mode)) {
            tender.cashAmount = formatAmount(0f);
            tender.upiAmount = formatAmount(total);
            return tender;
        }
        if (MODE_BANK.equals(tender.mode)) {
            tender.cashAmount = formatAmount(0f);
            tender.upiAmount = formatAmount(0f);
            return tender;
        }
        tender.cashAmount = formatAmount(total);
        tender.upiAmount = formatAmount(0f);
        return tender;
    }

    public static float resolvedCash(String paymentMode, String totalAmount, String cashAmount) {
        float stored = parseAmount(cashAmount);
        if (stored > 0.001f) {
            return stored;
        }
        if (isCash(paymentMode)) {
            return parseAmount(totalAmount);
        }
        return 0f;
    }

    public static float resolvedUpi(String paymentMode, String totalAmount, String upiAmount) {
        float stored = parseAmount(upiAmount);
        if (stored > 0.001f) {
            return stored;
        }
        if (isUpi(paymentMode)) {
            return parseAmount(totalAmount);
        }
        return 0f;
    }

    public static String displayLabel(String paymentMode, String cashAmount, String upiAmount,
                                      String totalAmount) {
        String mode = canonicalMode(paymentMode);
        if (mode.isEmpty()) {
            return "";
        }
        if (isSplit(mode)) {
            return MODE_SPLIT + " (Cash " + formatAmount(resolvedCash(mode, totalAmount, cashAmount))
                    + " + UPI " + formatAmount(resolvedUpi(mode, totalAmount, upiAmount)) + ")";
        }
        return mode;
    }

    /**
     * SQLite expression: cash collected for a row (legacy bills fall back to full total when Cash).
     */
    public static String sqlResolvedCash() {
        return "CASE"
                + " WHEN cashAmount IS NOT NULL AND TRIM(cashAmount) != '' AND CAST(cashAmount AS REAL) > 0.001 THEN CAST(cashAmount AS REAL)"
                + " WHEN LOWER(TRIM(IFNULL(paymentMode,''))) IN ('cash') THEN CAST(totalAmount AS REAL)"
                + " ELSE 0 END";
    }

    public static String sqlResolvedUpi() {
        return "CASE"
                + " WHEN upiAmount IS NOT NULL AND TRIM(upiAmount) != '' AND CAST(upiAmount AS REAL) > 0.001 THEN CAST(upiAmount AS REAL)"
                + " WHEN LOWER(TRIM(IFNULL(paymentMode,''))) IN ('upi','online') THEN CAST(totalAmount AS REAL)"
                + " ELSE 0 END";
    }

    public static class Tender {
        public String mode = "";
        public String cashAmount = "0.00";
        public String upiAmount = "0.00";
    }
}
