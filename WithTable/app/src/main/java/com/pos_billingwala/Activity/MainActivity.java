package com.pos_billingwala.Activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.CursorWindow;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.pos_billingwala.Extra.AppLanguage;
import com.pos_billingwala.Extra.BranchSession;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.Observability;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Fragment.Home;
import com.pos_billingwala.Fragment.InvoiceCompanyTable;
import com.pos_billingwala.Fragment.InvoiceMess;
import com.pos_billingwala.Fragment.InvoiceTakeAway;
import com.pos_billingwala.Fragment.UserSetting;
import com.pos_billingwala.R;

import java.lang.reflect.Field;

public class MainActivity extends BaseActivity {

    public static DrawerLayout drawerLayout;
    public static String userId, ownerId, organizationId, branchId, branchLabel, deviceId, userName, shopName, shopImage, LicenceKey, LicenceKeyRegDate,
            LicenceKeyExpireDate, currencyName, invoiceRunningStatus = "", cartOrderStatus = "",
            fastBilling, takeAway, dineIn, mess, reportPin, totalSaleData, todaySaleData;

    private static final long DOUBLE_BACK_EXIT_INTERVAL_MS = 2000L;
    private long lastBackPressAtHomeMs = 0L;
    @SuppressLint("DiscouragedPrivateApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        try {
            Field field = CursorWindow.class.getDeclaredField("sCursorWindowSize");
            field.setAccessible(true);
            field.set(null, 1024 * 1024 * 50); // Set to 50MB
        } catch (Exception e) {
            e.printStackTrace();
        }

        initViews();
        registerFragmentScreenTracking();
        setupBackPressHandler();
        userId = Common.getSavedUserData(this, "userId");
        ownerId = Common.getSavedUserData(this, "ownerId");
        BranchSession.loadFromPreferences(this);
        userName = Common.getSavedUserData(this, "userName");
        shopName = Common.getSavedUserData(this, "shopName");
        shopImage = Common.getSavedUserData(this, "shopImage");
        fastBilling = Common.getSavedUserData(this, "fastBilling");
        takeAway = Common.getSavedUserData(this, "takeAway");
        dineIn = Common.getSavedUserData(this, "dineIn");
        mess = Common.getSavedUserData(this, "mess");
        totalSaleData = Common.getSavedUserData(this, "totalSaleData");
        todaySaleData = Common.getSavedUserData(this, "todaySaleData");
        LicenceKey = Common.getSavedUserData(this, "LicenceKey");
        LicenceKeyRegDate = Common.getSavedUserData(this, "LicenceKeyRegDate");
        LicenceKeyExpireDate = Common.getSavedUserData(this, "LicenceKeyExpireDate");
        Observability.setUserContext(userId, LicenceKey);
        reportPin = Common.getSavedUserData(this, "reportPin");
        currencyName = Common.getSavedUserData(this, "currencyName");
        if (currencyName.equalsIgnoreCase("")) {
            currencyName = "\u20B9";
        } else {
            currencyName = currencyName;
        }

        try {
            Intent intent = getIntent();
            if (intent != null) {
                invoiceRunningStatus = intent.getStringExtra("invoiceRunningStatus") != null ? intent.getStringExtra("invoiceRunningStatus") : "";
                cartOrderStatus = intent.getStringExtra("cartOrderStatus");
            } else {
                invoiceRunningStatus = "";
                cartOrderStatus = "";
            }

            if (AppLanguage.consumeReopenUserSetting(this)) {
                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                loadFragment(new UserSetting(), false);
            } else if (savedInstanceState == null) {
                if (invoiceRunningStatus.equalsIgnoreCase("")) {
                    loadFragment(new Home(), false);
                } else if (cartOrderStatus == null || cartOrderStatus.equalsIgnoreCase("")) {
                    openAboveHome(new CreatePos());
                } else if (cartOrderStatus.equalsIgnoreCase("table_wise")) {
                    openAboveHome(new InvoiceCompanyTable());
                } else if (cartOrderStatus.equalsIgnoreCase("take_away")) {
                    openAboveHome(new InvoiceTakeAway());
                } else if (cartOrderStatus.equalsIgnoreCase("fast_billing")) {
                    openAboveHome(new CreatePos());
                } else if (cartOrderStatus.equalsIgnoreCase("mess")) {
                    openAboveHome(new InvoiceMess());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (savedInstanceState == null) {
                loadFragment(new Home(), false);
            }
        }
    }

    /** Soft language change: reinflate Settings with the new locale (no Activity recreate). */
    @SuppressWarnings("deprecation")
    public void reloadAfterLanguageChange() {
        Common.saveUserData(this, AppLanguage.KEY_REOPEN_USER_SETTING, "0");
        getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new UserSetting(), false);
    }

