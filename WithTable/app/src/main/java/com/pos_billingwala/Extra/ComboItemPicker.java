package com.pos_billingwala.Extra;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Adapter.ComboProductPickAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.ComboItemDraft;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Reuses existing product list + portion queries/dialog pattern to pick a combo component.
 */
public final class ComboItemPicker {

    public interface Listener {
        void onItemPicked(ComboItemDraft draft);
    }

    private ComboItemPicker() {
    }

    public static void show(Activity activity, POSBillingWalaDatabase database, Listener listener) {
        if (activity == null || database == null) {
            return;
        }
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_combo_item);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.setCancelable(true);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        if (dialog.getWindow() != null) {
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        }

        TextInputEditText search = dialog.findViewById(R.id.comboItemProductSearch);
        AutoFitGridRecyclerView productList = dialog.findViewById(R.id.comboItemProductRecyclerView);
        LinearLayout portionSection = dialog.findViewById(R.id.comboItemPortionSection);
        RadioGroup portionGroup = dialog.findViewById(R.id.comboItemPortionRadioGroup);
        TextView selectedProductName = dialog.findViewById(R.id.comboItemSelectedProduct);
        TextView quantityMinus = dialog.findViewById(R.id.comboItemQuantityMinus);
        TextView quantityValue = dialog.findViewById(R.id.comboItemQuantity);
        TextView quantityPlus = dialog.findViewById(R.id.comboItemQuantityPlus);
        TextView dismiss = dialog.findViewById(R.id.comboItemDismiss);
        TextView save = dialog.findViewById(R.id.comboItemSave);
        TextView noProduct = dialog.findViewById(R.id.comboItemNoProduct);

        List<ProductResponse> allProducts = database.getAllProductList("", "");
        List<ProductResponse> visible = new ArrayList<>(allProducts);
        final ProductResponse[] selectedProduct = {null};
        final List<ProductPortionResponse>[] portions = new List[]{new ArrayList<>()};
        final int[] quantity = {1};

        ComboProductPickAdapter adapter = new ComboProductPickAdapter(visible, product -> {
            selectedProduct[0] = product;
            selectedProductName.setText(product.getProductName());
            selectedProductName.setVisibility(View.VISIBLE);
            portions[0] = database.getProductPortionList(product.getProductId());
            portionGroup.removeAllViews();
            if (database.hasProductPortions(product.getProductId()) && portions[0] != null && !portions[0].isEmpty()) {
                portionSection.setVisibility(View.VISIBLE);
                for (int i = 0; i < portions[0].size(); i++) {
                    ProductPortionResponse portion = portions[0].get(i);
                    RadioButton radioButton = new RadioButton(activity);
                    radioButton.setId(View.generateViewId());
                    radioButton.setTag(i);
                    radioButton.setText(portion.getPortionName());
                    radioButton.setTextColor(activity.getResources().getColor(R.color.black));
                    radioButton.setPadding(24, 16, 24, 16);
                    portionGroup.addView(radioButton);
                    if (i == 0) {
                        radioButton.setChecked(true);
                    }
                }
            } else {
                portionSection.setVisibility(View.GONE);
                portions[0] = new ArrayList<>();
            }
            productList.setVisibility(View.GONE);
            search.setVisibility(View.GONE);
        });
        productList.setAdapter(adapter);
        noProduct.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String q = s != null ? s.toString().trim().toLowerCase() : "";
                visible.clear();
                for (ProductResponse product : allProducts) {
                    String name = product.getProductName() != null ? product.getProductName() : "";
                    String code = product.getProductCode() != null ? product.getProductCode() : "";
                    if (q.isEmpty() || name.toLowerCase().contains(q) || code.toLowerCase().contains(q)) {
                        visible.add(product);
                    }
                }
                adapter.notifyDataSetChanged();
                noProduct.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        quantityValue.setText(String.valueOf(quantity[0]));
        quantityMinus.setOnClickListener(v -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                quantityValue.setText(String.valueOf(quantity[0]));
            }
        });
        quantityPlus.setOnClickListener(v -> {
            quantity[0]++;
            quantityValue.setText(String.valueOf(quantity[0]));
        });
        dismiss.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            ProductResponse product = selectedProduct[0];
            if (product == null) {
                Toast.makeText(activity, activity.getString(R.string.ui_combo_select_product), Toast.LENGTH_SHORT).show();
                return;
            }
            boolean hasPortions = database.hasProductPortions(product.getProductId())
                    && portions[0] != null && !portions[0].isEmpty();
            String portionId = null;
            String portionName = null;
            if (hasPortions) {
                int checkedId = portionGroup.getCheckedRadioButtonId();
                RadioButton selected = dialog.findViewById(checkedId);
                if (selected == null || !(selected.getTag() instanceof Integer)) {
                    Toast.makeText(activity, activity.getString(R.string.toast_please_select_portion), Toast.LENGTH_SHORT).show();
                    return;
                }
                int index = (Integer) selected.getTag();
                ProductPortionResponse portion = portions[0].get(index);
                if (!database.portionBelongsToProduct(product.getProductId(), portion.getPortionId())) {
                    Toast.makeText(activity, activity.getString(R.string.ui_combo_portion_mismatch), Toast.LENGTH_SHORT).show();
                    return;
                }
                portionId = portion.getPortionId();
                portionName = portion.getPortionName();
            }
            String qty = String.valueOf(quantity[0]);
            String error = ComboValidator.validateComboItem(
                    product.getProductId(), true, database.isProductActive(product.getProductId()),
                    hasPortions, portionId, !hasPortions || portionId != null, qty);
            if (error != null) {
                Toast.makeText(activity, mapError(activity, error), Toast.LENGTH_SHORT).show();
                return;
            }
            ComboItemDraft draft = new ComboItemDraft();
            draft.setProductId(product.getProductId());
            draft.setProductName(product.getProductName());
            draft.setPortionId(portionId);
            draft.setPortionName(portionName);
            draft.setQuantity(qty);
            if (listener != null) {
                listener.onItemPicked(draft);
            }
            dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setAttributes(lp);
        }
    }

    public static String mapError(Activity activity, String code) {
        if (ComboValidator.ERR_NAME.equals(code)) {
            return activity.getString(R.string.ui_combo_name_required);
        }
        if (ComboValidator.ERR_PRICE.equals(code)) {
            return activity.getString(R.string.ui_combo_price_invalid);
        }
        if (ComboValidator.ERR_ITEMS.equals(code)) {
            return activity.getString(R.string.ui_combo_items_required);
        }
        if (ComboValidator.ERR_PRODUCT.equals(code) || ComboValidator.ERR_PRODUCT_INACTIVE.equals(code)) {
            return activity.getString(R.string.ui_combo_select_product);
        }
        if (ComboValidator.ERR_PORTION_REQUIRED.equals(code)) {
            return activity.getString(R.string.toast_please_select_portion);
        }
        if (ComboValidator.ERR_PORTION_MISMATCH.equals(code)) {
            return activity.getString(R.string.ui_combo_portion_mismatch);
        }
        if (ComboValidator.ERR_QUANTITY.equals(code)) {
            return activity.getString(R.string.ui_combo_quantity_invalid);
        }
        return code;
    }
}
