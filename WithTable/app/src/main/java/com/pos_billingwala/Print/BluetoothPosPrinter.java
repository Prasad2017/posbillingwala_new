package com.pos_billingwala.Print;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

/**
 * Adapts the existing {@link BluetoothPrinterChannel} to {@link PosPrinter}.
 * Does not replace Bluetooth connection logic — only wraps it.
 */
public final class BluetoothPosPrinter implements PosPrinter {

    private static final String TAG = "BluetoothPosPrinter";

    private final Context appContext;
    private final Activity hostActivity;
    private final BluetoothPrinterChannel channel;
    private final PrinterProfile profile;
    private final boolean isBill;

    public BluetoothPosPrinter(Context context, Activity activity, PrinterProfile profile, boolean isBill) {
        this.appContext = context != null ? context.getApplicationContext() : null;
        this.hostActivity = activity;
        this.profile = profile;
        this.isBill = isBill;
        this.channel = isBill ? BluetoothPrinterChannel.bill() : BluetoothPrinterChannel.kot();
    }

    @Override
    public boolean connect() {
        try {
            String mac = profile != null ? profile.bluetoothMac : "";
            if (mac == null || mac.trim().isEmpty()) {
                return false;
            }
            Context ctx = hostActivity != null ? hostActivity : appContext;
            if (ctx == null) {
                return false;
            }
            channel.connect(ctx, mac, hostActivity, false);
            // Give RFCOMM a short window; caller may already be connected.
            long deadline = System.currentTimeMillis() + 8000L;
            while (System.currentTimeMillis() < deadline) {
                if (channel.isReady()) {
                    return true;
                }
                if (!channel.isConnecting() && !channel.isReady()) {
                    // Still connecting asynchronously — wait a bit more
                    Thread.sleep(200);
                    if (channel.isReady()) {
                        return true;
                    }
                    // One more connect attempt if idle and not ready
                    if (!channel.isConnecting()) {
                        channel.connect(ctx, mac, hostActivity, false);
                    }
                }
                Thread.sleep(150);
            }
            return channel.isReady();
        } catch (Exception e) {
            Log.e(TAG, "connect failed", e);
            return false;
        }
    }

    @Override
    public boolean print(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return false;
            }
            if (!isConnected() && !connect()) {
                return false;
            }
            return channel.write(data);
        } catch (Exception e) {
            Log.e(TAG, "print failed", e);
            return false;
        }
    }

    @Override
    public boolean cut() {
        try {
            if (profile == null || !profile.shouldCut()) {
                return false;
            }
            return print(profile.cutBytes());
        } catch (Exception e) {
            Log.e(TAG, "cut failed", e);
            return false;
        }
    }

    @Override
    public boolean openCashDrawer() {
        try {
            if (profile == null || !profile.supportsCashDrawer) {
                return false;
            }
            return print(profile.drawerBytes());
        } catch (Exception e) {
            Log.e(TAG, "openCashDrawer failed", e);
            return false;
        }
    }

    @Override
    public void disconnect() {
        try {
            channel.release(appContext);
        } catch (Exception e) {
            Log.e(TAG, "disconnect failed", e);
        }
    }

    @Override
    public boolean isConnected() {
        return channel.isReady();
    }

    @Override
    public boolean supportsCutter() {
        return profile != null && profile.supportsCutter;
    }

    @Override
    public boolean supportsCashDrawer() {
        return profile != null && profile.supportsCashDrawer;
    }

    @Override
    public PrinterConnectionType getConnectionType() {
        return PrinterConnectionType.BLUETOOTH;
    }

    @Override
    public String getPrinterIdentity() {
        return profile != null ? profile.identityKey() : (isBill ? "bt:bill" : "bt:kot");
    }

    @Override
    public String getDisplayName() {
        String mac = profile != null ? profile.bluetoothMac : "";
        return mac != null && !mac.isEmpty() ? mac : "Bluetooth Printer";
    }

    public PrinterProfile getProfile() {
        return profile;
    }
}
