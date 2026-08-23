package com.posbillingwala.dealer.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Fragment.AddCustomerProductCategory;
import com.posbillingwala.dealer.Fragment.AddCustomerSubcategory;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.Model.ProductCategoryResponse;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.databinding.CategoryListBinding;
import com.posbillingwala.dealer.databinding.UpdateCategoryDialogBinding;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.MyViewHolder> {

    private Context context;
    private List<ProductCategoryResponse> productCategoryResponseList;

    public CategoryAdapter(Context context, List<ProductCategoryResponse> productCategoryResponseList) {
        this.context = context;
        this.productCategoryResponseList = productCategoryResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CategoryListBinding binding = CategoryListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductCategoryResponse productCategoryResponse = productCategoryResponseList.get(position);

        holder.binding.srNo.setText(String.valueOf(position + 1));
        String label = productCategoryResponse.getCategoryName();
        if (productCategoryResponse.getFoodTypeName() != null && productCategoryResponse.getFoodTypeName().length() > 0) {
            label = label + " (" + productCategoryResponse.getFoodTypeName() + ")";
        }
        holder.binding.categoryName.setText(label);

        holder.binding.categorySubcategories.setOnClickListener(v -> {
            AddCustomerSubcategory fragment = new AddCustomerSubcategory();
            Bundle bundle = new Bundle();
            bundle.putString("customerId", AddCustomerProductCategory.customerId);
            bundle.putString("categoryId", productCategoryResponse.getCategoryId());
            bundle.putString("categoryName", productCategoryResponse.getCategoryName());
            fragment.setArguments(bundle);
            ((MainActivity) context).loadFragment(fragment, true);
        });

        holder.binding.categoryEdit.setOnClickListener(v -> updateCategoryDialog(productCategoryResponse.getCategoryId(), productCategoryResponse.getCategoryName()));
        holder.binding.categoryRemove.setOnClickListener(v -> deleteCategoryDialog(productCategoryResponse.getCategoryId()));
    }

    private void deleteCategoryDialog(String categoryId) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Are you Sure?")
                .setMessage("Do you want to delete this category?")
                .setPositiveButton("YES", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    deleteCategory(categoryId);
                })
                .setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    private void updateCategoryDialog(String categoryId, String categoryName) {
        UpdateCategoryDialogBinding dialogBinding = UpdateCategoryDialogBinding.inflate(LayoutInflater.from(context));
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setView(dialogBinding.getRoot());

        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogBinding.categoryName.setText(categoryName);
        dialogBinding.categoryName.setSelection(dialogBinding.categoryName.getText().toString().length());

        dialogBinding.dismissCategory.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.updateCategory.setOnClickListener(v -> {
            dialog.dismiss();
            updateCategory(categoryId, dialogBinding.categoryName.getText().toString());
        });

        dialog.show();
    }

    private void updateCategory(String categoryId, String categoryName) {
        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().updateCategory(categoryId, categoryName);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        // Assuming AddCustomerProductCategory has a static method to refresh data
                        AddCustomerProductCategory.getProductCategoryList();
                    } else {
                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Oops...")
                        .setContentText("Something went wrong!")
                        .setCancelClickListener(SweetAlertDialog::dismiss)
                        .show();
            }
        });
    }

    private void deleteCategory(String categoryId) {
        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().deleteCategory(categoryId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        // Assuming AddCustomerProductCategory has a static method to refresh data
                        AddCustomerProductCategory.getProductCategoryList();
                    } else {
                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Oops...")
                        .setContentText("Something went wrong!")
                        .setCancelClickListener(SweetAlertDialog::dismiss)
                        .show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return productCategoryResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private CategoryListBinding binding;

        public MyViewHolder(@NonNull CategoryListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
