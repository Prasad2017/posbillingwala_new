package com.pos_billingwala.Print;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.pos_billingwala.Extra.ErrorLogReporter;
import com.pos_billingwala.R;

/**
 * Single entry for bill + KOT printer connection, safe writes, cut, and cash drawer.
 * Routes through {@link PosPrinterManager} so Bluetooth / USB / Wi-Fi share one path.
 * Existing receipt byte generation is unchanged.
 */
public final class PrinterConnectionHelper {

    private static final String TAG = "PrinterConnHelper";

    private PrinterConnectionHelper() {
    }

    public static boolean isBillPrinterReady() {
        try {
            return BluetoothPrinterChannel.bill().isReady()
                    || isNonBluetoothReady(true);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isKotPrinterReady() {
        try {
            return BluetoothPrinterChannel.kot().isReady()
                    || isNonBluetoothReady(false);
        } catch (Exception e) {
            return false;
        }
    }

    public static void autoConnectBillPrinter(Context context, String savedAddress) {
        try {
            PosPrinterManager.get().softRefresh(context,
                    context instanceof Activity ? (Activity) context : null);
            PrinterProfile profile = PosPrinterManager.get().billProfile(context);
            if (profile != null && profile.connectionType == PrinterConnectionType.BLUETOOTH) {
                BluetoothPrinterChannel.bill().autoConnect(context, savedAddress);
            }
        } catch (Exception e) {
            Log.e(TAG, "autoConnectBillPrinter failed", e);
        }
    }

    public static void autoConnectKotPrinter(Context context, String savedAddress) {
        try {
            PosPrinterManager.get().softRefresh(context,
                    context instanceof Activity ? (Activity) context : null);
            PrinterProfile profile = PosPrinterManager.get().kotProfile(context);
            if (profile != null && profile.connectionType == PrinterConnectionType.BLUETOOTH) {
                BluetoothPrinterChannel.kot().autoConnect(context, savedAddress);
            }
        } catch (Exception e) {
            Log.e(TAG, "autoConnectKotPrinter failed", e);
        }
    }

    public static boolean ensureBillPrinter(Activity activity, String savedAddress) {
        try {
            PosPrinterManager.get().softRefresh(activity, activity);
            PrinterProfile profile = PosPrinterManager.get().billProfile(activity);
            if (profile == null || profile.connectionType == PrinterConnectionType.BLUETOOTH) {
                if (isBillPrinterReady() && BluetoothPrinterChannel.bill().isReady()) {
                    return true;
                }
                if (BluetoothPrinterChannel.bill().isConnecting()) {
                    showToast(activity, R.string.toast_printer_connecting);
                    return false;
                }
                BluetoothPrinterChannel.bill().connect(activity, savedAddress, activity, false);
                return BluetoothPrinterChannel.bill().isReady();
            }
            PosPrinter printer = PosPrinterManager.get().bill(activity, activity);
            // Do not block UI with USB permission / TCP connect — print job will connect.
            return printer != null;
        } catch (Exception e) {
            Log.e(TAG, "ensureBillPrinter failed", e);
            showToast(activity, R.string.toast_bluetooth_printer_unavailable);
            return false;
        }
    }

    public static boolean ensureKotPrinter(Activity activity, String savedAddress) {
        try {
            PosPrinterManager.get().softRefresh(activity, activity);
            PrinterProfile profile = PosPrinterManager.get().kotProfile(activity);
            if (profile == null || profile.connectionType == PrinterConnectionType.BLUETOOTH) {
                if (isKotPrinterReady() && BluetoothPrinterChannel.kot().isReady()) {
                    return true;
                }
                if (BluetoothPrinterChannel.kot().isConnecting()) {
                    showToast(activity, R.string.toast_printer_connecting);
                    return false;
                }
                BluetoothPrinterChannel.kot().connect(activity, savedAddress, activity, false);
                return BluetoothPrinterChannel.kot().isReady();
            }
            PosPrinter printer = PosPrinterManager.get().kot(activity, activity);
            return printer != null;
        } catch (Exception e) {
            Log.e(TAG, "ensureKotPrinter failed", e);
            showToast(activity, R.string.toast_bluetooth_printer_unavailable);
            return false;
        }
    }

    public static boolean safeWriteBill(Context context, byte[] data) {
        try {
            if (data == null || data.length == 0) {
                showToast(context, R.string.print_error);
                return false;
            }
            Activity activity = context instanceof Activity ? (Activity) context : null;
            PosPrinterManager.get().softRefresh(context, activity);
            PrinterProfile profile = PosPrinterManager.get().billProfile(context);
            if (profile != null && profile.connectionType != PrinterConnectionType.BLUETOOTH) {
                return PosPrinterManager.get().printJob(context, activity, true, data, null, false);
            }
            // Bluetooth path — keep existing channel write, but serialize per printer
            String identity = profile != null ? profile.identityKey() : "bt:bill";
            return PrintJobQueue.get().submitAndWait(identity, () -> {
                if (!BluetoothPrinterChannel.bill().write(data)) {
                    showToast(context, R.string.toast_bluetooth_printer_unavailable);
                    return false;
                }
                return true;
            }, 60_000L);
        } catch (Exception e) {
            Log.e(TAG, "safeWriteBill failed", e);
            ErrorLogReporter.reportPrinterError(e, "BLUETOOTH", "", "", "write_bill");
            showToast(context, R.string.toast_bluetooth_printer_unavailable);
            return false;
        }
    }

    public static boolean safeWriteKot(Context context, byte[] data) {
        try {
            if (data == null || data.length == 0) {
                showToast(context, R.string.print_error);
                return false;
            }
            Activity activity = context instanceof Activity ? (Activity) context : null;
            PosPrinterManager.get().softRefresh(context, activity);
            PrinterProfile profile = PosPrinterManager.get().kotProfile(context);
            if (profile != null && profile.connectionType != PrinterConnectionType.BLUETOOTH) {
                return PosPrinterManager.get().printJob(context, activity, false, data, null, false);
            }
            String identity = profile != null ? profile.identityKey() : "bt:kot";
            return PrintJobQueue.get().submitAndWait(identity, () -> {
                if (!BluetoothPrinterChannel.kot().write(data)) {
                    showToast(context, R.string.toast_bluetooth_printer_unavailable);
                    return false;
                }
                return true;
            }, 60_000L);
        } catch (Exception e) {
            Log.e(TAG, "safeWriteKot failed", e);
            ErrorLogReporter.reportPrinterError(e, "BLUETOOTH", "", "", "write_kot");
            showToast(context, R.string.toast_bluetooth_printer_unavailable);
            return false;
        }
    }

    /**
     * After successful receipt + paper feed: auto-cut and optional cash drawer.
     */
    public static void finishBillPrint(Context context, String paymentMode) {
        try {
            Activity activity = context instanceof Activity ? (Activity) context : null;
            PosPrinterManager.get().finishPrint(context, activity, true, paymentMode, true);
        } catch (Exception e) {
            Log.e(TAG, "finishBillPrint failed", e);
        }
    }

    /**
     * After successful KOT + paper feed: auto-cut only (no cash drawer).
     */
    public static void finishKotPrint(Context context) {
        try {
            Activity activity = context instanceof Activity ? (Activity) context : null;
            PosPrinterManager.get().finishPrint(context, activity, false, null, false);
        } catch (Exception e) {
            Log.e(TAG, "finishKotPrint failed", e);
        }
    }

    public static boolean testCutter(Context context) {
        Activity activity = context instanceof Activity ? (Activity) context : null;
        return PosPrinterManager.get().testCut(context, activity, true);
    }

    public static boolean testCashDrawer(Context context) {
        Activity activity = context instanceof Activity ? (Activity) context : null;
        return PosPrinterManager.get().testDrawer(context, activity, true);
    }

    private static boolean isNonBluetoothReady(boolean bill) {
        try {
            PrinterProfile profile = bill
                    ? PosPrinterManager.get().billProfile(null)
                    : PosPrinterManager.get().kotProfile(null);
            if (profile == null || profile.connectionType == PrinterConnectionType.BLUETOOTH) {
                return false;
            }
            PosPrinter printer = bill
                    ? PosPrinterManager.get().bill(null, null)
                    : PosPrinterManager.get().kot(null, null);
            return printer != null && printer.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private static void showToast(Context context, int messageRes) {
        if (context == null) {
            return;
        }
        try {
            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.post(() -> {
                try {
                    Toast.makeText(context.getApplicationContext(),
                            context.getString(messageRes), Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }
}
