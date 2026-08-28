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
    @SerializedName("errorLogList")
    @Expose
    private List<ErrorLogSummary> errorLogList;
    @SerializedName("errorLogDetail")
    @Expose
    private ErrorLogDetail errorLogDetail;
    @SerializedName("invoiceResponse")
    @Expose
    private List<InvoiceSaleResponse> invoiceResponseList;
    @SerializedName("billCount")
    @Expose
    private String billCount;
    @SerializedName("netSales")
    @Expose
    private String netSales;
    @SerializedName("todaySales")
    @Expose
    private String todaySales;
    @SerializedName("customersAddedThisMonth")
    @Expose
    private String customersAddedThisMonth;
    @SerializedName("expiringLicenses7Days")
    @Expose
    private String expiringLicenses7Days;
    @SerializedName("trialLicenses")
    @Expose
    private String trialLicenses;
    @SerializedName("notificationCount")
    @Expose
    private String notificationCount;
    @SerializedName("trialLicensesExpiringTomorrow")
    @Expose
    private String trialLicensesExpiringTomorrow;
    @SerializedName("totalSales")
    @Expose
    private String totalSales;
    @SerializedName("salesSparkline")
    @Expose
    private List<String> salesSparkline;
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
    @SerializedName("categoryCount")
    @Expose
    private String categoryCount;
    @SerializedName("subcategoryCount")
    @Expose
    private String subcategoryCount;
    @SerializedName("productCount")
    @Expose
    private String productCount;
    @SerializedName("totalLicenses")
    @Expose
    private String totalLicenses;
    @SerializedName("monthSales")
    @Expose
    private String monthSales;
    @SerializedName("collection")
    @Expose
    private String collection;
    @SerializedName("licensesResponse")
    @Expose
    private List<LicenseResponse> licensesResponseList;
    @SerializedName("dealerSalesResponse")
    @Expose
    private List<DealerSalesResponse> dealerSalesResponseList;

    @SerializedName("periodLabel") @Expose private String periodLabel;
    @SerializedName("totalInvoices") @Expose private String totalInvoices;
    @SerializedName("avgBill") @Expose private String avgBill;
    @SerializedName("totalSalesTrend") @Expose private String totalSalesTrend;
    @SerializedName("invoicesTrend") @Expose private String invoicesTrend;
    @SerializedName("avgBillTrend") @Expose private String avgBillTrend;
    @SerializedName("salesTrend") @Expose private List<ReportRankItem> salesTrend;
    @SerializedName("topCustomers") @Expose private List<ReportRankItem> topCustomers;
    @SerializedName("growthBars") @Expose private List<ReportRankItem> growthBars;
    @SerializedName("expiryWindows") @Expose private List<ReportRankItem> expiryWindows;
    @SerializedName("activePercent") @Expose private String activePercent;
    @SerializedName("trialPercent") @Expose private String trialPercent;
    @SerializedName("expiredPercent") @Expose private String expiredPercent;
    @SerializedName("expiringPercent") @Expose private String expiringPercent;
    @SerializedName("inactivePercent") @Expose private String inactivePercent;
    @SerializedName("newPercent") @Expose private String newPercent;
    @SerializedName("notUsedPercent") @Expose private String notUsedPercent;
    @SerializedName("activeBranches") @Expose private String activeBranches;
    @SerializedName("inactiveBranches") @Expose private String inactiveBranches;
    @SerializedName("newBranches") @Expose private String newBranches;
    @SerializedName("activeDevices") @Expose private String activeDevices;
    @SerializedName("inactiveDevices") @Expose private String inactiveDevices;
    @SerializedName("notUsedDevices") @Expose private String notUsedDevices;
    @SerializedName("recentInvoices") @Expose private List<InvoiceSaleResponse> recentInvoices;
    @SerializedName("crashes") @Expose private List<CrashLogItem> crashes;
    @SerializedName("totalCrashes") @Expose private String totalCrashes;
    @SerializedName("affectedUsers") @Expose private String affectedUsers;
    @SerializedName("resolved") @Expose private String resolved;
    @SerializedName("totalCrashesTrend") @Expose private String totalCrashesTrend;
    @SerializedName("affectedUsersTrend") @Expose private String affectedUsersTrend;
    @SerializedName("resolvedTrend") @Expose private String resolvedTrend;
    @SerializedName("byApp") @Expose private List<ReportRankItem> byApp;
    @SerializedName("overTime") @Expose private List<ReportRankItem> overTime;
    @SerializedName("topErrors") @Expose private List<ReportRankItem> topErrors;
    @SerializedName("tickets") @Expose private List<SupportTicketItem> tickets;
    @SerializedName("contacts") @Expose private List<WebsiteContactItem> contacts;
    @SerializedName("messages") @Expose private List<SupportTicketItem.SupportMessageItem> ticketMessages;
    @SerializedName("ticketNo") @Expose private String ticketNo;
    @SerializedName("subject") @Expose private String subject;
    @SerializedName("description") @Expose private String description;
    @SerializedName("appName") @Expose private String appName;
    @SerializedName("stackTrace") @Expose private String stackTrace;
    @SerializedName("errorTitle") @Expose private String errorTitle;
    @SerializedName("errorClass") @Expose private String errorClass;
    @SerializedName("deviceName") @Expose private String deviceName;
    @SerializedName("androidVersion") @Expose private String androidVersion;
    @SerializedName("appVersion") @Expose private String appVersion;
    @SerializedName("userName") @Expose private String userName;
    @SerializedName("occurrences") @Expose private String occurrences;
    @SerializedName("createdAt") @Expose private String createdAt;
    @SerializedName("paymentStatus") @Expose private String paymentStatus;
    @SerializedName("paymentMethod") @Expose private String paymentMethod;
    @SerializedName("cashierName") @Expose private String cashierName;
    @SerializedName("subtotal") @Expose private String subtotal;
    @SerializedName("tax") @Expose private String tax;
    @SerializedName("paidAmount") @Expose private String paidAmount;
    @SerializedName("items") @Expose private List<ReportRankItem> invoiceItems;
    @SerializedName("invoiceNumber") @Expose private String invoiceNumber;
    @SerializedName("invoiceDate") @Expose private String invoiceDate;
    @SerializedName("shopName") @Expose private String shopName;
    @SerializedName("email") @Expose private String contactEmail;
    @SerializedName("totalAmount") @Expose private String totalAmount;
    @SerializedName("customerName") @Expose private String detailCustomerName;


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

    public List<ErrorLogSummary> getErrorLogList() {
        return errorLogList;
    }

    public void setErrorLogList(List<ErrorLogSummary> errorLogList) {
        this.errorLogList = errorLogList;
    }

    public ErrorLogDetail getErrorLogDetail() {
        return errorLogDetail;
    }

    public void setErrorLogDetail(ErrorLogDetail errorLogDetail) {
        this.errorLogDetail = errorLogDetail;
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

    public String getTodaySales() {
        return todaySales;
    }

    public String getCustomersAddedThisMonth() {
        return customersAddedThisMonth;
    }

    public String getExpiringLicenses7Days() {
        return expiringLicenses7Days;
    }

    public String getTrialLicenses() {
        return trialLicenses;
    }

    public String getNotificationCount() {
        return notificationCount;
    }

    public String getTrialLicensesExpiringTomorrow() {
        return trialLicensesExpiringTomorrow;
    }

    public String getTotalSales() {
        return totalSales;
    }

    public List<String> getSalesSparkline() {
        return salesSparkline;
    }

    public String getTotalCustomerTrendLabel() {
        return totalCustomerTrendLabel;
    }

    public String getActiveCustomerTrendLabel() {
        return activeCustomerTrendLabel;
    }

    public String getTrialCustomerTrendLabel() {
        return trialCustomerTrendLabel;
    }

    public String getExpiredCustomerTrendLabel() {
        return expiredCustomerTrendLabel;
    }

    public String getActiveLicensesTrendLabel() {
        return activeLicensesTrendLabel;
    }

    public String getExpiringLicensesTrendLabel() {
        return expiringLicensesTrendLabel;
    }

    public String getTrialLicensesTrendLabel() {
        return trialLicensesTrendLabel;
    }

    public String getExpiredLicensesTrendLabel() {
        return expiredLicensesTrendLabel;
    }

    public String getNetSalesTrendLabel() {
        return netSalesTrendLabel;
    }

    public String getTodaySalesTrendLabel() {
        return todaySalesTrendLabel;
    }

    public String getCustomersAddedTrendLabel() {
        return customersAddedTrendLabel;
    }

    public String getActiveBranchesTrendLabel() {
        return activeBranchesTrendLabel;
    }

    public String getNetSalesTrend() {
        return netSalesTrend;
    }

    public String getTodaySalesTrend() {
        return todaySalesTrend;
    }

    public String getCustomersAddedTrend() {
        return customersAddedTrend;
    }

    public String getActiveBranchesTrend() {
        return activeBranchesTrend;
    }

    public String getTotalCustomerTrend() {
        return totalCustomerTrend;
    }

    public String getActiveCustomerTrend() {
        return activeCustomerTrend;
    }

    public String getTrialCustomerTrend() {
        return trialCustomerTrend;
    }

    public String getExpiredCustomerTrend() {
        return expiredCustomerTrend;
    }

    public String getActiveLicensesTrend() {
        return activeLicensesTrend;
    }

    public String getExpiringLicensesTrend() {
        return expiringLicensesTrend;
    }

    public String getTrialLicensesTrend() {
        return trialLicensesTrend;
    }

    public String getExpiredLicensesTrend() {
        return expiredLicensesTrend;
    }

    public String getCategoryCount() {
        return categoryCount;
    }

    public String getSubcategoryCount() {
        return subcategoryCount;
    }

    public String getProductCount() {
        return productCount;
    }

    public String getTotalLicenses() {
        return totalLicenses;
    }

    public String getMonthSales() {
        return monthSales;
    }

    public String getCollection() {
        return collection;
    }

    public List<LicenseResponse> getLicensesResponseList() {
        return licensesResponseList;
    }

    public List<DealerSalesResponse> getDealerSalesResponseList() {
        return dealerSalesResponseList;
    }

    public String getPeriodLabel() { return periodLabel; }
    public String getNetSalesValue() { return netSales; }
    public String getTotalInvoices() { return totalInvoices; }
    public String getAvgBill() { return avgBill; }
    public String getTotalSalesTrend() { return totalSalesTrend; }
    public String getInvoicesTrend() { return invoicesTrend; }
    public String getAvgBillTrend() { return avgBillTrend; }
    public List<ReportRankItem> getSalesTrend() { return salesTrend; }
    public List<ReportRankItem> getTopCustomers() { return topCustomers; }
    public List<ReportRankItem> getGrowthBars() { return growthBars; }
    public List<ReportRankItem> getExpiryWindows() { return expiryWindows; }
    public String getActivePercent() { return activePercent; }
    public String getTrialPercent() { return trialPercent; }
    public String getExpiredPercent() { return expiredPercent; }
    public String getExpiringPercent() { return expiringPercent; }
    public String getInactivePercent() { return inactivePercent; }
    public String getNewPercent() { return newPercent; }
    public String getNotUsedPercent() { return notUsedPercent; }
    public String getActiveBranches() { return activeBranches; }
    public String getInactiveBranches() { return inactiveBranches; }
    public String getNewBranches() { return newBranches; }
    public String getActiveDevices() { return activeDevices; }
    public String getInactiveDevices() { return inactiveDevices; }
    public String getNotUsedDevices() { return notUsedDevices; }
    public List<InvoiceSaleResponse> getRecentInvoices() { return recentInvoices; }
    public List<CrashLogItem> getCrashes() { return crashes; }
    public String getTotalCrashes() { return totalCrashes; }
    public String getAffectedUsers() { return affectedUsers; }
    public String getResolved() { return resolved; }
    public String getTotalCrashesTrend() { return totalCrashesTrend; }
    public String getAffectedUsersTrend() { return affectedUsersTrend; }
    public String getResolvedTrend() { return resolvedTrend; }
    public List<ReportRankItem> getByApp() { return byApp; }
    public List<ReportRankItem> getOverTime() { return overTime; }
    public List<ReportRankItem> getTopErrors() { return topErrors; }
    public List<SupportTicketItem> getTickets() { return tickets; }
    public List<WebsiteContactItem> getContacts() { return contacts; }
    public List<SupportTicketItem.SupportMessageItem> getTicketMessages() { return ticketMessages; }
    public String getTicketNo() { return ticketNo; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getAppName() { return appName; }
    public String getStackTrace() { return stackTrace; }
    public String getErrorTitle() { return errorTitle; }
    public String getErrorClass() { return errorClass; }
    public String getDeviceName() { return deviceName; }
    public String getAndroidVersion() { return androidVersion; }
    public String getAppVersion() { return appVersion; }
    public String getUserName() { return userName; }
    public String getOccurrences() { return occurrences; }
    public String getCreatedAt() { return createdAt; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getCashierName() { return cashierName; }
    public String getSubtotal() { return subtotal; }
    public String getTax() { return tax; }
    public String getPaidAmount() { return paidAmount; }
    public List<ReportRankItem> getInvoiceItems() { return invoiceItems; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getInvoiceDate() { return invoiceDate; }
    public String getShopName() { return shopName; }
    public String getEmail() { return contactEmail; }
    public String getTotalAmount() { return totalAmount; }
    public String getDetailCustomerName() { return detailCustomerName; }
    public String getBillCountField() { return billCount; }
}
