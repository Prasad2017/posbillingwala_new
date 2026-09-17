package com.posbillingwala.admin.Model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class StoreOpsResponse {

    @SerializedName("status")
    public String status;
    @SerializedName("message")
    public String message;
    @SerializedName("userManagementEnabled")
    public String userManagementEnabled;
    @SerializedName("maxUsers")
    public String maxUsers;
    @SerializedName("maxDevices")
    public String maxDevices;
    @SerializedName("maxPrinters")
    public String maxPrinters;
    @SerializedName("staffCount")
    public String staffCount;
    @SerializedName("deviceCount")
    public String deviceCount;
    @SerializedName("printerCount")
    public String printerCount;
    @SerializedName("staffResponse")
    public List<Staff> staffResponse;
    @SerializedName("deviceResponse")
    public List<Device> deviceResponse;
    @SerializedName("printerResponse")
    public List<Printer> printerResponse;
    @SerializedName("routeResponse")
    public List<Route> routeResponse;

    public static class Staff {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("mobileNumber")
        public String mobileNumber;
        @SerializedName("role")
        public String role;
        @SerializedName("roleLabel")
        public String roleLabel;
        @SerializedName("status")
        public String status;
        @SerializedName("lastLoginAt")
        public String lastLoginAt;
        @SerializedName("allowedPermissions")
        public List<String> allowedPermissions;
    }

    public static class Device {
        @SerializedName("deviceId")
        public String deviceId;
        @SerializedName("deviceName")
        public String deviceName;
        @SerializedName("platform")
        public String platform;
        @SerializedName("status")
        public String status;
        @SerializedName("isPrintHost")
        public String isPrintHost;
        @SerializedName("lastSeenAt")
        public String lastSeenAt;
    }

    public static class Printer {
        @SerializedName("id")
        public String id;
        @SerializedName("printerName")
        public String printerName;
        @SerializedName("connectionType")
        public String connectionType;
        @SerializedName("paperSize")
        public String paperSize;
        @SerializedName("purpose")
        public String purpose;
        @SerializedName("status")
        public String status;
        @SerializedName("enabled")
        public String enabled;
    }

    public static class Route {
        @SerializedName("printerId")
        public String printerId;
        @SerializedName("documentType")
        public String documentType;
        @SerializedName("foodTypeCode")
        public String foodTypeCode;
        @SerializedName("categoryId")
        public String categoryId;
    }
}
