package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AllApiResponse {

    @SerializedName("status")
    @Expose
    public String status;
    @SerializedName("message")
    @Expose
    public String message;
    @SerializedName("licenceKey")
    @Expose
    public String licenceKey;
    @SerializedName("licenceId")
    @Expose
    public String licenceId;
    @SerializedName("mpin")
    @Expose
    public String mpin;
    @SerializedName("reportPin")
    @Expose
    public String reportPin;
    @SerializedName("trialDays")
    @Expose
    public String trialDays;
    @SerializedName("trialMaxBills")
    @Expose
    public String trialMaxBills;
    @SerializedName("categoryResponse")
    @Expose
    public List<ProductCategoryResponse> productCategoryResponseList;
    @SerializedName("productResponse")
    @Expose
    public List<ProductResponse> productResponseList;
    @SerializedName("companyResponse")
    @Expose
    public List<CompanyResponse> companyResponseList;
    @SerializedName("printerResponse")
    @Expose
    public List<PrinterSettingResponse> printerSettingResponseList;
    @SerializedName("invoiceResponse")
    @Expose
    public List<InvoiceResponse> invoiceResponseList;
    @SerializedName("invoiceProductResponse")
    @Expose
    public List<InvoiceProductResponse> invoiceProductResponseList;
    @SerializedName("inventoryResponse")
    @Expose
    public List<InventoryResponse> inventoryResponseList;
    @SerializedName("expensesResponse")
    @Expose
    public List<ExpenseResponse> expenseResponseList;
    @SerializedName("serviceAppointmentResponse")
    @Expose
    public List<ServiceAppointmentResponse> serviceAppointmentResponseList;
    @SerializedName("customOrderDepositResponse")
    @Expose
    public List<CustomOrderDepositResponse> customOrderDepositResponseList;
    @SerializedName("productPriceTierResponse")
    @Expose
    public List<ProductPriceTierResponse> productPriceTierResponseList;
    @SerializedName("productVariantResponse")
    @Expose
    public List<ProductVariantResponse> productVariantResponseList;
    @SerializedName("businessTemplateResponse")
    @Expose
    public List<BusinessTemplateSyncResponse> businessTemplateResponseList;
    @SerializedName("staffUserResponse")
    @Expose
    public List<StaffUserResponse> staffUserResponseList;
    @SerializedName("foodTypeResponse")
    @Expose
    public List<FoodTypeResponse> foodTypeResponseList;
    @SerializedName("subcategoryResponse")
    @Expose
    public List<ProductSubcategoryResponse> subcategoryResponseList;
    @SerializedName("portionResponse")
    @Expose
    public List<ProductPortionResponse> portionResponseList;
    @SerializedName("portionMasterResponse")
    @Expose
    public List<PortionMasterResponse> portionMasterResponseList;
    @SerializedName("comboResponse")
    @Expose
    public List<ComboResponse> comboResponseList;
    @SerializedName("comboItemResponse")
    @Expose
    public List<ComboItemResponse> comboItemResponseList;
    @SerializedName("invoiceComboItemResponse")
    @Expose
    public List<ComboItemResponse> invoiceComboItemResponseList;
    @SerializedName("memberResponse")
    @Expose
    public List<MemberResponse> memberResponseList;
    @SerializedName("memberInvoiceResponse")
    @Expose
    public List<MessInvoiceResponse> messInvoiceResponseList;
    @SerializedName("messTokenResponse")
    @Expose
    public List<MessTokenResponse> messTokenResponseList;

    @SerializedName("diningAreaResponse")
    @Expose
    public List<DiningAreaResponse> diningAreaResponseList;
    @SerializedName("tableTypeResponse")
    @Expose
    public List<TableTypeResponse> tableTypeResponseList;
    @SerializedName("posTableResponse")
    @Expose
    public List<PosTableResponse> posTableResponseList;

    @SerializedName("qr")
    @Expose
    public MessQrInfo messQr;
    @SerializedName("hasQr")
    @Expose
    public String hasQr;
    @SerializedName("sessions")
    @Expose
    public List<MessMealSessionItem> messSessions;
    @SerializedName("tokens")
    @Expose
    public List<MessMealTokenItem> messMealTokens;
    @SerializedName("counts")
    @Expose
    public List<MessSessionCount> messSessionCounts;
    @SerializedName("registrationNo")
    @Expose
    public String registrationNo;
    @SerializedName("qrStatus")
    @Expose
    public String qrStatus;
    @SerializedName("printStatus")
    @Expose
    public String printStatus;

    @SerializedName("tickets")
    @Expose
    private List<SupportTicketItem> tickets;
    @SerializedName("messages")
    @Expose
    private List<SupportTicketItem.SupportMessageItem> ticketMessages;
    @SerializedName("ticketNo")
    @Expose
    private String ticketNo;
    @SerializedName("ticketId")
    @Expose
    private String ticketId;
    @SerializedName("subject")
    @Expose
    private String subject;
    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("category")
    @Expose
    private String category;
    @SerializedName("ticketStatus")
    @Expose
    private String ticketStatus;
    @SerializedName("createdAt")
    @Expose
    private String createdAt;
    @SerializedName("appName")
    @Expose
    private String appName;
    @SerializedName("period")
    @Expose
    public String period;
    @SerializedName("primarySalesLabel")
    @Expose
    public String primarySalesLabel;
    @SerializedName("primarySales")
    @Expose
    public String primarySales;
    @SerializedName("todaySales")
    @Expose
    public String todaySales;
    @SerializedName("monthSales")
    @Expose
    public String monthSales;
    @SerializedName("allTimeSales")
    @Expose
    public String allTimeSales;
    @SerializedName("primarySalesTrend")
    @Expose
    public String primarySalesTrend;
    @SerializedName("todaySalesTrend")
    @Expose
    public String todaySalesTrend;
    @SerializedName("totalSubcategory")
    @Expose
    public String totalSubcategory;
    @SerializedName("totalProduct")
    @Expose
    public String totalProduct;
    @SerializedName("totalCombo")
    @Expose
    public String totalCombo;


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

    public String getLicenceKey() {
        return licenceKey;
    }

    public void setLicenceKey(String licenceKey) {
        this.licenceKey = licenceKey;
    }

    public String getLicenceId() {
        return licenceId;
    }

    public void setLicenceId(String licenceId) {
        this.licenceId = licenceId;
    }

    public String getMpin() {
        return mpin;
    }

    public void setMpin(String mpin) {
        this.mpin = mpin;
    }

    public String getReportPin() {
        return reportPin;
    }

    public void setReportPin(String reportPin) {
        this.reportPin = reportPin;
    }

    public String getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(String trialDays) {
        this.trialDays = trialDays;
    }

    public String getTrialMaxBills() {
        return trialMaxBills;
    }

    public void setTrialMaxBills(String trialMaxBills) {
        this.trialMaxBills = trialMaxBills;
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

    public List<CompanyResponse> getCompanyResponseList() {
        return companyResponseList;
    }

    public void setCompanyResponseList(List<CompanyResponse> companyResponseList) {
        this.companyResponseList = companyResponseList;
    }

    public List<PrinterSettingResponse> getPrinterSettingResponseList() {
        return printerSettingResponseList;
    }

    public void setPrinterSettingResponseList(List<PrinterSettingResponse> printerSettingResponseList) {
        this.printerSettingResponseList = printerSettingResponseList;
    }

    public List<InvoiceResponse> getInvoiceResponseList() {
        return invoiceResponseList;
    }

    public void setInvoiceResponseList(List<InvoiceResponse> invoiceResponseList) {
        this.invoiceResponseList = invoiceResponseList;
    }

    public List<InvoiceProductResponse> getInvoiceProductResponseList() {
        return invoiceProductResponseList;
    }

    public void setInvoiceProductResponseList(List<InvoiceProductResponse> invoiceProductResponseList) {
        this.invoiceProductResponseList = invoiceProductResponseList;
    }

    public List<InventoryResponse> getInventoryResponseList() {
        return inventoryResponseList;
    }

    public void setInventoryResponseList(List<InventoryResponse> inventoryResponseList) {
        this.inventoryResponseList = inventoryResponseList;
    }

    public List<ExpenseResponse> getExpenseResponseList() {
        return expenseResponseList;
    }

    public void setExpenseResponseList(List<ExpenseResponse> expenseResponseList) {
        this.expenseResponseList = expenseResponseList;
    }

    public List<ServiceAppointmentResponse> getServiceAppointmentResponseList() {
        return serviceAppointmentResponseList;
    }

    public void setServiceAppointmentResponseList(List<ServiceAppointmentResponse> serviceAppointmentResponseList) {
        this.serviceAppointmentResponseList = serviceAppointmentResponseList;
    }

    public List<CustomOrderDepositResponse> getCustomOrderDepositResponseList() {
        return customOrderDepositResponseList;
    }

    public void setCustomOrderDepositResponseList(List<CustomOrderDepositResponse> customOrderDepositResponseList) {
        this.customOrderDepositResponseList = customOrderDepositResponseList;
    }

    public List<ProductPriceTierResponse> getProductPriceTierResponseList() {
        return productPriceTierResponseList;
    }

    public void setProductPriceTierResponseList(List<ProductPriceTierResponse> productPriceTierResponseList) {
        this.productPriceTierResponseList = productPriceTierResponseList;
    }

    public List<ProductVariantResponse> getProductVariantResponseList() {
        return productVariantResponseList;
    }

    public void setProductVariantResponseList(List<ProductVariantResponse> productVariantResponseList) {
        this.productVariantResponseList = productVariantResponseList;
    }

    public List<BusinessTemplateSyncResponse> getBusinessTemplateResponseList() {
        return businessTemplateResponseList;
    }

    public void setBusinessTemplateResponseList(List<BusinessTemplateSyncResponse> businessTemplateResponseList) {
        this.businessTemplateResponseList = businessTemplateResponseList;
    }

    public List<StaffUserResponse> getStaffUserResponseList() {
        return staffUserResponseList;
    }

    public void setStaffUserResponseList(List<StaffUserResponse> staffUserResponseList) {
        this.staffUserResponseList = staffUserResponseList;
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

    public List<ComboResponse> getComboResponseList() {
        return comboResponseList;
    }

    public void setComboResponseList(List<ComboResponse> comboResponseList) {
        this.comboResponseList = comboResponseList;
    }

    public List<ComboItemResponse> getComboItemResponseList() {
        return comboItemResponseList;
    }

    public void setComboItemResponseList(List<ComboItemResponse> comboItemResponseList) {
        this.comboItemResponseList = comboItemResponseList;
    }

    public List<ComboItemResponse> getInvoiceComboItemResponseList() {
        return invoiceComboItemResponseList;
    }

    public void setInvoiceComboItemResponseList(List<ComboItemResponse> invoiceComboItemResponseList) {
        this.invoiceComboItemResponseList = invoiceComboItemResponseList;
    }

    public List<MemberResponse> getMemberResponseList() {
        return memberResponseList;
    }

    public void setMemberResponseList(List<MemberResponse> memberResponseList) {
        this.memberResponseList = memberResponseList;
    }

    public List<MessInvoiceResponse> getMessInvoiceResponseList() {
        return messInvoiceResponseList;
    }

    public void setMessInvoiceResponseList(List<MessInvoiceResponse> messInvoiceResponseList) {
        this.messInvoiceResponseList = messInvoiceResponseList;
    }

    public List<MessTokenResponse> getMessTokenResponseList() {
        return messTokenResponseList;
    }

    public void setMessTokenResponseList(List<MessTokenResponse> messTokenResponseList) {
        this.messTokenResponseList = messTokenResponseList;
    }

    public List<DiningAreaResponse> getDiningAreaResponseList() {
        return diningAreaResponseList;
    }

    public void setDiningAreaResponseList(List<DiningAreaResponse> diningAreaResponseList) {
        this.diningAreaResponseList = diningAreaResponseList;
    }

    public List<TableTypeResponse> getTableTypeResponseList() {
        return tableTypeResponseList;
    }

    public void setTableTypeResponseList(List<TableTypeResponse> tableTypeResponseList) {
        this.tableTypeResponseList = tableTypeResponseList;
    }

    public List<PosTableResponse> getPosTableResponseList() {
        return posTableResponseList;
    }

    public void setPosTableResponseList(List<PosTableResponse> posTableResponseList) {
        this.posTableResponseList = posTableResponseList;
    }

    public List<SupportTicketItem> getTickets() { return tickets; }
    public List<SupportTicketItem.SupportMessageItem> getTicketMessages() { return ticketMessages; }
    public String getTicketNo() { return ticketNo; }
    public String getTicketId() { return ticketId; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getTicketStatus() { return ticketStatus; }
    public String getCreatedAt() { return createdAt; }
    public String getAppName() { return appName; }
}
