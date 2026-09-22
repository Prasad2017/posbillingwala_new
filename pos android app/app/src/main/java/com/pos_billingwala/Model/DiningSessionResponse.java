package com.pos_billingwala.Model;

public class DiningSessionResponse {
    private String sessionId;
    private String primaryTableNumber;
    private String joinedTableNumbers;
    private String sessionStatus;
    private String guestCount;
    private String startedAt;
    private String closedAt;
    private String customerName;
    private String customerMobile;
    private String waiterName;
    private String unpaidInvoiceNumber;
    private String sessionVersion;
    private String paidAmount;
    private String organizationId;
    private String branchId;
    private String deviceId;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPrimaryTableNumber() {
        return primaryTableNumber;
    }

    public void setPrimaryTableNumber(String primaryTableNumber) {
        this.primaryTableNumber = primaryTableNumber;
    }

    public String getJoinedTableNumbers() {
        return joinedTableNumbers;
    }

    public void setJoinedTableNumbers(String joinedTableNumbers) {
        this.joinedTableNumbers = joinedTableNumbers;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public String getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(String guestCount) {
        this.guestCount = guestCount;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(String closedAt) {
        this.closedAt = closedAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerMobile() {
        return customerMobile;
    }

    public void setCustomerMobile(String customerMobile) {
        this.customerMobile = customerMobile;
    }

    public String getWaiterName() {
        return waiterName;
    }

    public void setWaiterName(String waiterName) {
        this.waiterName = waiterName;
    }

    public String getUnpaidInvoiceNumber() {
        return unpaidInvoiceNumber;
    }

    public void setUnpaidInvoiceNumber(String unpaidInvoiceNumber) {
        this.unpaidInvoiceNumber = unpaidInvoiceNumber;
    }

    public String getSessionVersion() {
        return sessionVersion;
    }

    public void setSessionVersion(String sessionVersion) {
        this.sessionVersion = sessionVersion;
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

    public String getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(String paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String dineInHeaderLabel() {
        String primary = primaryTableNumber == null ? "?" : primaryTableNumber.trim();
        String joined = joinedTableNumbers == null ? "" : joinedTableNumbers.trim();
        if (joined.isEmpty()) {
            return "DINE-IN • T" + primary;
        }
        StringBuilder sb = new StringBuilder("DINE-IN • T").append(primary);
        for (String part : joined.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                sb.append(" + T").append(t);
            }
        }
        return sb.toString();
    }
}
