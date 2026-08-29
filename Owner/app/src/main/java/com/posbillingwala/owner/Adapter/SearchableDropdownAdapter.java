package com.posbillingwala.owner.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchableDropdownAdapter extends RecyclerView.Adapter<SearchableDropdownAdapter.OptionViewHolder> {

    public interface OnOptionClickListener {
        void onOptionClick(int originalIndex, String label);
    }

    private final List<String> allItems;
    private final List<String> filteredItems = new ArrayList<>();
    private final List<Integer> filteredOriginalIndexes = new ArrayList<>();
    private final OnOptionClickListener listener;

    public SearchableDropdownAdapter(List<String> items, OnOptionClickListener listener) {
        this.allItems = new ArrayList<>(items);
        this.listener = listener;
        resetFilter();
    }

    public void filter(String query) {
        filteredItems.clear();
        filteredOriginalIndexes.clear();

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        if (normalizedQuery.isEmpty()) {
            for (int i = 0; i < allItems.size(); i++) {
                filteredItems.add(allItems.get(i));
                filteredOriginalIndexes.add(i);
            }
        } else {
            for (int i = 0; i < allItems.size(); i++) {
                String item = allItems.get(i);
                if (item != null && item.toLowerCase(Locale.getDefault()).contains(normalizedQuery)) {
                    filteredItems.add(item);
                    filteredOriginalIndexes.add(i);
                }
            }
        }
        notifyDataSetChanged();
    }

    public int getFilteredCount() {
        return filteredItems.size();
    }

    private void resetFilter() {
        filter("");
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_searchable_dropdown_option, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        String label = filteredItems.get(position);
        holder.label.setText(label);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOptionClick(filteredOriginalIndexes.get(position), label);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    static class OptionViewHolder extends RecyclerView.ViewHolder {
        final TextView label;

        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.optionLabel);
        }
    }
}