    public void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
    }

    public void lockUnlockDrawer(int lockMode) {
        drawerLayout.setDrawerLockMode(lockMode);
    }

    public void removeCurrentFragmentAndMoveBack() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        }
    }

    /**
     * Prefer this for system/hardware back and toolbar back.
     * Pops the back stack when possible; otherwise returns to Home
     * (or requires double-back to exit when already on Home).
     */
    public void navigateBack() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            // Soft pop — avoids main-thread hitch from Immediate when returning to Home
            fragmentManager.popBackStack();
            return;
        }
        if (isHomeVisible()) {
            handleHomeBackPress();
        } else {
            navigateToHome();
        }
    }

    /** Clears history and shows Home without flicker from intermediate pops. */
    public void navigateToHome() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        if (!isHomeVisible()) {
            loadFragment(new Home(), false);
        }
        lastBackPressAtHomeMs = 0L;
    }

    /**
     * Opens a screen with Home underneath so one back returns to Home.
     * Same navigation flow as before; Home heavy work is deferred until it is actually visible.
     */
    public void openAboveHome(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        Fragment current = fragmentManager.findFragmentById(R.id.frameLayout);
        if (!(current instanceof Home)) {
            Home.deferHeavyWorkForNextStart = true;
            FragmentTransaction homeTransaction = fragmentManager.beginTransaction();
            homeTransaction.replace(R.id.frameLayout, new Home());
            homeTransaction.commitNowAllowingStateLoss();
        }
        loadFragment(fragment, true);
    }

    /**
     * Navigate toward a parent screen.
     * When {@code addToBackStack} is true (typical toolbar / hardware "back"), pop the existing
     * entry instead of replace+push — avoids stack bloat, double-inflate lag, and wrong back order.
     * When false, replace with the given fragment without growing the stack (edit/save flows).
     */
    public void goBackTo(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (addToBackStack && fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
            return;
        }
        loadFragment(fragment, false);
    }

    public void loadFragment(Fragment fragment, Boolean bool) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setReorderingAllowed(true);
        if (bool) {
            // Forward navigation only — keep pops snappy
            transaction.setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    0,
                    0
            );
        }
        transaction.replace(R.id.frameLayout, fragment);
        if (bool) {
            transaction.addToBackStack(null);
        }
        transaction.commitAllowingStateLoss();
    }

    private boolean isHomeVisible() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.frameLayout);
        return current instanceof Home;
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isHomeVisible()) {
                    handleHomeBackPress();
                    return;
                }
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStack();
                } else {
                    navigateToHome();
                }
            }
        });
    }

    private void handleHomeBackPress() {
        long now = System.currentTimeMillis();
        if (now - lastBackPressAtHomeMs < DOUBLE_BACK_EXIT_INTERVAL_MS) {
            finish();
            return;
        }
        lastBackPressAtHomeMs = now;
        Toast.makeText(this, R.string.press_back_again_to_exit, Toast.LENGTH_SHORT).show();
    }

    /** Track which POS screen (fragment) is visible for crash reports. */
    private void registerFragmentScreenTracking() {
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                        Observability.setFragmentScreen(f.getClass().getSimpleName());
                    }
                },
                true
        );
    }
}
