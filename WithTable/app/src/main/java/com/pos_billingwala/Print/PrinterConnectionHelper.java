package com.pos_billingwala.Print;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.pos_billingwala.R;

/**
 * Single entry for bill + KOT printer connection, auto-reconnect, and safe writes.
 */
public final class PrinterConnectionHelper {

    private static final long CONNECT_WAIT_MS = 12000L;

    private PrinterConnectionHelper() {
    }

    /** Initialize persistent printer sessions for the app process. */
    public static void initializeApp(Context context) {
        BluetoothPrinterChannel.initializeApp(context);
    }

    public static void shutdownApp(Context context) {
        BluetoothPrinterChannel.shutdownApp(context);
    }

    public static boolean isBillPrinterReady() {
        return BluetoothPrinterChannel.bill().isReady();
    }

    public static boolean isKotPrinterReady() {
        return BluetoothPrinterChannel.kot().isReady();
    }

    public static void autoConnectBillPrinter(Context context, String savedAddress) {
        try {
            BluetoothPrinterChannel.bill().autoConnect(context, savedAddress);
        } catch (Exception ignored) {
        }
    }

    public static void autoConnectKotPrinter(Context context, String savedAddress) {
        try {
            BluetoothPrinterChannel.kot().autoConnect(context, savedAddress);
        } catch (Exception ignored) {
        }
    }

    public static boolean ensureBillPrinter(Activity activity, String savedAddress) {
        try {
            if (isBillPrinterReady()) {
                return true;
            }
            String addr = normalize(savedAddress);
            if (addr.isEmpty()) {
                showToast(activity, R.string.toast_printer_not_connected_select);
                return false;
            }
            WoosimPrnMng.connect(activity, addr, activity);
            BluetoothPrinterChannel channel = BluetoothPrinterChannel.bill();
            if (channel.isReady()) {
                return true;
            }
            if (channel.isConnecting()) {
                return channel.waitUntilReady(CONNECT_WAIT_MS);
            }
            return false;
        } catch (Exception e) {
            showToast(activity, R.string.connect_fail);
            return false;
        }
    }

    public static boolean ensureKotPrinter(Activity activity, String savedAddress) {
        try {
            if (isKotPrinterReady()) {
                return true;
            }
            String addr = normalize(savedAddress);
            if (addr.isEmpty()) {
                showToast(activity, R.string.toast_printer_not_connected_select);
                return false;
            }
            KOTWoosimPrnMng.connect(activity, addr, activity);
            BluetoothPrinterChannel channel = BluetoothPrinterChannel.kot();
            if (channel.isReady()) {
                return true;
            }
            if (channel.isConnecting()) {
                return channel.waitUntilReady(CONNECT_WAIT_MS);
            }
            return false;
        } catch (Exception e) {
            showToast(activity, R.string.connect_fail);
            return false;
        }
    }

    public static boolean safeWriteBill(Context context, byte[] data) {
        try {
            if (data == null || data.length == 0) {
                showToast(context, R.string.print_error);
                return false;
            }
            BluetoothPrinterChannel channel = BluetoothPrinterChannel.bill();
            if (!channel.isReady() && channel.isConnecting()) {
                channel.waitUntilReady(CONNECT_WAIT_MS);
            }
            if (!channel.write(data)) {
                showToast(context, R.string.toast_printer_not_connected_select);
                return false;
            }
            return true;
        } catch (Exception e) {
            showToast(context, R.string.connect_fail);
            return false;
        }
    }

    public static boolean safeWriteKot(Context context, byte[] data) {
        try {
            if (data == null || data.length == 0) {
                showToast(context, R.string.print_error);
                return false;
            }
            BluetoothPrinterChannel channel = BluetoothPrinterChannel.kot();
            if (!channel.isReady() && channel.isConnecting()) {
                channel.waitUntilReady(CONNECT_WAIT_MS);
            }
            if (!channel.write(data)) {
                showToast(context, R.string.toast_printer_not_connected_select);
                return false;
            }
            return true;
        } catch (Exception e) {
            showToast(context, R.string.connect_fail);
            return false;
        }
    }

    private static String normalize(String address) {
        return address != null ? address.trim() : "";
    }

    private static void showToast(Context context, int messageRes) {
        if (context == null) {
            return;
        }
        try {
            Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }
}
