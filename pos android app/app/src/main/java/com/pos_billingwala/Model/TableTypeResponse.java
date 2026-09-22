package com.pos_billingwala.Model;

public class TableTypeResponse {
    private String tableTypeId;
    private String tableTypeName;
    private String defaultCapacity;
    private String tableTypeActive;
    private String tableTypeNetworkStatus;
    private String organizationId;
    private String branchId;
    private String deviceId;

    public String getTableTypeId() {
        return tableTypeId;
    }

    public void setTableTypeId(String tableTypeId) {
        this.tableTypeId = tableTypeId;
    }

    public String getTableTypeName() {
        return tableTypeName;
    }

    public void setTableTypeName(String tableTypeName) {
        this.tableTypeName = tableTypeName;
    }

    public String getDefaultCapacity() {
        return defaultCapacity;
    }

    public void setDefaultCapacity(String defaultCapacity) {
        this.defaultCapacity = defaultCapacity;
    }

    public String getTableTypeActive() {
        return tableTypeActive;
    }

    public void setTableTypeActive(String tableTypeActive) {
        this.tableTypeActive = tableTypeActive;
    }

    public String getTableTypeNetworkStatus() {
        return tableTypeNetworkStatus;
    }

    public void setTableTypeNetworkStatus(String tableTypeNetworkStatus) {
        this.tableTypeNetworkStatus = tableTypeNetworkStatus;
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
