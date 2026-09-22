package com.pos_billingwala.Extra;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared background executors so SQLite / bitmap / heavy work stay off the main thread.
 * Use {@link #db()} for all database access (serialized). Use {@link #main(Runnable)} for UI.
 */
public final class AppExecutors {

    private static final AppExecutors INSTANCE = new AppExecutors();

    private final ExecutorService dbExecutor;
    private final ExecutorService ioExecutor;
    private final Handler mainHandler;
    /**
     * Generation bump via {@link #invalidatePending()} drops in-flight UI callbacks.
     * Concurrent {@link #runDbThenMain} calls must NOT cancel each other (billing loads
     * company + printer + categories + cart in parallel).
     */
    private final AtomicInteger invalidateGeneration = new AtomicInteger();

    private AppExecutors() {
        dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pos-db");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        ioExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "pos-io");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static AppExecutors get() {
        return INSTANCE;
    }

    /** Serialized SQLite / Room-style DB work. */
    public Executor db() {
        return dbExecutor;
    }

    /** Parallel IO (decode, file, light compute). */
    public Executor io() {
        return ioExecutor;
    }

    public void main(@NonNull Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            mainHandler.post(action);
        }
    }

    public void postMainDelayed(@NonNull Runnable action, long delayMs) {
        mainHandler.postDelayed(action, delayMs);
    }

    public void removeMainCallbacks(@NonNull Runnable action) {
        mainHandler.removeCallbacks(action);
    }

    /**
     * Run {@code background} on the DB thread, then {@code ui} on main if the fragment is still added
     * and {@link #invalidatePending()} has not been called since this task started.
     */
    public void runDbThenMain(@NonNull Fragment fragment, @NonNull Runnable background, @NonNull Runnable ui) {
        final int generation = invalidateGeneration.get();
        try {
            dbExecutor.execute(() -> {
                try {
                    background.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                main(() -> {
                    if (generation != invalidateGeneration.get()) {
                        return;
                    }
                    if (!fragment.isAdded()) {
                        return;
                    }
                    try {
                        ui.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run {@code background} on IO pool, then {@code ui} on main if fragment is added.
     */
    public void runIoThenMain(@NonNull Fragment fragment, @NonNull Runnable background, @NonNull Runnable ui) {
        final int generation = invalidateGeneration.get();
        try {
            ioExecutor.execute(() -> {
                try {
                    background.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                main(() -> {
                    if (generation != invalidateGeneration.get()) {
                        return;
                    }
                    if (!fragment.isAdded()) {
                        return;
                    }
                    try {
                        ui.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    /** Invalidate in-flight fragment callbacks (e.g. onDestroyView). */
    public void invalidatePending() {
        invalidateGeneration.incrementAndGet();
    }
}
