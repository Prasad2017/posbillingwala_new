package com.pos_billingwala.Model;

/** Wholesale / qty price tier for a product. */
public class ProductPriceTierResponse {

    private String tierId;
    private String localTierId;
    private String productId;
    private String productNetworkStatus;
    private String minQty;
    private String tierPrice;
    private String tierLabel;
    private String tierDeletedStatus;
    private String tierNetworkStatus;

    public String getTierId() {
        return tierId;
    }

    public void setTierId(String tierId) {
        this.tierId = tierId;
    }

    public String getLocalTierId() {
        return localTierId;
    }

    public void setLocalTierId(String localTierId) {
        this.localTierId = localTierId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductNetworkStatus() {
        return productNetworkStatus;
    }

    public void setProductNetworkStatus(String productNetworkStatus) {
        this.productNetworkStatus = productNetworkStatus;
    }

    public String getMinQty() {
        return minQty;
    }

    public void setMinQty(String minQty) {
        this.minQty = minQty;
    }

    public String getTierPrice() {
        return tierPrice;
    }

    public void setTierPrice(String tierPrice) {
        this.tierPrice = tierPrice;
    }

    public String getTierLabel() {
        return tierLabel;
    }

    public void setTierLabel(String tierLabel) {
        this.tierLabel = tierLabel;
    }

    public String getTierDeletedStatus() {
        return tierDeletedStatus;
    }

    public void setTierDeletedStatus(String tierDeletedStatus) {
        this.tierDeletedStatus = tierDeletedStatus;
    }

    public String getTierNetworkStatus() {
        return tierNetworkStatus;
    }

    public void setTierNetworkStatus(String tierNetworkStatus) {
        this.tierNetworkStatus = tierNetworkStatus;
    }
}
