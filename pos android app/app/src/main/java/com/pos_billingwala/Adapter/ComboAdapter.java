package com.pos_billingwala.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Fragment.ComboMaster;
import com.pos_billingwala.Fragment.UpdateCombo;
import com.pos_billingwala.Model.ComboItemResponse;
import com.pos_billingwala.Model.ComboResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ComboListBinding;

import java.util.List;

public class ComboAdapter extends RecyclerView.Adapter<ComboAdapter.Holder> {

    private final Context context;
    private final List<ComboResponse> combos;
    private POSBillingWalaDatabase database;

    public ComboAdapter(Context context, List<ComboResponse> combos) {
        this.context = context;
        this.combos = combos;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ComboListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ComboResponse combo = combos.get(position);
        database = new POSBillingWalaDatabase(context);
        holder.binding.comboName.setText(Html.fromHtml("<b>Combo</b>: " + nullToDash(combo.getComboName())));
        holder.binding.comboCode.setText(Html.fromHtml("<b>Code</b>: " + nullToDash(combo.getComboCode())));
        holder.binding.comboPrice.setText(Html.fromHtml("<b>Price</b>: "
                + MainActivity.currencyName + " " + nullToDash(combo.getComboPrice())));
        holder.binding.comboGst.setText(Html.fromHtml("<b>CGST/SGST</b>: "
                + nullToDash(combo.getComboCGST()) + " / " + nullToDash(combo.getComboSGST())));

        List<ComboItemResponse> items = database.getComboItemList(combo.getComboId());
        StringBuilder itemText = new StringBuilder("<b>Items</b>:<br/>");
        if (items.isEmpty()) {
            itemText.append("-");
        } else {
            for (ComboItemResponse item : items) {
                itemText.append("• ").append(item.getDisplayLabel()).append("<br/>");
            }
        }
        holder.binding.comboItems.setText(Html.fromHtml(itemText.toString()));
        boolean active = combo.isActive();
        holder.binding.comboStatus.setText(Html.fromHtml("<b>Status</b>: "
                + (active ? context.getString(R.string.ui_combo_enabled) : context.getString(R.string.ui_combo_disabled))));
        holder.binding.toggleCombo.setText(active
                ? context.getString(R.string.ui_disable_combo)
                : context.getString(R.string.ui_enable_combo));

        holder.binding.toggleCombo.setOnClickListener(v -> {
            database.setComboActiveStatus(combo.getComboId(), !active);
            ComboMaster.getComboList();
        });
        holder.binding.deleteCombo.setOnClickListener(v -> confirmDelete(combo.getComboId()));
        holder.binding.updateCombo.setOnClickListener(v -> {
            UpdateCombo updateCombo = new UpdateCombo();
            Bundle bundle = new Bundle();
            bundle.putString("comboId", combo.getComboId());
            if (ComboMaster.openedFromMaster) {
                bundle.putString("openedFrom", com.pos_billingwala.Fragment.MasterData.OPENED_FROM_MASTER);
            }
            updateCombo.setArguments(bundle);
            ((MainActivity) context).loadFragment(updateCombo, true);
        });
    }

    private void confirmDelete(String comboId) {
        BottomSheetUi.showConfirm(
                context,
                context.getString(R.string.toast_are_you_sure),
                context.getString(R.string.ui_combo_delete_confirm),
                "YES",
                "NO",
                true,
                () -> {
                    database.deleteCombo(comboId);
                    Toast.makeText(context, context.getString(R.string.ui_combo_deleted), Toast.LENGTH_SHORT).show();
                    ComboMaster.getComboList();
                });
    }

    private String nullToDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    @Override
    public int getItemCount() {
        return combos.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ComboListBinding binding;

        Holder(ComboListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
