package com.posbillingwala.admin.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Model.ComboResponse;
import com.posbillingwala.admin.databinding.ItemSimpleCardBinding;

import java.util.List;

public class ComboAdapter extends RecyclerView.Adapter<ComboAdapter.Holder> {

    private final List<ComboResponse> items;

    public ComboAdapter(List<ComboResponse> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemSimpleCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ComboResponse c = items.get(position);
        holder.binding.title.setText(c.getComboName() != null ? c.getComboName() : "Combo");
        boolean active = !"1".equals(c.getComboDeletedStatus()) && !"0".equals(c.getComboActiveStatus());
        holder.binding.meta.setText("Price: ₹ " + (c.getComboPrice() != null ? c.getComboPrice() : "0")
                + (c.getComboCode() != null && !c.getComboCode().isEmpty() ? "\nCode: " + c.getComboCode() : ""));
        holder.binding.status.setText(active ? "ACTIVE" : "INACTIVE");
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
