package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class FoodTypeResponse {

    @SerializedName("foodTypeId")
    @Expose
    private String foodTypeId;
    @SerializedName("foodTypeName")
    @Expose
    private String foodTypeName;
    @SerializedName("foodTypeCode")
    @Expose
    private String foodTypeCode;

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
