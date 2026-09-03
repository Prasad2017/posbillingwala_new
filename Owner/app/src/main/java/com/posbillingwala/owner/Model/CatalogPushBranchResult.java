package com.posbillingwala.owner.Model;

import com.google.gson.annotations.SerializedName;

public class CatalogPushBranchResult {

    @SerializedName("branchId")
    public String branchId;
    @SerializedName("branchLabel")
    public String branchLabel;
    @SerializedName("status")
    public String status;
    @SerializedName("message")
    public String message;
    @SerializedName("productsCopied")
    public int productsCopied;
    @SerializedName("productsUpdated")
    public int productsUpdated;
}
