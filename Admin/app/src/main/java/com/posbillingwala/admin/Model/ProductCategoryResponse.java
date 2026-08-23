package com.posbillingwala.admin.Model;

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
    private String categoryNetworkStatus;
    @SerializedName("foodTypeId")
    @Expose
    private String foodTypeId;
    @SerializedName("foodTypeName")
    @Expose
    private String foodTypeName;
    @SerializedName("foodTypeCode")
    @Expose
    private String foodTypeCode;


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

    public String getFoodTypeId() {
        return foodTypeId;
    }

    public void setFoodTypeId(String foodTypeId) {
        this.foodTypeId = foodTypeId;
    }

    public String getFoodTypeName() {
        return foodTypeName;
    }

    public void setFoodTypeName(String foodTypeName) {
        this.foodTypeName = foodTypeName;
    }

    public String getFoodTypeCode() {
        return foodTypeCode;
    }

    public void setFoodTypeCode(String foodTypeCode) {
        this.foodTypeCode = foodTypeCode;
    }
}
