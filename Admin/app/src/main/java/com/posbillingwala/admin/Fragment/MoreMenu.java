package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.BottomSheetUi;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.databinding.FragmentMoreMenuBinding;
import com.posbillingwala.admin.databinding.ItemMoreMenuRowBinding;

public class MoreMenu extends Fragment {

    public static Activity activity;
    FragmentMoreMenuBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMoreMenuBinding.inflate(inflater, container, false);
        activity = getActivity();
        MainActivity.title.setText("More");

        setupRow(binding.menuDealers, R.drawable.ic_report_dealers, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, "Dealers", "View & manage dealers");
        setupRow(binding.menuAddDealer, R.drawable.ic_nav_add_dealer, R.drawable.bg_quick_action_green,
                R.color.statusActive, "Add Dealer", "Register a new dealer");
        setupRow(binding.menuPosMonitoring, R.drawable.ic_nav_devices, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, "POS Monitoring", "Device online status");
        setupRow(binding.menuProductImport, R.drawable.ic_nav_export, R.drawable.bg_quick_action_navy,
                R.color.colorPrimaryDark, "Product Import", "Export / import catalog");
        setupRow(binding.menuCrashInfo, R.drawable.ic_crash, R.drawable.bg_quick_action_violet,
                R.color.deep_purple_400, "Crash & Error Logs", "POS crashes & API failures");
        setupRow(binding.menuSettings, R.drawable.ic_logout, R.drawable.bg_quick_action_red,
                R.color.statusExpired, "Logout", "Sign out of admin app");

        binding.menuDealers.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new AllDealerList(), true));
        binding.menuAddDealer.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new AddDealer(), true));
        binding.menuPosMonitoring.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new PosMonitoring(), true));
        binding.menuProductImport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new ProductExport(), true));
        binding.menuCrashInfo.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new CrashErrorLogList(), true));
        binding.menuSettings.getRoot().setOnClickListener(v -> {
            if (activity instanceof MainActivity) {
                BottomSheetUi.showConfirm(activity, "Logout", "Are you sure you want to logout?",
                        "Logout", "Cancel", true, () -> ((MainActivity) activity).performLogout());
            }
        });

        return binding.getRoot();
    }

    private void setupRow(ItemMoreMenuRowBinding row, int iconRes, int bgRes, int tintColor,
                          String title, String subtitle) {
        row.menuIcon.setBackgroundResource(bgRes);
        row.menuIcon.setImageResource(iconRes);
        row.menuIcon.setColorFilter(ContextCompat.getColor(requireContext(), tintColor));
        row.menuTitle.setText(title);
        row.menuSubtitle.setText(subtitle);
        if ("Logout".equals(title)) {
            row.menuTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.statusExpired));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(0);
        MainActivity.drawerLayout.closeDrawers();
    }
}
