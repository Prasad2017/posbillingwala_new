package com.pos_billingwala;

import android.app.Application;

import com.pos_billingwala.Extra.AppLanguage;
import com.pos_billingwala.Extra.Observability;
import com.pos_billingwala.Retrofit.HttpHttpsSupport;

/**
 * Application entry for production crash + ANR + performance monitoring.
 * Does not change billing, sync, or licence behavior.
 */
public class PosBillingWalaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppLanguage.applyStored(this);
        Observability.init(this);
        // Allow HTTPS image/API loads despite self-signed / mismatched server cert
        HttpHttpsSupport.installPlatformHostnameVerifier();
    }
}
