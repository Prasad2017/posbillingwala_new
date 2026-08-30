package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.ComboItemDraftAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ComboItemPicker;
import com.pos_billingwala.Extra.ComboValidator;
import com.pos_billingwala.Extra.MasterListTabletUi;
import com.pos_billingwala.Model.ComboItemDraft;
import com.pos_billingwala.Model.ComboItemResponse;
import com.pos_billingwala.Model.ComboResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentAddComboBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AddCombo extends Fragment implements View.OnClickListener {

    public static Activity activity;
    FragmentAddComboBinding binding;
    POSBillingWalaDatabase posBillingWalaDatabase;
    final List<ComboItemDraft> drafts = new ArrayList<>();
    ComboItemDraftAdapter adapter;
    String editingComboId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddComboBinding.inflate(inflater, container, false);
        activity = getActivity();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        View view = binding.getRoot();
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("tag", "add combo back");
                goBack();
                return true;
            }
            return false;
        });

        if (getArguments() != null) {
            editingComboId = getArguments().getString("comboId");
        }

        adapter = new ComboItemDraftAdapter(drafts, position -> {
            if (position >= 0 && position < drafts.size()) {
                drafts.remove(position);
                adapter.notifyDataSetChanged();
            }
        });
        binding.comboItemRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        binding.comboItemRecyclerView.setAdapter(adapter);

        binding.backToCombo.setOnClickListener(this);
        binding.addComboItem.setOnClickListener(this);
        binding.saveCombo.setOnClickListener(this);

        MasterListTabletUi.applyFormListSplit(activity, binding.comboFormContainer,
                binding.comboDetailsSection, binding.comboItemsSection);

        if (editingComboId != null) {
            binding.comboFormTitle.setText(getString(R.string.ui_update_combo));
            loadCombo();
        } else {
            suggestCode();
        }
        return view;
    }

    private void suggestCode() {
        ComboResponse latest = posBillingWalaDatabase.getLatestCombo();
        if (latest != null && latest.getComboCode() != null) {
            try {
                int next = Integer.parseInt(latest.getComboCode().trim()) + 1;
                binding.comboCode.setText(String.valueOf(next));
                return;
            } catch (Exception ignored) {
            }
        }
        binding.comboCode.setText("1");
    }

    private void loadCombo() {
        ComboResponse combo = posBillingWalaDatabase.getComboDetail(editingComboId);
        if (combo == null) {
            return;
        }
        binding.comboName.setText(combo.getComboName());
        binding.comboCode.setText(combo.getComboCode());
        binding.comboPrice.setText(combo.getComboPrice());
        binding.comboCGST.setText(combo.getComboCGST());
        binding.comboSGST.setText(combo.getComboSGST());
        binding.comboActiveSwitch.setChecked(combo.isActive());
        drafts.clear();
        List<ComboItemResponse> items = posBillingWalaDatabase.getComboItemList(editingComboId);
        for (ComboItemResponse item : items) {
            drafts.add(ComboItemDraft.fromResponse(item));
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToCombo) {
            goBack();
        } else if (id == R.id.addComboItem) {
            ComboItemPicker.show(activity, posBillingWalaDatabase, draft -> {
                for (int i = 0; i < drafts.size(); i++) {
                    if (drafts.get(i).itemKey().equals(draft.itemKey())) {
                        drafts.set(i, draft);
                        adapter.notifyDataSetChanged();
                        return;
                    }
                }
                drafts.add(draft);
                adapter.notifyDataSetChanged();
            });
        } else if (id == R.id.saveCombo) {
            saveCombo();
        }
    }

    private void saveCombo() {
        String name = text(binding.comboName);
        String price = text(binding.comboPrice);
        String error = ComboValidator.validateCombo(name, price, drafts.size());
        if (error != null) {
            Toast.makeText(activity, ComboItemPicker.mapError(activity, error), Toast.LENGTH_SHORT).show();
            return;
        }
        ComboResponse combo = new ComboResponse();
        combo.setComboName(name);
        combo.setComboCode(text(binding.comboCode));
        combo.setComboPrice(price);
        combo.setComboCGST(text(binding.comboCGST));
        combo.setComboSGST(text(binding.comboSGST));
        combo.setComboActiveStatus(binding.comboActiveSwitch.isChecked() ? "1" : "0");
        combo.setComboDeletedStatus("0");
        combo.setComboStatus("0");

        String comboId = editingComboId;
        if (comboId == null) {
            combo.setComboNetworkStatus(randomKey());
            long id = posBillingWalaDatabase.insertCombo(combo);
            if (id <= 0) {
                Toast.makeText(activity, getString(R.string.ui_combo_save_failed), Toast.LENGTH_SHORT).show();
                return;
            }
            comboId = String.valueOf(id);
        } else {
            ComboResponse existing = posBillingWalaDatabase.getComboDetail(comboId);
            if (existing != null) {
                combo.setComboNetworkStatus(existing.getComboNetworkStatus());
                combo.setComboSortOrder(existing.getComboSortOrder());
            }
            posBillingWalaDatabase.updateCombo(comboId, combo.getComboName(), combo.getComboCode(),
                    combo.getComboPrice(), combo.getComboCGST(), combo.getComboSGST(),
                    combo.getComboActiveStatus(), 0);
        }
        posBillingWalaDatabase.saveComboItems(comboId, drafts);
        Toast.makeText(activity, getString(R.string.ui_combo_saved), Toast.LENGTH_SHORT).show();
        goBack();
    }

    private String text(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private String randomKey() {
        String chars = "0123456789qwertyuiopasdfghjklzxcvbnm";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void goBack() {
        ((MainActivity) activity).navigateBack();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
    }
}
