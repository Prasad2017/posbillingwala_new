package com.posbillingwala.owner.Model;

import com.google.gson.annotations.SerializedName;

public class CatalogImportSummary {

    private int total;
    private int valid;
    @SerializedName("new")
    private int newCount;
    private int updated;
    private int errors;
    private int created;
    private int failed;

    public int getTotal() {
        return total;
    }

    public int getValid() {
        return valid;
    }

    public int getNewCount() {
        return newCount;
    }

    public int getUpdated() {
        return updated;
    }

    public int getErrors() {
        return errors;
    }

    public int getCreated() {
        return created;
    }

    public int getFailed() {
        return failed;
    }
}
