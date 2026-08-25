package com.posbillingwala.admin.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Model.LicenseResponse;
import com.posbillingwala.admin.databinding.ItemBranchCardBinding;

import java.util.List;

public class BranchCardAdapter extends RecyclerView.Adapter<BranchCardAdapter.Holder> {

    private final List<LicenseResponse> items;

    public BranchCardAdapter(List<LicenseResponse> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBranchCardBinding binding = ItemBranchCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        LicenseResponse lic = items.get(position);
        String branch = lic.getBranchLabel();
        if (branch == null || branch.isEmpty()) {
            branch = "owner".equalsIgnoreCase(lic.getUserType()) ? "Main Store" : "Franchise Branch";
        }
        holder.binding.branchName.setText(branch);

        StringBuilder meta = new StringBuilder();
        meta.append("License: ").append(nullToDash(lic.getLicenseKey()));
        if (lic.getPhoneNo1() != null && !lic.getPhoneNo1().trim().isEmpty()) {
            meta.append("\nPhone: ").append(lic.getPhoneNo1().trim());
            if (lic.getPhoneNo2() != null && !lic.getPhoneNo2().trim().isEmpty()) {
                meta.append(", ").append(lic.getPhoneNo2().trim());
            }
        }
        if (lic.getCompanyAddress() != null && !lic.getCompanyAddress().trim().isEmpty()) {
            meta.append("\nAddress: ").append(lic.getCompanyAddress().trim());
        }
        String type = lic.getUserType() != null ? lic.getUserType() : "";
        meta.append("\nType: ").append(type.isEmpty() ? "-" : type);
        holder.binding.branchMeta.setText(meta.toString());
    }

    private static String nullToDash(String v) {
        return v == null || v.trim().isEmpty() ? "-" : v.trim();
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemBranchCardBinding binding;

        Holder(ItemBranchCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
