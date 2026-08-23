package com.pos_billingwala.Retrofit;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pos_billingwala.BuildConfig;
import com.pos_billingwala.Extra.Common;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Api {

    public static String BASE_URL = BuildConfig.API_BASE_URL;
    private static Retrofit retrofit = null;

    public static ApiInterface getClient(Context context) {

        if (retrofit == null) {

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // Never log request/response bodies in release (licence keys, bills, device ids)
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            File cacheDirectory = new File(context.getCacheDir(), "http_cache");
            Cache cache = new Cache(cacheDirectory, 10 * 1024 * 1024); // 10 MB cache

            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .cache(cache)
                    .retryOnConnectionFailure(true)
                    .addInterceptor(new ApiFailureInterceptor())
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        String token = Common.getSavedUserData(context, "authToken");
                        if (token != null && !token.isEmpty()) {
                            Request authenticated = original.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                            return chain.proceed(authenticated);
                        }
                        return chain.proceed(original);
                    });

            if (BuildConfig.DEBUG) {
                httpClientBuilder.addInterceptor(logging);
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(httpClientBuilder.build())
                    .build();
        }

        return retrofit.create(ApiInterface.class);
    }
}
