package com.posbillingwala.pos_billingwala_v2

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.view.WindowCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayOutputStream

class MainActivity : FlutterActivity() {
    /* Keep POS UI fixed regardless of system Display size / Text size. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(fixedDisplayContext(newBase))
    }

    override fun onPostResume() {
        super.onPostResume()
        /* Fit system windows so layout stays fixed to OS status/nav insets
           (no jump when gesture nav briefly hides in landscape). */
        WindowCompat.setDecorFitsSystemWindows(window, true)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        applyFixedDisplay(newConfig)
        super.onConfigurationChanged(newConfig)
    }

    private fun fixedDisplayContext(base: Context): Context {
        val config = Configuration(base.resources.configuration)
        applyFixedDisplay(config)
        return base.createConfigurationContext(config)
    }

    private fun applyFixedDisplay(config: Configuration) {
        config.fontScale = 1.0f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.densityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            SCREENSHOT_CHANNEL,
        ).setMethodCallHandler { call, result ->
            if (call.method == "setAllowScreenshot") {
                val allow = call.arguments as? Boolean ?: true
                applyScreenshotPolicy(allow)
                result.success(null)
            } else {
                result.notImplemented()
            }
        }
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            PROCESS_EXIT_CHANNEL,
        ).setMethodCallHandler { call, result ->
            if (call.method == "collectProcessExits") {
                result.success(collectProcessExits())
            } else {
                result.notImplemented()
            }
        }
    }

    private fun applyScreenshotPolicy(allow: Boolean) {
        val win = window ?: return
        if (allow) {
            win.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            win.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
    }

    /**
     * Harvest ANR / crash / low-memory kills from ApplicationExitInfo (API 30+),
     * same idea as Android POS [ProcessExitLogCollector].
     */
    private fun collectProcessExits(): List<Map<String, Any?>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val am = getSystemService(ACTIVITY_SERVICE) as? ActivityManager ?: return emptyList()
        val prefs = getSharedPreferences(PROCESS_EXIT_PREFS, MODE_PRIVATE)
        val lastTs = prefs.getLong(PROCESS_EXIT_LAST_TS, 0L)
        val now = System.currentTimeMillis()
        val minTs = if (lastTs > 0) lastTs else (now - LOOKBACK_MS).coerceAtLeast(0)
        val exits = try {
            am.getHistoricalProcessExitReasons(packageName, 0, MAX_EXITS)
        } catch (_: Throwable) {
            return emptyList()
        }
        if (exits.isNullOrEmpty()) return emptyList()

        var newest = lastTs
        val out = ArrayList<Map<String, Any?>>()
        for (info in exits) {
            val ts = info.timestamp
            if (ts <= minTs) continue
            if (ts > newest) newest = ts
            if (!shouldSave(info)) continue
            val trace = readTrace(info)
            var description = info.description?.trim().orEmpty()
            if (info.reason == ApplicationExitInfo.REASON_ANR && description.isEmpty()) {
                description = "App not responding (ANR)"
            }
            out.add(
                mapOf(
                    "errorType" to reasonType(info),
                    "category" to reasonLabel(info),
                    "exceptionClass" to exceptionClassOf(description, info.reason),
                    "description" to description,
                    "stackTrace" to mergeTrace(trace, buildExtras(info)),
                    "severity" to severityFor(info),
                    "timestampMs" to ts,
                    "pid" to info.pid,
                    "reasonCode" to info.reason,
                ),
            )
        }
        if (newest > lastTs) {
            prefs.edit().putLong(PROCESS_EXIT_LAST_TS, newest).apply()
        }
        return out
    }

    private fun shouldSave(info: ApplicationExitInfo): Boolean {
        return when (info.reason) {
            ApplicationExitInfo.REASON_CRASH,
            ApplicationExitInfo.REASON_CRASH_NATIVE,
            ApplicationExitInfo.REASON_ANR,
            ApplicationExitInfo.REASON_LOW_MEMORY,
            -> true
            else -> false
        }
    }

    private fun reasonType(info: ApplicationExitInfo): String {
        return when (info.reason) {
            ApplicationExitInfo.REASON_CRASH_NATIVE -> "NATIVE_CRASH"
            ApplicationExitInfo.REASON_ANR -> "ANR"
            ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
            else -> "CRASH"
        }
    }

    private fun reasonLabel(info: ApplicationExitInfo): String {
        return when (info.reason) {
            ApplicationExitInfo.REASON_CRASH -> "java_crash"
            ApplicationExitInfo.REASON_CRASH_NATIVE -> "native_crash"
            ApplicationExitInfo.REASON_ANR -> "anr"
            ApplicationExitInfo.REASON_LOW_MEMORY -> "low_memory"
            else -> "process_exit"
        }
    }

    private fun exceptionClassOf(description: String, reason: Int): String {
        if (reason == ApplicationExitInfo.REASON_ANR) {
            return "android.app.ApplicationExitInfo.ANR"
        }
        if (reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
            return "NativeCrash"
        }
        if (reason == ApplicationExitInfo.REASON_LOW_MEMORY) {
            return "android.app.ApplicationExitInfo.LOW_MEMORY"
        }
        val lower = description.lowercase()
        if (lower.contains("nullpointer") || lower.contains("null pointer")) {
            return "java.lang.NullPointerException"
        }
        return "ProcessExit"
    }

    private fun severityFor(info: ApplicationExitInfo): String {
        return when (info.reason) {
            ApplicationExitInfo.REASON_ANR,
            ApplicationExitInfo.REASON_CRASH,
            ApplicationExitInfo.REASON_CRASH_NATIVE,
            -> "CRITICAL"
            ApplicationExitInfo.REASON_LOW_MEMORY -> "WARNING"
            else -> "ERROR"
        }
    }

    private fun buildExtras(info: ApplicationExitInfo): String {
        val sb = StringBuilder()
        sb.append("importance=").append(info.importance)
        if (info.pss > 0) sb.append("\npss_kb=").append(info.pss)
        if (info.reason == ApplicationExitInfo.REASON_LOW_MEMORY) {
            sb.append("\nnote=OS Low Memory Killer reclaimed this process")
        }
        return sb.toString()
    }

    private fun mergeTrace(trace: String, extras: String): String {
        if (extras.isEmpty()) return trace
        if (trace.isEmpty()) return extras
        return "$extras\n\n$trace"
    }

    private fun readTrace(info: ApplicationExitInfo): String {
        return try {
            info.traceInputStream?.use { input ->
                val bos = ByteArrayOutputStream()
                val buf = ByteArray(4096)
                var total = 0
                while (total < TRACE_MAX_BYTES) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    val take = minOf(n, TRACE_MAX_BYTES - total)
                    bos.write(buf, 0, take)
                    total += take
                }
                bos.toString(Charsets.UTF_8.name())
            } ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    companion object {
        private const val SCREENSHOT_CHANNEL = "pos_billingwala/screenshot"
        private const val PROCESS_EXIT_CHANNEL = "pos_billingwala/process_exit"
        private const val PROCESS_EXIT_PREFS = "pos_flutter_observability"
        private const val PROCESS_EXIT_LAST_TS = "last_process_exit_ts"
        private const val LOOKBACK_MS = 7L * 24 * 60 * 60 * 1000
        private const val MAX_EXITS = 20
        private const val TRACE_MAX_BYTES = 20480
    }
}
