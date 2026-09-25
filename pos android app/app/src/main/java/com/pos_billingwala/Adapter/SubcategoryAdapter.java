package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
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
import com.pos_billingwala.Extra.SearchableDropdownView;
import com.pos_billingwala.Fragment.AddSubcategory;
import com.pos_billingwala.Model.ProductCategoryResponse;
import com.pos_billingwala.Model.ProductSubcategoryResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.SubcategoryListBinding;
import com.pos_billingwala.databinding.SubcategorySectionHeaderBinding;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("SetTextI18n, NotifyDataSetChanged")
public class SubcategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final Context context;
    private final List<Row> rows = new ArrayList<>();
    private POSBillingWalaDatabase posBillingWalaDatabase;

    public SubcategoryAdapter(Context context, List<ProductSubcategoryResponse> subcategoryList) {
        this.context = context;
        buildRows(subcategoryList);
    }

    private void buildRows(List<ProductSubcategoryResponse> subcategoryList) {
        rows.clear();
        if (subcategoryList == null || subcategoryList.isEmpty()) {
            return;
        }
        String lastCategoryKey = null;
        int indexInCategory = 0;
        for (ProductSubcategoryResponse item : subcategoryList) {
            String categoryId = item.getCategoryId() != null ? item.getCategoryId() : "";
            String categoryName = !TextUtils.isEmpty(item.getCategoryName())
                    ? item.getCategoryName().trim()
                    : context.getString(R.string.category_name);
            String key = categoryId + "|" + categoryName;
            if (!key.equals(lastCategoryKey)) {
                rows.add(Row.header(categoryName));
                lastCategoryKey = key;
                indexInCategory = 0;
            }
            indexInCategory++;
            rows.add(Row.item(item, indexInCategory));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(SubcategorySectionHeaderBinding.inflate(inflater, parent, false));
        }
        return new ItemViewHolder(SubcategoryListBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).binding.sectionHeaderTitle.setText(row.headerTitle);
            return;
        }

        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        ProductSubcategoryResponse item = row.subcategory;
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        itemHolder.binding.srNo.setText("" + row.indexInCategory);
        itemHolder.binding.subcategoryName.setText(item.getSubcategoryName());
        itemHolder.binding.subcategoryEdit.setOnClickListener(v -> updateSubcategory(item));
        itemHolder.binding.subcategoryRemove.setOnClickListener(v -> deleteSubcategory(item.getSubcategoryId()));

        boolean lastInList = position == rows.size() - 1;
        boolean nextIsHeader = !lastInList && rows.get(position + 1).isHeader;
        itemHolder.binding.rowDivider.setVisibility(lastInList || nextIsHeader ? View.GONE : View.VISIBLE);
    }

    private void updateSubcategory(ProductSubcategoryResponse item) {
        Activity activity = (Activity) context;
        View content = LayoutInflater.from(activity).inflate(R.layout.update_subcategory_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        SearchableDropdownView categoryDropdown = content.findViewById(R.id.categoryDropdown);
        TextInputEditText subcategoryNameTxt = content.findViewById(R.id.subcategoryName);
        TextView updateSubcategoryTxt = content.findViewById(R.id.updateSubcategory);
        TextView dismissSubcategoryTxt = content.findViewById(R.id.dismissSubcategory);

        posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        List<ProductCategoryResponse> categories = posBillingWalaDatabase.getProductCategoryList();
        if (categories == null || categories.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.toast_please_add_a_category_first), Toast.LENGTH_SHORT).show();
            sheet.dismiss();
            return;
        }

        String[] categoryIds = new String[categories.size()];
        String[] categoryNames = new String[categories.size()];
        int selectedIndex = 0;
        for (int i = 0; i < categories.size(); i++) {
            categoryIds[i] = categories.get(i).getCategoryId();
            categoryNames[i] = categories.get(i).getCategoryName();
            if (item.getCategoryId() != null
                    && item.getCategoryId().equals(String.valueOf(categoryIds[i]))) {
                selectedIndex = i;
            }
        }
        categoryDropdown.setItems(categoryNames);
        categoryDropdown.setSelectedIndex(selectedIndex);
        final int[] selectedCategoryIndex = {selectedIndex};
        categoryDropdown.setOnItemSelectedListener((position, label) -> selectedCategoryIndex[0] = position);

        subcategoryNameTxt.setText(item.getSubcategoryName());
        subcategoryNameTxt.setSelection(subcategoryNameTxt.getText().toString().length());

        dismissSubcategoryTxt.setOnClickListener(v -> sheet.dismiss());

        updateSubcategoryTxt.setOnClickListener(v -> {
            String newName = subcategoryNameTxt.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.toast_please_enter_subcategory_name), Toast.LENGTH_SHORT).show();
                return;
            }
            int catIndex = selectedCategoryIndex[0];
            if (catIndex < 0 || catIndex >= categoryIds.length) {
                Toast.makeText(context, context.getString(R.string.toast_please_select_a_category), Toast.LENGTH_SHORT).show();
                return;
            }
            String newCategoryId = categoryIds[catIndex];
            String newCategoryName = categoryNames[catIndex];

            List<ProductSubcategoryResponse> existing = posBillingWalaDatabase.getProductSubcategoryNameList(
                    newCategoryId, newName);
            if (!existing.isEmpty() && !existing.get(0).getSubcategoryId().equals(item.getSubcategoryId())) {
                Toast.makeText(context, context.getString(R.string.toast_subcategory_already_exists_in_this_categ), Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            posBillingWalaDatabase.updateProductSubcategory(
                    item.getSubcategoryId(), newCategoryId, newCategoryName, newName, 0);
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
        return rows.size();
    }

    private static final class Row {
        final boolean isHeader;
        final String headerTitle;
        final ProductSubcategoryResponse subcategory;
        final int indexInCategory;

        private Row(boolean isHeader, String headerTitle, ProductSubcategoryResponse subcategory, int indexInCategory) {
            this.isHeader = isHeader;
            this.headerTitle = headerTitle;
            this.subcategory = subcategory;
            this.indexInCategory = indexInCategory;
        }

        static Row header(String title) {
            return new Row(true, title, null, 0);
        }

        static Row item(ProductSubcategoryResponse subcategory, int indexInCategory) {
            return new Row(false, null, subcategory, indexInCategory);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final SubcategorySectionHeaderBinding binding;

        HeaderViewHolder(SubcategorySectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        final SubcategoryListBinding binding;

        ItemViewHolder(SubcategoryListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
