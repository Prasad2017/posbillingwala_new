package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CompanyResponse {

    @SerializedName("companyId")
    @Expose
    public String companyId;
    @SerializedName("companyName")
    @Expose
    public String companyName;
    @SerializedName("companyLogo")
    @Expose
    public String companyLogo;
    @SerializedName("paymentLogo")
    @Expose
    public String paymentLogo;
    @SerializedName("cashierName")
    @Expose
    public String cashierName;
    @SerializedName("companyMobile")
    @Expose
    public String companyMobile;
    @SerializedName("companyAddress")
    @Expose
    public String companyAddress;
    @SerializedName("currencyName")
    @Expose
    public String currencyName;
    @SerializedName("tableStatus")
    @Expose
    public String tableStatus;
    @SerializedName("noOfTable")
    @Expose
    public String noOfTable;
    @SerializedName("countryName")
    @Expose
    public String countryName;
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
    @SerializedName("companyStatus")
    @Expose
    public String companyStatus;


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

    public String getCompanyStatus() {
        return companyStatus;
    }

    public void setCompanyStatus(String companyStatus) {
        this.companyStatus = companyStatus;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    public String getTableStatus() {
        return tableStatus;
    }

    public void setTableStatus(String tableStatus) {
        this.tableStatus = tableStatus;
    }

    public String getNoOfTable() {
        return noOfTable;
    }

    public void setNoOfTable(String noOfTable) {
        this.noOfTable = noOfTable;
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

    public String getCompanyLogo() {
        return companyLogo;
    }

    public void setCompanyLogo(String companyLogo) {
        this.companyLogo = companyLogo;
    }

    public String getPaymentLogo() {
        return paymentLogo;
    }

    public void setPaymentLogo(String paymentLogo) {
        this.paymentLogo = paymentLogo;
    }
}
