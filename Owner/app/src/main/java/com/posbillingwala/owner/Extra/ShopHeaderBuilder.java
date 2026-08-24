package com.posbillingwala.owner.Extra;

import com.posbillingwala.owner.Model.InvoiceProductResponse;

/**
 * Builds invoice shop header from structured Store Details.
 * Skips blank lines; falls back to legacy companyName / companyAddress / companyMobile.
 */
public final class ShopHeaderBuilder {

    private ShopHeaderBuilder() {
    }

    public static String resolveShopName1(InvoiceProductResponse invoice) {
        if (invoice == null) {
            return "";
        }
        return firstNonEmpty(invoice.getShopName1(), invoice.getCompanyName());
    }

    public static String buildShopDetailsBlock(InvoiceProductResponse invoice) {
        if (invoice == null) {
            return "";
        }
        StringBuilder details = new StringBuilder();
        appendLine(details, safeTrim(invoice.getShopName2()));
        appendLine(details, resolveAddressLine1(invoice));
        appendLine(details, safeTrim(invoice.getAddressLine2()));
        appendLine(details, safeTrim(invoice.getAddressLine3()));
        appendLine(details, firstNonEmpty(invoice.getPhoneNo1(), invoice.getCompanyMobile()));
        appendLine(details, safeTrim(invoice.getPhoneNo2()));
        if (invoice.getGstStatus() != null && invoice.getGstStatus().equalsIgnoreCase("on")) {
            String gst = safeTrim(invoice.getGstNumber());
            if (!gst.isEmpty()) {
                appendLine(details, "GSTIN: " + gst);
            }
        }
        String fssai = safeTrim(invoice.getCompanyFssis());
        if (!fssai.isEmpty()) {
            appendLine(details, "FSSAI No: " + fssai);
        }
        return details.toString();
    }

    private static String resolveAddressLine1(InvoiceProductResponse invoice) {
        String line1 = safeTrim(invoice.getAddressLine1());
        if (!line1.isEmpty()) {
            return line1;
        }
        boolean hasStructured = !safeTrim(invoice.getAddressLine2()).isEmpty()
                || !safeTrim(invoice.getAddressLine3()).isEmpty();
        if (hasStructured) {
            return "";
        }
        return safeTrim(invoice.getCompanyAddress());
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
