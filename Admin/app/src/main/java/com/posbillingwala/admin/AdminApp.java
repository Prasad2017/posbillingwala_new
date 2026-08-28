package com.posbillingwala.admin;

import android.app.Application;

import com.posbillingwala.admin.Extra.ScreenshotConfig;

public class AdminApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ScreenshotConfig.install(this);
    }
}
