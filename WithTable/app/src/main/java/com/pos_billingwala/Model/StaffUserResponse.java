package com.pos_billingwala.Model;

/** Local POS staff roster row (device multi-user + cloud sync). */
public class StaffUserResponse {

    private String staffId;
    private String localStaffId;
    private String staffName;
    private String staffRole;
    private String staffPin;
    private String staffActive;
    private String staffDeletedStatus;
    private String staffNetworkStatus;
    private String createdAt;

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getLocalStaffId() {
        return localStaffId;
    }

    public void setLocalStaffId(String localStaffId) {
        this.localStaffId = localStaffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getStaffRole() {
        return staffRole;
    }

    public void setStaffRole(String staffRole) {
        this.staffRole = staffRole;
    }

    public String getStaffPin() {
        return staffPin;
    }

    public void setStaffPin(String staffPin) {
        this.staffPin = staffPin;
    }

    public String getStaffActive() {
        return staffActive;
    }

    public void setStaffActive(String staffActive) {
        this.staffActive = staffActive;
    }

    public String getStaffDeletedStatus() {
        return staffDeletedStatus;
    }

    public void setStaffDeletedStatus(String staffDeletedStatus) {
        this.staffDeletedStatus = staffDeletedStatus;
    }

    public String getStaffNetworkStatus() {
        return staffNetworkStatus;
    }

    public void setStaffNetworkStatus(String staffNetworkStatus) {
        this.staffNetworkStatus = staffNetworkStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return !"0".equals(staffActive) && !"1".equals(staffDeletedStatus);
    }
}
