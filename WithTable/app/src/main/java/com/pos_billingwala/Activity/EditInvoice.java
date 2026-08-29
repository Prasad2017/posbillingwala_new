package com.pos_billingwala.Activity;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.NetworkToOffline.InvoicePendingSync;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivityEditInvoiceBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressLint("SetTextI18n")
public class EditInvoice extends BaseActivity {

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
    private float subTotal;
    private float gstAmount;
    private float totalAmount;
    private List<CompanyResponse> companyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditInvoiceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        binding.discountRow.setOnClickListener(v -> showDiscountDialog());
        binding.packingRow.setOnClickListener(v -> showPackingDialog());
        binding.paymentRow.setOnClickListener(v -> showPaymentDialog());
        binding.saveButton.setOnClickListener(v -> saveInvoice());

        recalculate();
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
        binding.paymentLabel.setText(getString(R.string.ui_payment_mode) + ": " + paymentMode);
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

        TextView continueToReport = content.findViewById(R.id.continueToReport);
        TextView dismissReport = content.findViewById(R.id.dismissReport);
        TextView totalView = content.findViewById(R.id.totalAmount);
        RadioGroup paymentGroup = content.findViewById(R.id.paymentGroup);

        String inr = MainActivity.currencyName != null ? MainActivity.currencyName : "";
        totalView.setText(getString(R.string.ui_total_amount) + ": " + inr
                + String.format(Locale.US, "%.2f", totalAmount));

        if (paymentMode.equalsIgnoreCase(getString(R.string.ui_upi)) || paymentMode.equalsIgnoreCase("UPI")
                || paymentMode.equalsIgnoreCase("Online")) {
            paymentGroup.check(R.id.online);
        } else {
            paymentGroup.check(R.id.cash);
        }

        paymentGroup.setOnCheckedChangeListener((group, checkedId) -> {
            View selected = group.findViewById(checkedId);
            if (selected instanceof RadioButton) {
                paymentMode = ((RadioButton) selected).getText().toString();
            }
        });

        dismissReport.setOnClickListener(v -> sheet.dismiss());
        continueToReport.setOnClickListener(v -> {
            int selectedId = paymentGroup.getCheckedRadioButtonId();
            RadioButton selected = paymentGroup.findViewById(selectedId);
            if (selected != null) {
                paymentMode = selected.getText().toString();
            }
            recalculate();
            sheet.dismiss();
        });
    }

    private void saveInvoice() {
        if (lines.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_keep_one_item), Toast.LENGTH_SHORT).show();
            return;
        }
        for (String deletedId : adapter.getDeletedProductIds()) {
            database.deleteInvoiceProduct(deletedId);
        }
        for (InvoiceProductResponse line : lines) {
            database.updateInvoiceProductQuantity(line.getInvoiceProductId(), line.getProductQuantity());
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
                paymentMode);
        Toast.makeText(this, getString(R.string.toast_bill_updated), Toast.LENGTH_SHORT).show();
        InvoicePendingSync.syncPendingInvoiceChanges(this);
        setResult(RESULT_OK, new Intent());
        finish();
    }
}
