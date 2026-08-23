package com.posbillingwala.owner.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Fragment.InvoiceProductDetails;
import com.posbillingwala.owner.Model.InvoiceResponse;
import com.posbillingwala.owner.databinding.InvoiceListBinding;

import java.util.List;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.MyViewHolder> {

    private final Context context;
    private final List<InvoiceResponse> invoiceResponseList;

    public InvoiceAdapter(Context context, List<InvoiceResponse> invoiceResponseList) {
        this.context = context;
        this.invoiceResponseList = invoiceResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use ViewBinding to inflate the layout
        InvoiceListBinding binding = InvoiceListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        InvoiceResponse invoiceResponse = invoiceResponseList.get(position);

        String invoiceNumber = "<b>" + invoiceResponse.getInvoiceNumber() + "</b>";
        holder.binding.invoiceNumber.setText(Html.fromHtml(invoiceNumber));

        String invoiceType;
        if ("table_wise".equalsIgnoreCase(invoiceResponse.getInvoiceType())) {
            invoiceType = "<b>Invoice Type:</b> Table No- " + invoiceResponse.getNoOfTable();
        } else if ("take_away".equalsIgnoreCase(invoiceResponse.getInvoiceType())) {
            invoiceType = "<b>Invoice Type:</b> Take Away- " + invoiceResponse.getNoOfTable();
        } else {
            invoiceType = "<b>Invoice Type:</b> Fast Billing- " + invoiceResponse.getNoOfTable();
        }
        holder.binding.invoiceType.setText(Html.fromHtml(invoiceType));

        String invoiceDate = "<b>Invoice Date:</b> " + invoiceResponse.getInvoiceDate();
        holder.binding.invoiceDate.setText(Html.fromHtml(invoiceDate));

        String subTotal = "<b>Sub Total:</b> " + MainActivity.currency + " " + invoiceResponse.getSubTotal();
        holder.binding.subTotal.setText(Html.fromHtml(subTotal));

        float gst = Float.parseFloat(invoiceResponse.getTotalGSTAmount());
        String shopGST = "<b>CGST:</b> " + MainActivity.currency + " " + (gst / 2) + "&nbsp;&nbsp;&nbsp;<b>SGST:</b> " + MainActivity.currency + " " + (gst / 2);
        holder.binding.gst.setText(Html.fromHtml(shopGST));

        String discount = "";
        if (invoiceResponse.getDiscountType() != null) {
            if ("Amount".equalsIgnoreCase(invoiceResponse.getDiscountType())) {
                discount = "<b>Discount:</b> " + MainActivity.currency + invoiceResponse.getDiscount();
            } else {
                discount = "<b>Discount(%):</b> " + invoiceResponse.getDiscount();
            }
        } else {
            discount = "<b>Discount(%):</b> " + invoiceResponse.getDiscount();
        }
        holder.binding.discount.setText(Html.fromHtml(discount));

        String totalAmount = "<b>Total Amount:</b> " + MainActivity.currency + " " + invoiceResponse.getTotalAmount();
        holder.binding.totalAmount.setText(Html.fromHtml(totalAmount));

        String paymentMode = "<b>Payment Mode</b>: " + invoiceResponse.getPaymentMode();
        holder.binding.payableMode.setText(Html.fromHtml(paymentMode));

        holder.binding.invoiceCardView.setOnClickListener(view -> {
            InvoiceProductDetails invoiceProductDetails = new InvoiceProductDetails();
            Bundle bundle = new Bundle();
            bundle.putString("invoiceId", invoiceResponse.getInvoiceId());
            invoiceProductDetails.setArguments(bundle);
            ((MainActivity) context).loadFragment(invoiceProductDetails, true);
        });
    }

    @Override
    public int getItemCount() {
        return invoiceResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final InvoiceListBinding binding;

        public MyViewHolder(@NonNull InvoiceListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
