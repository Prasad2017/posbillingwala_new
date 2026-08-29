package com.pos_billingwala.Extra;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Captures device health problems (memory pressure, low storage, thermal, battery)
 * and queues them for server sync via {@link ErrorLogReporter}.
 * Rate-limited so normal trim events do not flood the Admin inbox.
 */
public final class DeviceHealthMonitor {

    private static final String TAG = "POS_DEVICE_HEALTH";

    /** Min gap between reports of the same category. */
    private static final long COOLDOWN_MS = 15 * 60 * 1000L;
    /** Free internal storage below this (bytes) → STORAGE report. */
    private static final long LOW_STORAGE_BYTES = 200L * 1024 * 1024;
    /** Critical free storage. */
    private static final long CRITICAL_STORAGE_BYTES = 50L * 1024 * 1024;
    /** Battery % at or below → BATTERY report (once per cooldown). */
    private static final int LOW_BATTERY_PCT = 10;

    private static final AtomicLong lastMemoryReportAt = new AtomicLong(0);
    private static final AtomicLong lastStorageReportAt = new AtomicLong(0);
    private static final AtomicLong lastThermalReportAt = new AtomicLong(0);
    private static final AtomicLong lastBatteryReportAt = new AtomicLong(0);

    private static volatile Context appContext;
    private static volatile boolean started;

    private DeviceHealthMonitor() {
    }

    public static void init(Context context) {
        if (context == null) {
            return;
        }
        appContext = context.getApplicationContext();
        if (started) {
            return;
        }
        started = true;
        try {
            checkStorageAsync();
            checkBatteryAsync();
            registerThermalListener();
            Log.i(TAG, "Device health monitor started");
        } catch (Throwable t) {
            Log.e(TAG, "init failed: " + t.getMessage());
        }
    }

    /** Called from ComponentCallbacks2.onLowMemory. */
    public static void onLowMemory() {
        reportMemoryIssue("LOW_MEMORY", "CRITICAL", "system_low_memory",
                "System reported low memory — OutOfMemoryError risk");
    }

    /** Called from ComponentCallbacks2.onTrimMemory. */
    public static void onTrimMemory(int level) {
        // Trim levels are two series — do not use >= across them:
        // Running: MODERATE=5, LOW=10, CRITICAL=15
        // Background: UI_HIDDEN=20, BACKGROUND=40, MODERATE=60, COMPLETE=80
        // UI_HIDDEN is normal (user left the UI); only RUNNING_CRITICAL / COMPLETE are severe.
        if (level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
                || level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            reportMemoryIssue("LOW_MEMORY", "ERROR", "trim_memory_" + level,
                    "Critical memory trim level=" + level);
        } else if (level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
                || level == android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE
                || level == android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            Observability.log("SYSTEM trim_memory level=" + level + " — memory pressure");
        }
        // TRIM_MEMORY_UI_HIDDEN / RUNNING_MODERATE: normal — no report
    }

    public static void checkStorageAsync() {
        Context ctx = appContext;
        if (ctx == null) {
            return;
        }
        new Thread(() -> {
            try {
                checkStorage(ctx);
            } catch (Throwable t) {
                Log.e(TAG, "checkStorage: " + t.getMessage());
            }
        }, "DeviceHealth-Storage").start();
    }

    public static void checkBatteryAsync() {
        Context ctx = appContext;
        if (ctx == null) {
            return;
        }
        new Thread(() -> {
            try {
                checkBattery(ctx);
            } catch (Throwable t) {
                Log.e(TAG, "checkBattery: " + t.getMessage());
            }
        }, "DeviceHealth-Battery").start();
    }

