package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ProductCategoryResponse {

    @SerializedName("categoryNetworkStatus")
    @Expose
    public String categoryNetworkStatus;
    @SerializedName("categoryDeletedStatus")
    @Expose
    public String categoryDeletedStatus;
    @SerializedName("foodTypeId")
    @Expose
    public String foodTypeId;
    @SerializedName("foodTypeCode")
    @Expose
    public String foodTypeCode;
    @SerializedName("categoryId")
    @Expose
    String categoryId;
    @SerializedName("categoryName")
    @Expose
    String categoryName;

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

    public String getCategoryDeletedStatus() {
        return categoryDeletedStatus;
    }

    public void setCategoryDeletedStatus(String categoryDeletedStatus) {
        this.categoryDeletedStatus = categoryDeletedStatus;
    }

    public String getFoodTypeId() {
        return foodTypeId;
    }

    public void setFoodTypeId(String foodTypeId) {
        this.foodTypeId = foodTypeId;
    }

    public String getFoodTypeCode() {
        return foodTypeCode;
    }

    public void setFoodTypeCode(String foodTypeCode) {
        this.foodTypeCode = foodTypeCode;
    }
}
