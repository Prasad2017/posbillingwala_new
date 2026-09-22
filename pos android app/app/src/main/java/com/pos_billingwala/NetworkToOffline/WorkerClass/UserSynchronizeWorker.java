package com.pos_billingwala.NetworkToOffline.WorkerClass;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.NetworkToOffline.CloudSyncTracker;
import com.pos_billingwala.NetworkToOffline.SyncNotification;
import com.pos_billingwala.NetworkToOffline.UserSynchronizeData;
import com.pos_billingwala.R;

/**
 * Uploads unsynced local data to the cloud as a foreground WorkManager job
 * so sync continues when the app is backgrounded, the screen is off, or
 * the task is swiped away (until the OS force-stops the app).
 */
public class UserSynchronizeWorker extends Worker {

    public static final String UNIQUE_NAME = "pos_cloud_upload";
    public static final String KEY_PROGRESS = "progress_text";

    public UserSynchronizeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ForegroundInfo getForegroundInfo() {
        return SyncNotification.foregroundInfo(getApplicationContext(),
                getApplicationContext().getString(R.string.sync_notification_uploading), 0);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        try {
            setForegroundAsync(getForegroundInfo());
        } catch (Exception e) {
            Log.w("UserSynchronizeWorker", "foreground start: " + e.getMessage());
        }

        Log.i("UserSynchronizeWorker", "doWork start");
        CloudSyncTracker.beginRun();
        CloudSyncTracker.refresh(context);
        boolean success = false;
        try {
            UserSynchronizeData sync = UserSynchronizeData.forBackground(context, title -> {
                Log.i("UserSynchronizeWorker", title);
                try {
                    int uploaded = CloudSyncTracker.uploadedThisRun();
                    setForegroundAsync(SyncNotification.foregroundInfo(context, title, uploaded));
                    setProgressAsync(new Data.Builder().putString(KEY_PROGRESS, title).build());
                } catch (Exception ignored) {
                }
            });
            sync.runUpload();
            success = true;
            Log.i("UserSynchronizeWorker", "doWork finished ok");
        } catch (Exception e) {
            Log.e("UserSynchronizeWorker", "doWork failed", e);
        }

        CloudSyncTracker.endRun(success);
        if (success) {
            CloudSyncTracker.recordSuccessfulSync(context);
        }
        CloudSyncTracker.Snapshot snapshot = CloudSyncTracker.refresh(context);
        SyncNotification.showComplete(context, success, snapshot.uploadedThisRun, snapshot.pendingTotal);
        return success ? Result.success() : Result.retry();
    }
}
