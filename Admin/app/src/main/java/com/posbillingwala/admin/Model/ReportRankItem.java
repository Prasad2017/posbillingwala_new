package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ReportRankItem {
    @SerializedName("customerId") @Expose private String customerId;
    @SerializedName("customerName") @Expose private String customerName;
    @SerializedName("shopName") @Expose private String shopName;
    @SerializedName("totalSales") @Expose private String totalSales;
    @SerializedName("branchCount") @Expose private String branchCount;
    @SerializedName("deviceCount") @Expose private String deviceCount;
    @SerializedName("label") @Expose private String label;
    @SerializedName("count") @Expose private String count;
    @SerializedName("percent") @Expose private String percent;
    @SerializedName("name") @Expose private String name;
    @SerializedName("amount") @Expose private String amount;
    @SerializedName("date") @Expose private String date;
    @SerializedName("total") @Expose private String total;
    @SerializedName("dealerName") @Expose private String dealerName;
    @SerializedName("dealerId") @Expose private String dealerId;

    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getShopName() { return shopName; }
    public String getTotalSales() { return totalSales; }
    public String getBranchCount() { return branchCount; }
    public String getDeviceCount() { return deviceCount; }
    public String getLabel() { return label; }
    public String getCount() { return count; }
    public String getPercent() { return percent; }
    public String getName() { return name; }
    public String getAmount() { return amount; }
    public String getDate() { return date; }
    public String getTotal() { return total; }
    public String getDealerName() { return dealerName; }
    public String getDealerId() { return dealerId; }

    public String displayName() {
        if (shopName != null && !shopName.trim().isEmpty()) return shopName;
        if (customerName != null && !customerName.trim().isEmpty()) return customerName;
        if (dealerName != null && !dealerName.trim().isEmpty()) return dealerName;
        if (name != null && !name.trim().isEmpty()) return name;
        if (label != null) return label;
        return "—";
    }

    public String displayValue() {
        if (totalSales != null) return "₹ " + totalSales;
        if (amount != null) return "₹ " + amount;
        if (branchCount != null) return branchCount + " branches";
        if (deviceCount != null) return deviceCount + " devices";
        if (count != null) return count;
        if (total != null) return total;
        return "0";
    }
}
