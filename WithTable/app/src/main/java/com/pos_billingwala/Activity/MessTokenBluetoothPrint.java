package com.pos_billingwala.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ShopHeaderBuilder;
import com.pos_billingwala.Extra.MessTokenQrHelper;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Print.BluetoothPrintService;
import com.pos_billingwala.Print.DeviceListActivity;
import com.pos_billingwala.Print.PrintImage;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivityMessTokenBluetoothPrintBinding;

import java.util.ArrayList;
import java.util.List;

@SuppressLint({"SetTextI18n", "StaticFieldLeak"})
public class MessTokenBluetoothPrint extends BaseActivity implements View.OnClickListener {

    public static TextView twoShopName, twoShopDetails, twoInvoiceDetails, twoInvoiceMemberName, twoTokenTypeLabel, twoTokenCode;
    public static ImageView twoCompanyLogo, twoQrCode;
    public static NestedScrollView twoNestedScrollView;

    private ActivityMessTokenBluetoothPrintBinding binding;
    private POSBillingWalaDatabase posBillingWalaDatabase;
    private List<CompanyResponse> companyResponseList = new ArrayList<>();
    private List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    private ProgressDialog progressDialog;

    private String tokenCode, memberId, memberName, memberMobile, memberType, messType, tokenAmount, tokenDate, tokenNetworkStatus;
    private boolean tokenSaved = false;

