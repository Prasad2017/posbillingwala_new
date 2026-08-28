package com.posbillingwala.admin.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Fragment.DealerReport;
import com.posbillingwala.admin.Model.DealerPerformance;
import com.posbillingwala.admin.databinding.ItemDealerPerformanceBinding;

import java.util.ArrayList;
import java.util.List;

public class DealerPerformanceAdapter extends RecyclerView.Adapter<DealerPerformanceAdapter.Holder> {

    private final Context context;
    private final List<DealerPerformance> items = new ArrayList<>();

    public DealerPerformanceAdapter(Context context) {
        this.context = context;
    }

    public void submit(List<DealerPerformance> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDealerPerformanceBinding binding = ItemDealerPerformanceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        DealerPerformance item = items.get(position);
        holder.binding.dealerName.setText(item.getDealerName());
        holder.binding.dealerMeta.setText(
                "Customers: " + item.getTotalCustomers()
                        + "  ·  Active: " + item.getActiveCustomers()
                        + "  ·  Licenses: " + item.getActiveLicenses());
        holder.itemView.setOnClickListener(v -> {
            DealerReport report = new DealerReport();
            Bundle bundle = new Bundle();
            bundle.putString("dealerId", item.getDealerId());
            bundle.putString("dealerName", item.getDealerName());
            report.setArguments(bundle);
            ((MainActivity) context).loadFragment(report, true);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemDealerPerformanceBinding binding;

        Holder(ItemDealerPerformanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
