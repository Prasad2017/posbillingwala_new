package com.posbillingwala.admin.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Model.ProductPortionDraft;
import com.posbillingwala.admin.databinding.ProductPortionDraftListBinding;

import java.util.List;

public class ProductPortionDraftAdapter extends RecyclerView.Adapter<ProductPortionDraftAdapter.MyViewHolder> {

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
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ProductPortionDraftListBinding binding = ProductPortionDraftListBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductPortionDraft item = drafts.get(position);
        holder.binding.srNo.setText(String.valueOf(position + 1));
        holder.binding.portionName.setText(item.getPortionName());
        holder.binding.portionPrice.setText(MainActivity.currency + " " + item.getPortionPrice());
        holder.binding.portionRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onRemove(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return drafts.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final ProductPortionDraftListBinding binding;

        public MyViewHolder(@NonNull ProductPortionDraftListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
