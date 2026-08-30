package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.databinding.ProductPrintListBinding;

import java.util.ArrayList;
import java.util.List;

public class ProductPrintAdapter extends RecyclerView.Adapter<ProductPrintAdapter.MyViewHolder> {

    public static class ProductPrintRow {
        public final String productCode;
        public final String productName;
        public final String portionName;
        public final String price;

        public ProductPrintRow(String productCode, String productName, String portionName, String price) {
            this.productCode = productCode;
            this.productName = productName;
            this.portionName = portionName;
            this.price = price;
        }
    }

    public List<ProductPrintRow> productPrintRows = new ArrayList<>();
    Context context;

    public ProductPrintAdapter(Context context, List<ProductPrintRow> productPrintRows) {
        this.context = context;
        this.productPrintRows = productPrintRows != null ? productPrintRows : new ArrayList<>();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(ProductPrintListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductPrintRow row = productPrintRows.get(position);

        holder.binding.productCode.setText(!TextUtils.isEmpty(row.productCode) ? row.productCode : "-");
        holder.binding.productName.setText(!TextUtils.isEmpty(row.productName) ? row.productName : "-");
        holder.binding.productPortion.setText(!TextUtils.isEmpty(row.portionName) ? row.portionName : "-");
        String price = !TextUtils.isEmpty(row.price) ? row.price : "0.00";
        holder.binding.productPrice.setText(MainActivity.currencyName + " " + price);
    }

    @Override
    public int getItemCount() {
        return productPrintRows.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public final ProductPrintListBinding binding;

        public MyViewHolder(ProductPrintListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
