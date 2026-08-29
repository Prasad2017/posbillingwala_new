package com.posbillingwala.dealer.Adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.dealer.Model.CatalogImportHistoryItem;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.databinding.ItemCatalogImportHistoryBinding;

import java.util.List;

public class CatalogImportHistoryAdapter extends RecyclerView.Adapter<CatalogImportHistoryAdapter.ViewHolder> {

    private final Activity activity;
    private final List<CatalogImportHistoryItem> items;

    public CatalogImportHistoryAdapter(Activity activity, List<CatalogImportHistoryItem> items) {
        this.activity = activity;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCatalogImportHistoryBinding binding = ItemCatalogImportHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CatalogImportHistoryItem item = items.get(position);
        holder.binding.historyDate.setText(safe(item.getCreatedAt()));
        holder.binding.historyType.setText(formatType(item.getImportType()) + " · " + safe(item.getTotalRows()) + " rows");
        holder.binding.historyFile.setText(safe(item.getFileName()));
        holder.binding.historySummary.setText(
                "Created: " + safe(item.getCreatedCount())
                        + "  Updated: " + safe(item.getUpdatedCount())
                        + "  Errors: " + safe(item.getErrorRows()));
        holder.binding.historyStatus.setText(safe(item.getStatus()));
        int color = R.color.colorPrimary;
        if ("failed".equalsIgnoreCase(item.getStatus())) {
            color = R.color.red;
        } else if ("validated".equalsIgnoreCase(item.getStatus())) {
            color = android.R.color.holo_orange_dark;
        }
        holder.binding.historyStatus.setTextColor(ContextCompat.getColor(activity, color));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String value) {
        return value != null ? value : "-";
    }

    private String formatType(String type) {
        if (type == null) {
            return "Import";
        }
        switch (type) {
            case "categories":
                return "Categories";
            case "subcategories":
                return "Sub Categories";
            case "portions":
                return "Portions";
            case "products":
                return "Products";
            default:
                return type;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCatalogImportHistoryBinding binding;

        ViewHolder(ItemCatalogImportHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
