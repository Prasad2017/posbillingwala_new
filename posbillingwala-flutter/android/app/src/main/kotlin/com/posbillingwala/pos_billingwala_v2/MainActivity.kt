package com.posbillingwala.pos_billingwala_v2

import android.view.WindowManager
import com.posbillingwala.pos_billingwala_v2.print.WoosimPrintPlugin
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        WoosimPrintPlugin.register(flutterEngine, this)
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
