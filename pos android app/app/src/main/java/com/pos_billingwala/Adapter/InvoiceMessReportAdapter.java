package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Extra.RowDividerUi;
import com.pos_billingwala.Model.MessInvoiceResponse;
import com.pos_billingwala.databinding.InvoiceMessReportListBinding;

import java.util.List;


@SuppressLint("SetTextI18n")
public class InvoiceMessReportAdapter extends RecyclerView.Adapter<InvoiceMessReportAdapter.MyViewHolder> {

    Context context;
    List<MessInvoiceResponse> messInvoiceResponseList;

    public InvoiceMessReportAdapter(Context context, List<MessInvoiceResponse> messInvoiceResponseList) {
        this.context = context;
        this.messInvoiceResponseList = messInvoiceResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(InvoiceMessReportListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        MessInvoiceResponse messInvoiceResponse = messInvoiceResponseList.get(position);

        holder.binding.srNo.setText("" + (position + 1));
        holder.binding.invoiceDate.setText(messInvoiceResponse.getMessInvoiceDate());
        holder.binding.memberName.setText(messInvoiceResponse.getMemberName());
        holder.binding.messType.setText(messInvoiceResponse.getMessType());

        RowDividerUi.bindLastItem(holder.binding.rowDivider, position, getItemCount());
    }

    @Override
    public int getItemCount() {
        return messInvoiceResponseList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        InvoiceMessReportListBinding binding;

        public MyViewHolder(InvoiceMessReportListBinding binding) {
            super(binding.getRoot());

            this.binding = binding;

        }
    }
}