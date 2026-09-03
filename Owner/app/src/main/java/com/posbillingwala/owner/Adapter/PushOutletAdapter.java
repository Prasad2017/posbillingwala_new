package com.posbillingwala.owner.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Model.LicenseResponse;
import com.posbillingwala.owner.databinding.ItemPushOutletBinding;

import java.util.ArrayList;
import java.util.List;

public class PushOutletAdapter extends RecyclerView.Adapter<PushOutletAdapter.Holder> {

    public static class OutletChoice {
        public final LicenseResponse license;
        public boolean selected;
        public boolean enabled = true;

        OutletChoice(LicenseResponse license, boolean selected) {
            this.license = license;
            this.selected = selected;
        }
    }

    private final List<OutletChoice> items = new ArrayList<>();

    public void setOutlets(List<LicenseResponse> licenses, String disableBranchId) {
        items.clear();
        if (licenses != null) {
            for (LicenseResponse license : licenses) {
                boolean sameAsSource = disableBranchId != null
                        && disableBranchId.equals(license.getLicensesId());
                OutletChoice choice = new OutletChoice(license, !sameAsSource);
                choice.enabled = !sameAsSource;
                if (sameAsSource) {
                    choice.selected = false;
                }
                items.add(choice);
            }
        }
        notifyDataSetChanged();
    }

    public void setAllSelected(boolean selected) {
        for (OutletChoice item : items) {
            if (item.enabled) {
                item.selected = selected;
            }
        }
        notifyDataSetChanged();
    }

    public List<String> selectedIds() {
        List<String> ids = new ArrayList<>();
        for (OutletChoice item : items) {
            if (item.selected && item.license.getLicensesId() != null) {
                ids.add(item.license.getLicensesId());
            }
        }
        return ids;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemPushOutletBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        OutletChoice item = items.get(position);
        String label = item.license.getBranchLabel();
        if (label == null || label.trim().isEmpty()) {
            label = item.license.getShopName1();
        }
        if (label == null || label.trim().isEmpty()) {
            label = "Outlet " + item.license.getLicensesId();
        }
        holder.binding.outletCheck.setOnCheckedChangeListener(null);
        holder.binding.outletCheck.setText(label);
        holder.binding.outletCheck.setEnabled(item.enabled);
        holder.binding.outletCheck.setChecked(item.selected);
        holder.binding.outletCheck.setOnCheckedChangeListener((buttonView, isChecked) -> item.selected = isChecked);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemPushOutletBinding binding;

        Holder(ItemPushOutletBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
