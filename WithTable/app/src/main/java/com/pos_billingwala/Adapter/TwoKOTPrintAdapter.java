package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.databinding.TwoInchKotPrinterProductListBinding;

import java.util.List;

public class TwoKOTPrintAdapter extends RecyclerView.Adapter<TwoKOTPrintAdapter.MyViewHolder> {

    Context context;
    List<ProductCartResponse> productCartResponseList;

    public TwoKOTPrintAdapter(Context context, List<ProductCartResponse> productCartResponseList) {
        this.context = context;
        this.productCartResponseList = productCartResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(TwoInchKotPrinterProductListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        ProductCartResponse productCartResponse = productCartResponseList.get(position);

        float productPrice = Float.parseFloat(productCartResponse.getResolvedLinePrice());
        float productQuantity = Float.parseFloat(productCartResponse.getProductQuantity());
        float totalCGST = 0f, totalSGST = 0f;
        if (!productCartResponse.getProductCGST().equalsIgnoreCase("")) {
            totalCGST += Float.parseFloat(productCartResponse.getProductCGST());
        }
        if (!productCartResponse.getProductSGST().equalsIgnoreCase("")) {
            totalSGST += Float.parseFloat(productCartResponse.getProductSGST());
        }
        float totalPerProductGST = productPrice + (productPrice * ((totalCGST + totalSGST) / 100));

        holder.binding.productName.setText(productCartResponse.getDisplayLineName());
        holder.binding.productQuantity.setText("X" + productCartResponse.getProductQuantity());

    }

    @Override
    public int getItemCount() {
        return productCartResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        final TwoInchKotPrinterProductListBinding binding;

        public MyViewHolder(@NonNull TwoInchKotPrinterProductListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

