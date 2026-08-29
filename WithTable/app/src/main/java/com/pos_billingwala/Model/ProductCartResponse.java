package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ProductCartResponse {

    @SerializedName("cartId")
    @Expose
    public String cartId;
    @SerializedName("productId")
    @Expose
    public String productId;
    @SerializedName("productName")
    @Expose
    public String productName;
    @SerializedName("productOldPrice")
    @Expose
    public String productOldPrice;
    @SerializedName("productNewPrice")
    @Expose
    public String productNewPrice;
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
    @SerializedName("cartDiscount")
    @Expose
    public String cartDiscount;
    @SerializedName("cartDiscountType")
    @Expose
    public String cartDiscountType;
    @SerializedName("noOfTable")
    @Expose
    public String noOfTable;
    @SerializedName("cartOrderStatus")
    @Expose
    public String cartOrderStatus;
    @SerializedName("cartStatus")
    @Expose
    public String cartStatus;
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
    @SerializedName("openPrice")
    @Expose
    public String openPrice;
    @SerializedName("cartItemType")
    @Expose
    public String cartItemType;
    @SerializedName("comboId")
    @Expose
    public String comboId;
    @SerializedName("snapshotComboComponents")
    @Expose
    public String snapshotComboComponents;


    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
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

    public String getProductOldPrice() {
        return productOldPrice;
    }

    public void setProductOldPrice(String productOldPrice) {
        this.productOldPrice = productOldPrice;
    }

    public String getProductNewPrice() {
        return productNewPrice;
    }

    public void setProductNewPrice(String productNewPrice) {
        this.productNewPrice = productNewPrice;
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

    public String getCartDiscount() {
        return cartDiscount;
    }

    public void setCartDiscount(String cartDiscount) {
        this.cartDiscount = cartDiscount;
    }

    public String getCartDiscountType() {
        return cartDiscountType;
    }

    public void setCartDiscountType(String cartDiscountType) {
        this.cartDiscountType = cartDiscountType;
    }

    public String getNoOfTable() {
        return noOfTable;
    }

    public void setNoOfTable(String noOfTable) {
        this.noOfTable = noOfTable;
    }

    public String getCartOrderStatus() {
        return cartOrderStatus;
    }

    public void setCartOrderStatus(String cartOrderStatus) {
        this.cartOrderStatus = cartOrderStatus;
    }

    public String getCartStatus() {
        return cartStatus;
    }

    public void setCartStatus(String cartStatus) {
        this.cartStatus = cartStatus;
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

    public String getOpenPrice() {
        return openPrice != null ? openPrice : "off";
    }

    public void setOpenPrice(String openPrice) {
        this.openPrice = openPrice;
    }

    public boolean isOpenPrice() {
        return openPrice != null && "on".equalsIgnoreCase(openPrice.trim());
    }

    public String getCartItemType() {
        return cartItemType;
    }

    public void setCartItemType(String cartItemType) {
        this.cartItemType = cartItemType;
    }

    public String getComboId() {
        return comboId;
    }

    public void setComboId(String comboId) {
        this.comboId = comboId;
    }

    public String getSnapshotComboComponents() {
        return snapshotComboComponents;
    }

    public void setSnapshotComboComponents(String snapshotComboComponents) {
        this.snapshotComboComponents = snapshotComboComponents;
    }

    /** Frozen line label for print / reports (includes portion when set). */
    public String getDisplayLineName() {
        boolean combo = cartItemType != null && "COMBO".equalsIgnoreCase(cartItemType.trim());
        return BillLineSnapshot.displayName(productName, snapshotProductName, combo ? null : portionName,
                snapshotComboComponents);
    }

    /** Frozen unit price charged on this line. */
    public String getResolvedLinePrice() {
        return BillLineSnapshot.linePrice(snapshotLinePrice, productNewPrice, productOldPrice);
    }
}
