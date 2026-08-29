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

    // Local-only transport / device capability fields (offline printer upgrade)
    public String billConnectionType;
    public String kotConnectionType;
    public String billPrinterIp;
    public String kotPrinterIp;
    public String billPrinterPort;
    public String kotPrinterPort;
    public String billUsbDeviceKey;
    public String kotUsbDeviceKey;
    public String supportsCutter;
    public String supportsCashDrawer;
    public String autoCut;
    public String autoOpenCashDrawer;
    public String drawerOpenMode;
    public String drawerPin;
    public String drawerPulseOn;
    public String drawerPulseOff;
    public String cutCommand;
    public String printerModel;

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

    public String getBillConnectionType() {
        return billConnectionType;
    }

    public void setBillConnectionType(String billConnectionType) {
        this.billConnectionType = billConnectionType;
    }

    public String getKotConnectionType() {
        return kotConnectionType;
    }

    public void setKotConnectionType(String kotConnectionType) {
        this.kotConnectionType = kotConnectionType;
    }

    public String getBillPrinterIp() {
        return billPrinterIp;
    }

    public void setBillPrinterIp(String billPrinterIp) {
        this.billPrinterIp = billPrinterIp;
    }

    public String getKotPrinterIp() {
        return kotPrinterIp;
    }

    public void setKotPrinterIp(String kotPrinterIp) {
        this.kotPrinterIp = kotPrinterIp;
    }

    public String getBillPrinterPort() {
        return billPrinterPort;
    }

    public void setBillPrinterPort(String billPrinterPort) {
        this.billPrinterPort = billPrinterPort;
    }

    public String getKotPrinterPort() {
        return kotPrinterPort;
    }

    public void setKotPrinterPort(String kotPrinterPort) {
        this.kotPrinterPort = kotPrinterPort;
    }

    public String getBillUsbDeviceKey() {
        return billUsbDeviceKey;
    }

    public void setBillUsbDeviceKey(String billUsbDeviceKey) {
        this.billUsbDeviceKey = billUsbDeviceKey;
    }

    public String getKotUsbDeviceKey() {
        return kotUsbDeviceKey;
    }

    public void setKotUsbDeviceKey(String kotUsbDeviceKey) {
        this.kotUsbDeviceKey = kotUsbDeviceKey;
    }

    public String getSupportsCutter() {
        return supportsCutter;
    }

    public void setSupportsCutter(String supportsCutter) {
        this.supportsCutter = supportsCutter;
    }

    public String getSupportsCashDrawer() {
        return supportsCashDrawer;
    }

    public void setSupportsCashDrawer(String supportsCashDrawer) {
        this.supportsCashDrawer = supportsCashDrawer;
    }

    public String getAutoCut() {
        return autoCut;
    }

    public void setAutoCut(String autoCut) {
        this.autoCut = autoCut;
    }

    public String getAutoOpenCashDrawer() {
        return autoOpenCashDrawer;
    }

    public void setAutoOpenCashDrawer(String autoOpenCashDrawer) {
        this.autoOpenCashDrawer = autoOpenCashDrawer;
    }

    public String getDrawerOpenMode() {
        return drawerOpenMode;
    }

    public void setDrawerOpenMode(String drawerOpenMode) {
        this.drawerOpenMode = drawerOpenMode;
    }

    public String getDrawerPin() {
        return drawerPin;
    }

    public void setDrawerPin(String drawerPin) {
        this.drawerPin = drawerPin;
    }

    public String getDrawerPulseOn() {
        return drawerPulseOn;
    }

    public void setDrawerPulseOn(String drawerPulseOn) {
        this.drawerPulseOn = drawerPulseOn;
    }

    public String getDrawerPulseOff() {
        return drawerPulseOff;
    }

    public void setDrawerPulseOff(String drawerPulseOff) {
        this.drawerPulseOff = drawerPulseOff;
    }

    public String getCutCommand() {
        return cutCommand;
    }

    public void setCutCommand(String cutCommand) {
        this.cutCommand = cutCommand;
    }

    public String getPrinterModel() {
        return printerModel;
    }

    public void setPrinterModel(String printerModel) {
        this.printerModel = printerModel;
    }
}
