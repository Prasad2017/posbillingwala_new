package com.pos_billingwala.Print;

import android.app.Activity;
import android.content.Context;

/**
 * Thin facade for the KOT printer — delegates to {@link BluetoothPrinterChannel#kot()}.
 */
public final class KOTWoosimPrnMng {

    /** Must match activities that handle KOT BT enable / device pick results. */
    public static final int REQUEST_ENABLE_BT = 8;
    public static final int REQUEST_CONNECT_DEVICE = 10;

    public static final int MESSAGE_DEVICE_NAME = BluetoothPrintCallbacks.MESSAGE_DEVICE_NAME;
    public static final int MESSAGE_TOAST = BluetoothPrintCallbacks.MESSAGE_TOAST;
    public static final int MESSAGE_READ = BluetoothPrintCallbacks.MESSAGE_READ;
    public static final String DEVICE_NAME = BluetoothPrintCallbacks.DEVICE_NAME;
    public static final String TOAST = BluetoothPrintCallbacks.TOAST;

    private KOTWoosimPrnMng() {
    }

    /** Quiet reconnect — never opens device list. */
    public static void connect(Context context, String deviceAddr, Activity host) {
        BluetoothPrinterChannel.kot().connect(context, deviceAddr, host, false);
    }

    /**
     * Connect button: reconnect to saved MAC, or open Bluetooth device list once
     * if the address is empty / printer is not paired.
     */
    public static void connectFromButton(Context context, String deviceAddr, Activity host) {
        String addr = deviceAddr != null ? deviceAddr.trim() : "";
        if (addr.isEmpty()) {
            BluetoothPrinterChannel.kot().openDevicePicker(host);
        } else {
            BluetoothPrinterChannel.kot().connect(context, addr, host, true);
        }
    }

    public KOTWoosimPrnMng(Context context, String deviceAddr, Context host) {
        connect(context, deviceAddr, host instanceof Activity ? (Activity) host : null);
    }

    public static void pairPrinter(Context context, Activity activity) {
        BluetoothPrinterChannel.kot().openDevicePicker(activity);
    }

    public static boolean isBTopen(Context context, Activity activity) {
        return BluetoothPrinterChannel.kot().ensureBluetoothOn(activity, true);
    }

    public static BluetoothPrintService getServiceInstance() {
        return BluetoothPrinterChannel.kot().getPrintService();
    }

    public static boolean isServiceConnected() {
        return BluetoothPrinterChannel.kot().isReady();
    }

    public static boolean isPrinterConnected(Context context, Activity activity) {
        return isServiceConnected();
    }

    public static void sendAutoCutter() {
        try {
            byte[] cut = new byte[]{0x1B, 0x69};
            BluetoothPrinterChannel.kot().write(cut);
        } catch (Exception ignored) {
        }
    }

    public static void releaseAllocations(Context context) {
        BluetoothPrinterChannel.kot().release(context);
    }

    public boolean printSucc() {
        return isServiceConnected();
    }
}
