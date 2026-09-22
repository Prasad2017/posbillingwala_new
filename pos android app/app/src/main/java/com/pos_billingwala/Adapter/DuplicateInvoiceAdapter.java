package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Extra.RowDividerUi;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.databinding.DuplicateInvoiceListBinding;

import java.util.List;
import java.util.Locale;


@SuppressLint("SetTextI18n, NotifyDataSetChanged")
public class DuplicateInvoiceAdapter extends RecyclerView.Adapter<DuplicateInvoiceAdapter.MyViewHolder> {

    Context context;
    List<InvoiceProductResponse> invoiceProductResponseList;

    public DuplicateInvoiceAdapter(Context context, List<InvoiceProductResponse> invoiceProductResponseList) {
        this.context = context;
        this.invoiceProductResponseList = invoiceProductResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(DuplicateInvoiceListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        InvoiceProductResponse invoiceProductResponse = invoiceProductResponseList.get(position);

        float productPrice = Float.parseFloat(invoiceProductResponse.getResolvedLinePrice());
        float productQuantity = Float.parseFloat(invoiceProductResponse.getProductQuantity());

        holder.binding.productName.setText(invoiceProductResponse.getDisplayLineName());
        holder.binding.productQuantity.setText(invoiceProductResponse.getProductQuantity());
        holder.binding.productPrice.setText(String.format(Locale.US, "%.2f", productPrice));
        RowDividerUi.bindLastItem(holder.binding.rowDivider, position, getItemCount());
    }

    @Override
    public int getItemCount() {
        return invoiceProductResponseList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        DuplicateInvoiceListBinding binding;

        public MyViewHolder(DuplicateInvoiceListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }

    }
}

