package com.posbillingwala.admin.Adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Extra.LicenseStatusHelper;
import com.posbillingwala.admin.Model.LicenseResponse;
import com.posbillingwala.admin.databinding.ItemDeviceCardBinding;

import java.util.List;

public class DeviceCardAdapter extends RecyclerView.Adapter<DeviceCardAdapter.Holder> {

    private final List<LicenseResponse> items;

    public DeviceCardAdapter(List<LicenseResponse> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDeviceCardBinding binding = ItemDeviceCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        LicenseResponse lic = items.get(position);
        String deviceName = lic.getAndroidDeviceName();
        if (deviceName == null || deviceName.trim().isEmpty()) {
            deviceName = "Bound Device";
        }
        holder.binding.deviceTitle.setText(deviceName);

        String branch = lic.getBranchLabel();
        if (branch == null || branch.isEmpty()) {
            branch = "owner".equalsIgnoreCase(lic.getUserType()) ? "Main Store" : "Branch";
        }

        String meta = "Branch: " + branch
                + "\nLicense: " + nullToDash(lic.getLicenseKey())
                + "\nDevice ID: " + nullToDash(lic.getAndroidDeviceId());
        holder.binding.deviceMeta.setText(meta);

        String status = LicenseStatusHelper.displayStatus(lic);
        holder.binding.deviceStatus.setText(status);
        holder.binding.deviceStatus.setBackgroundTintList(
                ColorStateList.valueOf(LicenseStatusHelper.badgeColor(status)));
    }

    private static String nullToDash(String v) {
        return v == null || v.trim().isEmpty() ? "-" : v.trim();
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemDeviceCardBinding binding;

        Holder(ItemDeviceCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
