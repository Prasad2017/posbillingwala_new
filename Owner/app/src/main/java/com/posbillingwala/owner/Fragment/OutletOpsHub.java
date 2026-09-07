package com.posbillingwala.owner.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.databinding.FragmentOutletOpsHubBinding;
import com.posbillingwala.owner.databinding.ItemGroupedMenuRowBinding;

/**
 * Hub for franchise outlet tools (template, appointments, deposits, staff).
 */
public class OutletOpsHub extends Fragment {

    private Activity activity;
    private FragmentOutletOpsHubBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOutletOpsHubBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        activity = getActivity();

        binding.toolbar.toolbarTitle.setText(getString(R.string.outlet_ops_title));
        binding.toolbar.backButton.setOnClickListener(v -> goBack());
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                goBack();
                return true;
            }
            return false;
        });

        setupRow(binding.templateRow, R.drawable.ic_category, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.setting_outlet_template),
                getString(R.string.setting_hint_outlet_template));
        setupRow(binding.apptRow, R.drawable.ic_report_sales, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.setting_outlet_appointments),
                getString(R.string.setting_hint_outlet_appointments));
        setupRow(binding.depositRow, R.drawable.ic_cloud_download, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.setting_outlet_deposits),
                getString(R.string.setting_hint_outlet_deposits));
        setupRow(binding.staffRow, R.drawable.ic_person, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.setting_outlet_staff),
                getString(R.string.setting_hint_outlet_staff));

        binding.apptRow.rowDivider.setVisibility(View.VISIBLE);
        binding.depositRow.rowDivider.setVisibility(View.VISIBLE);
        binding.staffRow.rowDivider.setVisibility(View.VISIBLE);

        binding.templateRow.getRoot().setOnClickListener(v -> open(new OutletBusinessTemplate()));
        binding.apptRow.getRoot().setOnClickListener(v -> open(new OutletAppointmentCalendar()));
        binding.depositRow.getRoot().setOnClickListener(v -> open(new OutletDepositLedger()));
        binding.staffRow.getRoot().setOnClickListener(v -> open(new OutletStaffRoster()));

        return view;
    }

    private void setupRow(ItemGroupedMenuRowBinding row, int iconRes, int bgRes, int tintColor,
                          String title, String subtitle) {
        row.menuIcon.setBackgroundResource(bgRes);
        row.menuIcon.setImageResource(iconRes);
        row.menuIcon.clearColorFilter();
        row.menuIcon.setColorFilter(ContextCompat.getColor(requireContext(), tintColor));
        row.menuTitle.setText(title);
        row.menuSubtitle.setText(subtitle);
    }

    private void open(Fragment fragment) {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        ((MainActivity) activity).loadFragment(fragment, true);
    }

    private void goBack() {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        ((MainActivity) activity).loadFragment(new UserSetting(), true);
    }
}
