package com.posbillingwala.owner.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/** Cloud custom-order deposit row for Owner ledger. */
public class CustomOrderDepositResponse {

    @SerializedName("depositId")
    @Expose
    public String depositId;

    @SerializedName("localDepositId")
    @Expose
    public String localDepositId;

    @SerializedName("productName")
    @Expose
    public String productName;

    @SerializedName("orderNote")
    @Expose
    public String orderNote;

    @SerializedName("depositAmount")
    @Expose
    public String depositAmount;

    @SerializedName("dueDate")
    @Expose
    public String dueDate;

    @SerializedName("photoFile")
    @Expose
    public String photoFile;

    @SerializedName("depositStatus")
    @Expose
    public String depositStatus;

    @SerializedName("createdAt")
    @Expose
    public String createdAt;

    public String getDepositId() {
        return depositId != null ? depositId : "";
    }

    public String getLocalDepositId() {
        return localDepositId != null ? localDepositId : "";
    }

    public String getProductName() {
        return productName != null ? productName : "";
    }

    public String getOrderNote() {
        return orderNote != null ? orderNote : "";
    }

    public String getDepositAmount() {
        return depositAmount != null ? depositAmount : "";
    }

    public String getDueDate() {
        return dueDate != null ? dueDate : "";
    }

    public String getDepositStatus() {
        return depositStatus != null && !depositStatus.isEmpty() ? depositStatus : "open";
    }

    public String getCreatedAt() {
        return createdAt != null ? createdAt : "";
    }
}
