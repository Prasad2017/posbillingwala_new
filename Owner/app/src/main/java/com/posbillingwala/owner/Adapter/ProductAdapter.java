package com.posbillingwala.owner.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Fragment.AllCustomerProductList;
import com.posbillingwala.owner.Fragment.ManageCustomerProductPortions;
import com.posbillingwala.owner.Fragment.UpdateProduct;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.ProductResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.ProductListBinding;

import java.util.List;
import java.util.Locale;

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

        String name = productResponse.getProductName() != null ? productResponse.getProductName().trim() : "";
        holder.binding.productInitial.setText(getInitial(name));
        holder.binding.productName.setText(name);

        String categoryName = productResponse.getCategoryName();
        if (TextUtils.isEmpty(categoryName)) {
            holder.binding.productCategory.setVisibility(View.GONE);
        } else {
            holder.binding.productCategory.setVisibility(View.VISIBLE);
            holder.binding.productCategory.setText(categoryName);
        }

        String subcategoryName = productResponse.getSubcategoryName();
        if (!TextUtils.isEmpty(subcategoryName)) {
            holder.binding.productSubcategory.setVisibility(View.VISIBLE);
            holder.binding.productSubcategory.setText(subcategoryName);
        } else {
            holder.binding.productSubcategory.setVisibility(View.GONE);
        }

        String code = productResponse.getProductCode();
        if (!TextUtils.isEmpty(code)) {
            holder.binding.productCode.setText("#" + code);
            holder.binding.productCode.setVisibility(View.VISIBLE);
        } else {
            holder.binding.productCode.setVisibility(View.GONE);
        }

        String unit = productResponse.getProductUnit();
        if (!TextUtils.isEmpty(unit)) {
            holder.binding.productUnit.setVisibility(View.VISIBLE);
            holder.binding.productUnit.setText(unit);
        } else {
            holder.binding.productUnit.setVisibility(View.GONE);
        }

        String currency = MainActivity.currency != null ? MainActivity.currency + " " : "";
        String price = productResponse.getProductPrice();
        holder.binding.productPrice.setText(!TextUtils.isEmpty(price) ? currency + price : "");

        String cgst = productResponse.getProductCGST();
        String sgst = productResponse.getProductSGST();
        boolean hasCgst = !TextUtils.isEmpty(cgst);
        boolean hasSgst = !TextUtils.isEmpty(sgst);
        if (hasCgst || hasSgst) {
            holder.binding.productGstRow.setVisibility(View.VISIBLE);
            holder.binding.productCGST.setText("CGST " + (hasCgst ? cgst + "%" : "-"));
            holder.binding.productSGST.setText("SGST " + (hasSgst ? sgst + "%" : "-"));
        } else {
            holder.binding.productGstRow.setVisibility(View.GONE);
        }

        holder.binding.managePortionsLabel.setText(R.string.manage_portions);

        holder.binding.deleteProduct.setOnClickListener(v -> deleteProductDialog(productResponse.getProductId()));
        holder.binding.updateProduct.setOnClickListener(v -> {
            UpdateProduct updateProduct = new UpdateProduct();
            Bundle bundle = new Bundle();
            bundle.putString("productId", productResponse.getProductId());
            updateProduct.setArguments(bundle);
            ((MainActivity) context).loadFragment(updateProduct, true);
        });
        holder.binding.managePortions.setOnClickListener(v -> {
            ManageCustomerProductPortions fragment = new ManageCustomerProductPortions();
            Bundle bundle = new Bundle();
            bundle.putString("productId", productResponse.getProductId());
            bundle.putString("productName", productResponse.getProductName());
            fragment.setArguments(bundle);
            ((MainActivity) context).loadFragment(fragment, true);
        });
    }

    private static String getInitial(String name) {
        if (TextUtils.isEmpty(name)) {
            return "P";
        }
        return name.substring(0, 1).toUpperCase(Locale.getDefault());
    }

    private void deleteProductDialog(String productId) {
        BottomSheetUi.showConfirm(context, "Are you Sure?", "Do you want to delete this product?",
                "YES", "NO", true, () -> deleteProduct(productId));
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
