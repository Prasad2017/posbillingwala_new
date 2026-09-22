package com.pos_billingwala.Model;

public class PosTableResponse {
    private String tableId;
    private String tableNumber;
    private String tableName;
    private String tableTypeId;
    private String tableTypeName;
    private String capacity;
    private String areaId;
    private String areaName;
    private String tableActive;
    private String positionX;
    private String positionY;
    private String sortOrder;
    private String statusOverride;
    private String posTableNetworkStatus;
    private String organizationId;
    private String branchId;
    private String deviceId;

    // Runtime display (not persisted on master row)
    private String displayStatus = TableStatus.AVAILABLE;
    private float currentAmount;
    private int guestCount;
    private long sessionStartedAt;
    private String sessionId;
    private String unpaidInvoiceNumber;
    private boolean hasCartItems;
    private String joinedTableLabel;
    private float remainingAmount;
    private boolean printRetryAvailable;

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

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

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

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

    public String getTableActive() {
        return tableActive;
    }

    public void setTableActive(String tableActive) {
        this.tableActive = tableActive;
    }

    public String getPositionX() {
        return positionX;
    }

    public void setPositionX(String positionX) {
        this.positionX = positionX;
    }

    public String getPositionY() {
        return positionY;
    }

    public void setPositionY(String positionY) {
        this.positionY = positionY;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getStatusOverride() {
        return statusOverride;
    }

    public void setStatusOverride(String statusOverride) {
        this.statusOverride = statusOverride;
    }

    public String getPosTableNetworkStatus() {
        return posTableNetworkStatus;
    }

    public void setPosTableNetworkStatus(String posTableNetworkStatus) {
        this.posTableNetworkStatus = posTableNetworkStatus;
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

    public String getDisplayStatus() {
        return displayStatus;
    }

    public void setDisplayStatus(String displayStatus) {
        this.displayStatus = displayStatus;
    }

    public float getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(float currentAmount) {
        this.currentAmount = currentAmount;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(int guestCount) {
        this.guestCount = guestCount;
    }

    public long getSessionStartedAt() {
        return sessionStartedAt;
    }

    public void setSessionStartedAt(long sessionStartedAt) {
        this.sessionStartedAt = sessionStartedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUnpaidInvoiceNumber() {
        return unpaidInvoiceNumber;
    }

    public void setUnpaidInvoiceNumber(String unpaidInvoiceNumber) {
        this.unpaidInvoiceNumber = unpaidInvoiceNumber;
    }

    public boolean isHasCartItems() {
        return hasCartItems;
    }

    public void setHasCartItems(boolean hasCartItems) {
        this.hasCartItems = hasCartItems;
    }

    public String getJoinedTableLabel() {
        return joinedTableLabel;
    }

    public void setJoinedTableLabel(String joinedTableLabel) {
        this.joinedTableLabel = joinedTableLabel;
    }

    public float getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(float remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public boolean isPrintRetryAvailable() {
        return printRetryAvailable;
    }

    public void setPrintRetryAvailable(boolean printRetryAvailable) {
        this.printRetryAvailable = printRetryAvailable;
    }

    public String getDisplayCode() {
        if (tableName != null && !tableName.trim().isEmpty()) {
            return tableName.trim();
        }
        if (tableNumber != null && !tableNumber.trim().isEmpty()) {
            return "T" + tableNumber.trim();
        }
        return "T?";
    }

    public boolean isOccupied() {
        return displayStatus != null
                && !TableStatus.AVAILABLE.equals(displayStatus)
                && !TableStatus.BLOCKED.equals(displayStatus)
                && !TableStatus.RESERVED.equals(displayStatus);
    }
}
