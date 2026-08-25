package com.posbillingwala.admin.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Model.InvoiceSaleResponse;
import com.posbillingwala.admin.databinding.ItemSimpleCardBinding;

import java.util.List;

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.Holder> {

    private final List<InvoiceSaleResponse> items;

    public SalesAdapter(List<InvoiceSaleResponse> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemSimpleCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        InvoiceSaleResponse inv = items.get(position);
        holder.binding.title.setText("Bill #" + nz(inv.getInvoiceNumber()) + " · ₹ " + nz(inv.getTotalAmount()));
        holder.binding.meta.setText(
                "Date: " + nz(inv.getInvoiceDate())
                        + "\nType: " + nz(inv.getInvoiceType())
                        + " · Pay: " + nz(inv.getPaymentMode())
                        + "\nBranch: " + nz(inv.getBranchName())
                        + "\nCustomer: " + nz(inv.getCustomerName())
                        + "\nTax: ₹ " + nz(inv.getTotalGSTAmount())
                        + " · Disc: ₹ " + nz(inv.getDiscount()));
        holder.binding.status.setText(nz(inv.getPaymentMode()).isEmpty() ? "SALE" : inv.getPaymentMode());
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
