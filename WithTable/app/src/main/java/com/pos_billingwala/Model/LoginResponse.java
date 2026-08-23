package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("status")
    @Expose
    public String status;
    @SerializedName("message")
    @Expose
    public String message;
    @SerializedName("mpin")
    @Expose
    public String mpin;
    @SerializedName("licenceId")
    @Expose
    public String licenceId;
    @SerializedName("ownerId")
    @Expose
    public String ownerId;
    @SerializedName("userName")
    @Expose
    public String userName;
    @SerializedName("shopName")
    @Expose
    public String shopName;
    @SerializedName("shopImage")
    @Expose
    public String shopImage;
    @SerializedName("licenceKey")
    @Expose
    public String licenceKey;
    @SerializedName("licence_key_reg_date")
    @Expose
    public String licenceKeyRegDate;
    @SerializedName("licence_key_expire_date")
    @Expose
    public String licenceKeyExpireDate;
    @SerializedName("fastBilling")
    @Expose
    public String fastBilling;
    @SerializedName("takeAway")
    @Expose
    public String takeAway;
    @SerializedName("dineIn")
    @Expose
    public String dineIn;
    @SerializedName("mess")
    @Expose
    public String mess;
    @SerializedName("reportPin")
    @Expose
    public String reportPin;
    @SerializedName("totalSaleData")
    @Expose
    public String totalSaleData;
    @SerializedName("todaySaleData")
    @Expose
    public String todaySaleData;
    @SerializedName("licenseType")
    @Expose
    public String licenseType;
    @SerializedName("isTrial")
    @Expose
    public String isTrial;
    @SerializedName("trialDays")
    @Expose
    public String trialDays;
    @SerializedName("trialMaxBills")
    @Expose
    public String trialMaxBills;
    @SerializedName("trialBillCount")
    @Expose
    public String trialBillCount;
    @SerializedName("trialBillsRemaining")
    @Expose
    public String trialBillsRemaining;
    @SerializedName("authToken")
    @Expose
    public String authToken;
    @SerializedName("tokenExpiresAt")
    @Expose
    public String tokenExpiresAt;
    @SerializedName("licensePayload")
    @Expose
    public String licensePayload;
    @SerializedName("licenseSignature")
    @Expose
    public String licenseSignature;
    @SerializedName("organizationId")
    @Expose
    public String organizationId;
    @SerializedName("branchId")
    @Expose
    public String branchId;
    @SerializedName("branchLabel")
    @Expose
    public String branchLabel;
    @SerializedName("issuedAt")
    @Expose
    public String issuedAt;
    @SerializedName("offlineGraceUntil")
    @Expose
    public String offlineGraceUntil;
    @SerializedName("trialConsumed")
    @Expose
    public String trialConsumed;


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLicenceId() {
        return licenceId;
    }

    public void setLicenceId(String licenceId) {
        this.licenceId = licenceId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopImage() {
        return shopImage;
    }

    public void setShopImage(String shopImage) {
        this.shopImage = shopImage;
    }

    public String getLicenceKey() {
        return licenceKey;
    }

    public void setLicenceKey(String licenceKey) {
        this.licenceKey = licenceKey;
    }

    public String getLicenceKeyRegDate() {
        return licenceKeyRegDate;
    }

    public void setLicenceKeyRegDate(String licenceKeyRegDate) {
        this.licenceKeyRegDate = licenceKeyRegDate;
    }

    public String getLicenceKeyExpireDate() {
        return licenceKeyExpireDate;
    }

    public void setLicenceKeyExpireDate(String licenceKeyExpireDate) {
        this.licenceKeyExpireDate = licenceKeyExpireDate;
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

    public String getReportPin() {
        return reportPin;
    }

    public void setReportPin(String reportPin) {
        this.reportPin = reportPin;
    }

    public String getMess() {
        return mess;
    }

    public void setMess(String mess) {
        this.mess = mess;
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

    public String getMpin() {
        return mpin;
    }

    public void setMpin(String mpin) {
        this.mpin = mpin;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public String getIsTrial() {
        return isTrial;
    }

    public void setIsTrial(String isTrial) {
        this.isTrial = isTrial;
    }

    public String getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(String trialDays) {
        this.trialDays = trialDays;
    }

    public String getTrialMaxBills() {
        return trialMaxBills;
    }

    public void setTrialMaxBills(String trialMaxBills) {
        this.trialMaxBills = trialMaxBills;
    }

    public String getTrialBillCount() {
        return trialBillCount;
    }

    public void setTrialBillCount(String trialBillCount) {
        this.trialBillCount = trialBillCount;
    }

    public String getTrialBillsRemaining() {
        return trialBillsRemaining;
    }

    public void setTrialBillsRemaining(String trialBillsRemaining) {
        this.trialBillsRemaining = trialBillsRemaining;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(String tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public String getLicensePayload() {
        return licensePayload;
    }

    public void setLicensePayload(String licensePayload) {
        this.licensePayload = licensePayload;
    }

    public String getLicenseSignature() {
        return licenseSignature;
    }

    public void setLicenseSignature(String licenseSignature) {
        this.licenseSignature = licenseSignature;
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

    public String getBranchLabel() {
        return branchLabel;
    }

    public void setBranchLabel(String branchLabel) {
        this.branchLabel = branchLabel;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getOfflineGraceUntil() {
        return offlineGraceUntil;
    }

    public void setOfflineGraceUntil(String offlineGraceUntil) {
        this.offlineGraceUntil = offlineGraceUntil;
    }

    public String getTrialConsumed() {
        return trialConsumed;
    }

    public void setTrialConsumed(String trialConsumed) {
        this.trialConsumed = trialConsumed;
    }
}
