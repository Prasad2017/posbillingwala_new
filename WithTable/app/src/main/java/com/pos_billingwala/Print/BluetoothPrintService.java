package com.pos_billingwala.Print;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.pos_billingwala.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

/**
 * Low-level RFCOMM session for one thermal printer.
 * Keeps the link open to the same MAC address and never crashes on null sockets.
 */
@SuppressLint("MissingPermission")
public class BluetoothPrintService {

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private static final String TAG = "BluetoothPrintService";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public interface ConnectionListener {
        void onConnected(BluetoothDevice device);

        void onConnectionFailed();

        void onConnectionLost();
    }

    private final BluetoothAdapter adapter;
    private final Handler handler;
    private ConnectionListener connectionListener;

    private int state = STATE_NONE;
    private String connectedDeviceAddress = "";
    private String pendingDeviceAddress = "";

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private volatile boolean intentionalDisconnect;

    public BluetoothPrintService(Context context, Handler handler) {
        adapter = BluetoothAdapter.getDefaultAdapter();
        this.handler = handler;
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    public synchronized int getState() {
        return state;
    }

    public synchronized String getConnectedDeviceAddress() {
        return connectedDeviceAddress != null ? connectedDeviceAddress : "";
    }

    public boolean isBluetoothEnabled() {
        return adapter != null && adapter.isEnabled();
    }

    public synchronized void start() {
        cancelConnectThread();
        cancelConnectedThread();
        state = STATE_LISTEN;
        Log.d(TAG, "start -> LISTEN");
    }

    public synchronized void stop() {
        intentionalDisconnect = true;
        cancelConnectThread();
        cancelConnectedThread();
        connectedDeviceAddress = "";
        pendingDeviceAddress = "";
        state = STATE_NONE;
        Log.d(TAG, "stop -> NONE");
    }

    /**
     * Connect only when needed. If already connected to the same MAC, does nothing.
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        if (device == null) {
            Log.e(TAG, "connect: device null");
            notifyConnectionFailed();
            return;
        }
        if (adapter == null) {
            Log.e(TAG, "connect: adapter null");
            notifyConnectionFailed();
            return;
        }

        String address = safeAddress(device);
        if (address.isEmpty()) {
            notifyConnectionFailed();
            return;
        }

        if (state == STATE_CONNECTED && address.equalsIgnoreCase(connectedDeviceAddress)) {
            Log.d(TAG, "connect: already connected to " + address);
            notifyConnected(device);
            return;
        }

        if (state == STATE_CONNECTING && address.equalsIgnoreCase(pendingDeviceAddress)) {
            Log.d(TAG, "connect: already connecting to " + address);
            return;
        }

        pendingDeviceAddress = address;

        if (state == STATE_CONNECTED && !address.equalsIgnoreCase(connectedDeviceAddress)) {
            cancelConnectedThread();
        }
        if (state == STATE_CONNECTING) {
            cancelConnectThread();
        }

        try {
            connectThread = new ConnectThread(device, secure);
            connectThread.start();
            state = STATE_CONNECTING;
            Log.d(TAG, "connect: started -> " + address);
        } catch (Exception e) {
            Log.e(TAG, "connect: failed to start thread", e);
            notifyConnectionFailed();
        }
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device, String socketType) {
        if (socket == null || device == null) {
            notifyConnectionFailed();
            return;
        }

        cancelConnectThread();
        cancelConnectedThread();

        connectedThread = new ConnectedThread(socket, socketType);
        if (!connectedThread.hasValidStreams()) {
            connectedThread.cancel();
            connectedThread = null;
            notifyConnectionFailed();
            return;
        }
        connectedThread.start();

        connectedDeviceAddress = safeAddress(device);
        pendingDeviceAddress = "";
        intentionalDisconnect = false;
        state = STATE_CONNECTED;
        Log.d(TAG, "connected: " + connectedDeviceAddress + " via " + socketType);
        notifyConnected(device);
    }

    public boolean write(byte[] out) {
        if (out == null || out.length == 0) {
            return false;
        }
        ConnectedThread thread;
        synchronized (this) {
            if (state != STATE_CONNECTED) {
                return false;
            }
            thread = connectedThread;
        }
        if (thread == null) {
            return false;
        }
        return thread.write(out);
    }

    // -------------------------------------------------------------------------
    // Threads
    // -------------------------------------------------------------------------

    private class ConnectThread extends Thread {

        private final BluetoothDevice device;
        private final boolean secure;

        ConnectThread(BluetoothDevice device, boolean secure) {
            this.device = device;
            this.secure = secure;
            setName("BtConnect-" + safeAddress(device));
        }

        @Override
        public void run() {
            if (adapter != null) {
                try {
                    adapter.cancelDiscovery();
                } catch (Exception e) {
                    Log.w(TAG, "cancelDiscovery failed", e);
                }
            }

            BluetoothSocket socket = openSocketWithFallback(device, secure);
            if (socket == null) {
                Log.e(TAG, "ConnectThread: could not open socket");
                notifyConnectionFailed();
                return;
            }

            try {
                socket.connect();
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: connect IO error, trying fallback channel", e);
                closeQuietly(socket);
                socket = openFallbackSocketOnly(device);
                if (socket == null) {
                    notifyConnectionFailed();
                    return;
                }
                try {
                    socket.connect();
                } catch (IOException e2) {
                    Log.e(TAG, "ConnectThread: fallback connect failed", e2);
                    closeQuietly(socket);
                    notifyConnectionFailed();
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "ConnectThread: unexpected connect error", e);
                closeQuietly(socket);
                notifyConnectionFailed();
                return;
            }

            synchronized (BluetoothPrintService.this) {
                connectThread = null;
            }
            connected(socket, device, secure ? "Secure" : "Insecure");
        }

        void cancel() {
            // socket closed in service cancel
        }
    }

    private class ConnectedThread extends Thread {

        private final BluetoothSocket socket;
        private final InputStream in;
        private final OutputStream out;

        ConnectedThread(BluetoothSocket socket, String socketType) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: stream open failed", e);
            }
            in = tmpIn;
            out = tmpOut;
            setName("BtConnected-" + socketType);
        }

        boolean hasValidStreams() {
            return in != null && out != null;
        }

        @Override
        public void run() {
            if (!hasValidStreams()) {
                notifyConnectionFailed();
                return;
            }
            byte[] buffer = new byte[1024];
            while (!isInterrupted()) {
                try {
                    int bytes = in.read(buffer);
                    if (bytes > 0 && handler != null) {
                        byte[] copy = Arrays.copyOf(buffer, bytes);
                        handler.obtainMessage(BluetoothPrintCallbacks.MESSAGE_READ, bytes, -1, copy)
                                .sendToTarget();
                    } else if (bytes < 0) {
                        Log.w(TAG, "ConnectedThread: remote closed input");
                        break;
                    }
                } catch (IOException e) {
                    Log.w(TAG, "ConnectedThread: read ended", e);
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "ConnectedThread: read error", e);
                    break;
                }
            }
            if (!isInterrupted() && !intentionalDisconnect) {
                notifyConnectionLost();
            }
        }

        boolean write(byte[] buffer) {
            if (out == null || buffer == null || buffer.length == 0) {
                return false;
            }
            try {
                out.write(buffer);
                out.flush();
                return true;
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: write failed", e);
                return false;
            } catch (Exception e) {
                Log.e(TAG, "ConnectedThread: unexpected write error", e);
                return false;
            }
        }

        void cancel() {
            interrupt();
            closeQuietly(in);
            closeQuietly(out);
            closeQuietly(socket);
        }
    }

    // -------------------------------------------------------------------------
    // Socket helpers
    // -------------------------------------------------------------------------

    private static BluetoothSocket openSocketWithFallback(BluetoothDevice device, boolean secure) {
        if (device == null) {
            return null;
        }
        BluetoothSocket socket = null;
        try {
            socket = secure
                    ? device.createRfcommSocketToServiceRecord(SPP_UUID)
                    : device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
        } catch (IOException e) {
            Log.w(TAG, "openSocket primary failed", e);
        }
        if (socket == null) {
            socket = reflectionSocket(device, 1);
        }
        return socket;
    }

    private static BluetoothSocket openFallbackSocketOnly(BluetoothDevice device) {
        BluetoothSocket socket = reflectionSocket(device, 1);
        if (socket == null) {
            socket = reflectionSocket(device, 2);
        }
        return socket;
    }

    private static BluetoothSocket reflectionSocket(BluetoothDevice device, int channel) {
        if (device == null) {
            return null;
        }
        try {
            Method method = device.getClass().getMethod("createRfcommSocket", int.class);
            return (BluetoothSocket) method.invoke(device, channel);
        } catch (Exception e) {
            Log.w(TAG, "reflectionSocket ch=" + channel + " failed", e);
            return null;
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    private static void closeQuietly(BluetoothSocket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }

    private static String safeAddress(BluetoothDevice device) {
        try {
            return device != null && device.getAddress() != null ? device.getAddress() : "";
        } catch (Exception e) {
            return "";
        }
    }

    // -------------------------------------------------------------------------
    // Notifications
    // -------------------------------------------------------------------------

    private synchronized void notifyConnected(BluetoothDevice device) {
        ConnectionListener listener = connectionListener;
        if (listener != null) {
            try {
                listener.onConnected(device);
            } catch (Exception e) {
                Log.e(TAG, "listener onConnected failed", e);
            }
        }
    }

    private synchronized void notifyConnectionFailed() {
        connectedDeviceAddress = "";
        pendingDeviceAddress = "";
        state = STATE_LISTEN;
        ConnectionListener listener = connectionListener;
        if (listener != null) {
            try {
                listener.onConnectionFailed();
            } catch (Exception e) {
                Log.e(TAG, "listener onConnectionFailed failed", e);
            }
        } else {
            postToast(R.string.connect_fail);
        }
    }

    private synchronized void notifyConnectionLost() {
        if (state == STATE_NONE || intentionalDisconnect) {
            intentionalDisconnect = false;
            return;
        }
        connectedDeviceAddress = "";
        cancelConnectedThread();
        state = STATE_LISTEN;
        ConnectionListener listener = connectionListener;
        if (listener != null) {
            try {
                listener.onConnectionLost();
            } catch (Exception e) {
                Log.e(TAG, "listener onConnectionLost failed", e);
            }
        } else {
            postToast(R.string.connect_lost);
        }
    }

    private void postToast(int resId) {
        if (handler == null) {
            return;
        }
        try {
            android.os.Message msg = handler.obtainMessage(BluetoothPrintCallbacks.MESSAGE_TOAST);
            android.os.Bundle bundle = new android.os.Bundle();
            bundle.putInt(BluetoothPrintCallbacks.TOAST, resId);
            msg.setData(bundle);
            handler.sendMessage(msg);
        } catch (Exception e) {
            Log.e(TAG, "postToast failed", e);
        }
    }

    private synchronized void cancelConnectThread() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
    }

    private synchronized void cancelConnectedThread() {
        if (connectedThread != null) {
            intentionalDisconnect = true;
            connectedThread.cancel();
            connectedThread = null;
        }
    }
}
