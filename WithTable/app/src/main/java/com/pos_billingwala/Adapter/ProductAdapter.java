package com.pos_billingwala.Adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Fragment.ManageProductPortions;
import com.pos_billingwala.Fragment.ProductMaster;
import com.pos_billingwala.Fragment.UpdateProduct;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ProductListBinding;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.MyViewHolder> {

    Context context;
    List<ProductResponse> productResponseList;
    POSBillingWalaDatabase posBillingWalaDatabase;

    public ProductAdapter(Context context, List<ProductResponse> productResponseList) {
        this.context = context;
        this.productResponseList = productResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(ProductListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        ProductResponse productResponse = productResponseList.get(position);
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        String productCategory = "<b>Product Category</b>: " + (productResponse.getCategoryName() != null ? productResponse.getCategoryName() : "-");
        holder.binding.productCategory.setText(Html.fromHtml(productCategory));
        String productName = "<b>Product Name</b>: " + productResponse.getProductName();
        holder.binding.productName.setText(Html.fromHtml(productName));
        String productPrice = "<b>Product Price(Without GST)</b>: " + MainActivity.currencyName + " " + productResponse.getProductPrice();
        holder.binding.productPrice.setText(Html.fromHtml(productPrice));
        String productUnit = "<b>Product Unit</b>: " + productResponse.getProductUnit();
        holder.binding.productUnit.setText(Html.fromHtml(productUnit));
        String productCGST = "<b>Product CGST</b>: " + (!productResponse.getProductCGST().equals("") ? productResponse.getProductCGST() : "-");
        holder.binding.productCGST.setText(Html.fromHtml(productCGST));
        String productSGST = "<b>Product SGST</b>: " + (!productResponse.getProductSGST().equals("") ? productResponse.getProductSGST() : "-");
        holder.binding.productSGST.setText(Html.fromHtml(productSGST));
        String productCode = "<b>Product Code</b>: " + (!productResponse.getProductCode().equals("") ? productResponse.getProductCode() : "-");
        holder.binding.productCode.setText(Html.fromHtml(productCode));

        int portionCount = posBillingWalaDatabase.countActiveProductPortions(productResponse.getProductId());
        if (portionCount > 0) {
            holder.binding.managePortions.setText(context.getString(R.string.manage_portions) + " (" + portionCount + ")");
        } else {
            holder.binding.managePortions.setText(context.getString(R.string.manage_portions));
        }

        holder.binding.deleteProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteProductDialog(productResponse.getProductId());
            }
        });

        holder.binding.updateProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateProduct updateProduct = new UpdateProduct();
                Bundle bundle = new Bundle();
                bundle.putString("productId", productResponse.getProductId());
                updateProduct.setArguments(bundle);
                ((MainActivity) context).loadFragment(updateProduct, true);
            }
        });

        holder.binding.managePortions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ManageProductPortions manageProductPortions = new ManageProductPortions();
                Bundle bundle = new Bundle();
                bundle.putString("productId", productResponse.getProductId());
                manageProductPortions.setArguments(bundle);
                ((MainActivity) context).loadFragment(manageProductPortions, true);
            }
        });

    }

    public void deleteProductDialog(String productId) {

        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.toast_are_you_sure))
                .setMessage(context.getString(R.string.toast_do_you_want_to_delete_this_product))
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        posBillingWalaDatabase.deleteProduct(productId);
                        Toast.makeText(context, context.getString(R.string.toast_product_delete_successfully), Toast.LENGTH_SHORT).show();
                        ProductMaster.getProductList();
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();

    }

    @Override
    public int getItemCount() {
        return productResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        public final ProductListBinding binding;

        public MyViewHolder(ProductListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
