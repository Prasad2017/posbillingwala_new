package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DeviceMonitorResponse {
    @SerializedName("customerId")
    @Expose
    private String customerId;
    @SerializedName("shopName")
    @Expose
    private String shopName;
    @SerializedName("ownerName")
    @Expose
    private String ownerName;
    @SerializedName("contact_number")
    @Expose
    private String contactNumber;
    @SerializedName("licenseKey")
    @Expose
    private String licenseKey;
    @SerializedName("licenseStatus")
    @Expose
    private String licenseStatus;
    @SerializedName("branchLabel")
    @Expose
    private String branchLabel;
    @SerializedName("android_device_id")
    @Expose
    private String androidDeviceId;
    @SerializedName("android_device_name")
    @Expose
    private String androidDeviceName;
    @SerializedName("deviceBoundAt")
    @Expose
    private String deviceBoundAt;
    @SerializedName("lastLoginAt")
    @Expose
    private String lastLoginAt;
    @SerializedName("lastSeenAt")
    @Expose
    private String lastSeenAt;
    @SerializedName("lastSeenLabel")
    @Expose
    private String lastSeenLabel;
    @SerializedName("connectionStatus")
    @Expose
    private String connectionStatus;
    @SerializedName("expiryDate")
    @Expose
    private String expiryDate;

    public String getCustomerId() {
        return customerId;
    }

    public String getShopName() {
        return shopName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public String getLicenseStatus() {
        return licenseStatus;
    }

    public String getBranchLabel() {
        return branchLabel;
    }

    public String getAndroidDeviceId() {
        return androidDeviceId;
    }

    public String getAndroidDeviceName() {
        return androidDeviceName;
    }

    public String getDeviceBoundAt() {
        return deviceBoundAt;
    }

    public String getLastLoginAt() {
        return lastLoginAt;
    }

    public String getLastSeenAt() {
        return lastSeenAt;
    }

    public String getLastSeenLabel() {
        return lastSeenLabel;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public boolean isOnline() {
        return connectionStatus != null && "ONLINE".equalsIgnoreCase(connectionStatus);
    }
}
