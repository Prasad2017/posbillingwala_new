package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.ComboAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.AppExecutors;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Model.ComboResponse;
import com.pos_billingwala.databinding.FragmentComboMasterBinding;

import java.util.ArrayList;
import java.util.List;

public class ComboMaster extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static List<ComboResponse> comboResponseList = new ArrayList<>();
    public static ComboAdapter comboAdapter;
    public static FragmentComboMasterBinding binding;
    public static boolean openedFromMaster = false;

    public static void getComboList() {
        if (activity == null || posBillingWalaDatabase == null || binding == null) {
            return;
        }
        final cn.pedant.SweetAlert.SweetAlertDialog loader = ListLoader.show(activity);
        AppExecutors.get().db().execute(() -> {
            List<ComboResponse> list = posBillingWalaDatabase.getComboList(false, false);
            AppExecutors.get().main(() -> {
                try {
                    if (activity == null || binding == null) {
                        return;
                    }
                    comboResponseList = list != null ? list : new ArrayList<>();
                    if (!comboResponseList.isEmpty()) {
                        comboAdapter = new ComboAdapter(activity, comboResponseList);
                        binding.comboRecyclerView.setAdapter(comboAdapter);
                        binding.linearLayout.setVisibility(View.VISIBLE);
                        binding.noDataFound.setVisibility(View.GONE);
                    } else {
                        binding.linearLayout.setVisibility(View.GONE);
                        binding.noDataFound.setVisibility(View.VISIBLE);
                    }
                } finally {
                    ListLoader.dismiss(loader);
                }
            });
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentComboMasterBinding.inflate(inflater, container, false);
        activity = getActivity();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);
        openedFromMaster = getArguments() != null
                && MasterData.OPENED_FROM_MASTER.equals(getArguments().getString("openedFrom"));

        View view = binding.getRoot();
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("tag", "combo master back");
                navigateToCaller();
                return true;
            }
            return false;
        });

        binding.backToHome.setOnClickListener(this);
        binding.addCombo.setOnClickListener(this);
        binding.searchCombo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchCombos(s != null ? s.toString() : "");
            }
        });
        return view;
    }

    private void searchCombos(String query) {
        List<ComboResponse> filtered = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filtered.addAll(comboResponseList);
        } else {
            String q = query.toLowerCase().trim();
            for (ComboResponse combo : comboResponseList) {
                String name = combo.getComboName() != null ? combo.getComboName() : "";
                String code = combo.getComboCode() != null ? combo.getComboCode() : "";
                if (name.toLowerCase().contains(q) || code.toLowerCase().contains(q)) {
                    filtered.add(combo);
                }
            }
        }
        comboAdapter = new ComboAdapter(activity, filtered);
        binding.comboRecyclerView.setAdapter(comboAdapter);
        binding.noDataFound.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.comboRecyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == com.pos_billingwala.R.id.backToHome) {
            navigateToCaller();
        } else if (id == com.pos_billingwala.R.id.addCombo) {
            AddCombo addCombo = new AddCombo();
            if (openedFromMaster) {
                addCombo.setArguments(MasterData.openedFromMaster());
            }
            ((MainActivity) activity).loadFragment(addCombo, true);
        }
    }

    private void navigateToCaller() {
        ((MainActivity) activity).navigateBack();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getComboList();
    }
}
