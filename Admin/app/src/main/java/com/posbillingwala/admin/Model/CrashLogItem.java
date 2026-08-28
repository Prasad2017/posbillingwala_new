package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CrashLogItem {
    @SerializedName("id") @Expose private String id;
    @SerializedName("errorTitle") @Expose private String errorTitle;
    @SerializedName("errorClass") @Expose private String errorClass;
    @SerializedName("appName") @Expose private String appName;
    @SerializedName("status") @Expose private String status;
    @SerializedName("deviceName") @Expose private String deviceName;
    @SerializedName("androidVersion") @Expose private String androidVersion;
    @SerializedName("appVersion") @Expose private String appVersion;
    @SerializedName("userName") @Expose private String userName;
    @SerializedName("userId") @Expose private String userId;
    @SerializedName("occurrences") @Expose private String occurrences;
    @SerializedName("stackTrace") @Expose private String stackTrace;
    @SerializedName("createdAt") @Expose private String createdAt;

    public String getId() { return id; }
    public String getErrorTitle() { return errorTitle; }
    public String getErrorClass() { return errorClass; }
    public String getAppName() { return appName; }
    public String getStatus() { return status; }
    public String getDeviceName() { return deviceName; }
    public String getAndroidVersion() { return androidVersion; }
    public String getAppVersion() { return appVersion; }
    public String getUserName() { return userName; }
    public String getUserId() { return userId; }
    public String getOccurrences() { return occurrences; }
    public String getStackTrace() { return stackTrace; }
    public String getCreatedAt() { return createdAt; }
}
