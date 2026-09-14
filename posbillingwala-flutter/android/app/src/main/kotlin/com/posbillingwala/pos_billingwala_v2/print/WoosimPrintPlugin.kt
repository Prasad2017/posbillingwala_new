package com.posbillingwala.pos_billingwala_v2.print

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.woosim.printer.WoosimService
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/** Android print path matching WithTable [BluetoothPrinterChannel] + WoosimService. */
class WoosimPrintPlugin(
    private val context: Context,
) : MethodCallHandler {

    private val bill = Session("bill", context)
    private val kot = Session("kot", context)

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val kind = call.argument<String>("kind") ?: "bill"
        val session = if (kind == "kot") kot else bill
        when (call.method) {
            "connect" -> {
                val mac = call.argument<String>("mac").orEmpty()
                Thread({
                    val ok = session.connect(mac)
                    Handler(Looper.getMainLooper()).post { result.success(ok) }
                }, "woosim-$kind-connect").start()
            }
            "write" -> {
                val bytes = call.argument<ByteArray>("bytes")
                Thread({
                    val ok = session.write(bytes)
                    Handler(Looper.getMainLooper()).post { result.success(ok) }
                }, "woosim-$kind-write").start()
            }
            "disconnect" -> {
                session.disconnect()
                result.success(true)
            }
            "isReady" -> result.success(session.isReady())
            else -> result.notImplemented()
        }
    }

    private class Session(
        private val name: String,
        context: Context,
    ) {
        private val app = context.applicationContext
        private var printService: BluetoothPrintService? = null
        private var woosimService: WoosimService? = null
        private val handler: Handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == BluetoothPrintCallbacks.MESSAGE_READ &&
                    woosimService != null &&
                    msg.obj is ByteArray
                ) {
                    woosimService!!.processRcvData(msg.obj as ByteArray, msg.arg1)
                }
            }
        }

        fun connect(mac: String): Boolean {
            val address = mac.trim()
            if (address.isEmpty()) return false
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            if (!adapter.isEnabled) return false
            ensureService()
            val service = printService ?: return false
            if (service.getState() == BluetoothPrintService.STATE_CONNECTED &&
                service.connectedDeviceAddress.equals(address, ignoreCase = true)
            ) {
                return true
            }
            val device: BluetoothDevice = try {
                adapter.getRemoteDevice(address)
            } catch (_: Exception) {
                return false
            }
            service.connect(device, true)
            val deadline = System.currentTimeMillis() + 12_000
            while (System.currentTimeMillis() < deadline) {
                if (service.getState() == BluetoothPrintService.STATE_CONNECTED) {
                    return true
                }
                try {
                    Thread.sleep(150)
                } catch (_: InterruptedException) {
                    break
                }
            }
            return service.getState() == BluetoothPrintService.STATE_CONNECTED
        }

        fun write(bytes: ByteArray?): Boolean {
            if (bytes == null || bytes.isEmpty()) return false
            val service = printService ?: return false
            if (service.getState() != BluetoothPrintService.STATE_CONNECTED) {
                return false
            }
            return service.write(bytes)
        }

        fun disconnect() {
            printService?.stop()
        }

        fun isReady(): Boolean {
            return printService?.getState() == BluetoothPrintService.STATE_CONNECTED
        }

        private fun ensureService() {
            if (woosimService == null) {
                woosimService = WoosimService(handler)
            }
            if (printService == null) {
                printService = BluetoothPrintService(app, handler)
            }
        }
    }

    companion object {
        const val CHANNEL = "pos_billingwala/woosim_print"

        fun register(engine: FlutterEngine, activity: Activity) {
            MethodChannel(engine.dartExecutor.binaryMessenger, CHANNEL)
                .setMethodCallHandler(WoosimPrintPlugin(activity))
        }
    }
}
