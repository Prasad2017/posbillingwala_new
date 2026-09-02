package com.pos_billingwala.Activity;

import static com.pos_billingwala.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.pos_billingwala.Adapter.ProductPrintAdapter;
import com.pos_billingwala.BuildConfig;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.Print.BluetoothPrintService;
import com.pos_billingwala.Print.BluetoothPrinterChannel;
import com.pos_billingwala.Print.DeviceListActivity;
import com.pos_billingwala.Print.PrintImage;
import com.pos_billingwala.Print.PrinterConnectionHelper;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.Extra.TabletPrintUi;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivityProductListBluetoothPrintBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class ProductListBluetoothPrint extends BaseActivity implements View.OnClickListener {


    public static RecyclerView twoRecyclerView;
    public static NestedScrollView twoNestedScrollView;
    public static RecyclerView threeRecyclerView;
    public static NestedScrollView threeNestedScrollView;
    public static RelativeLayout productLayout;
    public static RecyclerView productRecyclerView;
    public static TextView noDataFound;
    public static CardView shareProductCardView, printProductCardView;
    public static List<ProductResponse> productResponseList = new ArrayList<>();
    public static Activity activity;
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    ProgressDialog progressDialog;
    //********************* Bluetooth Printer Start ************************//
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS;
    String bluetoothAddress;
    int REQUEST_ENABLE_BT = 4, REQUEST_CONNECT_DEVICE = 6;
    //******************** Bluetooth Printer End ************************//
    ActivityProductListBluetoothPrintBinding binding;


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

    public static void getProductList() {

        productResponseList.clear();
        productResponseList = posBillingWalaDatabase.getAllProductList("", "");
        if (!productResponseList.isEmpty()) {

            List<ProductPrintAdapter.ProductPrintRow> printRows = buildProductPrintRows(productResponseList);
            ProductPrintAdapter productAdapter = new ProductPrintAdapter(activity, printRows);
            productRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
            productRecyclerView.setAdapter(productAdapter);
            //2 inch Printer
            twoRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
            twoRecyclerView.setAdapter(new ProductPrintAdapter(activity, printRows));
            //3 inch Printer
            threeRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
            threeRecyclerView.setAdapter(new ProductPrintAdapter(activity, printRows));

            productLayout.setVisibility(View.VISIBLE);
            noDataFound.setVisibility(View.GONE);

        } else {
            productLayout.setVisibility(View.GONE);
            noDataFound.setVisibility(View.VISIBLE);
        }

    }

    static List<ProductPrintAdapter.ProductPrintRow> buildProductPrintRows(List<ProductResponse> products) {
        List<ProductPrintAdapter.ProductPrintRow> rows = new ArrayList<>();
        if (products == null) {
            return rows;
        }
        for (ProductResponse product : products) {
            if (product == null) {
                continue;
            }
            String code = product.getProductCode() != null ? product.getProductCode() : "";
            String name = product.getProductName() != null ? product.getProductName() : "";
            List<ProductPortionResponse> portions = posBillingWalaDatabase.getProductPortionList(product.getProductId());
            if (portions == null || portions.isEmpty()) {
                rows.add(new ProductPrintAdapter.ProductPrintRow(code, name, "-", product.getProductPrice()));
                continue;
            }
            for (ProductPortionResponse portion : portions) {
                String portionName = portion != null && portion.getPortionName() != null
                        ? portion.getPortionName() : "-";
                String portionPrice = portion != null ? portion.getPortionPrice() : "";
                rows.add(new ProductPrintAdapter.ProductPrintRow(code, name, portionName, portionPrice));
            }
        }
        return rows;
    }

    public void createPdf() {

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Bitmap bitmap = convertLayout(twoNestedScrollView, 48);

        if (bitmap != null) {
            Bitmap bitmap1 = getResizedBitmap(bitmap, 48);
            // Prepare to insert the image into MediaStore
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "productList.png"); // File name
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png"); // MIME type
            values.put(MediaStore.Images.Media.TITLE, "productList"); // Title
            values.put(MediaStore.Images.Media.DESCRIPTION, "Product List"); // Description
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000); // Date added
            values.put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000); // Date modified

            ContentResolver contentResolver = activity.getContentResolver();
            // Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            File file = new File(directory_path + "/productList.png");
            if (file != null) {
                try {
                    FileOutputStream outputStream = new FileOutputStream(file);
                    bitmap1.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.flush();
                    openGeneratedPDF();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void openGeneratedPDF() {

        File file = new File(directory_path + "/productList.png");
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(ProductListBluetoothPrint.this, BuildConfig.APPLICATION_ID + ".provider", file);
        intentShareFile.setType(URLConnection.guessContentTypeFromName(file.getName()));
        intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);
        List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(intentShareFile, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            this.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        startActivity(Intent.createChooser(intentShareFile, "Share Invoice"));

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductListBluetoothPrintBinding.inflate(getLayoutInflater());
        View view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here
        setContentView(view); //view is set by view binding

        activity = ProductListBluetoothPrint.this;
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        initViews();

        PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    public void initViews() {
        noDataFound = findViewById(R.id.noDataFound);
        productRecyclerView = findViewById(R.id.productRecyclerView);
        productLayout = findViewById(R.id.productLayout);
        twoNestedScrollView = findViewById(R.id.twoNestedScrollView);
        twoRecyclerView = findViewById(R.id.twoRecyclerView);
        threeNestedScrollView = findViewById(R.id.threeNestedScrollView);
        threeRecyclerView = findViewById(R.id.threeRecyclerView);
        printProductCardView = findViewById(R.id.printProductCardView);
        shareProductCardView = findViewById(R.id.shareProductCardView);

        binding.printProductCardView.setOnClickListener(this);
        binding.shareProductCardView.setOnClickListener(this);

        TabletPrintUi.applyLandscape(this);

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.printProductCardView) {
            if (!printerSettingResponseList.isEmpty()) {
                if (!PrinterConnectionHelper.ensureBillPrinter(activity, savedBillPrinterAddress())) {
                    return;
                }
                progressDialog = new ProgressDialog(activity);
                progressDialog.setMessage(getString(R.string.toast_printing_in_progress));
                if (printerSettingResponseList.get(0).getPrinterName().equalsIgnoreCase("2-Inch")) {
                    print2InchBill(false);
                } else if (printerSettingResponseList.get(0).getPrinterName().equalsIgnoreCase("3-Inch")) {
                    print3InchBill(false);
                }
            } else {
                Toast.makeText(activity, getString(R.string.toast_please_select_printer_from_setting), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.shareProductCardView) {
            createPdf();
        }

    }

    public void print2InchBill(boolean printStatus) {

        showDialog();

        Bitmap bitmap = convertLayout(twoNestedScrollView, 48);
        if (bitmap != null) {
            printImage(bitmap, 48);
        }

        hideDialog();

    }

    public void print3InchBill(boolean printStatus) {

        Bitmap bitmap = convertLayout(threeNestedScrollView, 72);
        if (bitmap != null) {
            printImage(bitmap, 72);
        }

        hideDialog();

    }

    protected void printImage(Bitmap image, int effectivePrintWidth) {
        PrintImage printImage = new PrintImage(getResizedBitmap(image, effectivePrintWidth));
        printImage.PrepareImage(com.pos_billingwala.Print.PrintImage.dither.floyd_steinberg, 128);
        if (!PrinterConnectionHelper.safeWriteBill(activity, printImage.getPrintImageData())) {
            WoosimPrnMng.connect(activity, savedBillPrinterAddress(), ProductListBluetoothPrint.this);
            return;
        }
        checkAndFeedPaper(5);
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

    private String savedBillPrinterAddress() {
        if (printerSettingResponseList == null || printerSettingResponseList.isEmpty()) {
            return "";
        }
        String addr = printerSettingResponseList.get(0).getBluetoothAddress();
        return addr != null ? addr : "";
    }

    public void getPrinterSettingDetails() {
        printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();
        if (!printerSettingResponseList.isEmpty()) {
            String bluetoothAddress = printerSettingResponseList.get(0).getBluetoothAddress() != null ? printerSettingResponseList.get(0).getBluetoothAddress() : "";
            if (!bluetoothAddress.equalsIgnoreCase("")) {
                try {
                    new WoosimPrnMng(activity, bluetoothAddress, ProductListBluetoothPrint.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void checkAndFeedPaper(int lines) {
        try {
            StringBuilder lineBreaks = new StringBuilder();
            for (int i = 0; i < lines; i++) {
                lineBreaks.append("\n");
            }
            PrinterConnectionHelper.safeWriteBill(activity, lineBreaks.toString().getBytes());
        } catch (Exception ignored) {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            WoosimPrnMng.connect(activity, savedBillPrinterAddress(), ProductListBluetoothPrint.this);
        } else if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == RESULT_OK && data != null && data.getExtras() != null) {
            String bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            if (bluetoothAddress != null) {
                BluetoothPrinterChannel.bill().onDevicePicked(bluetoothAddress);
                if (!printerSettingResponseList.isEmpty()) {
                    printerSettingResponseList.get(0).setBluetoothAddress(bluetoothAddress);
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        requestPermission();
        getPrinterSettingDetails();
        getProductList();
    }

    public void requestPermission() {

        Dexter.withContext(activity).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT).withListener(new MultiplePermissionsListener() {
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