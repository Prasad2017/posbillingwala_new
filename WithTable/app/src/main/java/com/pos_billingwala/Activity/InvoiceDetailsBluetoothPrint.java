package com.pos_billingwala.Activity;

import static com.pos_billingwala.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Adapter.InvoiceProductAdapter;
import com.pos_billingwala.Adapter.ThreeInvoicePrintAdapter;
import com.pos_billingwala.Adapter.TwoInvoicePrintAdapter;
import com.pos_billingwala.BuildConfig;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.ShopHeaderBuilder;
import com.pos_billingwala.NetworkToOffline.InvoicePendingSync;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Print.BluetoothPrintService;
import com.pos_billingwala.Print.DeviceListActivity;
import com.pos_billingwala.Print.KOTWoosimPrnMng;
import com.pos_billingwala.Print.PrintImage;
import com.pos_billingwala.Print.PrintImage.dither;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivityInvoiceDetailsBluetoothPrintBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@SuppressLint("SetTextI18n, StaticFieldLeak")
public class InvoiceDetailsBluetoothPrint extends BaseActivity implements View.OnClickListener {


    public static List<InvoiceProductResponse> invoiceProductResponseList = new ArrayList<>();
    public static List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
    public static List<CompanyResponse> companyResponseList = new ArrayList<>();
    public static List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    public static InvoiceProductAdapter adapter;
    public static String invoiceId;
    public static TextView invoiceShopName, invoiceShopDetails, invoiceInvoiceDetails, invoiceSubTotal, invoiceShopCGST, invoiceCGST,
            invoiceShopSGST, invoiceSGST, invoiceDiscount, invoiceTotalAmount;
    public static LinearLayout invoiceShopCGSTLayout, invoiceShopSGSTLayout;
    public static ImageView invoiceCompanyLogo;
    public static RecyclerView invoiceRecyclerView;
    public static NestedScrollView invoiceNestedScrollView;

    public static TextView twoShopName, twoShopDetails, twoInvoiceDetails, twoShopPrintStatus, twoSubTotal, twoShopCGST, twoCGST, twoShopSGST, twoSGST, twoDiscount, twoTotalAmount, twoInvoiceTermsCondition;
    public static ImageView twoCompanyLogo, twoQRLogo;
    public static TextView threeShopName, threeShopDetails, threeInvoiceDetails, threeShopPrintStatus, threeSubTotal, threeShopCGST, threeCGST, threeShopSGST, threeSGST, threeDiscount, threeTotalAmount, threeInvoiceTermsCondition;
    public static ImageView threeCompanyLogo, threeQRLogo;
    public static LinearLayout twoShopCGSTLayout, twoShopSGSTLayout, twoDiscountLayout;
    public static RecyclerView twoRecyclerView;
    public static NestedScrollView twoNestedScrollView;
    public static LinearLayout threeShopCGSTLayout, threeShopSGSTLayout, threeDiscountLayout;
    public static RecyclerView threeRecyclerView;
    public static NestedScrollView threeNestedScrollView;

    public static Activity activity;
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    ProgressDialog progressDialog;
    //********************* Bluetooth Printer Start ************************//
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS;
    String bluetoothAddress;
    int REQUEST_ENABLE_BT = 4, REQUEST_CONNECT_DEVICE = 6;
    int REQUEST_KOT_ENABLE_BT = 8, REQUEST_KOT_CONNECT_DEVICE = 10;
    //******************** Bluetooth Printer End ************************//
    ActivityInvoiceDetailsBluetoothPrintBinding binding;


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

