package com.posbillingwala.owner.Activity;

import android.os.Bundle;
import android.os.StrictMode;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.posbillingwala.owner.Extra.Common;
import com.posbillingwala.owner.Extra.ScreenshotConfig;
import com.posbillingwala.owner.Fragment.Home;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    public ActivityMainBinding binding;
    public static DrawerLayout drawerLayout;
    public static String userId, currency = "₹.", reportPin;
    public static int branchCount = 1;
    public static int licenseCount = 1;

    public static void setOutletCounts(int outlets) {
        int value = Math.max(0, outlets);
        branchCount = value;
        licenseCount = value;
    }

    public static boolean isMultiOutlet() {
        return branchCount > 1 || licenseCount > 1;
    }
    boolean doubleBackToExitPressedOnce = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenshotConfig.apply(this);
        Api.bindContext(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenshotConfig.apply(this);
        initViews();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            userId = Common.getSavedUserData(MainActivity.this, "userId");
            reportPin = Common.getSavedUserData(MainActivity.this, "reportPin");
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadFragment(new Home(), false);

    }

    @Override
    protected void onResume() {
        super.onResume();
        ScreenshotConfig.apply(this);
    }

    public void initViews() {
        drawerLayout = binding.drawerLayout;
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
