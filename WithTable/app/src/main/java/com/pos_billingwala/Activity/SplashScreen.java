package com.pos_billingwala.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.NetworkToOffline.CloudSyncNav;
import com.pos_billingwala.NetworkToOffline.OfflineNetworkData;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivitySplashScreenBinding;

@SuppressLint({"UseCompatLoadingForDrawables, NonConstantResourceId, CustomSplashScreen"})
public class SplashScreen extends BaseActivity {

    public static final int SPLASH_TIME_OUT = 800;
    ActivitySplashScreenBinding binding;
    OfflineNetworkData offlineNetworkData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        if (getIntent() != null && (getIntent().getBooleanExtra(CloudSyncNav.EXTRA_OPEN, false)
                || CloudSyncNav.ACTION_OPEN.equals(getIntent().getAction()))) {
            CloudSyncNav.markPending(this);
        }
        checkAppUpdates();

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    public void checkAppUpdates() {

        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (!(SplashScreen.this.isFinishing())) {
                    String strMessage = "Please update our <b> POS " + getResources().getString(R.string.app_name) + "</b> app to new version to continue. Before update our app please upload your data on server. We ae not responsible for losing your data.";
                    BottomSheetUi.showAction(
                            SplashScreen.this,
                            getString(R.string.toast_new_version_available),
                            Html.fromHtml(strMessage),
                            "Update",
                            "Cancel",
                            R.mipmap.ic_launcher,
                            false,
                            () -> {
                                Toast.makeText(SplashScreen.this, getString(R.string.toast_data_uploading_on_server), Toast.LENGTH_SHORT).show();
                                offlineNetworkData = new OfflineNetworkData(SplashScreen.this, "Update");
                            },
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

                if (Common.getSavedUserData(SplashScreen.this, "firstLogin") != null) {
                    if (Common.getSavedUserData(SplashScreen.this, "firstLogin").equalsIgnoreCase("firstLogin")) {
                        Intent i = new Intent(SplashScreen.this, LoginMPin.class);
                        CloudSyncNav.copyOpenFlag(getIntent(), i);
                        startActivity(i);
                        finish();
                    } else {
                        Intent i = new Intent(SplashScreen.this, Login.class);
                        CloudSyncNav.copyOpenFlag(getIntent(), i);
                        startActivity(i);
                        finish();
                    }
                } else {
                    Intent i = new Intent(SplashScreen.this, Login.class);
                    CloudSyncNav.copyOpenFlag(getIntent(), i);
                    startActivity(i);
                    finish();
                }

            }
        }, SPLASH_TIME_OUT);

    }


}