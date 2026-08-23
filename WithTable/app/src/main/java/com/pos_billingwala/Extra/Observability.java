package com.pos_billingwala.Extra;

import android.app.Activity;
import android.app.Application;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

/**
 * Production crash analytics — every report includes screen + reason in Firebase Crashlytics.
 * Fatal crashes, ANRs, and non-fatals all carry {@code screen_name}, {@code crash_reason}, etc.
 */
public final class Observability {

    public static final String TRACE_SAVE_INVOICE = "save_invoice_db";
    public static final String TRACE_OFFLINE_SYNC = "offline_sync_upload";
    public static final String TRACE_PRINT_BILL = "print_bill_bitmap";

    /** Firebase Crashlytics max length for a single custom-key value. */
    private static final int CUSTOM_KEY_MAX = 1024;
    /** Max request/response body captured for API failure breadcrumbs. */
    private static final int API_BODY_CAPTURE_MAX = 65536;
    /** Max chars per Crashlytics log line before splitting into chunks. */
    private static final int LOG_CHUNK_SIZE = 8192;

    private static volatile String currentActivity = "AppStart";
    private static volatile String currentFragment = "";

    private Observability() {
    }

    public static void init(Application application) {
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCrashlyticsCollectionEnabled(true);
            FirebasePerformance.getInstance().setPerformanceCollectionEnabled(true);

            loadStoredUserContext(application);
            registerActivityTracking(application);
            installFatalCrashHandler();
            syncScreenToCrashlytics();
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    /** Breadcrumb trail — visible in Crashlytics logs before a crash. */
    public static void log(String message) {
        if (message == null) {
            return;
        }
        try {
            FirebaseCrashlytics.getInstance().log(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Record a handled error with clear context.
     *
     * @param error   the exception
     * @param context short label, e.g. {@code bluetooth_print}, {@code save_invoice_db}
     */
    public static void logNonFatal(Throwable error, String context) {
        if (error == null) {
            return;
        }
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            String reason = crashReason(error);
            String screen = buildScreenName();

            if (context != null && !context.isEmpty()) {
                crashlytics.setCustomKey("crash_context", context);
            }
            crashlytics.setCustomKey("crash_reason", reason);
            crashlytics.setCustomKey("exception_type", error.getClass().getSimpleName());
            syncScreenToCrashlytics();

            String line = "NON_FATAL"
                    + (context != null && !context.isEmpty() ? " [" + context + "]" : "")
                    + " on " + screen + ": " + reason;
            crashlytics.log(line);
            crashlytics.recordException(error);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    }

    public static String getCurrentScreenName() {
        return buildScreenName();
    }

    /**
     * Record a failed API call with endpoint, URL, request, response, and current screen.
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
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            String screen = buildScreenName();
            String safeMethod = method != null ? method : "UNKNOWN";
            String safeUrl = truncate(url, CUSTOM_KEY_MAX);
            String safeApi = apiName != null && !apiName.isEmpty() ? apiName : "unknown";
            String safeReason = reason != null && !reason.isEmpty() ? reason : "unknown";
            String safeRequest = requestBody != null ? requestBody : "";
            String safeResponse = responseBody != null ? responseBody : "";

            crashlytics.setCustomKey("failure_type", "api");
            crashlytics.setCustomKey("api_name", safeApi);
            crashlytics.setCustomKey("api_method", safeMethod);
            crashlytics.setCustomKey("api_url", safeUrl);
            crashlytics.setCustomKey("api_status_code", statusCode);
            crashlytics.setCustomKey("api_failure_reason", truncate(safeReason, CUSTOM_KEY_MAX));
            crashlytics.setCustomKey("api_request", truncate(safeRequest, CUSTOM_KEY_MAX));
            crashlytics.setCustomKey("api_response", truncate(safeResponse, CUSTOM_KEY_MAX));
            syncScreenToCrashlytics();

            String summary = safeMethod + " " + safeApi
                    + (statusCode > 0 ? " HTTP " + statusCode : "")
                    + " on " + screen + ": " + truncate(safeReason, CUSTOM_KEY_MAX);

            crashlytics.log("API_FAIL " + summary);
            logChunked(crashlytics, "API_REQUEST", safeRequest);
            logChunked(crashlytics, "API_RESPONSE", safeResponse);
            crashlytics.recordException(new ApiFailureException(summary));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Trace startTrace(String name) {
        try {
            Trace trace = FirebasePerformance.getInstance().newTrace(name);
            trace.start();
            return trace;
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    /** Enrich an uncaught fatal crash with screen + human-readable reason. */
    static void enrichFatalCrash(Throwable error) {
        if (error == null) {
            return;
        }
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            String reason = crashReason(error);
            String screen = buildScreenName();

            crashlytics.setCustomKey("crash_reason", reason);
            crashlytics.setCustomKey("exception_type", error.getClass().getSimpleName());
            crashlytics.setCustomKey("crash_type", "fatal");
            syncScreenToCrashlytics();
            crashlytics.log("FATAL on " + screen + ": " + reason);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadStoredUserContext(Application application) {
        try {
            String userId = Common.getSavedUserData(application, "userId");
            String licenceKey = Common.getSavedUserData(application, "LicenceKey");
            setUserContext(userId, licenceKey);
        } catch (Exception e) {
            e.printStackTrace();
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
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            enrichFatalCrash(throwable);
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });
    }

    private static void syncScreenToCrashlytics() {
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            String screen = buildScreenName();
            crashlytics.setCustomKey("screen_name", screen);
            crashlytics.setCustomKey("activity_name", currentActivity);
            crashlytics.setCustomKey("fragment_name", currentFragment);
        } catch (Exception e) {
            e.printStackTrace();
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

    private static String crashReason(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        String message = error.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            return error.getClass().getSimpleName() + ": " + message.trim();
        }
        Throwable cause = error.getCause();
        if (cause != null && cause != error) {
            String causeReason = crashReason(cause);
            if (!"unknown".equals(causeReason) && !causeReason.equals(cause.getClass().getSimpleName())) {
                return error.getClass().getSimpleName() + " (" + causeReason + ")";
            }
        }
        return error.getClass().getSimpleName();
    }
}
