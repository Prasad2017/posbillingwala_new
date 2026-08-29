package com.posbillingwala.owner.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Fragment.AddCustomerProductCategory;
import com.posbillingwala.owner.Fragment.AddCustomerSubcategory;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.ProductCategoryResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.CategoryListBinding;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.MyViewHolder> {

    private final Context context;
    private final List<ProductCategoryResponse> productCategoryResponseList;

    public CategoryAdapter(Context context, List<ProductCategoryResponse> productCategoryResponseList) {
        this.context = context;
        this.productCategoryResponseList = productCategoryResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout using ViewBinding
        CategoryListBinding binding = CategoryListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductCategoryResponse productCategoryResponse = productCategoryResponseList.get(position);

        holder.binding.srNo.setText(String.valueOf(position + 1));
        holder.binding.categoryName.setText(productCategoryResponse.getCategoryName());

        holder.binding.categorySubcategories.setOnClickListener(v -> {
            AddCustomerSubcategory fragment = new AddCustomerSubcategory();
            Bundle bundle = new Bundle();
            bundle.putString("categoryId", productCategoryResponse.getCategoryId());
            bundle.putString("categoryName", productCategoryResponse.getCategoryName());
            fragment.setArguments(bundle);
            ((MainActivity) context).loadFragment(fragment, true);
        });

        holder.binding.categoryEdit.setOnClickListener(v -> updateCategoryDialog(productCategoryResponse.getCategoryId(), productCategoryResponse.getCategoryName()));

        holder.binding.categoryRemove.setOnClickListener(v -> deleteCategoryDialog(productCategoryResponse.getCategoryId()));
    }

    public void deleteCategoryDialog(String categoryId) {
        BottomSheetUi.showConfirm(context, "Are you Sure?", "Do you want to delete this category?",
                "YES", "NO", true, () -> deleteCategory(categoryId));
    }

    public void updateCategoryDialog(String categoryId, String categoryName) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.update_category_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(context, dialogView, false);
        if (sheet == null) {
            return;
        }

        TextInputEditText categoryNameTxt = dialogView.findViewById(R.id.categoryName);
        TextView updateCategoryTxt = dialogView.findViewById(R.id.updateCategory);
        TextView dismissCategoryTxt = dialogView.findViewById(R.id.dismissCategory);

        categoryNameTxt.setText(categoryName);
        categoryNameTxt.setSelection(categoryNameTxt.getText().toString().length());

        dismissCategoryTxt.setOnClickListener(v -> sheet.dismiss());

        updateCategoryTxt.setOnClickListener(v -> {
            sheet.dismiss();
            updateCategory(categoryId, categoryNameTxt.getText().toString());
        });
    }

    public void updateCategory(String categoryId, String categoryName) {
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
                    if ("1".equalsIgnoreCase(response.body().getStatus())) {
                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
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
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText("Oops...");
                sweetAlertDialog.setContentText("Something went wrong!");
                sweetAlertDialog.setCancelClickListener(sweetAlertDialog1 -> sweetAlertDialog1.dismiss()).show();
            }
        });
    }

    public void deleteCategory(String categoryId) {
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
                    if ("1".equalsIgnoreCase(response.body().getStatus())) {
                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
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
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText("Oops...");
                sweetAlertDialog.setContentText("Something went wrong!");
                sweetAlertDialog.setCancelClickListener(sweetAlertDialog1 -> sweetAlertDialog1.dismiss()).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return productCategoryResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        final CategoryListBinding binding;

        public MyViewHolder(@NonNull CategoryListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
