package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ProductSubcategoryResponse {

    @SerializedName("subcategoryId")
    @Expose
    private String subcategoryId;
    @SerializedName("categoryId")
    @Expose
    private String categoryId;
    @SerializedName("subcategoryName")
    @Expose
    private String subcategoryName;
    @SerializedName("subcategoryNetworkStatus")
    @Expose
    private String subcategoryNetworkStatus;

    public String getSubcategoryId() {
        return subcategoryId;
    }

    public void setSubcategoryId(String subcategoryId) {
        this.subcategoryId = subcategoryId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getSubcategoryName() {
        return subcategoryName;
    }

    public void setSubcategoryName(String subcategoryName) {
        this.subcategoryName = subcategoryName;
    }

    public String getSubcategoryNetworkStatus() {
        return subcategoryNetworkStatus;
    }

    public void setSubcategoryNetworkStatus(String subcategoryNetworkStatus) {
        this.subcategoryNetworkStatus = subcategoryNetworkStatus;
    }
}
