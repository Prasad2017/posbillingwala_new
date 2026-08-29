package com.pos_billingwala.Print;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * USB Host thermal printer adapter. Sends the same ESC/POS byte[] as Bluetooth.
 */
public final class UsbPosPrinter implements PosPrinter {

    private static final String TAG = "UsbPosPrinter";
    private static final String ACTION_USB_PERMISSION = "com.pos_billingwala.USB_PRINTER_PERMISSION";
    private static final int TRANSFER_TIMEOUT_MS = 5000;

    private final Context appContext;
    private final PrinterProfile profile;
    private final UsbManager usbManager;

    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbInterface usbInterface;
    private UsbEndpoint bulkOut;
    private final Object ioLock = new Object();

    public UsbPosPrinter(Context context, PrinterProfile profile) {
        this.appContext = context != null ? context.getApplicationContext() : null;
        this.profile = profile;
        this.usbManager = appContext != null
                ? (UsbManager) appContext.getSystemService(Context.USB_SERVICE)
                : null;
    }

    @Override
    public boolean connect() {
        synchronized (ioLock) {
            try {
                if (isConnected()) {
                    return true;
                }
                if (usbManager == null || appContext == null) {
                    return false;
                }
                device = findPrinterDevice();
                if (device == null) {
                    Log.w(TAG, "No USB printer device found");
                    return false;
                }
                if (!usbManager.hasPermission(device)) {
                    if (!requestPermissionAndWait(device)) {
                        Log.w(TAG, "USB permission denied");
                        return false;
                    }
                }
                return openDevice(device);
            } catch (Exception e) {
                Log.e(TAG, "connect failed", e);
                closeQuietly();
                return false;
            }
        }
    }

