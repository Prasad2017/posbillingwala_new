package com.pos_billingwala.Activity;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pos_billingwala.Extra.AppLanguage;

/**
 * Applies the saved app language to every screen via context wrap (no process restart).
 * Also locks fontScale so system display size settings do not break layouts.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppLanguage.wrap(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreenSizeSmall();
    }

    /**
     * Normalize font scale / density. Always re-applies app language so
     * {@code updateConfiguration} does not wipe the locale from {@link AppLanguage#wrap}.
     */
    @SuppressWarnings("deprecation")
    public void setScreenSizeSmall() {
        Configuration configuration = getResources().getConfiguration();
        configuration.fontScale = 1f; // 0.85 small, 1 normal, 1.15 big etc
        AppLanguage.preserveLocaleOnConfig(this, configuration);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        configuration.densityDpi = (int) getResources().getDisplayMetrics().xdpi;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }
}