    int PERMISSION_ALL = 1;
    int REQUEST_ENABLE_BT = 4, REQUEST_CONNECT_DEVICE = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMessTokenBluetoothPrintBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        posBillingWalaDatabase = new POSBillingWalaDatabase(this);
        initViews();
        readIntentExtras();
        requestPermission();

        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_ALL);
        }
    }

    private void readIntentExtras() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        tokenCode = intent.getStringExtra("tokenCode");
        memberId = intent.getStringExtra("memberId");
        memberName = intent.getStringExtra("memberName");
        memberMobile = intent.getStringExtra("memberMobile");
        memberType = intent.getStringExtra("memberType");
        messType = intent.getStringExtra("messType");
        tokenAmount = intent.getStringExtra("tokenAmount");
        tokenDate = intent.getStringExtra("tokenDate");
        tokenNetworkStatus = intent.getStringExtra("tokenNetworkStatus");
    }

    private void initViews() {
        twoCompanyLogo = findViewById(R.id.twoCompanyLogo);
        twoShopName = findViewById(R.id.twoShopName);
        twoShopDetails = findViewById(R.id.twoShopDetails);
        twoInvoiceDetails = findViewById(R.id.twoInvoiceDetails);
        twoInvoiceMemberName = findViewById(R.id.twoInvoiceMemberName);
        twoTokenTypeLabel = findViewById(R.id.twoTokenTypeLabel);
        twoQrCode = findViewById(R.id.twoQrCode);
        twoTokenCode = findViewById(R.id.twoTokenCode);
        twoNestedScrollView = findViewById(R.id.twoNestedScrollView);
        binding.printInvoiceCardView.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        getCompanyDetails();
        getPrinterSettingDetails();
        renderTokenPreview();
    }

    private void renderTokenPreview() {
        if (tokenCode == null || memberName == null) {
            return;
        }

        twoInvoiceMemberName.setText(memberName);
        twoInvoiceDetails.setText(messType + "\n" + tokenDate);
        twoTokenTypeLabel.setText("MESS QR TOKEN");
        twoTokenCode.setText("Token: " + tokenCode.substring(0, Math.min(8, tokenCode.length())).toUpperCase());

        String payload = MessTokenQrHelper.buildPayload(tokenCode, MainActivity.userId, MessTokenQrHelper.MEMBER_TYPE_MEMBER);
        Bitmap qrBitmap = MessTokenQrHelper.generateQrBitmap(payload, 512);
        if (qrBitmap != null) {
            twoQrCode.setImageBitmap(qrBitmap);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.printInvoiceCardView) {
            if (printerSettingResponseList.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_please_select_printer_from_setting), Toast.LENGTH_SHORT).show();
                return;
            }
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.toast_printing_in_progress));
            print2InchBill();
        }
    }

    private void print2InchBill() {
        showDialog();
        Bitmap bitmap = convertLayout(twoNestedScrollView);
        if (bitmap != null) {
            printImage(bitmap, 48);
        }
        hideDialog();
    }

    private void printImage(Bitmap image, int effectivePrintWidth) {
        if (WoosimPrnMng.isPrinterConnected(getApplicationContext(), MessTokenBluetoothPrint.this)) {
            BluetoothPrintService mService = WoosimPrnMng.getServiceInstance();
            PrintImage printImage = new PrintImage(getResizedBitmap(image, effectivePrintWidth));
            printImage.PrepareImage(com.pos_billingwala.Print.PrintImage.dither.floyd_steinberg, 128);
            mService.write(printImage.getPrintImageData());
            checkAndFeedPaper(printerSettingResponseList.get(0).getKotPrinterFeedLines());
            saveMessTokenIfNeeded();
        } else {
            new WoosimPrnMng(this, "", MessTokenBluetoothPrint.this);
        }
    }

    private void saveMessTokenIfNeeded() {
        if (tokenSaved) {
            finish();
            return;
        }

        posBillingWalaDatabase.saveMessToken(
                tokenCode,
                memberId != null ? memberId : "",
                memberName,
                memberMobile != null ? memberMobile : "",
                MessTokenQrHelper.MEMBER_TYPE_MEMBER,
                messType,
                tokenAmount != null ? tokenAmount : "0",
                tokenDate,
                tokenNetworkStatus,
                MessTokenQrHelper.TOKEN_STATE_ACTIVE,
                0,
                0
        );

        // Same daily meal slot as paper coupon — keeps One Time / Two Time limits intact
        posBillingWalaDatabase.saveMessInvoice(
                memberId != null ? memberId : "",
                memberName,
                messType,
                tokenDate,
                tokenNetworkStatus,
                0
        );

        tokenSaved = true;
        Toast.makeText(this, getString(R.string.toast_member_qr_token_printed), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void getCompanyDetails() {
        companyResponseList = posBillingWalaDatabase.getCompanyDetails();
        if (companyResponseList.isEmpty()) {
            return;
        }
        CompanyResponse company = companyResponseList.get(0);
        twoShopName.setText(ShopHeaderBuilder.resolveShopName1(company));
        twoShopDetails.setText(ShopHeaderBuilder.buildShopDetailsBlock(company, true, true, false, false));

        if (company.getCompanyLogo() != null) {
            byte[] bytes = Base64.decode(company.getCompanyLogo(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            twoCompanyLogo.setImageBitmap(bitmap);
            twoCompanyLogo.setVisibility(View.VISIBLE);
        } else {
            twoCompanyLogo.setVisibility(View.GONE);
        }
    }

    private void getPrinterSettingDetails() {
        printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();
        if (printerSettingResponseList.isEmpty()) {
            return;
        }
        String bluetoothAddress = printerSettingResponseList.get(0).getBluetoothAddress();
        if (!bluetoothAddress.equalsIgnoreCase("")) {
            try {
                new WoosimPrnMng(this, bluetoothAddress, MessTokenBluetoothPrint.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
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

    private Bitmap convertLayout(NestedScrollView nestedScrollView) {
        nestedScrollView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        nestedScrollView.layout(0, 0, nestedScrollView.getMeasuredWidth(), nestedScrollView.getMeasuredHeight());
        nestedScrollView.setDrawingCacheEnabled(true);
        nestedScrollView.buildDrawingCache();

        Bitmap bitmap = Bitmap.createBitmap(nestedScrollView.getWidth(),
                nestedScrollView.getHeight(), Bitmap.Config.ARGB_8888);
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
        }
        int newWidth = reqWidth;
        int newHeight = reqWidth * height / width;
        Matrix matrix = new Matrix();
        matrix.postScale((float) newWidth / width, (float) newHeight / height);
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    }

    private void checkAndFeedPaper(String lines) {
        if (WoosimPrnMng.isPrinterConnected(this, MessTokenBluetoothPrint.this)) {
            BluetoothPrintService mService = WoosimPrnMng.getServiceInstance();
            StringBuilder lineBreaks = new StringBuilder();
            for (int i = 0; i < Integer.parseInt(lines); i++) {
                lineBreaks.append("\n");
            }
            mService.write(lineBreaks.toString().getBytes());
        } else {
            new WoosimPrnMng(this, "", MessTokenBluetoothPrint.this);
        }
    }

    private void hideDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showDialog() {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            new WoosimPrnMng(this, "", MessTokenBluetoothPrint.this);
        } else if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == RESULT_OK && data != null) {
            String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            new WoosimPrnMng(this, address, MessTokenBluetoothPrint.this);
        }
    }

    public void requestPermission() {
        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT
                ).withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }
}
