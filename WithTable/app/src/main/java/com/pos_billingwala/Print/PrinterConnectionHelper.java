package com.pos_billingwala.Print;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.pos_billingwala.R;

/**
 * Single entry for bill + KOT printer connection, auto-reconnect, and safe writes.
 */
public final class PrinterConnectionHelper {

    private PrinterConnectionHelper() {
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
            if (BluetoothPrinterChannel.bill().isConnecting()) {
                showToast(activity, R.string.toast_printer_connecting);
                return false;
            }
            BluetoothPrinterChannel.bill().connect(activity, savedAddress, activity, true);
            return isBillPrinterReady();
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
            if (BluetoothPrinterChannel.kot().isConnecting()) {
                showToast(activity, R.string.toast_printer_connecting);
                return false;
            }
            BluetoothPrinterChannel.kot().connect(activity, savedAddress, activity, true);
            return isKotPrinterReady();
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
            if (!BluetoothPrinterChannel.bill().write(data)) {
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
            if (!BluetoothPrinterChannel.kot().write(data)) {
                showToast(context, R.string.toast_printer_not_connected_select);
                return false;
            }
            return true;
        } catch (Exception e) {
            showToast(context, R.string.connect_fail);
            return false;
        }
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
