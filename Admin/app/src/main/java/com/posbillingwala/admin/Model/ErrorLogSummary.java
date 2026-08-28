package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ErrorLogSummary {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("fingerprint")
    @Expose
    private String fingerprint;
    @SerializedName("occurrenceCount")
    @Expose
    private String occurrenceCount;
    @SerializedName("firstSeenAt")
    @Expose
    private String firstSeenAt;
    @SerializedName("lastSeenAt")
    @Expose
    private String lastSeenAt;
    @SerializedName("errorType")
    @Expose
    private String errorType;
    @SerializedName("severity")
    @Expose
    private String severity;
    @SerializedName("errorCategory")
    @Expose
    private String errorCategory;
    @SerializedName("summary")
    @Expose
    private String summary;
    @SerializedName("appType")
    @Expose
    private String appType;
    @SerializedName("appVersion")
    @Expose
    private String appVersion;
    @SerializedName("customerId")
    @Expose
    private String customerId;
    @SerializedName("shopName")
    @Expose
    private String shopName;
    @SerializedName("branchLabel")
    @Expose
    private String branchLabel;
    @SerializedName("deviceName")
    @Expose
    private String deviceName;
    @SerializedName("screenName")
    @Expose
    private String screenName;
    @SerializedName("userAction")
    @Expose
    private String userAction;
    @SerializedName("apiMethodPath")
    @Expose
    private String apiMethodPath;
    @SerializedName("httpStatus")
    @Expose
    private String httpStatus;
    @SerializedName("originalExceptionClass")
    @Expose
    private String originalExceptionClass;
    @SerializedName("originalErrorCode")
    @Expose
    private String originalErrorCode;

    public String getId() {
        return id;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getOccurrenceCount() {
        return occurrenceCount;
    }

    public String getFirstSeenAt() {
        return firstSeenAt;
    }

    public String getLastSeenAt() {
        return lastSeenAt;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getSeverity() {
        return severity;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public String getSummary() {
        return summary;
    }

    public String getAppType() {
        return appType;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getShopName() {
        return shopName;
    }

    public String getBranchLabel() {
        return branchLabel;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getScreenName() {
        return screenName;
    }

    public String getUserAction() {
        return userAction;
    }

    public String getApiMethodPath() {
        return apiMethodPath;
    }

    public String getHttpStatus() {
        return httpStatus;
    }

    public String getOriginalExceptionClass() {
        return originalExceptionClass;
    }

    public String getOriginalErrorCode() {
        return originalErrorCode;
    }
}
