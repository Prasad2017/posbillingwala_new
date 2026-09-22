package com.pos_billingwala.Extra;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pos_billingwala.Model.InAppNotification;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Persists in-app notifications shown on the Home bell (FCM + local licence alerts).
 */
public final class InAppNotificationStore {

    private static final String PREF = "in_app_notifications";
    private static final String KEY_ITEMS = "items_json";
    private static final int MAX_ITEMS = 50;
    private static final Gson GSON = new Gson();
    private static final CopyOnWriteArrayList<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private InAppNotificationStore() {
    }

    public static void addListener(@Nullable Runnable listener) {
        if (listener != null) {
            LISTENERS.addIfAbsent(listener);
        }
    }

    public static void removeListener(@Nullable Runnable listener) {
        if (listener != null) {
            LISTENERS.remove(listener);
        }
    }

    @NonNull
    public static List<InAppNotification> getAll(@NonNull Context context) {
        List<InAppNotification> list = read(context);
        Collections.sort(list, new Comparator<InAppNotification>() {
            @Override
            public int compare(InAppNotification a, InAppNotification b) {
                return Long.compare(b.createdAt, a.createdAt);
            }
        });
        return list;
    }

    public static int getUnreadCount(@NonNull Context context) {
        int count = 0;
        for (InAppNotification n : read(context)) {
            if (!n.read) {
                count++;
            }
        }
        return count;
    }

    /**
     * Adds a notification. If {@code dedupeKey} is set and already exists, skips insert.
     */
    @Nullable
    public static InAppNotification add(@NonNull Context context,
                                        @Nullable String type,
                                        @Nullable String title,
                                        @Nullable String body,
                                        @Nullable String url,
                                        @Nullable String dedupeKey) {
        Context app = context.getApplicationContext();
        List<InAppNotification> list = read(app);

        if (dedupeKey != null && !dedupeKey.trim().isEmpty()) {
            for (InAppNotification existing : list) {
                if (dedupeKey.equals(existing.dedupeKey)) {
                    return null;
                }
            }
        }

        InAppNotification item = new InAppNotification(
                UUID.randomUUID().toString(),
                type != null ? type : "general",
                title != null ? title : "",
                body != null ? body : "",
                url,
                System.currentTimeMillis(),
                false,
                dedupeKey
        );
        list.add(0, item);
        while (list.size() > MAX_ITEMS) {
            list.remove(list.size() - 1);
        }
        write(app, list);
        notifyUpdated();
        return item;
    }

    public static void markRead(@NonNull Context context, @Nullable String id) {
        if (id == null) {
            return;
        }
        Context app = context.getApplicationContext();
        List<InAppNotification> list = read(app);
        boolean changed = false;
        for (InAppNotification n : list) {
            if (id.equals(n.id) && !n.read) {
                n.read = true;
                changed = true;
                break;
            }
        }
        if (changed) {
            write(app, list);
            notifyUpdated();
        }
    }

    public static void markAllRead(@NonNull Context context) {
        Context app = context.getApplicationContext();
        List<InAppNotification> list = read(app);
        boolean changed = false;
        for (InAppNotification n : list) {
            if (!n.read) {
                n.read = true;
                changed = true;
            }
        }
        if (changed) {
            write(app, list);
            notifyUpdated();
        }
    }

    public static void clearAll(@NonNull Context context) {
        Context app = context.getApplicationContext();
        write(app, new ArrayList<>());
        notifyUpdated();
    }

    private static void notifyUpdated() {
        for (Runnable listener : LISTENERS) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    @NonNull
    private static List<InAppNotification> read(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ITEMS, "[]");
        try {
            Type type = new TypeToken<ArrayList<InAppNotification>>() {
            }.getType();
            List<InAppNotification> list = GSON.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void write(@NonNull Context context, @NonNull List<InAppNotification> list) {
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ITEMS, GSON.toJson(list)).apply();
    }
}
