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
 * App-wide crash analytics. Server error logs are limited to:
 * API failures (url + request + response), fatal crashes, ANRs, and low-memory kills.
 */
public final class Observability {

    public static final String TRACE_SAVE_INVOICE = "save_invoice_db";
    public static final String TRACE_OFFLINE_SYNC = "offline_sync_upload";
    public static final String TRACE_PRINT_BILL = "print_bill_bitmap";

    /** Logcat tag — filter with: adb logcat -s POS_OBS */
    public static final String TAG = "POS_OBS";

    private static final int CUSTOM_KEY_MAX = 1024;
    private static final int API_BODY_CAPTURE_MAX = 65536;
    private static final int LOG_CHUNK_SIZE = 8192;
    private static final int STACK_FRAME_LIMIT = 12;

    private static volatile String currentActivity = "AppStart";
    private static volatile String currentFragment = "";
    private static volatile boolean fatalHandlerInstalled;

    private Observability() {
    }

    public static void init(Application application) {
        try {
            installFatalCrashHandler();

            ErrorLogReporter.init(application);
            ErrorLogQueue.init(application);

            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCrashlyticsCollectionEnabled(true);
            FirebasePerformance.getInstance().setPerformanceCollectionEnabled(true);

            loadStoredUserContext(application);
            registerActivityTracking(application);
            DeviceHealthMonitor.init(application);
            installFatalCrashHandler();
            ErrorLogQueue.flushAsync();
            if (ErrorLogQueue.pendingCount(application) > 0) {
                ErrorLogFlushScheduler.schedule(application);
            }
            ProcessExitLogCollector.collectAsync(application);
            Log.i(TAG, "Observability initialized — logs: API failure, crash, ANR, low memory");
        } catch (Exception e) {
            Log.e(TAG, "Observability.init failed: " + describeThrowable(e), e);
            installFatalCrashHandler();
        }
    }

