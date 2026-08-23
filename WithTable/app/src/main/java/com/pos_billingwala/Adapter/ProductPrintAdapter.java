package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.databinding.ProductPrintListBinding;

import java.util.ArrayList;
import java.util.List;

public class ProductPrintAdapter extends RecyclerView.Adapter<ProductPrintAdapter.MyViewHolder> {

    public List<ProductResponse> productResponseList = new ArrayList<>();
    Context context;

    public ProductPrintAdapter(Context context, List<ProductResponse> productResponseList) {
        this.context = context;
        this.productResponseList = productResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(ProductPrintListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        ProductResponse productResponse = productResponseList.get(position);

        holder.binding.productCode.setText(!productResponse.getProductCode().equals("") ? productResponse.getProductCode() : "-");
        holder.binding.productName.setText(productResponse.getProductName());
        holder.binding.productPrice.setText(MainActivity.currencyName + " " + productResponse.getProductPrice());

    }

    @Override
    public int getItemCount() {
        return productResponseList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public final ProductPrintListBinding binding;

        public MyViewHolder(ProductPrintListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
