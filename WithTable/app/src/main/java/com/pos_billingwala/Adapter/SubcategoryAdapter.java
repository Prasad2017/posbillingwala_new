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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
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
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.update_subcategory_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextInputEditText subcategoryNameTxt = dialog.findViewById(R.id.subcategoryName);
        TextView updateSubcategoryTxt = dialog.findViewById(R.id.updateSubcategory);
        TextView dismissSubcategoryTxt = dialog.findViewById(R.id.dismissSubcategory);

        subcategoryNameTxt.setText(item.getSubcategoryName());
        subcategoryNameTxt.setSelection(subcategoryNameTxt.getText().toString().length());

        dismissSubcategoryTxt.setOnClickListener(v -> dialog.dismiss());

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
            dialog.dismiss();
            posBillingWalaDatabase.updateProductSubcategory(item.getSubcategoryId(), newName, 0);
            Toast.makeText(context, context.getString(R.string.toast_subcategory_updated), Toast.LENGTH_SHORT).show();
            AddSubcategory.getSubcategoryList();
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    private void deleteSubcategory(String subcategoryId) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.toast_are_you_sure))
                .setMessage(context.getString(R.string.toast_do_you_want_to_delete_this_subcategory))
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        posBillingWalaDatabase.deleteProductSubcategory(subcategoryId);
                        Toast.makeText(context, context.getString(R.string.toast_subcategory_deleted), Toast.LENGTH_SHORT).show();
                        AddSubcategory.getSubcategoryList();
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
