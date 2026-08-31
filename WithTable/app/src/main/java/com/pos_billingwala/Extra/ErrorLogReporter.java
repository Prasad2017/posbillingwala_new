package com.pos_billingwala.Extra;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.pos_billingwala.BuildConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds Admin error payloads for API failures, fatal crashes, ANRs, and low-memory kills only.
 */
public final class ErrorLogReporter {

    private static final String TAG = "POS_ERR_REP";
    private static final Pattern ANR_REASON = Pattern.compile(
            "Reason:\\s*(.+?)(?:\\n|$)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static volatile Context appContext;

    private ErrorLogReporter() {
    }

    public static void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
            ErrorLogQueue.init(appContext);
        }
    }

    /** No-op — breadcrumbs removed from server logs. */
    public static void addBreadcrumb(String step) {
    }

    /** No-op — user actions removed from server logs. */
    public static void recordUserAction(String action) {
    }

    public static void reportApiError(
            String method,
            String url,
            String apiName,
            int statusCode,
            String networkReason,
            String requestBody,
            String responseBody,
            long durationMs,
            Throwable networkError
    ) {
        try {
            if (ErrorLogUploader.isIngestUrl(url)) {
                return;
            }
            String safeRequest = LogSanitizer.sanitize(requestBody);
            String safeResponse = LogSanitizer.sanitize(responseBody);
            String safeUrl = LogSanitizer.sanitize(url, 1024);
            String originalMsg;
            String exceptionClass = "";
            String errorCode;

            if (networkError != null) {
                originalMsg = preserveOriginalMessage(networkError);
                exceptionClass = networkError.getClass().getName();
                errorCode = classifyNetworkCode(networkError);
            } else if (statusCode > 0) {
                originalMsg = buildHttpOriginalMessage(statusCode, networkReason, safeResponse);
                exceptionClass = "HttpException";
                errorCode = String.valueOf(statusCode);
            } else {
                originalMsg = networkReason != null ? networkReason : "";
                errorCode = "NETWORK";
            }

            String category = classifyHttpOrNetwork(statusCode, networkError);
            String severity = severityForHttpOrNetwork(statusCode, networkError);
            String summary = buildApiSummary(statusCode, apiName, method);

            ErrorLogPayload p = baseUserDevicePayload("API", severity, category, summary);
            p.put("api_method", method != null ? method : "");
            p.put("api_url", safeUrl);
            p.putInt("http_status", statusCode > 0 ? statusCode : null);
            p.put("request_body", safeRequest);
            p.put("response_body", safeResponse);
            p.putInt("request_size", requestBody != null ? requestBody.length() : 0);
            p.putInt("response_size", responseBody != null ? responseBody.length() : 0);
            p.putInt("request_duration_ms", (int) Math.min(durationMs, Integer.MAX_VALUE));
            p.put("original_error_message", LogSanitizer.sanitize(originalMsg));
            p.put("original_exception_class", exceptionClass);
            p.put("original_error_code", errorCode);
            p.put("original_api_response", safeResponse);
            p.put("what_happened", buildApiWhatHappened(method, apiName, statusCode, safeUrl, originalMsg));
            p.put("fingerprint", fingerprint("API", exceptionClass, originalMsg, safeUrl, statusCode));
            ErrorLogQueue.enqueue(p);
        } catch (Throwable t) {
            Log.e(TAG, "reportApiError failed: " + t.getMessage());
        }
    }

    /** Fatal uncaught crash — device info, user details, and stack trace only. */
    public static void reportFatalCrash(Throwable error) {
        try {
            if (error == null) {
                return;
            }
            String originalMsg = preserveOriginalMessage(error);
            String exceptionClass = error.getClass().getName();
            String stack = stackTraceOf(error);
            String category = ObservabilityPublic.categorize(error);
            String summary = buildCrashSummary(exceptionClass);

            ErrorLogPayload p = baseUserDevicePayload("CRASH", "CRITICAL", category, summary);
            fillCrashFields(p, originalMsg, exceptionClass, stack, category);
            p.put("what_happened", buildCrashWhatHappened(exceptionClass, originalMsg, stack));
            p.put("fingerprint", fingerprint("CRASH", exceptionClass, originalMsg, "fatal", 0));
            boolean saved = ErrorLogQueue.enqueueSync(p);
            Context ctx = appContext;
            if (saved && ctx != null) {
                int uploaded = ErrorLogUploader.flushPendingSync(ctx, 4000L);
                ErrorLogFlushScheduler.schedule(ctx);
                Log.i(TAG, "Fatal crash saved=" + saved + " uploaded=" + uploaded);
            }
        } catch (Throwable t) {
            Log.e(TAG, "reportFatalCrash failed: " + t.getMessage());
        }
    }

    /**
     * System process exit from ApplicationExitInfo: Java crash, native crash, ANR, or low memory only.
     */
    public static void reportProcessExit(String errorType, String category, String exceptionClass,
                                         String description, String trace, long timestampMs,
                                         int pid, int reasonCode, int statusCode, String severity) {
        try {
            String type = normalizeProcessExitType(errorType, reasonCode);
            if (type == null) {
                return;
            }
            String clazz = exceptionClass != null && !exceptionClass.isEmpty()
                    ? exceptionClass : "ProcessExit";
            String stack = trace != null ? trace : "";
            String originalMsg = buildProcessExitOriginal(type, description, stack, clazz);
            String cat = category != null ? category : type.toLowerCase(Locale.US);
            String summary = buildProcessExitSummary(type, clazz, originalMsg);
            String sev = severity != null && !severity.isEmpty() ? severity : "CRITICAL";

            ErrorLogPayload p = baseUserDevicePayload(type, sev, cat, summary);
            fillCrashFields(p, originalMsg, clazz, stack, cat);
            p.put("what_happened", buildProcessExitWhatHappened(type, clazz, originalMsg, pid,
                    reasonCode, statusCode, timestampMs, stack));
            p.put("fingerprint", fingerprint(type, clazz, originalMsg, "system_exit", reasonCode));
            ErrorLogQueue.enqueue(p);
        } catch (Throwable t) {
            Log.e(TAG, "reportProcessExit failed: " + t.getMessage());
        }
    }

    /** Preserve real SDK/OS/exception message — never substitute generics. */
    public static String preserveOriginalMessage(Throwable error) {
        if (error == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < 5) {
            if (depth > 0) {
                sb.append(" ← caused by ");
            }
            String msg = current.getMessage();
            if (msg != null && !msg.trim().isEmpty() && !LogSanitizer.isGenericMessage(msg)) {
                sb.append(msg.trim());
            } else if (msg != null && !msg.trim().isEmpty()) {
                sb.append(current.getClass().getSimpleName()).append(": ").append(msg.trim());
            } else {
                sb.append(current.getClass().getName()).append(" (no message)");
            }
            Throwable next = current.getCause();
            if (next == null || next == current) {
                break;
            }
            current = next;
            depth++;
        }
        return sb.toString();
    }

    public static String stackTraceOf(Throwable error) {
        if (error == null) {
            return "";
        }
        try {
            StringWriter sw = new StringWriter();
            error.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        } catch (Exception e) {
            return error.toString();
        }
    }

    /** Extract ANR reason from system description or trace dump. */
    public static String extractAnrReason(String description, String trace) {
        String fromTrace = extractAnrReasonFromText(trace);
        if (!fromTrace.isEmpty()) {
            return fromTrace;
        }
        String fromDesc = extractAnrReasonFromText(description);
        if (!fromDesc.isEmpty()) {
            return fromDesc;
        }
        if (description != null && !description.trim().isEmpty()) {
            return description.trim();
        }
        return "Application Not Responding — reason not available in trace";
    }

    private static String extractAnrReasonFromText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Matcher m = ANR_REASON.matcher(text);
        if (m.find()) {
            String reason = m.group(1).trim();
            if (!reason.isEmpty()) {
                return reason.length() > 512 ? reason.substring(0, 512) + "…" : reason;
            }
        }
        String lower = text.toLowerCase(Locale.US);
        if (lower.contains("input dispatching timed out")) {
            return "Input dispatching timed out — main thread blocked";
        }
        if (lower.contains("executing service")) {
            return "Service execution timed out";
        }
        if (lower.contains("broadcast of intent")) {
            return "Broadcast receiver timed out";
        }
        if (lower.contains("content provider")) {
            return "Content provider query timed out";
        }
        return "";
    }

    private static void fillCrashFields(ErrorLogPayload p, String msg, String clazz, String stack, String code) {
        p.put("original_error_message", LogSanitizer.sanitize(msg));
        p.put("original_exception_class", clazz != null ? clazz : "");
        p.put("original_stack_trace", LogSanitizer.sanitize(stack));
        p.put("original_error_code", code != null ? code : "");
    }

    private static ErrorLogPayload baseUserDevicePayload(String type, String severity, String category, String summary) {
        Context ctx = appContext;
        ErrorLogPayload p = new ErrorLogPayload();
        String userId = ctx != null ? Common.getSavedUserData(ctx, "userId") : "";
        String shop = ctx != null ? Common.getSavedUserData(ctx, "shopName") : "";
        String branch = ctx != null ? Common.getSavedUserData(ctx, "branchLabel") : "";
        String userName = ctx != null ? Common.getSavedUserData(ctx, "userName") : "";
        String deviceName = (Build.MANUFACTURER + " " + Build.MODEL + " (Android "
                + Build.VERSION.RELEASE + "/API " + Build.VERSION.SDK_INT + ")").trim();
        String deviceId = "";
        if (ctx != null) {
            try {
                String id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
                deviceId = id != null ? id : "";
            } catch (Throwable ignored) {
            }
        }

        p.put("userId", nullToEmpty(userId));
        p.put("error_type", type);
        p.put("severity", severity);
        p.put("error_category", nullToEmpty(category));
        p.put("summary", LogSanitizer.sanitize(summary, 512));
        p.put("app_type", "POS");
        p.put("app_version", BuildConfig.VERSION_NAME);
        p.put("customer_id", nullToEmpty(userId));
        p.put("shop_name", nullToEmpty(shop));
        p.put("branch_label", nullToEmpty(branch));
        p.put("device_name", deviceName);
        p.put("device_id", deviceId);
        p.put("user_label", nullToEmpty(userName));
        return p;
    }

    private static String normalizeProcessExitType(String errorType, int reasonCode) {
        if (errorType == null || errorType.isEmpty()) {
            return null;
        }
        switch (errorType) {
            case "CRASH":
            case "NATIVE_CRASH":
            case "ANR":
            case "LOW_MEMORY":
                return errorType;
            default:
                return null;
        }
    }

    private static String buildProcessExitOriginal(String type, String description, String trace, String clazz) {
        if ("ANR".equals(type)) {
            return extractAnrReason(description, trace);
        }
        if (description != null && !description.trim().isEmpty()) {
            return description.trim();
        }
        return friendlyExceptionName(clazz) + " (system process exit)";
    }

    private static String buildApiSummary(int statusCode, String apiName, String method) {
        if (statusCode >= 500) {
            return "HTTP " + statusCode + " — Server error on " + safeApi(apiName);
        }
        if (statusCode >= 400) {
            return "HTTP " + statusCode + " — Client/API error on " + safeApi(apiName);
        }
        if (statusCode <= 0) {
            return "Network error — " + (method != null ? method + " " : "") + safeApi(apiName);
        }
        return "API error — " + safeApi(apiName);
    }

    private static String safeApi(String apiName) {
        return apiName != null && !apiName.isEmpty() ? apiName : "request";
    }

    private static String buildApiWhatHappened(String method, String apiName, int statusCode,
                                               String url, String original) {
        StringBuilder sb = new StringBuilder();
        sb.append("API failure");
        if (method != null && !method.isEmpty()) {
            sb.append("\nMethod: ").append(method);
        }
        if (url != null && !url.isEmpty()) {
            sb.append("\nURL: ").append(url);
        }
        if (apiName != null && !apiName.isEmpty()) {
            sb.append("\nEndpoint: ").append(apiName);
        }
        if (statusCode > 0) {
            sb.append("\nHTTP status: ").append(statusCode);
        } else {
            sb.append("\nNetwork failure");
        }
        if (original != null && !original.isEmpty()) {
            sb.append("\nReason: ").append(original);
        }
        return sb.toString();
    }

    private static String buildHttpOriginalMessage(int statusCode, String reason, String response) {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP ").append(statusCode);
        if (reason != null && !reason.isEmpty() && !LogSanitizer.isGenericMessage(reason)) {
            sb.append(" — ").append(reason);
        }
        if (response != null && !response.isEmpty()) {
            sb.append(" | response=").append(response.length() > 500 ? response.substring(0, 500) + "…" : response);
        }
        return sb.toString();
    }

    private static String buildCrashSummary(String exceptionClass) {
        String simple = exceptionClass;
        int dot = exceptionClass != null ? exceptionClass.lastIndexOf('.') : -1;
        if (dot >= 0) {
            simple = exceptionClass.substring(dot + 1);
        }
        return friendlyExceptionName(simple) + " — app crashed";
    }

    private static String buildCrashWhatHappened(String exceptionClass, String original, String stack) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatal crash: ").append(friendlyExceptionName(exceptionClass));
        if (original != null && !original.isEmpty()) {
            sb.append("\nMessage: ").append(original);
        }
        Context ctx = appContext;
        if (ctx != null) {
            String mem = DeviceHealthMonitor.memorySnapshot(ctx);
            if (!mem.isEmpty()) {
                sb.append("\nDevice memory: ").append(mem);
            }
        }
        if (stack != null && !stack.isEmpty()) {
            sb.append("\n\nStack trace:\n").append(LogSanitizer.sanitize(stack, 8192));
        }
        return sb.toString();
    }

    private static String buildProcessExitSummary(String type, String exceptionClass, String description) {
        String name = friendlyExceptionName(exceptionClass);
        if ("ANR".equals(type)) {
            String reason = description != null ? description.trim() : "";
            if (!reason.isEmpty()) {
                return "ANR — " + (reason.length() > 120 ? reason.substring(0, 120) + "…" : reason);
            }
            return "App not responding (ANR)";
        }
        if ("NATIVE_CRASH".equals(type)) {
            return "Native crash — " + name;
        }
        if ("LOW_MEMORY".equals(type)) {
            return "App killed by OS — low memory";
        }
        return name + " — app crashed";
    }

    private static String buildProcessExitWhatHappened(String type, String exceptionClass,
                                                       String original, int pid, int reasonCode,
                                                       int statusCode, long timestampMs,
                                                       String trace) {
        StringBuilder sb = new StringBuilder();
        sb.append("System process exit");
        sb.append("\nType: ").append(type);
        sb.append("\nException: ").append(friendlyExceptionName(exceptionClass));
        if ("ANR".equals(type)) {
            sb.append("\nANR reason: ").append(original != null ? original : "unknown");
        } else if ("LOW_MEMORY".equals(type)) {
            sb.append("\nCause: OS Low Memory Killer reclaimed this process");
            Context ctx = appContext;
            if (ctx != null) {
                String mem = DeviceHealthMonitor.memorySnapshot(ctx);
                if (!mem.isEmpty()) {
                    sb.append("\nDevice memory: ").append(mem);
                }
            }
        } else if (original != null && !original.isEmpty()) {
            sb.append("\nMessage: ").append(original);
        }
        sb.append("\nReason code: ").append(reasonCode);
        sb.append("\nStatus: ").append(statusCode);
        sb.append("\nPID: ").append(pid);
        if (timestampMs > 0) {
            sb.append("\nExit time (ms): ").append(timestampMs);
        }
        if (trace != null && !trace.isEmpty() && !"ANR".equals(type)) {
            String head = trace.length() > 8192 ? trace.substring(0, 8192) + "…" : trace;
            sb.append("\n\nTrace:\n").append(head.trim());
        } else if ("ANR".equals(type) && trace != null && !trace.isEmpty()) {
            String anrHead = trace.length() > 4096 ? trace.substring(0, 4096) + "…" : trace;
            sb.append("\n\nANR trace (excerpt):\n").append(anrHead.trim());
        }
        return sb.toString();
    }

    private static String friendlyExceptionName(String exceptionClass) {
        if (exceptionClass == null || exceptionClass.isEmpty()) {
            return "Unknown crash";
        }
        int dot = exceptionClass.lastIndexOf('.');
        String simple = dot >= 0 ? exceptionClass.substring(dot + 1) : exceptionClass;
        if ("NullPointerException".equals(simple)) {
            return "Null pointer exception";
        }
        if ("OutOfMemoryError".equals(simple)) {
            return "Out of memory";
        }
        if ("LOW_MEMORY".equals(simple)) {
            return "System low-memory kill (LMK)";
        }
        return simple;
    }

    private static String classifyHttpOrNetwork(int statusCode, Throwable networkError) {
        if (networkError != null) {
            return classifyNetworkCode(networkError);
        }
        if (statusCode >= 500) {
            return "SERVER_ERROR";
        }
        if (statusCode >= 400) {
            return "CLIENT_API_ERROR";
        }
        return "HTTP_ERROR";
    }

    private static String classifyNetworkCode(Throwable error) {
        Throwable root = error;
        int d = 0;
        while (root.getCause() != null && root.getCause() != root && d < 8) {
            root = root.getCause();
            d++;
        }
        if (root instanceof java.net.UnknownHostException) {
            return "NO_INTERNET";
        }
        if (root instanceof java.net.SocketTimeoutException || root instanceof java.util.concurrent.TimeoutException) {
            return "TIMEOUT";
        }
        if (root instanceof java.net.ConnectException) {
            return "CONNECTION_ERROR";
        }
        String msg = root.getMessage() != null ? root.getMessage().toLowerCase(Locale.US) : "";
        if (msg.contains("unable to resolve host") || msg.contains("dns")) {
            return "DNS_ERROR";
        }
        return "CONNECTION_ERROR";
    }

    private static String severityForHttpOrNetwork(int statusCode, Throwable networkError) {
        if (networkError != null) {
            return "WARNING";
        }
        if (statusCode >= 500) {
            return "ERROR";
        }
        if (statusCode >= 400) {
            return "ERROR";
        }
        return "WARNING";
    }

    private static String fingerprint(String type, String clazz, String msg, String extra, int code) {
        String raw = type + "|" + nullToEmpty(clazz) + "|" + truncate(nullToEmpty(msg), 200)
                + "|" + truncate(nullToEmpty(extra), 120) + "|" + code;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    static final class ObservabilityPublic {
        static String categorize(Throwable error) {
            return Observability.categorizeErrorPublic(error);
        }
    }
}
