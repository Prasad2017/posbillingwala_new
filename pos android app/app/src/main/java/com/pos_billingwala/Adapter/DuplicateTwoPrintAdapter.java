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


@SuppressLint("SetTextI18n, NotifyDataSetChanged")
public class DuplicateTwoPrintAdapter extends RecyclerView.Adapter<DuplicateTwoPrintAdapter.MyViewHolder> {

    Context context;
    List<InvoiceProductResponse> invoiceProductResponseList;

    public DuplicateTwoPrintAdapter(Context context, List<InvoiceProductResponse> invoiceProductResponseList) {
        this.context = context;
        this.invoiceProductResponseList = invoiceProductResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(TwoInchPrinterProductListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        InvoiceProductResponse invoiceProductResponse = invoiceProductResponseList.get(position);

        float productPrice = Float.parseFloat(invoiceProductResponse.getResolvedLinePrice());
        float productQuantity = Float.parseFloat(invoiceProductResponse.getProductQuantity());
        float totalCGST = 0f, totalSGST = 0f;
        if (!invoiceProductResponse.getProductCGST().equalsIgnoreCase("")) {
            totalCGST += Float.parseFloat(invoiceProductResponse.getProductCGST());
        }
        if (!invoiceProductResponse.getProductSGST().equalsIgnoreCase("")) {
            totalSGST += Float.parseFloat(invoiceProductResponse.getProductSGST());
        }
        float totalPerProductGST = productPrice + (productPrice * ((totalCGST + totalSGST) / 100));

        holder.binding.productName.setText(invoiceProductResponse.getDisplayLineName());
        holder.binding.productRate.setText("X" + invoiceProductResponse.getProductQuantity());
        holder.binding.productAmount.setText(String.format(Locale.US, "%.2f", totalPerProductGST));
        float totalPerProductAmount = (totalPerProductGST * productQuantity);
        holder.binding.productQuantity.setText(String.format(Locale.US, "%.2f", totalPerProductAmount));

    }

    @Override
    public int getItemCount() {
        return invoiceProductResponseList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TwoInchPrinterProductListBinding binding;

        public MyViewHolder(TwoInchPrinterProductListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }
    }
}

