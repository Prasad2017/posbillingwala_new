package com.posbillingwala.dealer;

import android.app.Application;

import com.posbillingwala.dealer.Extra.ScreenshotConfig;

public class DealerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ScreenshotConfig.install(this);
    }
}
