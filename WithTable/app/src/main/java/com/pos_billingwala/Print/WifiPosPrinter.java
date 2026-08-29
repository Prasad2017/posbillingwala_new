package com.pos_billingwala.Print;

import android.util.Log;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.regex.Pattern;

/**
 * Local Wi-Fi / LAN ESC/POS printer over raw TCP (default port 9100).
 * No Internet / API required — LAN only.
 */
public final class WifiPosPrinter implements PosPrinter {

    private static final String TAG = "WifiPosPrinter";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int SO_TIMEOUT_MS = 8000;
    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    private final PrinterProfile profile;
    private Socket socket;
    private OutputStream outputStream;
    private final Object ioLock = new Object();

    public WifiPosPrinter(PrinterProfile profile) {
        this.profile = profile;
    }

    @Override
    public boolean connect() {
        synchronized (ioLock) {
            try {
                if (isConnected()) {
                    return true;
                }
                String ip = profile != null ? profile.printerIp : "";
                int port = profile != null ? profile.printerPort : 9100;
                if (!isValidIp(ip)) {
                    Log.w(TAG, "Invalid printer IP: " + ip);
                    return false;
                }
                closeQuietly();
                Socket s = new Socket();
                s.connect(new InetSocketAddress(InetAddress.getByName(ip), port), CONNECT_TIMEOUT_MS);
                s.setTcpNoDelay(true);
                s.setSoTimeout(SO_TIMEOUT_MS);
                outputStream = s.getOutputStream();
                socket = s;
                return true;
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
                if (outputStream == null) {
                    return false;
                }
                outputStream.write(data);
                outputStream.flush();
                return true;
            } catch (Exception e) {
                Log.e(TAG, "print failed", e);
                closeQuietly();
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
        return socket != null && socket.isConnected() && !socket.isClosed() && outputStream != null;
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
        return PrinterConnectionType.WIFI;
    }

    @Override
    public String getPrinterIdentity() {
        return profile != null ? profile.identityKey() : "wifi:default";
    }

    @Override
    public String getDisplayName() {
        if (profile == null) {
            return "Network Printer";
        }
        return (profile.printerIp != null ? profile.printerIp : "") + ":" + profile.printerPort;
    }

    public PrinterProfile getProfile() {
        return profile;
    }

    public static boolean isValidIp(String ip) {
        return ip != null && IPV4.matcher(ip.trim()).matches();
    }

    private void closeQuietly() {
        try {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception ignored) {
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        } finally {
            outputStream = null;
            socket = null;
        }
    }
}
