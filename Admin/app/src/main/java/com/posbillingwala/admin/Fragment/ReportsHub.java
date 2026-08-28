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
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.databinding.FragmentReportsHubBinding;
import com.posbillingwala.admin.databinding.ItemReportMenuRowBinding;

public class ReportsHub extends Fragment {

    Activity activity;
    FragmentReportsHubBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReportsHubBinding.inflate(inflater, container, false);
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Reports");

        // Icons/colors match reference Reports hub exactly
        setupRow(binding.rowSales, R.drawable.ic_report_sales, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, "Sales Overview", "Track sales performance & trends");
        setupRow(binding.rowCustomers, R.drawable.ic_report_customers, R.drawable.bg_quick_action_purple,
                R.color.deepPurple, "Customer Reports", "Customer growth & status analytics");
        setupRow(binding.rowLicenses, R.drawable.ic_report_licenses, R.drawable.bg_quick_action_green,
                R.color.statusActive, "License Reports", "License status & expiry insights");
        setupRow(binding.rowDealers, R.drawable.ic_report_dealers, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, "Dealer Reports", "Dealer performance & sales");
        setupRow(binding.rowBranches, R.drawable.ic_report_branches, R.drawable.bg_quick_action_navy,
                R.color.colorPrimaryDark, "Branch Reports", "Branch status & distribution");
        setupRow(binding.rowDevices, R.drawable.ic_report_devices, R.drawable.bg_quick_action_violet,
                R.color.deep_purple_400, "Device Reports", "Device usage & activity");

        binding.rowSales.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).navigateDetail(new SalesOverview(), "Sales Overview"));
        binding.rowCustomers.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).navigateDetail(new CustomerReports(), "Customer Reports"));
        binding.rowLicenses.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).navigateDetail(new LicenseReports(), "License Reports"));
        binding.rowDealers.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).navigateDetail(new DealerReports(), "Dealer Reports"));
        binding.rowBranches.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).navigateDetail(new BranchReports(), "Branch Reports"));
        binding.rowDevices.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).navigateDetail(new DeviceReports(), "Device Reports"));

        return binding.getRoot();
    }

    private void setupRow(ItemReportMenuRowBinding row, int iconRes, int bgRes, int tintColor,
                          String title, String subtitle) {
        row.menuIcon.setBackgroundResource(bgRes);
        row.menuIcon.setImageResource(iconRes);
        row.menuIcon.clearColorFilter();
        row.menuIcon.setColorFilter(ContextCompat.getColor(requireContext(), tintColor));
        row.menuTitle.setText(title);
        row.menuSubtitle.setText(subtitle);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(0);
        MainActivity.drawerLayout.closeDrawers();
    }
}
