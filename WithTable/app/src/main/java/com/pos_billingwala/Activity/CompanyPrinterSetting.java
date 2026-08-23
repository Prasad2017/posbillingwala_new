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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Print.DeviceListActivity;
import com.pos_billingwala.Print.KOTWoosimPrnMng;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivityCompanyPrinterSettingBinding;

import java.util.ArrayList;
import java.util.List;


@SuppressLint("NonConstantResourceId, StaticFieldLeak, SetTextI18n")
public class CompanyPrinterSetting extends AppCompatActivity implements View.OnClickListener {

    public static Activity activity;
    View view;
    String[] printerList;
    String printerName = "2-Inch", KOTPrinterName = "2-Inch", settingId, logoUse = "off", paymentUse = "off", customerUse = "off", productQuantityUpdate = "off";
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

        binding.printerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                printerName = printerList[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        binding.KOTPrinterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                KOTPrinterName = printerList[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        binding.logoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    logoUse = "on";
                } else {
                    logoUse = "off";
                }
            }
        });

        binding.paymentSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    paymentUse = "on";
                } else {
                    paymentUse = "off";
                }
            }
        });

        binding.customerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    customerUse = "on";
                } else {
                    customerUse = "off";
                }
            }
        });

        binding.productQuantityUpdate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    productQuantityUpdate = "on";
                } else {
                    productQuantityUpdate = "off";
                }
            }
        });

        PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (!hasPermissions(activity, PERMISSIONS)) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSION_ALL);
        }

        binding.connectPrinter.setOnClickListener(this);
        binding.connectKOTPrinter.setOnClickListener(this);
        binding.backToSetting.setOnClickListener(this);
        binding.saveSetting.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            finish();
        } else if (id == R.id.connectPrinter) {
            //Printer not connected and send request for connecting printer
            new WoosimPrnMng(activity, "", CompanyPrinterSetting.this);
        } else if (id == R.id.connectKOTPrinter) {
            //Printer not connected and send request for connecting printer
            new WoosimPrnMng(activity, "", CompanyPrinterSetting.this);
        } else if (id == R.id.saveSetting) {
            if (printerName != null) {
                if (!binding.invoicePrefix.getText().toString().isEmpty()) {
                    addCompanyPrinterSetting();
                } else {
                    Toast.makeText(activity, "Please add invoice prefix", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, "Please select printer", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void addCompanyPrinterSetting() {

        if (binding.saveSetting.getText().toString().equalsIgnoreCase("Save Setting")) {
            posBillingWalaDatabase.addCompanyPrinterSetting(printerName, KOTPrinterName, binding.invoicePrefix.getText().toString(), binding.invoiceTitle.getText().toString(), logoUse, paymentUse, customerUse, productQuantityUpdate, binding.invoiceTermsCondition.getText().toString(), bluetoothAddress, bluetoothKOTAddress, binding.printerFeedLines.getText().toString().isEmpty() ? "1" : binding.printerFeedLines.getText().toString(), binding.KotPrinterFeedLines.getText().toString().isEmpty() ? "1" : binding.KotPrinterFeedLines.getText().toString(), 0);
            Toast.makeText(activity, "Company setting saved", Toast.LENGTH_SHORT).show();
        } else {
            posBillingWalaDatabase.updateCompanyPrinterSetting(settingId, printerName, KOTPrinterName, binding.invoicePrefix.getText().toString(), binding.invoiceTitle.getText().toString(), logoUse, paymentUse, customerUse, productQuantityUpdate, binding.invoiceTermsCondition.getText().toString(), bluetoothAddress, bluetoothKOTAddress, binding.printerFeedLines.getText().toString().isEmpty() ? "1" : binding.printerFeedLines.getText().toString(), binding.KotPrinterFeedLines.getText().toString().isEmpty() ? "1" : binding.KotPrinterFeedLines.getText().toString(), 0);
            Toast.makeText(activity, "Company setting updated", Toast.LENGTH_SHORT).show();
        }

        getPrinterSettingDetails();

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
            logoUse = printerSettingResponse.getLogoUse() != null ? printerSettingResponse.getLogoUse() : "off";
            paymentUse = printerSettingResponse.getPaymentUse() != null ? printerSettingResponse.getPaymentUse() : "off";
            customerUse = printerSettingResponse.getCustomerUse() != null ? printerSettingResponse.getCustomerUse() : "off";
            productQuantityUpdate = printerSettingResponse.getProductQuantityUpdate() != null ? printerSettingResponse.getProductQuantityUpdate() : "off";
            bluetoothAddress = printerSettingResponse.getBluetoothAddress() != null ? printerSettingResponse.getBluetoothAddress() : "";
            bluetoothKOTAddress = printerSettingResponse.getBluetoothKOTAddress() != null ? printerSettingResponse.getBluetoothKOTAddress() : "";
            if (!bluetoothAddress.equalsIgnoreCase("")) {
                try {
                    new WoosimPrnMng(activity, bluetoothAddress, CompanyPrinterSetting.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!bluetoothKOTAddress.equalsIgnoreCase("")) {
                try {
                    new KOTWoosimPrnMng(activity, bluetoothAddress, CompanyPrinterSetting.this);
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

        printerList = activity.getResources().getStringArray(R.array.printer_list);
        try {
            final ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, printerList);
            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
            binding.printerSpinner.setAdapter(adapter);
            binding.KOTPrinterSpinner.setAdapter(adapter);
            if (printerName != null) {
                binding.printerSpinner.setSelection(adapter.getPosition(printerName));
            }
            if (KOTPrinterName != null) {
                binding.KOTPrinterSpinner.setSelection(adapter.getPosition(KOTPrinterName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            //bluetooth enabled and request for showing available bluetooth devices
            new WoosimPrnMng(activity, "", CompanyPrinterSetting.this);
        } else if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == RESULT_OK) {
            //bluetooth device selected and request pairing with device
            bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            new WoosimPrnMng(activity, bluetoothAddress, CompanyPrinterSetting.this);
        } else if (requestCode == REQUEST_KOT_ENABLE_BT && resultCode == RESULT_OK) {
            //bluetooth enabled and request for showing available bluetooth devices
            new KOTWoosimPrnMng(activity, "", CompanyPrinterSetting.this);
        } else if (requestCode == REQUEST_KOT_CONNECT_DEVICE && resultCode == RESULT_OK) {
            //bluetooth device selected and request pairing with device
            bluetoothKOTAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            new KOTWoosimPrnMng(activity, bluetoothAddress, CompanyPrinterSetting.this);
        }
    }


}