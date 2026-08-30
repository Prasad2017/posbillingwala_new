package com.pos_billingwala.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ActionButtonUi;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Print.BluetoothPrinterChannel;
import com.pos_billingwala.Print.DeviceListActivity;
import com.pos_billingwala.Print.KOTWoosimPrnMng;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.Extra.TabletFormUi;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivityCompanyPrinterSettingBinding;

import java.util.ArrayList;
import java.util.List;


@SuppressLint("NonConstantResourceId, StaticFieldLeak, SetTextI18n")
public class CompanyPrinterSetting extends BaseActivity implements View.OnClickListener {

    public static Activity activity;
    View view;
    String[] printerList;
    String printerName = "2-Inch", KOTPrinterName = "2-Inch", settingId, logoUse = "off", paymentUse = "off", customerUse = "off", productQuantityUpdate = "off", duplicateBillUse = "off";
    /** Paper size last used when a bill/KOT printer was successfully picked or loaded. */
    String lastConnectedPrinterName = "2-Inch", lastConnectedKOTPrinterName = "2-Inch";
    boolean loadingPrinterSpinners;
    boolean billSizeChangedByUser;
    boolean kotSizeChangedByUser;
    boolean printerSettingsLoaded;
    boolean suppressSwitchListener;
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    List<CompanyResponse> companyResponseList = new ArrayList<>();
    //********************* Bluetooth Printer Start ************************//
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS;
    String bluetoothAddress, bluetoothKOTAddress;
    int REQUEST_ENABLE_BT = 4, REQUEST_CONNECT_DEVICE = 6;
    int REQUEST_KOT_ENABLE_BT = 8, REQUEST_KOT_CONNECT_DEVICE = 10;
    //******************** Bluetooth Printer End ************************//
    ActivityCompanyPrinterSettingBinding binding;


