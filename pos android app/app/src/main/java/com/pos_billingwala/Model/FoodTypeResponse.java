package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * First-class catalog parent: Food Type → Category → (optional Subcategory) → Product → Portion.
 */
public class FoodTypeResponse {

    public static final String CODE_FOOD = "food";
    public static final String CODE_BEVERAGE = "beverage";

    @SerializedName("foodTypeId")
    @Expose
    private String foodTypeId;
    @SerializedName("foodTypeName")
    @Expose
    private String foodTypeName;
    @SerializedName("foodTypeCode")
    @Expose
    private String foodTypeCode;
    @SerializedName("foodTypeSortOrder")
    @Expose
    private String foodTypeSortOrder;
    @SerializedName("foodTypeStatus")
    @Expose
    private String foodTypeStatus;

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

    public String getFoodTypeSortOrder() {
        return foodTypeSortOrder;
    }

    public void setFoodTypeSortOrder(String foodTypeSortOrder) {
        this.foodTypeSortOrder = foodTypeSortOrder;
    }

    public String getFoodTypeStatus() {
        return foodTypeStatus;
    }

    public void setFoodTypeStatus(String foodTypeStatus) {
        this.foodTypeStatus = foodTypeStatus;
    }
}
