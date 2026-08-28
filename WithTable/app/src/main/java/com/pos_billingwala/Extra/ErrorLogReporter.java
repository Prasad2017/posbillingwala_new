package com.pos_billingwala.Extra;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.pos_billingwala.BuildConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds rich Admin error payloads from real device/SDK/API exceptions.
 * Preserves original_* fields; generates a separate human-readable summary.
 */
public final class ErrorLogReporter {

    private static final String TAG = "POS_ERR_REP";
    private static final int MAX_ACTIONS = 10;
    private static final int MAX_BREADCRUMBS = 20;

    private static final ArrayDeque<String> userActions = new ArrayDeque<>();
    private static final ArrayDeque<String> breadcrumbs = new ArrayDeque<>();

    private static volatile Context appContext;
    private static volatile String lastUserAction = "";

    private ErrorLogReporter() {
    }

    public static void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
            ErrorLogQueue.init(appContext);
        }
    }

    public static void recordUserAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            return;
        }
        String a = action.trim();
        lastUserAction = a;
        synchronized (userActions) {
            userActions.addLast(a);
            while (userActions.size() > MAX_ACTIONS) {
                userActions.removeFirst();
            }
        }
        addBreadcrumb(a);
    }

    public static void addBreadcrumb(String step) {
        if (step == null || step.trim().isEmpty()) {
            return;
        }
        synchronized (breadcrumbs) {
            breadcrumbs.addLast(step.trim());
            while (breadcrumbs.size() > MAX_BREADCRUMBS) {
                breadcrumbs.removeFirst();
            }
        }
    }

    public static String getLastUserAction() {
        return lastUserAction != null ? lastUserAction : "";
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
            String originalMsg;
            String exceptionClass = "";
            String stack = "";
            String errorCode;

            if (networkError != null) {
                originalMsg = preserveOriginalMessage(networkError);
                exceptionClass = networkError.getClass().getName();
                stack = stackTraceOf(networkError);
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

            ErrorLogPayload p = basePayload("API", severity, category, summary);
            p.put("api_method", method != null ? method : "");
            p.put("api_url", LogSanitizer.sanitize(url, 1024));
            p.putInt("http_status", statusCode > 0 ? statusCode : null);
            p.put("request_body", safeRequest);
            p.put("response_body", safeResponse);
            p.putInt("request_size", requestBody != null ? requestBody.length() : 0);
            p.putInt("response_size", responseBody != null ? responseBody.length() : 0);
            p.putInt("request_duration_ms", (int) Math.min(durationMs, Integer.MAX_VALUE));
            p.put("original_error_message", LogSanitizer.sanitize(originalMsg));
            p.put("original_exception_class", exceptionClass);
            p.put("original_stack_trace", LogSanitizer.sanitize(stack));
            p.put("original_error_code", errorCode);
            p.put("original_api_response", safeResponse);
            p.put("user_action", preferAction("API call: " + (apiName != null ? apiName : "request")));
            p.put("what_happened", buildApiWhatHappened(method, apiName, statusCode, originalMsg));
            p.put("fingerprint", fingerprint("API", exceptionClass, originalMsg, url, statusCode));
            ErrorLogQueue.enqueue(p);
            addBreadcrumb("API " + (statusCode > 0 ? statusCode : errorCode) + " " + (apiName != null ? apiName : ""));
        } catch (Throwable t) {
            Log.e(TAG, "reportApiError failed: " + t.getMessage());
        }
    }

    public static void reportAppError(Throwable error, String context, String severity, boolean fatal) {
        try {
            if (error == null) {
                return;
            }
            String originalMsg = preserveOriginalMessage(error);
            String exceptionClass = error.getClass().getName();
            String stack = stackTraceOf(error);
            String category = ObservabilityPublic.categorize(error);
            String type = mapType(category, context);
            String sev = severity != null ? severity : (fatal ? "CRITICAL" : "ERROR");
            String summary = buildAppSummary(type, context, exceptionClass);

            ErrorLogPayload p = basePayload(type, sev, category, summary);
            fillOriginals(p, originalMsg, exceptionClass, stack, category);
            p.put("user_action", preferAction(context != null ? context : "Unhandled error"));
            p.put("what_happened", buildAppWhatHappened(context, originalMsg));
            p.put("fingerprint", fingerprint(type, exceptionClass, originalMsg,
                    Observability.getCurrentScreenName(), 0));
            if (fatal) {
                ErrorLogQueue.enqueueSync(p);
            } else {
                ErrorLogQueue.enqueue(p);
            }
            addBreadcrumb((fatal ? "FATAL " : "ERROR ") + exceptionClass);
        } catch (Throwable t) {
            Log.e(TAG, "reportAppError failed: " + t.getMessage());
        }
    }

    public static void reportPrinterError(Throwable error, String printerType, String printerModel,
                                          String connection, String printOperation) {
        try {
            String originalMsg = error != null ? preserveOriginalMessage(error) : "Printer error (no exception message)";
            String exceptionClass = error != null ? error.getClass().getName() : "PrinterError";
            String stack = error != null ? stackTraceOf(error) : "";
            String summary = "Printer connection failed while " +
                    (printOperation != null && !printOperation.isEmpty() ? printOperation : "printing");

            ErrorLogPayload p = basePayload("PRINTER", "ERROR", "printer", summary);
            fillOriginals(p, originalMsg, exceptionClass, stack, "printer");
            p.put("printer_type", nullToEmpty(printerType));
            p.put("printer_model", nullToEmpty(printerModel));
            p.put("printer_connection", nullToEmpty(connection));
            p.put("print_operation", nullToEmpty(printOperation));
            p.put("user_action", preferAction("User tapped \"Print Bill\""));
            p.put("what_happened", "Print command was started. Printer connection/operation failed.\n"
                    + "Original: " + originalMsg);
            p.put("fingerprint", fingerprint("PRINTER", exceptionClass, originalMsg, printOperation, 0));
            ErrorLogQueue.enqueue(p);
            addBreadcrumb("Printer error: " + exceptionClass);
        } catch (Throwable t) {
            Log.e(TAG, "reportPrinterError failed: " + t.getMessage());
        }
    }

    public static void reportDatabaseError(Throwable error, String operation) {
        try {
            if (error == null) {
                return;
            }
            String originalMsg = preserveOriginalMessage(error);
            String exceptionClass = error.getClass().getName();
            String stack = stackTraceOf(error);
            String summary = "Database error while " + (operation != null ? operation : "saving data");

            ErrorLogPayload p = basePayload("DATABASE", "ERROR", "database", summary);
            fillOriginals(p, originalMsg, exceptionClass, stack, "database");
            p.put("user_action", preferAction(operation != null ? operation : "Database operation"));
            p.put("what_happened", "Local database operation failed.\nOriginal: " + originalMsg);
            p.put("fingerprint", fingerprint("DATABASE", exceptionClass, originalMsg, operation, 0));
            ErrorLogQueue.enqueue(p);
        } catch (Throwable t) {
            Log.e(TAG, "reportDatabaseError failed: " + t.getMessage());
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
                // Keep generic only if that is literally what the SDK returned; still attach class.
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

    private static void fillOriginals(ErrorLogPayload p, String msg, String clazz, String stack, String code) {
        p.put("original_error_message", LogSanitizer.sanitize(msg));
        p.put("original_exception_class", clazz != null ? clazz : "");
        p.put("original_stack_trace", LogSanitizer.sanitize(stack));
        p.put("original_error_code", code != null ? code : "");
    }

    private static ErrorLogPayload basePayload(String type, String severity, String category, String summary) {
        Context ctx = appContext;
        ErrorLogPayload p = new ErrorLogPayload();
        String userId = ctx != null ? Common.getSavedUserData(ctx, "userId") : "";
        String shop = ctx != null ? Common.getSavedUserData(ctx, "shopName") : "";
        String branch = ctx != null ? Common.getSavedUserData(ctx, "branchLabel") : "";
        String userName = ctx != null ? Common.getSavedUserData(ctx, "userName") : "";
        String deviceName = Build.MANUFACTURER + " " + Build.MODEL;

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
        p.put("device_name", deviceName.trim());
        p.put("device_id", "");
        p.put("user_label", nullToEmpty(userName));
        p.put("screen_name", ScreenNames.friendly(
                Observability.getActivityName(), Observability.getFragmentName()));
        p.put("activity_name", Observability.getActivityName());
        p.put("fragment_name", Observability.getFragmentName());
        p.put("user_flow", buildUserFlow());
        p.put("breadcrumbs", buildBreadcrumbsJson());
        return p;
    }

    private static String preferAction(String fallback) {
        if (lastUserAction != null && !lastUserAction.isEmpty()) {
            return lastUserAction;
        }
        return fallback != null ? fallback : "";
    }

    private static String buildUserFlow() {
        List<String> steps;
        synchronized (breadcrumbs) {
            steps = new ArrayList<>(breadcrumbs);
        }
        if (steps.isEmpty()) {
            synchronized (userActions) {
                steps = new ArrayList<>(userActions);
            }
        }
        if (steps.isEmpty()) {
            return Observability.getCurrentScreenName();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) {
                sb.append("\n    ↓\n");
            }
            sb.append(steps.get(i));
        }
        return sb.toString();
    }

    private static String buildBreadcrumbsJson() {
        List<String> steps;
        synchronized (breadcrumbs) {
            steps = new ArrayList<>(breadcrumbs);
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(steps.get(i).replace("\"", "'")).append('"');
        }
        sb.append(']');
        return sb.toString();
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

    private static String buildApiWhatHappened(String method, String apiName, int statusCode, String original) {
        StringBuilder sb = new StringBuilder();
        sb.append("API request started");
        if (apiName != null) {
            sb.append(" (").append(apiName).append(')');
        }
        sb.append('.');
        if (statusCode > 0) {
            sb.append(" HTTP ").append(statusCode).append(" received.");
        } else {
            sb.append(" Network failure.");
        }
        if (original != null && !original.isEmpty()) {
            sb.append("\nOriginal: ").append(original);
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

    private static String buildAppSummary(String type, String context, String exceptionClass) {
        String simple = exceptionClass;
        int dot = exceptionClass != null ? exceptionClass.lastIndexOf('.') : -1;
        if (dot >= 0) {
            simple = exceptionClass.substring(dot + 1);
        }
        String screen = ScreenNames.friendly(Observability.getActivityName(), Observability.getFragmentName());
        if ("PRINTER".equals(type)) {
            return "Printer error on " + screen;
        }
        if ("DATABASE".equals(type)) {
            return "Database error while " + (context != null ? context : "saving");
        }
        return (simple != null ? simple : "Application error") + " on " + screen;
    }

    private static String buildAppWhatHappened(String context, String original) {
        StringBuilder sb = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            sb.append("Context: ").append(context).append(". ");
        }
        sb.append("Application encountered an exception.");
        if (original != null && !original.isEmpty()) {
            sb.append("\nOriginal: ").append(original);
        }
        return sb.toString();
    }

    private static String mapType(String category, String context) {
        String c = (category != null ? category : "").toLowerCase(Locale.US);
        String ctx = (context != null ? context : "").toLowerCase(Locale.US);
        if (c.contains("database") || c.contains("sqlite") || ctx.contains("db") || ctx.contains("invoice_db")) {
            return "DATABASE";
        }
        if (c.contains("printer") || ctx.contains("print") || ctx.contains("bluetooth")) {
            return "PRINTER";
        }
        if (c.contains("no_network") || c.contains("timeout") || c.contains("connection") || c.equals("api")) {
            return "NETWORK";
        }
        return "APPLICATION";
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

    /** Tiny bridge so ErrorLogReporter can reuse Observability categorization without cycles. */
    static final class ObservabilityPublic {
        static String categorize(Throwable error) {
            return Observability.categorizeErrorPublic(error);
        }
    }
}
