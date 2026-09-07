package com.pos_billingwala;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.pos_billingwala.Extra.AppLanguage;
import com.pos_billingwala.Extra.DisplayScale;
import com.pos_billingwala.Extra.FcmTokenManager;
import com.pos_billingwala.Extra.Observability;
import com.pos_billingwala.Extra.PushNotificationHelper;
import com.pos_billingwala.Extra.ScreenshotConfig;
import com.pos_billingwala.Print.PrinterConnectionHelper;

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
        com.pos_billingwala.Extra.AppContexts.init(this);
        Observability.init(this);
        ScreenshotConfig.install(this);
        PrinterConnectionHelper.initializeApp(this);
        PushNotificationHelper.ensureChannel(this);
        FcmTokenManager.registerIfLoggedIn(this);
        // Ensure we keep using the Firebase project from google-services.json (pos-billingwala).
        Log.d("PosBillingWalaApp", "FCM channel ready; token register if logged in");
    }

    @Override
    public void onTerminate() {
        PrinterConnectionHelper.shutdownApp(this);
        super.onTerminate();
    }
}
