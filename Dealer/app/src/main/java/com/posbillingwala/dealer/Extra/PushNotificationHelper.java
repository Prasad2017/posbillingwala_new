package com.posbillingwala.dealer.Extra;

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

import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.R;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class PushNotificationHelper {
    public static final String CHANNEL_ID = "pos_push_alerts";
    private static final AtomicInteger NOTIFICATION_ID = new AtomicInteger(6200);

    private PushNotificationHelper() {}

    public static void ensureChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Alerts & offers", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Promotional and system alerts from Billingwala");
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }

    public static void showFromPayload(@NonNull Context context, @Nullable String title,
                                       @Nullable String body, @Nullable Map<String, String> data) {
        ensureChannel(context);
        if (title == null || title.trim().isEmpty()) title = "Billingwala Dealer";
        if (body == null || body.trim().isEmpty()) {
            body = data != null && data.get("message") != null ? data.get("message") : "New notification";
        }
        Intent intent;
        String url = data != null ? data.get("url") : null;
        if (url != null && !url.trim().isEmpty()) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, flags);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi);
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID.incrementAndGet(), builder.build());
        } catch (SecurityException ignored) {}
    }
}
