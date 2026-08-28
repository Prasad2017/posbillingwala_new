package com.pos_billingwala.Activity;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pos_billingwala.Extra.AppLanguage;
import com.pos_billingwala.Extra.DisplayScale;
import com.pos_billingwala.Extra.ScreenshotConfig;

/**
 * Applies saved app language and locks UI density on every screen.
 */
public abstract class BaseActivity extends AppCompatActivity implements DisplayScale.ResourcesHost {

    @Nullable
    private Resources adjustedResources;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppLanguage.wrap(DisplayScale.wrap(newBase)));
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ScreenshotConfig.apply(this);
        super.onCreate(savedInstanceState);
        ScreenshotConfig.apply(this);
        DisplayScale.refresh(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScreenshotConfig.apply(this);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        clearAdjustedResources();
        DisplayScale.refresh(this);
    }
}
