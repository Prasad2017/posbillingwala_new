package com.posbillingwala.admin.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Model.InvoiceSaleResponse;
import com.posbillingwala.admin.databinding.ItemSimpleCardBinding;

import java.util.List;

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.Holder> {

    public interface OnInvoiceClick {
        void onClick(InvoiceSaleResponse invoice);
    }

    private final List<InvoiceSaleResponse> items;
    private final OnInvoiceClick listener;

    public SalesAdapter(List<InvoiceSaleResponse> items) {
        this(items, null);
    }

    public SalesAdapter(List<InvoiceSaleResponse> items, OnInvoiceClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemSimpleCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        InvoiceSaleResponse inv = items.get(position);
        String number = nz(inv.getInvoiceNumber());
        holder.binding.title.setText((number.startsWith("#") ? "Bill " : "Bill #") + number
                + " · ₹ " + nz(inv.getTotalAmount()));
        String customer = nz(inv.getCustomerName());
        if ("-".equals(customer) && inv.getShopName() != null && !inv.getShopName().isEmpty()) {
            customer = inv.getShopName();
        }
        holder.binding.meta.setText(
                "Date: " + nz(inv.getInvoiceDate())
                        + "\nCustomer: " + customer
                        + (inv.getPaymentMode() != null && !inv.getPaymentMode().isEmpty()
                        ? "\nPay: " + inv.getPaymentMode() : ""));
        holder.binding.status.setText(inv.getPaymentStatus());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(inv);
        });
    }

    private static String nz(String v) {
        return v == null || v.isEmpty() ? "-" : v;
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemSimpleCardBinding binding;

        Holder(ItemSimpleCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
