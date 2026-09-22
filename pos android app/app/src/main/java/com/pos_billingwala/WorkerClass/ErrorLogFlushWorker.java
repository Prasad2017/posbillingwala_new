package com.pos_billingwala.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Extra.ErrorLogQueue;
import com.pos_billingwala.Extra.ErrorLogUploader;
import com.pos_billingwala.Extra.ProcessExitLogCollector;

/**
 * Retries pending crash/API error logs when network is available.
 * Survives process death — runs after reboot or next app open.
 */
public final class ErrorLogFlushWorker extends Worker {

    public ErrorLogFlushWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context app = getApplicationContext();
        try {
            ErrorLogQueue.init(app);
            ErrorLogQueue.restorePendingFromArchive(app);
            ErrorLogUploader.flushPending(app);
            ProcessExitLogCollector.collectAsync(app);
            if (ErrorLogQueue.pendingCount(app) > 0) {
                return Result.retry();
            }
            return Result.success();
        } catch (Throwable t) {
            return Result.retry();
        }
    }
}
