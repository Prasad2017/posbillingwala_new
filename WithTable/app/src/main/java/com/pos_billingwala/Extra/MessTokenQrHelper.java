package com.pos_billingwala.Extra;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class MessTokenQrHelper {

    public static final String MEMBER_TYPE_WALK_IN = "walk_in";
    public static final String MEMBER_TYPE_MEMBER = "member";
    public static final String TOKEN_STATE_ACTIVE = "active";
    public static final String TOKEN_STATE_VERIFIED = "verified";

    private static final String PREFIX = "POSBILL|v1|";

    private MessTokenQrHelper() {
    }

    public static String generateTokenCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String buildPayload(String tokenCode, String userId, String memberType) {
        return PREFIX + tokenCode + "|" + userId + "|" + memberType;
    }

    public static String[] parsePayload(String raw) {
        if (raw == null || !raw.startsWith(PREFIX)) {
            return null;
        }
        String[] parts = raw.split("\\|");
        if (parts.length < 5) {
            return null;
        }
        return new String[]{parts[2], parts[3], parts[4]};
    }

    public static Bitmap generateQrBitmap(String content, int sizePx) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            return null;
        }
    }

    public static String resolveMessType() {
        java.util.Calendar datetime = java.util.Calendar.getInstance();
        int hourOfDay = datetime.get(java.util.Calendar.HOUR_OF_DAY);
        if (hourOfDay >= 18) {
            return "Dinner";
        }
        if (hourOfDay >= 7) {
            return "Lunch";
        }
        return "Meal";
    }
}
