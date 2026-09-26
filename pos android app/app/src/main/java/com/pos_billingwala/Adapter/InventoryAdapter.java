package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Extra.RowDividerUi;
import com.pos_billingwala.Model.InventoryResponse;
import com.pos_billingwala.R;
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
        String name = inventoryResponse.getProductName();
        if (name == null || name.trim().isEmpty()) {
            name = "Product " + (inventoryResponse.getProductId() != null
                    ? inventoryResponse.getProductId() : "");
        }
        holder.binding.productName.setText(name.trim());
        holder.binding.productName.setVisibility(View.VISIBLE);
        holder.binding.productName.setTextColor(
                ContextCompat.getColor(context, R.color.colorTextPrimary));
        holder.binding.productName.setAlpha(1f);
        holder.binding.inventoryQty.setText(formatQty(inventoryResponse.getProductInventoryQuantity()));
        holder.binding.afterSaleInventoryQty.setText(formatQty(inventoryResponse.getAfterSaleInventoryQuantity()));
        holder.binding.saleInventoryQty.setText(formatQty(inventoryResponse.getSaleInventoryQuantity()));

        float remaining = parseQty(inventoryResponse.getAfterSaleInventoryQuantity());
        int stockColor = ContextCompat.getColor(context,
                remaining <= 0 ? R.color.statusExpired
                        : remaining <= 5 ? R.color.table_status_bill_requested
                        : R.color.green_600);
        holder.binding.afterSaleInventoryQty.setTextColor(stockColor);

        RowDividerUi.bindLastItem(holder.binding.rowDivider, position, getItemCount());
    }

    private static float parseQty(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0f;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private static String formatQty(String value) {
        float qty = parseQty(value);
        return String.format(java.util.Locale.US, "%.3f", qty);
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
