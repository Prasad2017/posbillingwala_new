package com.pos_billingwala.Extra;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.pos_billingwala.Model.AppSplashResponse;
import com.pos_billingwala.Retrofit.Api;
import com.pos_billingwala.Retrofit.ApiInterface;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;

/**
 * Admin splash: one image per screen size. Phone/tablet and portrait/landscape
 * pick the matching file; missing sizes fall back without cropping the wrong art.
 */
public final class AppSplashStore {

    private static final String TAG = "AppSplashStore";
    private static final String PREF_URL = "app_splash_image_url";
    private static final String PREF_MAP = "app_splash_slot_urls";
    private static final String LEGACY = "legacy";
    private static final String LEGACY_FILE = "app_splash_cached.png";
    private static final int TABLET_SMALLEST_DP = 600;

    private static final String[] SLOT_ORDER = {
            "mobile_portrait",
            "mobile_landscape",
            "tablet_portrait",
            "tablet_landscape",
            "web_portrait",
            "web_landscape",
            LEGACY
    };

    private AppSplashStore() {
    }

    public static final class Art {
        @Nullable
        public final String localPath;
        @Nullable
        public final String networkUrl;
        /** True when this file was uploaded for the current phone/tablet orientation. */
        public final boolean exactMatch;

        public Art(@Nullable String localPath, @Nullable String networkUrl, boolean exactMatch) {
            this.localPath = localPath;
            this.networkUrl = networkUrl;
            this.exactMatch = exactMatch;
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(localPath) && TextUtils.isEmpty(networkUrl);
        }
    }

    /** Phone vs tablet from smallest width, then portrait vs landscape. */
    public static String slotFor(Context context) {
        Configuration config = context.getResources().getConfiguration();
        boolean landscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
        boolean tablet = config.smallestScreenWidthDp >= TABLET_SMALLEST_DP;
        if (tablet) {
            return landscape ? "tablet_landscape" : "tablet_portrait";
        }
        return landscape ? "mobile_landscape" : "mobile_portrait";
    }

    /** Same orientation first, then the other orientation. Web art is a last resort on Android. */
    private static String[] fallbackOrder(String slot) {
        switch (slot) {
            case "mobile_landscape":
                return new String[]{
                        "mobile_landscape", "tablet_landscape", "web_landscape",
                        "mobile_portrait", "tablet_portrait", "web_portrait"
                };
            case "tablet_portrait":
                return new String[]{
                        "tablet_portrait", "web_portrait", "mobile_portrait",
                        "tablet_landscape", "web_landscape", "mobile_landscape"
                };
            case "tablet_landscape":
                return new String[]{
                        "tablet_landscape", "web_landscape", "mobile_landscape",
                        "tablet_portrait", "web_portrait", "mobile_portrait"
                };
            case "mobile_portrait":
            default:
                return new String[]{
                        "mobile_portrait", "tablet_portrait", "web_portrait",
                        "mobile_landscape", "tablet_landscape", "web_landscape"
                };
        }
    }

