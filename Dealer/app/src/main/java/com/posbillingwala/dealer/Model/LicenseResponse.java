package com.posbillingwala.dealer.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LicenseResponse {

    @SerializedName("licenses_id")
    @Expose
    private String licensesId;
    @SerializedName("companyAddress")
    @Expose
    private String companyAddress;
    @SerializedName("shopName1")
    @Expose
    private String shopName1;
    @SerializedName("phoneNo1")
    @Expose
    private String phoneNo1;
    @SerializedName("phoneNo2")
    @Expose
    private String phoneNo2;
    @SerializedName("licenseKey")
    @Expose
    private String licenseKey;
    @SerializedName("mpin")
    @Expose
    private String mpin;
    @SerializedName("licenseValidity")
    @Expose
    private String licenseValidity;
    @SerializedName("licenseType")
    @Expose
    private String licenseType;
    @SerializedName("licenseStatus")
    @Expose
    private String licenseStatus;
    @SerializedName("registrationDate")
    @Expose
    private String registrationDate;
    @SerializedName("expiryDate")
    @Expose
    private String expiryDate;
    @SerializedName("paymentStatus")
    @Expose
    private String paymentStatus;
    @SerializedName("amount")
    @Expose
    private String amount;
    @SerializedName("fastBilling")
    @Expose
    private String fastBilling;
    @SerializedName("takeAway")
    @Expose
    private String takeAway;
    @SerializedName("dineIn")
    @Expose
    private String dineIn;
    @SerializedName("mess")
    @Expose
    private String mess;
    @SerializedName("userType")
    @Expose
    private String userType;
    @SerializedName("userName")
    @Expose
    private String userName;
    @SerializedName("branchLabel")
    @Expose
    private String branchLabel;


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

    public String getMpin() {
        return mpin;
    }

    public void setMpin(String mpin) {
        this.mpin = mpin;
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

    public String getMess() {
        return mess;
    }

    public void setMess(String mess) {
        this.mess = mess;
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
