package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PrinterSettingResponse {

    @SerializedName("settingId")
    @Expose
    public String settingId;
    @SerializedName("printerName")
    @Expose
    public String printerName;
    @SerializedName("invoicePrefix")
    @Expose
    public String invoicePrefix;
    @SerializedName("invoiceTitle")
    @Expose
    public String invoiceTitle;
    @SerializedName("invoiceTermsCondition")
    @Expose
    public String invoiceTermsCondition;
    @SerializedName("logoUse")
    @Expose
    public String logoUse;
    @SerializedName("paymentUse")
    @Expose
    public String paymentUse;
    @SerializedName("customerUse")
    @Expose
    public String customerUse;
    @SerializedName("productQuantityUpdate")
    @Expose
    public String productQuantityUpdate;
    @SerializedName("duplicateBillUse")
    @Expose
    public String duplicateBillUse;
    @SerializedName("settingStatus")
    @Expose
    public String settingStatus;
    @SerializedName("bluetoothAddress")
    @Expose
    public String bluetoothAddress;
    @SerializedName("bluetoothKOTAddress")
    @Expose
    public String bluetoothKOTAddress;
    @SerializedName("KOTPrinterName")
    @Expose
    public String KOTPrinterName;
    @SerializedName("printerFeedLines")
    @Expose
    public String printerFeedLines;
    @SerializedName("KotPrinterFeedLines")
    @Expose
    public String KotPrinterFeedLines;


    public String getSettingId() {
        return settingId;
    }

    public void setSettingId(String settingId) {
        this.settingId = settingId;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public String getInvoicePrefix() {
        return invoicePrefix;
    }

    public void setInvoicePrefix(String invoicePrefix) {
        this.invoicePrefix = invoicePrefix;
    }

    public String getInvoiceTitle() {
        return invoiceTitle;
    }

    public void setInvoiceTitle(String invoiceTitle) {
        this.invoiceTitle = invoiceTitle;
    }

    public String getInvoiceTermsCondition() {
        return invoiceTermsCondition;
    }

    public void setInvoiceTermsCondition(String invoiceTermsCondition) {
        this.invoiceTermsCondition = invoiceTermsCondition;
    }

    public String getLogoUse() {
        return logoUse;
    }

    public void setLogoUse(String logoUse) {
        this.logoUse = logoUse;
    }

    public String getSettingStatus() {
        return settingStatus;
    }

    public void setSettingStatus(String settingStatus) {
        this.settingStatus = settingStatus;
    }

    public String getPaymentUse() {
        return paymentUse;
    }

    public void setPaymentUse(String paymentUse) {
        this.paymentUse = paymentUse;
    }

    public String getProductQuantityUpdate() {
        return productQuantityUpdate;
    }

    public void setProductQuantityUpdate(String productQuantityUpdate) {
        this.productQuantityUpdate = productQuantityUpdate;
    }

    public String getDuplicateBillUse() {
        return duplicateBillUse;
    }

    public void setDuplicateBillUse(String duplicateBillUse) {
        this.duplicateBillUse = duplicateBillUse;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    public String getBluetoothKOTAddress() {
        return bluetoothKOTAddress;
    }

    public void setBluetoothKOTAddress(String bluetoothKOTAddress) {
        this.bluetoothKOTAddress = bluetoothKOTAddress;
    }

    public String getKOTPrinterName() {
        return KOTPrinterName;
    }

    public void setKOTPrinterName(String KOTPrinterName) {
        this.KOTPrinterName = KOTPrinterName;
    }

    public String getCustomerUse() {
        return customerUse;
    }

    public void setCustomerUse(String customerUse) {
        this.customerUse = customerUse;
    }

    public String getPrinterFeedLines() {
        return printerFeedLines;
    }

    public void setPrinterFeedLines(String printerFeedLines) {
        this.printerFeedLines = printerFeedLines;
    }

    public String getKotPrinterFeedLines() {
        return KotPrinterFeedLines;
    }

    public void setKotPrinterFeedLines(String kotPrinterFeedLines) {
        KotPrinterFeedLines = kotPrinterFeedLines;
    }
}
