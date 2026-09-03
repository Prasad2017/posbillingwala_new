package com.pos_billingwala.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.pos_billingwala.Adapter.ThreePrintAdapter;
import com.pos_billingwala.Adapter.TwoPrintAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.PaymentUpiQrHelper;
import com.pos_billingwala.Extra.ShopHeaderBuilder;
import com.pos_billingwala.Extra.TabletPrintUi;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.Print.BluetoothPrinterChannel;
import com.pos_billingwala.Print.BluetoothPrintService;
import com.pos_billingwala.Print.DeviceListActivity;
import com.pos_billingwala.Print.PrintImage;
import com.pos_billingwala.Print.PrintImage.dither;
import com.pos_billingwala.Print.PrinterConnectionHelper;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivityTestInvoiceBluetoothPrintBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sample invoice preview from Printer Settings — connect printer and test print
 * without creating a real bill.
 */
@SuppressLint({"SetTextI18n", "StaticFieldLeak"})
public class TestInvoiceBluetoothPrint extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "TestInvoicePreview";

    ActivityTestInvoiceBluetoothPrintBinding binding;
    Activity activity;
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<CompanyResponse> companyResponseList = new ArrayList<>();
    List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    List<ProductCartResponse> sampleCartList = new ArrayList<>();
    ProgressDialog progressDialog;
    final ExecutorService printBitmapExecutor = Executors.newSingleThreadExecutor();
    String currency = "₹ ";
    boolean companyLogoReady = false;
    boolean paymentQrReady = false;
    int REQUEST_ENABLE_BT = 4, REQUEST_CONNECT_DEVICE = 6;
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS;

    public static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        binding = ActivityTestInvoiceBluetoothPrintBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        activity = this;
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        binding.backToSetting.setOnClickListener(this);
        binding.connectPrinter.setOnClickListener(this);
        binding.testPrint.setOnClickListener(this);

        PERMISSIONS = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        TabletPrintUi.applyLandscape(this);
        TabletPrintUi.applyCenteredForm(this, binding.previewFormContainer);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        requestPermission();
        loadSampleInvoice();
        getPrinterSettingDetails();
    }

    private void loadSampleInvoice() {
        companyResponseList = posBillingWalaDatabase.getCompanyDetails();
        printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();

        if (MainActivity.currencyName != null && !MainActivity.currencyName.trim().isEmpty()) {
            currency = MainActivity.currencyName + " ";
        } else if (!companyResponseList.isEmpty()
                && companyResponseList.get(0).getCurrencyName() != null
                && !companyResponseList.get(0).getCurrencyName().trim().isEmpty()) {
            currency = companyResponseList.get(0).getCurrencyName() + " ";
        }

        sampleCartList.clear();
        sampleCartList.add(buildSampleLine("Sample Item A", "Half", "100.00", "1"));
        sampleCartList.add(buildSampleLine("Sample Item B", "Full", "150.00", "2"));

        TwoPrintAdapter twoAdapter = new TwoPrintAdapter(activity, sampleCartList);
        binding.twoRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
        binding.twoRecyclerView.setAdapter(twoAdapter);

        ThreePrintAdapter threeAdapter = new ThreePrintAdapter(activity, sampleCartList);
        binding.threeRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
        binding.threeRecyclerView.setAdapter(threeAdapter);

        TwoPrintAdapter previewAdapter = new TwoPrintAdapter(activity, sampleCartList);
        binding.previewRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
        binding.previewRecyclerView.setAdapter(previewAdapter);

        String shopName = getString(R.string.ui_sample_shop_name);
        String shopDetails = getString(R.string.ui_sample_shop_details);
        String terms = "";
        String invoicePrefix = "POS";
        boolean logoOn = false;
        boolean qrOn = false;
        boolean customerOn = false;

        if (!companyResponseList.isEmpty()) {
            CompanyResponse company = companyResponseList.get(0);
            String resolvedName = ShopHeaderBuilder.resolveShopName1(company);
            if (!resolvedName.isEmpty()) {
                shopName = resolvedName;
            }
            String builtDetails = ShopHeaderBuilder.buildShopDetailsBlock(company);
            if (!builtDetails.isEmpty()) {
                shopDetails = builtDetails;
            }
            applyCompanyImages(company);
        } else {
            hideAllLogos();
            hideAllQr();
        }

        if (!printerSettingResponseList.isEmpty()) {
            PrinterSettingResponse setting = printerSettingResponseList.get(0);
            if (setting.getInvoicePrefix() != null && !setting.getInvoicePrefix().trim().isEmpty()) {
                invoicePrefix = setting.getInvoicePrefix().trim();
            }
            if (setting.getInvoiceTermsCondition() != null) {
                terms = setting.getInvoiceTermsCondition();
            }
            logoOn = setting.getLogoUse() != null && setting.getLogoUse().equalsIgnoreCase("on");
            qrOn = setting.getPaymentUse() != null && setting.getPaymentUse().equalsIgnoreCase("on");
            customerOn = setting.getCustomerUse() != null && setting.getCustomerUse().equalsIgnoreCase("on");
        }

        applyLogoToggle(logoOn);

        String dateStr = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US).format(new Date());
        StringBuilder invoiceDetails = new StringBuilder();
        invoiceDetails.append("Bill No: ").append(invoicePrefix).append("-TEST\n");
        invoiceDetails.append("Date: ").append(dateStr);
        if (customerOn) {
            invoiceDetails.append("\nCustomer Name: ").append(getString(R.string.ui_sample_customer_name));
            invoiceDetails.append("\nCustomer Mobile: ").append(getString(R.string.ui_sample_customer_mobile));
            invoiceDetails.append("\nCustomer Address: ").append(getString(R.string.ui_sample_customer_address));
        }
        String invoiceDetailsText = invoiceDetails.toString();

        float subTotal = 0f;
        for (ProductCartResponse line : sampleCartList) {
            float price = Float.parseFloat(line.getResolvedLinePrice());
            float qty = Float.parseFloat(line.getProductQuantity());
            subTotal += price * qty;
        }
        applyPaymentQr(qrOn, subTotal, shopName, invoicePrefix + "-TEST");
        String subTotalText = "Sub Total: " + currency + String.format(Locale.US, "%.2f", subTotal);
        String totalText = "Total: " + currency + String.format(Locale.US, "%.2f", subTotal);

        binding.previewShopName.setText(shopName);
        binding.previewShopDetails.setText(shopDetails);
        binding.previewInvoiceDetails.setText(invoiceDetailsText);
        binding.previewSubTotal.setText(subTotalText);
        binding.previewTotalAmount.setText(totalText);
        binding.previewTerms.setText(terms);

        binding.twoShopName.setText(shopName);
        binding.twoShopDetails.setText(shopDetails);
        binding.twoInvoiceDetails.setText(invoiceDetailsText);
        binding.twoSubTotal.setText(subTotalText);
        binding.twoTotalAmount.setText(totalText);
        binding.twoInvoiceTermsCondition.setText(terms);

        binding.threeShopName.setText(shopName);
        binding.threeShopDetails.setText(shopDetails);
        binding.threeInvoiceDetails.setText(invoiceDetailsText);
        binding.threeSubTotal.setText(subTotalText);
        binding.threeTotalAmount.setText(totalText);
        binding.threeInvoiceTermsCondition.setText(terms);
    }

    private void applyCompanyImages(CompanyResponse company) {
        companyLogoReady = false;

        if (company.getCompanyLogo() != null && !company.getCompanyLogo().trim().isEmpty()) {
            try {
                byte[] bytes = Base64.decode(company.getCompanyLogo(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    binding.previewCompanyLogo.setImageBitmap(bitmap);
                    binding.twoCompanyLogo.setImageBitmap(bitmap);
                    binding.threeCompanyLogo.setImageBitmap(bitmap);
                    companyLogoReady = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Logo decode failed", e);
            }
        }
    }

    /** Matches BluetoothPrint: logoUse / paymentUse control visibility. */
    private void applyLogoToggle(boolean logoOn) {
        int visibility = logoOn ? View.VISIBLE : View.GONE;
        // When ON without uploaded logo, keep default app_logo placeholder (same as bill layouts)
        binding.previewCompanyLogo.setVisibility(visibility);
        binding.twoCompanyLogo.setVisibility(visibility);
        binding.threeCompanyLogo.setVisibility(visibility);
    }

    private void applyPaymentQr(boolean qrOn, float amount, String payeeName, String note) {
        String upiId = "";
        if (!companyResponseList.isEmpty()) {
            upiId = companyResponseList.get(0).getPaymentLogo();
        }
        boolean applied = qrOn
                && PaymentUpiQrHelper.applyQrToViews(
                upiId, payeeName, amount, note,
                binding.previewQRLogo, binding.twoQRLogo, binding.threeQRLogo);
        paymentQrReady = applied;
        int visibility = applied ? View.VISIBLE : View.GONE;
        binding.previewQRLogo.setVisibility(visibility);
        binding.twoQRLogo.setVisibility(visibility);
        binding.threeQRLogo.setVisibility(visibility);
    }

    private void hideAllLogos() {
        companyLogoReady = false;
        binding.previewCompanyLogo.setVisibility(View.GONE);
        binding.twoCompanyLogo.setVisibility(View.GONE);
        binding.threeCompanyLogo.setVisibility(View.GONE);
    }

    private void hideAllQr() {
        paymentQrReady = false;
        binding.previewQRLogo.setVisibility(View.GONE);
        binding.twoQRLogo.setVisibility(View.GONE);
        binding.threeQRLogo.setVisibility(View.GONE);
    }

    private ProductCartResponse buildSampleLine(String name, String portion, String price, String qty) {
        ProductCartResponse line = new ProductCartResponse();
        line.setProductName(name);
        line.setSnapshotProductName(name);
        line.setPortionName(portion);
        line.setProductNewPrice(price);
        line.setSnapshotLinePrice(price);
        line.setProductOldPrice(price);
        line.setProductQuantity(qty);
        line.setProductCGST("");
        line.setProductSGST("");
        return line;
    }

    private String savedInvoicePrinterAddress() {
        if (printerSettingResponseList == null || printerSettingResponseList.isEmpty()) {
            return "";
        }
        String addr = printerSettingResponseList.get(0).getBluetoothAddress();
        return addr != null ? addr : "";
    }

    private void connectInvoicePrinter() {
        WoosimPrnMng.connectFromButton(activity, savedInvoicePrinterAddress(), activity);
    }

    private void getPrinterSettingDetails() {
        printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();
        if (!printerSettingResponseList.isEmpty()) {
            String bluetoothAddress = savedInvoicePrinterAddress();
            if (!bluetoothAddress.isEmpty()) {
                try {
                    new WoosimPrnMng(activity, bluetoothAddress, activity);
                } catch (Exception e) {
                    Log.e(TAG, "Auto-connect failed", e);
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            finish();
        } else if (id == R.id.connectPrinter) {
            connectInvoicePrinter();
        } else if (id == R.id.testPrint) {
            runTestPrint();
        }
    }

    private void runTestPrint() {
        if (printerSettingResponseList == null || printerSettingResponseList.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_please_select_printer_from_setting), Toast.LENGTH_SHORT).show();
            return;
        }

        PrinterConnectionHelper.ensureBillPrinterAsync(activity, savedInvoicePrinterAddress(),
                this::runTestPrintAfterPrinterReady);
    }

    private void runTestPrintAfterPrinterReady() {
        if (isFinishing() || printerSettingResponseList == null || printerSettingResponseList.isEmpty()) {
            return;
        }

        progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage(getString(R.string.toast_printing_in_progress));
        progressDialog.setCancelable(false);
        showDialog();

        try {
            String printerName = printerSettingResponseList.get(0).getPrinterName();
            if (printerName != null && printerName.equalsIgnoreCase("3-Inch")) {
                Bitmap bitmap = convertLayout(binding.threeNestedScrollView, 72);
                if (bitmap != null) {
                    printImage(bitmap, 72);
                } else {
                    hideDialog();
                    Toast.makeText(activity, getString(R.string.toast_print_layout_failed), Toast.LENGTH_SHORT).show();
                }
            } else {
                Bitmap bitmap = convertLayout(binding.twoNestedScrollView, 48);
                if (bitmap != null) {
                    printImage(bitmap, 48);
                } else {
                    hideDialog();
                    Toast.makeText(activity, getString(R.string.toast_print_layout_failed), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Test print failed", e);
            hideDialog();
            Toast.makeText(activity, getString(R.string.toast_print_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void printImage(Bitmap image, int effectivePrintWidth) {
        printBitmapExecutor.execute(() -> {
            String toastMsg = null;
            try {
                PrintImage printImage = new PrintImage(getResizedBitmap(image, effectivePrintWidth));
                printImage.PrepareImage(dither.floyd_steinberg, 128);
                if (!PrinterConnectionHelper.safeWriteBill(activity, printImage.getPrintImageData())) {
                    toastMsg = getString(R.string.toast_printer_offline_connect);
                    runOnUiThread(() -> {
                        try {
                            WoosimPrnMng.connect(activity, savedInvoicePrinterAddress(), activity);
                        } catch (Exception e) {
                            Log.e(TAG, "Connect prompt failed", e);
                        }
                    });
                } else {
                    String feed = printerSettingResponseList.get(0).getPrinterFeedLines();
                    checkAndFeedPaper(feed == null || feed.trim().isEmpty() ? "1" : feed);
                    toastMsg = getString(R.string.toast_test_print_sent);
                }
            } catch (Exception e) {
                Log.e(TAG, "printImage failed", e);
                toastMsg = getString(R.string.toast_print_failed);
            } finally {
                final String msg = toastMsg;
                runOnUiThread(() -> {
                    if (msg != null) {
                        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                    }
                    hideDialog();
                });
            }
        });
    }

    private void checkAndFeedPaper(String lines) {
        try {
            if (lines == null || lines.trim().isEmpty()) {
                return;
            }
            int count = Integer.parseInt(lines.trim());
            StringBuilder lineBreaks = new StringBuilder();
            for (int i = 0; i < count; i++) {
                lineBreaks.append("\n");
            }
            PrinterConnectionHelper.safeWriteBill(activity, lineBreaks.toString().getBytes());
        } catch (Exception e) {
            Log.e(TAG, "checkAndFeedPaper failed", e);
        }
    }

    private Bitmap convertLayout(NestedScrollView nestedScrollView, int effectivePrintWidth) {
        nestedScrollView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        nestedScrollView.layout(0, 0, nestedScrollView.getMeasuredWidth(), nestedScrollView.getMeasuredHeight());

        if (nestedScrollView.getWidth() <= 0 || nestedScrollView.getHeight() <= 0) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(nestedScrollView.getWidth(), nestedScrollView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable background = nestedScrollView.getBackground();
        if (background != null) {
            background.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
        }
        nestedScrollView.draw(canvas);
        return bitmap;
    }

    private Bitmap getResizedBitmap(Bitmap bm, int effectivePrintWidth) {
        int reqWidth = Math.round(effectivePrintWidth * 8);
        int width = bm.getWidth();
        int height = bm.getHeight();
        if (width == reqWidth) {
            return bm;
        } else if (width < reqWidth && width > 16) {
            int diff = width % 8;
            if (diff != 0) {
                int newWidth = width - diff;
                int newHeight = (width - diff) * height / width;
                float scaleWidth = ((float) newWidth) / width;
                float scaleHeight = ((float) newHeight) / height;
                Matrix matrix = new Matrix();
                matrix.postScale(scaleWidth, scaleHeight);
                Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
                bm.recycle();
                return resizedBitmap;
            }
        } else if (width > 16) {
            int newWidth = reqWidth;
            int newHeight = reqWidth * height / width;
            float scaleWidth = ((float) newWidth) / width;
            float scaleHeight = ((float) newHeight) / height;
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
            Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
            bm.recycle();
            return resizedBitmap;
        }
        return bm;
    }

    private void showDialog() {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    private void requestPermission() {
        Dexter.withContext(activity)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            connectInvoicePrinter();
        } else if (requestCode == REQUEST_CONNECT_DEVICE) {
            if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                String bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                if (bluetoothAddress != null) {
                    if (!printerSettingResponseList.isEmpty()) {
                        printerSettingResponseList.get(0).setBluetoothAddress(bluetoothAddress);
                    }
                    PrinterConnectionHelper.onBillDevicePicked(activity, bluetoothAddress);
                } else {
                    PrinterConnectionHelper.cancelPendingDevicePick(true);
                }
            } else {
                PrinterConnectionHelper.cancelPendingDevicePick(true);
            }
        }
    }

    @Override
    protected void onDestroy() {
        printBitmapExecutor.shutdownNow();
        super.onDestroy();
    }
}
