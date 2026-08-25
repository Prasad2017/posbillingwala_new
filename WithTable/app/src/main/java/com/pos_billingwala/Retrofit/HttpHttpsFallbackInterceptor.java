package com.pos_billingwala.Retrofit;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * If a call to the POS Billingwala host fails on http or https, retries once
 * with the other scheme so the app works when either protocol is blocked or
 * the server SSL cert is unavailable.
 */
public final class HttpHttpsFallbackInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        try {
            return chain.proceed(request);
        } catch (IOException primaryError) {
            HttpUrl url = request.url();
            if (!HttpHttpsSupport.isOurHost(url.host())) {
                throw primaryError;
            }

            String scheme = url.scheme();
            String altScheme;
            if ("https".equalsIgnoreCase(scheme)) {
                altScheme = "http";
            } else if ("http".equalsIgnoreCase(scheme)) {
                altScheme = "https";
            } else {
                throw primaryError;
            }

            Request altRequest = request.newBuilder()
                    .url(url.newBuilder().scheme(altScheme).build())
                    .build();
            try {
                return chain.proceed(altRequest);
            } catch (IOException ignored) {
                throw primaryError;
            }
        }
    }
}
