package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Extra.RowDividerUi;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.databinding.ReportProductInvoiceListBinding;

import java.util.List;

public class ReportProductAdapter extends RecyclerView.Adapter<ReportProductAdapter.MyViewHolder> {

    Context context;
    List<InvoiceProductResponse> invoiceProductResponseList;
    InvoiceProductResponse invoiceProductResponse;

    public ReportProductAdapter(Context context, List<InvoiceProductResponse> invoiceProductResponseList) {
        this.context = context;
        this.invoiceProductResponseList = invoiceProductResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(ReportProductInvoiceListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        invoiceProductResponse = invoiceProductResponseList.get(position);

        holder.binding.srNo.setText(String.valueOf(position + 1));
        holder.binding.productName.setText(invoiceProductResponse.getDisplayLineName());
        holder.binding.productQuantity.setText(invoiceProductResponse.getProductQuantity());


        RowDividerUi.bindLastItem(holder.binding.rowDivider, position, getItemCount());
    }

    @Override
    public int getItemCount() {
        return invoiceProductResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public final ReportProductInvoiceListBinding binding;

        public MyViewHolder(ReportProductInvoiceListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
