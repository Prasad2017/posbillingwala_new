package com.pos_billingwala.NetworkToOffline;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ForegroundInfo;

import com.pos_billingwala.R;

/**
 * Ongoing + completion notifications so cloud upload is visible when the
 * screen is off or the app is in the background.
 */
public final class SyncNotification {

    public static final String CHANNEL_ID = "pos_cloud_sync";
    public static final int ONGOING_ID = 4101;
    public static final int COMPLETE_ID = 4102;

    private SyncNotification() {
    }

    public static void ensureChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.sync_notification_channel),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(context.getString(R.string.sync_notification_channel));
        channel.setShowBadge(false);
        channel.enableVibration(false);
        manager.createNotificationChannel(channel);
    }

    @NonNull
    public static ForegroundInfo foregroundInfo(@NonNull Context context, @NonNull String text, int uploaded) {
        ensureChannel(context);
        Notification notification = ongoingNotification(context, text, uploaded);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new ForegroundInfo(ONGOING_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }
        return new ForegroundInfo(ONGOING_ID, notification);
    }

    @NonNull
    public static Notification ongoingNotification(@NonNull Context context, @NonNull String text, int uploaded) {
        ensureChannel(context);
        String body = uploaded > 0
                ? context.getString(R.string.sync_notification_progress_count, text, uploaded)
                : text;
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_sync)
                .setContentTitle(context.getString(R.string.sync_notification_title))
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setContentIntent(CloudSyncNav.contentIntent(context))
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }

    public static void showComplete(@NonNull Context context, boolean success, int uploaded, int pending) {
        ensureChannel(context);
        NotificationManagerCompat.from(context).cancel(ONGOING_ID);
        String text;
        if (success && pending == 0) {
            text = uploaded > 0
                    ? context.getString(R.string.sync_notification_complete_count, uploaded)
                    : context.getString(R.string.sync_notification_complete);
        } else if (success) {
            text = context.getString(R.string.sync_notification_partial, uploaded, pending);
        } else {
            text = context.getString(R.string.sync_notification_failed);
        }
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_sync)
                .setContentTitle(context.getString(R.string.sync_notification_title))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(CloudSyncNav.contentIntent(context))
                .build();
        try {
            NotificationManagerCompat.from(context).notify(COMPLETE_ID, notification);
        } catch (SecurityException ignored) {
        }
    }
}
