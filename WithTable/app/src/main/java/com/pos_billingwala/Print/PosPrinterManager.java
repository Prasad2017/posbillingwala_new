package com.pos_billingwala.Print;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ErrorLogReporter;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.R;

import java.util.List;

/**
 * Creates and holds bill/KOT {@link PosPrinter} instances from saved settings.
 * Existing receipt byte generation stays unchanged — only transport is selected here.
 */
public final class PosPrinterManager {

    private static final String TAG = "PosPrinterManager";
    private static final PosPrinterManager INSTANCE = new PosPrinterManager();

    private volatile PrinterSettingResponse cachedSettings;
    private volatile PosPrinter billPrinter;
    private volatile PosPrinter kotPrinter;
    private volatile PrinterProfile billProfile;
    private volatile PrinterProfile kotProfile;

    private PosPrinterManager() {
    }

    public static PosPrinterManager get() {
        return INSTANCE;
    }

    public synchronized void refresh(Context context) {
        try {
            if (context == null) {
                return;
            }
            disconnectQuietly(billPrinter);
            disconnectQuietly(kotPrinter);
            POSBillingWalaDatabase db = new POSBillingWalaDatabase(context.getApplicationContext());
            List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
            cachedSettings = (list != null && !list.isEmpty()) ? list.get(0) : null;
            billProfile = PrinterProfile.forBill(cachedSettings);
            kotProfile = PrinterProfile.forKot(cachedSettings);
            billPrinter = createPrinter(context, null, billProfile, true);
            kotPrinter = createPrinter(context, null, kotProfile, false);
        } catch (Exception e) {
            Log.e(TAG, "refresh failed", e);
        }
    }

    public synchronized void refresh(Context context, Activity activity) {
        try {
            if (context == null) {
                return;
            }
            disconnectQuietly(billPrinter);
            disconnectQuietly(kotPrinter);
            POSBillingWalaDatabase db = new POSBillingWalaDatabase(context.getApplicationContext());
            List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
            cachedSettings = (list != null && !list.isEmpty()) ? list.get(0) : null;
            billProfile = PrinterProfile.forBill(cachedSettings);
            kotProfile = PrinterProfile.forKot(cachedSettings);
            billPrinter = createPrinter(context, activity, billProfile, true);
            kotPrinter = createPrinter(context, activity, kotProfile, false);
        } catch (Exception e) {
            Log.e(TAG, "refresh(activity) failed", e);
        }
    }

    private static void disconnectQuietly(PosPrinter printer) {
        if (printer == null) {
            return;
        }
        try {
            // Never release Bluetooth channel on refresh — connection is persistent.
            if (printer.getConnectionType() == PrinterConnectionType.BLUETOOTH) {
                return;
            }
            printer.disconnect();
        } catch (Exception ignored) {
        }
    }

    public PosPrinter bill(Context context, Activity activity) {
        softRefresh(context, activity);
        return billPrinter;
    }

    public PosPrinter kot(Context context, Activity activity) {
        softRefresh(context, activity);
        return kotPrinter;
    }

    public PrinterProfile billProfile(Context context) {
        softRefresh(context, null);
        return billProfile;
    }

    public PrinterProfile kotProfile(Context context) {
        softRefresh(context, null);
        return kotProfile;
    }

    public PrinterSettingResponse settings() {
        return cachedSettings;
    }

