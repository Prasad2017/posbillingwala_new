package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Model.InventoryResponse;
import com.pos_billingwala.databinding.InventoryListBinding;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.MyViewHolder> {

    Context context;
    List<InventoryResponse> inventoryResponseList;

    public InventoryAdapter(Context context, List<InventoryResponse> inventoryResponseList) {
        this.context = context;
        this.inventoryResponseList = inventoryResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(InventoryListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        InventoryResponse inventoryResponse = inventoryResponseList.get(position);

        holder.binding.srNo.setText("" + (position + 1));
        holder.binding.productName.setText(inventoryResponse.getProductName());
        holder.binding.inventoryQty.setText(inventoryResponse.getProductInventoryQuantity());
        holder.binding.afterSaleInventoryQty.setText(inventoryResponse.getAfterSaleInventoryQuantity());
        holder.binding.saleInventoryQty.setText(inventoryResponse.getSaleInventoryQuantity());

    }

    @Override
    public int getItemCount() {
        return inventoryResponseList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        InventoryListBinding binding;

        public MyViewHolder(InventoryListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
