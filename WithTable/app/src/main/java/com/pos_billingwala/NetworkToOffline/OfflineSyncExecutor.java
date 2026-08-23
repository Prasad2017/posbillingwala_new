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

    private OfflineSyncExecutor() {
    }

    public static void execute(Runnable work) {
        EXECUTOR.execute(() -> {
            Trace trace = Observability.startTrace(Observability.TRACE_OFFLINE_SYNC);
            try {
                work.run();
            } catch (Exception e) {
                Observability.logNonFatal(e, "offline_sync_upload");
                throw e;
            } finally {
                Observability.stopTrace(trace);
            }
        });
    }
}
