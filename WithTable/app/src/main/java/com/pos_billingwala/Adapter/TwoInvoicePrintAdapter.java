package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.databinding.TwoInchPrinterProductListBinding;

import java.util.List;
import java.util.Locale;


public class TwoInvoicePrintAdapter extends RecyclerView.Adapter<TwoInvoicePrintAdapter.MyViewHolder> {

    Context context;
    List<InvoiceProductResponse> invoiceProductResponseList;

    public TwoInvoicePrintAdapter(Context context, List<InvoiceProductResponse> invoiceProductResponseList) {
        this.context = context;
        this.invoiceProductResponseList = invoiceProductResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(TwoInchPrinterProductListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        InvoiceProductResponse productCartResponse = invoiceProductResponseList.get(position);

        float productPrice = Float.parseFloat(productCartResponse.getResolvedLinePrice());
        float productQuantity = Float.parseFloat(productCartResponse.getProductQuantity());

        holder.binding.productName.setText(productCartResponse.getDisplayLineName());
        holder.binding.productQuantity.setText("X" + productCartResponse.getProductQuantity());
        holder.binding.productRate.setText(String.format(Locale.US, "%.2f", productPrice));
        float totalPerProductAmount = (productPrice * productQuantity);
        holder.binding.productAmount.setText(String.format(Locale.US, "%.2f", totalPerProductAmount));

    }

    @Override
    public int getItemCount() {
        return invoiceProductResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TwoInchPrinterProductListBinding binding;

        public MyViewHolder(@NonNull TwoInchPrinterProductListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