    @Override
    public boolean print(byte[] data) {
        synchronized (ioLock) {
            try {
                if (data == null || data.length == 0) {
                    return false;
                }
                if (!isConnected() && !connect()) {
                    return false;
                }
                if (connection == null || bulkOut == null) {
                    return false;
                }
                int offset = 0;
                while (offset < data.length) {
                    int chunk = Math.min(bulkOut.getMaxPacketSize() > 0
                            ? bulkOut.getMaxPacketSize() * 16
                            : 16384, data.length - offset);
                    int written = connection.bulkTransfer(bulkOut, data, offset, chunk, TRANSFER_TIMEOUT_MS);
                    if (written < 0) {
                        Log.e(TAG, "USB bulkTransfer failed at offset " + offset);
                        return false;
                    }
                    offset += written;
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "print failed", e);
                return false;
            }
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
        synchronized (ioLock) {
            closeQuietly();
        }
    }

    @Override
    public boolean isConnected() {
        return connection != null && bulkOut != null && device != null;
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
        return PrinterConnectionType.USB;
    }

    @Override
    public String getPrinterIdentity() {
        return profile != null ? profile.identityKey() : "usb:default";
    }

    @Override
    public String getDisplayName() {
        if (device != null) {
            String name = device.getProductName();
            if (name != null && !name.trim().isEmpty()) {
                return name.trim();
            }
            return "USB " + device.getVendorId() + ":" + device.getProductId();
        }
        return "USB Printer";
    }

    public PrinterProfile getProfile() {
        return profile;
    }

    public static List<UsbDevice> listLikelyPrinters(Context context) {
        List<UsbDevice> result = new ArrayList<>();
        if (context == null) {
            return result;
        }
        UsbManager manager = (UsbManager) context.getApplicationContext().getSystemService(Context.USB_SERVICE);
        if (manager == null) {
            return result;
        }
        HashMap<String, UsbDevice> map = manager.getDeviceList();
        if (map == null) {
            return result;
        }
        for (UsbDevice d : map.values()) {
            if (looksLikePrinter(d)) {
                result.add(d);
            }
        }
        return result;
    }

    public static String deviceKey(UsbDevice device) {
        if (device == null) {
            return "";
        }
        return device.getVendorId() + ":" + device.getProductId() + ":" + device.getDeviceName();
    }

    private UsbDevice findPrinterDevice() {
        HashMap<String, UsbDevice> map = usbManager.getDeviceList();
        if (map == null || map.isEmpty()) {
            return null;
        }
        String preferred = profile != null ? profile.usbDeviceKey : "";
        UsbDevice first = null;
        for (UsbDevice d : map.values()) {
            if (!looksLikePrinter(d)) {
                continue;
            }
            if (first == null) {
                first = d;
            }
            if (preferred != null && !preferred.isEmpty()) {
                String key = deviceKey(d);
                if (preferred.equals(key) || preferred.equals(d.getDeviceName())
                        || preferred.equals(d.getVendorId() + ":" + d.getProductId())) {
                    return d;
                }
            }
        }
        return first;
    }

    private static boolean looksLikePrinter(UsbDevice device) {
        if (device == null) {
            return false;
        }
        // Skip obvious non-printers
        if (device.getDeviceClass() == UsbConstants.USB_CLASS_MASS_STORAGE
                || device.getDeviceClass() == UsbConstants.USB_CLASS_HUB
                || device.getDeviceClass() == UsbConstants.USB_CLASS_HID) {
            return false;
        }
        // USB printer class
        if (device.getDeviceClass() == UsbConstants.USB_CLASS_PRINTER) {
            return true;
        }
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface iface = device.getInterface(i);
            if (iface == null) {
                continue;
            }
            int ifaceClass = iface.getInterfaceClass();
            if (ifaceClass == UsbConstants.USB_CLASS_MASS_STORAGE
                    || ifaceClass == UsbConstants.USB_CLASS_HID) {
                continue;
            }
            if (ifaceClass == UsbConstants.USB_CLASS_PRINTER) {
                return true;
            }
            // Vendor-specific ESC/POS printers commonly use class 0xFF + bulk OUT
            if (ifaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC && hasBulkOut(iface)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBulkOut(UsbInterface iface) {
        for (int e = 0; e < iface.getEndpointCount(); e++) {
            UsbEndpoint ep = iface.getEndpoint(e);
            if (ep != null
                    && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                    && ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                return true;
            }
        }
        return false;
    }

    private boolean openDevice(UsbDevice usbDevice) {
        closeQuietly();
        UsbInterface iface = null;
        UsbEndpoint out = null;
        for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
            UsbInterface candidate = usbDevice.getInterface(i);
            if (candidate == null) {
                continue;
            }
            UsbEndpoint candidateOut = findBulkOut(candidate);
            if (candidateOut != null) {
                iface = candidate;
                out = candidateOut;
                if (candidate.getInterfaceClass() == UsbConstants.USB_CLASS_PRINTER) {
                    break;
                }
            }
        }
        if (iface == null || out == null) {
            Log.e(TAG, "USB bulk OUT endpoint unavailable");
            return false;
        }
        UsbDeviceConnection conn = usbManager.openDevice(usbDevice);
        if (conn == null) {
            Log.e(TAG, "USB device unavailable");
            return false;
        }
        if (!conn.claimInterface(iface, true)) {
            conn.close();
            Log.e(TAG, "Failed to claim USB interface");
            return false;
        }
        this.device = usbDevice;
        this.connection = conn;
        this.usbInterface = iface;
        this.bulkOut = out;
        return true;
    }

    private static UsbEndpoint findBulkOut(UsbInterface iface) {
        for (int e = 0; e < iface.getEndpointCount(); e++) {
            UsbEndpoint ep = iface.getEndpoint(e);
            if (ep != null
                    && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                    && ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                return ep;
            }
        }
        return null;
    }

    private boolean requestPermissionAndWait(UsbDevice usbDevice) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean granted = new AtomicBoolean(false);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                        UsbDevice d = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        boolean ok = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                        if (ok && d != null && d.equals(usbDevice)) {
                            granted.set(true);
                        }
                        latch.countDown();
                    }
                } catch (Exception e) {
                    latch.countDown();
                }
            }
        };
        try {
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                appContext.registerReceiver(receiver, filter);
            }
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getBroadcast(appContext, 0,
                    new Intent(ACTION_USB_PERMISSION), flags);
            usbManager.requestPermission(usbDevice, pi);
            latch.await(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "USB permission request failed", e);
        } finally {
            try {
                appContext.unregisterReceiver(receiver);
            } catch (Exception ignored) {
            }
        }
        return granted.get() || usbManager.hasPermission(usbDevice);
    }

    private void closeQuietly() {
        try {
            if (connection != null && usbInterface != null) {
                try {
                    connection.releaseInterface(usbInterface);
                } catch (Exception ignored) {
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                }
            }
        } finally {
            connection = null;
            usbInterface = null;
            bulkOut = null;
            device = null;
        }
    }
}
