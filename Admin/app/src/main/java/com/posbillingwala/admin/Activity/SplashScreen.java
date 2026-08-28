package com.posbillingwala.admin.Activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;

import com.posbillingwala.admin.Extra.AuthTokens;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.ActivitySplashScreenBinding;


@SuppressLint({"SetTextI18n, NonConstantResourceId, ResourceType", "CustomSplashScreen"})
public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 2200;
    ActivitySplashScreenBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        Api.bindContext(this);
        binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    @Override
    protected void onStart() {
        super.onStart();
        moveNext();
    }

    private void moveNext() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                if (AuthTokens.hasValidSession(SplashScreen.this)) {
                    startActivity(new Intent(SplashScreen.this, MainActivity.class));
                } else {
                    if (AuthTokens.isTokenExpired(SplashScreen.this)) {
                        AuthTokens.clear(SplashScreen.this);
                    }
                    startActivity(new Intent(SplashScreen.this, Login.class));
                }
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}
