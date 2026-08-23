package com.pos_billingwala.Activity;

import static com.pos_billingwala.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
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
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.MessInvoiceResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Print.BluetoothPrintService;
import com.pos_billingwala.Print.DeviceListActivity;
import com.pos_billingwala.Print.PrintImage;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivityCouponBluetoothPrintBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;


@SuppressLint({"Range", "SetTextI18n, NewApi, StaticFieldLeak"})
public class CouponBluetoothPrint extends BaseActivity implements View.OnClickListener {

    public static TextView shopName, shopDetails, invoiceDetails, invoiceMemberName, couponCount;
    public static ImageView companyLogo;
    public static TextView twoShopName, twoShopDetails, twoInvoiceDetails, twoInvoiceMemberName, twoCouponCount;
    public static ImageView twoCompanyLogo;
    public static TextView threeShopName, threeShopDetails, threeInvoiceDetails, threeInvoiceMemberName, threeCouponCount;
    public static ImageView threeCompanyLogo;
    public static NestedScrollView nestedScrollView, twoNestedScrollView, threeNestedScrollView;
    public static Activity activity;
    public static List<CompanyResponse> companyResponseList = new ArrayList<>();
    public static List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    public static List<MessInvoiceResponse> messInvoiceResponseList = new ArrayList<>();
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static String inr, cartOrderStatus, invoiceRunningStatus, memberId, memberName, memberMobileNumber, messType, messDays, messInvoiceResponseListSize;
    ProgressDialog progressDialog;
    //********************* Bluetooth Printer Start ************************//
    int PERMISSION_ALL = 1;
    int REQUEST_ENABLE_BT = 4, REQUEST_CONNECT_DEVICE = 6;
    //******************** Bluetooth Printer End ************************//
    ActivityCouponBluetoothPrintBinding binding;

    public static void createFolder() {

        File myDirectory = new File(directory_path);
        if (!myDirectory.exists()) {
            myDirectory.mkdirs();
        }

    }