    public static boolean hasPermissions(Context context, String... permissions) {
        // Get current android os version.
        int currentAndroidVersion = Build.VERSION.SDK_INT;
        // Build.VERSION_CODES.M's value is 23.
        if (currentAndroidVersion >= Build.VERSION_CODES.M) {
            if (context != null && permissions != null) {
                for (String permission : permissions) {
                    if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCompanyPrinterSettingBinding.inflate(getLayoutInflater());
        View view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here
        setContentView(view); //view is set by view binding

        activity = CompanyPrinterSetting.this;

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        binding.invoiceTitle.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        binding.invoiceTermsCondition.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        binding.invoiceTitle.setSelection(binding.invoiceTitle.getText().toString().length());
        binding.invoiceTermsCondition.setSelection(binding.invoiceTermsCondition.getText().toString().length());
        binding.printerFeedLines.setSelection(binding.printerFeedLines.getText().toString().length());
        binding.KotPrinterFeedLines.setSelection(binding.KotPrinterFeedLines.getText().toString().length());

        binding.printerSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                if (loadingPrinterSpinners || printerList == null || position < 0 || position >= printerList.length) {
                    return;
                }
                printerName = printerList[position];
                billSizeChangedByUser = lastConnectedPrinterName == null
                        || !lastConnectedPrinterName.equalsIgnoreCase(printerName);
            }
        });
        binding.KOTPrinterSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                if (loadingPrinterSpinners || printerList == null || position < 0 || position >= printerList.length) {
                    return;
                }
                KOTPrinterName = printerList[position];
                kotSizeChangedByUser = lastConnectedKOTPrinterName == null
                        || !lastConnectedKOTPrinterName.equalsIgnoreCase(KOTPrinterName);
            }
        });

        binding.logoSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (!suppressSwitchListener) {
                logoUse = isChecked ? "on" : "off";
            }
        });

        binding.paymentSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (!suppressSwitchListener) {
                paymentUse = isChecked ? "on" : "off";
            }
        });

        binding.customerSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (!suppressSwitchListener) {
                customerUse = isChecked ? "on" : "off";
            }
        });

        binding.productQuantityUpdate.setOnCheckedChangeListener((button, isChecked) -> {
            if (!suppressSwitchListener) {
                productQuantityUpdate = isChecked ? "on" : "off";
            }
        });

        binding.duplicateBillSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (!suppressSwitchListener) {
                duplicateBillUse = isChecked ? "on" : "off";
            }
        });

        PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (!hasPermissions(activity, PERMISSIONS)) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSION_ALL);
        }

        binding.connectPrinter.setOnClickListener(this);
        binding.connectKOTPrinter.setOnClickListener(this);
        binding.invoicePreview.setOnClickListener(this);
        binding.backToSetting.setOnClickListener(this);
        binding.saveSetting.getRoot().setOnClickListener(this);
        ActionButtonUi.bind(binding.saveSetting.getRoot(), R.drawable.ic_save, R.string.ui_save_setting);

        applyTabletPrinterForm();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void applyTabletPrinterForm() {
        android.widget.LinearLayout container = binding.printerFormContainer;
        if (container.getChildCount() < 4) {
            return;
        }
        View[] left = {container.getChildAt(0), container.getChildAt(1)};
        View[] right = {container.getChildAt(2), container.getChildAt(3)};
        TabletFormUi.applyTwoColumnCards(this, container, left, right);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            finish();
        } else if (id == R.id.connectPrinter) {
            WoosimPrnMng.connectFromButton(activity, bluetoothAddress, CompanyPrinterSetting.this, billSizeChangedByUser);
        } else if (id == R.id.connectKOTPrinter) {
            KOTWoosimPrnMng.connectFromButton(activity, bluetoothKOTAddress, CompanyPrinterSetting.this, kotSizeChangedByUser);
        } else if (id == R.id.invoicePreview) {
            startActivity(new Intent(activity, TestInvoiceBluetoothPrint.class));
        } else if (id == R.id.saveSetting) {
            if (printerName != null) {
                if (!binding.invoicePrefix.getText().toString().isEmpty()) {
                    addCompanyPrinterSetting();
                } else {
                    Toast.makeText(activity, getString(R.string.toast_please_add_invoice_prefix), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, getString(R.string.toast_please_select_printer), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void addCompanyPrinterSetting() {

        if (ActionButtonUi.getLabel(binding.saveSetting.getRoot()).toString().equalsIgnoreCase(getString(R.string.ui_save_setting))) {
            posBillingWalaDatabase.addCompanyPrinterSetting(printerName, KOTPrinterName, binding.invoicePrefix.getText().toString(), binding.invoiceTitle.getText().toString(), logoUse, paymentUse, customerUse, productQuantityUpdate, duplicateBillUse, binding.invoiceTermsCondition.getText().toString(), bluetoothAddress, bluetoothKOTAddress, binding.printerFeedLines.getText().toString().isEmpty() ? "1" : binding.printerFeedLines.getText().toString(), binding.KotPrinterFeedLines.getText().toString().isEmpty() ? "1" : binding.KotPrinterFeedLines.getText().toString(), 0);
            Toast.makeText(activity, getString(R.string.toast_company_setting_saved), Toast.LENGTH_SHORT).show();
        } else {
            posBillingWalaDatabase.updateCompanyPrinterSetting(settingId, printerName, KOTPrinterName, binding.invoicePrefix.getText().toString(), binding.invoiceTitle.getText().toString(), logoUse, paymentUse, customerUse, productQuantityUpdate, duplicateBillUse, binding.invoiceTermsCondition.getText().toString(), bluetoothAddress, bluetoothKOTAddress, binding.printerFeedLines.getText().toString().isEmpty() ? "1" : binding.printerFeedLines.getText().toString(), binding.KotPrinterFeedLines.getText().toString().isEmpty() ? "1" : binding.KotPrinterFeedLines.getText().toString(), 0);
            Toast.makeText(activity, getString(R.string.toast_company_setting_updated), Toast.LENGTH_SHORT).show();
        }

        getPrinterSettingDetails();

    }

    @Override
    public void onStart() {
        super.onStart();
        getCompanyDetails();
        // Load once — reloading on every onStart (device list / BT dialog) would
        // reset the other printer's size-change flag and overwrite unsaved MACs.
        if (!printerSettingsLoaded) {
            printerSettingsLoaded = true;
            getPrinterSettingDetails();
        }
    }

    public void getCompanyDetails() {
        companyResponseList = posBillingWalaDatabase.getCompanyDetails();

        if (!companyResponseList.isEmpty()) {
            CompanyResponse companyResponse = companyResponseList.get(0);

            String tableStatus = companyResponse.getTableStatus() != null ? companyResponse.getTableStatus() : "";
            String noTable = companyResponse.getNoOfTable();
            if (tableStatus.equalsIgnoreCase("off")) {
                binding.KOTPrinterLayout.setVisibility(View.GONE);
            } else {
                binding.KOTPrinterLayout.setVisibility(View.VISIBLE);
            }

        }

    }


    public void getPrinterSettingDetails() {
        printerSettingResponseList.clear();
        printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();
        if (!printerSettingResponseList.isEmpty()) {
            PrinterSettingResponse printerSettingResponse = printerSettingResponseList.get(0);

            settingId = printerSettingResponse.getSettingId();
            printerName = printerSettingResponse.getPrinterName();
            KOTPrinterName = printerSettingResponse.getKOTPrinterName();
            lastConnectedPrinterName = printerName;
            lastConnectedKOTPrinterName = KOTPrinterName;
            billSizeChangedByUser = false;
            kotSizeChangedByUser = false;
            logoUse = printerSettingResponse.getLogoUse() != null ? printerSettingResponse.getLogoUse() : "off";
            paymentUse = printerSettingResponse.getPaymentUse() != null ? printerSettingResponse.getPaymentUse() : "off";
            customerUse = printerSettingResponse.getCustomerUse() != null ? printerSettingResponse.getCustomerUse() : "off";
            productQuantityUpdate = printerSettingResponse.getProductQuantityUpdate() != null ? printerSettingResponse.getProductQuantityUpdate() : "off";
            duplicateBillUse = printerSettingResponse.getDuplicateBillUse() != null ? printerSettingResponse.getDuplicateBillUse() : "off";
            bluetoothAddress = printerSettingResponse.getBluetoothAddress() != null ? printerSettingResponse.getBluetoothAddress() : "";
            bluetoothKOTAddress = printerSettingResponse.getBluetoothKOTAddress() != null ? printerSettingResponse.getBluetoothKOTAddress() : "";
            binding.invoicePrefix.setText(printerSettingResponse.getInvoicePrefix().isEmpty() ? "POS" : printerSettingResponse.getInvoicePrefix());
            binding.printerFeedLines.setText(printerSettingResponse.getPrinterFeedLines().isEmpty() ? "1" : printerSettingResponse.getPrinterFeedLines());
            binding.KotPrinterFeedLines.setText(printerSettingResponse.getKotPrinterFeedLines().isEmpty() ? "1" : printerSettingResponse.getKotPrinterFeedLines());
            binding.invoiceTitle.setText(printerSettingResponse.getInvoiceTitle());
            binding.invoiceTermsCondition.setText(printerSettingResponse.getInvoiceTermsCondition());

            ActionButtonUi.bind(binding.saveSetting.getRoot(), R.drawable.ic_save, R.string.ui_update_settings);
        } else {
            binding.invoicePrefix.setText("POS");
            binding.printerFeedLines.setText("1");
            binding.KotPrinterFeedLines.setText("1");
            ActionButtonUi.bind(binding.saveSetting.getRoot(), R.drawable.ic_save, R.string.ui_save_setting);
        }

        setSwitchCheckedSilently(binding.logoSwitch, logoUse.equalsIgnoreCase("on"));
        setSwitchCheckedSilently(binding.paymentSwitch, paymentUse.equalsIgnoreCase("on"));
        setSwitchCheckedSilently(binding.customerSwitch, customerUse.equalsIgnoreCase("on"));
        setSwitchCheckedSilently(binding.productQuantityUpdate, productQuantityUpdate.equalsIgnoreCase("on"));
        setSwitchCheckedSilently(binding.duplicateBillSwitch, duplicateBillUse.equalsIgnoreCase("on"));

        printerList = activity.getResources().getStringArray(R.array.printer_list);
        loadingPrinterSpinners = true;
        try {
            ArrayAdapter<String> invoiceAdapter = createPrinterSpinnerAdapter();
            ArrayAdapter<String> kotAdapter = createPrinterSpinnerAdapter();
            binding.printerSpinner.setAdapter(invoiceAdapter);
            binding.KOTPrinterSpinner.setAdapter(kotAdapter);
            if (printerName != null) {
                int printerIndex = invoiceAdapter.getPosition(printerName);
                if (printerIndex >= 0) {
                    binding.printerSpinner.setSelectedIndex(printerIndex);
                }
            }
            if (KOTPrinterName != null) {
                int kotIndex = kotAdapter.getPosition(KOTPrinterName);
                if (kotIndex >= 0) {
                    binding.KOTPrinterSpinner.setSelectedIndex(kotIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            loadingPrinterSpinners = false;
        }

    }

    private void setSwitchCheckedSilently(SwitchCompat switchView, boolean checked) {
        suppressSwitchListener = true;
        switchView.setChecked(checked);
        suppressSwitchListener = false;
    }

    private ArrayAdapter<String> createPrinterSpinnerAdapter() {
        return new ArrayAdapter<String>(activity, R.layout.item_printer_spinner, printerList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                styleSpinnerSelectedView(row, getItem(position));
                return row;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View row = getLayoutInflater().inflate(R.layout.item_printer_spinner_dropdown, parent, false);
                TextView label = row.findViewById(android.R.id.text1);
                if (label != null) {
                    label.setText(getItem(position));
                }
                return row;
            }
        };
    }

    private void styleSpinnerSelectedView(View row, String value) {
        if (row == null) {
            return;
        }
        TextView label = row.findViewById(android.R.id.text1);
        if (label != null) {
            label.setText(value);
            label.setTextColor(ContextCompat.getColor(activity, R.color.colorTextPrimary));
            label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            WoosimPrnMng.connectFromButton(activity, bluetoothAddress, CompanyPrinterSetting.this, billSizeChangedByUser);
        } else if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == RESULT_OK) {
            bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            lastConnectedPrinterName = printerName;
            billSizeChangedByUser = false;
            BluetoothPrinterChannel.bill().onDevicePicked(bluetoothAddress);
        } else if (requestCode == REQUEST_KOT_ENABLE_BT && resultCode == RESULT_OK) {
            KOTWoosimPrnMng.connectFromButton(activity, bluetoothKOTAddress, CompanyPrinterSetting.this, kotSizeChangedByUser);
        } else if (requestCode == REQUEST_KOT_CONNECT_DEVICE && resultCode == RESULT_OK) {
            bluetoothKOTAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            lastConnectedKOTPrinterName = KOTPrinterName;
            kotSizeChangedByUser = false;
            BluetoothPrinterChannel.kot().onDevicePicked(bluetoothKOTAddress);
        }
    }


}