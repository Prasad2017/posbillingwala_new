package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.StockTableAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.AppExecutors;
import com.pos_billingwala.Model.InventoryResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentInventoryBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@SuppressLint("StaticFieldLeak")
public class Inventory extends Fragment implements View.OnClickListener {

    private static final String REASON_PURCHASE = "purchase";
    private static final String REASON_DAMAGE = "damage";
    private static final String REASON_ADJUSTMENT = "adjustment";
    private static final String REASON_OTHER = "other";

    public static Activity activity;
    POSBillingWalaDatabase posBillingWalaDatabase;
    FragmentInventoryBinding binding;
    StockTableAdapter adapter;
    final List<StockTableAdapter.StockRow> allRows = new ArrayList<>();
    final List<ProductResponse> products = new ArrayList<>();
    String selectedProductId;
    boolean formVisible;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInventoryBinding.inflate(inflater, container, false);
        activity = getActivity();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        View view = binding.getRoot();
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("tag", "onKey Back listener is working!!!");
                if (formVisible) {
                    hideForm();
                    return true;
                }
                ((MainActivity) activity).navigateBack();
                return true;
            }
            return false;
        });

        adapter = new StockTableAdapter(this::selectRow);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        binding.recyclerView.setAdapter(adapter);

        binding.backToSetting.setOnClickListener(this);
        binding.addStock.setOnClickListener(this);
        binding.stockAdjustment.setOnClickListener(this);
        binding.saveStock.setOnClickListener(this);
        binding.searchItem.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.itemSpinner.setOnItemSelectedListener((position, label) -> {
            if (position >= 0 && position < products.size()) {
                selectedProductId = products.get(position).getProductId();
                showCurrentStock();
                adapter.submit(filteredRows(), selectedProductId);
            }
        });
        return view;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.addStock) {
            showForm(REASON_PURCHASE);
        } else if (id == R.id.stockAdjustment) {
            showForm(REASON_DAMAGE);
        } else if (id == R.id.saveStock) {
            saveStock();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        loadStock();
    }

    private void loadStock() {
        AppExecutors.get().db().execute(() -> {
            List<ProductResponse> productList = posBillingWalaDatabase.getHomeProductList("", "", "");
            List<InventoryResponse> inventory = posBillingWalaDatabase.getInventoryList();
            HashMap<String, Double> stockById = new HashMap<>();
            if (inventory != null) {
                for (InventoryResponse row : inventory) {
                    if (row.getProductId() == null) {
                        continue;
                    }
                    stockById.put(row.getProductId().trim(),
                            POSBillingWalaDatabase.parseStockQty(row.getAfterSaleInventoryQuantity()));
                }
            }
            List<StockTableAdapter.StockRow> rows = new ArrayList<>();
            List<ProductResponse> productCopy = productList != null ? productList : new ArrayList<>();
            String currency = MainActivity.currencyName != null ? MainActivity.currencyName : "\u20B9";
            for (ProductResponse product : productCopy) {
                StockTableAdapter.StockRow row = new StockTableAdapter.StockRow();
                row.productId = product.getProductId();
                row.name = product.getProductName() != null ? product.getProductName() : "";
                row.code = product.getProductCode() != null ? product.getProductCode() : "";
                String key = row.productId != null ? row.productId.trim() : "";
                row.stock = stockById.containsKey(key) ? stockById.get(key) : 0d;
                row.priceLabel = formatPrice(currency, product.getProductPrice());
                row.status = StockTableAdapter.statusFor(row.stock);
                rows.add(row);
                stockById.remove(key);
            }
            rows.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
            AppExecutors.get().main(() -> {
                if (!isAdded() || binding == null) {
                    return;
                }
                products.clear();
                products.addAll(productCopy);
                allRows.clear();
                allRows.addAll(rows);
                bindItemSpinner();
                applyFilter();
            });
        });
    }

    private void bindItemSpinner() {
        String[] labels = new String[products.size()];
        int selected = 0;
        for (int i = 0; i < products.size(); i++) {
            ProductResponse product = products.get(i);
            String name = product.getProductName() != null ? product.getProductName() : "";
            String code = product.getProductCode();
            labels[i] = (code == null || code.trim().isEmpty()) ? name : name + " (" + code.trim() + ")";
            if (selectedProductId != null && selectedProductId.equals(product.getProductId())) {
                selected = i;
            }
        }
        binding.itemSpinner.setItems(labels);
        if (labels.length > 0) {
            binding.itemSpinner.setSelectedIndex(selected);
            selectedProductId = products.get(selected).getProductId();
        }
        showCurrentStock();
    }

    private void applyFilter() {
        List<StockTableAdapter.StockRow> shown = filteredRows();
        adapter.submit(shown, selectedProductId);
        boolean empty = shown.isEmpty();
        binding.emptyStock.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        CharSequence query = binding.searchItem.getText();
        binding.emptyStock.setText(query != null && query.toString().trim().length() > 0
                ? R.string.ui_no_stock_match
                : R.string.ui_no_stock_items);
    }

    private List<StockTableAdapter.StockRow> filteredRows() {
        String query = binding.searchItem.getText() == null
                ? ""
                : binding.searchItem.getText().toString().trim().toLowerCase(Locale.US);
        if (query.isEmpty()) {
            return new ArrayList<>(allRows);
        }
        List<StockTableAdapter.StockRow> shown = new ArrayList<>();
        for (StockTableAdapter.StockRow row : allRows) {
            String name = row.name != null ? row.name.toLowerCase(Locale.US) : "";
            String code = row.code != null ? row.code.toLowerCase(Locale.US) : "";
            if (name.contains(query) || code.contains(query)) {
                shown.add(row);
            }
        }
        return shown;
    }

    private void selectRow(StockTableAdapter.StockRow row) {
        selectedProductId = row.productId;
        for (int i = 0; i < products.size(); i++) {
            if (row.productId != null && row.productId.equals(products.get(i).getProductId())) {
                binding.itemSpinner.setSelectedIndex(i);
                break;
            }
        }
        showCurrentStock();
        if (!formVisible) {
            showForm(REASON_PURCHASE);
        } else {
            adapter.submit(filteredRows(), selectedProductId);
        }
    }

    private void showForm(String reason) {
        formVisible = true;
        binding.stockForm.setVisibility(View.VISIBLE);
        String[] labels;
        String[] values;
        int index;
        if (REASON_PURCHASE.equals(reason)) {
            labels = new String[]{
                    getString(R.string.ui_reason_purchase),
                    getString(R.string.ui_reason_damage),
                    getString(R.string.ui_reason_adjustment),
                    getString(R.string.ui_reason_other)
            };
            values = new String[]{REASON_PURCHASE, REASON_DAMAGE, REASON_ADJUSTMENT, REASON_OTHER};
            index = 0;
        } else {
            labels = new String[]{
                    getString(R.string.ui_reason_damage),
                    getString(R.string.ui_reason_adjustment),
                    getString(R.string.ui_reason_other)
            };
            values = new String[]{REASON_DAMAGE, REASON_ADJUSTMENT, REASON_OTHER};
            index = REASON_ADJUSTMENT.equals(reason) ? 1 : REASON_OTHER.equals(reason) ? 2 : 0;
        }
        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_dropdown_item, labels);
        binding.reasonSpinner.setAdapter(reasonAdapter);
        binding.reasonSpinner.setTag(values);
        binding.reasonSpinner.setSelection(index);
        binding.qtyInput.setText("");
        showCurrentStock();
        adapter.submit(filteredRows(), selectedProductId);
    }

    private void hideForm() {
        formVisible = false;
        binding.stockForm.setVisibility(View.GONE);
    }

    private void showCurrentStock() {
        double stock = 0d;
        for (StockTableAdapter.StockRow row : allRows) {
            if (selectedProductId != null && selectedProductId.equals(row.productId)) {
                stock = row.stock;
                break;
            }
        }
        binding.currentStockValue.setText(getString(R.string.ui_current_stock)
                + " : " + POSBillingWalaDatabase.formatStockQty(stock));
    }

    private void saveStock() {
        if (selectedProductId == null || products.isEmpty()) {
            Toast.makeText(activity, R.string.toast_please_select_product, Toast.LENGTH_SHORT).show();
            return;
        }
        double qty = POSBillingWalaDatabase.parseStockQty(
                binding.qtyInput.getText() == null ? "" : binding.qtyInput.getText().toString());
        if (qty <= 0d) {
            Toast.makeText(activity, R.string.toast_stock_qty_required, Toast.LENGTH_SHORT).show();
            return;
        }
        Object tag = binding.reasonSpinner.getTag();
        String reason = REASON_PURCHASE;
        if (tag instanceof String[]) {
            String[] values = (String[]) tag;
            int index = binding.reasonSpinner.getSelectedItemPosition();
            if (index >= 0 && index < values.length) {
                reason = values[index];
            }
        }
        boolean increase = REASON_PURCHASE.equals(reason);
        double current = 0d;
        String productName = "";
        for (StockTableAdapter.StockRow row : allRows) {
            if (selectedProductId.equals(row.productId)) {
                current = row.stock;
                productName = row.name;
                break;
            }
        }
        if (!increase && qty > current) {
            Toast.makeText(activity, R.string.toast_stock_qty_exceeds, Toast.LENGTH_SHORT).show();
            return;
        }
        double delta = increase ? qty : -qty;
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String name = productName;
        String productId = selectedProductId;
        AppExecutors.get().db().execute(() -> {
            boolean saved = posBillingWalaDatabase.recordStockDelta(productId, name, delta, date, true);
            AppExecutors.get().main(() -> {
                if (!isAdded()) {
                    return;
                }
                if (!saved) {
                    Toast.makeText(activity, R.string.toast_please_select_product, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(activity, R.string.toast_stock_saved, Toast.LENGTH_SHORT).show();
                binding.qtyInput.setText("");
                loadStock();
            });
        });
    }

    private static String formatPrice(String currency, String rawPrice) {
        double value = POSBillingWalaDatabase.parseStockQty(rawPrice);
        if (Math.abs(value - Math.rint(value)) < 0.0005d) {
            return (currency + " " + (long) Math.rint(value)).trim();
        }
        return String.format(Locale.US, "%s %.2f", currency, value).trim();
    }
}
