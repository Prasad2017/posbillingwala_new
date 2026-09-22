package com.pos_billingwala.Model;

public class KotResponse {
    private String kotId;
    private String sessionId;
    private String orderRoundId;
    private String kotNumber;
    private String tableNumber;
    private String printStatus;
    private String createdAt;
    private String kitchenName;
    private String organizationId;
    private String branchId;
    private String deviceId;

    public static final String PRINT_PENDING = "PENDING";
    public static final String PRINT_PRINTING = "PRINTING";
    public static final String PRINT_PRINTED = "PRINTED";
    public static final String PRINT_FAILED = "FAILED";

    public String getKotId() {
        return kotId;
    }

    public void setKotId(String kotId) {
        this.kotId = kotId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getOrderRoundId() {
        return orderRoundId;
    }

    public void setOrderRoundId(String orderRoundId) {
        this.orderRoundId = orderRoundId;
    }

    public String getKotNumber() {
        return kotNumber;
    }

    public void setKotNumber(String kotNumber) {
        this.kotNumber = kotNumber;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getPrintStatus() {
        return printStatus;
    }

    public void setPrintStatus(String printStatus) {
        this.printStatus = printStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getKitchenName() {
        return kitchenName;
    }

    public void setKitchenName(String kitchenName) {
        this.kitchenName = kitchenName;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
