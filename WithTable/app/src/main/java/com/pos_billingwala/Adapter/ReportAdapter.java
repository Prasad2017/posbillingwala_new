package com.pos_billingwala.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.Extra.RowDividerUi;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.R;

import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_LOADING = 0;
    public static final int VIEW_TYPE_NORMAL = 1;
    Context context;
    List<InvoiceResponse> invoiceResponseList;
    private final boolean showDiscountAmount;
    private OnInvoiceClickListener invoiceClickListener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnInvoiceClickListener {
        void onInvoiceClick(InvoiceResponse invoice, int position);
    }

    public ReportAdapter(Context context, List<InvoiceResponse> invoiceResponseList) {
        this(context, invoiceResponseList, false);
    }

    public ReportAdapter(Context context, List<InvoiceResponse> invoiceResponseList, boolean showDiscountAmount) {
        this.context = context;
        this.invoiceResponseList = invoiceResponseList;
        this.showDiscountAmount = showDiscountAmount;
    }

    public void setOnInvoiceClickListener(OnInvoiceClickListener listener) {
        this.invoiceClickListener = listener;
    }

    public void setSelectedPosition(int position) {
        int previous = selectedPosition;
        selectedPosition = position;
        if (previous != RecyclerView.NO_POSITION && previous < getItemCount()) {
            notifyItemChanged(previous);
        }
        if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < getItemCount()) {
            notifyItemChanged(selectedPosition);
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
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
            holder.invoiceDate.setText(ReportCursorHelper.formatInvoiceDate(invoiceResponse.getInvoiceDate()));
            holder.invoiceNumber.setText(invoiceResponse.getInvoiceNumber() != null ? invoiceResponse.getInvoiceNumber() : "");
            float amount = showDiscountAmount
                    ? ReportCursorHelper.discountRupees(invoiceResponse.getDiscount(),
                    invoiceResponse.getDiscountType(), invoiceResponse.getSubTotal())
                    : ReportCursorHelper.parseAmount(invoiceResponse.getTotalAmount());
            holder.invoiceAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", amount));
            if (position == selectedPosition) {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryLight));
            } else {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
            }
            holder.itemView.setOnClickListener(v -> {
                if (invoiceClickListener != null) {
                    invoiceClickListener.onInvoiceClick(invoiceResponse, position);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        RowDividerUi.bindLastItem(holder.itemView.findViewById(R.id.rowDivider), position, getItemCount());
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
        viewHolder.progressBar.setVisibility(View.VISIBLE);
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
