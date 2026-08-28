package com.pos_billingwala.Print;

/**
 * Shared message codes between {@link BluetoothPrintService} and printer channels.
 */
public final class BluetoothPrintCallbacks {

    public static final int MESSAGE_DEVICE_NAME = 1;
    public static final int MESSAGE_TOAST = 2;
    public static final int MESSAGE_READ = 3;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private BluetoothPrintCallbacks() {
    }
}
