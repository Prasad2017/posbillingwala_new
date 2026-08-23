package com.pos_billingwala.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.NetworkToOffline.OfflineNetworkData;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivitySplashScreenBinding;

@SuppressLint({"UseCompatLoadingForDrawables, NonConstantResourceId, CustomSplashScreen"})
public class SplashScreen extends AppCompatActivity {

    public static final int SPLASH_TIME_OUT = 2000;
    ActivitySplashScreenBinding binding;
    OfflineNetworkData offlineNetworkData;

    public void setScreenSizeSmall() {
        Configuration configuration = getResources().getConfiguration();
        configuration.fontScale = (float) 1; //0.85 small size, 1 normal size, 1,15 big etc
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        configuration.densityDpi = (int) getResources().getDisplayMetrics().xdpi;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        View view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here
        setContentView(view); //view is set by view binding

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setScreenSizeSmall();

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
                    new MaterialAlertDialogBuilder(SplashScreen.this, R.style.ThemeDialog)
                            .setIcon(getResources().getDrawable(R.mipmap.ic_launcher))
                            .setTitle("New version available")
                            .setCancelable(false)
                            .setMessage(Html.fromHtml(strMessage))
                            .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Toast.makeText(SplashScreen.this, "Data uploading on server", Toast.LENGTH_SHORT).show();
                                    offlineNetworkData = new OfflineNetworkData(SplashScreen.this, "Update");
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    moveNext();
                                }
                            }).show();
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
                        startActivity(i);
                        finish();
                    } else {
                        Intent i = new Intent(SplashScreen.this, Login.class);
                        startActivity(i);
                        finish();
                    }
                } else {
                    Intent i = new Intent(SplashScreen.this, Login.class);
                    startActivity(i);
                    finish();
                }

            }
        }, SPLASH_TIME_OUT);

    }


}