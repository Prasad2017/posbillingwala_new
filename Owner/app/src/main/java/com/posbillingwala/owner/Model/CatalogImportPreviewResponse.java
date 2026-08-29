package com.posbillingwala.owner.Model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CatalogImportPreviewResponse {

    private boolean success;
    private String status;
    private String importSessionId;
    private String customerId;
    private String customerName;
    private CatalogImportSummary summary;
    private List<CatalogImportError> errors;
    private String expiresAt;
    private String message;
    private String code;

    public boolean isSuccess() {
        return success || (status != null && status.equalsIgnoreCase("true"));
    }

    public String getStatus() {
        return status;
    }

    public String getImportSessionId() {
        return importSessionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public CatalogImportSummary getSummary() {
        return summary;
    }

    public List<CatalogImportError> getErrors() {
        return errors;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }
}
