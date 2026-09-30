package com.pos_billingwala.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.app.AlertDialog;
import android.hardware.usb.UsbDevice;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import com.pos_billingwala.Extra.PosSwitchRowView;
import androidx.core.app.ActivityCompat;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ActionButtonUi;
import com.pos_billingwala.Extra.MessTokenQrHelper;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Print.BluetoothPrinterChannel;
import com.pos_billingwala.Print.DeviceListActivity;
import com.pos_billingwala.Print.EscPosCutHelper;
import com.pos_billingwala.Print.KOTWoosimPrnMng;
import com.pos_billingwala.Print.NetworkEscPosPrinter;
import com.pos_billingwala.Print.PrinterCapabilityManager;
import com.pos_billingwala.Print.PrinterConnectionHelper;
import com.pos_billingwala.Print.PrinterEndpointPrefs;
import com.pos_billingwala.Print.UsbEscPosPrinter;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.Extra.TabletFormUi;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivityCompanyPrinterSettingBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


@SuppressLint("NonConstantResourceId, StaticFieldLeak, SetTextI18n")
public class CompanyPrinterSetting extends BaseActivity implements View.OnClickListener {

    public static Activity activity;
    View view;
    String[] printerList;
    String printerName = "2-Inch", KOTPrinterName = "2-Inch", settingId, logoUse = "off", paymentUse = "off", customerUse = "off", productQuantityUpdate = "off", duplicateBillUse = "off", printFastBill = "off";
    String kotEnable = "on", kotPrefix = "KOT-", kotCopies = "1", kotAutoPrint = "off", kotPreview = "on";
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
    private BroadcastReceiver usbPermissionReceiver;


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
        binding.printFastBillSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (!suppressSwitchListener) {
                printFastBill = isChecked ? "on" : "off";
            }
        });
        binding.kotEnableSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (!suppressSwitchListener) {
                kotEnable = isChecked ? "on" : "off";
                updateKotSettingsVisibility();
            }
        });
        binding.kotAutoPrintSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (!suppressSwitchListener) {
                kotAutoPrint = isChecked ? "on" : "off";
            }
        });
        binding.kotPreviewSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (!suppressSwitchListener) {
                kotPreview = isChecked ? "on" : "off";
            }
        });
        binding.autoCutSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (!suppressSwitchListener) {
                PrinterCapabilityManager.setAutoCutEnabled(activity, isChecked);
                updateCutTypeVisibility(isChecked);
            }
        });
        binding.cutTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (suppressSwitchListener) {
                return;
            }
            EscPosCutHelper.CutType type = EscPosCutHelper.CutType.DEFAULT;
            if (checkedId == R.id.cutTypeFull) {
                type = EscPosCutHelper.CutType.FULL;
            } else if (checkedId == R.id.cutTypePartial) {
                type = EscPosCutHelper.CutType.PARTIAL;
            }
            PrinterCapabilityManager.setCutType(activity, type);
        });

        loadAutoCutSettings();
        loadEndpointSettings();

        PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (!hasPermissions(activity, PERMISSIONS)) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSION_ALL);
        }

        binding.connectPrinter.setOnClickListener(this);
        binding.disconnectPrinter.setOnClickListener(this);
        binding.connectKOTPrinter.setOnClickListener(this);
        binding.disconnectKOTPrinter.setOnClickListener(this);
        binding.invoicePreview.setOnClickListener(this);
        binding.kotPreview.setOnClickListener(this);
        binding.messCouponPreview.setOnClickListener(this);
        binding.messQrTokenPreview.setOnClickListener(this);
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
        View previewCard = container.getChildCount() >= 5 ? container.getChildAt(4) : null;
        View[] left = {container.getChildAt(0), container.getChildAt(1)};
        View[] right = {container.getChildAt(2), container.getChildAt(3)};
        if (previewCard != null) {
            container.removeView(previewCard);
        }
        TabletFormUi.applyTwoColumnCards(this, container, left, right);
        if (previewCard != null && previewCard.getParent() == null) {
            float density = getResources().getDisplayMetrics().density;
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = (int) (16 * density);
            previewCard.setLayoutParams(params);
            container.addView(previewCard);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            finish();
        } else if (id == R.id.connectPrinter) {
            connectBillEndpoint();
        } else if (id == R.id.disconnectPrinter) {
            disconnectInvoicePrinter();
        } else if (id == R.id.connectKOTPrinter) {
            connectKotEndpoint();
        } else if (id == R.id.disconnectKOTPrinter) {
            disconnectKotPrinter();
        } else if (id == R.id.invoicePreview) {
            Intent invoicePreview = new Intent(activity, TestInvoiceBluetoothPrint.class);
            invoicePreview.putExtra(TestInvoiceBluetoothPrint.EXTRA_PREVIEW_MODE,
                    TestInvoiceBluetoothPrint.MODE_INVOICE);
            startActivity(invoicePreview);
        } else if (id == R.id.kotPreview) {
            Intent kotPreviewIntent = new Intent(activity, TestInvoiceBluetoothPrint.class);
            kotPreviewIntent.putExtra(TestInvoiceBluetoothPrint.EXTRA_PREVIEW_MODE,
                    TestInvoiceBluetoothPrint.MODE_KOT);
            startActivity(kotPreviewIntent);
        } else if (id == R.id.messCouponPreview) {
            Intent messCoupon = new Intent(activity, CouponBluetoothPrint.class);
            messCoupon.putExtra("invoiceRunningStatus", "printBill");
            messCoupon.putExtra("cartOrderStatus", "mess");
            messCoupon.putExtra("memberId", "0");
            messCoupon.putExtra("memberName", "Demo Member");
            messCoupon.putExtra("memberMobileNumber", "9876543210");
            messCoupon.putExtra("messDays", "2");
            messCoupon.putExtra("messInvoiceResponseList", "0");
            messCoupon.putExtra(CouponBluetoothPrint.EXTRA_PREVIEW_ONLY, true);
            startActivity(messCoupon);
        } else if (id == R.id.messQrTokenPreview) {
            Intent messQr = new Intent(activity, MessTokenBluetoothPrint.class);
            messQr.putExtra("tokenCode", MessTokenQrHelper.generateTokenCode());
            messQr.putExtra("memberId", "0");
            messQr.putExtra("memberName", "Demo Member");
            messQr.putExtra("memberMobile", "9876543210");
            messQr.putExtra("memberType", MessTokenQrHelper.MEMBER_TYPE_MEMBER);
            messQr.putExtra("messType", "Lunch");
            messQr.putExtra("tokenAmount", "0");
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            messQr.putExtra("tokenDate", df.format(Calendar.getInstance().getTime()));
            messQr.putExtra("tokenNetworkStatus", "preview");
            messQr.putExtra(MessTokenBluetoothPrint.EXTRA_PREVIEW_ONLY, true);
            startActivity(messQr);
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
            posBillingWalaDatabase.addCompanyPrinterSetting(printerName, KOTPrinterName, binding.invoicePrefix.getText().toString(), binding.invoiceTitle.getText().toString(), logoUse, paymentUse, customerUse, productQuantityUpdate, duplicateBillUse, printFastBill, binding.invoiceTermsCondition.getText().toString(), bluetoothAddress, bluetoothKOTAddress, binding.printerFeedLines.getText().toString().isEmpty() ? "1" : binding.printerFeedLines.getText().toString(), binding.KotPrinterFeedLines.getText().toString().isEmpty() ? "1" : binding.KotPrinterFeedLines.getText().toString(), 0);
            Toast.makeText(activity, getString(R.string.toast_company_setting_saved), Toast.LENGTH_SHORT).show();
        } else {
            posBillingWalaDatabase.updateCompanyPrinterSetting(settingId, printerName, KOTPrinterName, binding.invoicePrefix.getText().toString(), binding.invoiceTitle.getText().toString(), logoUse, paymentUse, customerUse, productQuantityUpdate, duplicateBillUse, printFastBill, binding.invoiceTermsCondition.getText().toString(), bluetoothAddress, bluetoothKOTAddress, binding.printerFeedLines.getText().toString().isEmpty() ? "1" : binding.printerFeedLines.getText().toString(), binding.KotPrinterFeedLines.getText().toString().isEmpty() ? "1" : binding.KotPrinterFeedLines.getText().toString(), 0);
            Toast.makeText(activity, getString(R.string.toast_company_setting_updated), Toast.LENGTH_SHORT).show();
        }

        captureKotSwitchState();
        String savedKotEnable = kotEnable;
        String savedKotAutoPrint = kotAutoPrint;
        String savedKotPreview = kotPreview;
        if (settingId == null || settingId.trim().isEmpty()) {
            getPrinterSettingDetails();
        }
        kotEnable = savedKotEnable;
        kotAutoPrint = savedKotAutoPrint;
        kotPreview = savedKotPreview;
        persistKotSettings();
        getPrinterSettingDetails();
    }

    /** Switch position is the value to save. Reload must not run before this. */
    private void captureKotSwitchState() {
        kotEnable = binding.kotEnableSwitch.isChecked() ? "on" : "off";
        kotAutoPrint = binding.kotAutoPrintSwitch.isChecked() ? "on" : "off";
        kotPreview = binding.kotPreviewSwitch.isChecked() ? "on" : "off";
    }

    private void persistKotSettings() {
        if (settingId == null || settingId.trim().isEmpty()) {
            return;
        }
        String prefix = binding.kotPrefix.getText() != null ? binding.kotPrefix.getText().toString().trim() : "KOT-";
        if (prefix.isEmpty()) {
            prefix = "KOT-";
        }
        String copies = binding.kotCopies.getText() != null ? binding.kotCopies.getText().toString().trim() : "1";
        if (copies.isEmpty()) {
            copies = "1";
        }
        posBillingWalaDatabase.updateKotSettings(settingId, kotEnable, prefix, copies, kotAutoPrint, kotPreview);
        persistEndpointSettings();
    }

    private void persistEndpointSettings() {
        persistNetworkFields();
        PrinterEndpointPrefs.setAutoShareOnSave(activity,
                binding.autoShareOnSaveSwitch.isChecked());
    }

    private void persistNetworkFields() {
        String host = binding.networkHost.getText() != null
                ? binding.networkHost.getText().toString().trim() : "";
        int port = 9100;
        try {
            String portText = binding.networkPort.getText() != null
                    ? binding.networkPort.getText().toString().trim() : "9100";
            if (!portText.isEmpty()) {
                port = Integer.parseInt(portText);
            }
        } catch (Exception ignored) {
        }
        PrinterEndpointPrefs.setNetwork(activity, host, port);
    }

    private void loadEndpointSettings() {
        suppressSwitchListener = true;
        applyTransportRadio(binding.billTransportGroup, PrinterEndpointPrefs.billTransport(activity),
                R.id.billTransportBluetooth, R.id.billTransportUsb, R.id.billTransportNetwork);
        applyTransportRadio(binding.kotTransportGroup, PrinterEndpointPrefs.kotTransport(activity),
                R.id.kotTransportBluetooth, R.id.kotTransportUsb, R.id.kotTransportNetwork);
        binding.networkHost.setText(PrinterEndpointPrefs.networkHost(activity));
        binding.networkPort.setText(String.valueOf(PrinterEndpointPrefs.networkPort(activity)));
        setSwitchCheckedSilently(binding.autoShareOnSaveSwitch,
                PrinterEndpointPrefs.isAutoShareOnSave(activity));
        suppressSwitchListener = false;

        binding.billTransportGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (suppressSwitchListener) {
                return;
            }
            PrinterEndpointPrefs.setBillTransport(activity, transportFromRadio(checkedId,
                    R.id.billTransportUsb, R.id.billTransportNetwork));
            updateTransportUi();
        });
        binding.kotTransportGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (suppressSwitchListener) {
                return;
            }
            PrinterEndpointPrefs.setKotTransport(activity, transportFromRadio(checkedId,
                    R.id.kotTransportUsb, R.id.kotTransportNetwork));
            updateTransportUi();
        });
        binding.autoShareOnSaveSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (!suppressSwitchListener) {
                PrinterEndpointPrefs.setAutoShareOnSave(activity, isChecked);
            }
        });
        updateTransportUi();
    }

    private static void applyTransportRadio(RadioGroup group, PrinterEndpointPrefs.Transport transport,
                                            int bluetoothId, int usbId, int networkId) {
        if (transport == PrinterEndpointPrefs.Transport.USB) {
            group.check(usbId);
        } else if (transport == PrinterEndpointPrefs.Transport.NETWORK) {
            group.check(networkId);
        } else {
            group.check(bluetoothId);
        }
    }

    private static PrinterEndpointPrefs.Transport transportFromRadio(int checkedId, int usbId, int networkId) {
        if (checkedId == usbId) {
            return PrinterEndpointPrefs.Transport.USB;
        }
        if (checkedId == networkId) {
            return PrinterEndpointPrefs.Transport.NETWORK;
        }
        return PrinterEndpointPrefs.Transport.BLUETOOTH;
    }

    private void updateTransportUi() {
        PrinterEndpointPrefs.Transport bill = PrinterEndpointPrefs.billTransport(activity);
        PrinterEndpointPrefs.Transport kot = PrinterEndpointPrefs.kotTransport(activity);
        boolean showNetwork = bill == PrinterEndpointPrefs.Transport.NETWORK
                || kot == PrinterEndpointPrefs.Transport.NETWORK;
        binding.networkPrinterFields.setVisibility(showNetwork ? View.VISIBLE : View.GONE);

        boolean billUsb = bill == PrinterEndpointPrefs.Transport.USB;
        binding.billUsbStatus.setVisibility(billUsb ? View.VISIBLE : View.GONE);
        if (billUsb) {
            String name = PrinterEndpointPrefs.billUsbName(activity);
            if (name.isEmpty()) {
                name = PrinterEndpointPrefs.billUsbId(activity);
            }
            binding.billUsbStatus.setText(name.isEmpty()
                    ? getString(R.string.ui_usb_tap_connect)
                    : getString(R.string.ui_usb_selected, name));
        }

        boolean kotUsb = kot == PrinterEndpointPrefs.Transport.USB;
        binding.kotUsbStatus.setVisibility(kotUsb ? View.VISIBLE : View.GONE);
        if (kotUsb) {
            String name = PrinterEndpointPrefs.kotUsbName(activity);
            if (name.isEmpty()) {
                name = PrinterEndpointPrefs.kotUsbId(activity);
            }
            binding.kotUsbStatus.setText(name.isEmpty()
                    ? getString(R.string.ui_usb_tap_connect)
                    : getString(R.string.ui_usb_selected, name));
        }
        updatePrinterConnectionUi();
    }

    private void connectBillEndpoint() {
        persistNetworkFields();
        PrinterEndpointPrefs.Transport transport = PrinterEndpointPrefs.billTransport(activity);
        if (transport == PrinterEndpointPrefs.Transport.NETWORK) {
            testNetworkPrinter();
            return;
        }
        if (transport == PrinterEndpointPrefs.Transport.USB) {
            pickUsbPrinter(false);
            return;
        }
        WoosimPrnMng.connectFromButton(activity, bluetoothAddress, CompanyPrinterSetting.this, billSizeChangedByUser);
    }

    private void connectKotEndpoint() {
        persistNetworkFields();
        PrinterEndpointPrefs.Transport transport = PrinterEndpointPrefs.kotTransport(activity);
        if (transport == PrinterEndpointPrefs.Transport.NETWORK) {
            testNetworkPrinter();
            return;
        }
        if (transport == PrinterEndpointPrefs.Transport.USB) {
            pickUsbPrinter(true);
            return;
        }
        KOTWoosimPrnMng.connectFromButton(activity, bluetoothKOTAddress, CompanyPrinterSetting.this, kotSizeChangedByUser);
    }

    private void testNetworkPrinter() {
        persistNetworkFields();
        if (PrinterEndpointPrefs.networkHost(activity).trim().isEmpty()) {
            Toast.makeText(activity, R.string.toast_enter_network_printer, Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            boolean ok = NetworkEscPosPrinter.testConnection(activity);
            runOnUiThread(() -> {
                Toast.makeText(activity,
                        ok ? R.string.toast_network_test_ok : R.string.toast_network_test_fail,
                        Toast.LENGTH_SHORT).show();
                updatePrinterConnectionUi();
            });
        }).start();
    }

    private void ensureUsbPermissionReceiver() {
        if (usbPermissionReceiver != null) {
            return;
        }
        usbPermissionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || !UsbEscPosPrinter.ACTION_USB_PERMISSION.equals(intent.getAction())) {
                    return;
                }
                updateTransportUi();
            }
        };
    }

    @Override
    protected void onDestroy() {
        if (usbPermissionReceiver != null) {
            try {
                unregisterReceiver(usbPermissionReceiver);
            } catch (Exception ignored) {
            }
            usbPermissionReceiver = null;
        }
        super.onDestroy();
    }

    private void pickUsbPrinter(boolean isKot) {
        java.util.List<UsbDevice> devices = UsbEscPosPrinter.listPrinters(activity);
        if (devices.isEmpty()) {
            Toast.makeText(activity, R.string.toast_select_usb_printer, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            UsbDevice d = devices.get(i);
            labels[i] = UsbEscPosPrinter.deviceDisplayName(d) + " (" + UsbEscPosPrinter.deviceId(d) + ")";
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.ui_select_usb_printer)
                .setItems(labels, (dialog, which) -> {
                    UsbDevice selected = devices.get(which);
                    String id = UsbEscPosPrinter.deviceId(selected);
                    String name = UsbEscPosPrinter.deviceDisplayName(selected);
                    if (!UsbEscPosPrinter.hasPermission(activity, selected)) {
                        ensureUsbPermissionReceiver();
                        UsbEscPosPrinter.requestPermission(activity, selected, usbPermissionReceiver);
                    }
                    PrinterEndpointPrefs.setUsbFor(activity, isKot, id, name);
                    updateTransportUi();
                    Toast.makeText(activity, getString(R.string.ui_usb_selected, name), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void loadAutoCutSettings() {
        boolean enabled = PrinterCapabilityManager.isAutoCutEnabled(activity);
        setSwitchCheckedSilently(binding.autoCutSwitch, enabled);
        EscPosCutHelper.CutType type = PrinterCapabilityManager.cutType(activity);
        suppressSwitchListener = true;
        if (type == EscPosCutHelper.CutType.FULL) {
            binding.cutTypeGroup.check(R.id.cutTypeFull);
        } else if (type == EscPosCutHelper.CutType.PARTIAL) {
            binding.cutTypeGroup.check(R.id.cutTypePartial);
        } else {
            binding.cutTypeGroup.check(R.id.cutTypeDefault);
        }
        suppressSwitchListener = false;
        updateCutTypeVisibility(enabled);
    }

    private void updateCutTypeVisibility(boolean autoCutEnabled) {
        binding.cutTypeContainer.setVisibility(autoCutEnabled ? View.VISIBLE : View.GONE);
    }

    private void updateKotSettingsVisibility() {
        boolean enabled = "on".equalsIgnoreCase(kotEnable);
        binding.kotDetailsContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
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
        updateKotSettingsVisibility();
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
            printFastBill = printerSettingResponse.getPrintFastBill() != null
                    && !printerSettingResponse.getPrintFastBill().isEmpty()
                    ? printerSettingResponse.getPrintFastBill() : "off";
            kotEnable = flagToOnOff(printerSettingResponse.getKotEnable(), true);
            kotPrefix = printerSettingResponse.getKotPrefix() != null && !printerSettingResponse.getKotPrefix().isEmpty()
                    ? printerSettingResponse.getKotPrefix() : "KOT-";
            kotCopies = printerSettingResponse.getKotCopies() != null && !printerSettingResponse.getKotCopies().isEmpty()
                    ? printerSettingResponse.getKotCopies() : "1";
            kotAutoPrint = flagToOnOff(printerSettingResponse.getKotAutoPrint(), false);
            kotPreview = flagToOnOff(printerSettingResponse.getKotPreview(), true);
            bluetoothAddress = printerSettingResponse.getBluetoothAddress() != null ? printerSettingResponse.getBluetoothAddress() : "";
            bluetoothKOTAddress = printerSettingResponse.getBluetoothKOTAddress() != null ? printerSettingResponse.getBluetoothKOTAddress() : "";
            binding.invoicePrefix.setText(printerSettingResponse.getInvoicePrefix().isEmpty() ? "POS" : printerSettingResponse.getInvoicePrefix());
            binding.printerFeedLines.setText(printerSettingResponse.getPrinterFeedLines().isEmpty() ? "1" : printerSettingResponse.getPrinterFeedLines());
            binding.KotPrinterFeedLines.setText(printerSettingResponse.getKotPrinterFeedLines().isEmpty() ? "1" : printerSettingResponse.getKotPrinterFeedLines());
            binding.invoiceTitle.setText(printerSettingResponse.getInvoiceTitle());
            binding.invoiceTermsCondition.setText(printerSettingResponse.getInvoiceTermsCondition());
            binding.kotPrefix.setText(kotPrefix);
            binding.kotCopies.setText(kotCopies);

            ActionButtonUi.bind(binding.saveSetting.getRoot(), R.drawable.ic_save, R.string.ui_update_settings);
        } else {
            binding.invoicePrefix.setText("POS");
            binding.printerFeedLines.setText("1");
            binding.KotPrinterFeedLines.setText("1");
            binding.kotPrefix.setText("KOT-");
            binding.kotCopies.setText("1");
            ActionButtonUi.bind(binding.saveSetting.getRoot(), R.drawable.ic_save, R.string.ui_save_setting);
        }

        setSwitchCheckedSilently(binding.logoSwitch, logoUse.equalsIgnoreCase("on"));
        setSwitchCheckedSilently(binding.paymentSwitch, paymentUse.equalsIgnoreCase("on"));
        setSwitchCheckedSilently(binding.customerSwitch, customerUse.equalsIgnoreCase("on"));
        setSwitchCheckedSilently(binding.productQuantityUpdate, productQuantityUpdate.equalsIgnoreCase("on"));
        setSwitchCheckedSilently(binding.duplicateBillSwitch, duplicateBillUse.equalsIgnoreCase("on"));
        setSwitchCheckedSilently(binding.printFastBillSwitch, printFastBill.equalsIgnoreCase("on"));
        setSwitchCheckedSilently(binding.kotEnableSwitch, kotEnable.equalsIgnoreCase("on"));
        setSwitchCheckedSilently(binding.kotAutoPrintSwitch, kotAutoPrint.equalsIgnoreCase("on"));
        setSwitchCheckedSilently(binding.kotPreviewSwitch, isKotPreviewOn(kotPreview));
        updateKotSettingsVisibility();

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
            if (PrinterEndpointPrefs.billTransport(activity) == PrinterEndpointPrefs.Transport.BLUETOOTH
                    && !TextUtils.isEmpty(bluetoothAddress)
                    && !BluetoothPrinterChannel.bill().isReady()
                    && !BluetoothPrinterChannel.bill().isConnecting()) {
                PrinterConnectionHelper.autoConnectBillPrinter(activity, bluetoothAddress);
            }
            boolean sameAsBill = !TextUtils.isEmpty(bluetoothKOTAddress)
                    && bluetoothKOTAddress.equalsIgnoreCase(bluetoothAddress);
            if (PrinterEndpointPrefs.kotTransport(activity) == PrinterEndpointPrefs.Transport.BLUETOOTH
                    && !sameAsBill
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
        PrinterEndpointPrefs.Transport transport = PrinterEndpointPrefs.billTransport(activity);
        if (transport == PrinterEndpointPrefs.Transport.NETWORK) {
            return !PrinterEndpointPrefs.networkHost(activity).trim().isEmpty();
        }
        if (transport == PrinterEndpointPrefs.Transport.USB) {
            String id = PrinterEndpointPrefs.billUsbId(activity);
            return !TextUtils.isEmpty(id) && UsbEscPosPrinter.findById(activity, id) != null;
        }
        return !TextUtils.isEmpty(bluetoothAddress) && BluetoothPrinterChannel.bill().isReady();
    }

    private boolean isKotPrinterConnected() {
        PrinterEndpointPrefs.Transport transport = PrinterEndpointPrefs.kotTransport(activity);
        if (transport == PrinterEndpointPrefs.Transport.NETWORK) {
            return !PrinterEndpointPrefs.networkHost(activity).trim().isEmpty();
        }
        if (transport == PrinterEndpointPrefs.Transport.USB) {
            String id = PrinterEndpointPrefs.kotUsbId(activity);
            return !TextUtils.isEmpty(id) && UsbEscPosPrinter.findById(activity, id) != null;
        }
        return !TextUtils.isEmpty(bluetoothKOTAddress) && BluetoothPrinterChannel.kot().isReady();
    }

    private void disconnectInvoicePrinter() {
        PrinterEndpointPrefs.Transport transport = PrinterEndpointPrefs.billTransport(activity);
        if (transport == PrinterEndpointPrefs.Transport.USB) {
            PrinterEndpointPrefs.setBillUsb(activity, "", "");
        } else if (transport == PrinterEndpointPrefs.Transport.NETWORK) {
            PrinterEndpointPrefs.setNetwork(activity, "", PrinterEndpointPrefs.networkPort(activity));
            binding.networkHost.setText("");
        } else {
            bluetoothAddress = "";
            BluetoothPrinterChannel.bill().disconnect(activity);
            persistConnectionState();
        }
        updateTransportUi();
    }

    private void disconnectKotPrinter() {
        PrinterEndpointPrefs.Transport transport = PrinterEndpointPrefs.kotTransport(activity);
        if (transport == PrinterEndpointPrefs.Transport.USB) {
            PrinterEndpointPrefs.setKotUsb(activity, "", "");
        } else if (transport == PrinterEndpointPrefs.Transport.NETWORK) {
            PrinterEndpointPrefs.setNetwork(activity, "", PrinterEndpointPrefs.networkPort(activity));
            binding.networkHost.setText("");
        } else {
            bluetoothKOTAddress = "";
            BluetoothPrinterChannel.kot().disconnect(activity);
            persistConnectionState();
        }
        updateTransportUi();
    }

    private void persistConnectionState() {
        if (settingId == null || settingId.isEmpty()) {
            return;
        }
        posBillingWalaDatabase.updateCompanyPrinterSetting(settingId, printerName, KOTPrinterName,
                binding.invoicePrefix.getText().toString(), binding.invoiceTitle.getText().toString(),
                logoUse, paymentUse, customerUse, productQuantityUpdate, duplicateBillUse, printFastBill,
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

    /** Maps on/1/true and off/0/false into the values the switches save. */
    private static String flagToOnOff(String value, boolean defaultOn) {
        if (value == null || value.trim().isEmpty()) {
            return defaultOn ? "on" : "off";
        }
        return com.pos_billingwala.Extra.DineInKotHelper.isFlagOn(value) ? "on" : "off";
    }

    /** Empty or unknown values stay on, matching Flutter (off / 0 / false / no disable it). */
    private static boolean isKotPreviewOn(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        return !("off".equals(v) || "0".equals(v) || "false".equals(v) || "no".equals(v));
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