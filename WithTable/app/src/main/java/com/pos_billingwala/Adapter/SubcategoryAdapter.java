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
import com.pos_billingwala.Fragment.AddSubcategory;
import com.pos_billingwala.Model.ProductSubcategoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.SubcategoryListBinding;

import java.util.List;

@SuppressLint("SetTextI18n, NotifyDataSetChanged")
public class SubcategoryAdapter extends RecyclerView.Adapter<SubcategoryAdapter.MyViewHolder> {

    Context context;
    List<ProductSubcategoryResponse> subcategoryList;
    POSBillingWalaDatabase posBillingWalaDatabase;

    public SubcategoryAdapter(Context context, List<ProductSubcategoryResponse> subcategoryList) {
        this.context = context;
        this.subcategoryList = subcategoryList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(SubcategoryListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ProductSubcategoryResponse item = subcategoryList.get(position);
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        holder.binding.srNo.setText("" + (position + 1));
        holder.binding.subcategoryName.setText(item.getSubcategoryName());

        holder.binding.subcategoryEdit.setOnClickListener(v -> updateSubcategory(item));
        holder.binding.subcategoryRemove.setOnClickListener(v -> deleteSubcategory(item.getSubcategoryId()));
    }

    private void updateSubcategory(ProductSubcategoryResponse item) {
        Activity activity = (Activity) context;
        View content = LayoutInflater.from(activity).inflate(R.layout.update_subcategory_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextInputEditText subcategoryNameTxt = content.findViewById(R.id.subcategoryName);
        TextView updateSubcategoryTxt = content.findViewById(R.id.updateSubcategory);
        TextView dismissSubcategoryTxt = content.findViewById(R.id.dismissSubcategory);

        subcategoryNameTxt.setText(item.getSubcategoryName());
        subcategoryNameTxt.setSelection(subcategoryNameTxt.getText().toString().length());

        dismissSubcategoryTxt.setOnClickListener(v -> sheet.dismiss());

        updateSubcategoryTxt.setOnClickListener(v -> {
            String newName = subcategoryNameTxt.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.toast_please_enter_subcategory_name), Toast.LENGTH_SHORT).show();
                return;
            }
            List<ProductSubcategoryResponse> existing = posBillingWalaDatabase.getProductSubcategoryNameList(
                    item.getCategoryId(), newName);
            if (!existing.isEmpty() && !existing.get(0).getSubcategoryId().equals(item.getSubcategoryId())) {
                Toast.makeText(context, context.getString(R.string.toast_subcategory_already_exists_in_this_categ), Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            posBillingWalaDatabase.updateProductSubcategory(item.getSubcategoryId(), newName, 0);
            Toast.makeText(context, context.getString(R.string.toast_subcategory_updated), Toast.LENGTH_SHORT).show();
            AddSubcategory.getSubcategoryList();
        });
    }

    private void deleteSubcategory(String subcategoryId) {
        BottomSheetUi.showConfirm(
                context,
                context.getString(R.string.toast_are_you_sure),
                context.getString(R.string.toast_do_you_want_to_delete_this_subcategory),
                "YES",
                "NO",
                true,
                () -> {
                    posBillingWalaDatabase.deleteProductSubcategory(subcategoryId);
                    Toast.makeText(context, context.getString(R.string.toast_subcategory_deleted), Toast.LENGTH_SHORT).show();
                    AddSubcategory.getSubcategoryList();
                });
    }

    @Override
    public int getItemCount() {
        return subcategoryList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        SubcategoryListBinding binding;

        public MyViewHolder(SubcategoryListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
