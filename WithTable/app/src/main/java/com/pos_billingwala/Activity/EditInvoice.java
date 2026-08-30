package com.pos_billingwala.Activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.pos_billingwala.Adapter.EditInvoiceProductAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.CartItemType;
import com.pos_billingwala.Extra.EditBillProductPicker;
import com.pos_billingwala.Extra.PaymentSettlementBinder;
import com.pos_billingwala.Extra.PaymentSettlementHelper;
import com.pos_billingwala.Extra.TabletFormUi;
import com.pos_billingwala.Extra.TabletUi;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.NetworkToOffline.InvoicePendingSync;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivityEditInvoiceBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressLint("SetTextI18n")
public class EditInvoice extends BaseActivity {

    public static final String EXTRA_PRINT_AFTER_SAVE = "printAfterSave";

    private ActivityEditInvoiceBinding binding;
    private POSBillingWalaDatabase database;
    private EditInvoiceProductAdapter adapter;
    private final List<InvoiceProductResponse> lines = new ArrayList<>();
    private InvoiceResponse invoice;
    private String discountRaw = "0";
    private String discountType = "Percentage";
    private String packingRaw = "0";
    private String packingChargeType = "Percentage";
    private String paymentMode = "Cash";
    private String cashAmount = "";
    private String upiAmount = "";
    private float subTotal;
    private float gstAmount;
    private float totalAmount;
    private List<CompanyResponse> companyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditInvoiceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (TabletUi.isTablet(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        database = new POSBillingWalaDatabase(this);
        companyList = database.getCompanyDetails();

        String invoiceId = getIntent() != null ? getIntent().getStringExtra("invoiceId") : null;
        List<InvoiceResponse> details = invoiceId != null
                ? database.getInvoiceDetails(invoiceId)
                : new ArrayList<>();
        if (details.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_bill_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        invoice = details.get(0);
        if (invoice.isRefunded()) {
            Toast.makeText(this, getString(R.string.toast_cannot_edit_refunded), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        discountRaw = invoice.getDiscount() != null ? invoice.getDiscount() : "0";
        discountType = invoice.getDiscountType() != null && !invoice.getDiscountType().isEmpty()
                ? invoice.getDiscountType() : "Percentage";
        packingRaw = invoice.getPackingCharge() != null ? invoice.getPackingCharge() : "0";
        packingChargeType = invoice.getPackingChargeType() != null && !invoice.getPackingChargeType().isEmpty()
                ? invoice.getPackingChargeType() : "Percentage";
        paymentMode = invoice.getPaymentMode() != null && !invoice.getPaymentMode().isEmpty()
                ? invoice.getPaymentMode() : "Cash";
        cashAmount = invoice.getCashAmount();
        upiAmount = invoice.getUpiAmount();

        lines.clear();
        lines.addAll(database.getInvoiceProductList(invoice.getInvoiceNumber()));
        if (lines.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_keep_one_item), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new EditInvoiceProductAdapter(this, lines, this::recalculate);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        binding.backButton.setOnClickListener(v -> finish());
        binding.addProductButton.setOnClickListener(v -> showAddProductPicker());
        binding.discountRow.setOnClickListener(v -> showDiscountDialog());
        binding.packingRow.setOnClickListener(v -> showPackingDialog());
        binding.paymentRow.setOnClickListener(v -> showPaymentDialog());
        binding.saveButton.setOnClickListener(v -> saveInvoice(false));
        binding.printButton.setOnClickListener(v -> saveInvoice(true));

        TabletFormUi.applyEditBillSplit(this, binding.recyclerView, binding.bottomPanel, 360);

        recalculate();
    }

    private void showAddProductPicker() {
        EditBillProductPicker.show(this, database, this::onProductPicked);
    }

    private void onProductPicked(ProductResponse product, ProductPortionResponse portion, int quantity) {
        if (product == null || quantity <= 0) {
            return;
        }
        if (product.isOpenPrice()) {
            showOpenPriceDialog(product, portion, quantity);
        } else {
            addProductLine(product, portion, quantity, resolveLinePrice(product, portion));
        }
    }

    private void showOpenPriceDialog(ProductResponse product, ProductPortionResponse portion, int defaultQty) {
        View content = LayoutInflater.from(this).inflate(R.layout.update_amount_quantity_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(this, content, false);

        TextView continueToQuantity = content.findViewById(R.id.continueToQuantity);
        TextView dismissQuantity = content.findViewById(R.id.dismissQuantity);
        TextInputEditText amountTxt = content.findViewById(R.id.amount);
        TextInputEditText quantityTxt = content.findViewById(R.id.quantity);
        TextView detailsTxt = content.findViewById(R.id.details);
        detailsTxt.setText(getString(R.string.ui_open_price));
        amountTxt.setText(resolveLinePrice(product, portion));
        quantityTxt.setText(String.valueOf(defaultQty));
        amountTxt.requestFocus();

        dismissQuantity.setOnClickListener(v -> sheet.dismiss());
        continueToQuantity.setOnClickListener(v -> {
            String amountStr = amountTxt.getText() != null ? amountTxt.getText().toString().trim() : "";
            String qtyStr = quantityTxt.getText() != null ? quantityTxt.getText().toString().trim() : "";
            if (amountStr.isEmpty()) {
                amountTxt.setError(getString(R.string.ui_enter_amount));
                return;
            }
            float amount = ReportCursorHelper.parseAmount(amountStr);
            if (amount <= 0f) {
                amountTxt.setError(getString(R.string.ui_enter_amount));
                return;
            }
            if (qtyStr.isEmpty()) {
                quantityTxt.setError(getString(R.string.ui_enter_quantity));
                return;
            }
            float qty = ReportCursorHelper.parseAmount(qtyStr);
            if (qty <= 0f) {
                quantityTxt.setError(getString(R.string.ui_enter_quantity));
                return;
            }
            addProductLine(product, portion, qty, String.format(Locale.US, "%.2f", amount));
            sheet.dismiss();
        });
    }

    private void addProductLine(ProductResponse product, ProductPortionResponse portion, float quantity, String linePrice) {
        int mergeIndex = findMergeIndex(product, portion, linePrice);
        if (mergeIndex >= 0) {
            InvoiceProductResponse existing = lines.get(mergeIndex);
            float newQty = ReportCursorHelper.parseAmount(existing.getProductQuantity()) + quantity;
            existing.setProductQuantity(formatQty(newQty));
            adapter.notifyItemChanged(mergeIndex);
        } else {
            InvoiceProductResponse line = new InvoiceProductResponse();
            line.setInvoiceNumber(invoice.getInvoiceNumber());
            line.setSnapshotProductName(product.getProductName());
            line.setProductName(product.getProductName());
            line.setSnapshotLinePrice(linePrice);
            line.setProductPrice(linePrice);
            line.setProductUnit(product.getProductUnit() != null ? product.getProductUnit() : "");
            line.setProductCGST(product.getProductCGST());
            line.setProductSGST(product.getProductSGST());
            line.setProductQuantity(formatQty(quantity));
            line.setProductStatus("completed");
            line.setInvoiceItemType(CartItemType.PRODUCT);
            line.setSourceProductId(product.getProductId());
            if (portion != null) {
                line.setPortionId(portion.getPortionId());
                line.setPortionName(portion.getPortionName());
            }
            lines.add(line);
            adapter.notifyItemInserted(lines.size() - 1);
        }
        recalculate();
    }

    private int findMergeIndex(ProductResponse product, ProductPortionResponse portion, String linePrice) {
        String productName = product.getProductName() != null ? product.getProductName().trim() : "";
        String portionId = portion != null ? portion.getPortionId() : null;
        float targetPrice = ReportCursorHelper.parseAmount(linePrice);
        for (int i = 0; i < lines.size(); i++) {
            InvoiceProductResponse line = lines.get(i);
            if (CartItemType.isCombo(line.getInvoiceItemType())) {
                continue;
            }
            String base = line.getSnapshotProductName();
            if (base == null || base.trim().isEmpty()) {
                base = line.getProductName();
            }
            if (base == null || !productName.equalsIgnoreCase(base.trim())) {
                continue;
            }
            if (!sameOptionalId(portionId, line.getPortionId())) {
                continue;
            }
            if (Math.abs(ReportCursorHelper.parseAmount(line.getResolvedLinePrice()) - targetPrice) > 0.001f) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private static boolean sameOptionalId(String left, String right) {
        String a = left != null ? left.trim() : "";
        String b = right != null ? right.trim() : "";
        return a.equals(b);
    }

    private static String resolveLinePrice(ProductResponse product, ProductPortionResponse portion) {
        if (portion != null && portion.getPortionPrice() != null && !portion.getPortionPrice().trim().isEmpty()) {
            return portion.getPortionPrice();
        }
        return product.getProductPrice() != null ? product.getProductPrice() : "0";
    }

    private static String formatQty(float qty) {
        if (qty == (long) qty) {
            return String.valueOf((long) qty);
        }
        return String.format(Locale.US, "%.2f", qty);
    }

    private void recalculate() {
        subTotal = 0f;
        for (InvoiceProductResponse line : lines) {
            float price = ReportCursorHelper.parseAmount(line.getResolvedLinePrice());
            float qty = ReportCursorHelper.parseAmount(line.getProductQuantity());
            subTotal += price * qty;
        }
        gstAmount = 0f;
        if (!companyList.isEmpty() && companyList.get(0).getGstStatus() != null
                && companyList.get(0).getGstStatus().equalsIgnoreCase("On")) {
            float cgstPct = ReportCursorHelper.parseAmount(companyList.get(0).getShopCGST());
            float sgstPct = ReportCursorHelper.parseAmount(companyList.get(0).getShopSGST());
            gstAmount = subTotal * (cgstPct + sgstPct) / 100f;
        }
        float discRupees = ReportCursorHelper.discountRupees(discountRaw, discountType, String.valueOf(subTotal));
        float packingRupees = ReportCursorHelper.packingRupees(packingRaw, packingChargeType, String.valueOf(subTotal));
        totalAmount = subTotal - discRupees + packingRupees + gstAmount;
        if (totalAmount < 0f) {
            totalAmount = 0f;
        }

        String inr = MainActivity.currencyName != null ? MainActivity.currencyName : "";
        if (discountType != null && discountType.equalsIgnoreCase("Amount")) {
            binding.discountLabel.setText(getString(R.string.ui_discount) + ": " + inr + " "
                    + String.format(Locale.US, "%.2f", ReportCursorHelper.parseAmount(discountRaw)));
        } else {
            binding.discountLabel.setText(getString(R.string.ui_discount) + ": "
                    + String.format(Locale.US, "%.2f", ReportCursorHelper.parseAmount(discountRaw)) + "%");
        }
        if (packingChargeType != null && packingChargeType.equalsIgnoreCase("Amount")) {
            binding.packingLabel.setText(getString(R.string.ui_packing) + ": " + inr + " "
                    + String.format(Locale.US, "%.2f", ReportCursorHelper.parseAmount(packingRaw)));
        } else {
            binding.packingLabel.setText(getString(R.string.ui_packing) + ": "
                    + String.format(Locale.US, "%.2f", ReportCursorHelper.parseAmount(packingRaw)) + "%");
        }
        binding.paymentLabel.setText(getString(R.string.ui_payment_mode) + ": "
                + PaymentSettlementHelper.displayLabel(paymentMode, cashAmount, upiAmount,
                String.format(Locale.US, "%.2f", totalAmount)));
        binding.subTotalLabel.setText(getString(R.string.ui_sub_total) + ": " + inr + " "
                + String.format(Locale.US, "%.2f", subTotal));
        binding.gstLabel.setText("GST: " + inr + " " + String.format(Locale.US, "%.2f", gstAmount));
        binding.totalLabel.setText(getString(R.string.ui_total_amount) + ": " + inr + " "
                + String.format(Locale.US, "%.2f", totalAmount));
    }

    private void showDiscountDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.update_discount_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(this, content, true);

        TextInputEditText discountInput = content.findViewById(R.id.discountPercentage);
        TextView addDiscount = content.findViewById(R.id.addDiscountPercentage);
        TextView dismissDiscount = content.findViewById(R.id.dismissDiscountPercentage);
        MaterialSpinner discountTypeSpinner = content.findViewById(R.id.discountTypeSpinner);

        String[] discountTypeList = getResources().getStringArray(R.array.discount_type);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, discountTypeList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        discountTypeSpinner.setAdapter(spinnerAdapter);
        int discountIndex = spinnerAdapter.getPosition(discountType);
        if (discountIndex >= 0) {
            discountTypeSpinner.setSelectedIndex(discountIndex);
        }
        discountTypeSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                discountType = discountTypeList[position];
            }
        });
        discountInput.setText(discountRaw);

        dismissDiscount.setOnClickListener(v -> sheet.dismiss());
        addDiscount.setOnClickListener(v -> {
            String value = discountInput.getText() != null ? discountInput.getText().toString().trim() : "";
            if (value.isEmpty()) {
                value = "0";
            }
            discountRaw = value;
            recalculate();
            sheet.dismiss();
        });
    }

    private void showPackingDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.update_packing_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(this, content, true);

        TextInputEditText packingInput = content.findViewById(R.id.packingCharge);
        TextView addPacking = content.findViewById(R.id.addPackingCharge);
        TextView dismissPacking = content.findViewById(R.id.dismissPackingCharge);
        MaterialSpinner packingTypeSpinner = content.findViewById(R.id.packingTypeSpinner);

        String[] packingTypeList = getResources().getStringArray(R.array.discount_type);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, packingTypeList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        packingTypeSpinner.setAdapter(spinnerAdapter);
        int packingIndex = spinnerAdapter.getPosition(packingChargeType);
        if (packingIndex >= 0) {
            packingTypeSpinner.setSelectedIndex(packingIndex);
        }
        packingTypeSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                packingChargeType = packingTypeList[position];
            }
        });
        packingInput.setText(packingRaw);

