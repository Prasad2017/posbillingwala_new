package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Fragment.ManageProductPortions;
import com.pos_billingwala.Fragment.ProductMaster;
import com.pos_billingwala.Fragment.UpdateProduct;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.PortionListBinding;
import com.pos_billingwala.databinding.ProductListBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressLint("SetTextI18n")
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.MyViewHolder> {

    Context context;
    List<ProductResponse> productResponseList;
    POSBillingWalaDatabase posBillingWalaDatabase;

    public ProductAdapter(Context context, List<ProductResponse> productResponseList) {
        this.context = context;
        this.productResponseList = productResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(ProductListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        ProductResponse productResponse = productResponseList.get(position);
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        String name = productResponse.getProductName() != null ? productResponse.getProductName().trim() : "";
        holder.binding.productInitial.setText(getInitial(name));
        holder.binding.productName.setText(name);
        holder.binding.productCategory.setText(
                !TextUtils.isEmpty(productResponse.getCategoryName())
                        ? productResponse.getCategoryName() : "");
        if (TextUtils.isEmpty(productResponse.getCategoryName())) {
            holder.binding.productCategory.setVisibility(View.GONE);
        } else {
            holder.binding.productCategory.setVisibility(View.VISIBLE);
        }

        String subcategoryName = productResponse.getSubcategoryName();
        if (TextUtils.isEmpty(subcategoryName) && !TextUtils.isEmpty(productResponse.getSubcategoryId())) {
            subcategoryName = posBillingWalaDatabase.getSubcategoryNameById(productResponse.getSubcategoryId());
            if (!TextUtils.isEmpty(subcategoryName)) {
                productResponse.setSubcategoryName(subcategoryName);
            }
        }
        if (!TextUtils.isEmpty(subcategoryName)) {
            holder.binding.productSubcategory.setVisibility(View.VISIBLE);
            holder.binding.productSubcategory.setText(subcategoryName);
        } else {
            holder.binding.productSubcategory.setVisibility(View.GONE);
        }

        List<ProductPortionResponse> portions = posBillingWalaDatabase.getProductPortionList(productResponse.getProductId());
        if (portions == null) {
            portions = new ArrayList<>();
        }

        String code = productResponse.getProductCode();
        if (!TextUtils.isEmpty(code)) {
            holder.binding.productCode.setText("#" + code);
            holder.binding.productCode.setVisibility(View.VISIBLE);
        } else {
            holder.binding.productCode.setVisibility(View.GONE);
        }

        bindUnitAndPrice(holder, productResponse, portions);
        bindPortions(holder, portions);

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

        if (!portions.isEmpty()) {
            holder.binding.managePortions.setContentDescription(
                    context.getString(R.string.manage_portions) + " (" + portions.size() + ")");
            holder.binding.managePortionsLabel.setText(
                    context.getString(R.string.manage_portions) + " (" + portions.size() + ")");
        } else {
            holder.binding.managePortions.setContentDescription(
                    context.getString(R.string.manage_portions));
            holder.binding.managePortionsLabel.setText(context.getString(R.string.manage_portions));
        }

        holder.binding.deleteProduct.setOnClickListener(v -> deleteProductDialog(productResponse.getProductId()));

        holder.binding.updateProduct.setOnClickListener(v -> {
            UpdateProduct updateProduct = new UpdateProduct();
            Bundle bundle = new Bundle();
            bundle.putString("productId", productResponse.getProductId());
            updateProduct.setArguments(bundle);
            ((MainActivity) context).loadFragment(updateProduct, true);
        });

        holder.binding.managePortions.setOnClickListener(v -> {
            ManageProductPortions manageProductPortions = new ManageProductPortions();
            Bundle bundle = new Bundle();
            bundle.putString("productId", productResponse.getProductId());
            manageProductPortions.setArguments(bundle);
            ((MainActivity) context).loadFragment(manageProductPortions, true);
        });
    }

    private void bindUnitAndPrice(MyViewHolder holder, ProductResponse productResponse,
                                    List<ProductPortionResponse> portions) {
        String currency = MainActivity.currencyName + " ";

        if (portions.isEmpty()) {
            String unit = productResponse.getProductUnit();
            if (!TextUtils.isEmpty(unit)) {
                holder.binding.productUnit.setText(unit);
                holder.binding.productUnit.setVisibility(View.VISIBLE);
            } else {
                holder.binding.productUnit.setVisibility(View.GONE);
            }

            String basePrice = productResponse.getProductPrice();
            holder.binding.productPrice.setText(currency + (TextUtils.isEmpty(basePrice) ? "0" : basePrice));
            return;
        }

        holder.binding.productUnit.setVisibility(View.GONE);
        holder.binding.productPrice.setText(formatPortionPriceSummary(portions, currency));
    }

    private void bindPortions(MyViewHolder holder, List<ProductPortionResponse> portions) {
        holder.binding.productPortionsContainer.removeAllViews();

        if (portions.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText(context.getString(R.string.ui_product_portions_none));
            empty.setTextColor(context.getColor(R.color.colorTextSecondary));
            empty.setTextSize(12f);
            holder.binding.productPortionsContainer.addView(empty);
            return;
        }

        String currency = MainActivity.currencyName + " ";
        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < portions.size(); i++) {
            ProductPortionResponse portion = portions.get(i);
            PortionListBinding row = PortionListBinding.inflate(
                    inflater, holder.binding.productPortionsContainer, false);

            row.srNo.setText(String.valueOf(i + 1));
            row.portionName.setText(portion.getPortionName());
            String portionPrice = portion.getPortionPrice();
            row.portionPrice.setText(currency + (TextUtils.isEmpty(portionPrice) ? "0" : portionPrice.trim()));

            row.portionEdit.setOnClickListener(v -> updatePortion(portion));
            row.portionRemove.setOnClickListener(v -> deletePortion(portion.getPortionId()));

            holder.binding.productPortionsContainer.addView(row.getRoot());
        }
    }

    private void updatePortion(ProductPortionResponse item) {
        Activity activity = (Activity) context;
        View content = LayoutInflater.from(activity).inflate(R.layout.update_portion_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextInputEditText portionNameTxt = content.findViewById(R.id.portionName);
        TextInputEditText portionPriceTxt = content.findViewById(R.id.portionPrice);
        TextInputEditText portionSortOrderTxt = content.findViewById(R.id.portionSortOrder);
        TextView updatePortionTxt = content.findViewById(R.id.updatePortion);
        TextView dismissPortionTxt = content.findViewById(R.id.dismissPortion);

        portionNameTxt.setText(item.getPortionName());
        portionPriceTxt.setText(item.getPortionPrice());
        portionSortOrderTxt.setText(item.getPortionSortOrder() != null ? item.getPortionSortOrder() : "0");
        portionNameTxt.setEnabled(false);
        portionNameTxt.setFocusable(false);

        dismissPortionTxt.setOnClickListener(v -> sheet.dismiss());

        updatePortionTxt.setOnClickListener(v -> {
            String price = portionPriceTxt.getText().toString().trim();
            if (price.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.toast_please_enter_portion_price),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            int sortOrder = parseSortOrder(portionSortOrderTxt.getText().toString(), item.getPortionSortOrder());
            sheet.dismiss();
            posBillingWalaDatabase.updateProductPortionPriceAndSort(item.getPortionId(), price, sortOrder);
            Toast.makeText(context, context.getString(R.string.toast_portion_updated), Toast.LENGTH_SHORT).show();
            ProductMaster.getProductList();
        });
    }

    private int parseSortOrder(String input, String fallback) {
        try {
            if (input != null && !input.trim().isEmpty()) {
                return Integer.parseInt(input.trim());
            }
        } catch (NumberFormatException ignored) {
        }
        try {
            if (fallback != null && !fallback.trim().isEmpty()) {
                return Integer.parseInt(fallback.trim());
            }
        } catch (NumberFormatException ignored) {
        }
        return 0;
    }

    private void deletePortion(String portionId) {
        BottomSheetUi.showConfirm(
                context,
                context.getString(R.string.toast_are_you_sure),
                context.getString(R.string.toast_do_you_want_to_delete_this_portion),
                "YES",
                "NO",
                true,
                () -> {
                    posBillingWalaDatabase.deleteProductPortion(portionId);
                    Toast.makeText(context, context.getString(R.string.toast_portion_deleted),
                            Toast.LENGTH_SHORT).show();
                    ProductMaster.getProductList();
                });
    }

    private static String formatPortionPriceSummary(List<ProductPortionResponse> portions, String currency) {
        if (portions.size() == 1) {
            String price = portions.get(0).getPortionPrice();
            return currency + (TextUtils.isEmpty(price) ? "0" : price.trim());
        }

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        boolean hasPrice = false;
        for (ProductPortionResponse portion : portions) {
            double value = parsePrice(portion.getPortionPrice());
            if (value <= 0d) {
                continue;
            }
            hasPrice = true;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        if (!hasPrice) {
            return currency + "0";
        }
        if (min == max) {
            return currency + formatPrice(min);
        }
        return currency + formatPrice(min) + " – " + formatPrice(max);
    }

    private static double parsePrice(String price) {
        if (TextUtils.isEmpty(price)) {
            return 0d;
        }
        try {
            return Double.parseDouble(price.trim().replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0d;
        }
    }

    private static String formatPrice(double value) {
        if (Math.rint(value) == value) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private static String getInitial(String name) {
        if (TextUtils.isEmpty(name)) {
            return "P";
        }
        return name.substring(0, 1).toUpperCase(Locale.getDefault());
    }

    public void deleteProductDialog(String productId) {

        BottomSheetUi.showConfirm(
                context,
                context.getString(R.string.toast_are_you_sure),
                context.getString(R.string.toast_do_you_want_to_delete_this_product),
                "YES",
                "NO",
                true,
                () -> {
                    posBillingWalaDatabase.deleteProduct(productId);
                    Toast.makeText(context, context.getString(R.string.toast_product_delete_successfully), Toast.LENGTH_SHORT).show();
                    ProductMaster.getProductList();
                });
    }

    @Override
    public int getItemCount() {
        return productResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        public final ProductListBinding binding;

        public MyViewHolder(ProductListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
