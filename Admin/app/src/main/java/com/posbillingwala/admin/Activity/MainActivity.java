package com.posbillingwala.admin.Activity;

import android.annotation.SuppressLint;
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
import com.posbillingwala.admin.Extra.Common;
import com.posbillingwala.admin.Fragment.AllCustomerList;
import com.posbillingwala.admin.Fragment.AllDealerList;
import com.posbillingwala.admin.Fragment.Home;
import com.posbillingwala.admin.Fragment.ProductExport;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;

import butterknife.ButterKnife;
import butterknife.OnClick;

@SuppressLint("SetTextI18n, NonConstantResourceId, ResourceType, UseCompatLoadingForDrawables, StaticFieldLeak")
public class MainActivity extends AppCompatActivity {

    public static ImageView back;
    public static DrawerLayout drawerLayout;
    public static TextView title;
    public static AppBarLayout toolbarContainer;
    public static LinearLayout bottomNavigationLayout;
    public static String userId, currency = "₹. ";
    public static LinearLayout homeLinearLayout, dealerLinearLayout, customerLinearLayout, customerProductLinearLayout;
    boolean doubleBackToExitPressedOnce = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Api.bindContext(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initViews();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {

            userId = Common.getSavedUserData(MainActivity.this, "userId");

        } catch (Exception e) {
            e.printStackTrace();
        }

        loadFragment(new Home(), false);


    }

    private void initViews() {

        drawerLayout = findViewById(R.id.drawer_layout);
        toolbarContainer = findViewById(R.id.toolbar_container);
        bottomNavigationLayout = findViewById(R.id.bottomNavigationLayout);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);

        homeLinearLayout = findViewById(R.id.homeLinearLayout);
        dealerLinearLayout = findViewById(R.id.dealerLinearLayout);
        customerLinearLayout = findViewById(R.id.customerLinearLayout);
        customerProductLinearLayout = findViewById(R.id.customerProductLinearLayout);

    }


    @OnClick({R.id.back, R.id.homeLinearLayout, R.id.dealerLinearLayout, R.id.customerLinearLayout, R.id.customerProductLinearLayout})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                removeCurrentFragmentAndMoveBack();
                break;
            case R.id.homeLinearLayout:
                removeCurrentFragmentAndMoveBack();
                loadFragment(new Home(), false);
                break;

            case R.id.dealerLinearLayout:
                removeCurrentFragmentAndMoveBack();
                loadFragment(new AllDealerList(), false);
                break;

            case R.id.customerLinearLayout:
                removeCurrentFragmentAndMoveBack();
                loadFragment(new AllCustomerList(), false);
                break;

            case R.id.customerProductLinearLayout:
                removeCurrentFragmentAndMoveBack();
                loadFragment(new ProductExport(), false);
                break;

        }
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

}