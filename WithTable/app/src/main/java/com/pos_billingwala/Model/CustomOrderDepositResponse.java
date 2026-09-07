package com.pos_billingwala.Model;

/** Bakery custom-order deposit row (local + cloud). */
public class CustomOrderDepositResponse {

    private String depositId;
    private String localDepositId;
    private String productName;
    private String orderNote;
    private String depositAmount;
    private String dueDate;
    private String photoFile;
    private String depositStatus;
    private String depositNetworkStatus;
    private String createdAt;

    public String getDepositId() {
        return depositId;
    }

    public void setDepositId(String depositId) {
        this.depositId = depositId;
    }

    public String getLocalDepositId() {
        return localDepositId;
    }

    public void setLocalDepositId(String localDepositId) {
        this.localDepositId = localDepositId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getOrderNote() {
        return orderNote;
    }

    public void setOrderNote(String orderNote) {
        this.orderNote = orderNote;
    }

    public String getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(String depositAmount) {
        this.depositAmount = depositAmount;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getPhotoFile() {
        return photoFile;
    }

    public void setPhotoFile(String photoFile) {
        this.photoFile = photoFile;
    }

    public String getDepositStatus() {
        return depositStatus;
    }

    public void setDepositStatus(String depositStatus) {
        this.depositStatus = depositStatus;
    }

    public String getDepositNetworkStatus() {
        return depositNetworkStatus;
    }

    public void setDepositNetworkStatus(String depositNetworkStatus) {
        this.depositNetworkStatus = depositNetworkStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
