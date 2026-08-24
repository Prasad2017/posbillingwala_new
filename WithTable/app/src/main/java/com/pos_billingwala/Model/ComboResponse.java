package com.pos_billingwala.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ComboResponse {

    @SerializedName("comboId")
    @Expose
    private String comboId;
    @SerializedName("comboName")
    @Expose
    private String comboName;
    @SerializedName("comboCode")
    @Expose
    private String comboCode;
    @SerializedName("comboPrice")
    @Expose
    private String comboPrice;
    @SerializedName("comboCGST")
    @Expose
    private String comboCGST;
    @SerializedName("comboSGST")
    @Expose
    private String comboSGST;
    @SerializedName("comboWithGSTPrice")
    @Expose
    private String comboWithGSTPrice;
    @SerializedName("comboActiveStatus")
    @Expose
    private String comboActiveStatus;
    @SerializedName("comboDeletedStatus")
    @Expose
    private String comboDeletedStatus;
    @SerializedName("comboNetworkStatus")
    @Expose
    private String comboNetworkStatus;
    @SerializedName("comboStatus")
    @Expose
    private String comboStatus;
    @SerializedName("comboSortOrder")
    @Expose
    private String comboSortOrder;
    private String comboCartQuantity;

    public String getComboId() {
        return comboId;
    }

    public void setComboId(String comboId) {
        this.comboId = comboId;
    }

    public String getComboName() {
        return comboName;
    }

    public void setComboName(String comboName) {
        this.comboName = comboName;
    }

    public String getComboCode() {
        return comboCode;
    }

    public void setComboCode(String comboCode) {
        this.comboCode = comboCode;
    }

    public String getComboPrice() {
        return comboPrice;
    }

    public void setComboPrice(String comboPrice) {
        this.comboPrice = comboPrice;
    }

    public String getComboCGST() {
        return comboCGST;
    }

    public void setComboCGST(String comboCGST) {
        this.comboCGST = comboCGST;
    }

    public String getComboSGST() {
        return comboSGST;
    }

    public void setComboSGST(String comboSGST) {
        this.comboSGST = comboSGST;
    }

    public String getComboWithGSTPrice() {
        return comboWithGSTPrice;
    }

    public void setComboWithGSTPrice(String comboWithGSTPrice) {
        this.comboWithGSTPrice = comboWithGSTPrice;
    }

    public String getComboActiveStatus() {
        return comboActiveStatus;
    }

    public void setComboActiveStatus(String comboActiveStatus) {
        this.comboActiveStatus = comboActiveStatus;
    }

    public String getComboDeletedStatus() {
        return comboDeletedStatus;
    }

    public void setComboDeletedStatus(String comboDeletedStatus) {
        this.comboDeletedStatus = comboDeletedStatus;
    }

    public String getComboNetworkStatus() {
        return comboNetworkStatus;
    }

    public void setComboNetworkStatus(String comboNetworkStatus) {
        this.comboNetworkStatus = comboNetworkStatus;
    }

    public String getComboStatus() {
        return comboStatus;
    }

    public void setComboStatus(String comboStatus) {
        this.comboStatus = comboStatus;
    }

    public String getComboSortOrder() {
        return comboSortOrder;
    }

    public void setComboSortOrder(String comboSortOrder) {
        this.comboSortOrder = comboSortOrder;
    }

    public String getComboCartQuantity() {
        return comboCartQuantity;
    }

    public void setComboCartQuantity(String comboCartQuantity) {
        this.comboCartQuantity = comboCartQuantity;
    }

    public boolean isActive() {
        return comboActiveStatus == null || comboActiveStatus.trim().isEmpty()
                || "1".equals(comboActiveStatus.trim());
    }
}
