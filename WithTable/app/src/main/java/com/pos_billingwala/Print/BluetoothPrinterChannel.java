package com.pos_billingwala.Print;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.Set;

import com.pos_billingwala.R;
import com.woosim.printer.WoosimService;

/**
 * One persistent Bluetooth printer session (bill or KOT).
 * <ul>
 *   <li>Stays connected to the same paired address — does not reconnect if already connected.</li>
 *   <li>Auto-reconnects silently when the link drops but the saved address is still set.</li>
 *   <li>Only disconnects when the user taps Disconnect or the app process ends.</li>
 *   <li>Never throws — failures are surfaced as toasts only when appropriate.</li>
 * </ul>
 */
@SuppressLint("MissingPermission")
public final class BluetoothPrinterChannel {

    private static final String TAG = "BtPrinterChannel";
    private static final long RECONNECT_DELAY_MS = 2500L;
    private static final long RECONNECT_MAX_DELAY_MS = 30000L;
    /** Max wait for an in-flight connect before giving up on a print write. */
    private static final long CONNECT_WAIT_MS = 12000L;
    /** Bill + KOT must not open two RFCOMM sockets to the same MAC at once. */
    private static final Object GLOBAL_CONNECT_LOCK = new Object();

    private static final BluetoothPrinterChannel BILL = new BluetoothPrinterChannel("bill");
    private static final BluetoothPrinterChannel KOT = new BluetoothPrinterChannel("kot");

    private final String channelName;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Context appContext;
    private Activity hostActivity;
    private BluetoothPrintService printService;
    private WoosimService woosimService;
    private Handler printHandler;

    private String savedAddress = "";
    private boolean userInitiatedSession;
    /** True after a successful connect; cleared only by explicit disconnect. */
    private boolean persistentSession;
    private boolean reconnectScheduled;
    private long reconnectBackoffMs = RECONNECT_DELAY_MS;

    private final Runnable reconnectRunnable;

    private BluetoothPrinterChannel(String channelName) {
        this.channelName = channelName;
        this.reconnectRunnable = () -> {
            reconnectScheduled = false;
            if (savedAddress == null || savedAddress.trim().isEmpty()) {
                return;
            }
            if (isReady()) {
                return;
            }
            Log.i(TAG, channelName + ": auto-reconnect to " + savedAddress);
            connectInternal(savedAddress, false);
        };
    }

    public static BluetoothPrinterChannel bill() {
        return BILL;
    }

    public static BluetoothPrinterChannel kot() {
        return KOT;
    }

    /** Call once from {@link android.app.Application#onCreate()} for app-scoped sessions. */
    public static void initializeApp(Context context) {
        if (context == null) {
            return;
        }
        Context app = context.getApplicationContext();
        BILL.ensureAppContext(app);
        KOT.ensureAppContext(app);
    }

    /** Tear down both channels when the app process is ending. */
    public static void shutdownApp(Context context) {
        if (context == null) {
            return;
        }
        Context app = context.getApplicationContext();
        BILL.shutdownInternal(app);
        KOT.shutdownInternal(app);
    }

    /** Silent background connect — used from Home / billing screen onStart. */
    public void autoConnect(Context context, String address) {
        try {
            if (context == null) {
                return;
            }
            ensureAppContext(context);
            if (context instanceof Activity) {
                hostActivity = (Activity) context;
            }
            String addr = normalize(address);
            if (addr.isEmpty()) {
                return;
            }
            savedAddress = addr;
            if (isReady() && addr.equalsIgnoreCase(getConnectedAddress())) {
                persistentSession = true;
                return;
            }
            if (!isBluetoothAdapterEnabled()) {
                return;
            }
            if (shouldDelegateToPeer(addr)) {
                peerChannel().autoConnect(appContext, addr);
                return;
            }
            connectInternal(addr, false);
        } catch (Exception e) {
            Log.e(TAG, channelName + ": autoConnect failed", e);
        }
    }

