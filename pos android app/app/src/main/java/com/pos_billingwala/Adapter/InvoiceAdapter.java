package com.pos_billingwala.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.InvoiceDetailsBluetoothPrint;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.PaymentSettlementHelper;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.InvoiceListBinding;
import com.pos_billingwala.databinding.ItemLoadingBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


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
        if (invoiceResponse.getInvoiceType() == null) {
            return "";
        }
        String kind;
        if (invoiceResponse.getInvoiceType().equalsIgnoreCase("table_wise")) {
            kind = "Table";
        } else if (invoiceResponse.getInvoiceType().equalsIgnoreCase("take_away")) {
            kind = "Take Away";
        } else {
            kind = "Fast Billing";
        }
        String ref = invoiceResponse.getNoOfTable();
        if (ref == null) {
            return kind;
        }
        ref = ref.trim();
        if (ref.isEmpty() || ref.equals("0") || ref.equals("-") || ref.equalsIgnoreCase("null")) {
            return kind;
        }
        return kind + " · " + ref;
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

        holder.binding.invoiceNumber.setText(safe(invoiceResponse.getInvoiceNumber()));
        holder.binding.invoiceType.setText(getInvoiceType(invoiceResponse));
        holder.binding.invoiceDate.setText(formatInvoiceDate(invoiceResponse.getInvoiceDate()));
        holder.binding.subTotal.setText(money(invoiceResponse.getSubTotal()));
        String halfGst = money(halfOf(invoiceResponse.getTotalGSTAmount()));
        holder.binding.gst.setText(halfGst + "  /  " + halfGst);
        holder.binding.discount.setText(discountText(invoiceResponse));
        holder.binding.totalAmount.setText(money(invoiceResponse.getTotalAmount()));
        bindPaymentChip(holder, invoiceResponse);
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

        boolean canShowQr = !invoiceResponse.isRefunded()
                && !"cancelled".equalsIgnoreCase(invoiceResponse.getInvoiceOrderStatus());
        holder.binding.showQrCardView.setVisibility(canShowQr ? View.VISIBLE : View.GONE);
        holder.binding.showQrCardView.setOnClickListener(canShowQr
                ? v -> {
                    if (context instanceof android.app.Activity) {
                        com.pos_billingwala.PaymentDisplay.PaymentDisplayService.requestShowInvoiceQr(
                                (android.app.Activity) context, invoiceResponse);
                    }
                }
                : null);

        if (holder.binding.rowDivider != null) {
            holder.binding.rowDivider.setVisibility(View.GONE);
        }
    }

    private void bindPaymentChip(MyViewHolder holder, InvoiceResponse invoiceResponse) {
        String label = PaymentSettlementHelper.displayLabel(
                invoiceResponse.getPaymentMode(), invoiceResponse.getCashAmount(),
                invoiceResponse.getUpiAmount(), invoiceResponse.getTotalAmount());
        if (label == null || label.trim().isEmpty()) {
            holder.binding.payableMode.setVisibility(View.GONE);
            return;
        }
        holder.binding.payableMode.setVisibility(View.VISIBLE);
        holder.binding.payableMode.setText(label);

        String mode = PaymentSettlementHelper.canonicalMode(invoiceResponse.getPaymentMode());
        int background;
        int text;
        if (PaymentSettlementHelper.MODE_CASH.equals(mode)) {
            background = ContextCompat.getColor(context, R.color.dropdownSelectedBg);
            text = ContextCompat.getColor(context, R.color.dropdownSelectedText);
        } else if (PaymentSettlementHelper.MODE_UPI.equals(mode)) {
            background = ContextCompat.getColor(context, R.color.colorPrimaryLight);
            text = ContextCompat.getColor(context, R.color.colorPrimary);
        } else if (PaymentSettlementHelper.MODE_BANK.equals(mode)) {
            background = ContextCompat.getColor(context, R.color.dropdown_icon_bg);
            text = ContextCompat.getColor(context, R.color.kpiPurple);
        } else if (PaymentSettlementHelper.MODE_SPLIT.equals(mode)) {
            background = ContextCompat.getColor(context, R.color.colorItem2Background);
            text = ContextCompat.getColor(context, R.color.colorItem2Tint);
        } else {
            background = ContextCompat.getColor(context, R.color.colorInputFill);
            text = ContextCompat.getColor(context, R.color.colorTextSecondary);
        }
        Drawable chip = ContextCompat.getDrawable(context, R.drawable.bg_payment_chip);
        if (chip != null) {
            chip = chip.mutate();
            if (chip instanceof GradientDrawable) {
                ((GradientDrawable) chip).setColor(background);
            }
            holder.binding.payableMode.setBackground(chip);
        }
        holder.binding.payableMode.setTextColor(text);
    }

    private static String discountText(InvoiceResponse invoiceResponse) {
        boolean amount = invoiceResponse.getDiscountType() != null
                && invoiceResponse.getDiscountType().equalsIgnoreCase("Amount");
        if (amount) {
            return money(invoiceResponse.getDiscount());
        }
        String value = formatAmount(invoiceResponse.getDiscount());
        if (value.endsWith(".00")) {
            value = value.substring(0, value.length() - 3);
        }
        return value + "%";
    }

    private static String money(String raw) {
        String currency = MainActivity.currencyName == null ? "" : MainActivity.currencyName.trim();
        String amount = formatAmount(raw);
        return currency.isEmpty() ? amount : currency + " " + amount;
    }

    private static String halfOf(String raw) {
        try {
            if (raw == null || raw.trim().isEmpty()) {
                return "0";
            }
            return String.valueOf(Double.parseDouble(raw.trim()) / 2d);
        } catch (NumberFormatException ignored) {
            return "0";
        }
    }

    private static String formatAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "0.00";
        }
        try {
            return String.format(Locale.US, "%.2f", Double.parseDouble(raw.trim()));
        } catch (NumberFormatException ignored) {
            return raw.trim();
        }
    }

    private static String formatInvoiceDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(raw.trim());
            if (date == null) {
                return raw.trim();
            }
            return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date);
        } catch (ParseException ignored) {
            return raw.trim();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
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
