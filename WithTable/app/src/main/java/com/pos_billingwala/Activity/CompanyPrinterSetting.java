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
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import com.pos_billingwala.Extra.PosSwitchRowView;
import androidx.core.app.ActivityCompat;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ActionButtonUi;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Print.BluetoothPrinterChannel;
import com.pos_billingwala.Print.DeviceListActivity;
import com.pos_billingwala.Print.KOTWoosimPrnMng;
import com.pos_billingwala.Print.PrinterConnectionHelper;
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
    boolean loadingDropdowns;
    boolean billSizeChangedByUser;
    boolean kotSizeChangedByUser;
    boolean printerSettingsLoaded;
    boolean autoConnectAttempted;
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

        binding.printerDropdown.setOnItemSelectedListener((position, label) -> {
            if (loadingDropdowns || printerList == null || position < 0 || position >= printerList.length) {
                return;
            }
            printerName = printerList[position];
            billSizeChangedByUser = lastConnectedPrinterName == null
                    || !lastConnectedPrinterName.equalsIgnoreCase(printerName);
        });
        binding.kotPrinterDropdown.setOnItemSelectedListener((position, label) -> {
            if (loadingDropdowns || printerList == null || position < 0 || position >= printerList.length) {
                return;
            }
            KOTPrinterName = printerList[position];
            kotSizeChangedByUser = lastConnectedKOTPrinterName == null
                    || !lastConnectedKOTPrinterName.equalsIgnoreCase(KOTPrinterName);
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
        binding.disconnectPrinter.setOnClickListener(this);
        binding.connectKOTPrinter.setOnClickListener(this);
        binding.disconnectKOTPrinter.setOnClickListener(this);
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
        } else if (id == R.id.disconnectPrinter) {
            disconnectInvoicePrinter();
        } else if (id == R.id.connectKOTPrinter) {
            KOTWoosimPrnMng.connectFromButton(activity, bluetoothKOTAddress, CompanyPrinterSetting.this, kotSizeChangedByUser);
        } else if (id == R.id.disconnectKOTPrinter) {
            disconnectKotPrinter();
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
        autoConnectSavedPrinters();
        updatePrinterConnectionUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePrinterConnectionUi();
    }

    public void getCompanyDetails() {
        companyResponseList = posBillingWalaDatabase.getCompanyDetails();
        binding.KOTPrinterLayout.setVisibility(View.VISIBLE);
        binding.KotPrinterFeedLinesLayout.setVisibility(View.VISIBLE);
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
        loadingDropdowns = true;
        try {
            binding.printerDropdown.setItems(printerList);
            binding.kotPrinterDropdown.setItems(printerList);
            if (printerName != null) {
                for (int i = 0; i < printerList.length; i++) {
                    if (printerName.equals(printerList[i])) {
                        binding.printerDropdown.setSelectedIndex(i);
                        break;
                    }
                }
            }
            if (KOTPrinterName != null) {
                for (int i = 0; i < printerList.length; i++) {
                    if (KOTPrinterName.equals(printerList[i])) {
                        binding.kotPrinterDropdown.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            loadingDropdowns = false;
        }

        updatePrinterConnectionUi();
    }

    private void autoConnectSavedPrinters() {
        // Once per screen visit — re-running on every onStart (device list / BT enable
        // return) races focus windows with the system pairing dialog and caused ANRs.
        if (autoConnectAttempted) {
            return;
        }
        autoConnectAttempted = true;
        View root = binding != null ? binding.getRoot() : null;
        if (root == null) {
            return;
        }
        root.post(() -> {
            if (isFinishing()) {
                return;
            }
            if (!TextUtils.isEmpty(bluetoothAddress)
                    && !BluetoothPrinterChannel.bill().isReady()
                    && !BluetoothPrinterChannel.bill().isConnecting()) {
                PrinterConnectionHelper.autoConnectBillPrinter(activity, bluetoothAddress);
            }
            boolean sameAsBill = !TextUtils.isEmpty(bluetoothKOTAddress)
                    && bluetoothKOTAddress.equalsIgnoreCase(bluetoothAddress);
            if (!sameAsBill
                    && !TextUtils.isEmpty(bluetoothKOTAddress)
                    && !BluetoothPrinterChannel.kot().isReady()
                    && !BluetoothPrinterChannel.kot().isConnecting()) {
                PrinterConnectionHelper.autoConnectKotPrinter(activity, bluetoothKOTAddress);
            }
            root.postDelayed(this::updatePrinterConnectionUi, 800);
        });
    }

    private void updatePrinterConnectionUi() {
        boolean invoiceConnected = isInvoicePrinterConnected();
        boolean kotConnected = isKotPrinterConnected();

        binding.invoiceConnectedStatus.setVisibility(invoiceConnected ? View.VISIBLE : View.GONE);
        binding.connectPrinter.setVisibility(invoiceConnected ? View.GONE : View.VISIBLE);
        binding.disconnectPrinter.setVisibility(invoiceConnected ? View.VISIBLE : View.GONE);

        binding.kotConnectedStatus.setVisibility(kotConnected ? View.VISIBLE : View.GONE);
        binding.connectKOTPrinter.setVisibility(kotConnected ? View.GONE : View.VISIBLE);
        binding.disconnectKOTPrinter.setVisibility(kotConnected ? View.VISIBLE : View.GONE);
    }

    private boolean isInvoicePrinterConnected() {
        return !TextUtils.isEmpty(bluetoothAddress) && BluetoothPrinterChannel.bill().isReady();
    }

    private boolean isKotPrinterConnected() {
        return !TextUtils.isEmpty(bluetoothKOTAddress) && BluetoothPrinterChannel.kot().isReady();
    }

    private void disconnectInvoicePrinter() {
        bluetoothAddress = "";
        BluetoothPrinterChannel.bill().disconnect(activity);
        persistConnectionState();
        updatePrinterConnectionUi();
    }

    private void disconnectKotPrinter() {
        bluetoothKOTAddress = "";
        BluetoothPrinterChannel.kot().disconnect(activity);
        persistConnectionState();
        updatePrinterConnectionUi();
    }

    private void persistConnectionState() {
        if (settingId == null || settingId.isEmpty()) {
            return;
        }
        posBillingWalaDatabase.updateCompanyPrinterSetting(settingId, printerName, KOTPrinterName,
                binding.invoicePrefix.getText().toString(), binding.invoiceTitle.getText().toString(),
                logoUse, paymentUse, customerUse, productQuantityUpdate, duplicateBillUse,
                binding.invoiceTermsCondition.getText().toString(),
                bluetoothAddress != null ? bluetoothAddress : "",
                bluetoothKOTAddress != null ? bluetoothKOTAddress : "",
                binding.printerFeedLines.getText().toString().isEmpty() ? "1" : binding.printerFeedLines.getText().toString(),
                binding.KotPrinterFeedLines.getText().toString().isEmpty() ? "1" : binding.KotPrinterFeedLines.getText().toString(),
                0);
    }

    private void setSwitchCheckedSilently(PosSwitchRowView switchView, boolean checked) {
        suppressSwitchListener = true;
        switchView.setChecked(checked);
        suppressSwitchListener = false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            WoosimPrnMng.connectFromButton(activity, bluetoothAddress, CompanyPrinterSetting.this, billSizeChangedByUser);
        } else if (requestCode == REQUEST_CONNECT_DEVICE) {
            if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                lastConnectedPrinterName = printerName;
                billSizeChangedByUser = false;
                PrinterConnectionHelper.onBillDevicePicked(activity, bluetoothAddress);
                persistConnectionState();
                binding.getRoot().postDelayed(this::updatePrinterConnectionUi, 800);
            }
        } else if (requestCode == REQUEST_KOT_ENABLE_BT && resultCode == RESULT_OK) {
            KOTWoosimPrnMng.connectFromButton(activity, bluetoothKOTAddress, CompanyPrinterSetting.this, kotSizeChangedByUser);
        } else if (requestCode == REQUEST_KOT_CONNECT_DEVICE) {
            if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                bluetoothKOTAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                lastConnectedKOTPrinterName = KOTPrinterName;
                kotSizeChangedByUser = false;
                PrinterConnectionHelper.onKotDevicePicked(activity, bluetoothKOTAddress);
                persistConnectionState();
                binding.getRoot().postDelayed(this::updatePrinterConnectionUi, 800);
            }
        }
    }


}