package com.posbillingwala.dealer.Model;

import com.google.gson.annotations.SerializedName;

public class CatalogImportHistoryItem {

    @SerializedName("sessionId")
    private String sessionId;
    @SerializedName("customerId")
    private String customerId;
    @SerializedName("importType")
    private String importType;
    @SerializedName("fileName")
    private String fileName;
    @SerializedName("totalRows")
    private String totalRows;
    @SerializedName("validRows")
    private String validRows;
    @SerializedName("createdCount")
    private String createdCount;
    @SerializedName("updatedCount")
    private String updatedCount;
    @SerializedName("failedCount")
    private String failedCount;
    @SerializedName("errorRows")
    private String errorRows;
    @SerializedName("status")
    private String status;
    @SerializedName("confirmedAt")
    private String confirmedAt;
    @SerializedName("created_at")
    private String createdAt;

    public String getSessionId() {
        return sessionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getImportType() {
        return importType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTotalRows() {
        return totalRows;
    }

    public String getValidRows() {
        return validRows;
    }

    public String getCreatedCount() {
        return createdCount;
    }

    public String getUpdatedCount() {
        return updatedCount;
    }

    public String getFailedCount() {
        return failedCount;
    }

    public String getErrorRows() {
        return errorRows;
    }

    public String getStatus() {
        return status;
    }

    public String getConfirmedAt() {
        return confirmedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
