package com.pos_billingwala.Extra;

import android.graphics.Bitmap;
import android.widget.ImageView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

/**
 * Builds UPI intent QR codes from a shop UPI ID + bill amount.
 * Shop settings store the UPI VPA in {@code companys.paymentLogo} (text, not an image).
 */
public final class PaymentUpiQrHelper {

    private static final int DEFAULT_QR_SIZE_PX = 400;

    private PaymentUpiQrHelper() {
    }

    /** True when value looks like a UPI VPA (e.g. name@bank), not a legacy Base64 QR image. */
    public static boolean isUpiId(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > 120) {
            return false;
        }
        if (trimmed.contains("\n") || trimmed.contains(" ")) {
            return false;
        }
        int at = trimmed.indexOf('@');
        return at > 0 && at < trimmed.length() - 1;
    }

    public static String normalizeUpiId(String value) {
        return value == null ? "" : value.trim();
    }

    public static String buildUpiPayUri(String upiId, String payeeName, double amount, String note) {
        String pa = normalizeUpiId(upiId);
        String pn = encode(payeeName != null && !payeeName.trim().isEmpty() ? payeeName.trim() : "Merchant");
        String am = amount > 0 ? String.format(Locale.US, "%.2f", amount) : "";
        String tn = encode(note != null ? note : "");
        // Keep pa raw (VPA); encode only display/note params — some UPI apps reject %40 in pa
        StringBuilder sb = new StringBuilder("upi://pay?pa=").append(pa)
                .append("&pn=").append(pn)
                .append("&cu=INR");
        if (!am.isEmpty()) {
            sb.append("&am=").append(am);
        }
        if (note != null && !note.trim().isEmpty()) {
            sb.append("&tn=").append(tn);
        }
        return sb.toString();
    }

    public static Bitmap generateAmountQr(String upiId, String payeeName, double amount, String note) {
        return generateAmountQr(upiId, payeeName, amount, note, DEFAULT_QR_SIZE_PX);
    }

    public static Bitmap generateAmountQr(String upiId, String payeeName, double amount, String note, int sizePx) {
        if (!isUpiId(upiId) || amount <= 0) {
            return null;
        }
        String content = buildUpiPayUri(upiId, payeeName, amount, note);
        return MessTokenQrHelper.generateQrBitmap(content, sizePx);
    }

    /** Sets QR bitmap on views; returns true if a QR was applied. */
    public static boolean applyQrToViews(String upiId, String payeeName, double amount, String note,
                                         ImageView... views) {
        Bitmap bitmap = generateAmountQr(upiId, payeeName, amount, note);
        if (bitmap == null || views == null) {
            return false;
        }
        for (ImageView view : views) {
            if (view != null) {
                view.setImageBitmap(bitmap);
            }
        }
        return true;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
