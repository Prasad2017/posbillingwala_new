package com.pos_billingwala;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import com.pos_billingwala.Extra.AppLanguage;
import com.pos_billingwala.Extra.DisplayScale;
import com.pos_billingwala.Extra.Observability;
import com.pos_billingwala.Extra.ScreenshotConfig;

/**
 * Application entry for production monitoring.
 */
public class PosBillingWalaApp extends Application implements DisplayScale.ResourcesHost {

    private Resources adjustedResources;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(AppLanguage.wrap(DisplayScale.wrap(base)));
    }

    @Override
    public Resources getResources() {
        if (adjustedResources != null) {
            return adjustedResources;
        }
        adjustedResources = DisplayScale.adjustResources(this, super.getResources());
        return adjustedResources;
    }

    @Override
    public void clearAdjustedResources() {
        adjustedResources = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLanguage.applyStored(this);
        Observability.init(this);
        ScreenshotConfig.install(this);
    }
}
