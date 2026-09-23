package com.posbillingwala.pos_billingwala_v2

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.view.WindowCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

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

    companion object {
        private const val SCREENSHOT_CHANNEL = "pos_billingwala/screenshot"
    }
}
