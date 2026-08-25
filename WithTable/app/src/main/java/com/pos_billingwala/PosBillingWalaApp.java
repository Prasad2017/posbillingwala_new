package com.pos_billingwala;

import android.app.Application;

import com.pos_billingwala.Extra.AppLanguage;
import com.pos_billingwala.Extra.Observability;

/**
 * Application entry for production monitoring.
 * Installs a global crash handler so ANY uncaught crash is logged clearly
 * (UI, database, printing, sync, API, OOM, etc.) — not only API failures.
 */
public class PosBillingWalaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppLanguage.applyStored(this);
        Observability.init(this);
    }
}
