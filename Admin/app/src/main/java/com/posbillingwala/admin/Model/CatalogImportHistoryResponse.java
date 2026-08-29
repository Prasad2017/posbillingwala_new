package com.posbillingwala.admin.Model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CatalogImportHistoryResponse {

    @SerializedName("success")
    private boolean success;
    @SerializedName("status")
    private String status;
    @SerializedName("history")
    private List<CatalogImportHistoryItem> history;

    public boolean isSuccess() {
        return success || (status != null && status.equalsIgnoreCase("true"));
    }

    public List<CatalogImportHistoryItem> getHistory() {
        return history;
    }
}
