package com.pos_billingwala.Extra;

import android.app.Application;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

/**
 * P5-1: Centralized production observability.
 * Crashlytics captures crashes + ANRs (Android 11+, targetSdk 30+).
 * Performance traces highlight slow bill save / sync paths in Firebase console.
 */
public final class Observability {

    public static final String TRACE_SAVE_INVOICE = "save_invoice_db";
    public static final String TRACE_OFFLINE_SYNC = "offline_sync_upload";
    public static final String TRACE_PRINT_BILL = "print_bill_bitmap";

    private Observability() {
    }

    public static void init(Application application) {
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCrashlyticsCollectionEnabled(true);
            FirebasePerformance.getInstance().setPerformanceCollectionEnabled(true);
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

    public static void logNonFatal(Throwable error, String context) {
        if (error == null) {
            return;
        }
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            if (context != null && !context.isEmpty()) {
                crashlytics.log(context);
            }
            crashlytics.recordException(error);
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
}
