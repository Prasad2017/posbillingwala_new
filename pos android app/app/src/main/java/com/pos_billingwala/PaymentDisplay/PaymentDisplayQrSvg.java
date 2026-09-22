package com.pos_billingwala.PaymentDisplay;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

/** Builds an offline SVG QR (no CDN) for the browser display page. */
public final class PaymentDisplayQrSvg {

    private PaymentDisplayQrSvg() {
    }

    public static String fromPayload(String data) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 0, 0, hints);
            int modules = matrix.getWidth();
            int moduleSize = 8;
            int margin = 2;
            int dim = (modules + margin * 2) * moduleSize;
            StringBuilder sb = new StringBuilder(modules * modules * 8);
            sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
                    .append(dim).append(' ').append(dim)
                    .append("\" width=\"100%\" height=\"100%\" shape-rendering=\"crispEdges\">");
            sb.append("<rect width=\"").append(dim).append("\" height=\"").append(dim)
                    .append("\" fill=\"#ffffff\"/>");
            for (int y = 0; y < modules; y++) {
                for (int x = 0; x < modules; x++) {
                    if (!matrix.get(x, y)) {
                        continue;
                    }
                    int px = (x + margin) * moduleSize;
                    int py = (y + margin) * moduleSize;
                    sb.append("<rect x=\"").append(px).append("\" y=\"").append(py)
                            .append("\" width=\"").append(moduleSize)
                            .append("\" height=\"").append(moduleSize)
                            .append("\" fill=\"#000000\"/>");
                }
            }
            sb.append("</svg>");
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
