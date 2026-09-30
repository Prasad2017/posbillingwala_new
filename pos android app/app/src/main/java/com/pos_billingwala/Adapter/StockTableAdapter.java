package com.pos_billingwala.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.StockTableRowBinding;

import java.util.ArrayList;
import java.util.List;

public class StockTableAdapter extends RecyclerView.Adapter<StockTableAdapter.Holder> {

    public static final String STATUS_IN = "in";
    public static final String STATUS_LOW = "low";
    public static final String STATUS_OUT = "out";
    public static final double LOW_STOCK_BELOW = 6d;

    public interface OnRowClick {
        void onRowClick(StockRow row);
    }

    public static class StockRow {
        public String productId;
        public String name;
        public String code;
        public double stock;
        public String priceLabel;
        public String status;
    }

    private final List<StockRow> rows = new ArrayList<>();
    private final OnRowClick click;
    private String selectedProductId;

    public StockTableAdapter(OnRowClick click) {
        this.click = click;
    }

    public void submit(List<StockRow> next, String selectedProductId) {
        rows.clear();
        if (next != null) {
            rows.addAll(next);
        }
        this.selectedProductId = selectedProductId;
        notifyDataSetChanged();
    }

    public static String statusFor(double stock) {
        if (stock <= 0d) {
            return STATUS_OUT;
        }
        if (stock < LOW_STOCK_BELOW) {
            return STATUS_LOW;
        }
        return STATUS_IN;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(StockTableRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        StockRow row = rows.get(position);
        StockTableRowBinding binding = holder.binding;
        binding.colNo.setText(String.valueOf(position + 1));
        binding.colName.setText(row.name);
        binding.colCode.setText(row.code == null || row.code.trim().isEmpty() ? "—" : row.code);
        binding.colStock.setText(POSBillingWalaDatabase.formatStockQty(row.stock));
        binding.colPrice.setText(row.priceLabel);
        int statusColor;
        int statusText;
        if (STATUS_OUT.equals(row.status)) {
            statusColor = R.color.statusExpired;
            statusText = R.string.ui_out_of_stock;
        } else if (STATUS_LOW.equals(row.status)) {
            statusColor = R.color.table_status_bill_requested;
            statusText = R.string.ui_low_stock;
        } else {
            statusColor = R.color.green_600;
            statusText = R.string.ui_stock_remaining;
        }
        binding.colStatus.setText(statusText);
        binding.colStatus.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), statusColor));
        boolean selected = row.productId != null && row.productId.equals(selectedProductId);
        binding.stockRow.setBackgroundColor(ContextCompat.getColor(
                binding.getRoot().getContext(),
                selected ? R.color.colorPrimaryLight : android.R.color.white));
        binding.getRoot().setOnClickListener(v -> {
            if (click != null) {
                click.onRowClick(row);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final StockTableRowBinding binding;

        Holder(StockTableRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
