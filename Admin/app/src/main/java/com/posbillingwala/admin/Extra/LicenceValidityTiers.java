package com.posbillingwala.admin.Extra;

/**
 * P4-3: First-class licence validity tiers for Admin UI.
 * Maps display labels to day counts sent to the API.
 */
public final class LicenceValidityTiers {

    public static final String LABEL_7_DAYS = "7 Days";
    public static final String LABEL_6_MONTHS = "6 Months";
    public static final String LABEL_1_YEAR = "1 Year";
    public static final String LABEL_3_YEARS = "3 Years";
    public static final String LABEL_5_YEARS = "5 Years";
    public static final String LABEL_LIFETIME = "Lifetime";

    private LicenceValidityTiers() {
    }

    /** Day count string for API (Lifetime → 10958). */
    public static String toDayCount(String labelOrDays) {
        if (labelOrDays == null) {
            return "";
        }
        String raw = labelOrDays.trim();
        String lower = raw.toLowerCase()
                .replace(" days", "")
                .replace(" day", "")
                .trim();

        if (lower.equals("lifetime") || lower.equals("life time")) {
            return "10958";
        }
        if (lower.equals("6 months") || lower.equals("6 month") || lower.equals("6m") || lower.equals("183")) {
            return "183";
        }
        if (lower.equals("1 year") || lower.equals("1y") || lower.equals("12 months") || lower.equals("365")) {
            return "365";
        }
        if (lower.equals("3 years") || lower.equals("3 year") || lower.equals("3y") || lower.equals("1095")) {
            return "1095";
        }
        if (lower.equals("5 years") || lower.equals("5 year") || lower.equals("5y") || lower.equals("1825")) {
            return "1825";
        }
        if (lower.equals("7") || lower.equals("7 days")) {
            return "7";
        }
        if (lower.matches("\\d+")) {
            return lower;
        }
        return raw.replace(" Days", "").replace(" days", "").trim();
    }

    /** Regular paid tiers (not trial/demo short periods). */
    public static boolean isRegularTier(String labelOrDays) {
        String days = toDayCount(labelOrDays);
        return days.equals("183")
                || days.equals("365")
                || days.equals("1095")
                || days.equals("1825")
                || days.equals("10958")
                || "Lifetime".equalsIgnoreCase(labelOrDays != null ? labelOrDays.trim() : "");
    }

    public static String displayLabel(String storedDays) {
        if (storedDays == null) {
            return "";
        }
        String days = toDayCount(storedDays);
        switch (days) {
            case "7":
                return LABEL_7_DAYS;
            case "183":
                return LABEL_6_MONTHS;
            case "365":
                return LABEL_1_YEAR;
            case "1095":
                return LABEL_3_YEARS;
            case "1825":
                return LABEL_5_YEARS;
            case "10958":
                return LABEL_LIFETIME;
            default:
                if ("Lifetime".equalsIgnoreCase(storedDays.trim())) {
                    return LABEL_LIFETIME;
                }
                return storedDays.contains("Day") || storedDays.contains("Month") || storedDays.contains("Year")
                        || storedDays.equalsIgnoreCase("Lifetime")
                        ? storedDays
                        : storedDays + " Days";
        }
    }
}
