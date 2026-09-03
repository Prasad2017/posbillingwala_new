package com.posbillingwala.owner.Model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CatalogPushResponse {

    @SerializedName("status")
    public String status;
    @SerializedName("message")
    public String message;
    @SerializedName("sourceLabel")
    public String sourceLabel;
    @SerializedName("sourceProductCount")
    public String sourceProductCount;
    @SerializedName("outletsUpdated")
    public String outletsUpdated;
    @SerializedName("branchResults")
    public List<CatalogPushBranchResult> branchResults;

    public boolean isSuccess() {
        return "1".equals(status) || "true".equalsIgnoreCase(status);
    }
}
