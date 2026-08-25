package com.posbillingwala.admin.Activity;

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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.AppBarLayout;
import com.posbillingwala.admin.Extra.AuthTokens;
import com.posbillingwala.admin.Extra.Common;
import com.posbillingwala.admin.Fragment.AllCustomerList;
import com.posbillingwala.admin.Fragment.AllDealerList;
import com.posbillingwala.admin.Fragment.Home;
import com.posbillingwala.admin.Fragment.MoreMenu;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.ActivityMainBinding;

@SuppressLint("SetTextI18n, NonConstantResourceId, ResourceType, UseCompatLoadingForDrawables, StaticFieldLeak")
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static ImageView back;
    public static ImageView logout;
    public static DrawerLayout drawerLayout;
    public static TextView title;
    public static AppBarLayout toolbarContainer;
    public static LinearLayout bottomNavigationLayout;
    public static String userId, currency = "₹. ";
    public static LinearLayout homeLinearLayout, dealerLinearLayout, customerLinearLayout, customerProductLinearLayout;
    boolean doubleBackToExitPressedOnce = false;
    ActivityMainBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Api.bindContext(this);

        if (!AuthTokens.hasValidSession(this)) {
            AuthTokens.clear(this);
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        drawerLayout = binding.drawerLayout;
        toolbarContainer = binding.toolbarContainer;
        bottomNavigationLayout = binding.bottomNavigationLayout;
        back = binding.appBarMainPage.back;
        logout = binding.appBarMainPage.logout;
        title = binding.appBarMainPage.title;

        homeLinearLayout = binding.homeLinearLayout;
        dealerLinearLayout = binding.dealerLinearLayout;
        customerLinearLayout = binding.customerLinearLayout;
        customerProductLinearLayout = binding.customerProductLinearLayout;

        back.setOnClickListener(this);
        logout.setOnClickListener(this);
        homeLinearLayout.setOnClickListener(this);
        dealerLinearLayout.setOnClickListener(this);
        customerLinearLayout.setOnClickListener(this);
        customerProductLinearLayout.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.back) {
            removeCurrentFragmentAndMoveBack();
        } else if (id == R.id.logout) {
            confirmLogout();
        } else if (id == R.id.homeLinearLayout) {
            removeCurrentFragmentAndMoveBack();
            loadFragment(new Home(), false);
        } else if (id == R.id.dealerLinearLayout) {
            removeCurrentFragmentAndMoveBack();
            loadFragment(new AllDealerList(), false);
        } else if (id == R.id.customerLinearLayout) {
            removeCurrentFragmentAndMoveBack();
            loadFragment(new AllCustomerList(), false);
        } else if (id == R.id.customerProductLinearLayout) {
            removeCurrentFragmentAndMoveBack();
            loadFragment(new MoreMenu(), false);
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
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
