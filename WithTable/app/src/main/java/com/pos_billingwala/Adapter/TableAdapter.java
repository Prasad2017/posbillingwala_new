package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pos_billingwala.Activity.BluetoothPrint;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Fragment.InvoiceCompanyTable;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.TableListBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;


@SuppressLint("SetTextI18n, UseCompatLoadingForDrawables")
public class TableAdapter extends RecyclerView.Adapter<TableAdapter.MyViewHolder> {

    Context context;
    int noOfTablesList;
    POSBillingWalaDatabase posBillingWalaDatabase;
    String cartOrderStatus = "table_wise";
    List<ProductCartResponse> productCartResponseList = new ArrayList<>();
    List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
    List<CompanyResponse> companyResponseList = new ArrayList<>();
    String paymentMode = "";


    public TableAdapter(Context context, int noOfTablesList) {
        this.context = context;
        this.noOfTablesList = noOfTablesList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(TableListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        int tableNumber = position + 1;
        String table = "Table No<br/><b>" + tableNumber + "<b/>";
        holder.binding.tableNumber.setText(Html.fromHtml(table));

        getTableAmount(holder, String.valueOf(tableNumber));
        getBillSettlementList(holder, String.valueOf(tableNumber));

        holder.binding.tableNumberCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invoiceResponseList.clear();
                invoiceResponseList = posBillingWalaDatabase.checkTablePaymentMode(String.valueOf(tableNumber));
                if (invoiceResponseList.isEmpty()) {
                    CreatePos createPos = new CreatePos();
                    Bundle bundle = new Bundle();
                    bundle.putString("tableNumber", String.valueOf(tableNumber));
                    bundle.putString("cartOrderStatus", "table_wise");
                    createPos.setArguments(bundle);
                    ((MainActivity) context).loadFragment(createPos, true);
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_please_settle_previous_bill), Toast.LENGTH_SHORT).show();
                }

            }
        });

        holder.binding.billPrintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invoiceResponseList.clear();
                invoiceResponseList = posBillingWalaDatabase.checkTablePaymentMode(String.valueOf(tableNumber));
                if (invoiceResponseList.isEmpty()) {
                    Intent intent = new Intent(context, BluetoothPrint.class);
                    intent.putExtra("invoiceRunningStatus", "printBill");
                    intent.putExtra("tableNumber", String.valueOf(tableNumber));
                    intent.putExtra("cartOrderStatus", "table_wise");
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_please_settle_previous_bill), Toast.LENGTH_SHORT).show();
                }

            }
        });

        holder.binding.billSettlementPrintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invoiceResponseList.clear();
                invoiceResponseList = posBillingWalaDatabase.checkTablePaymentMode(String.valueOf(tableNumber));
                if (invoiceResponseList.size() > 0) {
                    setPaymentMode(String.valueOf(tableNumber), invoiceResponseList.get(0).getInvoiceNumber(), invoiceResponseList.get(0));
                }
            }
        });


    }

    public void setPaymentMode(String tableNumber, String invoiceNumber, InvoiceResponse invoiceResponse) {
        Activity activity = (Activity) context;
        View content = LayoutInflater.from(activity).inflate(R.layout.set_payment_mode_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextView continueToReport = content.findViewById(R.id.continueToReport);
        TextView dismissReport = content.findViewById(R.id.dismissReport);
        TextView totalAmount = content.findViewById(R.id.totalAmount);
        RadioGroup paymentGroup = content.findViewById(R.id.paymentGroup);

        totalAmount.setText("Total Amount: " + MainActivity.currencyName + invoiceResponse.getTotalAmount());
        paymentGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int selectedId = paymentGroup.getCheckedRadioButtonId();
                RadioButton radioPayButton = group.findViewById(selectedId);
                paymentMode = radioPayButton.getText().toString();
            }
        });

        dismissReport.setOnClickListener(v -> sheet.dismiss());

        continueToReport.setOnClickListener(v -> {
            if (!paymentMode.isEmpty()) {
                sheet.dismiss();
                posBillingWalaDatabase.updateInvoiceTablePaymentMode(invoiceNumber, tableNumber, paymentMode);
                InvoiceCompanyTable.getCompanyDetails();
            } else {
                Toast.makeText(context, context.getString(R.string.toast_please_select_payment_mode), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void getBillSettlementList(MyViewHolder holder, String tableNumber) {
        if (!posBillingWalaDatabase.checkTablePaymentMode(tableNumber).isEmpty()) {
            holder.binding.billSettlementPrintLayout.setVisibility(View.VISIBLE);
            holder.binding.billPrintLayout.setVisibility(View.GONE);
        } else {
            holder.binding.billSettlementPrintLayout.setVisibility(View.GONE);
            holder.binding.billPrintLayout.setVisibility(View.VISIBLE);
        }

    }


    public void getTableAmount(MyViewHolder holder, String tableNumber) {

        companyResponseList.clear();
        companyResponseList = posBillingWalaDatabase.getCompanyDetails();
        if (!companyResponseList.isEmpty()) {

            productCartResponseList.clear();
            productCartResponseList = posBillingWalaDatabase.getCartProductList(tableNumber, "table_wise");
            String discountType = "";
            float totalPerProductAmount = 0f, discountAmount = 0f, totalCGST = 0f, totalSGST = 0f, totalPerProductGST = 0f, totalGST = 0f;
            if (!productCartResponseList.isEmpty()) {

                for (int i = 0; i < productCartResponseList.size(); i++) {

                    float productPrice = Float.parseFloat(productCartResponseList.get(i).getProductOldPrice());
                    float productQuantity = Float.parseFloat(productCartResponseList.get(i).getProductQuantity());
                    if (!productCartResponseList.get(i).getProductCGST().equalsIgnoreCase("")) {
                        totalCGST += Float.parseFloat(productCartResponseList.get(i).getProductCGST());
                    }
                    if (!productCartResponseList.get(i).getProductSGST().equalsIgnoreCase("")) {
                        totalSGST += Float.parseFloat(productCartResponseList.get(i).getProductSGST());
                    }
                    discountAmount = Float.parseFloat(productCartResponseList.get(i).getCartDiscount());
                    discountType = productCartResponseList.get(0).getCartDiscountType();
                    totalPerProductGST = (productPrice * ((totalCGST + totalSGST) / 100));
                    totalGST += (productPrice * ((totalCGST + totalSGST) / 100)) * productQuantity;

                    totalPerProductAmount = totalPerProductAmount + ((productPrice + totalPerProductGST) * productQuantity);

                }

                float subTotalAmt = totalPerProductAmount - totalGST;

                if (discountType != null) {
                    if (discountType.equalsIgnoreCase("Amount")) {
                        discountAmount = discountAmount;
                    } else {
                        discountAmount = subTotalAmt / (100 / discountAmount);
                    }
                } else {
                    discountAmount = subTotalAmt / (100 / discountAmount);
                }

                float shopCGST = 0f, shopSGST = 0f;
                if (companyResponseList.get(0).getShopCGST() != null) {
                    shopCGST = subTotalAmt * (Float.parseFloat(companyResponseList.get(0).getShopCGST().trim()) / 100);
                }

                if (companyResponseList.get(0).getShopSGST() != null) {
                    if (!companyResponseList.get(0).getShopSGST().trim().equalsIgnoreCase("")) {
                        shopSGST = subTotalAmt * (Float.parseFloat(companyResponseList.get(0).getShopSGST().trim()) / 100);
                    }
                }
                float totalShopGST = shopCGST + shopSGST;

                float totalAmount = totalPerProductAmount - discountAmount + totalShopGST;
                totalAmount = (float) Math.ceil(totalAmount);
                Random rnd = new Random();
                int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                holder.binding.billAmount.setTextColor(color);
                holder.binding.billAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalAmount));

                holder.binding.addItems.setText("Add Items");
                holder.binding.billPrintLayout.setVisibility(View.VISIBLE);

                holder.binding.addItems.setBackground(context.getDrawable(R.drawable.continue_items_table_button_view));

            } else {
                holder.binding.addItems.setText("Add New Bill");
                holder.binding.billPrintLayout.setVisibility(View.GONE);
                holder.binding.addItems.setBackground(context.getDrawable(R.drawable.add_items_table_button_view));
                holder.binding.billAmount.setText(MainActivity.currencyName + " 0.00");
            }

        }

    }

    @Override
    public int getItemCount() {
        return noOfTablesList;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        final TableListBinding binding;

        public MyViewHolder(@NonNull TableListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

    }
}
