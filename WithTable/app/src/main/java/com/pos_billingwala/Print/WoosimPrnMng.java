package com.pos_billingwala.Print;

import android.app.Activity;
import android.content.Context;

/**
 * Thin facade for the bill printer — delegates to {@link BluetoothPrinterChannel#bill()}.
 */
public final class WoosimPrnMng {

    public static final int REQUEST_ENABLE_BT = 4;
    public static final int REQUEST_CONNECT_DEVICE = 6;

    public static final int MESSAGE_DEVICE_NAME = BluetoothPrintCallbacks.MESSAGE_DEVICE_NAME;
    public static final int MESSAGE_TOAST = BluetoothPrintCallbacks.MESSAGE_TOAST;
    public static final int MESSAGE_READ = BluetoothPrintCallbacks.MESSAGE_READ;
    public static final String DEVICE_NAME = BluetoothPrintCallbacks.DEVICE_NAME;
    public static final String TOAST = BluetoothPrintCallbacks.TOAST;

    private WoosimPrnMng() {
    }

    /** Quiet reconnect (settings load / print screens) — never opens device list. */
    public static void connect(Context context, String deviceAddr, Activity host) {
        BluetoothPrinterChannel.bill().connect(context, deviceAddr, host, false);
    }

    /**
     * Connect button: reconnect to saved MAC, or open Bluetooth device list once
     * if the address is empty / printer is not paired.
     */
    public static void connectFromButton(Context context, String deviceAddr, Activity host) {
        String addr = deviceAddr != null ? deviceAddr.trim() : "";
        if (addr.isEmpty()) {
            BluetoothPrinterChannel.bill().openDevicePicker(host);
        } else {
            BluetoothPrinterChannel.bill().connect(context, addr, host, true);
        }
    }

    /** Legacy entry point used across the app. */
    public WoosimPrnMng(Context context, String deviceAddr, Context host) {
        connect(context, deviceAddr, host instanceof Activity ? (Activity) host : null);
    }

    public static void pairPrinter(Context context, Activity activity) {
        BluetoothPrinterChannel.bill().openDevicePicker(activity);
    }

    public static boolean isBTopen(Context context, Activity activity) {
        return BluetoothPrinterChannel.bill().ensureBluetoothOn(activity, true);
    }

    public static BluetoothPrintService getServiceInstance() {
        return BluetoothPrinterChannel.bill().getPrintService();
    }

    public static boolean isServiceConnected() {
        return BluetoothPrinterChannel.bill().isReady();
    }

    public static boolean isPrinterConnected(Context context, Activity activity) {
        return isServiceConnected();
    }

    public static void sendAutoCutter() {
        try {
            BluetoothPrinterChannel.bill().write(EscPosCommands.CUT_FULL);
        } catch (Exception ignored) {
        }
    }

    public static void releaseAllocations(Context context) {
        BluetoothPrinterChannel.bill().release(context);
    }

    /** @deprecated use {@link #isServiceConnected()} */
    public boolean printSucc() {
        return isServiceConnected();
    }
}
