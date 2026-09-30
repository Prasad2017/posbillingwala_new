package com.pos_billingwala.Print;

import android.content.Context;
import android.util.Log;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Raw ESC/POS over TCP (default port 9100). Shared by bill + KOT network transport. */
public final class NetworkEscPosPrinter {

    private static final String TAG = "NetworkEscPos";
    private static final int CONNECT_TIMEOUT_MS = 4000;
    private static final int SO_TIMEOUT_MS = 8000;

    private NetworkEscPosPrinter() {
    }

    public static boolean write(Context context, byte[] data) {
        if (data == null || data.length == 0) {
            return false;
        }
        String host = PrinterEndpointPrefs.networkHost(context);
        int port = PrinterEndpointPrefs.networkPort(context);
        return writeTo(host, port, data);
    }

    public static boolean writeTo(String host, int port, byte[] data) {
        if (host == null || host.trim().isEmpty() || data == null || data.length == 0) {
            return false;
        }
        int p = port > 0 && port <= 65535 ? port : 9100;
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host.trim(), p), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(SO_TIMEOUT_MS);
            OutputStream out = socket.getOutputStream();
            out.write(data);
            out.flush();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Network print failed to " + host + ":" + p, e);
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static boolean testConnection(Context context) {
        String host = PrinterEndpointPrefs.networkHost(context);
        int port = PrinterEndpointPrefs.networkPort(context);
        if (host == null || host.trim().isEmpty()) {
            return false;
        }
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host.trim(), port > 0 ? port : 9100), CONNECT_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
