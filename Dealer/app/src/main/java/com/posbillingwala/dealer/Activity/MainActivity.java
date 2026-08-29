package com.posbillingwala.dealer.Activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.AppBarLayout;
import com.posbillingwala.dealer.Extra.BottomSheetUi;
import com.posbillingwala.dealer.AppUpdate.UpdateManager;
import com.posbillingwala.dealer.AppUpdate.UpdateManagerConstant;
import com.posbillingwala.dealer.Extra.AuthTokens;
import com.posbillingwala.dealer.Extra.Common;
import com.posbillingwala.dealer.Extra.ScreenshotConfig;
import com.posbillingwala.dealer.Fragment.AllCustomerList;
import com.posbillingwala.dealer.Fragment.DealerProfile;
import com.posbillingwala.dealer.Fragment.Home;
import com.posbillingwala.dealer.Fragment.ProductExport;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;

import java.io.File;


@SuppressLint("SetTextI18n, NonConstantResourceId, ResourceType, UseCompatLoadingForDrawables, StaticFieldLeak")
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static ImageView back;
    public static DrawerLayout drawerLayout;
    public static TextView title;
    public static AppBarLayout toolbarContainer;
    public static LinearLayout bottomNavigationLayout;
    public static String userId, currency = "₹. ";
    public static LinearLayout homeLinearLayout, customerLinearLayout, customerProductLinearLayout, accountLayout, accountLogoutLayout;
    boolean doubleBackToExitPressedOnce = false;
    // Declare the UpdateManager
    UpdateManager mUpdateManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenshotConfig.apply(this);
        Api.bindContext(this);
        setContentView(R.layout.activity_main);
        ScreenshotConfig.apply(this);
        initViews();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {

            userId = Common.getSavedUserData(MainActivity.this, "userId");

        } catch (Exception e) {
            e.printStackTrace();
        }

        loadFragment(new Home(), false);

        checkUpdateApp();

    }

    @Override
    protected void onResume() {
        super.onResume();
        ScreenshotConfig.apply(this);
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbarContainer = findViewById(R.id.toolbar_container);
        bottomNavigationLayout = findViewById(R.id.bottomNavigationLayout);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        homeLinearLayout = findViewById(R.id.homeLinearLayout);
        customerLinearLayout = findViewById(R.id.customerLinearLayout);
        customerProductLinearLayout = findViewById(R.id.customerProductLinearLayout);
        accountLayout = findViewById(R.id.accountLayout);
        accountLogoutLayout = findViewById(R.id.accountLogoutLayout);

        //Click Listener
        back.setOnClickListener(this);
        homeLinearLayout.setOnClickListener(this);
        customerLinearLayout.setOnClickListener(this);
        customerProductLinearLayout.setOnClickListener(this);
        accountLayout.setOnClickListener(this);
        accountLogoutLayout.setOnClickListener(this);

    }

    private void checkUpdateApp() {

        // Initialize the Update Manager with the Activity and the Update Mode
        mUpdateManager = UpdateManager.Builder(this);
        mUpdateManager.addUpdateInfoListener(new UpdateManager.UpdateInfoListener() {
            @Override
            public void onReceiveVersionCode(final int code) {

            }

            @Override
            public void onReceiveStalenessDays(final int days) {

            }
        });

        mUpdateManager.addFlexibleUpdateDownloadListener(new UpdateManager.FlexibleUpdateDownloadListener() {
            @Override
            public void onDownloadProgress(final long bytesDownloaded, final long totalBytes) {

            }
        });

        callFlexibleUpdate();

    }


    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.back) {
            removeCurrentFragmentAndMoveBack();
        } else if (id == R.id.homeLinearLayout) {
            removeCurrentFragmentAndMoveBack();
            loadFragment(new Home(), false);
        } else if (id == R.id.customerLinearLayout) {
            removeCurrentFragmentAndMoveBack();
            loadFragment(new AllCustomerList(), false);
        } else if (id == R.id.customerProductLinearLayout) {
            removeCurrentFragmentAndMoveBack();
            loadFragment(new ProductExport(), false);
        } else if (id == R.id.accountLayout) {
            removeCurrentFragmentAndMoveBack();
            loadFragment(new DealerProfile(), false);
        } else if (id == R.id.accountLogoutLayout) {
            logout();
        }
    }

    private void logout() {

        BottomSheetUi.showConfirm(MainActivity.this, "Logout", "Do you want to logout from application?",
                "YES", "NO", false, () -> {
                    AuthTokens.clear(MainActivity.this);
                    Common.saveUserData(MainActivity.this, "userId", "");

                    File file1 = new File("data/data/" + getPackageName() + "/shared_prefs/user.xml");
                    if (file1.exists()) {
                        file1.delete();
                    }

                    Intent intent = new Intent(MainActivity.this, Login.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });

    }

    @Override
    public void onBackPressed() {
        // double press to exit
        if (back.getVisibility() == View.GONE) {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }
        } else {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press back once more to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);

    }

    public void lockUnlockDrawer(int lockMode) {
        drawerLayout.setDrawerLockMode(lockMode);
        if (lockMode == DrawerLayout.LOCK_MODE_LOCKED_CLOSED) {
            back.setVisibility(View.VISIBLE);
            bottomNavigationLayout.setVisibility(View.GONE);
        } else {
            back.setVisibility(View.GONE);
            bottomNavigationLayout.setVisibility(View.VISIBLE);
        }

    }

    public void removeCurrentFragmentAndMoveBack() {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadFragment(Fragment fragment, Boolean bool) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frameLayout, fragment);
        if (bool) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    public void callFlexibleUpdate() {
        // Start a Flexible Update
        mUpdateManager.mode(UpdateManagerConstant.FLEXIBLE).start();
    }

}