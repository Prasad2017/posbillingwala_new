package com.posbillingwala.owner.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LicenseResponse {

    @SerializedName("licenses_id")
    @Expose
    public String licensesId;
    @SerializedName("companyAddress")
    @Expose
    public String companyAddress;
    @SerializedName("shopName1")
    @Expose
    public String shopName1;
    @SerializedName("shopName2")
    @Expose
    public String shopName2;
    @SerializedName("phoneNo1")
    @Expose
    public String phoneNo1;
    @SerializedName("phoneNo2")
    @Expose
    public String phoneNo2;
    @SerializedName("licenseKey")
    @Expose
    public String licenseKey;
    @SerializedName("androidDeviceName")
    @Expose
    public String androidDeviceName;
    @SerializedName("androidDeviceId")
    @Expose
    public String androidDeviceId;
    @SerializedName("licenseValidity")
    @Expose
    public String licenseValidity;
    @SerializedName("licenseType")
    @Expose
    public String licenseType;
    @SerializedName("licenseStatus")
    @Expose
    public String licenseStatus;
    @SerializedName("registrationDate")
    @Expose
    public String registrationDate;
    @SerializedName("expiryDate")
    @Expose
    public String expiryDate;
    @SerializedName("paymentStatus")
    @Expose
    public String paymentStatus;
    @SerializedName("amount")
    @Expose
    public String amount;
    @SerializedName("fastBilling")
    @Expose
    public String fastBilling;
    @SerializedName("takeAway")
    @Expose
    public String takeAway;
    @SerializedName("dineIn")
    @Expose
    public String dineIn;

    @SerializedName("totalSale")
    @Expose
    public String totalSale;
    @SerializedName("todaySale")
    @Expose
    public String todaySale;
    @SerializedName("currencyName")
    @Expose
    public String currencyName;
    @SerializedName("totalSaleData")
    @Expose
    public String totalSaleData;
    @SerializedName("todaySaleData")
    @Expose
    public String todaySaleData;
    @SerializedName("userType")
    @Expose
    public String userType;
    @SerializedName("userName")
    @Expose
    public String userName;
    @SerializedName("branchLabel")
    @Expose
    public String branchLabel;


    public String getLicensesId() {
        return licensesId;
    }

    public void setLicensesId(String licensesId) {
        this.licensesId = licensesId;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public String getShopName1() {
        return shopName1;
    }

    public void setShopName1(String shopName1) {
        this.shopName1 = shopName1;
    }

    public String getShopName2() {
        return shopName2;
    }

    public void setShopName2(String shopName2) {
        this.shopName2 = shopName2;
    }

    public String getPhoneNo1() {
        return phoneNo1;
    }

    public void setPhoneNo1(String phoneNo1) {
        this.phoneNo1 = phoneNo1;
    }

    public String getPhoneNo2() {
        return phoneNo2;
    }

    public void setPhoneNo2(String phoneNo2) {
        this.phoneNo2 = phoneNo2;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public String getLicenseValidity() {
        return licenseValidity;
    }

    public void setLicenseValidity(String licenseValidity) {
        this.licenseValidity = licenseValidity;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public String getLicenseStatus() {
        return licenseStatus;
    }

    public void setLicenseStatus(String licenseStatus) {
        this.licenseStatus = licenseStatus;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getFastBilling() {
        return fastBilling;
    }

    public void setFastBilling(String fastBilling) {
        this.fastBilling = fastBilling;
    }

    public String getTakeAway() {
        return takeAway;
    }

    public void setTakeAway(String takeAway) {
        this.takeAway = takeAway;
    }

    public String getDineIn() {
        return dineIn;
    }

    public void setDineIn(String dineIn) {
        this.dineIn = dineIn;
    }

    public String getTotalSale() {
        return totalSale;
    }

    public void setTotalSale(String totalSale) {
        this.totalSale = totalSale;
    }

    public String getTodaySale() {
        return todaySale;
    }

    public void setTodaySale(String todaySale) {
        this.todaySale = todaySale;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    public String getAndroidDeviceName() {
        return androidDeviceName;
    }

    public void setAndroidDeviceName(String androidDeviceName) {
        this.androidDeviceName = androidDeviceName;
    }

    public String getAndroidDeviceId() {
        return androidDeviceId;
    }

    public void setAndroidDeviceId(String androidDeviceId) {
        this.androidDeviceId = androidDeviceId;
    }

    public String getTotalSaleData() {
        return totalSaleData;
    }

    public void setTotalSaleData(String totalSaleData) {
        this.totalSaleData = totalSaleData;
    }

    public String getTodaySaleData() {
        return todaySaleData;
    }

    public void setTodaySaleData(String todaySaleData) {
        this.todaySaleData = todaySaleData;
    }

    public String getUserType() {
        return userType;
    }

    public String getUserName() {
        return userName;
    }

    public String getBranchLabel() {
        return branchLabel;
    }
}
