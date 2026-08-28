package com.pos_billingwala.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ItemReportInvoiceDetailRowBinding;

import java.util.List;
import java.util.Locale;

public class ReportDetailTableAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_LOADING = 0;
    public static final int VIEW_TYPE_NORMAL = 1;

    private final Context context;
    private final List<InvoiceResponse> invoiceResponseList;

    public ReportDetailTableAdapter(Context context, List<InvoiceResponse> invoiceResponseList) {
        this.context = context;
        this.invoiceResponseList = invoiceResponseList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_NORMAL) {
            ItemReportInvoiceDetailRowBinding binding = ItemReportInvoiceDetailRowBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new RowHolder(binding);
        }
        return new LoadingHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_loading, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof RowHolder) {
            bindRow((RowHolder) holder, position);
        }
    }

    private void bindRow(RowHolder holder, int position) {
        InvoiceResponse invoice = invoiceResponseList.get(position);
        if (invoice == null) {
            return;
        }
        holder.binding.srNo.setText(String.valueOf(position + 1));
        holder.binding.invoiceDate.setText(ReportCursorHelper.formatInvoiceDate(invoice.getInvoiceDate()));
        holder.binding.invoiceNumber.setText(invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "");
        float totalAmount = ReportCursorHelper.parseAmount(invoice.getTotalAmount());
        holder.binding.invoiceAmount.setText(MainActivity.currencyName + " "
                + String.format(Locale.US, "%.2f", totalAmount));
    }

    @Override
    public int getItemCount() {
        return invoiceResponseList != null ? invoiceResponseList.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return invoiceResponseList.get(position) != null ? VIEW_TYPE_NORMAL : VIEW_TYPE_LOADING;
    }

    static class RowHolder extends RecyclerView.ViewHolder {
        final ItemReportInvoiceDetailRowBinding binding;

        RowHolder(ItemReportInvoiceDetailRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class LoadingHolder extends RecyclerView.ViewHolder {
        LoadingHolder(android.view.View itemView) {
            super(itemView);
        }
    }
}
