package com.posbillingwala.owner.Adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Model.BranchComparisonResponse;
import com.posbillingwala.owner.databinding.BranchComparisonItemBinding;

import java.util.List;

@SuppressLint("SetTextI18n")
public class BranchComparisonAdapter extends RecyclerView.Adapter<BranchComparisonAdapter.ViewHolder> {

    private final List<BranchComparisonResponse> items;

    public BranchComparisonAdapter(List<BranchComparisonResponse> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BranchComparisonItemBinding binding = BranchComparisonItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BranchComparisonResponse row = items.get(position);
        String label = row.getBranchLabel() != null ? row.getBranchLabel() : "Branch";
        holder.binding.branchLabel.setText(label);

        String currency = row.getCurrencyName() != null ? row.getCurrencyName() : "₹";
        String device = "1".equals(row.getDeviceBound())
                ? (row.getAndroidDeviceName() != null ? row.getAndroidDeviceName() : "Bound")
                : "Offline / not bound";

        String metrics = "Total: " + currency + " " + row.getTotalSale()
                + "\nToday: " + currency + " " + row.getTodaySale()
                + "\nBills: " + row.getBillCount() + " (today: " + row.getTodayBillCount() + ")"
                + "\nAvg bill: " + currency + " " + row.getAvgBillAmount()
                + "\nDevice: " + device;

        holder.binding.metrics.setText(metrics);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final BranchComparisonItemBinding binding;

        ViewHolder(BranchComparisonItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
