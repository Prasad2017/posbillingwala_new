package com.posbillingwala.owner.Utils;

import android.content.Context;
import android.os.Environment;

import com.posbillingwala.owner.BuildConfig;
import com.posbillingwala.owner.Extra.Common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class CatalogFileHelper {

    private CatalogFileHelper() {
    }

    public interface DownloadCallback {
        void onSuccess(File savedFile);

        void onError(String message);
    }

    public static void downloadCatalogFile(Context context, String endpointQuery, String outputFileName, DownloadCallback callback) {
        new Thread(() -> {
            try {
                String url = BuildConfig.API_BASE_URL + endpointQuery;
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.MINUTES)
                        .readTimeout(5, TimeUnit.MINUTES)
                        .writeTimeout(5, TimeUnit.MINUTES)
                        .build();

                Request.Builder requestBuilder = new Request.Builder().url(url).get();
                String token = Common.getSavedUserData(context, "authToken");
                if (token != null && !token.isEmpty()) {
                    requestBuilder.header("Authorization", "Bearer " + token);
                }

                Response response = client.newCall(requestBuilder.build()).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    if (callback != null) {
                        callback.onError("Download failed (" + response.code() + ")");
                    }
                    return;
                }

                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "POS Billingwala");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File outFile = new File(dir, outputFileName);

                InputStream inputStream = response.body().byteStream();
                FileOutputStream outputStream = new FileOutputStream(outFile);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();

                if (callback != null) {
                    callback.onSuccess(outFile);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Download failed");
                }
            }
        }).start();
    }
}
