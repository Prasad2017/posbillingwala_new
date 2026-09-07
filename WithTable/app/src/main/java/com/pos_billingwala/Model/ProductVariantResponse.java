package com.pos_billingwala.Model;

/**
 * Size / color / SKU row for fashion &amp; jewellery — not an F&amp;B portion.
 */
public class ProductVariantResponse {

    private String variantId;
    private String localVariantId;
    private String productId;
    private String productNetworkStatus;
    private String variantSize;
    private String variantColor;
    private String variantSku;
    private String variantPrice;
    private String variantDeletedStatus;
    private String variantSortOrder;
    private String variantNetworkStatus;

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public String getLocalVariantId() {
        return localVariantId;
    }

    public void setLocalVariantId(String localVariantId) {
        this.localVariantId = localVariantId;
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

    public String getVariantSize() {
        return variantSize;
    }

    public void setVariantSize(String variantSize) {
        this.variantSize = variantSize;
    }

    public String getVariantColor() {
        return variantColor;
    }

    public void setVariantColor(String variantColor) {
        this.variantColor = variantColor;
    }

    public String getVariantSku() {
        return variantSku;
    }

    public void setVariantSku(String variantSku) {
        this.variantSku = variantSku;
    }

    public String getVariantPrice() {
        return variantPrice;
    }

    public void setVariantPrice(String variantPrice) {
        this.variantPrice = variantPrice;
    }

    public String getVariantDeletedStatus() {
        return variantDeletedStatus;
    }

    public void setVariantDeletedStatus(String variantDeletedStatus) {
        this.variantDeletedStatus = variantDeletedStatus;
    }

    public String getVariantSortOrder() {
        return variantSortOrder;
    }

    public void setVariantSortOrder(String variantSortOrder) {
        this.variantSortOrder = variantSortOrder;
    }

    public String getVariantNetworkStatus() {
        return variantNetworkStatus;
    }

    public void setVariantNetworkStatus(String variantNetworkStatus) {
        this.variantNetworkStatus = variantNetworkStatus;
    }
}
