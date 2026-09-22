package com.pos_billingwala.PaymentDisplay;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class PaymentDisplayBillPayload {

    public final String billId;
    public final String billNumber;
    public final double amount;
    public final String currency;
    public final String shopName;
    public final String upiId;
    public final String payeeName;
    public final String qrPayload;
    public final String qrSvg;
    public final long displayedAtMs;
    public final long expiresAtMs;

    public PaymentDisplayBillPayload(
            String billId,
            String billNumber,
            double amount,
            String currency,
            String shopName,
            String upiId,
            String payeeName,
            String qrPayload,
            String qrSvg,
            long displayedAtMs,
            long expiresAtMs
    ) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.amount = amount;
        this.currency = currency;
        this.shopName = shopName;
        this.upiId = upiId;
        this.payeeName = payeeName;
        this.qrPayload = qrPayload;
        this.qrSvg = qrSvg;
        this.displayedAtMs = displayedAtMs;
        this.expiresAtMs = expiresAtMs;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAtMs;
    }

    public long remainingMs() {
        return Math.max(0L, expiresAtMs - System.currentTimeMillis());
    }

    public JSONObject toWireJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("billId", billId);
            o.put("billNumber", billNumber);
            o.put("amount", amount);
            o.put("currency", currency);
            o.put("shopName", shopName);
            o.put("payeeName", payeeName);
            o.put("qrPayload", qrPayload);
            o.put("qrSvg", qrSvg);
            o.put("displayedAt", iso(displayedAtMs));
            o.put("expiresAt", iso(expiresAtMs));
        } catch (Exception ignored) {
        }
        return o;
    }

    public JSONObject toPersistJson() {
        JSONObject o = toWireJson();
        try {
            o.put("upiId", maskUpi(upiId));
        } catch (Exception ignored) {
        }
        return o;
    }

    public static PaymentDisplayBillPayload fromJson(JSONObject o) {
        if (o == null) {
            return null;
        }
        try {
            return new PaymentDisplayBillPayload(
                    o.optString("billId", ""),
                    o.optString("billNumber", ""),
                    o.optDouble("amount", 0),
                    o.optString("currency", "INR"),
                    o.optString("shopName", ""),
                    o.optString("upiId", ""),
                    o.optString("payeeName", ""),
                    o.optString("qrPayload", ""),
                    o.optString("qrSvg", ""),
                    parseIso(o.optString("displayedAt", "")),
                    parseIso(o.optString("expiresAt", ""))
            );
        } catch (Exception e) {
            return null;
        }
    }

    public static String maskUpi(String upi) {
        if (upi == null) {
            return "***";
        }
        String v = upi.trim();
        int at = v.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return v.charAt(0) + "***" + v.substring(at);
    }

    private static String iso(long ms) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(ms));
    }

    private static long parseIso(String value) {
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
            Date d = sdf.parse(value);
            return d != null ? d.getTime() : 0L;
        } catch (Exception e) {
            try {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date d = sdf2.parse(value);
                return d != null ? d.getTime() : 0L;
            } catch (Exception e2) {
                return 0L;
            }
        }
    }
}