    @Nullable
    private static Map<String, String> readUrlMap(Context context) {
        String raw = Common.getSavedUserData(context, PREF_MAP);
        Map<String, String> map = new LinkedHashMap<>();
        if (!TextUtils.isEmpty(raw)) {
            try {
                JSONObject json = new JSONObject(raw);
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String url = clean(json.optString(key, ""));
                    if (url != null) {
                        map.put(key, url);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (map.isEmpty()) {
            String legacy = clean(Common.getSavedUserData(context, PREF_URL));
            if (legacy != null) {
                map.put(LEGACY, legacy);
            }
        }
        return map;
    }

    private static void writeUrlMap(Context context, Map<String, String> urls) {
        SharedPreferences pref = context.getSharedPreferences(Common.SHARED_PREF, 0);
        SharedPreferences.Editor editor = pref.edit();
        if (urls == null || urls.isEmpty()) {
            editor.remove(PREF_MAP);
            editor.remove(PREF_URL);
            editor.apply();
            return;
        }
        JSONObject json = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : urls.entrySet()) {
                String url = clean(entry.getValue());
                if (url != null) {
                    json.put(entry.getKey(), url);
                }
            }
        } catch (Exception ignored) {
        }
        editor.putString(PREF_MAP, json.toString());
        String legacy = clean(urls.get(LEGACY));
        if (legacy == null) {
            editor.remove(PREF_URL);
        } else {
            editor.putString(PREF_URL, legacy);
        }
        editor.apply();
    }

    @Nullable
    private static File cacheFile(Context context, String slot) {
        try {
            String name = LEGACY.equals(slot) ? LEGACY_FILE : "app_splash_" + slot + ".png";
            return new File(context.getFilesDir(), name);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static Art artFor(Context context, String slot, @Nullable String url, boolean exact) {
        File file = cacheFile(context, slot);
        boolean hasFile = file != null && file.exists() && file.length() > 32;
        if (hasFile) {
            return new Art(file.getAbsolutePath(), url, exact);
        }
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        return new Art(null, url, exact);
    }

    @Nullable
    public static Art readCachedArt(Context context) {
        Map<String, String> urls = readUrlMap(context);
        String wanted = slotFor(context);
        for (String slot : fallbackOrder(wanted)) {
            Art art = artFor(context, slot, urls.get(slot), slot.equals(wanted));
            if (art != null && !art.isEmpty()) {
                return art;
            }
        }
        Art legacy = artFor(context, LEGACY, urls.get(LEGACY), false);
        if (legacy != null && !legacy.isEmpty()) {
            return legacy;
        }
        return null;
    }

    public static void clearCache(Context context) {
        writeUrlMap(context, null);
        for (String slot : SLOT_ORDER) {
            File file = cacheFile(context, slot);
            if (file != null && file.exists()) {
                // noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
        File legacyBin = new File(context.getFilesDir(), "app_splash_cached.bin");
        if (legacyBin.exists()) {
            // noinspection ResultOfMethodCallIgnored
            legacyBin.delete();
        }
    }

    private static boolean looksLikeImage(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            return false;
        }
        /* PNG */
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return true;
        }
        /* JPEG */
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return true;
        }
        /* WEBP */
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private static void downloadToCache(Context context, String slot, String url) {
        File file = cacheFile(context, slot);
        if (file == null || TextUtils.isEmpty(url)) {
            return;
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "*/*")
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return;
            }
            ResponseBody body = response.body();
            if (body == null) {
                return;
            }
            byte[] bytes = body.bytes();
            if (!looksLikeImage(bytes)) {
                return;
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(bytes);
                out.flush();
            }
        } catch (IOException e) {
            Log.w(TAG, "downloadToCache failed: " + e.getMessage());
        }
    }

    private static void deleteSlotFile(Context context, String slot) {
        File file = cacheFile(context, slot);
        if (file != null && file.exists()) {
            // noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    /** Online: fetch URLs from API, save prefs + download image bytes. */
    @Nullable
    public static Art fetchAndCache(Context context) {
        try {
            ApiInterface api = Api.getClient(context);
            Call<AppSplashResponse> call = api.getAppSplash();
            retrofit2.Response<AppSplashResponse> response = call.execute();
            if (!response.isSuccessful() || response.body() == null) {
                return readCachedArt(context);
            }
            AppSplashResponse body = response.body();
            if (!body.isSuccess()) {
                return readCachedArt(context);
            }

            Map<String, String> urls = new LinkedHashMap<>();
            if (body.images != null) {
                for (String slot : SLOT_ORDER) {
                    if (LEGACY.equals(slot)) {
                        continue;
                    }
                    String url = body.images.urlFor(slot);
                    if (url != null) {
                        urls.put(slot, url);
                    }
                }
            }
            String legacy = body.normalizedLegacyUrl();
            if (legacy != null) {
                urls.put(LEGACY, legacy);
            }
            if (urls.isEmpty()) {
                clearCache(context);
                return null;
            }

            Map<String, String> previous = readUrlMap(context);
            writeUrlMap(context, urls);
            for (String slot : SLOT_ORDER) {
                if (!urls.containsKey(slot)) {
                    deleteSlotFile(context, slot);
                }
            }
            for (Map.Entry<String, String> entry : urls.entrySet()) {
                boolean unchanged = entry.getValue().equals(previous.get(entry.getKey()));
                File file = cacheFile(context, entry.getKey());
                boolean cached = file != null && file.exists() && file.length() > 32;
                if (unchanged && cached) {
                    continue;
                }
                downloadToCache(context, entry.getKey(), entry.getValue());
            }
            return readCachedArt(context);
        } catch (Exception e) {
            Log.w(TAG, "fetchAndCache failed: " + e.getMessage());
            return readCachedArt(context);
        }
    }

    @Nullable
    private static String clean(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String url = raw.trim();
        return url.isEmpty() ? null : url;
    }
}
