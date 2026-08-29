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
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("contact_number")
    @Expose
    private String contactNumber;
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
    @SerializedName("activeLicenses")
    @Expose
    private String activeLicenses;
    @SerializedName("expiringLicenses")
    @Expose
    private String expiringLicenses;
    @SerializedName("expiredLicenses")
    @Expose
    private String expiredLicenses;
    @SerializedName("totalBranches")
    @Expose
    private String totalBranches;
    @SerializedName("trialLicenses")
    @Expose
    private String trialLicenses;
    @SerializedName("expiringLicenses7Days")
    @Expose
    private String expiringLicenses7Days;
    @SerializedName("trialLicensesExpiringTomorrow")
    @Expose
    private String trialLicensesExpiringTomorrow;
    @SerializedName("customersAddedThisMonth")
    @Expose
    private String customersAddedThisMonth;
    @SerializedName("netSales")
    @Expose
    private String netSales;
    @SerializedName("todaySales")
    @Expose
    private String todaySales;
    @SerializedName("notificationCount")
    @Expose
    private String notificationCount;
    @SerializedName("salesSparkline")
    @Expose
    private List<String> salesSparkline;
    @SerializedName("dealerSalesResponse")
    @Expose
    private List<DealerSalesResponse> dealerSalesResponseList;
    @SerializedName("totalCustomerTrend")
    @Expose
    private String totalCustomerTrend;
    @SerializedName("activeCustomerTrend")
    @Expose
    private String activeCustomerTrend;
    @SerializedName("trialCustomerTrend")
    @Expose
    private String trialCustomerTrend;
    @SerializedName("expiredCustomerTrend")
    @Expose
    private String expiredCustomerTrend;
    @SerializedName("activeLicensesTrend")
    @Expose
    private String activeLicensesTrend;
    @SerializedName("expiringLicensesTrend")
    @Expose
    private String expiringLicensesTrend;
    @SerializedName("trialLicensesTrend")
    @Expose
    private String trialLicensesTrend;
    @SerializedName("expiredLicensesTrend")
    @Expose
    private String expiredLicensesTrend;
    @SerializedName("netSalesTrend")
    @Expose
    private String netSalesTrend;
    @SerializedName("todaySalesTrend")
    @Expose
    private String todaySalesTrend;
    @SerializedName("customersAddedTrend")
    @Expose
    private String customersAddedTrend;
    @SerializedName("activeBranchesTrend")
    @Expose
    private String activeBranchesTrend;
    @SerializedName("totalCustomerTrendLabel")
    @Expose
    private String totalCustomerTrendLabel;
    @SerializedName("activeCustomerTrendLabel")
    @Expose
    private String activeCustomerTrendLabel;
    @SerializedName("trialCustomerTrendLabel")
    @Expose
    private String trialCustomerTrendLabel;
    @SerializedName("expiredCustomerTrendLabel")
    @Expose
    private String expiredCustomerTrendLabel;
    @SerializedName("activeLicensesTrendLabel")
    @Expose
    private String activeLicensesTrendLabel;
    @SerializedName("expiringLicensesTrendLabel")
    @Expose
    private String expiringLicensesTrendLabel;
    @SerializedName("trialLicensesTrendLabel")
    @Expose
    private String trialLicensesTrendLabel;
    @SerializedName("expiredLicensesTrendLabel")
    @Expose
    private String expiredLicensesTrendLabel;
    @SerializedName("netSalesTrendLabel")
    @Expose
    private String netSalesTrendLabel;
    @SerializedName("todaySalesTrendLabel")
    @Expose
    private String todaySalesTrendLabel;
    @SerializedName("customersAddedTrendLabel")
    @Expose
    private String customersAddedTrendLabel;
    @SerializedName("activeBranchesTrendLabel")
    @Expose
    private String activeBranchesTrendLabel;


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

    public String getName() {
        return name;
    }

    public String getContactNumber() {
        return contactNumber;
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

    public String getActiveLicenses() { return activeLicenses; }
    public String getExpiringLicenses() { return expiringLicenses; }
    public String getExpiredLicenses() { return expiredLicenses; }
    public String getTotalBranches() { return totalBranches; }
    public String getTrialLicenses() { return trialLicenses; }
    public String getExpiringLicenses7Days() { return expiringLicenses7Days; }
    public String getTrialLicensesExpiringTomorrow() { return trialLicensesExpiringTomorrow; }
    public String getCustomersAddedThisMonth() { return customersAddedThisMonth; }
    public String getNetSales() { return netSales; }
    public String getTodaySales() { return todaySales; }
    public String getNotificationCount() { return notificationCount; }
    public List<String> getSalesSparkline() { return salesSparkline; }
    public List<DealerSalesResponse> getDealerSalesResponseList() { return dealerSalesResponseList; }
    public String getTotalCustomerTrend() { return totalCustomerTrend; }
    public String getActiveCustomerTrend() { return activeCustomerTrend; }
    public String getTrialCustomerTrend() { return trialCustomerTrend; }
    public String getExpiredCustomerTrend() { return expiredCustomerTrend; }
    public String getActiveLicensesTrend() { return activeLicensesTrend; }
    public String getExpiringLicensesTrend() { return expiringLicensesTrend; }
    public String getTrialLicensesTrend() { return trialLicensesTrend; }
    public String getExpiredLicensesTrend() { return expiredLicensesTrend; }
    public String getNetSalesTrend() { return netSalesTrend; }
    public String getTodaySalesTrend() { return todaySalesTrend; }
    public String getCustomersAddedTrend() { return customersAddedTrend; }
    public String getActiveBranchesTrend() { return activeBranchesTrend; }
    public String getTotalCustomerTrendLabel() { return totalCustomerTrendLabel; }
    public String getActiveCustomerTrendLabel() { return activeCustomerTrendLabel; }
    public String getTrialCustomerTrendLabel() { return trialCustomerTrendLabel; }
    public String getExpiredCustomerTrendLabel() { return expiredCustomerTrendLabel; }
    public String getActiveLicensesTrendLabel() { return activeLicensesTrendLabel; }
    public String getExpiringLicensesTrendLabel() { return expiringLicensesTrendLabel; }
    public String getTrialLicensesTrendLabel() { return trialLicensesTrendLabel; }
    public String getExpiredLicensesTrendLabel() { return expiredLicensesTrendLabel; }
    public String getNetSalesTrendLabel() { return netSalesTrendLabel; }
    public String getTodaySalesTrendLabel() { return todaySalesTrendLabel; }
    public String getCustomersAddedTrendLabel() { return customersAddedTrendLabel; }
    public String getActiveBranchesTrendLabel() { return activeBranchesTrendLabel; }
}
