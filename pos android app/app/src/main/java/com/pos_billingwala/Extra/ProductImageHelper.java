package com.pos_billingwala.Extra;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.pos_billingwala.BuildConfig;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Product image encode/display — mirrors Flutter product_image_thumb.dart.
 * Stores data URLs (data:image/...;base64,...) or http(s)/relative media paths.
 */
public final class ProductImageHelper {

    public static final int MAX_EDGE_PX = 480;
    public static final int JPEG_QUALITY = 70;
    public static final int MAX_BYTES = 900_000;

    private ProductImageHelper() {
    }

    public static boolean hasImage(@Nullable String value) {
        return !TextUtils.isEmpty(value) && value.trim().length() > 0;
    }

    /** Encode gallery/camera URI to a compressed JPEG data URL (Flutter parity). */
    @Nullable
    public static String encodeUri(Context context, Uri uri) {
        if (context == null || uri == null) {
            return null;
        }
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            if (bitmap == null) {
                return null;
            }
            bitmap = scaleDown(bitmap, MAX_EDGE_PX);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream);
            byte[] bytes = stream.toByteArray();
            if (bytes.length == 0 || bytes.length > MAX_BYTES) {
                return null;
            }
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Nullable
    public static Bitmap decodeToBitmap(@Nullable String value) {
        if (!hasImage(value)) {
            return null;
        }
        String raw = value.trim();
        try {
            if (raw.startsWith("data:image")) {
                int comma = raw.indexOf(',');
                if (comma <= 0 || comma >= raw.length() - 1) {
                    return null;
                }
                byte[] bytes = Base64.decode(raw.substring(comma + 1), Base64.DEFAULT);
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
            /* Plain base64 (legacy company-logo style) */
            if (!raw.startsWith("http") && !raw.contains("/")) {
                byte[] bytes = Base64.decode(raw, Base64.DEFAULT);
                if (bytes != null && bytes.length > 0) {
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bmp != null) {
                        return bmp;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static String resolveNetworkUrl(@Nullable String value) {
        if (!hasImage(value)) {
            return null;
        }
        String raw = value.trim();
        if (raw.startsWith("data:image")) {
            return null;
        }
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw;
        }
        String base = BuildConfig.MEDIA_BASE_URL;
        if (base == null) {
            base = "";
        }
        if (!base.endsWith("/") && !raw.startsWith("/")) {
            return base + "/" + raw;
        }
        return base + raw;
    }

    /**
     * Bind product image to ImageView. Hides view when empty (Flutter: no placeholder on billing).
     */
    public static void bind(@Nullable ImageView imageView, @Nullable String value, boolean showPlaceholder) {
        if (imageView == null) {
            return;
        }
        if (!hasImage(value)) {
            if (showPlaceholder) {
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            } else {
                imageView.setVisibility(View.GONE);
                imageView.setImageDrawable(null);
            }
            return;
        }
        Bitmap local = decodeToBitmap(value);
        if (local != null) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(local);
            return;
        }
        String url = resolveNetworkUrl(value);
        if (url != null) {
            imageView.setVisibility(View.VISIBLE);
            try {
                Picasso.get()
                        .load(url)
                        .resize(112, 112)
                        .centerCrop()
                        .into(imageView);
            } catch (Exception e) {
                if (showPlaceholder) {
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                } else {
                    imageView.setVisibility(View.GONE);
                }
            }
            return;
        }
        if (showPlaceholder) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        } else {
            imageView.setVisibility(View.GONE);
            imageView.setImageDrawable(null);
        }
    }

    private static Bitmap scaleDown(Bitmap src, int maxEdge) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxEdge && h <= maxEdge) {
            return src;
        }
        float scale = Math.min((float) maxEdge / w, (float) maxEdge / h);
        int nw = Math.max(1, Math.round(w * scale));
        int nh = Math.max(1, Math.round(h * scale));
        return Bitmap.createScaledBitmap(src, nw, nh, true);
    }
}
