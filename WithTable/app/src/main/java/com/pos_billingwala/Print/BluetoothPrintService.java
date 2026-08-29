package com.pos_billingwala.Print;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import com.pos_billingwala.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    /** Discovery cancel is async; a short wait avoids SDP handshake timeouts. */
    private static final long DISCOVERY_SETTLE_MS = 400L;
    /** Stack must release the RFCOMM channel after a failed connect() before retry. */
    private static final long RETRY_SETTLE_MS = 450L;

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
        private final boolean preferSecure;
        private volatile BluetoothSocket workingSocket;
        private volatile boolean cancelled;

        ConnectThread(BluetoothDevice device, boolean secure) {
            this.device = device;
            this.preferSecure = secure;
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
            sleepQuietly(DISCOVERY_SETTLE_MS);

            BluetoothDevice target = resolveDevice(device);
            ConnectResult result = connectWithStrategies(target);
            if (cancelled) {
                closeQuietly(result != null ? result.socket : null);
                return;
            }
            if (result == null || result.socket == null) {
                Log.e(TAG, "ConnectThread: all RFCOMM strategies failed");
                notifyConnectionFailed();
                return;
            }

            synchronized (BluetoothPrintService.this) {
                if (cancelled || connectThread != this) {
                    closeQuietly(result.socket);
                    return;
                }
                connectThread = null;
            }
            connected(result.socket, target, result.socketType);
        }

        private ConnectResult connectWithStrategies(BluetoothDevice target) {
            List<SocketStrategy> strategies = buildStrategies(target, preferSecure);
            for (int i = 0; i < strategies.size(); i++) {
                if (cancelled || isInterrupted()) {
                    return null;
                }
                SocketStrategy strategy = strategies.get(i);
                BluetoothSocket socket = strategy.open();
                if (socket == null) {
                    continue;
                }
                workingSocket = socket;
                try {
                    Log.d(TAG, "ConnectThread: trying " + strategy.label);
                    socket.connect();
                    if (cancelled) {
                        closeQuietly(socket);
                        return null;
                    }
                    Log.i(TAG, "ConnectThread: connected via " + strategy.label);
                    return new ConnectResult(socket, strategy.label);
                } catch (IOException e) {
                    Log.w(TAG, "ConnectThread: " + strategy.label + " failed: " + e.getMessage());
                    closeQuietly(socket);
                    workingSocket = null;
                    if (i < strategies.size() - 1) {
                        sleepQuietly(RETRY_SETTLE_MS);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ConnectThread: unexpected error on " + strategy.label, e);
                    closeQuietly(socket);
                    workingSocket = null;
                    sleepQuietly(RETRY_SETTLE_MS);
                }
            }
            return null;
        }

        void cancel() {
            cancelled = true;
            interrupt();
            closeQuietly(workingSocket);
            workingSocket = null;
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

    /**
     * Thermal printers often fail SDP UUID connect with
     * "read failed, socket might closed or timeout, read ret: -1".
     * Channel-1 reflection sockets skip SDP. Insecure is required for most ESC/POS printers.
     */
    private List<SocketStrategy> buildStrategies(BluetoothDevice device, boolean preferSecure) {
        List<SocketStrategy> strategies = new ArrayList<>();
        if (device == null) {
            return strategies;
        }

        SocketStrategy insecureSpp = uuidStrategy(device, SPP_UUID, false, "Insecure-SPP");
        SocketStrategy secureSpp = uuidStrategy(device, SPP_UUID, true, "Secure-SPP");
        SocketStrategy insecureCh1 = reflectionStrategy(device, "createInsecureRfcommSocket", 1);
        SocketStrategy secureCh1 = reflectionStrategy(device, "createRfcommSocket", 1);

        if (preferSecure) {
            addStrategy(strategies, secureSpp);
            addStrategy(strategies, insecureSpp);
            addStrategy(strategies, secureCh1);
            addStrategy(strategies, insecureCh1);
        } else {
            addStrategy(strategies, insecureSpp);
            addStrategy(strategies, insecureCh1);
            addStrategy(strategies, secureSpp);
            addStrategy(strategies, secureCh1);
        }

        ParcelUuid[] advertised = null;
        try {
            advertised = device.getUuids();
        } catch (Exception e) {
            Log.w(TAG, "getUuids failed", e);
        }
        if (advertised != null) {
            for (ParcelUuid parcelUuid : advertised) {
                if (parcelUuid == null || parcelUuid.getUuid() == null) {
                    continue;
                }
                UUID uuid = parcelUuid.getUuid();
                if (SPP_UUID.equals(uuid)) {
                    continue;
                }
                addStrategy(strategies, uuidStrategy(device, uuid, false, "Insecure-" + uuid));
                addStrategy(strategies, uuidStrategy(device, uuid, true, "Secure-" + uuid));
            }
        }

        addStrategy(strategies, reflectionStrategy(device, "createInsecureRfcommSocket", 2));
        addStrategy(strategies, reflectionStrategy(device, "createRfcommSocket", 2));
        return strategies;
    }

    private static void addStrategy(List<SocketStrategy> list, SocketStrategy strategy) {
        if (strategy != null) {
            list.add(strategy);
        }
    }

    private static SocketStrategy uuidStrategy(BluetoothDevice device, UUID uuid, boolean secure, String label) {
        return new SocketStrategy(label, () -> {
            try {
                return secure
                        ? device.createRfcommSocketToServiceRecord(uuid)
                        : device.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.w(TAG, "open " + label + " failed", e);
                return null;
            }
        });
    }

    private static SocketStrategy reflectionStrategy(BluetoothDevice device, String methodName, int channel) {
        return new SocketStrategy(methodName + "(ch=" + channel + ")", () ->
                reflectionSocket(device, methodName, channel));
    }

    private BluetoothDevice resolveDevice(BluetoothDevice device) {
        if (device == null || adapter == null) {
            return device;
        }
        try {
            String address = device.getAddress();
            if (address != null && !address.isEmpty()) {
                return adapter.getRemoteDevice(address);
            }
        } catch (Exception e) {
            Log.w(TAG, "resolveDevice failed, using original", e);
        }
        return device;
    }

    private static BluetoothSocket reflectionSocket(BluetoothDevice device, String methodName, int channel) {
        if (device == null) {
            return null;
        }
        try {
            Method method = device.getClass().getMethod(methodName, int.class);
            return (BluetoothSocket) method.invoke(device, channel);
        } catch (Exception e) {
            Log.w(TAG, methodName + " ch=" + channel + " failed", e);
            return null;
        }
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class SocketStrategy {
        final String label;
        final SocketOpener opener;

        SocketStrategy(String label, SocketOpener opener) {
            this.label = label;
            this.opener = opener;
        }

        BluetoothSocket open() {
            try {
                return opener.open();
            } catch (Exception e) {
                Log.w(TAG, "strategy open failed: " + label, e);
                return null;
            }
        }
    }

    private interface SocketOpener {
        BluetoothSocket open();
    }

    private static final class ConnectResult {
        final BluetoothSocket socket;
        final String socketType;

        ConnectResult(BluetoothSocket socket, String socketType) {
            this.socket = socket;
            this.socketType = socketType;
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
