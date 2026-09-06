package com.posbillingwala.owner.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Extra.ReportUiHelper;
import com.posbillingwala.owner.Model.OwnerReportListItem;
import com.posbillingwala.owner.databinding.ItemReportRankRowBinding;

import java.util.ArrayList;
import java.util.List;

public class OwnerReportAdapter extends RecyclerView.Adapter<OwnerReportAdapter.Holder> {

    private final List<OwnerReportListItem> items = new ArrayList<>();
    private boolean showMoney = true;

    public void setItems(List<OwnerReportListItem> list, boolean moneyValue) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        showMoney = moneyValue;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemReportRankRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        OwnerReportListItem item = items.get(position);
        String title = item.getTitle() != null ? item.getTitle() : "—";
        holder.binding.rankTitle.setText(title);
        holder.binding.rankInitials.setText(initials(title));
        String sub = item.getSubtitle() != null ? item.getSubtitle() : "";
        holder.binding.rankSubtitle.setText(sub);
        holder.binding.rankSubtitle.setVisibility(sub.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
        if (showMoney) {
            holder.binding.rankValue.setText(ReportUiHelper.money(item.getAmount()));
        } else {
            holder.binding.rankValue.setText(item.getAmount() != null ? item.getAmount() : "0");
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String initials(String title) {
        String t = title.trim();
        if (t.isEmpty()) return "?";
        String[] parts = t.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        String a = parts[0].substring(0, 1);
        String b = parts[1].substring(0, 1);
        return (a + b).toUpperCase();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemReportRankRowBinding binding;

        Holder(ItemReportRankRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
