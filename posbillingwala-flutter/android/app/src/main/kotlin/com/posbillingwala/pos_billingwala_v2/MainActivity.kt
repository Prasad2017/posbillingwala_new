package com.posbillingwala.pos_billingwala_v2

import com.posbillingwala.pos_billingwala_v2.print.WoosimPrintPlugin
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        WoosimPrintPlugin.register(flutterEngine, this)
    }
}
