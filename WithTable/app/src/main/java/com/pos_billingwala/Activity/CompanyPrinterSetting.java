package com.pos_billingwala.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Print.DeviceListActivity;
import com.pos_billingwala.Print.DrawerOpenMode;
import com.pos_billingwala.Print.KOTWoosimPrnMng;
import com.pos_billingwala.Print.PosPrinterManager;
import com.pos_billingwala.Print.PrinterConnectionHelper;
import com.pos_billingwala.Print.PrinterConnectionType;
import com.pos_billingwala.Print.UsbPosPrinter;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivityCompanyPrinterSettingBinding;

import java.util.ArrayList;
import java.util.List;


@SuppressLint("NonConstantResourceId, StaticFieldLeak, SetTextI18n")
public class CompanyPrinterSetting extends BaseActivity implements View.OnClickListener {

    public static Activity activity;
    String[] printerList;
    String printerName = "2-Inch", KOTPrinterName = "2-Inch", settingId, logoUse = "off", paymentUse = "off", customerUse = "off", productQuantityUpdate = "off", duplicateBillUse = "off";
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    List<CompanyResponse> companyResponseList = new ArrayList<>();

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS;
    String bluetoothAddress, bluetoothKOTAddress;
    int REQUEST_ENABLE_BT = 4, REQUEST_CONNECT_DEVICE = 6;
    int REQUEST_KOT_ENABLE_BT = 8, REQUEST_KOT_CONNECT_DEVICE = 10;

    String billConnectionType = "BLUETOOTH";
    String autoCut = "on";
    String supportsCashDrawer = "off";
    String autoOpenCashDrawer = "on";
    String drawerOpenMode = "CASH_ONLY";
    String billUsbDeviceKey = "";
    List<UsbDevice> usbDevices = new ArrayList<>();

    ActivityCompanyPrinterSettingBinding binding;


