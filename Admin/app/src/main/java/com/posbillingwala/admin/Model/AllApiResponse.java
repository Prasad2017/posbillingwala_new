package com.posbillingwala.admin.Model;

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
    @SerializedName("totalCustomer")
    @Expose
    private String totalCustomer;
    @SerializedName("totalDealer")
    @Expose
    private String totalDealer;
    @SerializedName("activeCustomer")
    @Expose
    private String activeCustomer;
    @SerializedName("trialCustomer")
    @Expose
    private String trialCustomer;
    @SerializedName("expiredCustomer")
    @Expose
    private String expiredCustomer;
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
    @SerializedName("totalDevices")
    @Expose
    private String totalDevices;
    @SerializedName("licenseKey")
    @Expose
    private String licenseKey;
    @SerializedName("userId")
    @Expose
    private String userId;
    @SerializedName("authToken")
    @Expose
    private String authToken;
    @SerializedName("tokenExpiresAt")
    @Expose
    private String tokenExpiresAt;
    @SerializedName("productId")
    @Expose
    private String productId;
    @SerializedName("customerResponse")
    @Expose
    private List<CustomerResponse> customerResponseList;
    @SerializedName("dealerResponse")
    @Expose
    private List<DealerResponse> dealerResponseList;
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
    private List<PortionMasterResponse> portionMasterResponse;
    @SerializedName("comboResponse")
    @Expose
    private List<ComboResponse> comboResponseList;
    @SerializedName("deviceResponse")
    @Expose
    private List<DeviceMonitorResponse> deviceResponseList;
    @SerializedName("invoiceResponse")
    @Expose
    private List<InvoiceSaleResponse> invoiceResponseList;
    @SerializedName("billCount")
    @Expose
    private String billCount;
    @SerializedName("netSales")
    @Expose
    private String netSales;


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

    public String getTotalCustomer() {
        return totalCustomer;
    }

    public void setTotalCustomer(String totalCustomer) {
        this.totalCustomer = totalCustomer;
    }

    public String getTotalDealer() {
        return totalDealer;
    }

    public void setTotalDealer(String totalDealer) {
        this.totalDealer = totalDealer;
    }

    public String getActiveCustomer() {
        return activeCustomer;
    }

    public void setActiveCustomer(String activeCustomer) {
        this.activeCustomer = activeCustomer;
    }

    public String getTrialCustomer() {
        return trialCustomer;
    }

    public void setTrialCustomer(String trialCustomer) {
        this.trialCustomer = trialCustomer;
    }

    public String getExpiredCustomer() {
        return expiredCustomer;
    }

    public void setExpiredCustomer(String expiredCustomer) {
        this.expiredCustomer = expiredCustomer;
    }

    public String getActiveLicenses() {
        return activeLicenses;
    }

    public void setActiveLicenses(String activeLicenses) {
        this.activeLicenses = activeLicenses;
    }

    public String getExpiringLicenses() {
        return expiringLicenses;
    }

    public void setExpiringLicenses(String expiringLicenses) {
        this.expiringLicenses = expiringLicenses;
    }

    public String getExpiredLicenses() {
        return expiredLicenses;
    }

    public void setExpiredLicenses(String expiredLicenses) {
        this.expiredLicenses = expiredLicenses;
    }

    public String getTotalBranches() {
        return totalBranches;
    }

    public void setTotalBranches(String totalBranches) {
        this.totalBranches = totalBranches;
    }

    public String getTotalDevices() {
        return totalDevices;
    }

    public void setTotalDevices(String totalDevices) {
        this.totalDevices = totalDevices;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public List<CustomerResponse> getCustomerResponseList() {
        return customerResponseList;
    }

    public void setCustomerResponseList(List<CustomerResponse> customerResponseList) {
        this.customerResponseList = customerResponseList;
    }

    public List<DealerResponse> getDealerResponseList() {
        return dealerResponseList;
    }

    public void setDealerResponseList(List<DealerResponse> dealerResponseList) {
        this.dealerResponseList = dealerResponseList;
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

    public List<PortionMasterResponse> getPortionMasterResponse() {
        return portionMasterResponse;
    }

    public void setPortionMasterResponse(List<PortionMasterResponse> portionMasterResponse) {
        this.portionMasterResponse = portionMasterResponse;
    }

    public List<ComboResponse> getComboResponseList() {
        return comboResponseList;
    }

    public void setComboResponseList(List<ComboResponse> comboResponseList) {
        this.comboResponseList = comboResponseList;
    }

    public List<DeviceMonitorResponse> getDeviceResponseList() {
        return deviceResponseList;
    }

    public void setDeviceResponseList(List<DeviceMonitorResponse> deviceResponseList) {
        this.deviceResponseList = deviceResponseList;
    }

    public List<InvoiceSaleResponse> getInvoiceResponseList() {
        return invoiceResponseList;
    }

    public void setInvoiceResponseList(List<InvoiceSaleResponse> invoiceResponseList) {
        this.invoiceResponseList = invoiceResponseList;
    }

    public String getBillCount() {
        return billCount;
    }

    public void setBillCount(String billCount) {
        this.billCount = billCount;
    }

    public String getNetSales() {
        return netSales;
    }

    public void setNetSales(String netSales) {
        this.netSales = netSales;
    }
}
