package com.pos_billingwala.PaymentDisplay;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

/**
 * Local HTTP + WebSocket server for browser payment display (LAN only).
 */
public final class PaymentDisplayServer {

    private static final String TAG = "PaymentDisplay";
    private static final int[] PREFERRED_PORTS = {8080, 8081, 8765, 9090, 18080};
    private static final long PAIRING_TOKEN_TTL_MS = 30L * 60L * 1000L;

    public interface ClientCountListener {
        void onClientCountChanged(int count);
    }

    private final Context appContext;
    private final ClientCountListener clientCountListener;
    private final List<DisplaySocket> clients = new CopyOnWriteArrayList<>();

    private Host host;
    private String localIp;
    private String pairingToken;
    private long pairingTokenExpiresAt;
    private PaymentDisplayBillPayload activeBill;

    public PaymentDisplayServer(Context context, ClientCountListener listener) {
        this.appContext = context.getApplicationContext();
        this.clientCountListener = listener;
    }

    public synchronized boolean startServer() {
        if (isRunning()) {
            return true;
        }
        localIp = PaymentDisplayLocalIp.detect();
        if (localIp == null) {
            Log.w(TAG, "PAYMENT_DISPLAY_ERROR local_ip_missing");
            return false;
        }
        Exception last = null;
        for (int port : PREFERRED_PORTS) {
            try {
                Host candidate = new Host(port);
                candidate.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
                host = candidate;
                rotatePairingToken();
                Log.i(TAG, "PAYMENT_DISPLAY_SERVER_STARTED port=" + port + " ip=" + localIp);
                return true;
            } catch (Exception e) {
                last = e;
            }
        }
        Log.e(TAG, "PAYMENT_DISPLAY_ERROR bind_failed", last);
        return false;
    }

