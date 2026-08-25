package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.databinding.FragmentMoreMenuBinding;

public class MoreMenu extends Fragment {

    public static Activity activity;
    FragmentMoreMenuBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMoreMenuBinding.inflate(inflater, container, false);
        activity = getActivity();
        MainActivity.title.setText("More");

        binding.menuPosMonitoring.setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new PosMonitoring(), true));
        binding.menuAddDealer.setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new AddDealer(), true));
        binding.menuProductImport.setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new ProductExport(), true));
        binding.menuCrashInfo.setOnClickListener(v ->
                Toast.makeText(activity,
                        "POS crashes are reported via Firebase Crashlytics. No in-app crash inbox yet.",
                        Toast.LENGTH_LONG).show());
        binding.menuSettings.setOnClickListener(v -> {
            if (activity instanceof MainActivity) {
                new AlertDialog.Builder(activity)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Logout", (d, w) -> ((MainActivity) activity).performLogout())
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(0);
        MainActivity.drawerLayout.closeDrawers();
    }
}
