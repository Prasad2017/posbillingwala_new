package com.pos_billingwala.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.TextUtils;
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
import com.pos_billingwala.BuildConfig;
import com.pos_billingwala.Extra.AppSplashStore;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.NetworkToOffline.CloudSyncNav;
import com.pos_billingwala.NetworkToOffline.OfflineNetworkData;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivitySplashScreenBinding;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * App logo while resolving; admin splash when available (3s hold).
 * No blank white handoff.
 */
@SuppressLint({"UseCompatLoadingForDrawables, NonConstantResourceId, CustomSplashScreen"})
public class SplashScreen extends BaseActivity {

    private static final long DYNAMIC_SPLASH_HOLD_MS = 3000L;

    ActivitySplashScreenBinding binding;
    OfflineNetworkData offlineNetworkData;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService splashExecutor = Executors.newSingleThreadExecutor();
    private boolean navigated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        showAppLogo();

        if (getIntent() != null && (getIntent().getBooleanExtra(CloudSyncNav.EXTRA_OPEN, false)
                || CloudSyncNav.ACTION_OPEN.equals(getIntent().getAction()))) {
            CloudSyncNav.markPending(this);
        }

        startSplashSequence();
    }

    private void showAppLogo() {
        binding.logoIcon.setVisibility(View.VISIBLE);
        binding.logoIcon.setImageResource(R.drawable.app_logo);
        binding.dynamicSplashImage.setVisibility(View.GONE);
        binding.dynamicSplashImage.setImageDrawable(null);
    }

    private void showDynamicArt(@Nullable AppSplashStore.Art art) {
        if (art == null || art.isEmpty()) {
            showAppLogo();
            return;
        }
        binding.logoIcon.setVisibility(View.GONE);
        binding.dynamicSplashImage.setVisibility(View.VISIBLE);

        Callback fallbackToLogo = new Callback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(Exception e) {
                if (!isFinishing()) {
                    showAppLogo();
                }
            }
        };

        if (!TextUtils.isEmpty(art.networkUrl)) {
            Picasso.get()
                    .load(art.networkUrl)
                    .fit()
                    .centerCrop()
                    .into(binding.dynamicSplashImage, new Callback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(Exception e) {
                            if (!TextUtils.isEmpty(art.localPath)) {
                                File file = new File(art.localPath);
                                if (file.exists()) {
                                    Picasso.get()
                                            .load(file)
                                            .fit()
                                            .centerCrop()
                                            .into(binding.dynamicSplashImage, fallbackToLogo);
                                    return;
                                }
                            }
                            fallbackToLogo.onError(e);
                        }
                    });
            return;
        }

        if (!TextUtils.isEmpty(art.localPath)) {
            File file = new File(art.localPath);
            if (file.exists()) {
                Picasso.get()
                        .load(file)
                        .fit()
                        .centerCrop()
                        .into(binding.dynamicSplashImage, fallbackToLogo);
                return;
            }
        }
        showAppLogo();
    }

    /**
     * 1) App logo while loading
     * 2) Cached splash if any
     * 3) Online → fetch + refresh cache
     * 4) Hold dynamic 3s then continue
     */
    private void startSplashSequence() {
        final boolean online = DetectConnection.checkInternetConnection(this);
        splashExecutor.execute(() -> {
            final AppSplashStore.Art cached = AppSplashStore.readCachedArt(SplashScreen.this);

            mainHandler.post(() -> {
                if (isFinishing() || navigated) {
                    return;
                }
                if (cached != null && !cached.isEmpty()) {
                    showDynamicArt(cached);
                }

                splashExecutor.execute(() -> {
                    AppSplashStore.Art fresh = null;
                    if (online) {
                        fresh = AppSplashStore.fetchAndCache(SplashScreen.this);
                    }
                    final AppSplashStore.Art result = fresh;
                    final boolean hasDynamic;
                    if (online) {
                        hasDynamic = result != null && !result.isEmpty();
                    } else {
                        hasDynamic = cached != null && !cached.isEmpty();
                    }

                    mainHandler.post(() -> {
                        if (isFinishing() || navigated) {
                            return;
                        }
                        if (online) {
                            if (hasDynamic) {
                                showDynamicArt(result);
                            } else {
                                showAppLogo();
                            }
                        }
                        long hold = hasDynamic ? DYNAMIC_SPLASH_HOLD_MS : 0L;
                        mainHandler.postDelayed(this::finishSplashAndContinue, hold);
                    });
                });
            });
        });
    }

    private void finishSplashAndContinue() {
        if (navigated || isFinishing()) {
            return;
        }
        if (BuildConfig.DEBUG) {
            goNextScreen();
        } else {
            checkAppUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        splashExecutor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
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
                            this::goNextScreen);
                }
            } else {
                goNextScreen();
            }
        }).addOnFailureListener(e -> {
            Log.e("TAG", "checkAppUpdates: " + e.getMessage());
            goNextScreen();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 100) {
                goNextScreen();
            }
        }
    }

    /** @deprecated use {@link #goNextScreen()} — kept for any external callers */
    public void moveNext() {
        goNextScreen();
    }

    public void goNextScreen() {
        if (navigated || isFinishing()) {
            return;
        }
        navigated = true;

        if (Common.getSavedUserData(SplashScreen.this, "firstLogin") != null
                && Common.getSavedUserData(SplashScreen.this, "firstLogin").equalsIgnoreCase("firstLogin")) {
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
    }
}
