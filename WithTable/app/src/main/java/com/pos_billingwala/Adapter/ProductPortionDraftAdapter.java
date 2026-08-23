package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Model.ProductPortionDraft;
import com.pos_billingwala.databinding.PortionListBinding;

import java.util.List;

@SuppressLint("SetTextI18n")
public class ProductPortionDraftAdapter extends RecyclerView.Adapter<ProductPortionDraftAdapter.ViewHolder> {

    public interface Listener {
        void onRemove(int position);
    }

    private final Context context;
    private final List<ProductPortionDraft> drafts;
    private final Listener listener;

    public ProductPortionDraftAdapter(Context context, List<ProductPortionDraft> drafts, Listener listener) {
        this.context = context;
        this.drafts = drafts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(PortionListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductPortionDraft item = drafts.get(position);
        holder.binding.srNo.setText(String.valueOf(position + 1));
        holder.binding.portionName.setText(item.getPortionName());
        holder.binding.portionPrice.setText(MainActivity.currencyName + " " + item.getPortionPrice());
        holder.binding.portionEdit.setVisibility(android.view.View.GONE);
        holder.binding.portionRemove.setOnClickListener(v -> {
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        final PortionListBinding binding;

        ViewHolder(PortionListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