    public static boolean hasPermissions(Context context, String... permissions) {
        int currentAndroidVersion = Build.VERSION.SDK_INT;
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
        View view = binding.getRoot();
        setContentView(view);

        activity = CompanyPrinterSetting.this;

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        binding.invoiceTitle.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        binding.invoiceTermsCondition.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        binding.invoiceTitle.setSelection(binding.invoiceTitle.getText().toString().length());
        binding.invoiceTermsCondition.setSelection(binding.invoiceTermsCondition.getText().toString().length());
        binding.printerFeedLines.setSelection(binding.printerFeedLines.getText().toString().length());
        binding.KotPrinterFeedLines.setSelection(binding.KotPrinterFeedLines.getText().toString().length());

        setupConnectionSpinners();

        binding.printerSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                printerName = printerList[position];
            }
        });
        binding.KOTPrinterSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                KOTPrinterName = printerList[position];
            }
        });

        binding.logoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> logoUse = isChecked ? "on" : "off");
        binding.paymentSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> paymentUse = isChecked ? "on" : "off");
        binding.customerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> customerUse = isChecked ? "on" : "off");
        binding.productQuantityUpdate.setOnCheckedChangeListener((buttonView, isChecked) -> productQuantityUpdate = isChecked ? "on" : "off");
        binding.duplicateBillSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> duplicateBillUse = isChecked ? "on" : "off");
        binding.autoCutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> autoCut = isChecked ? "on" : "off");
        binding.cashDrawerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            supportsCashDrawer = isChecked ? "on" : "off";
            autoOpenCashDrawer = isChecked ? "on" : "off";
        });

        PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (!hasPermissions(activity, PERMISSIONS)) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSION_ALL);
        }

        binding.connectPrinter.setOnClickListener(this);
        binding.connectKOTPrinter.setOnClickListener(this);
        binding.invoicePreview.setOnClickListener(this);
        binding.backToSetting.setOnClickListener(this);
        binding.saveSetting.setOnClickListener(this);
        binding.testCutterButton.setOnClickListener(this);
        binding.testCashDrawerButton.setOnClickListener(this);
        binding.refreshUsbPrinter.setOnClickListener(this);
    }

    private void setupConnectionSpinners() {
        String[] connTypes = new String[]{
                getString(R.string.conn_bluetooth),
                getString(R.string.conn_usb),
                getString(R.string.conn_wifi)
        };
        ArrayAdapter<String> connAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, connTypes);
        connAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        binding.connectionTypeSpinner.setAdapter(connAdapter);
        binding.connectionTypeSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                if (position == 1) {
                    billConnectionType = "USB";
                } else if (position == 2) {
                    billConnectionType = "WIFI";
                } else {
                    billConnectionType = "BLUETOOTH";
                }
                updateConnectionUi();
            }
        });

        String[] drawerModes = new String[]{
                getString(R.string.drawer_cash_only),
                getString(R.string.drawer_always),
                getString(R.string.drawer_never)
        };
        ArrayAdapter<String> drawerAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, drawerModes);
        drawerAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        binding.drawerOpenModeSpinner.setAdapter(drawerAdapter);
        binding.drawerOpenModeSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                if (position == 1) {
                    drawerOpenMode = "ALWAYS";
                } else if (position == 2) {
                    drawerOpenMode = "NEVER";
                } else {
                    drawerOpenMode = "CASH_ONLY";
                }
            }
        });
    }

    private void updateConnectionUi() {
        PrinterConnectionType type = PrinterConnectionType.fromStored(billConnectionType);
        binding.wifiPrinterLayout.setVisibility(type == PrinterConnectionType.WIFI ? View.VISIBLE : View.GONE);
        binding.usbPrinterLayout.setVisibility(type == PrinterConnectionType.USB ? View.VISIBLE : View.GONE);
        if (type == PrinterConnectionType.USB) {
            refreshUsbList();
        }
    }

    private void refreshUsbList() {
        usbDevices = UsbPosPrinter.listLikelyPrinters(activity);
        List<String> labels = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < usbDevices.size(); i++) {
            UsbDevice d = usbDevices.get(i);
            String name = d.getProductName();
            if (name == null || name.trim().isEmpty()) {
                name = "USB " + d.getVendorId() + ":" + d.getProductId();
            }
            String key = UsbPosPrinter.deviceKey(d);
            labels.add(name);
            if (key.equals(billUsbDeviceKey)) {
                selected = i;
            }
        }
        if (labels.isEmpty()) {
            labels.add(getString(R.string.toast_usb_printer_unavailable));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        binding.usbPrinterSpinner.setAdapter(adapter);
        if (!usbDevices.isEmpty()) {
            binding.usbPrinterSpinner.setSelectedIndex(selected);
            billUsbDeviceKey = UsbPosPrinter.deviceKey(usbDevices.get(selected));
            binding.usbPrinterSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
                @Override
                public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                    if (position >= 0 && position < usbDevices.size()) {
                        billUsbDeviceKey = UsbPosPrinter.deviceKey(usbDevices.get(position));
                    }
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            finish();
        } else if (id == R.id.connectPrinter) {
            if (PrinterConnectionType.fromStored(billConnectionType) == PrinterConnectionType.BLUETOOTH) {
                WoosimPrnMng.connectFromButton(activity, bluetoothAddress, CompanyPrinterSetting.this);
            } else {
                Toast.makeText(activity, getString(R.string.ui_save_setting), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.connectKOTPrinter) {
            KOTWoosimPrnMng.connectFromButton(activity, bluetoothKOTAddress, CompanyPrinterSetting.this);
        } else if (id == R.id.invoicePreview) {
            startActivity(new Intent(activity, TestInvoiceBluetoothPrint.class));
        } else if (id == R.id.refreshUsbPrinter) {
            refreshUsbList();
        } else if (id == R.id.testCutterButton) {
            saveTransportOnly();
            new Thread(() -> PrinterConnectionHelper.testCutter(activity), "test-cutter").start();
        } else if (id == R.id.testCashDrawerButton) {
            saveTransportOnly();
            new Thread(() -> PrinterConnectionHelper.testCashDrawer(activity), "test-drawer").start();
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

    private void saveTransportOnly() {
        if (settingId == null || settingId.isEmpty()) {
            return;
        }
        String ip = binding.printerIpAddress.getText() != null
                ? binding.printerIpAddress.getText().toString().trim() : "";
        String port = binding.printerPort.getText() != null
                ? binding.printerPort.getText().toString().trim() : "9100";
        if (port.isEmpty()) {
            port = "9100";
        }
        posBillingWalaDatabase.updatePrinterTransportSettings(
                settingId,
                billConnectionType, billConnectionType,
                ip, ip, port, port,
                billUsbDeviceKey, billUsbDeviceKey,
                autoCut, supportsCashDrawer,
                autoCut, autoOpenCashDrawer,
                drawerOpenMode, "0", "25", "120",
                "FULL", "");
        PosPrinterManager.get().refresh(activity, activity);
    }

    public void addCompanyPrinterSetting() {
        if (binding.saveSetting.getText().toString().equalsIgnoreCase("Save Setting")) {
            posBillingWalaDatabase.addCompanyPrinterSetting(printerName, KOTPrinterName, binding.invoicePrefix.getText().toString(), binding.invoiceTitle.getText().toString(), logoUse, paymentUse, customerUse, productQuantityUpdate, duplicateBillUse, binding.invoiceTermsCondition.getText().toString(), bluetoothAddress, bluetoothKOTAddress, binding.printerFeedLines.getText().toString().isEmpty() ? "1" : binding.printerFeedLines.getText().toString(), binding.KotPrinterFeedLines.getText().toString().isEmpty() ? "1" : binding.KotPrinterFeedLines.getText().toString(), 0);
            Toast.makeText(activity, getString(R.string.toast_company_setting_saved), Toast.LENGTH_SHORT).show();
        } else {
            posBillingWalaDatabase.updateCompanyPrinterSetting(settingId, printerName, KOTPrinterName, binding.invoicePrefix.getText().toString(), binding.invoiceTitle.getText().toString(), logoUse, paymentUse, customerUse, productQuantityUpdate, duplicateBillUse, binding.invoiceTermsCondition.getText().toString(), bluetoothAddress, bluetoothKOTAddress, binding.printerFeedLines.getText().toString().isEmpty() ? "1" : binding.printerFeedLines.getText().toString(), binding.KotPrinterFeedLines.getText().toString().isEmpty() ? "1" : binding.KotPrinterFeedLines.getText().toString(), 0);
            Toast.makeText(activity, getString(R.string.toast_company_setting_updated), Toast.LENGTH_SHORT).show();
        }

        getPrinterSettingDetails();
        if (settingId != null && !settingId.isEmpty()) {
            saveTransportOnly();
        }
        PosPrinterManager.get().refresh(activity, activity);
    }

    @Override
    public void onStart() {
        super.onStart();
        getCompanyDetails();
        getPrinterSettingDetails();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void getCompanyDetails() {
        companyResponseList = posBillingWalaDatabase.getCompanyDetails();
        if (!companyResponseList.isEmpty()) {
            CompanyResponse companyResponse = companyResponseList.get(0);
            String tableStatus = companyResponse.getTableStatus() != null ? companyResponse.getTableStatus() : "";
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
            logoUse = printerSettingResponse.getLogoUse() != null ? printerSettingResponse.getLogoUse() : "off";
            paymentUse = printerSettingResponse.getPaymentUse() != null ? printerSettingResponse.getPaymentUse() : "off";
            customerUse = printerSettingResponse.getCustomerUse() != null ? printerSettingResponse.getCustomerUse() : "off";
            productQuantityUpdate = printerSettingResponse.getProductQuantityUpdate() != null ? printerSettingResponse.getProductQuantityUpdate() : "off";
            duplicateBillUse = printerSettingResponse.getDuplicateBillUse() != null ? printerSettingResponse.getDuplicateBillUse() : "off";
            bluetoothAddress = printerSettingResponse.getBluetoothAddress() != null ? printerSettingResponse.getBluetoothAddress() : "";
            bluetoothKOTAddress = printerSettingResponse.getBluetoothKOTAddress() != null ? printerSettingResponse.getBluetoothKOTAddress() : "";

            billConnectionType = printerSettingResponse.getBillConnectionType() != null
                    ? printerSettingResponse.getBillConnectionType() : "BLUETOOTH";
            autoCut = printerSettingResponse.getAutoCut() != null ? printerSettingResponse.getAutoCut() : "on";
            supportsCashDrawer = printerSettingResponse.getSupportsCashDrawer() != null
                    ? printerSettingResponse.getSupportsCashDrawer() : "off";
            autoOpenCashDrawer = printerSettingResponse.getAutoOpenCashDrawer() != null
                    ? printerSettingResponse.getAutoOpenCashDrawer() : "on";
            drawerOpenMode = printerSettingResponse.getDrawerOpenMode() != null
                    ? printerSettingResponse.getDrawerOpenMode() : "CASH_ONLY";
            billUsbDeviceKey = printerSettingResponse.getBillUsbDeviceKey() != null
                    ? printerSettingResponse.getBillUsbDeviceKey() : "";

            if (binding.printerIpAddress != null) {
                binding.printerIpAddress.setText(printerSettingResponse.getBillPrinterIp() != null
                        ? printerSettingResponse.getBillPrinterIp() : "");
            }
            if (binding.printerPort != null) {
                String port = printerSettingResponse.getBillPrinterPort();
                binding.printerPort.setText(port != null && !port.isEmpty() ? port : "9100");
            }

            if (!bluetoothAddress.equalsIgnoreCase("")
                    && PrinterConnectionType.fromStored(billConnectionType) == PrinterConnectionType.BLUETOOTH) {
                try {
                    new WoosimPrnMng(activity, bluetoothAddress, CompanyPrinterSetting.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!bluetoothKOTAddress.equalsIgnoreCase("")) {
                try {
                    new KOTWoosimPrnMng(activity, bluetoothKOTAddress, CompanyPrinterSetting.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            binding.invoicePrefix.setText(printerSettingResponse.getInvoicePrefix().isEmpty() ? "POS" : printerSettingResponse.getInvoicePrefix());
            binding.printerFeedLines.setText(printerSettingResponse.getPrinterFeedLines().isEmpty() ? "1" : printerSettingResponse.getPrinterFeedLines());
            binding.KotPrinterFeedLines.setText(printerSettingResponse.getKotPrinterFeedLines().isEmpty() ? "1" : printerSettingResponse.getKotPrinterFeedLines());
            binding.invoiceTitle.setText(printerSettingResponse.getInvoiceTitle());
            binding.invoiceTermsCondition.setText(printerSettingResponse.getInvoiceTermsCondition());

            binding.saveSetting.setText("Update Setting");
        } else {
            binding.invoicePrefix.setText("POS");
            binding.printerFeedLines.setText("1");
            binding.KotPrinterFeedLines.setText("1");
            binding.saveSetting.setText("Save Setting");
        }

        binding.logoSwitch.setChecked(logoUse.equalsIgnoreCase("on"));
        binding.paymentSwitch.setChecked(paymentUse.equalsIgnoreCase("on"));
        binding.customerSwitch.setChecked(customerUse.equalsIgnoreCase("on"));
        binding.productQuantityUpdate.setChecked(productQuantityUpdate.equalsIgnoreCase("on"));
        binding.duplicateBillSwitch.setChecked(duplicateBillUse.equalsIgnoreCase("on"));
        binding.autoCutSwitch.setChecked(!"off".equalsIgnoreCase(autoCut));
        binding.cashDrawerSwitch.setChecked("on".equalsIgnoreCase(supportsCashDrawer));

        PrinterConnectionType type = PrinterConnectionType.fromStored(billConnectionType);
        int connIndex = type == PrinterConnectionType.USB ? 1 : (type == PrinterConnectionType.WIFI ? 2 : 0);
        binding.connectionTypeSpinner.setSelectedIndex(connIndex);

        DrawerOpenMode mode = DrawerOpenMode.fromStored(drawerOpenMode);
        int drawerIndex = mode == DrawerOpenMode.ALWAYS ? 1 : (mode == DrawerOpenMode.NEVER ? 2 : 0);
        binding.drawerOpenModeSpinner.setSelectedIndex(drawerIndex);
        updateConnectionUi();

        printerList = activity.getResources().getStringArray(R.array.printer_list);
        try {
            final ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, printerList);
            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
            binding.printerSpinner.setAdapter(adapter);
            binding.KOTPrinterSpinner.setAdapter(adapter);
            if (printerName != null) {
                int printerIndex = adapter.getPosition(printerName);
                if (printerIndex >= 0) {
                    binding.printerSpinner.setSelectedIndex(printerIndex);
                }
            }
            if (KOTPrinterName != null) {
                int kotIndex = adapter.getPosition(KOTPrinterName);
                if (kotIndex >= 0) {
                    binding.KOTPrinterSpinner.setSelectedIndex(kotIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            WoosimPrnMng.connectFromButton(activity, bluetoothAddress, CompanyPrinterSetting.this);
        } else if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == RESULT_OK) {
            bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            new WoosimPrnMng(activity, bluetoothAddress, CompanyPrinterSetting.this);
        } else if (requestCode == REQUEST_KOT_ENABLE_BT && resultCode == RESULT_OK) {
            KOTWoosimPrnMng.connectFromButton(activity, bluetoothKOTAddress, CompanyPrinterSetting.this);
        } else if (requestCode == REQUEST_KOT_CONNECT_DEVICE && resultCode == RESULT_OK) {
            bluetoothKOTAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            new KOTWoosimPrnMng(activity, bluetoothKOTAddress, CompanyPrinterSetting.this);
        }
    }
}
