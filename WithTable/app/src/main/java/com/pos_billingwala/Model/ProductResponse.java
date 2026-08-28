package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ProductResponse {

    @SerializedName("productNetworkStatus")
    @Expose
    public String productNetworkStatus;
    @SerializedName("productInventoryQuantity")
    @Expose
    public String productInventoryQuantity;
    @SerializedName("productCartQuantity")
    @Expose
    public String productCartQuantity;
    @SerializedName("productDeletedStatus")
    @Expose
    public String productDeletedStatus;
    @SerializedName("productId")
    @Expose
    String productId;
    @SerializedName("categoryId")
    @Expose
    String categoryId;
    @SerializedName("categoryName")
    @Expose
    String categoryName;
    @SerializedName("subcategoryId")
    @Expose
    String subcategoryId;
    @SerializedName("subcategoryName")
    @Expose
    String subcategoryName;
    @SerializedName("productName")
    @Expose
    String productName;
    @SerializedName("productCode")
    @Expose
    String productCode;
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

    public String getSubcategoryId() {
        return subcategoryId;
    }

    public void setSubcategoryId(String subcategoryId) {
        this.subcategoryId = subcategoryId;
    }

    public String getSubcategoryName() {
        return subcategoryName;
    }

    public void setSubcategoryName(String subcategoryName) {
        this.subcategoryName = subcategoryName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
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

    public String getProductCartQuantity() {
        return productCartQuantity;
    }

    public void setProductCartQuantity(String productCartQuantity) {
        this.productCartQuantity = productCartQuantity;
    }

    public String getProductDeletedStatus() {
        return productDeletedStatus;
    }

    public void setProductDeletedStatus(String productDeletedStatus) {
        this.productDeletedStatus = productDeletedStatus;
    }
}
