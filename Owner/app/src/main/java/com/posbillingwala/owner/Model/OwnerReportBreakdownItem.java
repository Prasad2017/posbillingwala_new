package com.posbillingwala.owner.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class OwnerReportBreakdownItem {

    @SerializedName("label")
    @Expose
    private String label;

    @SerializedName("amount")
    @Expose
    private String amount;

    @SerializedName("count")
    @Expose
    private String count;

    public String getLabel() {
        return label;
    }

    public String getAmount() {
        return amount;
    }

    public String getCount() {
        return count;
    }
}
