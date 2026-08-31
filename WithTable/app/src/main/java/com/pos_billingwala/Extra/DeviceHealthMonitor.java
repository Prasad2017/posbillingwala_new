package com.pos_billingwala.Extra;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

/**
 * Device RAM helpers for crash context and low-RAM tuning.
 * Does not report storage, battery, thermal, or trim-memory events to Admin.
 */
public final class DeviceHealthMonitor {

    private static final String TAG = "POS_DEVICE_HEALTH";

    private static volatile Context appContext;

    private DeviceHealthMonitor() {
    }

    /** Kept for API compatibility — no background health reporting. */
    public static void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
            Log.i(TAG, "Device health monitor ready (crash memory snapshot only)");
        }
    }

    public static void onLowMemory() {
    }

    public static void onTrimMemory(int level) {
    }

    /** Snapshot string for crash / low-memory log enrichment. */
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

    /** POS devices with ≤2 GB RAM — used for CursorWindow and heap tuning. */
    public static boolean isLowRamDevice(Context context) {
        return getTotalRamMb(context) > 0 && getTotalRamMb(context) <= 2048;
    }

    public static long getTotalRamMb(Context context) {
        Context ctx = context != null ? context : appContext;
        if (ctx == null) {
            return 0;
        }
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) {
                return 0;
            }
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            return mi.totalMem / (1024 * 1024);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int recommendedCursorWindowBytes(Context context) {
        long totalMb = getTotalRamMb(context);
        if (totalMb <= 2048) {
            return 2 * 1024 * 1024;
        }
        if (totalMb <= 3072) {
            return 4 * 1024 * 1024;
        }
        return 8 * 1024 * 1024;
    }
}
