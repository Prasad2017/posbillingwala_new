package com.posbillingwala.admin;

import android.app.Application;

import com.posbillingwala.admin.Extra.FcmTokenManager;
import com.posbillingwala.admin.Extra.PushNotificationHelper;
import com.posbillingwala.admin.Extra.ScreenshotConfig;
import com.posbillingwala.admin.Retrofit.Api;

public class AdminApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ScreenshotConfig.install(this);
        Api.bindContext(this);
        PushNotificationHelper.ensureChannel(this);
        FcmTokenManager.registerIfLoggedIn(this);
    }
}
