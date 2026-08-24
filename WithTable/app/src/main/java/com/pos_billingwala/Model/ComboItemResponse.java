package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.pos_billingwala.Extra.ComboValidator;

public class ComboItemResponse {

    @SerializedName("comboItemId")
    @Expose
    private String comboItemId;
    @SerializedName("comboId")
    @Expose
    private String comboId;
    @SerializedName("productId")
    @Expose
    private String productId;
    @SerializedName("portionId")
    @Expose
    private String portionId;
    @SerializedName("comboItemQuantity")
    @Expose
    private String comboItemQuantity;
    @SerializedName("comboItemSortOrder")
    @Expose
    private String comboItemSortOrder;
    @SerializedName("comboItemDeletedStatus")
    @Expose
    private String comboItemDeletedStatus;
    @SerializedName("comboItemNetworkStatus")
    @Expose
    private String comboItemNetworkStatus;
    @SerializedName("comboItemStatus")
    @Expose
    private String comboItemStatus;
    @SerializedName("comboNetworkStatus")
    @Expose
    private String comboNetworkStatus;
    @SerializedName("productNetworkStatus")
    @Expose
    private String productNetworkStatus;
    @SerializedName("portionNetworkStatus")
    @Expose
    private String portionNetworkStatus;
    @SerializedName("productName")
    @Expose
    private String productName;
    @SerializedName("portionName")
    @Expose
    private String portionName;
    @SerializedName("invoiceNumber")
    @Expose
    private String invoiceNumber;
    @SerializedName("invoiceProductNetworkStatus")
    @Expose
    private String invoiceProductNetworkStatus;
    @SerializedName("invoiceComboItemNetworkStatus")
    @Expose
    private String invoiceComboItemNetworkStatus;
    @SerializedName("invoiceComboItemStatus")
    @Expose
    private String invoiceComboItemStatus;
    @SerializedName("cartId")
    @Expose
    private String cartId;

    public String getComboItemId() {
        return comboItemId;
    }

    public void setComboItemId(String comboItemId) {
        this.comboItemId = comboItemId;
    }

    public String getComboId() {
        return comboId;
    }

    public void setComboId(String comboId) {
        this.comboId = comboId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getPortionId() {
        return portionId;
    }

    public void setPortionId(String portionId) {
        this.portionId = portionId;
    }

    public String getComboItemQuantity() {
        return comboItemQuantity;
    }

    public void setComboItemQuantity(String comboItemQuantity) {
        this.comboItemQuantity = comboItemQuantity;
    }

    public String getComboItemSortOrder() {
        return comboItemSortOrder;
    }

    public void setComboItemSortOrder(String comboItemSortOrder) {
        this.comboItemSortOrder = comboItemSortOrder;
    }

    public String getComboItemDeletedStatus() {
        return comboItemDeletedStatus;
    }

    public void setComboItemDeletedStatus(String comboItemDeletedStatus) {
        this.comboItemDeletedStatus = comboItemDeletedStatus;
    }

    public String getComboItemNetworkStatus() {
        return comboItemNetworkStatus;
    }

    public void setComboItemNetworkStatus(String comboItemNetworkStatus) {
        this.comboItemNetworkStatus = comboItemNetworkStatus;
    }

    public String getComboItemStatus() {
        return comboItemStatus;
    }

    public void setComboItemStatus(String comboItemStatus) {
        this.comboItemStatus = comboItemStatus;
    }

    public String getComboNetworkStatus() {
        return comboNetworkStatus;
    }

    public void setComboNetworkStatus(String comboNetworkStatus) {
        this.comboNetworkStatus = comboNetworkStatus;
    }

    public String getProductNetworkStatus() {
        return productNetworkStatus;
    }

    public void setProductNetworkStatus(String productNetworkStatus) {
        this.productNetworkStatus = productNetworkStatus;
    }

    public String getPortionNetworkStatus() {
        return portionNetworkStatus;
    }

    public void setPortionNetworkStatus(String portionNetworkStatus) {
        this.portionNetworkStatus = portionNetworkStatus;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getPortionName() {
        return portionName;
    }

    public void setPortionName(String portionName) {
        this.portionName = portionName;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getInvoiceProductNetworkStatus() {
        return invoiceProductNetworkStatus;
    }

    public void setInvoiceProductNetworkStatus(String invoiceProductNetworkStatus) {
        this.invoiceProductNetworkStatus = invoiceProductNetworkStatus;
    }

    public String getInvoiceComboItemNetworkStatus() {
        return invoiceComboItemNetworkStatus;
    }

    public void setInvoiceComboItemNetworkStatus(String invoiceComboItemNetworkStatus) {
        this.invoiceComboItemNetworkStatus = invoiceComboItemNetworkStatus;
    }

    public String getInvoiceComboItemStatus() {
        return invoiceComboItemStatus;
    }

    public void setInvoiceComboItemStatus(String invoiceComboItemStatus) {
        this.invoiceComboItemStatus = invoiceComboItemStatus;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    public String getDisplayLabel() {
        return ComboValidator.comboItemDisplayName(productName, portionName, comboItemQuantity);
    }

    public String getSnapshotLine() {
        return ComboValidator.formatComponentLine(productName, portionName, comboItemQuantity);
    }
}
