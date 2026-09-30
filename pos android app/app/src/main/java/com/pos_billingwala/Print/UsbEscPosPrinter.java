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
import java.util.Locale;

/**
 * Basic ESC/POS USB bulk-out writer. Device id format: {@code vendorId:productId} (decimal).
 */
public final class UsbEscPosPrinter {

    private static final String TAG = "UsbEscPos";
    public static final String ACTION_USB_PERMISSION = "com.pos_billingwala.USB_PERMISSION";

    private UsbEscPosPrinter() {
    }

    public static String deviceId(UsbDevice device) {
        if (device == null) {
            return "";
        }
        return device.getVendorId() + ":" + device.getProductId();
    }

    public static String deviceDisplayName(UsbDevice device) {
        if (device == null) {
            return "";
        }
        String name = device.getProductName();
        if (name == null || name.trim().isEmpty()) {
            name = device.getDeviceName();
        }
        return name != null ? name : deviceId(device);
    }

    public static List<UsbDevice> listPrinters(Context context) {
        List<UsbDevice> out = new ArrayList<>();
        UsbManager manager = usbManager(context);
        if (manager == null) {
            return out;
        }
        HashMap<String, UsbDevice> map = manager.getDeviceList();
        if (map == null) {
            return out;
        }
        out.addAll(map.values());
        return out;
    }

    public static UsbDevice findById(Context context, String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        String want = id.trim().toLowerCase(Locale.ROOT);
        for (UsbDevice device : listPrinters(context)) {
            if (deviceId(device).equalsIgnoreCase(want)) {
                return device;
            }
        }
        return null;
    }

    public static boolean hasPermission(Context context, UsbDevice device) {
        UsbManager manager = usbManager(context);
        return manager != null && device != null && manager.hasPermission(device);
    }

    public static void requestPermission(Context context, UsbDevice device, BroadcastReceiver receiver) {
        UsbManager manager = usbManager(context);
        if (manager == null || device == null) {
            return;
        }
        if (manager.hasPermission(device)) {
            return;
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), flags);
        if (receiver != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(
                            receiver,
                            new IntentFilter(ACTION_USB_PERMISSION),
                            Context.RECEIVER_NOT_EXPORTED);
                } else {
                    context.registerReceiver(receiver, new IntentFilter(ACTION_USB_PERMISSION));
                }
            } catch (Exception ignored) {
            }
        }
        manager.requestPermission(device, pi);
    }

    public static boolean write(Context context, boolean isKot, byte[] data) {
        String id = PrinterEndpointPrefs.usbIdFor(context, isKot);
        return writeTo(context, id, data);
    }

    public static boolean writeTo(Context context, String deviceId, byte[] data) {
        if (data == null || data.length == 0) {
            return false;
        }
        UsbDevice device = findById(context, deviceId);
        if (device == null) {
            Log.w(TAG, "USB device not found: " + deviceId);
            return false;
        }
        UsbManager manager = usbManager(context);
        if (manager == null || !manager.hasPermission(device)) {
            Log.w(TAG, "USB permission missing for " + deviceId);
            return false;
        }
        UsbInterface iface = findPrinterInterface(device);
        UsbEndpoint endpoint = iface != null ? findBulkOut(iface) : null;
        if (iface == null || endpoint == null) {
            Log.w(TAG, "No bulk OUT endpoint on " + deviceId);
            return false;
        }
        UsbDeviceConnection connection = manager.openDevice(device);
        if (connection == null) {
            return false;
        }
        try {
            if (!connection.claimInterface(iface, true)) {
                return false;
            }
            int offset = 0;
            while (offset < data.length) {
                int chunk = Math.min(endpoint.getMaxPacketSize() > 0
                        ? endpoint.getMaxPacketSize() * 16
                        : 16384, data.length - offset);
                int written = connection.bulkTransfer(endpoint, data, offset, chunk, 5000);
                if (written < 0) {
                    return false;
                }
                offset += written;
            }
            return true;
        } catch (Exception e) {
            Log.w(TAG, "USB write failed", e);
            return false;
        } finally {
            try {
                connection.releaseInterface(iface);
            } catch (Exception ignored) {
            }
            try {
                connection.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static UsbInterface findPrinterInterface(UsbDevice device) {
        UsbInterface printer = null;
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface iface = device.getInterface(i);
            if (iface == null) {
                continue;
            }
            if (iface.getInterfaceClass() == UsbConstants.USB_CLASS_PRINTER) {
                return iface;
            }
            if (findBulkOut(iface) != null && printer == null) {
                printer = iface;
            }
        }
        return printer;
    }

    private static UsbEndpoint findBulkOut(UsbInterface iface) {
        for (int i = 0; i < iface.getEndpointCount(); i++) {
            UsbEndpoint ep = iface.getEndpoint(i);
            if (ep != null
                    && ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                    && ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                return ep;
            }
        }
        return null;
    }

    private static UsbManager usbManager(Context context) {
        if (context == null) {
            return null;
        }
        return (UsbManager) context.getApplicationContext().getSystemService(Context.USB_SERVICE);
    }
}
