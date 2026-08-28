package com.posbillingwala.admin.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ErrorLogDetail {

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
    @SerializedName("deviceId")
    @Expose
    private String deviceId;
    @SerializedName("userLabel")
    @Expose
    private String userLabel;
    @SerializedName("screenName")
    @Expose
    private String screenName;
    @SerializedName("activityName")
    @Expose
    private String activityName;
    @SerializedName("fragmentName")
    @Expose
    private String fragmentName;
    @SerializedName("userAction")
    @Expose
    private String userAction;
    @SerializedName("whatHappened")
    @Expose
    private String whatHappened;
    @SerializedName("userFlow")
    @Expose
    private String userFlow;
    @SerializedName("breadcrumbs")
    @Expose
    private String breadcrumbs;
    @SerializedName("apiMethod")
    @Expose
    private String apiMethod;
    @SerializedName("apiUrl")
    @Expose
    private String apiUrl;
    @SerializedName("httpStatus")
    @Expose
    private String httpStatus;
    @SerializedName("requestBody")
    @Expose
    private String requestBody;
    @SerializedName("responseBody")
    @Expose
    private String responseBody;
    @SerializedName("requestSize")
    @Expose
    private String requestSize;
    @SerializedName("responseSize")
    @Expose
    private String responseSize;
    @SerializedName("requestDurationMs")
    @Expose
    private String requestDurationMs;
    @SerializedName("printerType")
    @Expose
    private String printerType;
    @SerializedName("printerModel")
    @Expose
    private String printerModel;
    @SerializedName("printerConnection")
    @Expose
    private String printerConnection;
    @SerializedName("printOperation")
    @Expose
    private String printOperation;
    @SerializedName("originalErrorMessage")
    @Expose
    private String originalErrorMessage;
    @SerializedName("originalExceptionClass")
    @Expose
    private String originalExceptionClass;
    @SerializedName("originalStackTrace")
    @Expose
    private String originalStackTrace;
    @SerializedName("originalErrorCode")
    @Expose
    private String originalErrorCode;
    @SerializedName("originalApiResponse")
    @Expose
    private String originalApiResponse;
    @SerializedName("resolutionNotes")
    @Expose
    private String resolutionNotes;
    @SerializedName("resolvedAt")
    @Expose
    private String resolvedAt;
    @SerializedName("resolvedBy")
    @Expose
    private String resolvedBy;

    public String getId() { return id; }
    public String getFingerprint() { return fingerprint; }
    public String getOccurrenceCount() { return occurrenceCount; }
    public String getFirstSeenAt() { return firstSeenAt; }
    public String getLastSeenAt() { return lastSeenAt; }
    public String getErrorType() { return errorType; }
    public String getSeverity() { return severity; }
    public String getErrorCategory() { return errorCategory; }
    public String getSummary() { return summary; }
    public String getAppType() { return appType; }
    public String getAppVersion() { return appVersion; }
    public String getCustomerId() { return customerId; }
    public String getShopName() { return shopName; }
    public String getBranchLabel() { return branchLabel; }
    public String getDeviceName() { return deviceName; }
    public String getDeviceId() { return deviceId; }
    public String getUserLabel() { return userLabel; }
    public String getScreenName() { return screenName; }
    public String getActivityName() { return activityName; }
    public String getFragmentName() { return fragmentName; }
    public String getUserAction() { return userAction; }
    public String getWhatHappened() { return whatHappened; }
    public String getUserFlow() { return userFlow; }
    public String getBreadcrumbs() { return breadcrumbs; }
    public String getApiMethod() { return apiMethod; }
    public String getApiUrl() { return apiUrl; }
    public String getHttpStatus() { return httpStatus; }
    public String getRequestBody() { return requestBody; }
    public String getResponseBody() { return responseBody; }
    public String getRequestSize() { return requestSize; }
    public String getResponseSize() { return responseSize; }
    public String getRequestDurationMs() { return requestDurationMs; }
    public String getPrinterType() { return printerType; }
    public String getPrinterModel() { return printerModel; }
    public String getPrinterConnection() { return printerConnection; }
    public String getPrintOperation() { return printOperation; }
    public String getOriginalErrorMessage() { return originalErrorMessage; }
    public String getOriginalExceptionClass() { return originalExceptionClass; }
    public String getOriginalStackTrace() { return originalStackTrace; }
    public String getOriginalErrorCode() { return originalErrorCode; }
    public String getOriginalApiResponse() { return originalApiResponse; }
    public String getResolutionNotes() { return resolutionNotes; }
    public String getResolvedAt() { return resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}
