package com.posbillingwala.admin.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Model.LicenseResponse;
import com.posbillingwala.admin.databinding.ItemModuleCardBinding;

import java.util.List;

public class ModuleCardAdapter extends RecyclerView.Adapter<ModuleCardAdapter.Holder> {

    private final List<LicenseResponse> items;

    public ModuleCardAdapter(List<LicenseResponse> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemModuleCardBinding binding = ItemModuleCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        LicenseResponse lic = items.get(position);
        String branch = lic.getBranchLabel();
        if (branch == null || branch.isEmpty()) {
            branch = "owner".equalsIgnoreCase(lic.getUserType()) ? "Main Store" : "Branch";
        }
        holder.binding.moduleBranch.setText(branch + " · " + nullToDash(lic.getLicenseKey()));

        holder.binding.moduleFlags.setText(
                flag("Fast Billing", lic.getFastBilling()) + "\n"
                        + flag("Dine-In", lic.getDineIn()) + "\n"
                        + flag("Takeaway", lic.getTakeAway()) + "\n"
                        + flag("Mess", lic.getMess()));
    }

    private static String flag(String label, String value) {
        boolean on = "1".equals(value) || "true".equalsIgnoreCase(value);
        return label + ": " + (on ? "ON" : "OFF");
    }

    private static String nullToDash(String v) {
        return v == null || v.trim().isEmpty() ? "-" : v.trim();
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemModuleCardBinding binding;

        Holder(ItemModuleCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
