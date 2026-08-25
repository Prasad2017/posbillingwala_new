package com.posbillingwala.admin.Adapter;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.posbillingwala.admin.Fragment.AddCustomerProductCategory;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.ProductCategoryResponse;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.CategoryListBinding;
import com.posbillingwala.admin.databinding.UpdateCategoryDialogBinding;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.MyViewHolder> {

    Context context;
    List<ProductCategoryResponse> productCategoryResponseList;


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

        holder.binding.srNo.setText("" + (position + 1));
        String label = productCategoryResponse.getCategoryName();
        if (productCategoryResponse.getFoodTypeName() != null && productCategoryResponse.getFoodTypeName().length() > 0) {
            label = label + " (" + productCategoryResponse.getFoodTypeName() + ")";
        }
        holder.binding.categoryName.setText(label);

        holder.binding.categorySubcategories.setVisibility(View.GONE);

        holder.binding.categoryEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCategoryDialog(productCategoryResponse.getCategoryId(), productCategoryResponse.getCategoryName());
            }
        });

        holder.binding.categoryRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteCategoryDialog(productCategoryResponse.getCategoryId());
            }
        });

    }

    private void deleteCategoryDialog(String categoryId) {

        new MaterialAlertDialogBuilder(context)
                .setTitle("Are you Sure?")
                .setMessage("Do you want to delete this category?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        deleteCategory(categoryId);
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

    private void updateCategoryDialog(String categoryId, String categoryName) {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        UpdateCategoryDialogBinding dialogBinding = UpdateCategoryDialogBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(dialogBinding.getRoot());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        dialogBinding.categoryName.setText("" + categoryName);
        dialogBinding.categoryName.setSelection(dialogBinding.categoryName.getText().toString().length());

        dialogBinding.dismissCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialogBinding.updateCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                updateCategory(categoryId, dialogBinding.categoryName.getText().toString());
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);

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
                        Toast.makeText(context, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        AddCustomerProductCategory.getProductCategoryList();
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
                        Toast.makeText(context, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        AddCustomerProductCategory.getProductCategoryList();
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
        return productCategoryResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final CategoryListBinding binding;

        public MyViewHolder(@NonNull CategoryListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
