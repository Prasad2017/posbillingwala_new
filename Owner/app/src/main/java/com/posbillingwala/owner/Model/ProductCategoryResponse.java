package com.posbillingwala.owner.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ProductCategoryResponse {

    @SerializedName("categoryId")
    @Expose
    String categoryId;
    @SerializedName("categoryName")
    @Expose
    String categoryName;
    @SerializedName("categoryNetworkStatus")
    @Expose
    public String categoryNetworkStatus;


    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryNetworkStatus() {
        return categoryNetworkStatus;
    }

    public void setCategoryNetworkStatus(String categoryNetworkStatus) {
        this.categoryNetworkStatus = categoryNetworkStatus;
    }
}
