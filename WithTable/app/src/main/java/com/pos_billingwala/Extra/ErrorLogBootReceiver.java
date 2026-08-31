package com.pos_billingwala.Extra;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Flushes pending error logs after device boot or app update,
 * even if the user has not opened the app yet (WorkManager persists the job).
 */
public final class ErrorLogBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Context app = context.getApplicationContext();
            ErrorLogQueue.init(app);
            ErrorLogQueue.restorePendingFromArchive(app);
            ErrorLogFlushScheduler.schedule(app);
            ErrorLogQueue.flushAsync();
        }
    }
}