    public static void setUserContext(String userId, String licenceKey) {
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            if (userId != null && !userId.trim().isEmpty()) {
                crashlytics.setUserId(userId.trim());
            }
            if (licenceKey != null && !licenceKey.trim().isEmpty()) {
                crashlytics.setCustomKey("licence_key", licenceKey.trim());
            }
            ErrorLogQueue.flushAsync();
        } catch (Exception e) {
            Log.e(TAG, "setUserContext failed: " + describeThrowable(e), e);
        }
    }

    /** Local Logcat only — not sent to Admin error inbox. */
    public static void log(String message) {
        if (message != null) {
            Log.d(TAG, message);
        }
    }

    /** No-op — user actions are not sent to Admin error inbox. */
    public static void recordUserAction(String action) {
    }

    /**
     * Handled errors go to Firebase Crashlytics / Logcat only — not the Admin error inbox.
     */
    public static void logNonFatal(Throwable error, String context) {
        if (error == null) {
            return;
        }
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            String reason = describeThrowable(error);
            String safeContext = (context != null && !context.isEmpty()) ? context : "unknown";

            crashlytics.setCustomKey("failure_type", "non_fatal");
            crashlytics.setCustomKey("crash_context", safeContext);
            crashlytics.setCustomKey("crash_reason", truncate(reason, CUSTOM_KEY_MAX));
            crashlytics.setCustomKey("exception_type", error.getClass().getSimpleName());
            crashlytics.setCustomKey("error_category", categorizeError(error));

            String line = "NON_FATAL [" + safeContext + "] | category=" + categorizeError(error)
                    + " | " + reason;
            Log.e(TAG, line, error);
            crashlytics.log(line);
            crashlytics.recordException(error);
        } catch (Exception e) {
            Log.e(TAG, "logNonFatal failed while reporting: " + describeThrowable(e), e);
        }
    }

    /**
     * Returns a short network/API error description. Does not enqueue Admin logs
     * (API failures are captured by {@link com.pos_billingwala.Retrofit.ApiFailureInterceptor}).
     */
    public static String logCallbackFailure(Throwable error, String apiAction) {
        String message = describeNetworkOrApiError(error);
        String action = (apiAction != null && !apiAction.isEmpty()) ? apiAction : "api_callback";
        if (error != null) {
            Log.e(TAG, "API_CALLBACK_FAIL [" + action + "] | " + message
                    + " | detail=" + describeThrowable(error), error);
        } else {
            log("API_CALLBACK_FAIL [" + action + "] | " + message);
        }
        return message;
    }

    public static void setActivityScreen(String activityName) {
        currentActivity = (activityName != null && !activityName.isEmpty()) ? activityName : "unknown";
        currentFragment = "";
    }

    public static void setFragmentScreen(String fragmentName) {
        currentFragment = (fragmentName != null) ? fragmentName : "";
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

    public static String categorizeErrorPublic(Throwable error) {
        return categorizeError(error);
    }

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
            String safeMethod = method != null ? method : "UNKNOWN";
            String safeUrl = truncate(url, CUSTOM_KEY_MAX);
            String safeApi = apiName != null && !apiName.isEmpty() ? apiName : "unknown";
            String safeReason = reason != null && !reason.isEmpty() ? reason : "unknown";
            String safeRequest = requestBody != null ? requestBody : "";
            String safeResponse = responseBody != null ? responseBody : "";

            String summary = safeMethod + " " + safeApi
                    + (statusCode > 0 ? " → HTTP " + statusCode : " → NETWORK ERROR")
                    + " | reason=" + truncate(safeReason, CUSTOM_KEY_MAX);

            String logBlock = "API_FAIL " + summary
                    + "\n  url=" + safeUrl
                    + "\n  request=" + truncate(safeRequest, 500)
                    + "\n  response=" + truncate(safeResponse, 500);
            Log.e(TAG, logBlock);

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

    static void enrichFatalCrash(Thread thread, Throwable error) {
        if (error == null) {
            return;
        }
        try {
            String reason = describeThrowable(error);
            String threadName = thread != null ? thread.getName() : Thread.currentThread().getName();
            boolean mainThread = thread != null && thread.getId() == android.os.Looper.getMainLooper().getThread().getId();
            String category = categorizeError(error);
            String topFrames = formatTopStackFrames(error, STACK_FRAME_LIMIT);

            String line = "FATAL CRASH"
                    + " | category=" + category
                    + " | thread=" + threadName + (mainThread ? " (MAIN/UI)" : " (background)")
                    + " | " + reason;
            String block = "========== APP CRASH (FATAL) =========="
                    + "\n" + line
                    + "\n--- top stack ---\n" + topFrames
                    + "\n========================================";

            Log.e(TAG, block, error);
            FirebaseCrashlytics.getInstance().log(line);
            ErrorLogReporter.reportFatalCrash(error);
        } catch (Exception e) {
            Log.e(TAG, "FATAL CRASH (enrichment failed): " + error, error);
            Log.e(TAG, "enrichFatalCrash failed: " + describeThrowable(e), e);
        }
    }

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
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
                setActivityScreen(activity.getClass().getSimpleName());
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
            }
        });
    }

    private static void installFatalCrashHandler() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        if (previous instanceof AppCrashHandler) {
            fatalHandlerInstalled = true;
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler(new AppCrashHandler(previous));
        fatalHandlerInstalled = true;
        Log.i(TAG, "Global crash handler active");
    }

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
            }
            if (previous != null && !(previous instanceof AppCrashHandler)) {
                previous.uncaughtException(thread, throwable);
            } else {
                Log.e(TAG, "Uncaught exception (no previous handler)", throwable);
                System.exit(2);
            }
        }
    }

    private static String buildScreenName() {
        if (currentFragment != null && !currentFragment.isEmpty()) {
            return currentActivity + " > " + currentFragment;
        }
        return currentActivity;
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
}
