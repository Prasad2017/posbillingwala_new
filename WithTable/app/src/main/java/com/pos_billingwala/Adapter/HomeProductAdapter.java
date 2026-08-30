package com.pos_billingwala.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Interface.ClickListerInterface;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.databinding.HomeProductListBinding;

import java.util.List;

public class HomeProductAdapter extends RecyclerView.Adapter<HomeProductAdapter.MyViewHolder> {

    Context context;
    List<ProductResponse> productResponseList;
    ClickListerInterface clickListerInterface;
    String productPriceUnit;

    public HomeProductAdapter(Context context, List<ProductResponse> productResponseList, ClickListerInterface clickListerInterface) {
        this.context = context;
        this.productResponseList = productResponseList;
        this.clickListerInterface = clickListerInterface;
    }

    public void submitList(List<ProductResponse> products) {
        this.productResponseList = products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(HomeProductListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        ProductResponse productResponse = productResponseList.get(position);

        holder.binding.productName.setText(productResponse.getProductName());
        float productPrice = Float.parseFloat(productResponse.getProductPrice());
        if (!CreatePos.companyResponseList.isEmpty()) {
            if (CreatePos.companyResponseList.get(0).getGstStatus() != null) {
                if (CreatePos.companyResponseList.get(0).getGstStatus().equalsIgnoreCase("On")) {
                    float totalCGST = 0f, totalSGST = 0f;
                    if (!productResponse.getProductCGST().equalsIgnoreCase("")) {
                        totalCGST += Float.parseFloat(productResponse.getProductCGST());
                    }
                    if (!productResponse.getProductSGST().equalsIgnoreCase("")) {
                        totalSGST += Float.parseFloat(productResponse.getProductSGST());
                    }
                    float totalPerProductGST = productPrice + (productPrice * ((totalCGST + totalSGST) / 100));
                    productPriceUnit = MainActivity.currencyName + " " + totalPerProductGST + "/" + productResponse.getProductUnit();
                } else {
                    productPriceUnit = MainActivity.currencyName + " " + productPrice + "/" + productResponse.getProductUnit();
                }
            } else {
                productPriceUnit = MainActivity.currencyName + " " + productPrice + "/" + productResponse.getProductUnit();
            }
        } else {
            productPriceUnit = MainActivity.currencyName + " " + productPrice + "/" + productResponse.getProductUnit();
        }
        holder.binding.productPriceUnit.setText(productPriceUnit);
        boolean hasQty = productResponse.getProductCartQuantity() != null
                && !productResponse.getProductCartQuantity().trim().isEmpty()
                && !"0".equals(productResponse.getProductCartQuantity().trim());
        if (hasQty) {
            holder.binding.productQuantity.setText(productResponse.getProductCartQuantity());
            holder.binding.productQuantity.setVisibility(View.VISIBLE);
            holder.binding.productAdd.setVisibility(View.GONE);
        } else {
            holder.binding.productQuantity.setText("");
            holder.binding.productQuantity.setVisibility(View.GONE);
            holder.binding.productAdd.setVisibility(View.VISIBLE);
        }

        View.OnClickListener addListener = v -> clickListerInterface.productClicked(productResponse);
        holder.binding.productCardView.setOnClickListener(addListener);
        holder.binding.productAdd.setOnClickListener(addListener);

    }

    @Override
    public int getItemCount() {
        return productResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        HomeProductListBinding binding;

        public MyViewHolder(HomeProductListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
