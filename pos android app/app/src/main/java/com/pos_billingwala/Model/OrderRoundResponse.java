package com.pos_billingwala.Model;

public class OrderRoundResponse {
    private String orderRoundId;
    private String sessionId;
    private String roundNumber;
    private String createdAt;
    private String kotId;
    private String organizationId;
    private String branchId;
    private String deviceId;

    public String getOrderRoundId() {
        return orderRoundId;
    }

    public void setOrderRoundId(String orderRoundId) {
        this.orderRoundId = orderRoundId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(String roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getKotId() {
        return kotId;
    }

    public void setKotId(String kotId) {
        this.kotId = kotId;
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
