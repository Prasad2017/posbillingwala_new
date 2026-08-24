package com.pos_billingwala.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Model.ComboItemDraft;
import com.pos_billingwala.databinding.ComboItemListBinding;

import java.util.List;

public class ComboItemDraftAdapter extends RecyclerView.Adapter<ComboItemDraftAdapter.Holder> {

    public interface Listener {
        void onRemove(int position);
    }

    private final List<ComboItemDraft> drafts;
    private final Listener listener;

    public ComboItemDraftAdapter(List<ComboItemDraft> drafts, Listener listener) {
        this.drafts = drafts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ComboItemListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ComboItemDraft draft = drafts.get(position);
        holder.binding.comboItemLabel.setText(draft.getDisplayLabel());
        holder.binding.comboItemRemove.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onRemove(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return drafts.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ComboItemListBinding binding;

        Holder(ComboItemListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
