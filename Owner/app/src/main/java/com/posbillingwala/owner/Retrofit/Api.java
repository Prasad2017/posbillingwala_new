package com.posbillingwala.owner.Retrofit;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.posbillingwala.owner.BuildConfig;
import com.posbillingwala.owner.Extra.Common;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Api {

    public static String BASE_URL = BuildConfig.API_BASE_URL;
    private static Context appContext;
    private static Retrofit retrofit = null;

    public static void bindContext(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static ApiInterface getClient() {

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        if (retrofit == null) {

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                    .connectTimeout(50, TimeUnit.MINUTES)
                    .writeTimeout(50, TimeUnit.MINUTES)
                    .readTimeout(50, TimeUnit.MINUTES)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        if (appContext != null) {
                            String token = Common.getSavedUserData(appContext, "authToken");
                            if (token != null && !token.isEmpty()) {
                                Request authenticated = original.newBuilder()
                                        .header("Authorization", "Bearer " + token)
                                        .build();
                                return chain.proceed(authenticated);
                            }
                        }
                        return chain.proceed(original);
                    })
                    .addInterceptor(logging)
                    .retryOnConnectionFailure(true);

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(httpClientBuilder.build())
                    .build();

        }

        return retrofit.create(ApiInterface.class);

    }

}
