package com.pos_billingwala;

import android.app.Application;

import com.pos_billingwala.Extra.Observability;

/**
 * Application entry for production crash + ANR + performance monitoring.
 * Does not change billing, sync, or licence behavior.
 */
public class PosBillingWalaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Observability.init(this);
    }
}
