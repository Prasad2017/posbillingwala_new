package com.posbillingwala.dealer;

import android.app.Application;

import com.posbillingwala.dealer.Extra.ScreenshotConfig;
import com.posbillingwala.dealer.Retrofit.Api;

public class DealerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ScreenshotConfig.install(this);
        Api.bindContext(this);
    }
}
