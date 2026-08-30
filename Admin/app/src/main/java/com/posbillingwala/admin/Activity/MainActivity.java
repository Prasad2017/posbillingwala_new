package com.posbillingwala.admin.Activity;



import android.annotation.SuppressLint;

import android.content.Intent;

import android.graphics.Color;

import android.net.Uri;

import android.os.Bundle;

import android.os.Handler;

import android.os.Looper;

import android.os.StrictMode;

import android.view.View;

import android.widget.ImageView;

import android.widget.LinearLayout;

import android.widget.TextView;

import android.widget.Toast;



import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.app.AppCompatDelegate;

import androidx.core.content.ContextCompat;

import androidx.core.view.GravityCompat;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.fragment.app.Fragment;

import androidx.fragment.app.FragmentManager;

import androidx.fragment.app.FragmentTransaction;



import com.google.android.material.appbar.AppBarLayout;

import com.posbillingwala.admin.Extra.AuthTokens;
import com.posbillingwala.admin.Extra.BottomSheetUi;

import com.posbillingwala.admin.Extra.Common;

import com.posbillingwala.admin.Extra.ScreenshotConfig;

import com.posbillingwala.admin.Fragment.AddDealer;

import com.posbillingwala.admin.Fragment.AllCustomerList;

import com.posbillingwala.admin.Fragment.AllDealerList;

import com.posbillingwala.admin.Fragment.CustomerRegistration;

import com.posbillingwala.admin.Fragment.Home;

import com.posbillingwala.admin.Fragment.PosMonitoring;

import com.posbillingwala.admin.Fragment.ProductExport;

import com.posbillingwala.admin.Fragment.ReportsHub;

import com.posbillingwala.admin.Fragment.SalesOverview;

import com.posbillingwala.admin.Fragment.SettingsHub;

import com.posbillingwala.admin.Fragment.SettingsNotifications;

import com.posbillingwala.admin.Fragment.SupportHub;

import com.posbillingwala.admin.Fragment.CrashErrorLogList;

import com.posbillingwala.admin.Fragment.SalesDashboard;

import com.posbillingwala.admin.R;

import com.posbillingwala.admin.Retrofit.Api;

import com.posbillingwala.admin.databinding.ActivityMainBinding;

import com.posbillingwala.admin.databinding.ItemNavDrawerRowBinding;



@SuppressLint("SetTextI18n, NonConstantResourceId, ResourceType, UseCompatLoadingForDrawables, StaticFieldLeak")

public class MainActivity extends AppCompatActivity implements View.OnClickListener {



    public static final int NAV_DASHBOARD = 1;

    public static final int NAV_CUSTOMERS = 2;

    public static final int NAV_ADD_CUSTOMER = 3;

    public static final int NAV_LICENSES = 4;

    public static final int NAV_DEVICES = 5;

    public static final int NAV_DEALERS = 6;

    public static final int NAV_ADD_DEALER = 7;

    public static final int NAV_SALES = 8;

    public static final int NAV_REPORTS = 9;

    public static final int NAV_CATALOG = 10;

    public static final int NAV_EXPORT = 11;

    public static final int NAV_NOTIFICATIONS = 12;

    public static final int NAV_SUPPORT = 13;

    public static final int NAV_ABOUT = 14;

    public static final int NAV_SETTINGS = 15;

    public static final int NAV_CRASH = 16;



    public static ImageView back;

    public static ImageView menu;

    public static ImageView logout;

    public static DrawerLayout drawerLayout;

    public static TextView title;

    public static TextView notificationBadge;

    public static AppBarLayout toolbarContainer;

    public static String userId, currency = "₹. ";



    private static final int DRAWER_UNLOCKED = DrawerLayout.LOCK_MODE_UNLOCKED;

    private static final int DRAWER_LOCKED = DrawerLayout.LOCK_MODE_LOCKED_CLOSED;



    ActivityMainBinding binding;

    boolean doubleBackToExitPressedOnce = false;

    int selectedNavId = NAV_DASHBOARD;

    ItemNavDrawerRowBinding[] navRows;



    @Override

    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        ScreenshotConfig.apply(this);

        Api.bindContext(this);



        if (!AuthTokens.hasValidSession(this)) {

            AuthTokens.clear(this);

            startActivity(new Intent(this, Login.class));

            finish();

            return;

        }



