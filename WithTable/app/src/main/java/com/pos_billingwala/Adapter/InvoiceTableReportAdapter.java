package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.Fragment.InvoiceTableListReport;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.databinding.InvoiceTableReportListBinding;

import java.util.List;
import java.util.Locale;


public class InvoiceTableReportAdapter extends RecyclerView.Adapter<InvoiceTableReportAdapter.MyViewHolder> {

    Context context;
    List<InvoiceResponse> invoiceResponseList;

    public InvoiceTableReportAdapter(Context context, List<InvoiceResponse> invoiceResponseList) {
        this.context = context;
        this.invoiceResponseList = invoiceResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(InvoiceTableReportListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        InvoiceResponse invoiceResponse = invoiceResponseList.get(position);

        holder.binding.srNo.setText(String.valueOf(position + 1));
        holder.binding.invoiceNumber.setText(invoiceResponse.getNoOfTable());
        float totalAmount = ReportCursorHelper.parseAmount(invoiceResponse.getTotalAmount());
        holder.binding.invoiceAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalAmount));

        holder.binding.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InvoiceTableListReport invoiceTableListReport = new InvoiceTableListReport();
                Bundle bundle = new Bundle();
                bundle.putString("noOfTable", invoiceResponse.getNoOfTable());
                bundle.putString("invoiceType", invoiceResponse.getInvoiceType());
                invoiceTableListReport.setArguments(bundle);
                ((MainActivity) context).loadFragment(invoiceTableListReport, true);
            }
        });

    }

    @Override
    public int getItemCount() {
        return invoiceResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        public final InvoiceTableReportListBinding binding;

        public MyViewHolder(InvoiceTableReportListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

