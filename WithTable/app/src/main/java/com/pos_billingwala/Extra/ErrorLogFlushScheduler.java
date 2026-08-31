package com.pos_billingwala.Extra;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.pos_billingwala.WorkerClass.ErrorLogFlushWorker;

import java.util.concurrent.TimeUnit;

/** Schedules durable background upload of pending crash/error logs. */
public final class ErrorLogFlushScheduler {

    public static final String UNIQUE_WORK_NAME = "pos_error_log_flush";

    private ErrorLogFlushScheduler() {
    }

    public static void schedule(Context context) {
        if (context == null) {
            return;
        }
        try {
            Context app = context.getApplicationContext();
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ErrorLogFlushWorker.class)
                    .setConstraints(new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build();
            WorkManager.getInstance(app).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request
            );
        } catch (Throwable ignored) {
            // WorkManager may be unavailable during very early crash — disk queue still holds the log.
        }
    }
}
