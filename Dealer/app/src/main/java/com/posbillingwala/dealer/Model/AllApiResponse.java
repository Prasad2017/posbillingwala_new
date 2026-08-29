package com.posbillingwala.dealer.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AllApiResponse {


    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("userId")
    @Expose
    private String userId;
    @SerializedName("authToken")
    @Expose
    private String authToken;
    @SerializedName("tokenExpiresAt")
    @Expose
    private String tokenExpiresAt;
    @SerializedName("customerResponse")
    @Expose
    private List<CustomerResponse> customerResponseList;
    @SerializedName("categoryResponse")
    @Expose
    private List<ProductCategoryResponse> productCategoryResponseList;
    @SerializedName("productResponse")
    @Expose
    private List<ProductResponse> productResponseList;
    @SerializedName("foodTypeResponse")
    @Expose
    private List<FoodTypeResponse> foodTypeResponseList;
    @SerializedName("subcategoryResponse")
    @Expose
    private List<ProductSubcategoryResponse> subcategoryResponseList;
    @SerializedName("portionResponse")
    @Expose
    private List<ProductPortionResponse> portionResponseList;
    @SerializedName("portionMasterResponse")
    @Expose
    private List<PortionMasterResponse> portionMasterResponseList;
    @SerializedName("productId")
    @Expose
    private String productId;
    @SerializedName("portionMasterId")
    @Expose
    private String portionMasterId;
    @SerializedName("totalCustomer")
    @Expose
    private String totalCustomer;
    @SerializedName("activeCustomer")
    @Expose
    private String activeCustomer;
    @SerializedName("trialCustomer")
    @Expose
    private String trialCustomer;
    @SerializedName("expiredCustomer")
    @Expose
    private String expiredCustomer;
    @SerializedName("activePercent")
    @Expose
    private String activePercent;
    @SerializedName("trialPercent")
    @Expose
    private String trialPercent;
    @SerializedName("expiredPercent")
    @Expose
    private String expiredPercent;


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(String tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public List<CustomerResponse> getCustomerResponseList() {
        return customerResponseList;
    }

    public void setCustomerResponseList(List<CustomerResponse> customerResponseList) {
        this.customerResponseList = customerResponseList;
    }

    public List<ProductCategoryResponse> getProductCategoryResponseList() {
        return productCategoryResponseList;
    }

    public void setProductCategoryResponseList(List<ProductCategoryResponse> productCategoryResponseList) {
        this.productCategoryResponseList = productCategoryResponseList;
    }

    public List<ProductResponse> getProductResponseList() {
        return productResponseList;
    }

    public void setProductResponseList(List<ProductResponse> productResponseList) {
        this.productResponseList = productResponseList;
    }

    public List<FoodTypeResponse> getFoodTypeResponseList() {
        return foodTypeResponseList;
    }

    public void setFoodTypeResponseList(List<FoodTypeResponse> foodTypeResponseList) {
        this.foodTypeResponseList = foodTypeResponseList;
    }

    public List<ProductSubcategoryResponse> getSubcategoryResponseList() {
        return subcategoryResponseList;
    }

    public void setSubcategoryResponseList(List<ProductSubcategoryResponse> subcategoryResponseList) {
        this.subcategoryResponseList = subcategoryResponseList;
    }

    public List<ProductPortionResponse> getPortionResponseList() {
        return portionResponseList;
    }

    public void setPortionResponseList(List<ProductPortionResponse> portionResponseList) {
        this.portionResponseList = portionResponseList;
    }

    public List<PortionMasterResponse> getPortionMasterResponseList() {
        return portionMasterResponseList;
    }

    public void setPortionMasterResponseList(List<PortionMasterResponse> portionMasterResponseList) {
        this.portionMasterResponseList = portionMasterResponseList;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getPortionMasterId() {
        return portionMasterId;
    }

    public void setPortionMasterId(String portionMasterId) {
        this.portionMasterId = portionMasterId;
    }

    public String getTotalCustomer() {
        return totalCustomer;
    }

    public String getActiveCustomer() {
        return activeCustomer;
    }

    public String getTrialCustomer() {
        return trialCustomer;
    }

    public String getExpiredCustomer() {
        return expiredCustomer;
    }

    public String getActivePercent() {
        return activePercent;
    }

    public String getTrialPercent() {
        return trialPercent;
    }

    public String getExpiredPercent() {
        return expiredPercent;
    }
}
