package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class InvoiceProductResponse {

    @SerializedName("invoiceProductId")
    @Expose
    public String invoiceProductId;
    @SerializedName("invoiceNumber")
    @Expose
    public String invoiceNumber;
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
    @SerializedName("invoiceProductStatus")
    @Expose
    public String invoiceProductStatus;
    @SerializedName("invoiceProductNetworkStatus")
    @Expose
    public String invoiceProductNetworkStatus;
    @SerializedName("portionId")
    @Expose
    public String portionId;
    @SerializedName("portionName")
    @Expose
    public String portionName;
    @SerializedName("snapshotProductName")
    @Expose
    public String snapshotProductName;
    @SerializedName("snapshotLinePrice")
    @Expose
    public String snapshotLinePrice;
    @SerializedName("invoiceItemType")
    @Expose
    public String invoiceItemType;
    @SerializedName("comboId")
    @Expose
    public String comboId;
    @SerializedName("comboNetworkStatus")
    @Expose
    public String comboNetworkStatus;
    @SerializedName("snapshotComboComponents")
    @Expose
    public String snapshotComboComponents;


    public String getInvoiceProductId() {
        return invoiceProductId;
    }

    public void setInvoiceProductId(String invoiceProductId) {
        this.invoiceProductId = invoiceProductId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
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
        return productCGST != null ? productCGST : "";
    }

    public void setProductCGST(String productCGST) {
        this.productCGST = productCGST;
    }

    public String getProductSGST() {
        return productSGST != null ? productSGST : "";
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

    public String getInvoiceProductNetworkStatus() {
        return invoiceProductNetworkStatus;
    }

    public void setInvoiceProductNetworkStatus(String invoiceProductNetworkStatus) {
        this.invoiceProductNetworkStatus = invoiceProductNetworkStatus;
    }

    public String getInvoiceProductStatus() {
        return invoiceProductStatus;
    }

    public void setInvoiceProductStatus(String invoiceProductStatus) {
        this.invoiceProductStatus = invoiceProductStatus;
    }

    public String getPortionId() {
        return portionId;
    }

    public void setPortionId(String portionId) {
        this.portionId = portionId;
    }

    public String getPortionName() {
        return portionName;
    }

    public void setPortionName(String portionName) {
        this.portionName = portionName;
    }

    public String getSnapshotProductName() {
        return snapshotProductName;
    }

    public void setSnapshotProductName(String snapshotProductName) {
        this.snapshotProductName = snapshotProductName;
    }

    public String getSnapshotLinePrice() {
        return snapshotLinePrice;
    }

    public void setSnapshotLinePrice(String snapshotLinePrice) {
        this.snapshotLinePrice = snapshotLinePrice;
    }

    public String getInvoiceItemType() {
        return invoiceItemType;
    }

    public void setInvoiceItemType(String invoiceItemType) {
        this.invoiceItemType = invoiceItemType;
    }

    public String getComboId() {
        return comboId;
    }

    public void setComboId(String comboId) {
        this.comboId = comboId;
    }

    public String getComboNetworkStatus() {
        return comboNetworkStatus;
    }

    public void setComboNetworkStatus(String comboNetworkStatus) {
        this.comboNetworkStatus = comboNetworkStatus;
    }

    public String getSnapshotComboComponents() {
        return snapshotComboComponents;
    }

    public void setSnapshotComboComponents(String snapshotComboComponents) {
        this.snapshotComboComponents = snapshotComboComponents;
    }

    public String getDisplayLineName() {
        boolean combo = invoiceItemType != null && "COMBO".equalsIgnoreCase(invoiceItemType.trim());
        return BillLineSnapshot.displayName(productName, snapshotProductName, combo ? null : portionName,
                snapshotComboComponents);
    }

    public String getResolvedLinePrice() {
        return BillLineSnapshot.linePrice(snapshotLinePrice, productPrice, null);
    }
}
