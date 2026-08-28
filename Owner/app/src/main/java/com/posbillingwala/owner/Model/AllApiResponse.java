package com.posbillingwala.owner.Model;

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
    @SerializedName("totalCategory")
    @Expose
    public String totalCategory;
    @SerializedName("totalProduct")
    @Expose
    public String totalProduct;
    @SerializedName("totalSale")
    @Expose
    public String totalSale;
    @SerializedName("todaySale")
    @Expose
    public String todaySale;
    @SerializedName("branchCount")
    @Expose
    public String branchCount;
    @SerializedName("storeCount")
    @Expose
    public String storeCount;
    @SerializedName("customerResponse")
    @Expose
    public List<CustomerResponse> customerResponseList;
    @SerializedName("invoiceResponse")
    @Expose
    public List<InvoiceResponse> invoiceResponseList;
    @SerializedName("invoiceProductResponse")
    @Expose
    public List<InvoiceProductResponse> invoiceProductResponseList;
    @SerializedName("categoryResponse")
    @Expose
    public List<ProductCategoryResponse> productCategoryResponseList;
    @SerializedName("subcategoryResponse")
    @Expose
    public List<ProductSubcategoryResponse> subcategoryResponseList;
    @SerializedName("productResponse")
    @Expose
    public List<ProductResponse> productResponseList;
    @SerializedName("portionResponse")
    @Expose
    public List<ProductPortionResponse> portionResponseList;
    @SerializedName("portionMasterResponse")
    @Expose
    public List<PortionMasterResponse> portionMasterResponseList;
    @SerializedName("licensesResponse")
    @Expose
    public List<LicenseResponse> licenseResponseList;
    @SerializedName("branches")
    @Expose
    public List<BranchComparisonResponse> branchComparisonList;
    @SerializedName("periodLabel")
    @Expose
    private String periodLabel;
    @SerializedName("totalSales")
    @Expose
    private String totalSales;
    @SerializedName("netSales")
    @Expose
    private String netSales;
    @SerializedName("totalInvoices")
    @Expose
    private String totalInvoices;
    @SerializedName("avgBill")
    @Expose
    private String avgBill;
    @SerializedName("totalSalesTrend")
    @Expose
    private String totalSalesTrend;
    @SerializedName("netSalesTrend")
    @Expose
    private String netSalesTrend;
    @SerializedName("invoicesTrend")
    @Expose
    private String invoicesTrend;
    @SerializedName("avgBillTrend")
    @Expose
    private String avgBillTrend;
    @SerializedName("salesTrend")
    @Expose
    private List<ReportRankItem> salesTrend;
    @SerializedName("topCustomers")
    @Expose
    private List<ReportRankItem> topCustomers;
    @SerializedName("recentInvoices")
    @Expose
    private List<InvoiceResponse> recentInvoices;
    @SerializedName("productId")
    @Expose
    public String productId;



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

    public String getTotalCategory() {
        return totalCategory;
    }

    public void setTotalCategory(String totalCategory) {
        this.totalCategory = totalCategory;
    }

    public String getTotalProduct() {
        return totalProduct;
    }

    public void setTotalProduct(String totalProduct) {
        this.totalProduct = totalProduct;
    }

    public String getTotalSale() {
        return totalSale;
    }

    public void setTotalSale(String totalSale) {
        this.totalSale = totalSale;
    }

    public String getTodaySale() {
        return todaySale;
    }

    public void setTodaySale(String todaySale) {
        this.todaySale = todaySale;
    }

    public String getBranchCount() {
        return branchCount;
    }

    public String getStoreCount() {
        return storeCount;
    }

    public List<CustomerResponse> getCustomerResponseList() {
        return customerResponseList;
    }

    public void setCustomerResponseList(List<CustomerResponse> customerResponseList) {
        this.customerResponseList = customerResponseList;
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

    public List<ProductCategoryResponse> getProductCategoryResponseList() {
        return productCategoryResponseList;
    }

    public void setProductCategoryResponseList(List<ProductCategoryResponse> productCategoryResponseList) {
        this.productCategoryResponseList = productCategoryResponseList;
    }

    public List<ProductSubcategoryResponse> getSubcategoryResponseList() {
        return subcategoryResponseList;
    }

    public void setSubcategoryResponseList(List<ProductSubcategoryResponse> subcategoryResponseList) {
        this.subcategoryResponseList = subcategoryResponseList;
    }

    public List<ProductResponse> getProductResponseList() {
        return productResponseList;
    }

    public void setProductResponseList(List<ProductResponse> productResponseList) {
        this.productResponseList = productResponseList;
    }

    public List<LicenseResponse> getLicenseResponseList() {
        return licenseResponseList;
    }

    public void setLicenseResponseList(List<LicenseResponse> licenseResponseList) {
        this.licenseResponseList = licenseResponseList;
    }

    public List<BranchComparisonResponse> getBranchComparisonList() {
        return branchComparisonList;
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

    public String getPeriodLabel() {
        return periodLabel;
    }

    public String getTotalSales() {
        return totalSales;
    }

    public String getNetSales() {
        return netSales;
    }

    public String getTotalInvoices() {
        return totalInvoices;
    }

    public String getAvgBill() {
        return avgBill;
    }

    public String getTotalSalesTrend() {
        return totalSalesTrend;
    }

    public String getNetSalesTrend() {
        return netSalesTrend;
    }

    public String getInvoicesTrend() {
        return invoicesTrend;
    }

    public String getAvgBillTrend() {
        return avgBillTrend;
    }

    public List<ReportRankItem> getSalesTrend() {
        return salesTrend;
    }

    public List<ReportRankItem> getTopCustomers() {
        return topCustomers;
    }

    public List<InvoiceResponse> getRecentInvoices() {
        return recentInvoices;
    }
}
