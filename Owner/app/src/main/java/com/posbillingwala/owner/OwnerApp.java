package com.posbillingwala.owner;

import android.app.Application;

import com.posbillingwala.owner.Extra.ScreenshotConfig;

public class OwnerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ScreenshotConfig.install(this);
    }
}
