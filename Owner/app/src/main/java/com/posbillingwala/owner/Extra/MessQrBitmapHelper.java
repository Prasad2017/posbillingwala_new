package com.posbillingwala.owner.Extra;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import androidx.annotation.Nullable;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import com.posbillingwala.owner.R;

import java.util.EnumMap;
import java.util.Map;

/** Branded Mess Common QR: blue, rounded finders, center logo. */
public final class MessQrBitmapHelper {

    private static final int BRAND_BLUE = 0xFF0B2AC4;
    private static final int QUIET_MODULES = 2;
    private static final float LOGO_RATIO = 0.26f;

    private MessQrBitmapHelper() {}

    public static Bitmap generateQrBitmap(String content, int sizePx) {
        return generateBrandedMessQr(null, content, sizePx);
    }

    public static Bitmap generateBrandedMessQr(@Nullable Context context, String content, int sizePx) {
        return render(context, content, sizePx, BRAND_BLUE, true);
    }

    @Nullable
    private static Bitmap render(@Nullable Context context, String content, int sizePx,
                                 int moduleColor, boolean withLogo) {
        if (content == null || content.trim().isEmpty() || sizePx < 64) {
            return null;
        }
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            QRCode qrCode = Encoder.encode(content, ErrorCorrectionLevel.H, hints);
            ByteMatrix input = qrCode.getMatrix();
            if (input == null) {
                return null;
            }
            int n = input.getWidth();
            int totalModules = n + QUIET_MODULES * 2;
            int modulePx = Math.max(2, sizePx / totalModules);
            int out = modulePx * totalModules;

            Bitmap bitmap = Bitmap.createBitmap(out, out, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);

            boolean[][] finder = markFinder(n);
            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setColor(moduleColor);
            fill.setStyle(Paint.Style.FILL);

            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    if (input.get(x, y) == 1 && !finder[y][x]) {
                        float l = (x + QUIET_MODULES) * modulePx;
                        float t = (y + QUIET_MODULES) * modulePx;
                        canvas.drawRect(l, t, l + modulePx, t + modulePx, fill);
                    }
                }
            }

            float finderSize = 7f * modulePx;
            drawRoundedFinder(canvas, QUIET_MODULES * modulePx, QUIET_MODULES * modulePx, finderSize, moduleColor);
            drawRoundedFinder(canvas, (QUIET_MODULES + n - 7) * modulePx, QUIET_MODULES * modulePx, finderSize, moduleColor);
            drawRoundedFinder(canvas, QUIET_MODULES * modulePx, (QUIET_MODULES + n - 7) * modulePx, finderSize, moduleColor);

            if (withLogo && context != null) {
                drawCenterLogo(canvas, context, out, moduleColor);
            }
            return bitmap;
        } catch (WriterException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean[][] markFinder(int n) {
        boolean[][] f = new boolean[n][n];
        markFinderAt(f, 0, 0);
        markFinderAt(f, n - 7, 0);
        markFinderAt(f, 0, n - 7);
        return f;
    }

    private static void markFinderAt(boolean[][] f, int ox, int oy) {
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                int yy = oy + y;
                int xx = ox + x;
                if (yy >= 0 && yy < f.length && xx >= 0 && xx < f[0].length) {
                    f[yy][xx] = true;
                }
            }
        }
    }

    private static void drawRoundedFinder(Canvas canvas, float left, float top, float size, int color) {
        float m = size / 7f;
        float outerR = m * 1.35f;
        float innerR = m * 0.85f;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        Path ring = new Path();
        ring.addRoundRect(new RectF(left, top, left + size, top + size), outerR, outerR, Path.Direction.CW);
        Path hole = new Path();
        hole.addRoundRect(new RectF(left + m, top + m, left + size - m, top + size - m),
                innerR, innerR, Path.Direction.CW);
        ring.op(hole, Path.Op.DIFFERENCE);
        canvas.drawPath(ring, paint);

        float eye = m * 3f;
        float ex = left + m * 2f;
        float ey = top + m * 2f;
        canvas.drawRoundRect(new RectF(ex, ey, ex + eye, ey + eye), m * 0.75f, m * 0.75f, paint);
    }

    private static void drawCenterLogo(Canvas canvas, Context context, int qrSize, int accentColor) {
        try {
            Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.app_logo);
            if (logo == null) {
                return;
            }
            int logoBox = Math.round(qrSize * LOGO_RATIO);
            int pad = Math.max(6, logoBox / 10);
            int total = logoBox + pad * 2;
            float left = (qrSize - total) / 2f;
            float top = (qrSize - total) / 2f;

            Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
            white.setColor(Color.WHITE);
            white.setStyle(Paint.Style.FILL);
            float radius = total * 0.18f;
            canvas.drawRoundRect(new RectF(left, top, left + total, top + total), radius, radius, white);

            Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            stroke.setColor(accentColor);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Math.max(2f, pad / 2f));
            canvas.drawRoundRect(new RectF(left + 1, top + 1, left + total - 1, top + total - 1),
                    radius, radius, stroke);

            Bitmap scaled = Bitmap.createScaledBitmap(logo, logoBox, logoBox, true);
            canvas.drawBitmap(scaled, left + pad, top + pad, null);
            if (scaled != logo) {
                scaled.recycle();
            }
        } catch (Exception ignored) {
        }
    }
}