    @NonNull
    public static String getBillDetails() {
        String BillDetails = "";
        String invoiceType = invoiceResponseList.get(0).getInvoiceType();
        if (invoiceType != null && invoiceType.equalsIgnoreCase("table_wise")) {
            BillDetails = "<b>Bill No:</b> " + invoiceResponseList.get(0).getInvoiceNumber() + "<br/><b>Date:</b> " + invoiceResponseList.get(0).getInvoiceDate() + "<br/><b>Table No:</b> " + invoiceResponseList.get(0).getNoOfTable();
        } else {
            BillDetails = "<b>Bill No:</b> " + invoiceResponseList.get(0).getInvoiceNumber() + "<br/><b>Date:</b> " + invoiceResponseList.get(0).getInvoiceDate();
        }

        String customerName = invoiceResponseList.get(0).getCustomerName();
        if (customerName != null && !customerName.equalsIgnoreCase("")) {
            BillDetails = BillDetails + "<br/><b>Customer Name:</b> " + (invoiceResponseList.get(0).getCustomerName() != null ? invoiceResponseList.get(0).getCustomerName() : "NA") +
                    "<br/><b>Customer Mobile:</b> " + (invoiceResponseList.get(0).getCustomerMobile() != null ? invoiceResponseList.get(0).getCustomerMobile() : "NA") +
                    "<br/><b>Customer Address:</b> " + (invoiceResponseList.get(0).getCustomerAddress() != null ? invoiceResponseList.get(0).getCustomerAddress() : "NA");
        }
        if (invoiceResponseList.get(0).isRefunded()) {
            BillDetails = BillDetails + "<br/><b>Status:</b> Refunded";
        }
        return String.valueOf(Html.fromHtml(BillDetails));
    }

    public static String getShopDetails() {
        if (companyResponseList == null || companyResponseList.isEmpty()) {
            return "";
        }
        return ShopHeaderBuilder.buildShopDetailsBlock(companyResponseList.get(0));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInvoiceDetailsBluetoothPrintBinding.inflate(getLayoutInflater());
        View view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here
        setContentView(view); //view is set by view binding

        Intent intent = getIntent();
        if (intent != null) {
            invoiceId = intent.getStringExtra("invoiceId");
        }


        activity = InvoiceDetailsBluetoothPrint.this;
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        initViews();

        PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

    }

