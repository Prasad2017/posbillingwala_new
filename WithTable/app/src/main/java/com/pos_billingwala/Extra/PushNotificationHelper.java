package com.pos_billingwala.Extra;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.R;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shows FCM push notifications (licence expiry + promotional).
 */
public final class PushNotificationHelper {

    public static final String CHANNEL_ID = "pos_push_alerts";
    private static final AtomicInteger NOTIFICATION_ID = new AtomicInteger(5100);

    private PushNotificationHelper() {
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
                context.getString(R.string.push_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.push_notification_channel_desc));
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }

    public static void showFromPayload(@NonNull Context context,
                                       @Nullable String title,
                                       @Nullable String body,
                                       @Nullable Map<String, String> data) {
        ensureChannel(context);

        String type = data != null ? data.get("type") : null;
        if (title == null || title.trim().isEmpty()) {
            if ("license_expiring".equals(type)) {
                title = context.getString(R.string.push_license_expiring_title);
            } else {
                title = context.getString(R.string.push_default_title);
            }
        }
        if (body == null || body.trim().isEmpty()) {
            if ("license_expiring".equals(type) && data != null && data.get("days_left") != null) {
                body = context.getString(R.string.push_license_expiring_body, data.get("days_left"));
            } else {
                body = context.getString(R.string.push_default_body);
            }
        }

        PendingIntent contentIntent = buildContentIntent(context, data);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent);

        if ("license_expiring".equals(type)) {
            builder.setCategory(NotificationCompat.CATEGORY_REMINDER);
        } else {
            builder.setCategory(NotificationCompat.CATEGORY_PROMO);
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID.incrementAndGet(), builder.build());
        } catch (SecurityException ignored) {
        }
    }

    @NonNull
    private static PendingIntent buildContentIntent(@NonNull Context context, @Nullable Map<String, String> data) {
        String url = data != null ? data.get("url") : null;
        Intent intent;
        if (url != null && !url.trim().isEmpty()) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, 0, intent, flags);
    }
}
