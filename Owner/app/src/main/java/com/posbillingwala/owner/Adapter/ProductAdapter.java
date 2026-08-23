package com.posbillingwala.owner.Adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Fragment.AllCustomerProductList;
import com.posbillingwala.owner.Fragment.UpdateProduct;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.ProductResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.ProductListBinding; // Import the generated binding class

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.MyViewHolder> {

    private final Context context;
    private final List<ProductResponse> productResponseList;

    public ProductAdapter(Context context, List<ProductResponse> productResponseList) {
        this.context = context;
        this.productResponseList = productResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use ViewBinding to inflate the layout
        ProductListBinding binding = ProductListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductResponse productResponse = productResponseList.get(position);

        String productCategory = "<b>Product Category</b>: " + productResponse.getCategoryName();
        holder.binding.productCategory.setText(Html.fromHtml(productCategory));
        String productName = "<b>Product Name</b>: " + productResponse.getProductName();
        holder.binding.productName.setText(Html.fromHtml(productName));
        String productPrice = "<b>Product Price(Without GST)</b>: " + MainActivity.currency + " " + productResponse.getProductPrice();
        holder.binding.productPrice.setText(Html.fromHtml(productPrice));
        String productUnit = "<b>Product Unit</b>: " + productResponse.getProductUnit();
        holder.binding.productUnit.setText(Html.fromHtml(productUnit));
        String productCGST = "<b>Product CGST</b>: " + productResponse.getProductCGST();
        holder.binding.productCGST.setText(Html.fromHtml(productCGST));
        String productSGST = "<b>Product SGST</b>: " + productResponse.getProductSGST();
        holder.binding.productSGST.setText(Html.fromHtml(productSGST));

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
    }

    private void deleteProductDialog(String productId) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Are you Sure?")
                .setMessage("Do you want to delete this product?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        deleteProduct(productId);
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

    private void deleteProduct(String productId) {
        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().deleteProduct(productId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        AllCustomerProductList.getProductList();
                    } else {
                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText("Oops...");
                sweetAlertDialog.setContentText("Something went wrong!");
                sweetAlertDialog.setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismiss();
                    }
                }).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return productResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final ProductListBinding binding;

        public MyViewHolder(@NonNull ProductListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
