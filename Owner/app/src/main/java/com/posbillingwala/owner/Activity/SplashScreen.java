package com.posbillingwala.owner.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.databinding.ActivitySplashScreenBinding;


@SuppressLint("CustomSplashScreen")
public class SplashScreen extends AppCompatActivity {

    public static final int SPLASH_TIME_OUT = 2000;
    public ActivitySplashScreenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        checkAppUpdates();

    }


    @SuppressLint("UseCompatLoadingForDrawables")
    public void checkAppUpdates() {

        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (!(SplashScreen.this.isFinishing())) {
                    String strMessage = "Please update our <b> POS " + getResources().getString(R.string.app_name) + "</b> app to new version to continue.";
                    BottomSheetUi.showAction(
                            SplashScreen.this,
                            "New version available",
                            Html.fromHtml(strMessage),
                            "Update",
                            "Cancel",
                            R.mipmap.ic_launcher,
                            false,
                            null,
                            this::moveNext);
                }
            } else {
                moveNext();
            }
        }).addOnFailureListener(e -> {
            Log.e("TAG", "checkAppUpdates: " + e.getMessage());
            moveNext();
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 100) {
                moveNext();
            }
        }
    }

    public void moveNext() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(SplashScreen.this, Login.class);
                startActivity(i);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}
