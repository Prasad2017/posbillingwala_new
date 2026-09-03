package com.pos_billingwala.Print;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.pos_billingwala.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single entry for bill + KOT printer connection, auto-reconnect, and safe writes.
 * <p>
 * Never blocks the main thread waiting for RFCOMM — that caused
 * "Changing to new focus window timeout" ANRs during billing print and Settings connect.
 * <p>
 * If the saved printer is missing/unpaired or connect fails, opens the paired device list
 * so the user can pick a new printer, then resumes any pending print callback.
 */
public final class PrinterConnectionHelper {

    private static final long CONNECT_WAIT_MS = 12000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService CONNECT_WAIT_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "printer-connect-wait");
                t.setDaemon(true);
                return t;
            });

    private static volatile Runnable pendingBillReady;
    private static volatile Runnable pendingKotReady;
    private static volatile Runnable pendingBillFailed;
    private static volatile Runnable pendingKotFailed;

    private PrinterConnectionHelper() {
    }

    /** Initialize persistent printer sessions for the app process. */
    public static void initializeApp(Context context) {
        BluetoothPrinterChannel.initializeApp(context);
    }

    public static void shutdownApp(Context context) {
        BluetoothPrinterChannel.shutdownApp(context);
        clearPending(true);
        clearPending(false);
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

    /**
     * Non-blocking: returns true only if already connected.
     * If offline / not found, opens the paired device list (no UI sleep).
     */
    public static boolean ensureBillPrinter(Activity activity, String savedAddress) {
        try {
            if (isBillPrinterReady()) {
                return true;
            }
            String addr = normalize(savedAddress);
            if (addr.isEmpty() || !BluetoothPrinterChannel.bill().isPairedAddress(addr)) {
                openPickerForUser(activity, true, R.string.toast_printer_not_connected_select);
                return false;
            }
            WoosimPrnMng.connect(activity, addr, activity);
            if (isBillPrinterReady()) {
                return true;
            }
            showToast(activity, R.string.toast_printer_connecting);
            return false;
        } catch (Exception e) {
            openPickerForUser(activity, true, R.string.toast_printer_not_found_select);
            return false;
        }
    }

    /**
     * Non-blocking: returns true only if already connected.
     * If offline / not found, opens the paired device list (no UI sleep).
     */
    public static boolean ensureKotPrinter(Activity activity, String savedAddress) {
        try {
            if (isKotPrinterReady()) {
                return true;
            }
            String addr = normalize(savedAddress);
            if (addr.isEmpty() || !BluetoothPrinterChannel.kot().isPairedAddress(addr)) {
                openPickerForUser(activity, false, R.string.toast_printer_not_connected_select);
                return false;
            }
            KOTWoosimPrnMng.connect(activity, addr, activity);
            if (isKotPrinterReady()) {
                return true;
            }
            showToast(activity, R.string.toast_printer_connecting);
            return false;
        } catch (Exception e) {
            openPickerForUser(activity, false, R.string.toast_printer_not_found_select);
            return false;
        }
    }

    /**
     * Ensures the bill printer is ready, then runs {@code onReady} on the main thread.
     * Waits for an in-flight connect on a background thread only — never on the UI thread.
     * If the saved printer is unpaired or connect fails, opens the paired device list.
     */
    public static void ensureBillPrinterAsync(Activity activity, String savedAddress, Runnable onReady) {
        ensurePrinterAsync(activity, savedAddress, true, onReady, null);
    }

    public static void ensureKotPrinterAsync(Activity activity, String savedAddress, Runnable onReady) {
        ensurePrinterAsync(activity, savedAddress, false, onReady, null);
    }

    public static void ensureBillPrinterAsync(Activity activity, String savedAddress,
                                             Runnable onReady, Runnable onFailed) {
        ensurePrinterAsync(activity, savedAddress, true, onReady, onFailed);
    }

    public static void ensureKotPrinterAsync(Activity activity, String savedAddress,
                                            Runnable onReady, Runnable onFailed) {
        ensurePrinterAsync(activity, savedAddress, false, onReady, onFailed);
    }

    /**
     * Call from {@code onActivityResult} after the user picks a bill printer.
     * Connects the new device and resumes any pending print callback.
     */
    public static void onBillDevicePicked(Activity activity, String address) {
        String addr = normalize(address);
        if (addr.isEmpty()) {
            cancelPendingDevicePick(true);
            return;
        }
        BluetoothPrinterChannel.bill().onDevicePicked(addr);
        resumeAfterDevicePicked(activity, true, addr);
    }

    /**
     * Call from {@code onActivityResult} after the user picks a KOT printer.
     */
    public static void onKotDevicePicked(Activity activity, String address) {
        String addr = normalize(address);
        if (addr.isEmpty()) {
            cancelPendingDevicePick(false);
            return;
        }
        BluetoothPrinterChannel.kot().onDevicePicked(addr);
        resumeAfterDevicePicked(activity, false, addr);
    }

    /** Call when the device list is cancelled (RESULT_CANCELED). */
    public static void cancelPendingDevicePick(boolean bill) {
        Runnable failed = bill ? pendingBillFailed : pendingKotFailed;
        clearPending(bill);
        if (failed != null) {
            MAIN.post(failed);
        }
    }

    private static void ensurePrinterAsync(Activity activity, String savedAddress, boolean bill,
                                          Runnable onReady, Runnable onFailed) {
        if (activity == null || onReady == null) {
            return;
        }
        if (activity.isFinishing()) {
            return;
        }
        boolean ready = bill ? isBillPrinterReady() : isKotPrinterReady();
        if (ready) {
            onReady.run();
            return;
        }

        BluetoothPrinterChannel channel = bill
                ? BluetoothPrinterChannel.bill()
                : BluetoothPrinterChannel.kot();
        String addr = normalize(savedAddress);

        // No saved MAC, or saved MAC not in paired list → let user choose.
        if (addr.isEmpty() || !channel.isPairedAddress(addr)) {
            stashPending(bill, onReady, onFailed);
            openPickerForUser(activity, bill,
                    addr.isEmpty()
                            ? R.string.toast_printer_not_connected_select
                            : R.string.toast_printer_not_found_select);
            if (onFailed != null) {
                onFailed.run();
            }
            return;
        }

        try {
            if (bill) {
                WoosimPrnMng.connect(activity, addr, activity);
            } else {
                KOTWoosimPrnMng.connect(activity, addr, activity);
            }
        } catch (Exception e) {
            stashPending(bill, onReady, onFailed);
            openPickerForUser(activity, bill, R.string.toast_printer_not_found_select);
            if (onFailed != null) {
                onFailed.run();
            }
            return;
        }

        if (bill ? isBillPrinterReady() : isKotPrinterReady()) {
            onReady.run();
            return;
        }

        showToast(activity, R.string.toast_printer_connecting);
        CONNECT_WAIT_EXECUTOR.execute(() -> {
            // Brief grace so ConnectThread can enter STATE_CONNECTING before we poll.
            try {
                Thread.sleep(300L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            boolean ok = channel.isReady() || channel.waitUntilReady(CONNECT_WAIT_MS);
            MAIN.post(() -> {
                if (activity.isFinishing()) {
                    if (onFailed != null) {
                        onFailed.run();
                    }
                    return;
                }
                if (ok || (bill ? isBillPrinterReady() : isKotPrinterReady())) {
                    onReady.run();
                } else {
                    // Saved printer not reachable — show paired list for a new choice.
                    stashPending(bill, onReady, onFailed);
                    openPickerForUser(activity, bill, R.string.toast_printer_not_found_select);
                    if (onFailed != null) {
                        onFailed.run();
                    }
                }
            });
        });
    }

    private static void resumeAfterDevicePicked(Activity activity, boolean bill, String address) {
        Runnable onReady = bill ? pendingBillReady : pendingKotReady;
        Runnable onFailed = bill ? pendingBillFailed : pendingKotFailed;
        clearPending(bill);
        if (activity == null || activity.isFinishing()) {
            if (onFailed != null) {
                onFailed.run();
            }
            return;
        }
        if (onReady == null) {
            // Picker opened from Settings / Connect button — just connect, no print resume.
            return;
        }
        // Wait for the just-started connect, then continue print.
        showToast(activity, R.string.toast_printer_connecting);
        CONNECT_WAIT_EXECUTOR.execute(() -> {
            BluetoothPrinterChannel channel = bill
                    ? BluetoothPrinterChannel.bill()
                    : BluetoothPrinterChannel.kot();
            try {
                Thread.sleep(300L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            boolean ok = channel.isReady() || channel.waitUntilReady(CONNECT_WAIT_MS);
            MAIN.post(() -> {
                if (activity.isFinishing()) {
                    if (onFailed != null) {
                        onFailed.run();
                    }
                    return;
                }
                if (ok || (bill ? isBillPrinterReady() : isKotPrinterReady())) {
                    onReady.run();
                } else {
                    showToast(activity, R.string.connect_fail);
                    if (onFailed != null) {
                        onFailed.run();
                    }
                }
            });
        });
    }

    private static void stashPending(boolean bill, Runnable onReady, Runnable onFailed) {
        if (bill) {
            pendingBillReady = onReady;
            pendingBillFailed = onFailed;
        } else {
            pendingKotReady = onReady;
            pendingKotFailed = onFailed;
        }
    }

    private static void clearPending(boolean bill) {
        if (bill) {
            pendingBillReady = null;
            pendingBillFailed = null;
        } else {
            pendingKotReady = null;
            pendingKotFailed = null;
        }
    }

    private static void openPickerForUser(Activity activity, boolean bill, int toastRes) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        showToast(activity, toastRes);
        try {
            if (bill) {
                BluetoothPrinterChannel.bill().openDevicePicker(activity);
            } else {
                BluetoothPrinterChannel.kot().openDevicePicker(activity);
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean safeWriteBill(Context context, byte[] data) {
        try {
            if (data == null || data.length == 0) {
                showToast(context, R.string.print_error);
                return false;
            }
            BluetoothPrinterChannel channel = BluetoothPrinterChannel.bill();
            waitOffMainIfConnecting(channel);
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
            waitOffMainIfConnecting(channel);
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

    /** Sleep only when called from a worker thread (print executors). */
    private static void waitOffMainIfConnecting(BluetoothPrinterChannel channel) {
        if (channel == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return;
        }
        if (!channel.isReady() && channel.isConnecting()) {
            channel.waitUntilReady(CONNECT_WAIT_MS);
        }
    }

    private static String normalize(String address) {
        return address != null ? address.trim() : "";
    }

    private static void showToast(Context context, int messageRes) {
        if (context == null) {
            return;
        }
        Runnable show = () -> {
            try {
                Toast.makeText(context.getApplicationContext(),
                        context.getApplicationContext().getString(messageRes),
                        Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            show.run();
        } else {
            MAIN.post(show);
        }
    }
}