    /** Snapshot string for crash/error what_happened enrichment. */
    public static String memorySnapshot(Context context) {
        Context ctx = context != null ? context : appContext;
        if (ctx == null) {
            return "";
        }
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            long maxMb = rt.maxMemory() / (1024 * 1024);
            StringBuilder sb = new StringBuilder();
            sb.append("heap_used_mb=").append(usedMb)
                    .append(" heap_max_mb=").append(maxMb);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                sb.append(" avail_mb=").append(mi.availMem / (1024 * 1024))
                        .append(" total_mb=").append(mi.totalMem / (1024 * 1024))
                        .append(" lowMemory=").append(mi.lowMemory)
                        .append(" threshold_mb=").append(mi.threshold / (1024 * 1024));
            }
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    public static String storageSnapshot(Context context) {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            long free = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
            long total = stat.getBlockCountLong() * stat.getBlockSizeLong();
            return String.format(Locale.US, "storage_free_mb=%d storage_total_mb=%d",
                    free / (1024 * 1024), total / (1024 * 1024));
        } catch (Throwable t) {
            return "";
        }
    }

    private static void reportMemoryIssue(String type, String severity, String category, String summary) {
        if (!allow(lastMemoryReportAt)) {
            Observability.log("SYSTEM " + summary + " (cooldown)");
            return;
        }
        Context ctx = appContext;
        String snap = memorySnapshot(ctx);
        Observability.log("SYSTEM " + summary + " | " + snap);
        ErrorLogReporter.reportDeviceIssue(
                type,
                severity,
                category,
                summary,
                "Device memory pressure detected.\n" + snap + "\n" + storageSnapshot(ctx),
                "android.content.ComponentCallbacks2",
                snap
        );
    }

    private static void checkStorage(Context context) {
        StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        long free = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        if (free > LOW_STORAGE_BYTES) {
            return;
        }
        if (!allow(lastStorageReportAt)) {
            return;
        }
        boolean critical = free <= CRITICAL_STORAGE_BYTES;
        String snap = storageSnapshot(context);
        ErrorLogReporter.reportDeviceIssue(
                "DEVICE",
                critical ? "CRITICAL" : "WARNING",
                "low_storage",
                critical ? "Critical low storage on device" : "Low storage on device",
                "Internal storage is running low. App may fail to save invoices or queue logs.\n" + snap,
                "android.os.StatFs",
                snap
        );
    }

    private static void checkBattery(Context context) {
        Intent battery = null;
        try {
            battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (Throwable ignored) {
        }
        if (battery == null) {
            return;
        }
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) {
            return;
        }
        int pct = (int) ((level / (float) scale) * 100f);
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        if (charging || pct > LOW_BATTERY_PCT) {
            return;
        }
        if (!allow(lastBatteryReportAt)) {
            return;
        }
        String detail = "battery_pct=" + pct + " charging=" + charging;
        ErrorLogReporter.reportDeviceIssue(
                "DEVICE",
                "WARNING",
                "low_battery",
                "Device battery critically low (" + pct + "%)",
                "Battery is very low. Device may shut down and interrupt billing/sync.\n" + detail,
                "android.os.BatteryManager",
                detail
        );
    }

    private static void registerThermalListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        Context ctx = appContext;
        if (ctx == null) {
            return;
        }
        try {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            if (pm == null) {
                return;
            }
            pm.addThermalStatusListener(status -> {
                if (status < PowerManager.THERMAL_STATUS_SEVERE) {
                    return;
                }
                if (!allow(lastThermalReportAt)) {
                    return;
                }
                String label = thermalLabel(status);
                ErrorLogReporter.reportDeviceIssue(
                        "DEVICE",
                        status >= PowerManager.THERMAL_STATUS_CRITICAL ? "CRITICAL" : "ERROR",
                        "thermal_" + label,
                        "Device thermal throttling (" + label + ")",
                        "Device overheating may cause ANR, slow UI, or forced kill.\nthermal_status=" + status,
                        "android.os.PowerManager",
                        "thermal_status=" + status + " (" + label + ")"
                );
            });
        } catch (Throwable t) {
            Log.w(TAG, "thermal listener unavailable: " + t.getMessage());
        }
    }

    private static String thermalLabel(int status) {
        switch (status) {
            case PowerManager.THERMAL_STATUS_SEVERE:
                return "severe";
            case PowerManager.THERMAL_STATUS_CRITICAL:
                return "critical";
            case PowerManager.THERMAL_STATUS_EMERGENCY:
                return "emergency";
            case PowerManager.THERMAL_STATUS_SHUTDOWN:
                return "shutdown";
            default:
                return "status_" + status;
        }
    }

    private static boolean allow(AtomicLong lastAt) {
        long now = System.currentTimeMillis();
        long prev = lastAt.get();
        if (now - prev < COOLDOWN_MS) {
            return false;
        }
        return lastAt.compareAndSet(prev, now);
    }
}
