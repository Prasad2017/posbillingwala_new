package com.pos_billingwala.Extra;

import com.pos_billingwala.Model.CompanyResponse;

/**
 * Builds bill header shop lines from structured Store Details.
 * Skips null/blank fields so printers never get empty placeholder lines.
 * Falls back to legacy companyName / companyAddress / companyMobile when needed.
 */
public final class ShopHeaderBuilder {

    private ShopHeaderBuilder() {
    }

    public static String resolveShopName1(CompanyResponse company) {
        if (company == null) {
            return "";
        }
        return firstNonEmpty(company.getShopName1(), company.getCompanyName());
    }

    public static String resolveShopName2(CompanyResponse company) {
        if (company == null) {
            return "";
        }
        return safeTrim(company.getShopName2());
    }

    public static String resolvePhone1(CompanyResponse company) {
        if (company == null) {
            return "";
        }
        return firstNonEmpty(company.getPhoneNo1(), company.getCompanyMobile());
    }

    public static String resolvePhone2(CompanyResponse company) {
        if (company == null) {
            return "";
        }
        return safeTrim(company.getPhoneNo2());
    }

    public static String resolveAddressLine1(CompanyResponse company) {
        if (company == null) {
            return "";
        }
        String line1 = safeTrim(company.getAddressLine1());
        if (!line1.isEmpty()) {
            return line1;
        }
        // Legacy single address only when no structured lines exist
        if (hasAnyStructuredAddress(company)) {
            return "";
        }
        return safeTrim(company.getCompanyAddress());
    }

    public static String resolveAddressLine2(CompanyResponse company) {
        if (company == null) {
            return "";
        }
        return safeTrim(company.getAddressLine2());
    }

    public static String resolveAddressLine3(CompanyResponse company) {
        if (company == null) {
            return "";
        }
        return safeTrim(company.getAddressLine3());
    }

    /**
     * Bill details block below the primary shop name: optional shop name 2, address lines,
     * phones, then GSTIN / FSSAI when requested. Empty fields are omitted entirely.
     */
    public static String buildShopDetailsBlock(CompanyResponse company,
                                               boolean includeShopName2,
                                               boolean includePhones,
                                               boolean includeGst,
                                               boolean includeFssai) {
        if (company == null) {
            return "";
        }
        StringBuilder details = new StringBuilder();
        if (includeShopName2) {
            appendLine(details, resolveShopName2(company));
        }
        appendLine(details, resolveAddressLine1(company));
        appendLine(details, resolveAddressLine2(company));
        appendLine(details, resolveAddressLine3(company));
        if (includePhones) {
            appendLine(details, resolvePhone1(company));
            appendLine(details, resolvePhone2(company));
        }
        if (includeGst && isGstOn(company)) {
            String gst = safeTrim(company.getGstNumber());
            if (!gst.isEmpty()) {
                appendLine(details, "GSTIN: " + gst);
            }
        }
        if (includeFssai) {
            String fssai = safeTrim(company.getCompanyFssis());
            if (!fssai.isEmpty()) {
                appendLine(details, "FSSAI No: " + fssai);
            }
        }
        return details.toString();
    }

    /** Default bill header details: name2 + addresses + phones + GST + FSSAI. */
    public static String buildShopDetailsBlock(CompanyResponse company) {
        return buildShopDetailsBlock(company, true, true, true, true);
    }

    /** Compose legacy companyAddress from structured lines (non-empty only). */
    public static String composeLegacyAddress(String addressLine1, String addressLine2, String addressLine3) {
        StringBuilder composed = new StringBuilder();
        appendLine(composed, safeTrim(addressLine1));
        appendLine(composed, safeTrim(addressLine2));
        appendLine(composed, safeTrim(addressLine3));
        return composed.toString();
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean hasAnyStructuredAddress(CompanyResponse company) {
        return !safeTrim(company.getAddressLine1()).isEmpty()
                || !safeTrim(company.getAddressLine2()).isEmpty()
                || !safeTrim(company.getAddressLine3()).isEmpty();
    }

    private static boolean isGstOn(CompanyResponse company) {
        String status = company.getGstStatus();
        return status != null && status.equalsIgnoreCase("on");
    }

    private static void appendLine(StringBuilder builder, String line) {
        if (line == null || line.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(line);
    }

    private static String firstNonEmpty(String primary, String fallback) {
        String first = safeTrim(primary);
        if (!first.isEmpty()) {
            return first;
        }
        return safeTrim(fallback);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