    /**
     * Connect to a printer address.
     *
     * @param allowDevicePicker when true (Connect button only): open the Bluetooth
     *                          device list if the address is empty or the saved MAC
     *                          is not in the paired list. Never opens the list on
     *                          connection failure / auto-reconnect.
     */
    public void connect(Context context, String address, Activity activity, boolean allowDevicePicker) {
        try {
            if (context == null) {
                return;
            }
            ensureAppContext(context);
            if (activity != null) {
                hostActivity = activity;
            } else if (context instanceof Activity) {
                hostActivity = (Activity) context;
            }
            userInitiatedSession = allowDevicePicker;

            Activity btActivity = hostActivity;
            if (allowDevicePicker && btActivity == null) {
                showToast(appContext, R.string.connect_fail);
                return;
            }

            if (allowDevicePicker && !checkBluetoothEnabled(btActivity, true)) {
                return;
            }
            if (!allowDevicePicker && !isBluetoothAdapterEnabled()) {
                return;
            }

            String addr = normalize(address);
            if (!addr.isEmpty()) {
                savedAddress = addr;
            } else {
                addr = normalize(savedAddress);
            }

            if (addr.isEmpty()) {
                if (allowDevicePicker && btActivity != null) {
                    openDevicePicker(btActivity);
                }
                return;
            }

            if (isReady() && addr.equalsIgnoreCase(getConnectedAddress())) {
                persistentSession = true;
                if (allowDevicePicker) {
                    showConnectedToast(btActivity != null ? btActivity : appContext, addr);
                }
                return;
            }

            if (allowDevicePicker) {
                // Paired saved printer: try silent connect first instead of forcing re-pick
                if (!addr.isEmpty() && isPairedAddress(addr)) {
                    if (shouldDelegateToPeer(addr)) {
                        peerChannel().connect(context, addr, activity, false);
                        return;
                    }
                    if (isConnecting()) {
                        return;
                    }
                    connectInternal(addr, true);
                    return;
                }
                if (btActivity != null) {
                    openDevicePicker(btActivity);
                }
                return;
            }

            if (isConnecting()) {
                return;
            }

            connectInternal(addr, false);
        } catch (Exception e) {
            Log.e(TAG, channelName + ": connect failed", e);
            showToast(appContext, R.string.connect_fail);
        }
    }

    public void openDevicePicker(Activity activity) {
        try {
            if (activity == null) {
                return;
            }
            hostActivity = activity;
            if (!checkBluetoothEnabled(activity, true)) {
                return;
            }
            activity.startActivityForResult(
                    new Intent(activity, DeviceListActivity.class),
                    channelName.equals("kot")
                            ? KOTWoosimPrnMng.REQUEST_CONNECT_DEVICE
                            : WoosimPrnMng.REQUEST_CONNECT_DEVICE);
        } catch (Exception e) {
            Log.e(TAG, channelName + ": openDevicePicker failed", e);
            showToast(activity, R.string.connect_fail);
        }
    }

    public void onDevicePicked(String address) {
        String addr = normalize(address);
        if (addr.isEmpty()) {
            return;
        }
        savedAddress = addr;
        userInitiatedSession = true;
        connectInternal(addr, true);
    }

    public boolean isReady() {
        BluetoothPrinterChannel owner = connectionOwner();
        return owner.isInternallyReady();
    }

    public boolean isConnecting() {
        BluetoothPrinterChannel owner = connectionOwner();
        return owner.isInternallyConnecting();
    }

    public String getConnectedAddress() {
        BluetoothPrinterChannel owner = connectionOwner();
        return owner.printService != null ? owner.printService.getConnectedDeviceAddress() : "";
    }

