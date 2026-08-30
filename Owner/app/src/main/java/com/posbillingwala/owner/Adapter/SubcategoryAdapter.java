package com.posbillingwala.owner.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.Extra.RowDividerUi;
import com.posbillingwala.owner.Fragment.AddCustomerSubcategory;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.ProductSubcategoryResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.SubcategoryListBinding;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubcategoryAdapter extends RecyclerView.Adapter<SubcategoryAdapter.MyViewHolder> {

    private final Context context;
    private final List<ProductSubcategoryResponse> subcategoryList;

    public SubcategoryAdapter(Context context, List<ProductSubcategoryResponse> subcategoryList) {
        this.context = context;
        this.subcategoryList = subcategoryList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
        SubcategoryListBinding binding = SubcategoryListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductSubcategoryResponse item = subcategoryList.get(position);
        holder.binding.srNo.setText(String.valueOf(position + 1));
        holder.binding.subcategoryName.setText(item.getSubcategoryName());
        holder.binding.subcategoryEdit.setOnClickListener(v -> updateSubcategoryDialog(item));
        holder.binding.subcategoryRemove.setOnClickListener(v -> deleteSubcategoryDialog(item.getSubcategoryId()));

        RowDividerUi.bindLastItem(holder.binding.rowDivider, position, getItemCount());
    }

    private void updateSubcategoryDialog(ProductSubcategoryResponse item) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.update_category_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(context, dialogView, false);
        if (sheet == null) {
            return;
        }

        TextInputEditText nameTxt = dialogView.findViewById(R.id.categoryName);
        TextView updateTxt = dialogView.findViewById(R.id.updateCategory);
        TextView dismissTxt = dialogView.findViewById(R.id.dismissCategory);

        nameTxt.setHint("Subcategory Name");
        nameTxt.setText(item.getSubcategoryName());
        if (item.getSubcategoryName() != null) {
            nameTxt.setSelection(item.getSubcategoryName().length());
        }

        dismissTxt.setOnClickListener(v -> sheet.dismiss());
        updateTxt.setOnClickListener(v -> {
            String newName = nameTxt.getText() != null ? nameTxt.getText().toString().trim() : "";
            if (newName.isEmpty()) {
                Toast.makeText(context, "Please enter subcategory name", Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            updateSubcategory(item.getSubcategoryId(), newName);
        });
    }

    private void updateSubcategory(String subcategoryId, String subcategoryName) {
        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().updateSubcategory(MainActivity.userId, subcategoryId, subcategoryName)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                        pDialog.dismiss();
                        if (response.body() != null) {
                            Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            if ("1".equalsIgnoreCase(response.body().getStatus())) {
                                AddCustomerSubcategory.getSubcategoryList();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        pDialog.dismiss();
                        Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteSubcategoryDialog(String subcategoryId) {
        BottomSheetUi.showConfirm(context, "Are you Sure?", "Do you want to delete this subcategory?",
                "YES", "NO", true, () -> deleteSubcategory(subcategoryId));
    }

    private void deleteSubcategory(String subcategoryId) {
        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().deleteSubcategory(MainActivity.userId, subcategoryId)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                        pDialog.dismiss();
                        if (response.body() != null) {
                            Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            if ("1".equalsIgnoreCase(response.body().getStatus())) {
                                AddCustomerSubcategory.getSubcategoryList();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        pDialog.dismiss();
                        Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public int getItemCount() {
        return subcategoryList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private final SubcategoryListBinding binding;

        MyViewHolder(@NonNull SubcategoryListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
