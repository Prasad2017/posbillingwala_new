package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.BluetoothPrint;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.databinding.TakeAwayListBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InvoiceTakAwayAdapter extends RecyclerView.Adapter<InvoiceTakAwayAdapter.MyViewHolder> {

    Context context;
    int noOfTablesList;
    POSBillingWalaDatabase posBillingWalaDatabase;
    String cartOrderStatus = "table_wise";
    List<ProductCartResponse> productCartResponseList = new ArrayList<>();
    List<ProductCartResponse> productTakeAwayResponseList;
    List<CompanyResponse> companyResponseList = new ArrayList<>();


    public InvoiceTakAwayAdapter(Context context, List<ProductCartResponse> productTakeAwayResponseList) {
        this.context = context;
        this.productTakeAwayResponseList = productTakeAwayResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(TakeAwayListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        ProductCartResponse productCartResponse = productTakeAwayResponseList.get(position);

        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        int pos = position + 1;
        holder.binding.srNo.setText(String.valueOf(pos));
        holder.binding.takeAwayNo.setText(productCartResponse.getNoOfTable());

        getTableAmount(holder, productCartResponse.getNoOfTable());


        holder.binding.goToItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                CreatePos createPos = new CreatePos();
                Bundle bundle = new Bundle();
                bundle.putString("tableNumber", productCartResponse.getNoOfTable());
                bundle.putString("cartOrderStatus", "take_away");
                createPos.setArguments(bundle);
                ((MainActivity) context).loadFragment(createPos, true);
            }
        });

        holder.binding.goToPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, BluetoothPrint.class);
                intent.putExtra("invoiceRunningStatus", "printBill");
                intent.putExtra("tableNumber", productCartResponse.getNoOfTable());
                intent.putExtra("cartOrderStatus", "take_away");
                context.startActivity(intent);
            }
        });

    }

    @SuppressLint("SetTextI18n")
    public void getTableAmount(MyViewHolder holder, String tableNumber) {

        companyResponseList.clear();
        companyResponseList = posBillingWalaDatabase.getCompanyDetails();

        if (!companyResponseList.isEmpty()) {

            productCartResponseList.clear();
            productCartResponseList = posBillingWalaDatabase.getCartProductList(tableNumber, "take_away");
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

                holder.binding.takeAwayBillAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalAmount));

            }

        }

    }

    @Override
    public int getItemCount() {
        return productTakeAwayResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        public final TakeAwayListBinding binding;

        public MyViewHolder(TakeAwayListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
