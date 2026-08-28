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

import com.pos_billingwala.R;
import com.woosim.printer.WoosimService;

/**
 * One persistent Bluetooth printer session (bill or KOT).
 * <ul>
 *   <li>Stays connected to the same paired address — does not reconnect if already connected.</li>
 *   <li>Auto-reconnects silently when the link drops but the saved address is still set.</li>
 *   <li>Never throws — failures are surfaced as toasts only when appropriate.</li>
 * </ul>
 */
@SuppressLint("MissingPermission")
public final class BluetoothPrinterChannel {

    private static final String TAG = "BtPrinterChannel";
    private static final long RECONNECT_DELAY_MS = 2500L;

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
    private boolean reconnectScheduled;

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

    /** Silent background connect — used from Home / billing screen onStart. */
    public void autoConnect(Context context, String address) {
        try {
            if (context == null) {
                return;
            }
            appContext = context.getApplicationContext();
            if (!(context instanceof Activity)) {
                return;
            }
            hostActivity = (Activity) context;
            String addr = normalize(address);
            if (addr.isEmpty()) {
                return;
            }
            savedAddress = addr;
            userInitiatedSession = false;
            if (isReady() && addr.equalsIgnoreCase(getConnectedAddress())) {
                return;
            }
            if (!checkBluetoothEnabled(hostActivity, false)) {
                return;
            }
            connectInternal(addr, false);
        } catch (Exception e) {
            Log.e(TAG, channelName + ": autoConnect failed", e);
        }
    }

    /**
     * User tapped Print or opened device picker.
     *
     * @param openPickerWhenEmpty show {@link DeviceListActivity} when no saved address
     */
    public void connect(Context context, String address, Activity activity, boolean openPickerWhenEmpty) {
        try {
            if (context == null) {
                return;
            }
            appContext = context.getApplicationContext();
            hostActivity = activity != null ? activity : (context instanceof Activity ? (Activity) context : hostActivity);
            userInitiatedSession = true;

            String addr = normalize(address);
            if (!addr.isEmpty()) {
                savedAddress = addr;
            }

            if (isReady() && !savedAddress.isEmpty()
                    && savedAddress.equalsIgnoreCase(getConnectedAddress())) {
                return;
            }

            Activity btActivity = hostActivity;
            if (btActivity == null) {
                showToast(appContext, R.string.connect_fail);
                return;
            }

            if (!checkBluetoothEnabled(btActivity, true)) {
                return;
            }

            if (savedAddress.isEmpty()) {
                if (openPickerWhenEmpty) {
                    showToast(btActivity, R.string.toast_printer_not_connected_select);
                    openDevicePicker(btActivity);
                }
                return;
            }

            if (isConnecting()) {
                showToast(btActivity, R.string.toast_printer_connecting);
                return;
            }

            showToast(btActivity, R.string.toast_printer_connecting);
            connectInternal(savedAddress, true);
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
        return printService != null
                && printService.getState() == BluetoothPrintService.STATE_CONNECTED;
    }

    public boolean isConnecting() {
        return printService != null
                && printService.getState() == BluetoothPrintService.STATE_CONNECTING;
    }

    public String getConnectedAddress() {
        return printService != null ? printService.getConnectedDeviceAddress() : "";
    }

    public String getSavedAddress() {
        return savedAddress != null ? savedAddress : "";
    }

    public BluetoothPrintService getPrintService() {
        return printService;
    }

    public boolean write(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return false;
            }
            if (!isReady() || printService == null) {
                scheduleReconnect();
                return false;
            }
            boolean ok = printService.write(data);
            if (!ok) {
                scheduleReconnect();
            }
            return ok;
        } catch (Exception e) {
            Log.e(TAG, channelName + ": write failed", e);
            scheduleReconnect();
            return false;
        }
    }

    public void release(Context context) {
        try {
            cancelReconnect();
            userInitiatedSession = false;
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

    public boolean ensureBluetoothOn(Activity activity, boolean fromUser) {
        return checkBluetoothEnabled(activity, fromUser);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void connectInternal(String address, boolean fromUser) {
        try {
            if (fromUser) {
                userInitiatedSession = true;
            }
            savedAddress = address;

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
                showToast(appContext, R.string.connect_fail);
                return;
            }

            ensurePrintService();
            if (printService.getState() == BluetoothPrintService.STATE_NONE) {
                printService.start();
            }

            printService.setConnectionListener(new BluetoothPrintService.ConnectionListener() {
                @Override
                public void onConnected(BluetoothDevice connectedDevice) {
                    cancelReconnect();
                    if (userInitiatedSession && appContext != null) {
                        String name = connectedDevice != null ? connectedDevice.getName() : "";
                        String deviceLabel = name != null && !name.isEmpty() ? name : address;
                        showToast(appContext, appContext.getString(R.string.connected) + " " + deviceLabel);
                    }
                }

                @Override
                public void onConnectionFailed() {
                    if (userInitiatedSession) {
                        showToast(appContext, R.string.connect_fail);
                    }
                    scheduleReconnect();
                }

                @Override
                public void onConnectionLost() {
                    if (userInitiatedSession) {
                        showToast(appContext, R.string.connect_lost);
                    }
                    scheduleReconnect();
                }
            });

            printService.connect(device, false);
        } catch (Exception e) {
            Log.e(TAG, channelName + ": connectInternal failed", e);
            if (fromUser) {
                showToast(appContext, R.string.connect_fail);
            }
            scheduleReconnect();
        }
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
        }
    }

    private void scheduleReconnect() {
        if (savedAddress == null || savedAddress.trim().isEmpty()) {
            return;
        }
        if (reconnectScheduled || isReady() || isConnecting()) {
            return;
        }
        reconnectScheduled = true;
        mainHandler.removeCallbacks(reconnectRunnable);
        mainHandler.postDelayed(reconnectRunnable, RECONNECT_DELAY_MS);
    }

    private void cancelReconnect() {
        reconnectScheduled = false;
        mainHandler.removeCallbacks(reconnectRunnable);
    }

    private static String normalize(String address) {
        return address != null ? address.trim() : "";
    }

    private static boolean checkBluetoothEnabled(Activity activity, boolean fromUser) {
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
                        == PackageManager.PERMISSION_GRANTED) {
                    activity.startActivityForResult(enableIntent, WoosimPrnMng.REQUEST_ENABLE_BT);
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "ensureBluetoothOn failed", e);
            return false;
        }
    }

    private static void showToast(Context context, int resId) {
        if (context == null) {
            return;
        }
        try {
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    private static void showToast(Context context, String message) {
        if (context == null || message == null || message.trim().isEmpty()) {
            return;
        }
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }
}
