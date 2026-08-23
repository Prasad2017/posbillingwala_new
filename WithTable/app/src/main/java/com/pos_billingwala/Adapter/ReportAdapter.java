package com.pos_billingwala.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.R;

import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_LOADING = 0;
    public static final int VIEW_TYPE_NORMAL = 1;
    Context context;
    List<InvoiceResponse> invoiceResponseList;

    public ReportAdapter(Context context, List<InvoiceResponse> invoiceResponseList) {
        this.context = context;
        this.invoiceResponseList = invoiceResponseList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_NORMAL) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.report_invoice_list, parent, false);
            return new MyViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {

        if (viewHolder instanceof MyViewHolder) {
            populateItemRows((MyViewHolder) viewHolder, position);
        } else if (viewHolder instanceof LoadingViewHolder) {
            showLoadingView((LoadingViewHolder) viewHolder, position);
        }

    }

    public void populateItemRows(MyViewHolder holder, int position) {

        InvoiceResponse invoiceResponse = invoiceResponseList.get(position);

        try {
            holder.srNo.setText("" + (position + 1));
            holder.invoiceDate.setText(invoiceResponse.getInvoiceDate().substring(0, 10));
            holder.invoiceNumber.setText(invoiceResponse.getInvoiceNumber());
            float totalAmount = Float.parseFloat(invoiceResponse.getTotalAmount());
            holder.invoiceAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalAmount));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public int getItemCount() {
        return invoiceResponseList != null ? invoiceResponseList.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return invoiceResponseList.get(position) != null ? VIEW_TYPE_NORMAL : VIEW_TYPE_LOADING;
    }

    public void showLoadingView(LoadingViewHolder viewHolder, int position) {
        //ProgressBar would be displayed
        viewHolder.progressBar.setVisibility(View.GONE);
    }

    public static class LoadingViewHolder extends RecyclerView.ViewHolder {

        ProgressBar progressBar;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView srNo, invoiceDate, invoiceNumber, invoiceAmount;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            srNo = itemView.findViewById(R.id.srNo);
            invoiceDate = itemView.findViewById(R.id.invoiceDate);
            invoiceNumber = itemView.findViewById(R.id.invoiceNumber);
            invoiceAmount = itemView.findViewById(R.id.invoiceAmount);

        }
    }
}
