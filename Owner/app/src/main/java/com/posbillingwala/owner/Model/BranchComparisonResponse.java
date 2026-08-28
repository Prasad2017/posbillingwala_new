package com.posbillingwala.owner.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class BranchComparisonResponse {

    @SerializedName("branchId")
    @Expose
    public String branchId;
    @SerializedName("organizationId")
    @Expose
    public String organizationId;
    @SerializedName("branchLabel")
    @Expose
    public String branchLabel;
    @SerializedName("userType")
    @Expose
    public String userType;
    @SerializedName("totalSale")
    @Expose
    public String totalSale;
    @SerializedName("todaySale")
    @Expose
    public String todaySale;
    @SerializedName("billCount")
    @Expose
    public String billCount;
    @SerializedName("todayBillCount")
    @Expose
    public String todayBillCount;
    @SerializedName("avgBillAmount")
    @Expose
    public String avgBillAmount;
    @SerializedName("deviceBound")
    @Expose
    public String deviceBound;
    @SerializedName("androidDeviceName")
    @Expose
    public String androidDeviceName;
    @SerializedName("currencyName")
    @Expose
    public String currencyName;
    @SerializedName("shopName1")
    @Expose
    public String shopName1;
    @SerializedName("companyName")
    @Expose
    public String companyName;

    public String getShopName1() {
        if (shopName1 != null && !shopName1.trim().isEmpty()) {
            return shopName1;
        }
        return companyName;
    }

    public String getBranchLabel() {
        return branchLabel;
    }

    public String getTotalSale() {
        return totalSale;
    }

    public String getTodaySale() {
        return todaySale;
    }

    public String getBillCount() {
        return billCount;
    }

    public String getTodayBillCount() {
        return todayBillCount;
    }

    public String getAvgBillAmount() {
        return avgBillAmount;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getDeviceBound() {
        return deviceBound;
    }

    public String getAndroidDeviceName() {
        return androidDeviceName;
    }
}
