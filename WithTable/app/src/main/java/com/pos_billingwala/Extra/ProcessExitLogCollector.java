package com.pos_billingwala.Extra;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Harvests Android's process-exit history — the same source Play Protect uses for
 * "crashed repeatedly due to a null pointer exception". Captures Java crashes, native
 * crashes, ANRs, low-memory kills, and signaled deaths that the in-process handler
 * can miss, then saves + uploads them like every other error log.
 */
public final class ProcessExitLogCollector {

    private static final String TAG = "POS_EXIT_LOG";

    private ProcessExitLogCollector() {
    }

    public static void collectAsync(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                Api30.collect(app);
            } catch (Throwable t) {
                Log.e(TAG, "collect failed: " + t.getMessage());
            }
        }, "ProcessExitLogCollector").start();
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static final class Api30 {

        private static final String PREFS = "pos_observability";
        private static final String KEY_LAST_TS = "last_process_exit_ts";
        private static final long LOOKBACK_MS = 7L * 24 * 60 * 60 * 1000;
        private static final int MAX_EXITS = 20;
        private static final int TRACE_MAX_BYTES = 20480;

        private static final Pattern EXCEPTION_CLASS = Pattern.compile(
                "((?:[a-zA-Z_][\\w]*\\.)+[A-Za-z_][\\w]*(?:Exception|Error|Throwable))");

        static void collect(Context context) {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) {
                return;
            }
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long lastTs = prefs.getLong(KEY_LAST_TS, 0L);
            long now = System.currentTimeMillis();
            long minTs = lastTs > 0 ? lastTs : Math.max(0, now - LOOKBACK_MS);

            List<ApplicationExitInfo> exits;
            try {
                exits = am.getHistoricalProcessExitReasons(context.getPackageName(), 0, MAX_EXITS);
            } catch (Throwable t) {
                Log.e(TAG, "getHistoricalProcessExitReasons: " + t.getMessage());
                return;
            }
            if (exits == null || exits.isEmpty()) {
                return;
            }

            long newest = lastTs;
            int saved = 0;
            for (ApplicationExitInfo info : exits) {
                if (info == null) {
                    continue;
                }
                long ts = info.getTimestamp();
                if (ts <= minTs) {
                    continue;
                }
                if (ts > newest) {
                    newest = ts;
                }
                String trace = readTrace(info);
                String description = descriptionOf(info);
                if (!shouldSave(info, description, trace)) {
                    continue;
                }
                String extras = buildExtras(info);
                ErrorLogReporter.reportProcessExit(
                        reasonType(info),
                        reasonLabel(info),
                        exceptionClassOf(description, trace, info.getReason()),
                        description,
                        mergeTrace(trace, extras),
                        ts,
                        info.getPid(),
                        info.getReason(),
                        info.getStatus(),
                        severityFor(info)
                );
                saved++;
            }
            if (newest > lastTs) {
                prefs.edit().putLong(KEY_LAST_TS, newest).apply();
            }
            if (saved > 0) {
                Log.i(TAG, "Saved " + saved + " system process-exit log(s)");
                ErrorLogQueue.flushAsync();
            }
        }

        private static boolean shouldSave(ApplicationExitInfo info, String description, String trace) {
            int reason = info.getReason();
            if (reason == ApplicationExitInfo.REASON_CRASH
                    || reason == ApplicationExitInfo.REASON_CRASH_NATIVE
                    || reason == ApplicationExitInfo.REASON_ANR
                    || reason == ApplicationExitInfo.REASON_LOW_MEMORY
                    || reason == ApplicationExitInfo.REASON_SIGNALED
                    || reason == ApplicationExitInfo.REASON_INITIALIZATION_FAILURE
                    || reason == ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE) {
                return true;
            }
            if (reason == ApplicationExitInfo.REASON_OTHER) {
                String text = (description + "\n" + trace).toLowerCase(Locale.US);
                return text.contains("exception") || text.contains("fatal") || text.contains("anr");
            }
            return false;
        }

        private static String reasonType(ApplicationExitInfo info) {
            switch (info.getReason()) {
                case ApplicationExitInfo.REASON_CRASH_NATIVE:
                    return "NATIVE_CRASH";
                case ApplicationExitInfo.REASON_ANR:
                    return "ANR";
                case ApplicationExitInfo.REASON_LOW_MEMORY:
                    return "LOW_MEMORY";
                case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE:
                    return "DEVICE";
                default:
                    return "CRASH";
            }
        }

        private static String reasonLabel(ApplicationExitInfo info) {
            switch (info.getReason()) {
                case ApplicationExitInfo.REASON_CRASH:
                    return "java_crash";
                case ApplicationExitInfo.REASON_CRASH_NATIVE:
                    return "native_crash";
                case ApplicationExitInfo.REASON_ANR:
                    return "anr";
                case ApplicationExitInfo.REASON_LOW_MEMORY:
                    return "low_memory";
                case ApplicationExitInfo.REASON_SIGNALED:
                    return "signaled";
                case ApplicationExitInfo.REASON_INITIALIZATION_FAILURE:
                    return "init_failure";
                case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE:
                    return "resource_kill";
                default:
                    return "process_exit";
            }
        }

        private static String descriptionOf(ApplicationExitInfo info) {
            String d = info.getDescription();
            return d != null ? d.trim() : "";
        }

        private static String exceptionClassOf(String description, String trace, int reason) {
            String blob = description + "\n" + trace;
            Matcher m = EXCEPTION_CLASS.matcher(blob);
            if (m.find()) {
                return m.group(1);
            }
            String desc = description != null ? description.toLowerCase(Locale.US) : "";
            if (desc.contains("null pointer") || desc.contains("nullpointer")) {
                return "java.lang.NullPointerException";
            }
            if (reason == ApplicationExitInfo.REASON_ANR) {
                return "android.app.ApplicationExitInfo.ANR";
            }
            if (reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                return "NativeCrash";
            }
            // LMK is OS reclaim — not a Java OutOfMemoryError stack.
            if (reason == ApplicationExitInfo.REASON_LOW_MEMORY) {
                return "android.app.ApplicationExitInfo.LOW_MEMORY";
            }
            return "ProcessExit";
        }

        private static String severityFor(ApplicationExitInfo info) {
            if (info.getReason() == ApplicationExitInfo.REASON_LOW_MEMORY) {
                int importance = info.getImportance();
                // Cached / gone processes are normal OS reclaim under device pressure.
                if (importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED) {
                    return "WARNING";
                }
                if (importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                    return "ERROR";
                }
                return "CRITICAL";
            }
            if (info.getReason() == ApplicationExitInfo.REASON_ANR
                    || info.getReason() == ApplicationExitInfo.REASON_CRASH
                    || info.getReason() == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                return "CRITICAL";
            }
            return "ERROR";
        }

        private static String buildExtras(ApplicationExitInfo info) {
            StringBuilder sb = new StringBuilder();
            sb.append("importance=").append(importanceLabel(info.getImportance()));
            sb.append(" (").append(info.getImportance()).append(')');
            long pss = info.getPss();
            if (pss > 0) {
                sb.append("\npss_kb=").append(pss);
            }
            if (Build.VERSION.SDK_INT >= 35) {
                try {
                    long rss = info.getRss();
                    if (rss > 0) {
                        sb.append("\nrss_kb=").append(rss);
                    }
                } catch (Throwable ignored) {
                }
            }
            if (info.getReason() == ApplicationExitInfo.REASON_LOW_MEMORY) {
                sb.append("\nnote=OS Low Memory Killer reclaimed this process; ");
                sb.append("not a Java OutOfMemoryError unless a heap dump/stack says so.");
            }
            return sb.toString();
        }

        private static String importanceLabel(int importance) {
            if (importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return "FOREGROUND";
            }
            if (importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                return "VISIBLE";
            }
            if (importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                return "SERVICE";
            }
            if (importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED) {
                return "CACHED";
            }
            return "GONE_OR_OTHER";
        }

        private static String mergeTrace(String trace, String extras) {
            if (extras == null || extras.isEmpty()) {
                return trace != null ? trace : "";
            }
            if (trace == null || trace.isEmpty()) {
                return extras;
            }
            return extras + "\n\n" + trace;
        }

        private static String readTrace(ApplicationExitInfo info) {
            InputStream in = null;
            try {
                in = info.getTraceInputStream();
                if (in == null) {
                    return "";
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                int total = 0;
                while ((n = in.read(buf)) != -1 && total < TRACE_MAX_BYTES) {
                    int take = Math.min(n, TRACE_MAX_BYTES - total);
                    bos.write(buf, 0, take);
                    total += take;
                }
                return bos.toString("UTF-8");
            } catch (Throwable t) {
                return "";
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
}
