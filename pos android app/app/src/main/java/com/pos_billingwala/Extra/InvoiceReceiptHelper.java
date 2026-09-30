package com.pos_billingwala.Extra;

import com.pos_billingwala.Model.PrinterSettingResponse;

import java.util.List;

/** Shared bill header helpers (sales invoice title on printed receipt). */
public final class InvoiceReceiptHelper {

    private InvoiceReceiptHelper() {
    }

    public static String salesInvoiceTitleFrom(List<PrinterSettingResponse> settings) {
        if (settings == null || settings.isEmpty()) {
            return "";
        }
        String title = settings.get(0).getInvoiceTitle();
        return title != null ? title.trim() : "";
    }

    /** Prepends a centered bold title line to HTML bill meta (shop block → title → bill no). */
    public static String prependSalesInvoiceTitle(String billDetailsHtml, String invoiceTitle) {
        if (billDetailsHtml == null) {
            billDetailsHtml = "";
        }
        if (invoiceTitle == null || invoiceTitle.trim().isEmpty()) {
            return billDetailsHtml;
        }
        String safe = invoiceTitle.trim()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return "<center><b>" + safe + "</b></center><br/>" + billDetailsHtml;
    }
}
