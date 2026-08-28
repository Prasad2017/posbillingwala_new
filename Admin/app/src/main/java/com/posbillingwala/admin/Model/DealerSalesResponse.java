package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DealerSalesResponse {

    @SerializedName("dealerId")
    @Expose
    private String dealerId;

    @SerializedName("dealerName")
    @Expose
    private String dealerName;

    @SerializedName("totalCustomer")
    @Expose
    private String totalCustomer;

    @SerializedName("activeLicenses")
    @Expose
    private String activeLicenses;

    @SerializedName("totalSales")
    @Expose
    private String totalSales;

    public String getDealerId() {
        return dealerId;
    }

    public String getDealerName() {
        return dealerName;
    }

    public String getTotalCustomer() {
        return totalCustomer;
    }

    public String getActiveLicenses() {
        return activeLicenses;
    }

    public String getTotalSales() {
        return totalSales;
    }

    public String shortLabel() {
        String name = dealerName != null ? dealerName.trim() : "Dealer";
        if (name.length() <= 12) {
            return name;
        }
        return name.substring(0, 11) + "…";
    }
}
