package com.posbillingwala.owner.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Model.InvoiceProductResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.databinding.SalesProductListBinding; // Import the generated binding class

import java.util.List;
import java.util.Locale;

public class InvoiceProductAdapter extends RecyclerView.Adapter<InvoiceProductAdapter.MyViewHolder> {

    private final Context context;
    private final List<InvoiceProductResponse> invoiceProductResponseList;

    public InvoiceProductAdapter(Context context, List<InvoiceProductResponse> invoiceProductResponseList) {
        this.context = context;
        this.invoiceProductResponseList = invoiceProductResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use ViewBinding to inflate the layout
        SalesProductListBinding binding = SalesProductListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        InvoiceProductResponse invoiceProductResponse = invoiceProductResponseList.get(position);

        float productPrice = Float.parseFloat(invoiceProductResponse.getProductPrice());
        float productQuantity = Float.parseFloat(invoiceProductResponse.getProductQuantity());

        holder.binding.productName.setText(invoiceProductResponse.getProductName());
        holder.binding.productQuantity.setText(invoiceProductResponse.getProductQuantity());
        holder.binding.productRate.setText(String.format(Locale.US, "%.2f", productPrice));

        float totalPerProductAmount = (productPrice * productQuantity);
        holder.binding.productAmount.setText(String.format(Locale.US, "%.2f", totalPerProductAmount));
    }

    @Override
    public int getItemCount() {
        return invoiceProductResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final SalesProductListBinding binding;

        public MyViewHolder(@NonNull SalesProductListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
