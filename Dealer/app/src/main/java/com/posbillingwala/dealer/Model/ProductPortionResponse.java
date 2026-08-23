package com.posbillingwala.dealer.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ProductPortionResponse {

    @SerializedName("portionId")
    @Expose
    private String portionId;
    @SerializedName("productId")
    @Expose
    private String productId;
    @SerializedName("portionName")
    @Expose
    private String portionName;
    @SerializedName("portionPrice")
    @Expose
    private String portionPrice;
    @SerializedName("portionSortOrder")
    @Expose
    private String portionSortOrder;
    @SerializedName("portionNetworkStatus")
    @Expose
    private String portionNetworkStatus;

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

    public String getPortionNetworkStatus() {
        return portionNetworkStatus;
    }

    public void setPortionNetworkStatus(String portionNetworkStatus) {
        this.portionNetworkStatus = portionNetworkStatus;
    }
}
