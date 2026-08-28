package com.posbillingwala.admin.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Model.DeviceMonitorResponse;
import com.posbillingwala.admin.databinding.ItemSimpleCardBinding;

import java.util.List;

public class DeviceMonitorAdapter extends RecyclerView.Adapter<DeviceMonitorAdapter.Holder> {

    private final List<DeviceMonitorResponse> items;

    public DeviceMonitorAdapter(List<DeviceMonitorResponse> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemSimpleCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        DeviceMonitorResponse d = items.get(position);
        String title = (d.getShopName() != null ? d.getShopName() : "Customer")
                + " · " + (d.getAndroidDeviceName() != null && !d.getAndroidDeviceName().isEmpty()
                ? d.getAndroidDeviceName() : "Device");
        holder.binding.title.setText(title);
        holder.binding.meta.setText(
                "Branch: " + nz(d.getBranchLabel())
                        + "\nLicense: " + nz(d.getLicenseKey())
                        + "\nDevice ID: " + nz(d.getAndroidDeviceId())
                        + "\nOwner: " + nz(d.getOwnerName())
                        + " · " + nz(d.getContactNumber())
                        + "\nBound: " + nz(d.getDeviceBoundAt())
                        + "\nLast seen: " + (d.getLastSeenLabel() != null ? d.getLastSeenLabel() : nz(d.getLastSeenAt()))
                        + "\nLast login: " + nz(d.getLastLoginAt())
                        + "\nExpiry: " + nz(d.getExpiryDate()));
        String status = d.getConnectionStatus() != null ? d.getConnectionStatus() : "OFFLINE";
        holder.binding.status.setText(status);
        holder.binding.status.setTextColor(d.isOnline()
                ? holder.itemView.getContext().getColor(android.R.color.holo_green_dark)
                : holder.itemView.getContext().getColor(android.R.color.darker_gray));
    }

    private static String nz(String v) {
        return v == null || v.isEmpty() ? "-" : v;
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemSimpleCardBinding binding;

        Holder(ItemSimpleCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
