package com.pos_billingwala.Model;

public class DiningAreaResponse {
    private String areaId;
    private String areaName;
    private String areaSortOrder;
    private String areaActive;
    private String areaNetworkStatus;
    private String organizationId;
    private String branchId;
    private String deviceId;

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getAreaSortOrder() {
        return areaSortOrder;
    }

    public void setAreaSortOrder(String areaSortOrder) {
        this.areaSortOrder = areaSortOrder;
    }

    public String getAreaActive() {
        return areaActive;
    }

    public void setAreaActive(String areaActive) {
        this.areaActive = areaActive;
    }

    public String getAreaNetworkStatus() {
        return areaNetworkStatus;
    }

    public void setAreaNetworkStatus(String areaNetworkStatus) {
        this.areaNetworkStatus = areaNetworkStatus;
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
