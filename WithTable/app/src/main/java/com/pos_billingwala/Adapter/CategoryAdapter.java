package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.RowDividerUi;
import com.pos_billingwala.Fragment.AddCategory;
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
        holder.binding.categoryName.setText(productCategoryResponse.getCategoryName());

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

        RowDividerUi.bindLastItem(holder.binding.rowDivider, position, getItemCount());
    }

    public void updateCategory(ProductCategoryResponse productCategoryResponse) {
        Activity activity = (Activity) context;
        View content = LayoutInflater.from(activity).inflate(R.layout.update_category_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextInputEditText categoryNameTxt = content.findViewById(R.id.categoryName);
        TextView updateCategoryTxt = content.findViewById(R.id.updateCategory);
        TextView dismissCategoryTxt = content.findViewById(R.id.dismissCategory);

        categoryNameTxt.setText(productCategoryResponse.getCategoryName());
        categoryNameTxt.setSelection(categoryNameTxt.getText().toString().length());

        dismissCategoryTxt.setOnClickListener(v -> sheet.dismiss());

        updateCategoryTxt.setOnClickListener(v -> {
            sheet.dismiss();
            long foodTypeId = 0;
            try {
                if (productCategoryResponse.getFoodTypeId() != null
                        && !productCategoryResponse.getFoodTypeId().trim().isEmpty()) {
                    foodTypeId = Long.parseLong(productCategoryResponse.getFoodTypeId());
                }
            } catch (NumberFormatException ignored) {
            }
            posBillingWalaDatabase.updateCategory(
                    productCategoryResponse.getCategoryId(),
                    categoryNameTxt.getText().toString(),
                    0,
                    foodTypeId);
            Toast.makeText(context, context.getString(R.string.toast_category_updated), Toast.LENGTH_SHORT).show();
            AddCategory.getHomeProductCategoryList();
        });
    }

    public void deleteCategory(String categoryId) {

        BottomSheetUi.showConfirm(
                context,
                context.getString(R.string.toast_are_you_sure),
                context.getString(R.string.toast_do_you_want_to_delete_this_category),
                "YES",
                "NO",
                true,
                () -> {
                    posBillingWalaDatabase.deleteCategory(categoryId);
                    Toast.makeText(context, context.getString(R.string.toast_category_deleted), Toast.LENGTH_SHORT).show();
                    AddCategory.getHomeProductCategoryList();
                });
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
