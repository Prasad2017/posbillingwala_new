package com.pos_billingwala.Extra;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLException;

/**
 * App-wide crash + error analytics for ANY failure — UI, DB, print, sync, API, OOM, etc.
 * Every fatal crash and non-fatal is reported to Firebase Crashlytics and mirrored to Logcat
 * ({@code POS_OBS}) with screen, domain, reason, and stack so developers see the exact error.
 */
public final class Observability {

    public static final String TRACE_SAVE_INVOICE = "save_invoice_db";
    public static final String TRACE_OFFLINE_SYNC = "offline_sync_upload";
    public static final String TRACE_PRINT_BILL = "print_bill_bitmap";

    /** Logcat tag — filter with: adb logcat -s POS_OBS */
    public static final String TAG = "POS_OBS";

    /** Firebase Crashlytics max length for a single custom-key value. */
    private static final int CUSTOM_KEY_MAX = 1024;
    /** Max request/response body captured for API failure breadcrumbs. */
    private static final int API_BODY_CAPTURE_MAX = 65536;
    /** Max chars per Crashlytics log line before splitting into chunks. */
    private static final int LOG_CHUNK_SIZE = 8192;
    /** How many app stack frames to keep in crash keys / Logcat summary. */
    private static final int STACK_FRAME_LIMIT = 12;

    private static volatile String currentActivity = "AppStart";
    private static volatile String currentFragment = "";
    private static volatile boolean fatalHandlerInstalled;

    private Observability() {
    }

    public static void init(Application application) {
        try {
            // Fatal handler first so ANY uncaught crash (UI/DB/print/sync/API) is enriched
            // even if Crashlytics init is slow or partially fails.
            installFatalCrashHandler();

            ErrorLogReporter.init(application);
            ErrorLogQueue.init(application);

            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCrashlyticsCollectionEnabled(true);
            FirebasePerformance.getInstance().setPerformanceCollectionEnabled(true);

            loadStoredUserContext(application);
            registerActivityTracking(application);
            registerMemoryBreadcrumbs(application);
            // Re-wrap after Firebase so our enricher stays outermost and still chains to Crashlytics.
            installFatalCrashHandler();
            syncScreenToCrashlytics();
            ErrorLogQueue.flushAsync();
            Log.i(TAG, "Observability initialized | catches ALL uncaught crashes"
                    + " (UI, DB, print, sync, API, OOM, …) | screen=" + buildScreenName());
        } catch (Exception e) {
            Log.e(TAG, "Observability.init failed: " + describeThrowable(e), e);
            installFatalCrashHandler();
        }
    }

