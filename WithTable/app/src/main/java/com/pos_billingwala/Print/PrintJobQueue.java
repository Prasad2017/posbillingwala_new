package com.pos_billingwala.Print;

import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;

/**
 * One serialized print queue per physical printer identity.
 * Prevents overlapping ConnectThreads / concurrent writes to the same device.
 */
public final class PrintJobQueue {

    private static final String TAG = "PrintJobQueue";
    private static final PrintJobQueue INSTANCE = new PrintJobQueue();

    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();
    private final Object mapLock = new Object();

    private PrintJobQueue() {
    }

    public static PrintJobQueue get() {
        return INSTANCE;
    }

    public void enqueue(String printerIdentity, Runnable job) {
        if (job == null) {
            return;
        }
        String key = normalize(printerIdentity);
        try {
            executorFor(key).execute(() -> {
                try {
                    job.run();
                } catch (Throwable t) {
                    Log.e(TAG, "print job failed for " + key, t);
                }
            });
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "queue rejected job for " + key, e);
        } catch (Exception e) {
            Log.e(TAG, "enqueue failed for " + key, e);
        }
    }

    /**
     * Runs a print job on the printer's queue and waits for completion.
     * Use from background print threads (never from main thread for long jobs).
     */
    public boolean submitAndWait(String printerIdentity, Callable<Boolean> job, long timeoutMs) {
        if (job == null) {
            return false;
        }
        String key = normalize(printerIdentity);
        try {
            Future<Boolean> future = executorFor(key).submit(() -> {
                try {
                    Boolean result = job.call();
                    return result != null && result;
                } catch (Throwable t) {
                    Log.e(TAG, "queued call failed for " + key, t);
                    return false;
                }
            });
            long wait = timeoutMs > 0 ? timeoutMs : 60_000L;
            Boolean ok = future.get(wait, TimeUnit.MILLISECONDS);
            return ok != null && ok;
        } catch (Exception e) {
            Log.e(TAG, "submitAndWait failed for " + key, e);
            return false;
        }
    }

    private ExecutorService executorFor(String key) {
        ExecutorService existing = executors.get(key);
        if (existing != null && !existing.isShutdown()) {
            return existing;
        }
        synchronized (mapLock) {
            existing = executors.get(key);
            if (existing != null && !existing.isShutdown()) {
                return existing;
            }
            ExecutorService created = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "PrintQueue-" + key);
                t.setDaemon(true);
                return t;
            });
            executors.put(key, created);
            return created;
        }
    }

    private static String normalize(String identity) {
        if (identity == null || identity.trim().isEmpty()) {
            return "default";
        }
        return identity.trim().toLowerCase();
    }
}
