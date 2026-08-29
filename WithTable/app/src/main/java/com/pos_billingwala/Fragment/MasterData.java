package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentMasterDataBinding;
import com.pos_billingwala.databinding.ItemGroupedMenuRowBinding;

public class MasterData extends Fragment implements View.OnClickListener {

    public static final String OPENED_FROM_MASTER = "master";

    public static Activity activity;
    public static FragmentMasterDataBinding binding;
    View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMasterDataBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    goToSettings();
                    return true;
                }
                return false;
            }
        });

        initViews();
        return view;
    }

    public void initViews() {
        binding.toolbar.toolbarTitle.setText(getString(R.string.master_data_title));
        binding.toolbar.backButton.setOnClickListener(this);
        setupRow(binding.categoryLayout, R.drawable.ic_category, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.master_category), getString(R.string.master_hint_category));
        setupRow(binding.subcategoryLayout, R.drawable.ic_folder, R.drawable.bg_quick_action_purple,
                R.color.deepPurple, getString(R.string.master_subcategory), getString(R.string.master_hint_subcategory));
        setupRow(binding.portionLayout, R.drawable.ic_layers, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.master_portions), getString(R.string.master_hint_portions));
        setupRow(binding.productLayout, R.drawable.ic_report_product, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.master_products), getString(R.string.master_hint_products));
        setupRow(binding.comboLayout, R.drawable.ic_report_combo, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.master_combos), getString(R.string.master_hint_combos));
        showGroupDividers(binding.categoryLayout, binding.subcategoryLayout, binding.portionLayout,
                binding.productLayout, binding.comboLayout);

        binding.categoryLayout.getRoot().setOnClickListener(this);
        binding.subcategoryLayout.getRoot().setOnClickListener(this);
        binding.portionLayout.getRoot().setOnClickListener(this);
        binding.productLayout.getRoot().setOnClickListener(this);
        binding.comboLayout.getRoot().setOnClickListener(this);
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

    private void showGroupDividers(ItemGroupedMenuRowBinding... rows) {
        for (int i = 1; i < rows.length; i++) {
            rows[i].rowDivider.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backButton) {
            goToSettings();
        } else if (id == R.id.categoryLayout) {
            AddCategory addCategory = new AddCategory();
            addCategory.setArguments(openedFromMaster());
            ((MainActivity) activity).loadFragment(addCategory, true);
        } else if (id == R.id.subcategoryLayout) {
            AddSubcategory addSubcategory = new AddSubcategory();
            addSubcategory.setArguments(openedFromMaster());
            ((MainActivity) activity).loadFragment(addSubcategory, true);
        } else if (id == R.id.portionLayout) {
            AddPortionMaster addPortionMaster = new AddPortionMaster();
            Bundle bundle = openedFromMaster();
            bundle.putString("returnTo", "masterData");
            addPortionMaster.setArguments(bundle);
            ((MainActivity) activity).loadFragment(addPortionMaster, true);
        } else if (id == R.id.productLayout) {
            ProductMaster productMaster = new ProductMaster();
            productMaster.setArguments(openedFromMaster());
            ((MainActivity) activity).loadFragment(productMaster, true);
        } else if (id == R.id.comboLayout) {
            ComboMaster comboMaster = new ComboMaster();
            comboMaster.setArguments(openedFromMaster());
            ((MainActivity) activity).loadFragment(comboMaster, true);
        }
    }

    private void goToSettings() {
        ((MainActivity) activity).navigateBack();
    }

    static Bundle openedFromMaster() {
        Bundle bundle = new Bundle();
        bundle.putString("openedFrom", OPENED_FROM_MASTER);
        return bundle;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
    }
}
