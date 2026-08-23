package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class InventoryResponse {

    @SerializedName("inventoryId")
    @Expose
    public String inventoryId;
    @SerializedName("productId")
    @Expose
    public String productId;
    @SerializedName("productName")
    @Expose
    public String productName;
    @SerializedName("productInventoryQuantity")
    @Expose
    public String productInventoryQuantity;
    @SerializedName("afterSaleInventoryQuantity")
    @Expose
    public String afterSaleInventoryQuantity;
    @SerializedName("saleInventoryQuantity")
    @Expose
    public String saleInventoryQuantity;
    @SerializedName("inventoryDate")
    @Expose
    public String inventoryDate;
    @SerializedName("inventoryNetworkStatus")
    @Expose
    public String inventoryNetworkStatus;
    @SerializedName("inventoryStatus")
    @Expose
    public String inventoryStatus;


    public String getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(String inventoryId) {
        this.inventoryId = inventoryId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductInventoryQuantity() {
        return productInventoryQuantity;
    }

    public void setProductInventoryQuantity(String productInventoryQuantity) {
        this.productInventoryQuantity = productInventoryQuantity;
    }

    public String getInventoryDate() {
        return inventoryDate;
    }

    public void setInventoryDate(String inventoryDate) {
        this.inventoryDate = inventoryDate;
    }

    public String getInventoryNetworkStatus() {
        return inventoryNetworkStatus;
    }

    public void setInventoryNetworkStatus(String inventoryNetworkStatus) {
        this.inventoryNetworkStatus = inventoryNetworkStatus;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public String getAfterSaleInventoryQuantity() {
        return afterSaleInventoryQuantity;
    }

    public void setAfterSaleInventoryQuantity(String afterSaleInventoryQuantity) {
        this.afterSaleInventoryQuantity = afterSaleInventoryQuantity;
    }

    public String getSaleInventoryQuantity() {
        return saleInventoryQuantity;
    }

    public void setSaleInventoryQuantity(String saleInventoryQuantity) {
        this.saleInventoryQuantity = saleInventoryQuantity;
    }
}
