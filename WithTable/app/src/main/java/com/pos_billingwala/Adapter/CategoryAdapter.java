package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Fragment.AddCategory;
import com.pos_billingwala.Model.FoodTypeResponse;
import com.pos_billingwala.Model.ProductCategoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.CategoryListBinding;

import java.util.List;

@SuppressLint("SetTextI18n, NotifyDataSetChanged")
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.MyViewHolder> {

    Context context;
    List<ProductCategoryResponse> productCategoryResponseList;
    POSBillingWalaDatabase posBillingWalaDatabase;

    public CategoryAdapter(Context context, List<ProductCategoryResponse> productCategoryResponseList) {
        this.context = context;
        this.productCategoryResponseList = productCategoryResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(CategoryListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        ProductCategoryResponse productCategoryResponse = productCategoryResponseList.get(position);
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        holder.binding.srNo.setText("" + (position + 1));
        String typeLabel = resolveFoodTypeLabel(productCategoryResponse.getFoodTypeId());
        if (!typeLabel.isEmpty()) {
            holder.binding.categoryName.setText(productCategoryResponse.getCategoryName() + " (" + typeLabel + ")");
        } else {
            holder.binding.categoryName.setText(productCategoryResponse.getCategoryName());
        }

        holder.binding.categoryEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCategory(productCategoryResponse);
            }
        });

        holder.binding.categoryRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteCategory(productCategoryResponse.getCategoryId());
            }
        });

    }

    public void updateCategory(ProductCategoryResponse productCategoryResponse) {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.update_category_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextInputEditText categoryNameTxt = dialog.findViewById(R.id.categoryName);
        RadioGroup foodTypeGroup = dialog.findViewById(R.id.foodTypeGroup);
        RadioButton foodTypeFood = dialog.findViewById(R.id.foodTypeFood);
        RadioButton foodTypeBeverage = dialog.findViewById(R.id.foodTypeBeverage);
        TextView updateCategoryTxt = dialog.findViewById(R.id.updateCategory);
        TextView dismissCategoryTxt = dialog.findViewById(R.id.dismissCategory);

        categoryNameTxt.setText(productCategoryResponse.getCategoryName());
        categoryNameTxt.setSelection(categoryNameTxt.getText().toString().length());

        long beverageId = posBillingWalaDatabase.getFoodTypeIdByCode(FoodTypeResponse.CODE_BEVERAGE);
        if (productCategoryResponse.getFoodTypeId() != null
                && String.valueOf(beverageId).equals(productCategoryResponse.getFoodTypeId())) {
            foodTypeBeverage.setChecked(true);
        } else {
            foodTypeFood.setChecked(true);
        }

        dismissCategoryTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        updateCategoryTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                long foodTypeId = foodTypeBeverage.isChecked()
                        ? posBillingWalaDatabase.getFoodTypeIdByCode(FoodTypeResponse.CODE_BEVERAGE)
                        : posBillingWalaDatabase.getDefaultFoodTypeId();
                posBillingWalaDatabase.updateCategory(
                        productCategoryResponse.getCategoryId(),
                        categoryNameTxt.getText().toString(),
                        0,
                        foodTypeId);
                Toast.makeText(context, context.getString(R.string.toast_category_updated), Toast.LENGTH_SHORT).show();
                AddCategory.getHomeProductCategoryList();
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);

    }

    private String resolveFoodTypeLabel(String foodTypeId) {
        if (foodTypeId == null || foodTypeId.trim().isEmpty()) {
            return "";
        }
        try {
            return posBillingWalaDatabase.getFoodTypeNameById(Long.parseLong(foodTypeId));
        } catch (NumberFormatException e) {
            return "";
        }
    }

    public void deleteCategory(String categoryId) {

        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.toast_are_you_sure))
                .setMessage(context.getString(R.string.toast_do_you_want_to_delete_this_category))
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        posBillingWalaDatabase.deleteCategory(categoryId);
                        Toast.makeText(context, context.getString(R.string.toast_category_deleted), Toast.LENGTH_SHORT).show();
                        AddCategory.getHomeProductCategoryList();
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
        return productCategoryResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        CategoryListBinding binding;

        public MyViewHolder(CategoryListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }

    }
}