    /**
     * Blocks the calling thread until connected or timeout. Safe to call from a background executor.
     */
    public boolean waitUntilReady(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isReady()) {
                return true;
            }
            if (!isConnecting()) {
                return false;
            }
            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return isReady();
            }
        }
        return isReady();
    }

    public String getSavedAddress() {
        return savedAddress != null ? savedAddress : "";
    }

    /** True when the MAC is in the phone's bonded Bluetooth list. */
    public boolean isPairedAddress(String address) {
        String addr = normalize(address);
        if (addr.isEmpty()) {
            return false;
        }
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                return false;
            }
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            if (bonded == null || bonded.isEmpty()) {
                return false;
            }
            for (BluetoothDevice device : bonded) {
                if (device != null && addr.equalsIgnoreCase(device.getAddress())) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, channelName + ": isPairedAddress failed", e);
        }
        return false;
    }

    public BluetoothPrintService getPrintService() {
        return connectionOwner().printService;
    }

    public boolean write(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return false;
            }
            BluetoothPrinterChannel owner = connectionOwner();
            if (!owner.isInternallyReady() || owner.printService == null) {
                // Never sleep on the main thread — that causes focus-window ANRs.
                if (Looper.myLooper() != Looper.getMainLooper()
                        && !isReady() && isConnecting()) {
                    waitUntilReady(CONNECT_WAIT_MS);
                }
                owner = connectionOwner();
            }
            if (!owner.isInternallyReady() || owner.printService == null) {
                owner.scheduleReconnect();
                return false;
            }
            boolean ok = owner.printService.write(data);
            if (!ok) {
                owner.scheduleReconnect();
            }
            return ok;
        } catch (Exception e) {
            Log.e(TAG, channelName + ": write failed", e);
            connectionOwner().scheduleReconnect();
            return false;
        }
    }

    public void release(Context context) {
        try {
            cancelReconnect();
            userInitiatedSession = false;
            persistentSession = false;
            reconnectBackoffMs = RECONNECT_DELAY_MS;
            if (printService != null) {
                final BluetoothPrintService service = printService;
                printService = null;
                new Thread(() -> {
                    try {
                        service.stop();
                    } catch (Exception e) {
                        Log.e(TAG, channelName + ": release stop failed", e);
                    }
                }, channelName + "-release").start();
            }
            woosimService = null;
            printHandler = null;
            if (context != null) {
                showToast(context.getApplicationContext(), R.string.toast_printer_disconnect);
            }
        } catch (Exception e) {
            Log.e(TAG, channelName + ": release failed", e);
        }
    }

    public void disconnect(Context context) {
        savedAddress = "";
        userInitiatedSession = false;
        persistentSession = false;
        reconnectBackoffMs = RECONNECT_DELAY_MS;
        cancelReconnect();
        release(context);
    }

    public boolean ensureBluetoothOn(Activity activity, boolean fromUser) {
        return checkBluetoothEnabled(activity, fromUser);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void connectInternal(String address, boolean fromUser) {
        synchronized (GLOBAL_CONNECT_LOCK) {
            try {
                if (fromUser) {
                    userInitiatedSession = true;
                }
                savedAddress = address;

                if (shouldDelegateToPeer(address)) {
                    peerChannel().connectInternal(address, fromUser);
                    return;
                }

                if (isInternallyReady() && address.equalsIgnoreCase(getConnectedAddress())) {
                    persistentSession = true;
                    cancelReconnect();
                    reconnectBackoffMs = RECONNECT_DELAY_MS;
                    return;
                }
                if (isInternallyConnecting() && address.equalsIgnoreCase(savedAddress)) {
                    return;
                }

                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter == null) {
                    showToast(appContext, R.string.bluetooth_not_available);
                    return;
                }

                BluetoothDevice device;
                try {
                    device = adapter.getRemoteDevice(address);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, channelName + ": invalid address " + address, e);
                    if (fromUser) {
                        showToast(appContext, R.string.connect_fail);
                    }
                    return;
                }

                ensurePrintService();
                if (printService == null) {
                    if (fromUser) {
                        showToast(appContext, R.string.connect_fail);
                    }
                    scheduleReconnect();
                    return;
                }
                if (printService.getState() == BluetoothPrintService.STATE_NONE) {
                    printService.start();
                }

                printService.connect(device, false);
            } catch (Exception e) {
                Log.e(TAG, channelName + ": connectInternal failed", e);
                if (fromUser) {
                    showToast(appContext, R.string.connect_fail);
                }
                scheduleReconnect();
            }
        }
    }

    private void onServiceConnected(BluetoothDevice connectedDevice) {
        cancelReconnect();
        reconnectBackoffMs = RECONNECT_DELAY_MS;
        persistentSession = true;
        if (userInitiatedSession && appContext != null) {
            String name = connectedDevice != null ? connectedDevice.getName() : "";
            String deviceLabel = name != null && !name.isEmpty()
                    ? name
                    : normalize(savedAddress);
            showConnectedToast(appContext, deviceLabel);
        }
    }

    private void onServiceConnectionFailed() {
        if (userInitiatedSession) {
            showToast(appContext, R.string.connect_fail);
        }
        scheduleReconnect();
    }

    private void onServiceConnectionLost() {
        if (userInitiatedSession || persistentSession) {
            showToast(appContext, R.string.connect_lost);
        }
        scheduleReconnect();
    }

    private void ensurePrintService() {
        if (printService != null && printHandler != null) {
            return;
        }
        if (appContext == null && hostActivity != null) {
            appContext = hostActivity.getApplicationContext();
        }
        if (appContext == null) {
            return;
        }

        printHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                try {
                    switch (msg.what) {
                        case BluetoothPrintCallbacks.MESSAGE_READ:
                            if (woosimService != null && msg.obj instanceof byte[]) {
                                woosimService.processRcvData((byte[]) msg.obj, msg.arg1);
                            }
                            break;
                        case WoosimService.MESSAGE_PRINTER:
                            showToast(appContext, R.string.toast_msr_message);
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, channelName + ": handler error", e);
                }
            }
        };

        if (woosimService == null) {
            woosimService = new WoosimService(printHandler);
        }
        if (printService == null) {
            printService = new BluetoothPrintService(appContext, printHandler);
            printService.setConnectionListener(new BluetoothPrintService.ConnectionListener() {
                @Override
                public void onConnected(BluetoothDevice connectedDevice) {
                    BluetoothPrinterChannel.this.onServiceConnected(connectedDevice);
                }

                @Override
                public void onConnectionFailed() {
                    BluetoothPrinterChannel.this.onServiceConnectionFailed();
                }

                @Override
                public void onConnectionLost() {
                    BluetoothPrinterChannel.this.onServiceConnectionLost();
                }
            });
        }
    }

    private void ensureAppContext(Context context) {
        if (context == null) {
            return;
        }
        appContext = context.getApplicationContext();
    }

    private void shutdownInternal(Context app) {
        savedAddress = "";
        userInitiatedSession = false;
        persistentSession = false;
        cancelReconnect();
        reconnectBackoffMs = RECONNECT_DELAY_MS;
        if (printService != null) {
            try {
                printService.stop();
            } catch (Exception e) {
                Log.e(TAG, channelName + ": shutdown stop failed", e);
            }
            printService = null;
        }
        woosimService = null;
        printHandler = null;
    }

    private boolean isBluetoothAdapterEnabled() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            return adapter != null && adapter.isEnabled();
        } catch (Exception e) {
            Log.e(TAG, channelName + ": isBluetoothAdapterEnabled failed", e);
            return false;
        }
    }

    private void scheduleReconnect() {
        if (savedAddress == null || savedAddress.trim().isEmpty()) {
            return;
        }
        BluetoothPrinterChannel owner = connectionOwner();
        if (owner != this) {
            owner.scheduleReconnect();
            return;
        }
        if (reconnectScheduled || isInternallyReady() || isInternallyConnecting()) {
            return;
        }
        reconnectScheduled = true;
        mainHandler.removeCallbacks(reconnectRunnable);
        mainHandler.postDelayed(reconnectRunnable, reconnectBackoffMs);
        reconnectBackoffMs = Math.min(reconnectBackoffMs * 2, RECONNECT_MAX_DELAY_MS);
    }

    private BluetoothPrinterChannel peerChannel() {
        return this == BILL ? KOT : BILL;
    }

    /** When bill and KOT share one physical printer, use a single RFCOMM socket (bill is primary). */
    private BluetoothPrinterChannel connectionOwner() {
        if (!usesSamePrinterAsPeer()) {
            return this;
        }
        if (this == KOT && (BILL.isInternallyReady() || BILL.isInternallyConnecting())) {
            return BILL;
        }
        if (this == BILL && !isInternallyReady() && !isInternallyConnecting()
                && KOT.isInternallyReady()) {
            return KOT;
        }
        return this;
    }

    private boolean usesSamePrinterAsPeer() {
        String peerAddr = peerChannel().getSavedAddress();
        return savedAddress != null && !savedAddress.isEmpty()
                && savedAddress.equalsIgnoreCase(peerAddr);
    }

    private boolean shouldDelegateToPeer(String address) {
        return this == KOT
                && address.equalsIgnoreCase(BILL.getSavedAddress())
                && !address.isEmpty();
    }

    private boolean isInternallyReady() {
        return printService != null
                && printService.getState() == BluetoothPrintService.STATE_CONNECTED;
    }

    private boolean isInternallyConnecting() {
        return printService != null
                && printService.getState() == BluetoothPrintService.STATE_CONNECTING;
    }

    private void cancelReconnect() {
        reconnectScheduled = false;
        mainHandler.removeCallbacks(reconnectRunnable);
    }

    private static String normalize(String address) {
        return address != null ? address.trim() : "";
    }

    private boolean checkBluetoothEnabled(Activity activity, boolean fromUser) {
        try {
            if (activity == null) {
                return false;
            }
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                showToast(activity, R.string.bluetooth_not_available);
                return false;
            }
            if (adapter.isEnabled()) {
                return true;
            }
            if (fromUser) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED
                        || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                    int enableReq = "kot".equals(channelName)
                            ? KOTWoosimPrnMng.REQUEST_ENABLE_BT
                            : WoosimPrnMng.REQUEST_ENABLE_BT;
                    activity.startActivityForResult(enableIntent, enableReq);
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "ensureBluetoothOn failed", e);
            return false;
        }
    }

    private static void showConnectedToast(Context context, String deviceLabel) {
        if (context == null) {
            return;
        }
        String message = context.getString(R.string.toast_printer_connected);
        if (deviceLabel != null && !deviceLabel.trim().isEmpty()) {
            message = message + ": " + deviceLabel.trim();
        }
        showToast(context, message);
    }

    private static void showToast(Context context, int resId) {
        if (context == null) {
            return;
        }
        try {
            showToast(context, context.getApplicationContext().getString(resId));
        } catch (Exception ignored) {
        }
    }

    private static void showToast(Context context, String message) {
        if (context == null || message == null || message.trim().isEmpty()) {
            return;
        }
        Context app = context.getApplicationContext();
        Runnable show = () -> {
            try {
                Toast.makeText(app, message, Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            show.run();
        } else {
            // Connection callbacks may arrive off the main thread.
            new Handler(Looper.getMainLooper()).post(show);
        }
    }
}