        dismissPacking.setOnClickListener(v -> sheet.dismiss());
        addPacking.setOnClickListener(v -> {
            String value = packingInput.getText() != null ? packingInput.getText().toString().trim() : "";
            if (value.isEmpty()) {
                value = "0";
            }
            packingRaw = value;
            recalculate();
            sheet.dismiss();
        });
    }

    private void showPaymentDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.set_payment_mode_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(this, content, true);

        PaymentSettlementBinder.bind(content, totalAmount, inrSymbol(), paymentMode,
                new PaymentSettlementBinder.Callback() {
                    @Override
                    public void onConfirmed(String mode, String cash, String upi) {
                        if (mode == null || mode.isEmpty()) {
                            Toast.makeText(EditInvoice.this,
                                    getString(R.string.toast_please_select_payment_mode), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        paymentMode = mode;
                        cashAmount = cash;
                        upiAmount = upi;
                        recalculate();
                        sheet.dismiss();
                    }

                    @Override
                    public void onDismissed() {
                        sheet.dismiss();
                    }
                });
    }

    private String inrSymbol() {
        return MainActivity.currencyName != null ? MainActivity.currencyName : "";
    }

    private void saveInvoice(boolean printAfterSave) {
        if (lines.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_keep_one_item), Toast.LENGTH_SHORT).show();
            return;
        }
        for (String deletedId : adapter.getDeletedProductIds()) {
            database.deleteInvoiceProduct(deletedId);
        }
        for (InvoiceProductResponse line : lines) {
            String lineId = line.getInvoiceProductId();
            if (lineId != null && !lineId.trim().isEmpty()) {
                database.updateInvoiceProductQuantity(lineId, line.getProductQuantity());
            } else {
                database.insertLocalInvoiceProductLine(line, line.getSourceProductId());
            }
        }
        database.updateInvoiceHeader(
                invoice.getInvoiceNumber(),
                String.format(Locale.US, "%.2f", subTotal),
                String.format(Locale.US, "%.2f", gstAmount),
                discountRaw,
                discountType,
                packingRaw,
                packingChargeType,
                String.format(Locale.US, "%.2f", totalAmount),
                paymentMode,
                cashAmount,
                upiAmount);
        Toast.makeText(this, getString(R.string.toast_bill_updated), Toast.LENGTH_SHORT).show();
        InvoicePendingSync.syncPendingInvoiceChanges(this);
        Intent result = new Intent();
        result.putExtra(EXTRA_PRINT_AFTER_SAVE, printAfterSave);
        setResult(RESULT_OK, result);
        finish();
    }
}