    public static boolean hasPermissions(Context context, String... permissions) {
        // Get current android os version.
        int currentAndroidVersion = Build.VERSION.SDK_INT;
        // Build.VERSION_CODES.M's value is 23.
        if (currentAndroidVersion >= Build.VERSION_CODES.M) {
            if (context != null && permissions != null) {
                for (String permission : permissions) {
                    if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        createFolder();
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void setScreenSizeSmall() {
        Configuration configuration = getResources().getConfiguration();
        configuration.fontScale = (float) 1; //0.85 small size, 1 normal size, 1,15 big etc
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        configuration.densityDpi = (int) getResources().getDisplayMetrics().xdpi;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCouponBluetoothPrintBinding.inflate(getLayoutInflater());
        View view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here
        setContentView(view); //view is set by view binding

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setScreenSizeSmall();

        activity = CouponBluetoothPrint.this;
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        initViews();

        try {

            Intent intent = getIntent();
            if (intent != null) {
                invoiceRunningStatus = intent.getStringExtra("invoiceRunningStatus");
                cartOrderStatus = intent.getStringExtra("cartOrderStatus");
                memberId = intent.getStringExtra("memberId");
                memberName = intent.getStringExtra("memberName");
                memberMobileNumber = intent.getStringExtra("memberMobileNumber");
                messDays = intent.getStringExtra("messDays");
                messInvoiceResponseListSize = intent.getStringExtra("messInvoiceResponseList");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        //Add runtime permissions
        String[] PERMISSIONS = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

    }

    public void initViews() {

        inr = MainActivity.currencyName + " ";
        //***************** Print Start ******************//
        companyLogo = findViewById(R.id.companyLogo);
        shopName = findViewById(R.id.shopName);
        shopDetails = findViewById(R.id.shopDetails);
        invoiceDetails = findViewById(R.id.invoiceDetails);
        invoiceMemberName = findViewById(R.id.invoiceMemberName);
        nestedScrollView = findViewById(R.id.nestedScrollView);
        couponCount = findViewById(R.id.couponCount);
        //***************** Print End ******************//

        //***************** 2 Inch Printer Start ******************//
        twoCompanyLogo = findViewById(R.id.twoCompanyLogo);
        twoShopName = findViewById(R.id.twoShopName);
        twoShopDetails = findViewById(R.id.twoShopDetails);
        twoInvoiceDetails = findViewById(R.id.twoInvoiceDetails);
        twoInvoiceMemberName = findViewById(R.id.twoInvoiceMemberName);
        twoNestedScrollView = findViewById(R.id.twoNestedScrollView);
        twoCouponCount = findViewById(R.id.twoCouponCount);
        //***************** 2 Inch Printer End ******************//

        //***************** 3 Inch Printer Start ******************//
        threeCompanyLogo = findViewById(R.id.threeCompanyLogo);
        threeShopName = findViewById(R.id.threeShopName);
        threeShopDetails = findViewById(R.id.threeShopDetails);
        threeInvoiceMemberName = findViewById(R.id.threeInvoiceMemberName);
        threeInvoiceDetails = findViewById(R.id.threeInvoiceDetails);
        threeNestedScrollView = findViewById(R.id.threeNestedScrollView);
        threeCouponCount = findViewById(R.id.threeCouponCount);
        //***************** 3 Inch Printer End ******************//

        binding.printInvoiceCardView.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.printInvoiceCardView) {
            if (!printerSettingResponseList.isEmpty()) {

                progressDialog = new ProgressDialog(activity);
                progressDialog.setMessage(getString(R.string.toast_printing_in_progress));

                if (printerSettingResponseList.get(0).getPrinterName().equalsIgnoreCase("2-Inch")) {
                    print2InchBill();
                } else if (printerSettingResponseList.get(0).getPrinterName().equalsIgnoreCase("3-Inch")) {
                    print3InchBill();
                }

            } else {
                Toast.makeText(activity, getString(R.string.toast_please_select_printer_from_setting), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void print2InchBill() {

        showDialog();

        Bitmap bitmap = convertLayout(twoNestedScrollView);
        if (bitmap != null) {
            printImage(bitmap, 48);
        }

        hideDialog();

    }

    public void print3InchBill() {

        showDialog();

        Bitmap bitmap = convertLayout(threeNestedScrollView);
        if (bitmap != null) {
            printImage(bitmap, 72);
        }

        hideDialog();

    }

    protected void printImage(Bitmap image, int effectivePrintWidth) {

        if (WoosimPrnMng.isPrinterConnected(getApplicationContext(), CouponBluetoothPrint.this)) {

            BluetoothPrintService mService = null;
            mService = WoosimPrnMng.getServiceInstance();
            PrintImage PrintImage = new PrintImage(getResizedBitmap(image, effectivePrintWidth));
            PrintImage.PrepareImage(com.pos_billingwala.Print.PrintImage.dither.floyd_steinberg, 128);
            mService.write(PrintImage.getPrintImageData());

            checkAndFeedPaper(printerSettingResponseList.get(0).getKotPrinterFeedLines());

            saveMessInvoice();

        } else {
            //Printer not connected and send request for connecting printer
            new WoosimPrnMng(activity, "", CouponBluetoothPrint.this);
        }

    }

    public void saveMessInvoice() {

        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String messInvoiceDate = df.format(c);

        posBillingWalaDatabase.saveMessInvoice(memberId, memberName, messType, messInvoiceDate, getRandomString(10), 0);

        Toast.makeText(this, messInvoiceDate, Toast.LENGTH_SHORT).show();

        onCallBack();

    }

    public String getRandomString(final int sizeOfRandomString) {

        String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";

        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    public Bitmap convertLayout(NestedScrollView nestedScrollView) {

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
        nestedScrollView.buildDrawingCache();

        return bitmap;

    }

    public Bitmap getResizedBitmap(Bitmap bm, int effectivePrintWidth) {
        int newWidth = 248;
        int newHeight = 297;
        int reqWidth = Math.round(effectivePrintWidth * 8);
        int width = bm.getWidth();
        int height = bm.getHeight();
        if (width == reqWidth) {
            return bm;
        } else if (width < reqWidth && width > 16) {
            int diff = width % 8;
            if (diff != 0) {
                newWidth = width - diff;
                newHeight = (width - diff) * height / width;
                float scaleWidth = ((float) newWidth) / width;
                float scaleHeight = ((float) newHeight) / height;
                // CREATE A MATRIX FOR THE MANIPULATION
                Matrix matrix = new Matrix();
                // RESIZE THE BIT MAP
                matrix.postScale(scaleWidth, scaleHeight);
                // "RECREATE" THE NEW BITMAP
                Bitmap resizedBitmap = Bitmap.createBitmap(
                        bm, 0, 0, width, height, matrix, false);
                bm.recycle();
                return resizedBitmap;
            }
        } else if (width > 16) {
            newWidth = reqWidth;
            newHeight = reqWidth * height / width;
            float scaleWidth = ((float) newWidth) / width;
            float scaleHeight = ((float) newHeight) / height;
            // CREATE A MATRIX FOR THE MANIPULATION
            Matrix matrix = new Matrix();
            // RESIZE THE BIT MAP
            matrix.postScale(scaleWidth, scaleHeight);

            // "RECREATE" THE NEW BITMAP
            Bitmap resizedBitmap = Bitmap.createBitmap(
                    bm, 0, 0, width, height, matrix, false);
            bm.recycle();
            return resizedBitmap;
        }
        return bm;
    }

    public void checkAndFeedPaper(String lines) {

        if (WoosimPrnMng.isPrinterConnected(activity, CouponBluetoothPrint.this)) {
            BluetoothPrintService mService = null;
            mService = WoosimPrnMng.getServiceInstance();
            StringBuilder lineBreaks = new StringBuilder();
            for (int i = 0; i < Integer.parseInt(lines); i++) {
                lineBreaks.append("\n"); // Add a newline character for each extra line
            }
            // Convert to bytes and send to the printer
            byte[] lineBreakBytes = lineBreaks.toString().getBytes();
            mService.write(lineBreakBytes);
        } else {
            //Printer not connected and send request for connecting printer
            new WoosimPrnMng(activity, "", CouponBluetoothPrint.this);
        }

    }

    public void hideDialog() {
        if (null != progressDialog && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public void showDialog() {
        if (null != progressDialog && (!progressDialog.isShowing())) {
            progressDialog.show();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        requestPermission();

        getCompanyDetails();
        getPrinterSettingDetails();
        getInvoiceDetails();

    }

    public void getCompanyDetails() {

        companyResponseList = posBillingWalaDatabase.getCompanyDetails();

        if (!companyResponseList.isEmpty()) {

            shopName.setText(companyResponseList.get(0).getCompanyName());
            twoShopName.setText(companyResponseList.get(0).getCompanyName());
            threeShopName.setText(companyResponseList.get(0).getCompanyName());

            String shopDetail = "";
            if (companyResponseList.get(0).getGstStatus() != null) {
                if (companyResponseList.get(0).getGstStatus().equalsIgnoreCase("on")) {
                    shopDetail = companyResponseList.get(0).getCompanyAddress() + "\nPH:" + companyResponseList.get(0).getCompanyMobile() + "GSTIN: " + companyResponseList.get(0).getGstNumber();

                } else if (companyResponseList.get(0).getGstStatus().equalsIgnoreCase("off")) {
                    shopDetail = companyResponseList.get(0).getCompanyAddress() + "\nPH:" + companyResponseList.get(0).getCompanyMobile();
                }
            } else {
                shopDetail = companyResponseList.get(0).getCompanyAddress() + "\nPH:" + companyResponseList.get(0).getCompanyMobile();
            }

            shopDetails.setText(shopDetail);
            twoShopDetails.setText(shopDetail);
            threeShopDetails.setText(shopDetail);

            Date c = Calendar.getInstance().getTime();
            System.out.println("Current time => " + c);
            SimpleDateFormat df = new SimpleDateFormat("dd MMM, yyyy hh:mm aa", Locale.getDefault());
            String paymentDate = df.format(c);

            Calendar datetime = Calendar.getInstance();
            int hourOfDay = datetime.get(Calendar.HOUR_OF_DAY);
            int minutes = datetime.get(Calendar.MINUTE);
            if (hourOfDay >= 18 && minutes > 0) {
                messType = "Dinner";
                invoiceDetails.setText(messType + "\n" + paymentDate);
                twoInvoiceDetails.setText(messType + "\n" + paymentDate);
                threeInvoiceDetails.setText(messType + "\n" + paymentDate);
            } else if (hourOfDay >= 7 && hourOfDay < 18 && minutes > 0) {
                if (messInvoiceResponseListSize.equalsIgnoreCase("1")) {
                    messType = "Dinner";
                } else {
                    messType = "Lunch";
                }
                invoiceDetails.setText(messType + "\n" + paymentDate);
                twoInvoiceDetails.setText(messType + "\n" + paymentDate);
                threeInvoiceDetails.setText(messType + "\n" + paymentDate);
            } else {
                messType = "";
                invoiceDetails.setText(paymentDate);
                twoInvoiceDetails.setText(paymentDate);
                threeInvoiceDetails.setText(paymentDate);
            }

            if (companyResponseList.get(0).getCompanyLogo() != null) {

                String companyLogoImage = companyResponseList.get(0).getCompanyLogo();
                // decode base64 string
                byte[] bytes = Base64.decode(companyLogoImage, Base64.DEFAULT);
                // Initialize bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                companyLogo.setImageBitmap(bitmap);
                twoCompanyLogo.setImageBitmap(bitmap);
                threeCompanyLogo.setImageBitmap(bitmap);

                companyLogo.setVisibility(View.VISIBLE);
                twoCompanyLogo.setVisibility(View.VISIBLE);
                threeCompanyLogo.setVisibility(View.VISIBLE);

            } else {
                companyLogo.setVisibility(View.GONE);
                twoCompanyLogo.setVisibility(View.GONE);
                threeCompanyLogo.setVisibility(View.GONE);
            }

        }
    }

    public void getPrinterSettingDetails() {
        printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();
        String bluetoothAddress = printerSettingResponseList.get(0).getBluetoothAddress();
        if (!bluetoothAddress.equalsIgnoreCase("")) {
            try {
                new WoosimPrnMng(activity, bluetoothAddress, CouponBluetoothPrint.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void getInvoiceDetails() {

        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);
        SimpleDateFormat dfInvoice = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String paymentInvoiceDate = dfInvoice.format(c);

        messInvoiceResponseList = posBillingWalaDatabase.gerMessInvoiceUserWiseList(memberName, paymentInvoiceDate);
        invoiceMemberName.setText(memberName);
        twoInvoiceMemberName.setText(memberName);
        threeInvoiceMemberName.setText(memberName);

        int couponNumber = messInvoiceResponseList.size() + 1;
        couponCount.setText("MESS COUPON No: " + couponNumber);
        twoInvoiceDetails.setText("MESS COUPON No: " + couponNumber);
        threeInvoiceDetails.setText("MESS COUPON No: " + couponNumber);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            //bluetooth enabled and request for showing available bluetooth devices
            new WoosimPrnMng(activity, "", CouponBluetoothPrint.this);
        } else if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == RESULT_OK) {
            //bluetooth device selected and request pairing with device
            String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            new WoosimPrnMng(activity, address, CouponBluetoothPrint.this);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onCallBack();
    }

    public void onCallBack() {

        Intent intent = new Intent(CouponBluetoothPrint.this, MainActivity.class);
        intent.putExtra("invoiceRunningStatus", "mess");
        intent.putExtra("cartOrderStatus", cartOrderStatus);
        startActivity(intent);
        finish();

    }

    public void requestPermission() {

        Dexter.withContext(activity)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT
                ).withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {

                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

    }


}