        binding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        ScreenshotConfig.apply(this);



        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));



        initViews();

        setupDrawerNav();



        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);



        try {

            userId = Common.getSavedUserData(MainActivity.this, "userId");

        } catch (Exception e) {

            e.printStackTrace();

        }



        navigateRoot(new Home(), "Dashboard", NAV_DASHBOARD);

    }



    private void initViews() {

        drawerLayout = binding.drawerLayout;

        toolbarContainer = binding.toolbarContainer;

        back = binding.appBarMainPage.back;

        menu = binding.appBarMainPage.menu;

        logout = binding.appBarMainPage.logout;

        title = binding.appBarMainPage.title;

        notificationBadge = binding.appBarMainPage.notificationBadge;



        back.setOnClickListener(this);

        menu.setOnClickListener(this);

        logout.setOnClickListener(this);

        binding.appBarMainPage.notifications.setOnClickListener(this);

        binding.navDrawer.navLogout.setOnClickListener(this);



        String email = Common.getSavedUserData(this, "userEmail");

        if (email == null || email.trim().isEmpty()) {

            email = "admin@posbillingwala.com";

        }

        binding.navDrawer.drawerUserEmail.setText(email);

    }



    private void setupDrawerNav() {

        configureNavRow(binding.navDrawer.navDashboard, R.drawable.ic_nav_dashboard, "Dashboard", 0);

        configureNavRow(binding.navDrawer.navCustomers, R.drawable.ic_nav_customers, "All Customers", 0);

        configureNavRow(binding.navDrawer.navAddCustomer, R.drawable.ic_nav_add, "Add Customer", 0);

        configureNavRow(binding.navDrawer.navLicenses, R.drawable.ic_nav_licenses, "Licenses", 0);

        configureNavRow(binding.navDrawer.navDevices, R.drawable.ic_nav_devices, "POS Monitoring", 0);

        configureNavRow(binding.navDrawer.navDealers, R.drawable.ic_nav_dealers, "All Dealers", 0);

        configureNavRow(binding.navDrawer.navAddDealer, R.drawable.ic_nav_add_dealer, "Add Dealer", 0);

        configureNavRow(binding.navDrawer.navSales, R.drawable.ic_nav_sales, "Sales Dashboard", 0);

        configureNavRow(binding.navDrawer.navReports, R.drawable.ic_nav_reports, "Reports", 0);

        configureNavRow(binding.navDrawer.navCatalog, R.drawable.ic_nav_catalog, "Product Catalog", 0);

        configureNavRow(binding.navDrawer.navExport, R.drawable.ic_nav_export, "Export Data", 0);

        configureNavRow(binding.navDrawer.navNotifications, R.drawable.ic_notifications, "Notifications", 0);

        configureNavRow(binding.navDrawer.navSettings, R.drawable.ic_settings, "Settings", 0);

        configureNavRow(binding.navDrawer.navCrash, R.drawable.ic_crash, "Crash & Error Logs", 0);

        configureNavRow(binding.navDrawer.navSupport, R.drawable.ic_nav_support, "Help & Support", 0);

        binding.navDrawer.navAbout.getRoot().setVisibility(View.GONE);



        navRows = new ItemNavDrawerRowBinding[]{

                binding.navDrawer.navDashboard,

                binding.navDrawer.navCustomers,

                binding.navDrawer.navAddCustomer,

                binding.navDrawer.navLicenses,

                binding.navDrawer.navDevices,

                binding.navDrawer.navDealers,

                binding.navDrawer.navAddDealer,

                binding.navDrawer.navSales,

                binding.navDrawer.navReports,

                binding.navDrawer.navCatalog,

                binding.navDrawer.navExport,

                binding.navDrawer.navNotifications,

                binding.navDrawer.navSettings,

                binding.navDrawer.navCrash,

                binding.navDrawer.navSupport,

                binding.navDrawer.navAbout

        };



        binding.navDrawer.navDashboard.getRoot().setOnClickListener(v -> openNav(NAV_DASHBOARD));

        binding.navDrawer.navCustomers.getRoot().setOnClickListener(v -> openNav(NAV_CUSTOMERS));

        binding.navDrawer.navAddCustomer.getRoot().setOnClickListener(v -> openNav(NAV_ADD_CUSTOMER));

        binding.navDrawer.navLicenses.getRoot().setOnClickListener(v -> openNav(NAV_LICENSES));

        binding.navDrawer.navDevices.getRoot().setOnClickListener(v -> openNav(NAV_DEVICES));

        binding.navDrawer.navDealers.getRoot().setOnClickListener(v -> openNav(NAV_DEALERS));

        binding.navDrawer.navAddDealer.getRoot().setOnClickListener(v -> openNav(NAV_ADD_DEALER));

        binding.navDrawer.navSales.getRoot().setOnClickListener(v -> openNav(NAV_SALES));

        binding.navDrawer.navReports.getRoot().setOnClickListener(v -> openNav(NAV_REPORTS));

        binding.navDrawer.navCatalog.getRoot().setOnClickListener(v -> openNav(NAV_CATALOG));

        binding.navDrawer.navExport.getRoot().setOnClickListener(v -> openNav(NAV_EXPORT));

        binding.navDrawer.navNotifications.getRoot().setOnClickListener(v -> openNav(NAV_NOTIFICATIONS));

        binding.navDrawer.navSettings.getRoot().setOnClickListener(v -> openNav(NAV_SETTINGS));

        binding.navDrawer.navCrash.getRoot().setOnClickListener(v -> openNav(NAV_CRASH));

        binding.navDrawer.navSupport.getRoot().setOnClickListener(v -> openNav(NAV_SUPPORT));

    }



    private void configureNavRow(ItemNavDrawerRowBinding row, int iconRes, String label, int badgeCount) {
        row.navRowIcon.setImageResource(iconRes);
        row.navRowTitle.setText(label);
        if (badgeCount > 0) {
            row.navRowBadge.setVisibility(View.VISIBLE);
            row.navRowBadge.setText(String.valueOf(badgeCount));
        } else {
            row.navRowBadge.setVisibility(View.GONE);
        }
    }



    private void openNav(int navId) {

        drawerLayout.closeDrawer(GravityCompat.START);

        switch (navId) {

            case NAV_DASHBOARD:

                navigateRoot(new Home(), "Dashboard", NAV_DASHBOARD);

                break;

            case NAV_CUSTOMERS:

                navigateRoot(new AllCustomerList(), "All Customers", NAV_CUSTOMERS);

                break;

            case NAV_ADD_CUSTOMER:

                navigateDetail(new CustomerRegistration(), "Add Customer");

                highlightNavItem(NAV_ADD_CUSTOMER);

                break;

            case NAV_LICENSES:

                navigateRoot(new AllCustomerList(), "Licenses", NAV_LICENSES);

                break;

            case NAV_DEVICES:

                navigateRoot(new PosMonitoring(), "POS Monitoring", NAV_DEVICES);

                break;

            case NAV_DEALERS:

                navigateRoot(new AllDealerList(), "All Dealers", NAV_DEALERS);

                break;

            case NAV_ADD_DEALER:

                navigateDetail(new AddDealer(), "Add Dealer");

                highlightNavItem(NAV_ADD_DEALER);

                break;

            case NAV_SALES:

                navigateRoot(new SalesDashboard(), "Sales Dashboard", NAV_SALES);

                break;

            case NAV_REPORTS:

                navigateRoot(new ReportsHub(), "Reports", NAV_REPORTS);

                break;

            case NAV_CATALOG:

            case NAV_EXPORT:

                navigateRoot(new ProductExport(), navId == NAV_CATALOG ? "Product Catalog" : "Export Data", navId);

                break;

            case NAV_NOTIFICATIONS:

                navigateRoot(new SettingsNotifications(), "Notifications", NAV_NOTIFICATIONS);

                break;

            case NAV_SUPPORT:

                navigateRoot(new SupportHub(), "Support", NAV_SUPPORT);

                break;

            case NAV_SETTINGS:

                navigateRoot(new SettingsHub(), "Settings", NAV_SETTINGS);

                break;

            case NAV_CRASH:

                navigateRoot(new CrashErrorLogList(), "Crash & Error Logs", NAV_CRASH);

                break;

            case NAV_ABOUT:

                highlightNavItem(NAV_ABOUT);

                openAboutPage();

                break;

            default:

                break;

        }

    }



    public void navigateRoot(Fragment fragment, String screenTitle, int navId) {

        clearBackStack();

        loadFragment(fragment, false);

        setScreenTitle(screenTitle);

        lockUnlockDrawer(DRAWER_UNLOCKED);

        highlightNavItem(navId);

        drawerLayout.closeDrawers();

    }



    public void navigateDetail(Fragment fragment, String screenTitle) {

        loadFragment(fragment, true);

        setScreenTitle(screenTitle);

        lockUnlockDrawer(DRAWER_LOCKED);

        drawerLayout.closeDrawers();

    }



    public void setScreenTitle(String screenTitle) {

        if (title != null) {

            title.setText(screenTitle);

            title.setVisibility(View.VISIBLE);

        }

    }



    public void highlightNavItem(int navId) {

        selectedNavId = navId;

        if (navRows == null) {

            return;

        }

        int[] ids = {

                NAV_DASHBOARD, NAV_CUSTOMERS, NAV_ADD_CUSTOMER, NAV_LICENSES, NAV_DEVICES,

                NAV_DEALERS, NAV_ADD_DEALER, NAV_SALES, NAV_REPORTS, NAV_CATALOG, NAV_EXPORT,

                NAV_NOTIFICATIONS, NAV_SETTINGS, NAV_CRASH, NAV_SUPPORT

        };

        int activeColor = ContextCompat.getColor(this, R.color.colorPrimary);

        int inactiveColor = ContextCompat.getColor(this, R.color.colorTextPrimary);

        int inactiveIcon = ContextCompat.getColor(this, R.color.colorTextSecondary);



        for (int i = 0; i < navRows.length; i++) {

            ItemNavDrawerRowBinding row = navRows[i];

            boolean selected = ids[i] == navId;

            row.getRoot().setBackgroundResource(selected

                    ? R.drawable.bg_nav_drawer_item_selected

                    : android.R.color.transparent);

            row.navRowTitle.setTextColor(selected ? Color.WHITE : inactiveColor);

            row.navRowIcon.setColorFilter(selected ? Color.WHITE : inactiveIcon);

        }

    }



    public void setNotificationCount(int count) {

        if (notificationBadge == null || binding == null) {

            return;

        }

        if (count > 0) {

            notificationBadge.setVisibility(View.VISIBLE);

            notificationBadge.setText(String.valueOf(count));

            binding.navDrawer.navNotifications.navRowBadge.setVisibility(View.VISIBLE);

            binding.navDrawer.navNotifications.navRowBadge.setText(String.valueOf(count));

        } else {

            notificationBadge.setVisibility(View.GONE);

            binding.navDrawer.navNotifications.navRowBadge.setVisibility(View.GONE);

        }

    }



    private void dialSupport() {

        try {

            Intent intent = new Intent(Intent.ACTION_DIAL,

                    Uri.parse("tel:" + getString(R.string.support_phone_dial)));

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(this, R.string.support_phone_display, Toast.LENGTH_LONG).show();

        }

    }



    public void openAboutPage() {

        try {

            Intent intent = new Intent(Intent.ACTION_VIEW,

                    Uri.parse("https://posbillingwala.com/about.html"));

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(this, "POS Billing Wala Admin", Toast.LENGTH_SHORT).show();

        }

    }



    @Override

    public void onClick(View view) {

        int id = view.getId();

        if (id == R.id.menu) {

            drawerLayout.openDrawer(GravityCompat.START);

        } else if (id == R.id.back) {

            removeCurrentFragmentAndMoveBack();

        } else if (id == R.id.logout || id == R.id.navLogout) {

            confirmLogout();

        } else if (id == R.id.notifications) {

            navigateRoot(new SettingsNotifications(), "Notifications", NAV_NOTIFICATIONS);

        }

    }



    private void clearBackStack() {

        try {

            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        } catch (Exception e) {

            e.printStackTrace();

        }

    }



    private void confirmLogout() {

        drawerLayout.closeDrawers();

        BottomSheetUi.showConfirm(this, "Logout", "Are you sure you want to logout?",
                "Logout", "Cancel", true, this::performLogout);

    }



    public void performLogout() {

        AuthTokens.clear(this);

        Intent intent = new Intent(this, Login.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);

        finish();

    }



    @Override

    public void onBackPressed() {

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {

            drawerLayout.closeDrawer(GravityCompat.START);

            return;

        }

        if (back.getVisibility() == View.VISIBLE) {

            super.onBackPressed();

            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {

                lockUnlockDrawer(DRAWER_UNLOCKED);

                highlightNavItem(selectedNavId);

            }

            return;

        }

        if (doubleBackToExitPressedOnce) {

            super.onBackPressed();

            return;

        }

        doubleBackToExitPressedOnce = true;

        Toast.makeText(this, "Press back once more to exit", Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);

    }



    public void lockUnlockDrawer(int lockMode) {

        drawerLayout.setDrawerLockMode(lockMode);

        if (lockMode == DRAWER_LOCKED) {

            back.setVisibility(View.VISIBLE);

            menu.setVisibility(View.GONE);

        } else {

            back.setVisibility(View.GONE);

            menu.setVisibility(View.VISIBLE);

        }

    }



    public void removeCurrentFragmentAndMoveBack() {

        try {

            getSupportFragmentManager().popBackStack();

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

}