    public synchronized void stopServer() {
        for (DisplaySocket client : new ArrayList<>(clients)) {
            try {
                client.close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "stop", false);
            } catch (Exception ignored) {
            }
        }
        clients.clear();
        notifyClients();
        if (host != null) {
            host.stop();
            host = null;
        }
        pairingToken = null;
        Log.i(TAG, "PAYMENT_DISPLAY_SERVER_STOPPED");
    }

    public boolean isRunning() {
        return host != null && host.isAlive();
    }

    public String getLocalIp() {
        refreshIp();
        return localIp;
    }

    public int getPort() {
        return host != null ? host.getListeningPort() : 0;
    }

    public int getConnectedClientCount() {
        int n = 0;
        for (DisplaySocket c : clients) {
            if (c.isOpen()) {
                n++;
            }
        }
        return n;
    }

    public PaymentDisplayBillPayload getActiveBill() {
        return activeBill;
    }

    public void setActiveBill(PaymentDisplayBillPayload bill) {
        this.activeBill = bill;
    }

    public String getPairingUrl() {
        if (!isRunning() || localIp == null || pairingToken == null) {
            return null;
        }
        if (System.currentTimeMillis() > pairingTokenExpiresAt) {
            return null;
        }
        return "http://" + localIp + ":" + getPort() + "/display?token=" + pairingToken;
    }

    public String getDisplayBaseUrl() {
        if (!isRunning() || localIp == null) {
            return null;
        }
        return "http://" + localIp + ":" + getPort() + "/display";
    }

    public void rotatePairingToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        pairingToken = android.util.Base64.encodeToString(bytes,
                android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);
        pairingTokenExpiresAt = System.currentTimeMillis() + PAIRING_TOKEN_TTL_MS;
        Log.i(TAG, "PAYMENT_DISPLAY_PAIRING_STARTED");
    }

    public void refreshIp() {
        String ip = PaymentDisplayLocalIp.detect();
        if (ip != null) {
            localIp = ip;
        }
    }

    public void broadcastBillUpdated(PaymentDisplayBillPayload bill) {
        activeBill = bill;
        JSONObject msg = new JSONObject();
        try {
            msg.put("type", "BILL_UPDATED");
            msg.put("payload", bill.toWireJson());
        } catch (Exception ignored) {
        }
        broadcast(msg.toString());
        Log.i(TAG, "PAYMENT_DISPLAY_BILL_SENT bill=" + bill.billNumber + " amount=" + bill.amount);
    }

    public void broadcastBillCleared(String reason) {
        activeBill = null;
        JSONObject msg = new JSONObject();
        try {
            msg.put("type", "BILL_CLEARED");
            JSONObject payload = new JSONObject();
            payload.put("reason", reason != null ? reason : "expired");
            msg.put("payload", payload);
        } catch (Exception ignored) {
        }
        broadcast(msg.toString());
        Log.i(TAG, "PAYMENT_DISPLAY_BILL_EXPIRED reason=" + reason);
    }

    public void disconnectAllClients() {
        for (DisplaySocket client : new ArrayList<>(clients)) {
            try {
                client.close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "disconnect", false);
            } catch (Exception ignored) {
            }
        }
        clients.clear();
        notifyClients();
        Log.i(TAG, "PAYMENT_DISPLAY_DISCONNECTED all");
    }

    private void broadcast(String text) {
        for (DisplaySocket client : clients) {
            try {
                if (client.isOpen()) {
                    client.send(text);
                }
            } catch (Exception e) {
                clients.remove(client);
            }
        }
        notifyClients();
    }

    void registerClient(DisplaySocket socket) {
        clients.add(socket);
        notifyClients();
        Log.i(TAG, "PAYMENT_DISPLAY_CONNECTED clients=" + getConnectedClientCount());
        Log.i(TAG, "PAYMENT_DISPLAY_PAIRED");
    }

    void unregisterClient(DisplaySocket socket) {
        clients.remove(socket);
        notifyClients();
        Log.i(TAG, "PAYMENT_DISPLAY_DISCONNECTED clients=" + getConnectedClientCount());
    }

    private void notifyClients() {
        if (clientCountListener != null) {
            clientCountListener.onClientCountChanged(getConnectedClientCount());
        }
    }

    boolean isTokenValid(String token) {
        if (token == null || token.isEmpty() || pairingToken == null) {
            return false;
        }
        if (System.currentTimeMillis() > pairingTokenExpiresAt) {
            return false;
        }
        return token.equals(pairingToken);
    }

    private final class Host extends NanoWSD {
        Host(int port) {
            super(port);
        }

        @Override
        protected WebSocket openWebSocket(IHTTPSession handshake) {
            Map<String, String> params = handshake.getParms();
            String token = params != null ? params.get("token") : null;
            if (!isTokenValid(token)) {
                Log.w(TAG, "PAYMENT_DISPLAY_ERROR ws_rejected_token");
                return new RejectedSocket(handshake);
            }
            return new DisplaySocket(handshake, PaymentDisplayServer.this);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            if (uri == null) {
                uri = "/";
            }
            if ("/display/ws".equals(uri) || "/ws".equals(uri)) {
                return super.serve(session);
            }
            Map<String, String> params = session.getParms();
            String token = params != null ? params.get("token") : null;
            boolean tokenOk = isTokenValid(token);

            if ("/display".equals(uri) || "/display/".equals(uri) || "/".equals(uri)) {
                if (!tokenOk) {
                    return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "text/html",
                            "<!DOCTYPE html><html><body style=\"font-family:sans-serif;text-align:center;padding:40px\">"
                                    + "<h2>Payment display</h2><p>Invalid or expired pairing link.</p>"
                                    + "<p>Open Settings → Payment Display on the POS and scan again.</p>"
                                    + "</body></html>");
                }
                return serveAsset("index.html", "text/html");
            }
            if ("/display/status".equals(uri)) {
                if (!tokenOk) {
                    return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", "{}");
                }
                JSONObject o = new JSONObject();
                try {
                    boolean has = activeBill != null && !activeBill.isExpired();
                    o.put("connectedClients", getConnectedClientCount());
                    o.put("hasBill", has);
                    o.put("bill", has ? activeBill.toWireJson() : JSONObject.NULL);
                } catch (Exception ignored) {
                }
                return newFixedLengthResponse(Response.Status.OK, "application/json", o.toString());
            }
            if (uri.startsWith("/display/assets/")) {
                return serveAsset(uri.substring("/display/assets/".length()), contentType(uri));
            }
            if (uri.startsWith("/assets/")) {
                return serveAsset(uri.substring("/assets/".length()), contentType(uri));
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not found");
        }

        private Response serveAsset(String relative, String mime) {
            String safe = sanitize(relative);
            if (safe == null) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "Forbidden");
            }
            try {
                AssetManager am = appContext.getAssets();
                InputStream in = am.open("payment_display/" + safe);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    bos.write(buf, 0, n);
                }
                in.close();
                byte[] data = bos.toByteArray();
                Response r = newFixedLengthResponse(Response.Status.OK, mime,
                        new ByteArrayInputStream(data), data.length);
                r.addHeader("Cache-Control", "no-store, max-age=0");
                return r;
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not found");
            }
        }

        private String sanitize(String relative) {
            if (relative == null) {
                return null;
            }
            String path = relative.replace('\\', '/');
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.contains("..") || path.contains(":")) {
                return null;
            }
            if (path.isEmpty()) {
                return "index.html";
            }
            if ("index.html".equals(path) || "css/display.css".equals(path) || "js/display.js".equals(path)) {
                return path;
            }
            return null;
        }

        private String contentType(String path) {
            if (path.endsWith(".html")) {
                return "text/html";
            }
            if (path.endsWith(".css")) {
                return "text/css";
            }
            if (path.endsWith(".js")) {
                return "application/javascript";
            }
            return "application/octet-stream";
        }
    }

    static final class RejectedSocket extends NanoWSD.WebSocket {
        RejectedSocket(NanoHTTPD.IHTTPSession handshake) {
            super(handshake);
        }

        @Override
        protected void onOpen() {
            try {
                close(NanoWSD.WebSocketFrame.CloseCode.PolicyViolation, "unauthorized", false);
            } catch (Exception ignored) {
            }
        }

        @Override
        protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        }

        @Override
        protected void onMessage(NanoWSD.WebSocketFrame message) {
        }

        @Override
        protected void onPong(NanoWSD.WebSocketFrame pong) {
        }

        @Override
        protected void onException(IOException exception) {
        }
    }

    static final class DisplaySocket extends NanoWSD.WebSocket {
        private final PaymentDisplayServer owner;

        DisplaySocket(NanoHTTPD.IHTTPSession handshake, PaymentDisplayServer owner) {
            super(handshake);
            this.owner = owner;
        }

        @Override
        protected void onOpen() {
            owner.registerClient(this);
            try {
                JSONObject connected = new JSONObject();
                connected.put("type", "DISPLAY_CONNECTED");
                connected.put("payload", new JSONObject());
                send(connected.toString());

                PaymentDisplayBillPayload bill = owner.activeBill;
                if (bill != null && !bill.isExpired()) {
                    JSONObject upd = new JSONObject();
                    upd.put("type", "BILL_UPDATED");
                    upd.put("payload", bill.toWireJson());
                    send(upd.toString());
                } else {
                    if (bill != null && bill.isExpired()) {
                        owner.activeBill = null;
                    }
                    JSONObject cleared = new JSONObject();
                    cleared.put("type", "BILL_CLEARED");
                    JSONObject payload = new JSONObject();
                    payload.put("reason", "none");
                    cleared.put("payload", payload);
                    send(cleared.toString());
                }
            } catch (Exception e) {
                Log.e(TAG, "PAYMENT_DISPLAY_ERROR ws_open", e);
            }
        }

        @Override
        protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            owner.unregisterClient(this);
        }

        @Override
        protected void onMessage(NanoWSD.WebSocketFrame message) {
            try {
                String text = message.getTextPayload();
                JSONObject o = new JSONObject(text);
                if ("DISPLAY_PING".equals(o.optString("type"))) {
                    JSONObject pong = new JSONObject();
                    pong.put("type", "DISPLAY_PONG");
                    JSONObject payload = new JSONObject();
                    payload.put("serverTime", System.currentTimeMillis());
                    pong.put("payload", payload);
                    send(pong.toString());
                }
            } catch (Exception e) {
                Log.e(TAG, "PAYMENT_DISPLAY_ERROR ws_message", e);
            }
        }

        @Override
        protected void onPong(NanoWSD.WebSocketFrame pong) {
        }

        @Override
        protected void onException(IOException exception) {
            Log.e(TAG, "PAYMENT_DISPLAY_ERROR ws_client", exception);
            owner.unregisterClient(this);
        }
    }
}
