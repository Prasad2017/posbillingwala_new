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

    /** Preferred base URL (HTTPS). Fallback interceptor can switch to HTTP. */
    public static String BASE_URL = BuildConfig.API_BASE_URL;
    private static Retrofit retrofit = null;
    private static OkHttpClient sharedClient = null;

    public static OkHttpClient getOkHttpClient(Context context) {
        if (sharedClient == null) {
            final Context appContext = context.getApplicationContext();

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

            File cacheDirectory = new File(appContext.getCacheDir(), "http_cache");
            Cache cache = new Cache(cacheDirectory, 10 * 1024 * 1024);

            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .callTimeout(180, TimeUnit.SECONDS)
                    .cache(cache)
                    .retryOnConnectionFailure(true);

            HttpHttpsSupport.applyTo(httpClientBuilder);

            httpClientBuilder
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        String token = Common.getSavedUserData(appContext, "authToken");
                        if (token != null && !token.isEmpty()) {
                            Request authenticated = original.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                            return chain.proceed(authenticated);
                        }
                        return chain.proceed(original);
                    })
                    .addInterceptor(new ApiFailureInterceptor())
                    // After failure logger so successful HTTP fallback is not treated as failure
                    .addInterceptor(new HttpHttpsFallbackInterceptor());

            if (BuildConfig.DEBUG) {
                httpClientBuilder.addInterceptor(logging);
            }

            sharedClient = httpClientBuilder.build();
        }
        return sharedClient;
    }

    public static ApiInterface getClient(Context context) {

        if (retrofit == null) {
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(getOkHttpClient(context))
                    .build();
        }

        return retrofit.create(ApiInterface.class);
    }
}
