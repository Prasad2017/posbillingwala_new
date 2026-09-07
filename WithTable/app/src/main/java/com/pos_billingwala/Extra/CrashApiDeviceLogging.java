package com.pos_billingwala.Extra;

import android.content.Context;

/**
 * Crash / API / device logging facade over live Observability + ErrorLog* pipeline.
 * Phase 18: wrap existing Crashlytics + local queue upload.
 */
public final class CrashApiDeviceLogging {

    private CrashApiDeviceLogging() {
    }

    public static void logNonFatal(Throwable t, String contextTag) {
        Observability.logNonFatal(t, contextTag);
    }

    public static void recordUserAction(String action) {
        Observability.recordUserAction(action);
    }

    public static void setUserContext(String userId, String licenceKey) {
        Observability.setUserContext(userId, licenceKey);
    }

    public static String moduleSummary(Context context) {
        return "Crashlytics + Performance: Observability\n"
                + "Local queue: ErrorLogQueue → ErrorLogUploader / ErrorLogFlushWorker\n"
                + "Admin inbox: Admin CrashErrorLogList\n"
                + "Device: DeviceHealthMonitor, ProcessExitLogCollector";
    }
}