    /**
     * Print receipt bytes, then optional cut + drawer. Serialized per printer identity.
     */
    public boolean printJob(Context context, Activity activity, boolean billChannel,
                            byte[] receiptData, String paymentMode, boolean allowDrawer) {
        try {
            softRefresh(context, activity);
            final PosPrinter printer = billChannel ? billPrinter : kotPrinter;
            final PrinterProfile profile = billChannel ? billProfile : kotProfile;
            if (printer == null || receiptData == null || receiptData.length == 0) {
                return false;
            }
            String identity = printer.getPrinterIdentity();
            return PrintJobQueue.get().submitAndWait(identity, () -> {
                try {
                    if (!printer.isConnected() && !printer.connect()) {
                        logPrinterError(context, printer, profile,
                                new IllegalStateException("Printer not connected"), "connect");
                        showTransportToast(context, printer.getConnectionType());
                        return false;
                    }
                    if (!printer.print(receiptData)) {
                        logPrinterError(context, printer, profile,
                                new IllegalStateException("Print write failed"), "print");
                        showTransportToast(context, printer.getConnectionType());
                        return false;
                    }
                    return true;
                } catch (Exception e) {
                    logPrinterError(context, printer, profile, e, "print");
                    showTransportToast(context, printer.getConnectionType());
                    return false;
                }
            }, 90_000L);
        } catch (Exception e) {
            Log.e(TAG, "printJob failed", e);
            return false;
        }
    }

    /**
     * After successful print + paper feed: cut and optionally open cash drawer.
     */
    public void finishPrint(Context context, Activity activity, boolean billChannel,
                            String paymentMode, boolean allowDrawer) {
        try {
            softRefresh(context, activity);
            final PosPrinter printer = billChannel ? billPrinter : kotPrinter;
            final PrinterProfile profile = billChannel ? billProfile : kotProfile;
            if (printer == null || profile == null) {
                return;
            }
            PrintJobQueue.get().submitAndWait(printer.getPrinterIdentity(), () -> {
                try {
                    if (profile.shouldCut()) {
                        if (!printer.cut()) {
                            logPrinterError(context, printer, profile,
                                    new IllegalStateException("Cutter command failed"), "cut");
                            toast(context, R.string.toast_printer_cutter_unavailable);
                        }
                    }
                    if (billChannel && allowDrawer && profile.shouldOpenDrawer(paymentMode)) {
                        if (!printer.openCashDrawer()) {
                            logPrinterError(context, printer, profile,
                                    new IllegalStateException("Cash drawer command failed"), "drawer");
                            toast(context, R.string.toast_printer_drawer_unavailable);
                        }
                    }
                    return true;
                } catch (Exception e) {
                    logPrinterError(context, printer, profile, e, "finish");
                    return false;
                }
            }, 30_000L);
        } catch (Exception e) {
            Log.e(TAG, "finishPrint failed", e);
        }
    }

    public boolean testCut(Context context, Activity activity, boolean billChannel) {
        try {
            softRefresh(context, activity);
            PosPrinter printer = billChannel ? billPrinter : kotPrinter;
            PrinterProfile profile = billChannel ? billProfile : kotProfile;
            if (printer == null || profile == null) {
                return false;
            }
            return PrintJobQueue.get().submitAndWait(printer.getPrinterIdentity(), () -> {
                if (!printer.isConnected() && !printer.connect()) {
                    showTransportToast(context, printer.getConnectionType());
                    return false;
                }
                if (!profile.supportsCutter) {
                    toast(context, R.string.toast_printer_cutter_unavailable);
                    return false;
                }
                boolean ok = printer.print(profile.cutBytes());
                if (!ok) {
                    toast(context, R.string.toast_printer_cutter_unavailable);
                }
                return ok;
            }, 30_000L);
        } catch (Exception e) {
            Log.e(TAG, "testCut failed", e);
            toast(context, R.string.toast_printer_cutter_unavailable);
            return false;
        }
    }

    public boolean testDrawer(Context context, Activity activity, boolean billChannel) {
        try {
            softRefresh(context, activity);
            PosPrinter printer = billChannel ? billPrinter : kotPrinter;
            PrinterProfile profile = billChannel ? billProfile : kotProfile;
            if (printer == null || profile == null) {
                return false;
            }
            return PrintJobQueue.get().submitAndWait(printer.getPrinterIdentity(), () -> {
                if (!printer.isConnected() && !printer.connect()) {
                    showTransportToast(context, printer.getConnectionType());
                    return false;
                }
                if (!profile.supportsCashDrawer) {
                    toast(context, R.string.toast_printer_drawer_unavailable);
                    return false;
                }
                boolean ok = printer.openCashDrawer();
                if (!ok) {
                    toast(context, R.string.toast_printer_drawer_unavailable);
                }
                return ok;
            }, 30_000L);
        } catch (Exception e) {
            Log.e(TAG, "testDrawer failed", e);
            toast(context, R.string.toast_printer_drawer_unavailable);
            return false;
        }
    }

