package com.pos_billingwala.Activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.CursorWindow;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.pos_billingwala.Extra.BranchSession;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.Observability;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Fragment.Home;
import com.pos_billingwala.Fragment.InvoiceCompanyTable;
import com.pos_billingwala.Fragment.InvoiceMess;
import com.pos_billingwala.Fragment.InvoiceTakeAway;
import com.pos_billingwala.R;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {

    public static DrawerLayout drawerLayout;
    public static String userId, ownerId, organizationId, branchId, branchLabel, deviceId, userName, shopName, shopImage, LicenceKey, LicenceKeyRegDate,
            LicenceKeyExpireDate, currencyName, invoiceRunningStatus = "", cartOrderStatus = "",
            fastBilling, takeAway, dineIn, mess, reportPin, totalSaleData, todaySaleData;


    public void setScreenSizeSmall() {
        Configuration configuration = getResources().getConfiguration();
        configuration.fontScale = (float) 1; //0.85 small size, 1 normal size, 1,15 big etc
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        configuration.densityDpi = (int) getResources().getDisplayMetrics().xdpi;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    @SuppressLint("DiscouragedPrivateApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setScreenSizeSmall();

        try {
            Field field = CursorWindow.class.getDeclaredField("sCursorWindowSize");
            field.setAccessible(true);
            field.set(null, 1024 * 1024 * 50); // Set to 50MB
        } catch (Exception e) {
            e.printStackTrace();
        }

        initViews();
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

            if (invoiceRunningStatus.equalsIgnoreCase("")) {
                loadFragment(new Home(), false);
            } else {
                if (cartOrderStatus.equalsIgnoreCase("")) {
                    loadFragment(new CreatePos(), true);
                } else if (cartOrderStatus.equalsIgnoreCase("table_wise")) {
                    loadFragment(new InvoiceCompanyTable(), true);
                } else if (cartOrderStatus.equalsIgnoreCase("take_away")) {
                    loadFragment(new InvoiceTakeAway(), true);
                } else if (cartOrderStatus.equalsIgnoreCase("fast_billing")) {
                    loadFragment(new CreatePos(), true);
                } else if (cartOrderStatus.equalsIgnoreCase("mess")) {
                    loadFragment(new InvoiceMess(), true);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            loadFragment(new Home(), false);
        }

    }

    public void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
    }

    public void lockUnlockDrawer(int lockMode) {
        drawerLayout.setDrawerLockMode(lockMode);
    }

    public void removeCurrentFragmentAndMoveBack() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack();
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