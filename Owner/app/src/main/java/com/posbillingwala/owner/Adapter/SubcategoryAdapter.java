package com.posbillingwala.owner.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Model.ProductSubcategoryResponse;
import com.posbillingwala.owner.databinding.SubcategoryListBinding;

import java.util.List;

public class SubcategoryAdapter extends RecyclerView.Adapter<SubcategoryAdapter.MyViewHolder> {

    private final Context context;
    private final List<ProductSubcategoryResponse> subcategoryList;

    public SubcategoryAdapter(Context context, List<ProductSubcategoryResponse> subcategoryList) {
        this.context = context;
        this.subcategoryList = subcategoryList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SubcategoryListBinding binding = SubcategoryListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductSubcategoryResponse item = subcategoryList.get(position);
        holder.binding.srNo.setText(String.valueOf(position + 1));
        holder.binding.subcategoryName.setText(item.getSubcategoryName());
    }

    @Override
    public int getItemCount() {
        return subcategoryList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private final SubcategoryListBinding binding;

        MyViewHolder(@NonNull SubcategoryListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
