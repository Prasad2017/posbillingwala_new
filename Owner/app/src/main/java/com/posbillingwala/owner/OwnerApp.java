package com.posbillingwala.owner;

import android.app.Application;

import com.posbillingwala.owner.Extra.FcmTokenManager;
import com.posbillingwala.owner.Extra.PushNotificationHelper;
import com.posbillingwala.owner.Extra.ScreenshotConfig;

public class OwnerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ScreenshotConfig.install(this);
        com.posbillingwala.owner.Retrofit.Api.bindContext(this);
        PushNotificationHelper.ensureChannel(this);
        FcmTokenManager.registerIfLoggedIn(this);
    }
}
