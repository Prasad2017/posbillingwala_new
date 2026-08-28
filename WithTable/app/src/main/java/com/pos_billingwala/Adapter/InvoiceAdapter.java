package com.pos_billingwala.Adapter;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.InvoiceDetailsBluetoothPrint;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.databinding.InvoiceListBinding;
import com.pos_billingwala.databinding.ItemLoadingBinding;

import java.util.ArrayList;
import java.util.List;


public class InvoiceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_LOADING = 0;
    public static final int VIEW_TYPE_NORMAL = 1;
    public POSBillingWalaDatabase posBillingWalaDatabase;
    public List<InvoiceProductResponse> invoiceProductResponseList = new ArrayList<>();
    public List<CompanyResponse> companyResponseList = new ArrayList<>();
    Context context;
    List<InvoiceResponse> invoiceResponseList;

    public InvoiceAdapter(Context context, List<InvoiceResponse> invoiceResponseList) {
        this.context = context;
        this.invoiceResponseList = invoiceResponseList;
    }

    public static @NonNull String getInvoiceType(InvoiceResponse invoiceResponse) {
        String invoiceType = "";
        if (invoiceResponse.getInvoiceType() == null) {
            return invoiceType;
        }
        if (invoiceResponse.getInvoiceType().equalsIgnoreCase("table_wise")) {
            invoiceType = "<b>Invoice Type:</b> Table No- " + invoiceResponse.getNoOfTable();
        } else if (invoiceResponse.getInvoiceType().equalsIgnoreCase("take_away")) {
            invoiceType = "<b>Invoice Type:</b> Take Away- " + invoiceResponse.getNoOfTable();
        } else {
            invoiceType = "<b>Invoice Type:</b> Fast Billing- " + invoiceResponse.getNoOfTable();
        }
        return invoiceType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_NORMAL) {
            return new MyViewHolder(InvoiceListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        } else {
            return new LoadingViewHolder(ItemLoadingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
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

        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        String invoiceNumber = "<b>" + invoiceResponse.getInvoiceNumber() + "</b>";
        holder.binding.invoiceNumber.setText(Html.fromHtml(invoiceNumber));
        String invoiceType = getInvoiceType(invoiceResponse);
        holder.binding.invoiceType.setText(Html.fromHtml(invoiceType));
        String invoiceDate = "<b>Invoice Date:</b> " + invoiceResponse.getInvoiceDate();
        holder.binding.invoiceDate.setText(Html.fromHtml(invoiceDate));
        String subTotal = "<b>Sub Total:</b> " + MainActivity.currencyName + " " + invoiceResponse.getSubTotal();
        holder.binding.subTotal.setText(Html.fromHtml(subTotal));
        float gst = Float.parseFloat(invoiceResponse.getTotalGSTAmount());
        String shopGST = "<b>CGST:</b> " + MainActivity.currencyName + " " + (gst / 2) + "&nbsp;&nbsp;&nbsp;<b>SGST:</b> " + MainActivity.currencyName + " " + (gst / 2);
        holder.binding.gst.setText(Html.fromHtml(shopGST));

        String discount = "";
        if (invoiceResponse.getDiscountType() != null) {
            if (invoiceResponse.getDiscountType().equalsIgnoreCase("Amount")) {
                discount = "<b>Discount:</b> " + MainActivity.currencyName + invoiceResponse.getDiscount();
            } else {
                discount = "<b>Discount(%):</b> " + invoiceResponse.getDiscount();
            }
        } else {
            discount = "<b>Discount(%):</b> " + invoiceResponse.getDiscount();
        }

        holder.binding.discount.setText(Html.fromHtml(discount));

        String totalAmount = "<b>Total Amount:</b> " + MainActivity.currencyName + " " + invoiceResponse.getTotalAmount();
        holder.binding.totalAmount.setText(Html.fromHtml(totalAmount));
        String paymentMode = "<b>Payment Mode</b>: " + invoiceResponse.getPaymentMode();
        holder.binding.payableMode.setText(Html.fromHtml(paymentMode));
        holder.binding.refundedLabel.setVisibility(invoiceResponse.isRefunded() ? View.VISIBLE : View.GONE);

        holder.binding.invoiceCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(context, InvoiceDetailsBluetoothPrint.class);
                intent.putExtra("invoiceId", invoiceResponse.getInvoiceId());
                context.startActivity(intent);

               /* InvoiceProductDetails invoiceProductDetails = new InvoiceProductDetails();
                Bundle bundle = new Bundle();
                bundle.putString("invoiceId", invoiceResponse.getInvoiceId());
                invoiceProductDetails.setArguments(bundle);
                ((MainActivity) context).loadFragment(invoiceProductDetails, true);*/

            }
        });

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
        viewHolder.binding.progressBar.setVisibility(View.VISIBLE);
    }

    public static class LoadingViewHolder extends RecyclerView.ViewHolder {

        ItemLoadingBinding binding;

        public LoadingViewHolder(ItemLoadingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        InvoiceListBinding binding;

        public MyViewHolder(InvoiceListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

    }
}