    /** Attach licence context for crash/ANR reports — no PII beyond existing login ids. */
    public static void setUserContext(String userId, String licenceKey) {
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            if (userId != null && !userId.trim().isEmpty()) {
                crashlytics.setUserId(userId.trim());
            }
            if (licenceKey != null && !licenceKey.trim().isEmpty()) {
                crashlytics.setCustomKey("licence_key", licenceKey.trim());
            }
        } catch (Exception e) {
            Log.e(TAG, "setUserContext failed: " + describeThrowable(e), e);
        }
    }

    /** Breadcrumb trail — visible in Crashlytics logs and Logcat before a crash. */
    public static void log(String message) {
        if (message == null) {
            return;
        }
        try {
            Log.d(TAG, message);
            FirebaseCrashlytics.getInstance().log(message);
            ErrorLogReporter.addBreadcrumb(message);
        } catch (Exception e) {
            Log.e(TAG, "log failed: " + describeThrowable(e), e);
        }
    }

    /** Record a user-facing action for Admin error context. */
    public static void recordUserAction(String action) {
        try {
            ErrorLogReporter.recordUserAction(action);
            log("USER_ACTION " + action);
        } catch (Exception e) {
            Log.e(TAG, "recordUserAction failed: " + describeThrowable(e), e);
        }
    }

    /**
     * Record a handled error with clear context for developers.
     *
     * @param error   the exception
     * @param context short label, e.g. {@code bluetooth_print}, {@code login_check}
     */
    public static void logNonFatal(Throwable error, String context) {
        if (error == null) {
            return;
        }
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            String reason = describeThrowable(error);
            String screen = buildScreenName();
            String safeContext = (context != null && !context.isEmpty()) ? context : "unknown";

            crashlytics.setCustomKey("failure_type", "non_fatal");
            crashlytics.setCustomKey("crash_context", safeContext);
            crashlytics.setCustomKey("crash_reason", truncate(reason, CUSTOM_KEY_MAX));
            crashlytics.setCustomKey("exception_type", error.getClass().getSimpleName());
            crashlytics.setCustomKey("error_category", categorizeError(error));
            syncScreenToCrashlytics();

            String line = "NON_FATAL [" + safeContext + "] on " + screen
                    + " | category=" + categorizeError(error)
                    + " | " + reason;
            Log.e(TAG, line, error);
            crashlytics.log(line);
            crashlytics.recordException(error);

            String ctxLower = safeContext.toLowerCase();
            if (ctxLower.contains("print") || ctxLower.contains("bluetooth")) {
                ErrorLogReporter.reportPrinterError(error, "Bluetooth Printer", "", "Bluetooth", safeContext);
            } else if (ctxLower.contains("db") || ctxLower.contains("sqlite") || ctxLower.contains("invoice")) {
                ErrorLogReporter.reportDatabaseError(error, safeContext);
            } else {
                ErrorLogReporter.reportAppError(error, safeContext, "ERROR", false);
            }
        } catch (Exception e) {
            Log.e(TAG, "logNonFatal failed while reporting: " + describeThrowable(e), e);
        }
    }

    /**
     * Retrofit {@code onFailure} helper — logs with API action name and returns a short
     * developer/user-facing description (network, timeout, etc.).
     *
     * @param error     callback throwable
     * @param apiAction e.g. {@code login_check}, {@code check_licence_expire}
     * @return clear one-line message suitable for logs or UI content text
     */
    public static String logCallbackFailure(Throwable error, String apiAction) {
        String message = describeNetworkOrApiError(error);
        String action = (apiAction != null && !apiAction.isEmpty()) ? apiAction : "api_callback";
        if (error != null) {
            logNonFatal(error, action);
            Log.e(TAG, "API_CALLBACK_FAIL [" + action + "] on " + buildScreenName()
                    + " | " + message
                    + " | detail=" + describeThrowable(error), error);
        } else {
            log("API_CALLBACK_FAIL [" + action + "] on " + buildScreenName() + " | " + message);
        }
        return message;
    }

    /** Called when an Activity becomes visible. */
    public static void setActivityScreen(String activityName) {
        currentActivity = (activityName != null && !activityName.isEmpty()) ? activityName : "unknown";
        currentFragment = "";
        syncScreenToCrashlytics();
    }

    /** Called when a Fragment inside MainActivity becomes visible. */
    public static void setFragmentScreen(String fragmentName) {
        currentFragment = (fragmentName != null) ? fragmentName : "";
        syncScreenToCrashlytics();
        try {
            ErrorLogReporter.addBreadcrumb("Opened "
                    + ScreenNames.friendly(currentActivity, currentFragment));
        } catch (Exception ignored) {
        }
    }

    public static String getCurrentScreenName() {
        return buildScreenName();
    }

    public static String getActivityName() {
        return currentActivity != null ? currentActivity : "";
    }

    public static String getFragmentName() {
        return currentFragment != null ? currentFragment : "";
    }

    /** Public wrapper for Admin error classification. */
    public static String categorizeErrorPublic(Throwable error) {
        return categorizeError(error);
    }

    /**
     * Record a failed API call with endpoint, URL, request, response, and current screen.
     * Also writes a clear multi-line Logcat block under {@link #TAG}.
     */
    public static void logApiFailure(
            String method,
            String url,
            String apiName,
            int statusCode,
            String reason,
            String requestBody,
            String responseBody
    ) {
        logApiFailure(method, url, apiName, statusCode, reason, requestBody, responseBody, 0L, null);
    }

    public static void logApiFailure(
            String method,
            String url,
            String apiName,
            int statusCode,
            String reason,
            String requestBody,
            String responseBody,
            long durationMs,
            Throwable networkError
    ) {
        try {
            if (ErrorLogUploader.isIngestUrl(url)) {
                return;
            }
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            String screen = buildScreenName();
            String safeMethod = method != null ? method : "UNKNOWN";
            String safeUrl = truncate(url, CUSTOM_KEY_MAX);
            String safeApi = apiName != null && !apiName.isEmpty() ? apiName : "unknown";
            String safeReason = reason != null && !reason.isEmpty() ? reason : "unknown";
            String safeRequest = requestBody != null ? requestBody : "";
            String safeResponse = responseBody != null ? responseBody : "";
            String category = statusCode > 0 ? "http_" + statusCode : "network";

            crashlytics.setCustomKey("failure_type", "api");
            crashlytics.setCustomKey("api_name", safeApi);
            crashlytics.setCustomKey("api_method", safeMethod);
            crashlytics.setCustomKey("api_url", safeUrl);
            crashlytics.setCustomKey("api_status_code", statusCode);
            crashlytics.setCustomKey("api_failure_reason", truncate(safeReason, CUSTOM_KEY_MAX));
            crashlytics.setCustomKey("api_request", truncate(safeRequest, CUSTOM_KEY_MAX));
            crashlytics.setCustomKey("api_response", truncate(safeResponse, CUSTOM_KEY_MAX));
            crashlytics.setCustomKey("error_category", category);
            syncScreenToCrashlytics();

            String summary = safeMethod + " " + safeApi
                    + (statusCode > 0 ? " → HTTP " + statusCode : " → NETWORK ERROR")
                    + " | screen=" + screen
                    + " | reason=" + truncate(safeReason, CUSTOM_KEY_MAX);

            String logBlock = "API_FAIL " + summary
                    + "\n  url=" + safeUrl
                    + "\n  request=" + truncate(safeRequest, 500)
                    + "\n  response=" + truncate(safeResponse, 500);

            Log.e(TAG, logBlock);
            crashlytics.log("API_FAIL " + summary);
            logChunked(crashlytics, "API_REQUEST", safeRequest);
            logChunked(crashlytics, "API_RESPONSE", safeResponse);
            crashlytics.recordException(new ApiFailureException(summary));

            ErrorLogReporter.reportApiError(
                    safeMethod, url, safeApi, statusCode, safeReason,
                    safeRequest, safeResponse, durationMs, networkError
            );
        } catch (Exception e) {
            Log.e(TAG, "logApiFailure failed: " + describeThrowable(e), e);
        }
    }

    public static Trace startTrace(String name) {
        try {
            Trace trace = FirebasePerformance.getInstance().newTrace(name);
            trace.start();
            return trace;
        } catch (Exception e) {
            Log.e(TAG, "startTrace failed: " + describeThrowable(e), e);
            return null;
        }
    }

    public static void stopTrace(Trace trace) {
        if (trace == null) {
            return;
        }
        try {
            trace.stop();
        } catch (Exception e) {
            Log.e(TAG, "stopTrace failed: " + describeThrowable(e), e);
        }
    }

    /**
     * Human-readable description for network / API callback errors
     * (suitable for developer logs and optional UI content).
     */
    public static String describeNetworkOrApiError(Throwable error) {
        if (error == null) {
            return "Unknown API failure (no exception details)";
        }
        Throwable root = rootCause(error);
        if (root instanceof UnknownHostException) {
            return "No internet / DNS failed — cannot reach server (" + root.getClass().getSimpleName() + ")";
        }
        if (root instanceof SocketTimeoutException || root instanceof TimeoutException) {
            return "Request timed out — server took too long to respond (" + root.getClass().getSimpleName() + ")";
        }
        if (root instanceof ConnectException) {
            return "Connection refused — server unreachable (" + root.getClass().getSimpleName() + ")";
        }
        if (root instanceof SSLException) {
            return "SSL/TLS error — secure connection failed (" + root.getClass().getSimpleName() + ")";
        }
        if (root instanceof java.io.IOException) {
            String msg = root.getMessage();
            if (msg != null && msg.toLowerCase().contains("unable to resolve host")) {
                return "No internet — unable to resolve host";
            }
            if (msg != null && msg.toLowerCase().contains("failed to connect")) {
                return "Failed to connect to server";
            }
            return "Network I/O error: " + describeThrowable(root);
        }
        return describeThrowable(error);
    }

    /**
     * Enrich ANY uncaught fatal crash (not only API) with screen, domain, stack, and clear reason.
     * Called for NullPointerException, SQLite errors, printer failures, OOM, ClassCast, etc.
     */
    static void enrichFatalCrash(Thread thread, Throwable error) {
        if (error == null) {
            return;
        }
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            String reason = describeThrowable(error);
            String screen = buildScreenName();
            String threadName = thread != null ? thread.getName() : Thread.currentThread().getName();
            boolean mainThread = thread != null && thread.getId() == android.os.Looper.getMainLooper().getThread().getId();
            String category = categorizeError(error);
            String domain = inferCrashDomain(error);
            String topFrames = formatTopStackFrames(error, STACK_FRAME_LIMIT);
            String crashWhere = findLikelyAppFrame(error);

            crashlytics.setCustomKey("failure_type", "fatal_crash");
            crashlytics.setCustomKey("crash_reason", truncate(reason, CUSTOM_KEY_MAX));
            crashlytics.setCustomKey("exception_type", error.getClass().getName());
            crashlytics.setCustomKey("crash_type", "fatal");
            crashlytics.setCustomKey("error_category", category);
            crashlytics.setCustomKey("crash_domain", domain);
            crashlytics.setCustomKey("crash_thread", threadName != null ? threadName : "unknown");
            crashlytics.setCustomKey("crash_is_main_thread", mainThread);
            crashlytics.setCustomKey("crash_where", truncate(crashWhere, CUSTOM_KEY_MAX));
            crashlytics.setCustomKey("crash_stack_top", truncate(topFrames, CUSTOM_KEY_MAX));
            syncScreenToCrashlytics();

            String header = "========== APP CRASH (FATAL) ==========";
            String line = "FATAL CRASH"
                    + " | screen=" + screen
                    + " | domain=" + domain
                    + " | category=" + category
                    + " | thread=" + threadName + (mainThread ? " (MAIN/UI)" : " (background)")
                    + " | where=" + crashWhere
                    + " | " + reason;
            String block = header
                    + "\n" + line
                    + "\n--- top stack ---\n" + topFrames
                    + "\n========================================";

            Log.e(TAG, block, error);
            crashlytics.log(line);
            logChunked(crashlytics, "CRASH_STACK", topFrames);
            ErrorLogReporter.reportAppError(error, "fatal_crash", "CRITICAL", true);
        } catch (Exception e) {
            // Last resort — never let enrichment itself hide the original crash.
            Log.e(TAG, "FATAL CRASH (enrichment failed): " + error, error);
            Log.e(TAG, "enrichFatalCrash failed: " + describeThrowable(e), e);
        }
    }

    /** @deprecated use {@link #enrichFatalCrash(Thread, Throwable)} */
    @Deprecated
    static void enrichFatalCrash(Throwable error) {
        enrichFatalCrash(Thread.currentThread(), error);
    }

    private static void loadStoredUserContext(Application application) {
        try {
            String userId = Common.getSavedUserData(application, "userId");
            String licenceKey = Common.getSavedUserData(application, "LicenceKey");
            setUserContext(userId, licenceKey);
        } catch (Exception e) {
            Log.e(TAG, "loadStoredUserContext failed: " + describeThrowable(e), e);
        }
    }

    private static void registerActivityTracking(Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, android.os.Bundle savedInstanceState) {
                log("lifecycle CREATE " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
                setActivityScreen(activity.getClass().getSimpleName());
                log("lifecycle RESUME " + buildScreenName());
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, android.os.Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                log("lifecycle DESTROY " + activity.getClass().getSimpleName());
            }
        });
    }

    private static void registerMemoryBreadcrumbs(Application application) {
        application.registerComponentCallbacks(new android.content.ComponentCallbacks2() {
            @Override
            public void onConfigurationChanged(android.content.res.Configuration newConfig) {
            }

            @Override
            public void onLowMemory() {
                log("SYSTEM low_memory — app may crash with OutOfMemoryError soon");
            }

            @Override
            public void onTrimMemory(int level) {
                if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
                        || level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
                    log("SYSTEM trim_memory level=" + level + " — memory pressure");
                }
            }
        });
    }

    /**
     * Global handler for ANY uncaught Throwable on ANY thread (main UI + background).
     * Covers NPE, SQLite, Bluetooth, ClassCast, OOM, InflateException, etc. — not only API.
     * Default handler applies to the main thread and all background threads.
     */
    private static void installFatalCrashHandler() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        if (previous instanceof AppCrashHandler) {
            fatalHandlerInstalled = true;
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler(new AppCrashHandler(previous));
        fatalHandlerInstalled = true;
        Log.i(TAG, "Global crash handler active — ALL uncaught app crashes will be logged");
    }

    /**
     * Named handler so we can detect re-install and avoid infinite wrapping.
     * Forwards to Firebase/system handler after enriching ANY crash type.
     */
    private static final class AppCrashHandler implements Thread.UncaughtExceptionHandler {
        private final Thread.UncaughtExceptionHandler previous;

        AppCrashHandler(Thread.UncaughtExceptionHandler previous) {
            this.previous = previous;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            try {
                enrichFatalCrash(thread, throwable);
            } catch (Throwable ignored) {
                // never block crash pipeline
            }
            if (previous != null && !(previous instanceof AppCrashHandler)) {
                previous.uncaughtException(thread, throwable);
            } else {
                Log.e(TAG, "Uncaught exception (no previous handler)", throwable);
                System.exit(2);
            }
        }
    }

    private static void syncScreenToCrashlytics() {
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            String screen = buildScreenName();
            crashlytics.setCustomKey("screen_name", screen);
            crashlytics.setCustomKey("activity_name", currentActivity);
            crashlytics.setCustomKey("fragment_name", currentFragment);
        } catch (Exception e) {
            Log.e(TAG, "syncScreenToCrashlytics failed: " + describeThrowable(e), e);
        }
    }

    private static String buildScreenName() {
        if (currentFragment != null && !currentFragment.isEmpty()) {
            return currentActivity + " > " + currentFragment;
        }
        return currentActivity;
    }

    private static void logChunked(FirebaseCrashlytics crashlytics, String prefix, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        String capped = truncate(content, API_BODY_CAPTURE_MAX);
        if (capped.length() <= LOG_CHUNK_SIZE) {
            crashlytics.log(prefix + " " + capped);
            return;
        }
        int totalChunks = (int) Math.ceil((double) capped.length() / LOG_CHUNK_SIZE);
        for (int i = 0; i < totalChunks; i++) {
            int start = i * LOG_CHUNK_SIZE;
            int end = Math.min(start + LOG_CHUNK_SIZE, capped.length());
            crashlytics.log(prefix + " [" + (i + 1) + "/" + totalChunks + "] " + capped.substring(start, end));
        }
    }

    private static String truncate(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "…[truncated]";
    }

    /** Full exception + cause chain for exact developer diagnosis. */
    public static String describeThrowable(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        StringBuilder sb = new StringBuilder();
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < 5) {
            if (depth > 0) {
                sb.append(" ← caused by ");
            }
            sb.append(current.getClass().getSimpleName());
            String message = current.getMessage();
            if (message != null && !message.trim().isEmpty()) {
                sb.append(": ").append(message.trim());
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

    private static Throwable rootCause(Throwable error) {
        Throwable current = error;
        int depth = 0;
        while (current.getCause() != null && current.getCause() != current && depth < 8) {
            current = current.getCause();
            depth++;
        }
        return current;
    }

    private static String categorizeError(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        Throwable root = rootCause(error);
        if (root instanceof UnknownHostException) {
            return "no_network";
        }
        if (root instanceof SocketTimeoutException || root instanceof TimeoutException) {
            return "timeout";
        }
        if (root instanceof ConnectException) {
            return "connection_refused";
        }
        if (root instanceof SSLException) {
            return "ssl";
        }
        if (root instanceof android.database.sqlite.SQLiteException
                || root instanceof android.database.SQLException) {
            return "database";
        }
        if (root instanceof android.view.InflateException) {
            return "layout_inflate";
        }
        if (root instanceof ClassCastException) {
            return "class_cast";
        }
        if (root instanceof IndexOutOfBoundsException) {
            return "index_oob";
        }
        if (root instanceof IllegalStateException) {
            return "illegal_state";
        }
        if (root instanceof IllegalArgumentException) {
            return "illegal_argument";
        }
        if (root instanceof SecurityException) {
            return "security";
        }
        if (root instanceof java.io.IOException) {
            return "io";
        }
        if (root instanceof NullPointerException) {
            return "npe";
        }
        if (root instanceof OutOfMemoryError) {
            return "oom";
        }
        if (root instanceof StackOverflowError) {
            return "stack_overflow";
        }
        if (root instanceof NoClassDefFoundError || root instanceof ClassNotFoundException) {
            return "missing_class";
        }
        if (error instanceof ApiFailureException) {
            return "api";
        }
        if (root instanceof Error) {
            return "jvm_error";
        }
        return root.getClass().getSimpleName();
    }

    /**
     * Best-effort domain from stack so developers instantly know WHERE the crash happened.
     * Examples: ui, database, bluetooth_print, retrofit, sync, billing, unknown.
     */
    private static String inferCrashDomain(Throwable error) {
        String stack = stackAsString(error).toLowerCase();
        if (stack.contains("sqlite") || stack.contains("posbillingwaladatabase")
                || stack.contains("android.database")) {
            return "database";
        }
        if (stack.contains("bluetooth") || stack.contains("woosim") || stack.contains("printer")
                || stack.contains("bluetoothprint")) {
            return "bluetooth_print";
        }
        if (stack.contains("retrofit") || stack.contains("okhttp") || stack.contains("apifailure")) {
            return "api_network";
        }
        if (stack.contains("offlinesync") || stack.contains("synchronize")
                || (stack.contains("offline") && stack.contains("network"))) {
            return "sync";
        }
        if (stack.contains("ads.") || stack.contains("admob") || stack.contains("gms.ads")) {
            return "ads";
        }
        if (stack.contains("fragment") || stack.contains("activity")
                || stack.contains("android.view") || stack.contains("inflate")) {
            return "ui";
        }
        if (stack.contains("com.pos_billingwala")) {
            return "app_code";
        }
        return "unknown";
    }

    private static String findLikelyAppFrame(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < 5) {
            StackTraceElement[] frames = current.getStackTrace();
            if (frames != null) {
                for (StackTraceElement frame : frames) {
                    if (frame == null) {
                        continue;
                    }
                    String cn = frame.getClassName();
                    if (cn != null && cn.startsWith("com.pos_billingwala")
                            && !cn.contains("Observability")) {
                        return frame.getClassName() + "." + frame.getMethodName()
                                + "(" + frame.getFileName() + ":" + frame.getLineNumber() + ")";
                    }
                }
            }
            Throwable next = current.getCause();
            if (next == null || next == current) {
                break;
            }
            current = next;
            depth++;
        }
        StackTraceElement[] top = error.getStackTrace();
        if (top != null && top.length > 0 && top[0] != null) {
            StackTraceElement f = top[0];
            return f.getClassName() + "." + f.getMethodName()
                    + "(" + f.getFileName() + ":" + f.getLineNumber() + ")";
        }
        return "unknown";
    }

    private static String formatTopStackFrames(Throwable error, int limit) {
        if (error == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Throwable current = error;
        int depth = 0;
        int written = 0;
        while (current != null && depth < 3 && written < limit) {
            if (depth > 0) {
                sb.append("Caused by: ").append(current.getClass().getName()).append('\n');
            }
            StackTraceElement[] frames = current.getStackTrace();
            if (frames != null) {
                for (int i = 0; i < frames.length && written < limit; i++) {
                    StackTraceElement f = frames[i];
                    if (f == null) {
                        continue;
                    }
                    sb.append("  at ").append(f.toString()).append('\n');
                    written++;
                }
            }
            Throwable next = current.getCause();
            if (next == null || next == current) {
                break;
            }
            current = next;
            depth++;
        }
        return sb.toString().trim();
    }

    private static String stackAsString(Throwable error) {
        if (error == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < 4) {
            sb.append(current.getClass().getName()).append(' ');
            StackTraceElement[] frames = current.getStackTrace();
            if (frames != null) {
                int n = Math.min(frames.length, 25);
                for (int i = 0; i < n; i++) {
                    if (frames[i] != null) {
                        sb.append(frames[i].getClassName()).append('.');
                    }
                }
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
}
