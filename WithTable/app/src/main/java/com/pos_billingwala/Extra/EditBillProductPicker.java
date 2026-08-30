package com.pos_billingwala.Extra;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Adapter.ComboProductPickAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.List;

/** Search + portion + qty picker for adding catalogue products onto an existing bill. */
public final class EditBillProductPicker {

    public interface Listener {
        void onProductPicked(ProductResponse product, ProductPortionResponse portion, int quantity);
    }

    private EditBillProductPicker() {
    }

    public static void show(Activity activity, POSBillingWalaDatabase database, Listener listener) {
        if (activity == null || database == null) {
            return;
        }
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_combo_item, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, dialogView, true);

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        TextInputEditText search = dialogView.findViewById(R.id.comboItemProductSearch);
        AutoFitGridRecyclerView productList = dialogView.findViewById(R.id.comboItemProductRecyclerView);
        LinearLayout portionSection = dialogView.findViewById(R.id.comboItemPortionSection);
        RadioGroup portionGroup = dialogView.findViewById(R.id.comboItemPortionRadioGroup);
        TextView selectedProductName = dialogView.findViewById(R.id.comboItemSelectedProduct);
        TextView quantityMinus = dialogView.findViewById(R.id.comboItemQuantityMinus);
        TextView quantityValue = dialogView.findViewById(R.id.comboItemQuantity);
        TextView quantityPlus = dialogView.findViewById(R.id.comboItemQuantityPlus);
        TextView dismiss = dialogView.findViewById(R.id.comboItemDismiss);
        TextView save = dialogView.findViewById(R.id.comboItemSave);
        TextView noProduct = dialogView.findViewById(R.id.comboItemNoProduct);

        title.setText(activity.getString(R.string.ui_add_product));
        save.setText(activity.getString(R.string.ui_add_product));

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
        dismiss.setOnClickListener(v -> sheet.dismiss());
        save.setOnClickListener(v -> {
            ProductResponse product = selectedProduct[0];
            if (product == null) {
                Toast.makeText(activity, activity.getString(R.string.toast_please_select_product), Toast.LENGTH_SHORT).show();
                return;
            }
            boolean hasPortions = database.hasProductPortions(product.getProductId())
                    && portions[0] != null && !portions[0].isEmpty();
            ProductPortionResponse portion = null;
            if (hasPortions) {
                int checkedId = portionGroup.getCheckedRadioButtonId();
                RadioButton selected = dialogView.findViewById(checkedId);
                if (selected == null || !(selected.getTag() instanceof Integer)) {
                    Toast.makeText(activity, activity.getString(R.string.toast_please_select_portion), Toast.LENGTH_SHORT).show();
                    return;
                }
                portion = portions[0].get((Integer) selected.getTag());
            }
            if (listener != null) {
                listener.onProductPicked(product, portion, quantity[0]);
            }
            sheet.dismiss();
        });
    }
}
