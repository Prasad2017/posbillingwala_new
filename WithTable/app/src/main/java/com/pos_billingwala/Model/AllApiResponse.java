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
