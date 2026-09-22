package com.pos_billingwala.NetworkToOffline;

import com.pos_billingwala.Extra.Observability;
import com.google.firebase.perf.metrics.Trace;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Serializes offline→online upload work off the UI thread.
 * Single thread prevents concurrent upload storms / duplicate POST races.
 */
public final class OfflineSyncExecutor {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "pos-offline-sync");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private static final Object LOCK = new Object();
    private static volatile boolean uploadInFlight = false;
    private static volatile long lastEnqueueAtMs = 0L;
    private static final long MIN_ENQUEUE_INTERVAL_MS = 15_000L;

    private OfflineSyncExecutor() {
    }

    /** True when an upload runnable is queued or running. */
    public static boolean isUploadInFlight() {
        return uploadInFlight;
    }

    /**
     * Enqueue upload work. Skips if an upload is already in flight or was enqueued recently
     * (connectivity flaps while Home is visible).
     */
    public static void execute(Runnable work) {
        long now = System.currentTimeMillis();
        synchronized (LOCK) {
            if (uploadInFlight || (now - lastEnqueueAtMs) < MIN_ENQUEUE_INTERVAL_MS) {
                return;
            }
            uploadInFlight = true;
            lastEnqueueAtMs = now;
        }
        EXECUTOR.execute(() -> {
            Trace trace = Observability.startTrace(Observability.TRACE_OFFLINE_SYNC);
            try {
                work.run();
            } catch (Exception e) {
                Observability.logNonFatal(e, "offline_sync_upload");
                throw e;
            } finally {
                Observability.stopTrace(trace);
                uploadInFlight = false;
            }
        });
    }
}
