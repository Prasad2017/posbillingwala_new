package com.posbillingwala.owner.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class InvoiceProductResponse {

    @SerializedName("invoiceId")
    @Expose
    public String invoiceId;
    @SerializedName("userId")
    @Expose
    public String userId;
    @SerializedName("noOfTable")
    @Expose
    public String noOfTable;
    @SerializedName("invoiceNumber")
    @Expose
    public String invoiceNumber;
    @SerializedName("customerName")
    @Expose
    public String customerName;
    @SerializedName("customerMobile")
    @Expose
    public String customerMobile;
    @SerializedName("customerEmail")
    @Expose
    public String customerEmail;
    @SerializedName("customerAddress")
    @Expose
    public String customerAddress;
    @SerializedName("subTotal")
    @Expose
    public String subTotal;
    @SerializedName("totalGSTAmount")
    @Expose
    public String totalGSTAmount;
    @SerializedName("discount")
    @Expose
    public String discount;
    @SerializedName("discountType")
    @Expose
    public String discountType;
    @SerializedName("totalAmount")
    @Expose
    public String totalAmount;
    @SerializedName("paymentMode")
    @Expose
    public String paymentMode;
    @SerializedName("invoiceDate")
    @Expose
    public String invoiceDate;
    @SerializedName("invoiceOrderStatus")
    @Expose
    public String invoiceOrderStatus;
    @SerializedName("invoiceType")
    @Expose
    public String invoiceType;
    @SerializedName("invoiceStatus")
    @Expose
    public String invoiceStatus;

    @SerializedName("invoiceProductId")
    @Expose
    public String invoiceProductId;
    @SerializedName("productName")
    @Expose
    public String productName;
    @SerializedName("productPrice")
    @Expose
    public String productPrice;
    @SerializedName("productUnit")
    @Expose
    public String productUnit;
    @SerializedName("productCGST")
    @Expose
    public String productCGST;
    @SerializedName("productSGST")
    @Expose
    public String productSGST;
    @SerializedName("productQuantity")
    @Expose
    public String productQuantity;
    @SerializedName("productStatus")
    @Expose
    public String productStatus;
    @SerializedName("invoiceNetworkStatus")
    @Expose
    public String invoiceNetworkStatus;
    @SerializedName("companyId")
    @Expose
    public String companyId;
    @SerializedName("companyName")
    @Expose
    public String companyName;
    @SerializedName("cashierName")
    @Expose
    public String cashierName;
    @SerializedName("companyMobile")
    @Expose
    public String companyMobile;
    @SerializedName("companyAddress")
    @Expose
    public String companyAddress;
    @SerializedName("countryName")
    @Expose
    public String countryName;
    @SerializedName("tableStatus")
    @Expose
    public String tableStatus;
    @SerializedName("currencyName")
    @Expose
    public String currencyName;
    @SerializedName("stateName")
    @Expose
    public String stateName;
    @SerializedName("gstStatus")
    @Expose
    public String gstStatus;
    @SerializedName("gstNumber")
    @Expose
    public String gstNumber;
    @SerializedName("shopCGST")
    @Expose
    public String shopCGST;
    @SerializedName("shopSGST")
    @Expose
    public String shopSGST;
    @SerializedName("panNumber")
    @Expose
    public String panNumber;
    @SerializedName("companyFssis")
    @Expose
    public String companyFssis;


    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNoOfTable() {
        return noOfTable;
    }

    public void setNoOfTable(String noOfTable) {
        this.noOfTable = noOfTable;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerMobile() {
        return customerMobile;
    }

    public void setCustomerMobile(String customerMobile) {
        this.customerMobile = customerMobile;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(String subTotal) {
        this.subTotal = subTotal;
    }

    public String getTotalGSTAmount() {
        return totalGSTAmount;
    }

    public void setTotalGSTAmount(String totalGSTAmount) {
        this.totalGSTAmount = totalGSTAmount;
    }

    public String getDiscount() {
        return discount;
    }

    public void setDiscount(String discount) {
        this.discount = discount;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getInvoiceOrderStatus() {
        return invoiceOrderStatus;
    }

    public void setInvoiceOrderStatus(String invoiceOrderStatus) {
        this.invoiceOrderStatus = invoiceOrderStatus;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getInvoiceStatus() {
        return invoiceStatus;
    }

    public void setInvoiceStatus(String invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }

    public String getInvoiceProductId() {
        return invoiceProductId;
    }

    public void setInvoiceProductId(String invoiceProductId) {
        this.invoiceProductId = invoiceProductId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(String productPrice) {
        this.productPrice = productPrice;
    }

    public String getProductUnit() {
        return productUnit;
    }

    public void setProductUnit(String productUnit) {
        this.productUnit = productUnit;
    }

    public String getProductCGST() {
        return productCGST;
    }

    public void setProductCGST(String productCGST) {
        this.productCGST = productCGST;
    }

    public String getProductSGST() {
        return productSGST;
    }

    public void setProductSGST(String productSGST) {
        this.productSGST = productSGST;
    }

    public String getProductQuantity() {
        return productQuantity;
    }

    public void setProductQuantity(String productQuantity) {
        this.productQuantity = productQuantity;
    }

    public String getProductStatus() {
        return productStatus;
    }

    public void setProductStatus(String productStatus) {
        this.productStatus = productStatus;
    }

    public String getInvoiceNetworkStatus() {
        return invoiceNetworkStatus;
    }

    public void setInvoiceNetworkStatus(String invoiceNetworkStatus) {
        this.invoiceNetworkStatus = invoiceNetworkStatus;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public String getCompanyMobile() {
        return companyMobile;
    }

    public void setCompanyMobile(String companyMobile) {
        this.companyMobile = companyMobile;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getTableStatus() {
        return tableStatus;
    }

    public void setTableStatus(String tableStatus) {
        this.tableStatus = tableStatus;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public String getGstStatus() {
        return gstStatus;
    }

    public void setGstStatus(String gstStatus) {
        this.gstStatus = gstStatus;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }

    public String getShopCGST() {
        return shopCGST;
    }

    public void setShopCGST(String shopCGST) {
        this.shopCGST = shopCGST;
    }

    public String getShopSGST() {
        return shopSGST;
    }

    public void setShopSGST(String shopSGST) {
        this.shopSGST = shopSGST;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getCompanyFssis() {
        return companyFssis;
    }

    public void setCompanyFssis(String companyFssis) {
        this.companyFssis = companyFssis;
    }
}