    /**
     * Reloads settings and recreates printers only when connection identity changed.
     */
    public synchronized void softRefresh(Context context, Activity activity) {
        try {
            if (context == null) {
                return;
            }
            POSBillingWalaDatabase db = new POSBillingWalaDatabase(context.getApplicationContext());
            List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
            PrinterSettingResponse next = (list != null && !list.isEmpty()) ? list.get(0) : null;
            PrinterProfile nextBill = PrinterProfile.forBill(next);
            PrinterProfile nextKot = PrinterProfile.forKot(next);
            boolean billChanged = billProfile == null
                    || !nextBill.identityKey().equals(billProfile.identityKey())
                    || nextBill.connectionType != billProfile.connectionType
                    || nextBill.autoCut != billProfile.autoCut
                    || nextBill.supportsCashDrawer != billProfile.supportsCashDrawer
                    || nextBill.drawerOpenMode != billProfile.drawerOpenMode;
            boolean kotChanged = kotProfile == null
                    || !nextKot.identityKey().equals(kotProfile.identityKey())
                    || nextKot.connectionType != kotProfile.connectionType
                    || nextKot.autoCut != kotProfile.autoCut;
            cachedSettings = next;
            if (billChanged) {
                disconnectQuietly(billPrinter);
                billProfile = nextBill;
                billPrinter = createPrinter(context, activity, billProfile, true);
            } else {
                billProfile = nextBill;
            }
            if (kotChanged) {
                disconnectQuietly(kotPrinter);
                kotProfile = nextKot;
                kotPrinter = createPrinter(context, activity, kotProfile, false);
            } else {
                kotProfile = nextKot;
            }
            if (billPrinter == null) {
                billPrinter = createPrinter(context, activity, billProfile, true);
            }
            if (kotPrinter == null) {
                kotPrinter = createPrinter(context, activity, kotProfile, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "softRefresh failed", e);
        }
    }

    private static PosPrinter createPrinter(Context context, Activity activity,
                                            PrinterProfile profile, boolean isBill) {
        if (profile == null) {
            profile = new PrinterProfile.Builder().channel(isBill ? "bill" : "kot").build();
        }
        switch (profile.connectionType) {
            case USB:
                return new UsbPosPrinter(context, profile);
            case WIFI:
                return new WifiPosPrinter(profile);
            case BLUETOOTH:
            default:
                return new BluetoothPosPrinter(context, activity, profile, isBill);
        }
    }

    public static void showTransportToast(Context context, PrinterConnectionType type) {
        if (type == null) {
            toast(context, R.string.connect_fail);
            return;
        }
        switch (type) {
            case USB:
                toast(context, R.string.toast_usb_printer_unavailable);
                break;
            case WIFI:
                toast(context, R.string.toast_network_printer_unavailable);
                break;
            case BLUETOOTH:
            default:
                toast(context, R.string.toast_bluetooth_printer_unavailable);
                break;
        }
    }

    private static void toast(Context context, int resId) {
        if (context == null) {
            return;
        }
        try {
            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.post(() -> {
                try {
                    android.widget.Toast.makeText(context.getApplicationContext(),
                            context.getString(resId), android.widget.Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static void logPrinterError(Context context, PosPrinter printer, PrinterProfile profile,
                                        Throwable error, String operation) {
        try {
            String type = printer != null ? printer.getConnectionType().toStored() : "";
            String model = profile != null ? profile.printerModel : "";
            String connection = printer != null ? printer.getDisplayName() : "";
            ErrorLogReporter.reportPrinterError(error, type, model, connection, operation);
            ErrorLogReporter.addBreadcrumb("printer:" + type + ":" + operation);
        } catch (Exception e) {
            Log.e(TAG, "logPrinterError failed", e);
        }
    }
}
