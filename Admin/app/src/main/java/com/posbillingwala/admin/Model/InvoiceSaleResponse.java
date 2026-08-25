package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class InvoiceSaleResponse {
    @SerializedName("invoiceId")
    @Expose
    private String invoiceId;
    @SerializedName("invoiceNumber")
    @Expose
    private String invoiceNumber;
    @SerializedName("invoiceDate")
    @Expose
    private String invoiceDate;
    @SerializedName("invoiceType")
    @Expose
    private String invoiceType;
    @SerializedName("customerName")
    @Expose
    private String customerName;
    @SerializedName("totalAmount")
    @Expose
    private String totalAmount;
    @SerializedName("discount")
    @Expose
    private String discount;
    @SerializedName("totalGSTAmount")
    @Expose
    private String totalGSTAmount;
    @SerializedName("paymentMode")
    @Expose
    private String paymentMode;
    @SerializedName("branchName")
    @Expose
    private String branchName;
    @SerializedName("licenseKey")
    @Expose
    private String licenseKey;

    public String getInvoiceId() {
        return invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public String getDiscount() {
        return discount;
    }

    public String getTotalGSTAmount() {
        return totalGSTAmount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getLicenseKey() {
        return licenseKey;
    }
}
