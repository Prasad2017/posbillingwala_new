package com.pos_billingwala.Print;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Bill / KOT endpoint prefs aligned with Flutter PrinterSettings transports.
 * Bluetooth MAC stays in company_printer_setting DB; USB / network / share live here.
 */
public final class PrinterEndpointPrefs {

    public enum Transport {
        BLUETOOTH,
        USB,
        NETWORK
    }

    private static final String PREFS = "printer_endpoint_prefs";
    private static final String KEY_BILL_TRANSPORT = "bill_transport";
    private static final String KEY_KOT_TRANSPORT = "kot_transport";
    private static final String KEY_NETWORK_HOST = "network_host";
    private static final String KEY_NETWORK_PORT = "network_port";
    private static final String KEY_BILL_USB_ID = "bill_usb_id";
    private static final String KEY_BILL_USB_NAME = "bill_usb_name";
    private static final String KEY_KOT_USB_ID = "kot_usb_id";
    private static final String KEY_KOT_USB_NAME = "kot_usb_name";
    private static final String KEY_AUTO_SHARE = "auto_share_on_save";

    private PrinterEndpointPrefs() {
    }

    public static Transport billTransport(Context context) {
        return fromStorage(prefs(context).getString(KEY_BILL_TRANSPORT, "bluetooth"));
    }

    public static Transport kotTransport(Context context) {
        return fromStorage(prefs(context).getString(KEY_KOT_TRANSPORT, "bluetooth"));
    }

    public static void setBillTransport(Context context, Transport transport) {
        prefs(context).edit().putString(KEY_BILL_TRANSPORT, toStorage(transport)).apply();
    }

    public static void setKotTransport(Context context, Transport transport) {
        prefs(context).edit().putString(KEY_KOT_TRANSPORT, toStorage(transport)).apply();
    }

    public static String networkHost(Context context) {
        return prefs(context).getString(KEY_NETWORK_HOST, "");
    }

    public static int networkPort(Context context) {
        return prefs(context).getInt(KEY_NETWORK_PORT, 9100);
    }

    public static void setNetwork(Context context, String host, int port) {
        prefs(context).edit()
                .putString(KEY_NETWORK_HOST, host != null ? host.trim() : "")
                .putInt(KEY_NETWORK_PORT, port > 0 && port <= 65535 ? port : 9100)
                .apply();
    }

    public static String billUsbId(Context context) {
        return prefs(context).getString(KEY_BILL_USB_ID, "");
    }

    public static String billUsbName(Context context) {
        return prefs(context).getString(KEY_BILL_USB_NAME, "");
    }

    public static String kotUsbId(Context context) {
        return prefs(context).getString(KEY_KOT_USB_ID, "");
    }

    public static String kotUsbName(Context context) {
        return prefs(context).getString(KEY_KOT_USB_NAME, "");
    }

    public static void setBillUsb(Context context, String id, String name) {
        prefs(context).edit()
                .putString(KEY_BILL_USB_ID, id != null ? id.trim() : "")
                .putString(KEY_BILL_USB_NAME, name != null ? name.trim() : "")
                .apply();
    }

    public static void setKotUsb(Context context, String id, String name) {
        prefs(context).edit()
                .putString(KEY_KOT_USB_ID, id != null ? id.trim() : "")
                .putString(KEY_KOT_USB_NAME, name != null ? name.trim() : "")
                .apply();
    }

    public static boolean isAutoShareOnSave(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_SHARE, true);
    }

    public static void setAutoShareOnSave(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_SHARE, enabled).apply();
    }

    public static Transport transportFor(Context context, boolean isKot) {
        return isKot ? kotTransport(context) : billTransport(context);
    }

    public static String usbIdFor(Context context, boolean isKot) {
        return isKot ? kotUsbId(context) : billUsbId(context);
    }

    public static String usbNameFor(Context context, boolean isKot) {
        return isKot ? kotUsbName(context) : billUsbName(context);
    }

    public static void setUsbFor(Context context, boolean isKot, String id, String name) {
        if (isKot) {
            setKotUsb(context, id, name);
        } else {
            setBillUsb(context, id, name);
        }
    }

    static String toStorage(Transport transport) {
        if (transport == Transport.USB) {
            return "usb";
        }
        if (transport == Transport.NETWORK) {
            return "network";
        }
        return "bluetooth";
    }

    static Transport fromStorage(String raw) {
        if (raw == null) {
            return Transport.BLUETOOTH;
        }
        switch (raw.trim().toLowerCase()) {
            case "usb":
                return Transport.USB;
            case "network":
            case "wifi":
                return Transport.NETWORK;
            default:
                return Transport.BLUETOOTH;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
