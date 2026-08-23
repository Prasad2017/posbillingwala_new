package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class InvoiceResponse {

    @SerializedName("invoiceId")
    @Expose
    public String invoiceId;
    @SerializedName("userId")
    @Expose
    public String userId;
    @SerializedName("noOfTable")
    @Expose
    public String noOfTable;
    @SerializedName("invoiceNumber")
    @Expose
    public String invoiceNumber;
    @SerializedName("customerName")
    @Expose
    public String customerName;
    @SerializedName("customerMobile")
    @Expose
    public String customerMobile;
    @SerializedName("customerEmail")
    @Expose
    public String customerEmail;
    @SerializedName("customerAddress")
    @Expose
    public String customerAddress;
    @SerializedName("subTotal")
    @Expose
    public String subTotal;
    @SerializedName("totalGSTAmount")
    @Expose
    public String totalGSTAmount;
    @SerializedName("discount")
    @Expose
    public String discount;
    @SerializedName("discountType")
    @Expose
    public String discountType;
    @SerializedName("totalAmount")
    @Expose
    public String totalAmount;
    @SerializedName("paymentMode")
    @Expose
    public String paymentMode;
    @SerializedName("invoiceDate")
    @Expose
    public String invoiceDate;
    @SerializedName("invoiceOrderStatus")
    @Expose
    public String invoiceOrderStatus;
    @SerializedName("invoiceType")
    @Expose
    public String invoiceType;
    @SerializedName("invoiceNetworkStatus")
    @Expose
    public String invoiceNetworkStatus;
    @SerializedName("invoiceStatus")
    @Expose
    public String invoiceStatus;
    @SerializedName("organizationId")
    @Expose
    public String organizationId;
    @SerializedName("branchId")
    @Expose
    public String branchId;
    @SerializedName("deviceId")
    @Expose
    public String deviceId;


    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNoOfTable() {
        return noOfTable;
    }

    public void setNoOfTable(String noOfTable) {
        this.noOfTable = noOfTable;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerMobile() {
        return customerMobile;
    }

    public void setCustomerMobile(String customerMobile) {
        this.customerMobile = customerMobile;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(String subTotal) {
        this.subTotal = subTotal;
    }

    public String getTotalGSTAmount() {
        return totalGSTAmount;
    }

    public void setTotalGSTAmount(String totalGSTAmount) {
        this.totalGSTAmount = totalGSTAmount;
    }

    public String getDiscount() {
        return discount;
    }

    public void setDiscount(String discount) {
        this.discount = discount;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getInvoiceOrderStatus() {
        return invoiceOrderStatus;
    }

    public void setInvoiceOrderStatus(String invoiceOrderStatus) {
        this.invoiceOrderStatus = invoiceOrderStatus;
    }

    public String getInvoiceNetworkStatus() {
        return invoiceNetworkStatus;
    }

    public void setInvoiceNetworkStatus(String invoiceNetworkStatus) {
        this.invoiceNetworkStatus = invoiceNetworkStatus;
    }

    public String getInvoiceStatus() {
        return invoiceStatus;
    }

    public void setInvoiceStatus(String invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
