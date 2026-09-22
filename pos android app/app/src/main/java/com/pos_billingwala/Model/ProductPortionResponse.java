package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Configurable price variant for a product (Half, Full, Kg, etc.).
 * Products with zero active portions keep using {@code product.productPrice}.
 */
public class ProductPortionResponse {

    @SerializedName("portionId")
    @Expose
    private String portionId;
    @SerializedName("productId")
    @Expose
    private String productId;
    @SerializedName("productNetworkStatus")
    @Expose
    private String productNetworkStatus;
    @SerializedName("portionName")
    @Expose
    private String portionName;
    @SerializedName("portionPrice")
    @Expose
    private String portionPrice;
    @SerializedName("portionSortOrder")
    @Expose
    private String portionSortOrder;
    @SerializedName("portionDeletedStatus")
    @Expose
    private String portionDeletedStatus;
    @SerializedName("portionNetworkStatus")
    @Expose
    private String portionNetworkStatus;
    @SerializedName("portionStatus")
    @Expose
    private String portionStatus;
    @SerializedName("portionMasterId")
    @Expose
    private String portionMasterId;
    @SerializedName("portionMasterNetworkStatus")
    @Expose
    private String portionMasterNetworkStatus;

    public String getPortionId() {
        return portionId;
    }

    public void setPortionId(String portionId) {
        this.portionId = portionId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getPortionName() {
        return portionName;
    }

    public void setPortionName(String portionName) {
        this.portionName = portionName;
    }

    public String getPortionPrice() {
        return portionPrice;
    }

    public void setPortionPrice(String portionPrice) {
        this.portionPrice = portionPrice;
    }

    public String getPortionSortOrder() {
        return portionSortOrder;
    }

    public void setPortionSortOrder(String portionSortOrder) {
        this.portionSortOrder = portionSortOrder;
    }

    public String getPortionDeletedStatus() {
        return portionDeletedStatus;
    }

    public void setPortionDeletedStatus(String portionDeletedStatus) {
        this.portionDeletedStatus = portionDeletedStatus;
    }

    public String getPortionNetworkStatus() {
        return portionNetworkStatus;
    }

    public void setPortionNetworkStatus(String portionNetworkStatus) {
        this.portionNetworkStatus = portionNetworkStatus;
    }

    public String getPortionStatus() {
        return portionStatus;
    }

    public void setPortionStatus(String portionStatus) {
        this.portionStatus = portionStatus;
    }

    public String getProductNetworkStatus() {
        return productNetworkStatus;
    }

    public void setProductNetworkStatus(String productNetworkStatus) {
        this.productNetworkStatus = productNetworkStatus;
    }

    public String getPortionMasterId() {
        return portionMasterId;
    }

    public void setPortionMasterId(String portionMasterId) {
        this.portionMasterId = portionMasterId;
    }

    public String getPortionMasterNetworkStatus() {
        return portionMasterNetworkStatus;
    }

    public void setPortionMasterNetworkStatus(String portionMasterNetworkStatus) {
        this.portionMasterNetworkStatus = portionMasterNetworkStatus;
    }
}
