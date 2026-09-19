package com.pos_billingwala.PaymentDisplay;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton: server lifecycle, pairing, authoritative 5-minute QR expiry.
 */
public final class DisplayConnectionManager {

    private static final String TAG = "PaymentDisplay";

    public enum Status {
        STOPPED, STARTING, WAITING_FOR_PAIR, CONNECTED, ERROR
    }

    public interface Listener {
        void onDisplayStateChanged();
    }

    private static DisplayConnectionManager instance;

    public static synchronized DisplayConnectionManager get(Context context) {
        if (instance == null) {
            instance = new DisplayConnectionManager(context.getApplicationContext());
        }
        return instance;
    }

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private PaymentDisplayServer server;
    private Status status = Status.STOPPED;
    private String errorMessage;
    private int connectedClients;
    private String pairingUrl = "";
    private String localUrl = "";
    private PaymentDisplayBillPayload activeBill;
    private final Runnable expiryRunnable = this::onExpiry;
    private final Runnable ipWatchRunnable = this::onIpWatch;

    private DisplayConnectionManager(Context context) {
        this.appContext = context.getApplicationContext();
        restoreBillIfValid();
    }

    public void addListener(Listener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Status getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getConnectedClients() {
        return connectedClients;
    }

    public String getPairingUrl() {
        return pairingUrl;
    }

    public String getLocalUrl() {
        return localUrl;
    }

    public PaymentDisplayBillPayload getActiveBill() {
        return activeBill;
    }

    public boolean isServerRunning() {
        return server != null && server.isRunning();
    }

    public boolean isConnected() {
        return isServerRunning() && connectedClients > 0;
    }

    public boolean isAutoDisplayEnabled() {
        return PaymentDisplayPrefs.isAutoDisplayEnabled(appContext);
    }

    public void setAutoDisplayEnabled(boolean enabled) {
        PaymentDisplayPrefs.setAutoDisplayEnabled(appContext, enabled);
        notifyListeners();
    }

    public int getQrDurationSeconds() {
        return PaymentDisplayPrefs.getQrDurationSeconds(appContext);
    }

    public void setQrDurationSeconds(int seconds) {
        PaymentDisplayPrefs.setQrDurationSeconds(appContext, seconds);
        notifyListeners();
    }

    public synchronized boolean startServer(boolean rotateToken) {
        status = Status.STARTING;
        errorMessage = null;
        notifyListeners();
        if (server == null) {
            server = new PaymentDisplayServer(appContext, count -> mainHandler.post(() -> {
                connectedClients = count;
                status = !isServerRunning()
                        ? Status.STOPPED
                        : (count > 0 ? Status.CONNECTED : Status.WAITING_FOR_PAIR);
                pairingUrl = server != null && server.getPairingUrl() != null ? server.getPairingUrl() : pairingUrl;
                localUrl = server != null && server.getDisplayBaseUrl() != null ? server.getDisplayBaseUrl() : localUrl;
                notifyListeners();
            }));
        }
        boolean ok = server.startServer();
        if (!ok) {
            status = Status.ERROR;
            errorMessage = "Could not start display server. Enable hotspot/Wi-Fi.";
            notifyListeners();
            return false;
        }
        if (rotateToken) {
            server.rotatePairingToken();
        }
        if (activeBill != null && !activeBill.isExpired()) {
            server.setActiveBill(activeBill);
        }
        pairingUrl = server.getPairingUrl() != null ? server.getPairingUrl() : "";
        localUrl = server.getDisplayBaseUrl() != null ? server.getDisplayBaseUrl() : "";
        PaymentDisplayPrefs.setLastLocalUrl(appContext, localUrl);
        connectedClients = server.getConnectedClientCount();
        status = connectedClients > 0 ? Status.CONNECTED : Status.WAITING_FOR_PAIR;
        mainHandler.removeCallbacks(ipWatchRunnable);
        mainHandler.postDelayed(ipWatchRunnable, 20_000);
        notifyListeners();
        return true;
    }

    public synchronized void stopServer() {
        mainHandler.removeCallbacks(expiryRunnable);
        mainHandler.removeCallbacks(ipWatchRunnable);
        if (server != null) {
            server.stopServer();
        }
        status = Status.STOPPED;
        connectedClients = 0;
        pairingUrl = "";
        notifyListeners();
    }

    public synchronized void showPairingQr() {
        if (!isServerRunning()) {
            startServer(true);
        } else if (server != null) {
            server.rotatePairingToken();
            pairingUrl = server.getPairingUrl() != null ? server.getPairingUrl() : "";
            localUrl = server.getDisplayBaseUrl() != null ? server.getDisplayBaseUrl() : localUrl;
            status = connectedClients > 0 ? Status.CONNECTED : Status.WAITING_FOR_PAIR;
            notifyListeners();
        }
    }

    public synchronized void reconnect() {
        if (server != null) {
            server.refreshIp();
        }
        if (!isServerRunning()) {
            startServer(false);
            return;
        }
        pairingUrl = server.getPairingUrl() != null ? server.getPairingUrl() : pairingUrl;
        localUrl = server.getDisplayBaseUrl() != null ? server.getDisplayBaseUrl() : localUrl;
        PaymentDisplayPrefs.setLastLocalUrl(appContext, localUrl);
        Log.i(TAG, "PAYMENT_DISPLAY_RECONNECT");
        notifyListeners();
    }

    public synchronized void disconnectDisplays() {
        if (server != null) {
            server.disconnectAllClients();
        }
        connectedClients = 0;
        status = isServerRunning() ? Status.WAITING_FOR_PAIR : Status.STOPPED;
        notifyListeners();
    }

    public synchronized ShowResult publishBill(PaymentDisplayBillPayload bill) {
        if (!isServerRunning() || server == null) {
            return ShowResult.notConnected();
        }
        if (connectedClients < 1) {
            return ShowResult.notConnected();
        }
        mainHandler.removeCallbacks(expiryRunnable);
        server.broadcastBillUpdated(bill);
        activeBill = bill;
        PaymentDisplayPrefs.setLastBillJson(appContext, bill.toPersistJson().toString());
        long delay = Math.max(0L, bill.expiresAtMs - System.currentTimeMillis());
        mainHandler.postDelayed(expiryRunnable, delay);
        notifyListeners();
        return ShowResult.success(bill);
    }

    public synchronized void clearBill(String reason) {
        mainHandler.removeCallbacks(expiryRunnable);
        if (server != null && server.isRunning()) {
            server.broadcastBillCleared(reason);
        }
        activeBill = null;
        PaymentDisplayPrefs.clearLastBill(appContext);
        notifyListeners();
    }

    private void onExpiry() {
        clearBill("expired");
    }

    private void onIpWatch() {
        if (!isServerRunning()) {
            return;
        }
        reconnect();
        mainHandler.postDelayed(ipWatchRunnable, 20_000);
    }

    private void restoreBillIfValid() {
        try {
            String raw = PaymentDisplayPrefs.getLastBillJson(appContext);
            if (raw == null || raw.trim().isEmpty()) {
                return;
            }
            PaymentDisplayBillPayload bill = PaymentDisplayBillPayload.fromJson(new JSONObject(raw));
            if (bill != null && !bill.isExpired()) {
                activeBill = bill;
                long delay = bill.remainingMs();
                mainHandler.postDelayed(expiryRunnable, delay);
            } else {
                PaymentDisplayPrefs.clearLastBill(appContext);
            }
        } catch (Exception e) {
            PaymentDisplayPrefs.clearLastBill(appContext);
        }
        localUrl = PaymentDisplayPrefs.getLastLocalUrl(appContext);
    }

    private void notifyListeners() {
        for (Listener l : listeners) {
            try {
                l.onDisplayStateChanged();
            } catch (Exception ignored) {
            }
        }
    }

    public static final class ShowResult {
        public enum Code {
            SUCCESS, NOT_CONNECTED, UPI_NOT_CONFIGURED, INVALID_AMOUNT,
            INVOICE_INVALID, QR_FAILED, ERROR
        }

        public final Code code;
        public final String message;
        public final PaymentDisplayBillPayload payload;

        private ShowResult(Code code, String message, PaymentDisplayBillPayload payload) {
            this.code = code;
            this.message = message;
            this.payload = payload;
        }

        public static ShowResult success(PaymentDisplayBillPayload p) {
            return new ShowResult(Code.SUCCESS, null, p);
        }

        public static ShowResult notConnected() {
            return new ShowResult(Code.NOT_CONNECTED, "Payment display is not connected.", null);
        }

        public static ShowResult fail(Code code, String message) {
            return new ShowResult(code, message, null);
        }

        public boolean isSuccess() {
            return code == Code.SUCCESS;
        }
    }
}
