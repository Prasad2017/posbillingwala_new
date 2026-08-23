package com.posbillingwala.admin.Adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Fragment.AllCustomerProductList;
import com.posbillingwala.admin.Fragment.ManageCustomerProductPortions;
import com.posbillingwala.admin.Fragment.UpdateProduct;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.ProductResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;

import java.util.List;

import butterknife.BindViews;
import butterknife.ButterKnife;
import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.MyViewHolder> {

    Context context;
    List<ProductResponse> productResponseList;


    public ProductAdapter(Context context, List<ProductResponse> productResponseList) {
        this.context = context;
        this.productResponseList = productResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_list, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        ProductResponse productResponse = productResponseList.get(position);

        String productCategory = "<b>Product Category</b>: " + productResponse.getCategoryName();
        holder.textViews.get(0).setText(Html.fromHtml(productCategory));
        String productName = "<b>Product Name</b>: " + productResponse.getProductName();
        holder.textViews.get(1).setText(Html.fromHtml(productName));
        if (productResponse.getSubcategoryName() != null && productResponse.getSubcategoryName().length() > 0) {
            String subcategoryLine = "<b>Subcategory</b>: " + productResponse.getSubcategoryName();
            holder.textViews.get(1).setText(Html.fromHtml(productName + "<br>" + subcategoryLine));
        }
        String productPrice = "<b>Product Price(Without GST)</b>: " + MainActivity.currency + " " + productResponse.getProductPrice();
        holder.textViews.get(2).setText(Html.fromHtml(productPrice));
        String productUnit = "<b>Product Unit</b>: " + productResponse.getProductUnit();
        holder.textViews.get(3).setText(Html.fromHtml(productUnit));
        String productCGST = "<b>Product CGST</b>: " + productResponse.getProductCGST();
        holder.textViews.get(4).setText(Html.fromHtml(productCGST));
        String productSGST = "<b>Product SGST</b>: " + productResponse.getProductSGST();
        holder.textViews.get(5).setText(Html.fromHtml(productSGST));

        holder.productActions.get(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteProductDialog(productResponse.getProductId());
            }
        });

        holder.productActions.get(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateProduct updateProduct = new UpdateProduct();
                Bundle bundle = new Bundle();
                bundle.putString("productId", "" + productResponse.getProductId());
                bundle.putString("customerId", "" + productResponse.getUserId());
                updateProduct.setArguments(bundle);
                ((MainActivity) context).loadFragment(updateProduct, true);
            }
        });

        holder.productActions.get(2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ManageCustomerProductPortions fragment = new ManageCustomerProductPortions();
                Bundle bundle = new Bundle();
                bundle.putString("customerId", "" + productResponse.getUserId());
                bundle.putString("productId", "" + productResponse.getProductId());
                bundle.putString("productName", "" + productResponse.getProductName());
                fragment.setArguments(bundle);
                ((MainActivity) context).loadFragment(fragment, true);
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
                        Toast.makeText(context, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        AllCustomerProductList.getProductList();
                    } else {
                        Toast.makeText(context, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
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

    public class MyViewHolder extends RecyclerView.ViewHolder {

        @BindViews({R.id.productCategory, R.id.productName, R.id.productPrice, R.id.productUnit, R.id.productCGST, R.id.productSGST})
        List<TextView> textViews;
        @BindViews({R.id.deleteProduct, R.id.updateProduct, R.id.managePortions})
        List<TextView> productActions;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