    public void initViews() {

        invoiceCompanyLogo = findViewById(R.id.invoiceCompanyLogo);
        invoiceShopName = findViewById(R.id.invoiceShopName);
        invoiceShopDetails = findViewById(R.id.invoiceShopDetails);
        invoiceInvoiceDetails = findViewById(R.id.invoiceInvoiceDetails);
        invoiceSubTotal = findViewById(R.id.invoiceSubTotal);
        invoiceShopCGST = findViewById(R.id.invoiceShopCGST);
        invoiceCGST = findViewById(R.id.invoiceCGST);
        invoiceShopSGST = findViewById(R.id.invoiceShopSGST);
        invoiceSGST = findViewById(R.id.invoiceSGST);
        invoiceDiscount = findViewById(R.id.invoiceDiscount);
        invoiceTotalAmount = findViewById(R.id.invoiceTotalAmount);
        invoiceShopCGSTLayout = findViewById(R.id.invoiceShopCGSTLayout);
        invoiceShopSGSTLayout = findViewById(R.id.invoiceShopSGSTLayout);
        invoiceRecyclerView = findViewById(R.id.invoiceRecyclerView);
        invoiceNestedScrollView = findViewById(R.id.invoiceNestedScrollView);

        //***************** 2 Inch Printer Start ******************//
        twoCompanyLogo = findViewById(R.id.twoCompanyLogo);
        twoQRLogo = findViewById(R.id.twoQRLogo);
        twoShopName = findViewById(R.id.twoShopName);
        twoShopDetails = findViewById(R.id.twoShopDetails);
        twoInvoiceDetails = findViewById(R.id.twoInvoiceDetails);
        twoShopPrintStatus = findViewById(R.id.twoShopPrintStatus);
        twoSubTotal = findViewById(R.id.twoSubTotal);
        twoShopCGST = findViewById(R.id.twoShopCGST);
        twoCGST = findViewById(R.id.twoCGST);
        twoShopSGST = findViewById(R.id.twoShopSGST);
        twoSGST = findViewById(R.id.twoSGST);
        twoDiscount = findViewById(R.id.twoDiscount);
        twoTotalAmount = findViewById(R.id.twoTotalAmount);
        twoShopCGSTLayout = findViewById(R.id.twoShopCGSTLayout);
        twoShopSGSTLayout = findViewById(R.id.twoShopSGSTLayout);
        twoDiscountLayout = findViewById(R.id.twoDiscountLayout);
        twoRecyclerView = findViewById(R.id.twoRecyclerView);
        twoNestedScrollView = findViewById(R.id.twoNestedScrollView);
        twoInvoiceTermsCondition = findViewById(R.id.twoInvoiceTermsCondition);
        //***************** 2 Inch Printer End ******************//

        //***************** 3 Inch Printer Start ******************//
        threeCompanyLogo = findViewById(R.id.threeCompanyLogo);
        threeQRLogo = findViewById(R.id.threeQRLogo);
        threeShopName = findViewById(R.id.threeShopName);
        threeShopDetails = findViewById(R.id.threeShopDetails);
        threeInvoiceDetails = findViewById(R.id.threeInvoiceDetails);
        threeShopPrintStatus = findViewById(R.id.threeShopPrintStatus);
        threeSubTotal = findViewById(R.id.threeSubTotal);
        threeShopCGST = findViewById(R.id.threeShopCGST);
        threeCGST = findViewById(R.id.threeCGST);
        threeShopSGST = findViewById(R.id.threeShopSGST);
        threeSGST = findViewById(R.id.threeSGST);
        threeDiscount = findViewById(R.id.threeDiscount);
        threeTotalAmount = findViewById(R.id.threeTotalAmount);
        threeShopCGSTLayout = findViewById(R.id.threeShopCGSTLayout);
        threeShopSGSTLayout = findViewById(R.id.threeShopSGSTLayout);
        threeDiscountLayout = findViewById(R.id.threeDiscountLayout);
        threeRecyclerView = findViewById(R.id.threeRecyclerView);
        threeNestedScrollView = findViewById(R.id.threeNestedScrollView);
        threeInvoiceTermsCondition = findViewById(R.id.threeInvoiceTermsCondition);
        //***************** 3 Inch Printer End ******************//


        binding.backToInvoice.setOnClickListener(this);
        binding.printInvoiceCardView.setOnClickListener(this);
        binding.shareInvoiceCardView.setOnClickListener(this);
        binding.editInvoiceButton.setOnClickListener(this);
        binding.refundInvoiceButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToInvoice) {
            finish();
        } else if (id == R.id.printInvoiceCardView) {
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
        } else if (id == R.id.shareInvoiceCardView) {
            createPdf();
        } else if (id == R.id.editInvoiceButton) {
            openEditInvoice();
        } else if (id == R.id.refundInvoiceButton) {
            confirmRefund();
        }

    }

    private void openEditInvoice() {
        if (invoiceResponseList.isEmpty() || invoiceResponseList.get(0).isRefunded()) {
            Toast.makeText(activity, getString(R.string.toast_cannot_edit_refunded), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(activity, EditInvoice.class);
        intent.putExtra("invoiceId", invoiceId);
        startActivity(intent);
    }

    private void confirmRefund() {
        if (invoiceResponseList.isEmpty() || invoiceResponseList.get(0).isRefunded()) {
            return;
        }
        BottomSheetUi.showConfirm(
                activity,
                getString(R.string.refund_confirm_title),
                getString(R.string.refund_confirm_message),
                getString(R.string.refund_bill),
                getString(R.string.cancel),
                true,
                () -> {
                    posBillingWalaDatabase.refundInvoice(invoiceResponseList.get(0).getInvoiceNumber());
                    Toast.makeText(activity, getString(R.string.refund_success), Toast.LENGTH_SHORT).show();
                    InvoicePendingSync.syncPendingInvoiceChanges(activity);
                    getInvoiceDetails();
                });
    }

    public void createPdf() {

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        String invoiceNumber = invoiceResponseList.get(0).getInvoiceNumber();
        String[] separated = invoiceNumber.split("/");
        try {
            invoiceNumber = separated[2];
            invoiceNumber = "SalesInvoice_" + invoiceNumber;
        } catch (Exception e) {
            e.printStackTrace();
            invoiceNumber = separated[1];
            invoiceNumber = "SalesInvoice_" + invoiceNumber;
        }

        String BillDetails = getBillDetails();

        invoiceInvoiceDetails.setText(Html.fromHtml(BillDetails));

        Bitmap bitmap = convertLayout(twoNestedScrollView, 48);

        if (bitmap != null) {
            Bitmap bitmap1 = getResizedBitmap(bitmap, 48);
            // Prepare to insert the image into MediaStore
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, invoiceNumber + ".png"); // File name
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png"); // MIME type
            values.put(MediaStore.Images.Media.TITLE, invoiceNumber); // Title
            values.put(MediaStore.Images.Media.DESCRIPTION, "Invoice Image"); // Description
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000); // Date added
            values.put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000); // Date modified

            ContentResolver contentResolver = activity.getContentResolver();
            // Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            File file = new File(directory_path + "/" + invoiceNumber + ".png");
            if (file != null) {
                try {
                    FileOutputStream outputStream = new FileOutputStream(file);
                    bitmap1.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.flush();
                    openGeneratedPDF(invoiceNumber);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void openGeneratedPDF(String invoiceNumber) {

        File file = new File(directory_path + "/" + invoiceNumber + ".png");
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
        intentShareFile.setType(URLConnection.guessContentTypeFromName(file.getName()));
        intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);
        List<ResolveInfo> resInfoList = activity.getPackageManager().queryIntentActivities(intentShareFile, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            activity.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        //  intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intentShareFile, "Share Invoice"));

    }

    public void print2InchBill() {

        showDialog();

        Bitmap bitmap = convertLayout(twoNestedScrollView, 48);
        if (bitmap != null) {
            printImage(bitmap, 48);
        }

        hideDialog();

    }

    public void print3InchBill() {

        showDialog();

        Bitmap bitmap = convertLayout(threeNestedScrollView, 72);
        if (bitmap != null) {
            printImage(bitmap, 72);
        }

        hideDialog();

    }

    protected void printImage(Bitmap image, int effectivePrintWidth) {

        if (WoosimPrnMng.isPrinterConnected(getApplicationContext(), InvoiceDetailsBluetoothPrint.this)) {

            BluetoothPrintService mService = null;
            mService = WoosimPrnMng.getServiceInstance();
            PrintImage PrintImage = new PrintImage(getResizedBitmap(image, effectivePrintWidth));
            PrintImage.PrepareImage(dither.floyd_steinberg, 128);
            mService.write(PrintImage.getPrintImageData());

            checkAndFeedPaper(printerSettingResponseList.get(0).getKotPrinterFeedLines());

        } else {
            //Printer not connected and send request for connecting printer
            new WoosimPrnMng(activity, "", InvoiceDetailsBluetoothPrint.this);
        }

    }

    public Bitmap convertLayout(NestedScrollView nestedScrollView, int effectivePrintWidth) {

        int printWidthInPixels = effectivePrintWidth * 16;

        // Measure and layout the nestedScrollView
        nestedScrollView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        nestedScrollView.layout(0, 0, nestedScrollView.getMeasuredWidth(), nestedScrollView.getMeasuredHeight());

        nestedScrollView.setDrawingCacheEnabled(true);
        nestedScrollView.buildDrawingCache();

        Bitmap bitmap = Bitmap.createBitmap(nestedScrollView.getWidth(), nestedScrollView.getHeight(), Bitmap.Config.ARGB_8888);
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
                Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
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
            Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
            bm.recycle();
            return resizedBitmap;
        }
        return bm;
    }

    public void checkAndFeedPaper(String lines) {

        if (WoosimPrnMng.isPrinterConnected(activity, InvoiceDetailsBluetoothPrint.this)) {
            BluetoothPrintService mService = null;
            mService = KOTWoosimPrnMng.getServiceInstance();
            StringBuilder lineBreaks = new StringBuilder();
            for (int i = 0; i < Integer.parseInt(lines); i++) {
                lineBreaks.append("\n"); // Add a newline character for each extra line
            }
            // Convert to bytes and send to the printer
            byte[] lineBreakBytes = lineBreaks.toString().getBytes();
            mService.write(lineBreakBytes);
        } else {
            //Printer not connected and send request for connecting printer
            new WoosimPrnMng(activity, "", InvoiceDetailsBluetoothPrint.this);
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
        getCompanyDetails();
        getPrinterSettingDetails();
        getInvoiceDetails();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void getInvoiceDetails() {

        invoiceResponseList = posBillingWalaDatabase.getInvoiceDetails(invoiceId);
        if (!invoiceResponseList.isEmpty()) {

            String BillDetails = getBillDetails();

            invoiceInvoiceDetails.setText(Html.fromHtml(BillDetails));
            twoInvoiceDetails.setText(Html.fromHtml(BillDetails));
            threeInvoiceDetails.setText(Html.fromHtml(BillDetails));

            getInvoiceProductDetails(invoiceResponseList.get(0).getInvoiceNumber());
            bindInvoiceActions();

        }

    }

    private void bindInvoiceActions() {
        boolean refunded = !invoiceResponseList.isEmpty() && invoiceResponseList.get(0).isRefunded();
        binding.refundedBanner.setVisibility(refunded ? View.VISIBLE : View.GONE);
        binding.invoiceActionButtons.setVisibility(refunded ? View.GONE : View.VISIBLE);
    }

    public void getCompanyDetails() {
        companyResponseList = posBillingWalaDatabase.getCompanyDetails();
        if (!companyResponseList.isEmpty()) {

            String primaryShopName = ShopHeaderBuilder.resolveShopName1(companyResponseList.get(0));
            invoiceShopName.setText(primaryShopName);
            twoShopName.setText(primaryShopName);
            threeShopName.setText(primaryShopName);

            String shopDetails = getShopDetails();

            invoiceShopDetails.setText(shopDetails);
            twoShopDetails.setText(shopDetails);
            threeShopDetails.setText(shopDetails);

            /*invoiceShopCGST.setText("CGST @" + companyResponseList.get(0).getShopCGST() + "%");
            invoiceShopSGST.setText("SGST @" + companyResponseList.get(0).getShopSGST() + "%");

            twoShopCGST.setText("CGST @" + companyResponseList.get(0).getShopCGST() + "%");
            twoShopSGST.setText("SGST @" + companyResponseList.get(0).getShopSGST() + "%");

            threeShopCGST.setText("CGST @" + companyResponseList.get(0).getShopCGST() + "%");
            threeShopSGST.setText("SGST @" + companyResponseList.get(0).getShopSGST() + "%");*/

            if (companyResponseList.get(0).getCompanyLogo() != null) {
                String companyLogo = companyResponseList.get(0).getCompanyLogo();
                // decode base64 string
                byte[] bytes = Base64.decode(companyLogo, Base64.DEFAULT);
                // Initialize bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                twoCompanyLogo.setImageBitmap(bitmap);
                threeCompanyLogo.setImageBitmap(bitmap);
                invoiceCompanyLogo.setImageBitmap(bitmap);
            }

            if (companyResponseList.get(0).getPaymentLogo() != null) {
                String paymentLogo = companyResponseList.get(0).getPaymentLogo();
                // decode base64 string
                byte[] bytes = Base64.decode(paymentLogo, Base64.DEFAULT);
                // Initialize bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                twoQRLogo.setImageBitmap(bitmap);
                threeQRLogo.setImageBitmap(bitmap);
            }

        }
    }

    public void getPrinterSettingDetails() {
        printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();

        if (!printerSettingResponseList.isEmpty()) {
            String bluetoothAddress = printerSettingResponseList.get(0).getBluetoothAddress() != null ? printerSettingResponseList.get(0).getBluetoothAddress() : "";
            if (!bluetoothAddress.equalsIgnoreCase("")) {
                try {
                    new WoosimPrnMng(activity, bluetoothAddress, InvoiceDetailsBluetoothPrint.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //Company Logo
            if (printerSettingResponseList.get(0).getLogoUse() != null) {
                if (printerSettingResponseList.get(0).getLogoUse().equalsIgnoreCase("on")) {
                    twoCompanyLogo.setVisibility(View.VISIBLE);
                    threeCompanyLogo.setVisibility(View.VISIBLE);
                    invoiceCompanyLogo.setVisibility(View.VISIBLE);
                } else {
                    twoCompanyLogo.setVisibility(View.GONE);
                    threeCompanyLogo.setVisibility(View.GONE);
                    invoiceCompanyLogo.setVisibility(View.GONE);
                }
            } else {
                twoCompanyLogo.setVisibility(View.GONE);
                threeCompanyLogo.setVisibility(View.GONE);
                invoiceCompanyLogo.setVisibility(View.GONE);
            }
            //QR Code Payment
            if (printerSettingResponseList.get(0).getPaymentUse() != null) {
                if (printerSettingResponseList.get(0).getPaymentUse().equalsIgnoreCase("on")) {
                    twoQRLogo.setVisibility(View.VISIBLE);
                    threeQRLogo.setVisibility(View.VISIBLE);
                } else {
                    twoQRLogo.setVisibility(View.GONE);
                    threeQRLogo.setVisibility(View.GONE);
                }
            } else {
                twoQRLogo.setVisibility(View.GONE);
                threeQRLogo.setVisibility(View.GONE);
            }

            applyDuplicateBillCopyLabel();
        } else {
            twoCompanyLogo.setVisibility(View.GONE);
            threeCompanyLogo.setVisibility(View.GONE);
            invoiceCompanyLogo.setVisibility(View.GONE);
            twoQRLogo.setVisibility(View.GONE);
            threeQRLogo.setVisibility(View.GONE);
            applyDuplicateBillCopyLabel();
        }

        if (!printerSettingResponseList.isEmpty() && printerSettingResponseList.get(0).getInvoiceTermsCondition() != null) {
            twoInvoiceTermsCondition.setText(printerSettingResponseList.get(0).getInvoiceTermsCondition());
            threeInvoiceTermsCondition.setText(printerSettingResponseList.get(0).getInvoiceTermsCondition());
        } else {
            twoInvoiceTermsCondition.setVisibility(View.GONE);
            threeInvoiceTermsCondition.setVisibility(View.GONE);
        }


    }

    /**
     * Duplicate Bill label applies only on Invoice List reprint ({@link InvoiceDetailsBluetoothPrint}).
     * Other print screens (POS billing, KOT, etc.) ignore this setting.
     */
    private void applyDuplicateBillCopyLabel() {
        boolean duplicateOn = !printerSettingResponseList.isEmpty()
                && printerSettingResponseList.get(0).getDuplicateBillUse() != null
                && printerSettingResponseList.get(0).getDuplicateBillUse().equalsIgnoreCase("on");

        if (twoShopPrintStatus != null) {
            if (duplicateOn) {
                twoShopPrintStatus.setText(getString(R.string.ui__duplicate_copy_));
                twoShopPrintStatus.setVisibility(View.VISIBLE);
            } else {
                twoShopPrintStatus.setVisibility(View.GONE);
            }
        }
        if (threeShopPrintStatus != null) {
            if (duplicateOn) {
                threeShopPrintStatus.setText(getString(R.string.ui__duplicate_copy_));
                threeShopPrintStatus.setVisibility(View.VISIBLE);
            } else {
                threeShopPrintStatus.setVisibility(View.GONE);
            }
        }
    }


    public void getInvoiceProductDetails(String invoiceNumber) {

        invoiceProductResponseList.clear();
        invoiceProductResponseList = posBillingWalaDatabase.getInvoiceProductList(invoiceNumber);

        float totalPerProductAmount = 0f, totalCGST = 0f, totalSGST = 0f, totalUnitPrice = 0f, totalGST = 0f, totalPerProductGST = 0f;
        int totalQty = 0;
        String discountType = "";
        if (!invoiceProductResponseList.isEmpty()) {

            invoiceRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
            adapter = new InvoiceProductAdapter(activity, invoiceProductResponseList);
            invoiceRecyclerView.setAdapter(adapter);

            //Two Inch Printer List
            TwoInvoicePrintAdapter twoPrintAdapter = new TwoInvoicePrintAdapter(activity, invoiceProductResponseList);
            twoRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
            twoRecyclerView.setAdapter(twoPrintAdapter);
            //Three Inch Printer List
            ThreeInvoicePrintAdapter threePrintAdapter = new ThreeInvoicePrintAdapter(activity, invoiceProductResponseList);
            threeRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
            threeRecyclerView.setAdapter(threePrintAdapter);

            for (InvoiceProductResponse invoiceProductResponse : invoiceProductResponseList) {

                discountType = invoiceResponseList.get(0).getDiscountType();

                float productPrice = Float.parseFloat(invoiceProductResponse.getProductPrice());
                totalUnitPrice += Float.parseFloat(invoiceProductResponse.getProductPrice());
                float productQuantity = Float.parseFloat(invoiceProductResponse.getProductQuantity());
                if (!invoiceProductResponse.getProductCGST().equalsIgnoreCase("")) {
                    totalCGST += Float.parseFloat(invoiceProductResponse.getProductCGST());
                }
                if (!invoiceProductResponse.getProductSGST().equalsIgnoreCase("")) {
                    totalSGST += Float.parseFloat(invoiceProductResponse.getProductSGST());
                }
                totalQty += Float.parseFloat(invoiceProductResponse.getProductQuantity());

                totalPerProductGST = (productPrice * ((totalCGST + totalSGST) / 100));
                totalGST += (productPrice * ((totalCGST + totalSGST) / 100)) * productQuantity;

                totalPerProductAmount = totalPerProductAmount + ((productPrice + totalPerProductGST) * productQuantity);

            }

            float subTotalAmt = totalPerProductAmount + totalGST;

            float discountAmt = Float.parseFloat(invoiceResponseList.get(0).getDiscount());
            float totalShopGST = Float.parseFloat(invoiceResponseList.get(0).getTotalGSTAmount());

            if (discountType != null) {
                if (discountType.equalsIgnoreCase("Amount")) {
                    discountAmt = discountAmt;
                } else {
                    discountAmt = subTotalAmt / (100 / discountAmt);
                }
            } else {
                discountAmt = subTotalAmt / (100 / discountAmt);
            }

            float totalAmt = 0f;
            totalAmt = (subTotalAmt - discountAmt) + totalShopGST;

            invoiceSubTotal.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", subTotalAmt));
            invoiceCGST.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
            invoiceSGST.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
            invoiceDiscount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", discountAmt));
            invoiceTotalAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalAmt));

            twoSubTotal.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", subTotalAmt));
            threeSubTotal.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", subTotalAmt));
            twoDiscount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", discountAmt));
            threeDiscount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", discountAmt));
            twoTotalAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalAmt));
            threeTotalAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalAmt));


            twoShopCGST.setText("CGST@" + companyResponseList.get(0).getShopCGST() + "%");
            twoCGST.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
            threeShopCGST.setText("CGST@" + companyResponseList.get(0).getShopCGST() + "%");
            threeCGST.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", (totalShopGST / 2)));

            twoShopSGST.setText("SGST@" + companyResponseList.get(0).getShopSGST() + "%");
            twoSGST.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", (totalShopGST / 2)));

            threeShopSGST.setText("SGST@" + companyResponseList.get(0).getShopSGST() + "%");
            threeSGST.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", (totalShopGST / 2)));

            binding.printInvoiceCardView.setVisibility(View.VISIBLE);
            binding.shareInvoiceCardView.setVisibility(View.VISIBLE);
            invoiceNestedScrollView.setVisibility(View.VISIBLE);

        } else {
            binding.printInvoiceCardView.setVisibility(View.GONE);
            binding.shareInvoiceCardView.setVisibility(View.GONE);
            invoiceNestedScrollView.setVisibility(View.GONE);
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            //bluetooth enabled and request for showing available bluetooth devices
            new WoosimPrnMng(activity, "", InvoiceDetailsBluetoothPrint.this);
        } else if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == RESULT_OK) {
            //bluetooth device selected and request pairing with device
            String bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            new WoosimPrnMng(activity, bluetoothAddress, InvoiceDetailsBluetoothPrint.this);
        } else if (requestCode == REQUEST_KOT_ENABLE_BT && resultCode == RESULT_OK) {
            //bluetooth enabled and request for showing available bluetooth devices
            new KOTWoosimPrnMng(activity, "", InvoiceDetailsBluetoothPrint.this);
        } else if (requestCode == REQUEST_KOT_CONNECT_DEVICE && resultCode == RESULT_OK) {
            //bluetooth device selected and request pairing with device
            String bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            new KOTWoosimPrnMng(activity, bluetoothAddress, InvoiceDetailsBluetoothPrint.this);
        }
    }

}