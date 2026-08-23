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

public class InvoiceProductAdapter extends RecyclerView.Adapter<InvoiceProductAdapter.MyViewHolder> {

    Context context;
    List<InvoiceProductResponse> invoiceProductResponseList;

    public InvoiceProductAdapter(Context context, List<InvoiceProductResponse> invoiceProductResponseList) {
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
        InvoiceProductResponse invoiceProductResponse = invoiceProductResponseList.get(position);

        float productPrice = Float.parseFloat(invoiceProductResponse.getResolvedLinePrice());
        float productQuantity = Float.parseFloat(invoiceProductResponse.getProductQuantity());

        holder.binding.productName.setText(invoiceProductResponse.getDisplayLineName());
        holder.binding.productQuantity.setText("X" + invoiceProductResponse.getProductQuantity());
        holder.binding.productRate.setText(String.format(Locale.US, "%.2f", productPrice));
        float totalPerProductAmount = (productPrice * productQuantity);
        holder.binding.productAmount.setText(String.format(Locale.US, "%.2f", totalPerProductAmount));

    }

    @Override
    public int getItemCount() {
        return invoiceProductResponseList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public final TwoInchPrinterProductListBinding binding;

        public MyViewHolder(TwoInchPrinterProductListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
