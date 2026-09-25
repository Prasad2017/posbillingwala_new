package com.pos_billingwala.Extra;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.pos_billingwala.Model.AppSplashResponse;
import com.pos_billingwala.Retrofit.Api;
import com.pos_billingwala.Retrofit.ApiInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;

/**
 * Admin splash: first load from net → file cache; later show cache;
 * when online again refresh from server and update cache.
 */
public final class AppSplashStore {

    private static final String TAG = "AppSplashStore";
    private static final String PREF_URL = "app_splash_image_url";
    private static final String CACHE_FILE = "app_splash_cached.png";

    private AppSplashStore() {
    }

    public static final class Art {
        @Nullable
        public final String localPath;
        @Nullable
        public final String networkUrl;

        public Art(@Nullable String localPath, @Nullable String networkUrl) {
            this.localPath = localPath;
            this.networkUrl = networkUrl;
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(localPath) && TextUtils.isEmpty(networkUrl);
        }
    }

    @Nullable
    public static String readCachedUrl(Context context) {
        String url = Common.getSavedUserData(context, PREF_URL);
        if (url == null) {
            return null;
        }
        url = url.trim();
        return url.isEmpty() ? null : url;
    }

    public static void writeCachedUrl(Context context, @Nullable String url) {
        SharedPreferences pref = context.getSharedPreferences(Common.SHARED_PREF, 0);
        SharedPreferences.Editor editor = pref.edit();
        if (TextUtils.isEmpty(url)) {
            editor.remove(PREF_URL);
        } else {
            editor.putString(PREF_URL, url.trim());
        }
        editor.apply();
    }

    @Nullable
    public static File cacheFile(Context context) {
        try {
            return new File(context.getFilesDir(), CACHE_FILE);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static Art readCachedArt(Context context) {
        String url = readCachedUrl(context);
        File file = cacheFile(context);
        if (file != null && file.exists() && file.length() > 32) {
            return new Art(file.getAbsolutePath(), url);
        }
        if (url == null) {
            return null;
        }
        return new Art(null, url);
    }

    public static void clearCache(Context context) {
        writeCachedUrl(context, null);
        File file = cacheFile(context);
        if (file != null && file.exists()) {
            // noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        File legacy = new File(context.getFilesDir(), "app_splash_cached.bin");
        if (legacy.exists()) {
            // noinspection ResultOfMethodCallIgnored
            legacy.delete();
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

    public static void downloadToCache(Context context, String url) {
        File file = cacheFile(context);
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

    /** Online: fetch URL from API, save prefs + download image bytes. */
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
            String url = body.normalizedImageUrl();
            if (url == null) {
                clearCache(context);
                return null;
            }
            writeCachedUrl(context, url);
            downloadToCache(context, url);
            return readCachedArt(context);
        } catch (Exception e) {
            Log.w(TAG, "fetchAndCache failed: " + e.getMessage());
            return readCachedArt(context);
        }
    }
}
