package com.posbillingwala.owner.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ProductResponse {

    @SerializedName("productId")
    @Expose
    String productId;
    @SerializedName("categoryId")
    @Expose
    String categoryId;
    @SerializedName("categoryName")
    @Expose
    String categoryName;
    @SerializedName("productCode")
    @Expose
    public String productCode;
    @SerializedName("productName")
    @Expose
    String productName;
    @SerializedName("productPrice")
    @Expose
    String productPrice;
    @SerializedName("productUnit")
    @Expose
    String productUnit;
    @SerializedName("productCGST")
    @Expose
    String productCGST;
    @SerializedName("productSGST")
    @Expose
    String productSGST;
    @SerializedName("productStatus")
    @Expose
    String productStatus;
    @SerializedName("productNetworkStatus")
    @Expose
    public String productNetworkStatus;
    @SerializedName("productInventoryQuantity")
    @Expose
    public String productInventoryQuantity;


    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public String getProductStatus() {
        return productStatus;
    }

    public void setProductStatus(String productStatus) {
        this.productStatus = productStatus;
    }

    public String getProductNetworkStatus() {
        return productNetworkStatus;
    }

    public void setProductNetworkStatus(String productNetworkStatus) {
        this.productNetworkStatus = productNetworkStatus;
    }

    public String getProductInventoryQuantity() {
        return productInventoryQuantity;
    }

    public void setProductInventoryQuantity(String productInventoryQuantity) {
        this.productInventoryQuantity = productInventoryQuantity;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }
}
