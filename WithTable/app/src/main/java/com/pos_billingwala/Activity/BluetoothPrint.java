package com.pos_billingwala.Activity;

import static com.pos_billingwala.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.textfield.TextInputEditText;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.pos_billingwala.Adapter.CartAdapter;
import com.pos_billingwala.Adapter.ThreePrintAdapter;
import com.pos_billingwala.Adapter.TwoKOTPrintAdapter;
import com.pos_billingwala.Adapter.TwoPrintAdapter;
import com.pos_billingwala.BuildConfig;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.Observability;
import com.pos_billingwala.Extra.SimpleDividerItemDecoration;
import com.pos_billingwala.Extra.LicenceExpiredUi;
import com.pos_billingwala.Extra.LicenseSession;
import com.google.firebase.perf.metrics.Trace;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.InventoryResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.Print.BluetoothPrintService;
import com.pos_billingwala.Print.DeviceListActivity;
import com.pos_billingwala.Print.KOTWoosimPrnMng;
import com.pos_billingwala.Print.PrintImage;
import com.pos_billingwala.Print.PrintImage.dither;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.R;
import com.pos_billingwala.WorkerClass.CartProductWorker;
import com.pos_billingwala.databinding.ActivityBluetoothPrintBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SuppressLint({"Range", "SetTextI18n, NewApi, StaticFieldLeak"})
public class BluetoothPrint extends BaseActivity implements View.OnClickListener {

    public static TextView kotPrint, twoKOTShopName, twoKOTInvoiceDetails;
    public static ImageView twoKOTCompanyLogo;
    public static TextView twoShopName, twoShopDetails, twoInvoiceDetails, twoShopPrintStatus, twoSubTotal, twoShopCGST, twoCGST, twoShopSGST, twoSGST, twoDiscount, twoTotalAmount, twoInvoiceTermsCondition;
    public static ImageView twoCompanyLogo, twoQRLogo;
    public static TextView threeKOTShopName, threeKOTInvoiceDetails;
    public static ImageView threeKOTCompanyLogo;
    public static TextView threeShopName, threeShopDetails, threeInvoiceDetails, threeShopPrintStatus, threeSubTotal, threeShopCGST, threeCGST, threeShopSGST, threeSGST, threeDiscount, threeTotalAmount, threeInvoiceTermsCondition;
    public static ImageView threeCompanyLogo, threeQRLogo;
    public static LinearLayout twoShopCGSTLayout, twoShopSGSTLayout, twoDiscountLayout;
    public static RecyclerView twoRecyclerView;
    public static NestedScrollView twoNestedScrollView;
    public static RecyclerView twoKOTRecyclerView;
    public static NestedScrollView twoKOTNestedScrollView;

    public static LinearLayout threeShopCGSTLayout, threeShopSGSTLayout, threeDiscountLayout;
    public static RecyclerView threeRecyclerView;
    public static NestedScrollView threeNestedScrollView;
    public static RecyclerView threeKOTRecyclerView;
    public static NestedScrollView threeKOTNestedScrollView;

    public static String invoiceRunningStatus, tableNumber, cartOrderStatus;
    public static RadioButton cashButton, onlineButton, bankButton;
    public static Activity activity;
    public static RecyclerView cartRecyclerView;
    public static List<ProductCartResponse> productCartResponseList = new ArrayList<>();
    public static List<InventoryResponse> inventoryResponseList = new ArrayList<>();
    public static List<CompanyResponse> companyResponseList = new ArrayList<>();
    public static List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static CartAdapter cartAdapter;
    public static TextView totalPayableAmountTxt, subTotalTxt, discountTxt, totalAmountTxt;
    public static RelativeLayout cartLayout;
    public static TextView noDataFound;
    public static String inr, paymentMode = "", invoiceNumber, invoiceDate, discountType = "Percentage";
    public static String[] discountTypeList;
    ProgressDialog progressDialog;
    View view;
    PopupWindow mypopupWindow;
    ActivityBluetoothPrintBinding binding;
    private final ExecutorService invoiceSaveExecutor = Executors.newSingleThreadExecutor();
    /** Resize + dither + BT write — keep off UI to avoid ANR on large bills. */
    private final ExecutorService printBitmapExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean invoiceSaveInProgress = false;
    //********************* Bluetooth Printer Start ************************//
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS;
    String bluetoothAddress;
    int REQUEST_ENABLE_BT = 4, REQUEST_CONNECT_DEVICE = 6;
    int REQUEST_KOT_ENABLE_BT = 8, REQUEST_KOT_CONNECT_DEVICE = 10;


    public static void getCartProductList() {

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(CartProductWorker.class).build();
        WorkManager.getInstance(activity).getWorkInfoByIdLiveData(workRequest.getId())
                .observe((LifecycleOwner) activity, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {

                        float totalPerProductAmount = 0f, discountAmount = 0f, totalUnitPrice = 0f, totalCGST = 0f, totalSGST = 0f, totalGST = 0f, totalPerProductGST = 0f, subTotalAmt = 0f, totalShopGST = 0f;
                        int totalQty = 0;
                        float shopCGST = 0f, shopSGST = 0f, totalAmt = 0f;

                        if (!productCartResponseList.isEmpty()) {

                            //Add to Cart Purchase Product list
                            cartAdapter = new CartAdapter(activity, productCartResponseList);
                            cartRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));

                            cartRecyclerView.setAdapter(cartAdapter);
                            cartRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(activity));
                            // cartAdapter.notifyItemInserted(productCartResponseList.size() - 1);

                            //Two Inch Printer List
                            TwoPrintAdapter twoPrintAdapter = new TwoPrintAdapter(activity, productCartResponseList);
                            twoRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));

                            twoRecyclerView.setAdapter(twoPrintAdapter);
                            //Three Inch Printer List
                            ThreePrintAdapter threePrintAdapter = new ThreePrintAdapter(activity, productCartResponseList);
                            threeRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));

                            threeRecyclerView.setAdapter(threePrintAdapter);

                            //Two Inch KOT Printer List
                            TwoKOTPrintAdapter twoKOTPrintAdapter = new TwoKOTPrintAdapter(activity, productCartResponseList);
                            twoKOTRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));

                            twoKOTRecyclerView.setAdapter(twoKOTPrintAdapter);
                            //Three Inch KOT Printer List
                            threeKOTRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));

                            threeKOTRecyclerView.setAdapter(twoKOTPrintAdapter);

                            for (ProductCartResponse productCartResponse : productCartResponseList) {

                                discountTxt.setText("Discount(%)\n" + productCartResponse.getCartDiscount());
                                discountAmount = Float.parseFloat(productCartResponseList.get(0).getCartDiscount());
                                discountType = productCartResponse.getCartDiscountType();

                                float productPrice = Float.parseFloat(productCartResponse.getResolvedLinePrice());
                                totalUnitPrice += Float.parseFloat(productCartResponse.getResolvedLinePrice());
                                float productQuantity = Float.parseFloat(productCartResponse.getProductQuantity());
                                if (!CreatePos.companyResponseList.isEmpty()) {
                                    if (CreatePos.companyResponseList.get(0).getGstStatus() != null) {
                                        if (CreatePos.companyResponseList.get(0).getGstStatus().equalsIgnoreCase("On")) {
                                            if (!productCartResponse.getProductCGST().equalsIgnoreCase("")) {
                                                totalCGST += Float.parseFloat(productCartResponse.getProductCGST());
                                            }

                                            if (!productCartResponse.getProductSGST().equalsIgnoreCase("")) {
                                                totalSGST += Float.parseFloat(productCartResponse.getProductSGST());
                                            }

                                            totalQty += Float.parseFloat(productCartResponse.getProductQuantity());

                                            totalPerProductGST = (productPrice * ((totalCGST + totalSGST) / 100));
                                            totalGST += (productPrice * ((totalCGST + totalSGST) / 100)) * productQuantity;

                                            totalPerProductAmount = totalPerProductAmount + ((productPrice + totalPerProductGST) * productQuantity);
                                        } else {
                                            totalPerProductAmount = totalPerProductAmount + (productPrice * productQuantity);
                                        }
                                    } else {
                                        totalPerProductAmount = totalPerProductAmount + (productPrice * productQuantity);
                                    }
                                } else {
                                    totalPerProductAmount = totalPerProductAmount + (productPrice * productQuantity);
                                }
                            }

                            subTotalAmt = totalPerProductAmount + totalGST;


                            if (companyResponseList.get(0).getGstStatus() != null) {

                                if (companyResponseList.get(0).getGstStatus().equalsIgnoreCase("On")) {

                                    if (companyResponseList.get(0).getShopCGST() != null) {
                                        if (!companyResponseList.get(0).getShopCGST().trim().equalsIgnoreCase("")) {
                                            shopCGST = subTotalAmt * (Float.parseFloat(companyResponseList.get(0).getShopCGST().trim()) / 100);

                                            twoShopCGST.setText("CGST@" + companyResponseList.get(0).getShopCGST() + "%");
                                            twoCGST.setText(inr + String.format(Locale.US, "%.2f", shopCGST));
                                            twoShopCGSTLayout.setVisibility(View.VISIBLE);

                                            threeShopCGST.setText("CGST@" + companyResponseList.get(0).getShopCGST() + "%");
                                            threeCGST.setText(inr + String.format(Locale.US, "%.2f", shopCGST));
                                            threeShopCGSTLayout.setVisibility(View.VISIBLE);

                                        } else {
                                            twoShopCGSTLayout.setVisibility(View.GONE);
                                            threeShopCGSTLayout.setVisibility(View.GONE);
                                        }
                                    } else {
                                        twoShopCGSTLayout.setVisibility(View.GONE);
                                        threeShopCGSTLayout.setVisibility(View.GONE);
                                    }

                                    if (companyResponseList.get(0).getShopSGST() != null) {
                                        if (!companyResponseList.get(0).getShopSGST().trim().equalsIgnoreCase("")) {
                                            shopSGST = subTotalAmt * (Float.parseFloat(companyResponseList.get(0).getShopSGST().trim()) / 100);

                                            twoShopSGST.setText("SGST@" + companyResponseList.get(0).getShopSGST() + "%");
                                            twoSGST.setText(inr + String.format(Locale.US, "%.2f", shopSGST));
                                            twoShopSGSTLayout.setVisibility(View.VISIBLE);

                                            threeShopSGST.setText("SGST@" + companyResponseList.get(0).getShopSGST() + "%");
                                            threeSGST.setText(inr + String.format(Locale.US, "%.2f", shopSGST));
                                            threeShopSGSTLayout.setVisibility(View.VISIBLE);

                                        } else {
                                            twoShopSGSTLayout.setVisibility(View.GONE);
                                            threeShopSGSTLayout.setVisibility(View.GONE);
                                        }
                                    } else {
                                        twoShopSGSTLayout.setVisibility(View.GONE);
                                        threeShopSGSTLayout.setVisibility(View.GONE);
                                    }

                                } else {
                                    twoShopCGSTLayout.setVisibility(View.GONE);
                                    threeShopCGSTLayout.setVisibility(View.GONE);

                                    twoShopSGSTLayout.setVisibility(View.GONE);
                                    threeShopSGSTLayout.setVisibility(View.GONE);
                                }

                            } else {
                                twoShopCGSTLayout.setVisibility(View.GONE);
                                threeShopCGSTLayout.setVisibility(View.GONE);

                                twoShopSGSTLayout.setVisibility(View.GONE);
                                threeShopSGSTLayout.setVisibility(View.GONE);
                            }


                            totalShopGST = shopCGST + shopSGST;

                            subTotalTxt.setText("Sub Total\n" + inr + String.format(Locale.US, "%.2f", subTotalAmt));
                            twoSubTotal.setText(inr + String.format(Locale.US, "%.2f", subTotalAmt));
                            threeSubTotal.setText(inr + String.format(Locale.US, "%.2f", subTotalAmt));

                            if (discountType != null) {
                                if (discountType.equalsIgnoreCase("Amount")) {
                                    discountAmount = discountAmount;
                                } else {
                                    discountAmount = subTotalAmt / (100 / discountAmount);
                                }

                                twoDiscountLayout.setVisibility(View.VISIBLE);
                                threeDiscountLayout.setVisibility(View.VISIBLE);

                            } else {
                                discountAmount = subTotalAmt / (100 / discountAmount);
                                twoDiscountLayout.setVisibility(View.GONE);
                                threeDiscountLayout.setVisibility(View.GONE);
                            }

                            twoDiscount.setText(inr + String.format(Locale.US, "%.2f", discountAmount));
                            threeDiscount.setText(inr + String.format(Locale.US, "%.2f", discountAmount));

                            if (companyResponseList.get(0).getGstStatus().equalsIgnoreCase("on")) {
                                totalAmt = (subTotalAmt - discountAmount) + totalShopGST;
                            } else {
                                totalAmt = subTotalAmt - discountAmount;
                            }

                            float totalAmount = (float) Math.ceil(totalAmt);

                            String totalPayableAmount = "Payable Amount<br/><b>" + inr + String.format(Locale.US, "%.2f", totalAmount) + "</b>";
                            totalAmountTxt.setText("Total Amount\n" + inr + String.format(Locale.US, "%.2f", totalAmt));
                            totalPayableAmountTxt.setText(Html.fromHtml(totalPayableAmount));

                            twoTotalAmount.setText(inr + String.format(Locale.US, "%.2f", totalAmount));
                            threeTotalAmount.setText(inr + String.format(Locale.US, "%.2f", totalAmount));

                            cartLayout.setVisibility(View.VISIBLE);
                            noDataFound.setVisibility(View.GONE);

                        } else {
                            cartLayout.setVisibility(View.GONE);
                            noDataFound.setVisibility(View.VISIBLE);
                        }


                    }
                });

        // Enqueue the work request
        WorkManager.getInstance(activity).enqueue(workRequest);


    }

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

    @SuppressLint("Range")
    public static String getInvoiceNumber() {

        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat todayDF = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat invoiceNumberDateFormat = new SimpleDateFormat("dd-MM", Locale.getDefault());
        invoiceDate = df.format(c);
        String todayDate = todayDF.format(c);
        String invoiceNumberDate = invoiceNumberDateFormat.format(c);

        String companyPrefix;
        int invoiceId = 0;
        if (!printerSettingResponseList.isEmpty()) {
            companyPrefix = printerSettingResponseList.get(0).getInvoicePrefix() + "/";

            if (printerSettingResponseList.get(0).getInvoiceTermsCondition() != null) {
                twoInvoiceTermsCondition.setText(printerSettingResponseList.get(0).getInvoiceTermsCondition());
                threeInvoiceTermsCondition.setText(printerSettingResponseList.get(0).getInvoiceTermsCondition());
            } else {
                twoInvoiceTermsCondition.setVisibility(View.GONE);
                threeInvoiceTermsCondition.setVisibility(View.GONE);
            }

        } else {
            companyPrefix = "";
        }

        SQLiteDatabase database = posBillingWalaDatabase.getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT COUNT(invoiceId) as invoiceId FROM " + POSBillingWalaDatabase.INVOICE_TABLE + " WHERE invoiceDate LIKE '%" + todayDate + "%'", null);
        while (cursor.moveToNext()) {
            invoiceId = Integer.parseInt(cursor.getString(cursor.getColumnIndex("invoiceId")));
        }
        database.close();

        int lastInvoiceId = invoiceId + 1;
        if (lastInvoiceId > 0 && lastInvoiceId <= 9) {
            invoiceNumber = companyPrefix + invoiceNumberDate + "/000" + lastInvoiceId;
        } else if (lastInvoiceId > 9 && lastInvoiceId <= 99) {
            invoiceNumber = companyPrefix + invoiceNumberDate + "/00" + lastInvoiceId;
        } else if (lastInvoiceId > 99 && lastInvoiceId <= 999) {
            invoiceNumber = companyPrefix + invoiceNumberDate + "/0" + lastInvoiceId;
        } else if (lastInvoiceId > 999 && lastInvoiceId <= 999999999) {
            invoiceNumber = companyPrefix + invoiceNumberDate + lastInvoiceId;
        }

        return invoiceNumber;
    }

    /**
     * Reserve one invoice number for this checkout and reuse it for print / KOT / PDF / save.
     * Never regenerate mid-checkout (avoids printed number differing from saved number).
     */
    @NonNull
    public static String resolveInvoiceNumber() {
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            return getInvoiceNumber();
        }
        return invoiceNumber;
    }

    @NonNull
    public static String getBillDetails(String customerName, String customerMobile, String customerAddress) {
        String BillDetails = "";
        String billNo = resolveInvoiceNumber();
        if (cartOrderStatus.equalsIgnoreCase("table_wise")) {
            BillDetails = "<b>Bill No:</b> " + billNo + "<br/><b>Date:</b> " + invoiceDate + "<br/><b>Table No:</b> " + tableNumber;
        } else {
            BillDetails = "<b>Bill No:</b> " + billNo + "<br/><b>Date:</b> " + invoiceDate;
        }
        if (printerSettingResponseList.get(0).getCustomerUse() != null) {
            if (printerSettingResponseList.get(0).getCustomerUse().equalsIgnoreCase("on")) {
                BillDetails = BillDetails + "<br/><b>Customer Name:</b> " + (customerName != null ? customerName : "NA") +
                        "<br/><b>Customer Mobile:</b> " + (customerMobile != null ? customerMobile : "NA") +
                        "<br/><b>Customer Address:</b> " + (customerAddress != null ? customerAddress : "NA");
            }
        }
        return String.valueOf(Html.fromHtml(BillDetails));
    }

    public void automaticSavePDF(String customerName, String customerMobile, String customerAddress, String invoiceNumber) {

        String[] separated = invoiceNumber.split("/");
        try {
            invoiceNumber = separated[2];
            invoiceNumber = "SalesInvoice_" + invoiceNumber;
        } catch (Exception e) {
            e.printStackTrace();
            invoiceNumber = separated[1];
            invoiceNumber = "SalesInvoice_" + invoiceNumber;
        }

        createPdf(customerName, customerMobile, customerAddress, invoiceNumber);

    }

    public void createPdf(String customerName, String customerMobile, String customerAddress, String invoiceNumber) {

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        String BillDetails = getBillDetails(customerName, customerMobile, customerAddress);
        twoInvoiceDetails.setText(BillDetails);
        twoShopPrintStatus.setText("**** Original Copy ****");
        threeShopPrintStatus.setText("**** Original Copy ****");
        Bitmap bitmap = convertLayout(twoNestedScrollView, 48);
        if (bitmap != null) {
            final String fileName = invoiceNumber;
            printBitmapExecutor.execute(() -> {
                try {
                    Bitmap bitmap1 = getResizedBitmap(bitmap, 48);
                    runOnUiThread(() -> saveImageToMediaStore(bitmap1, fileName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void saveImageToMediaStore(Bitmap bitmap, String invoiceNumber) {

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
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                openGeneratedPDF(invoiceNumber);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void openGeneratedPDF(String invoiceNumber) {

        File file = new File(directory_path + "/" + invoiceNumber + ".png");
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(BluetoothPrint.this, BuildConfig.APPLICATION_ID + ".provider", file);
        intentShareFile.setType(URLConnection.guessContentTypeFromName(file.getName()));
        intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);
        List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(intentShareFile, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            this.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        // intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intentShareFile, "Share Invoice"));

    }

    public void setScreenSizeSmall() {
        Configuration configuration = getResources().getConfiguration();
        configuration.fontScale = (float) 1; //0.85 small size, 1 normal size, 1, 15 big etc
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        configuration.densityDpi = (int) getResources().getDisplayMetrics().xdpi;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBluetoothPrintBinding.inflate(getLayoutInflater());
        View view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here
        setContentView(view); //view is set by view binding

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setScreenSizeSmall();

        activity = BluetoothPrint.this;
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);
        // Fresh checkout session — do not reuse a previous bill's reserved number
        invoiceNumber = "";
        paymentMode = "";

        initViews();

        try {

            Intent intent = getIntent();
            if (intent != null) {
                invoiceRunningStatus = intent.getStringExtra("invoiceRunningStatus");
                tableNumber = intent.getStringExtra("tableNumber");
                cartOrderStatus = intent.getStringExtra("cartOrderStatus");
                if (cartOrderStatus.equalsIgnoreCase("table_wise")) {
                    kotPrint.setVisibility(View.VISIBLE);
                } else {
                    kotPrint.setVisibility(View.GONE);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        //Add runtime permissions
        PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

    }

    public void initViews() {

        inr = MainActivity.currencyName + " ";


        binding.kotPrint.setOnClickListener(this);
        binding.menuIcon.setOnClickListener(this);
        binding.printInvoiceCardView.setOnClickListener(this);
        binding.shareInvoiceCardView.setOnClickListener(this);

        cashButton = findViewById(R.id.cash);
        onlineButton = findViewById(R.id.online);
        bankButton = findViewById(R.id.bank);
        kotPrint = findViewById(R.id.kotPrint);

        cartRecyclerView = findViewById(R.id.cartRecyclerView);
        totalPayableAmountTxt = findViewById(R.id.payableAmount);
        subTotalTxt = findViewById(R.id.subTotal);
        discountTxt = findViewById(R.id.discount);
        totalAmountTxt = findViewById(R.id.totalProductAmount);
        noDataFound = findViewById(R.id.noDataFound);
        cartLayout = findViewById(R.id.cartLayout);
        //***************** 2 Inch Printer Start ******************//
        twoCompanyLogo = findViewById(R.id.twoCompanyLogo);
        twoKOTCompanyLogo = findViewById(R.id.twoKOTCompanyLogo);
        twoQRLogo = findViewById(R.id.twoQRLogo);
        twoKOTShopName = findViewById(R.id.twoKOTShopName);
        twoShopName = findViewById(R.id.twoShopName);
        twoShopDetails = findViewById(R.id.twoShopDetails);
        twoKOTInvoiceDetails = findViewById(R.id.twoKOTInvoiceDetails);
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
        twoKOTRecyclerView = findViewById(R.id.twoKOTRecyclerView);
        twoNestedScrollView = findViewById(R.id.twoNestedScrollView);
        twoKOTNestedScrollView = findViewById(R.id.twoKOTNestedScrollView);
        twoInvoiceTermsCondition = findViewById(R.id.twoInvoiceTermsCondition);
        //***************** 2 Inch Printer End ******************//

        //***************** 3 Inch Printer Start ******************//
        threeCompanyLogo = findViewById(R.id.threeCompanyLogo);
        threeKOTCompanyLogo = findViewById(R.id.threeKOTCompanyLogo);
        threeQRLogo = findViewById(R.id.threeQRLogo);
        threeShopName = findViewById(R.id.threeShopName);
        threeKOTShopName = findViewById(R.id.threeKOTShopName);
        threeShopDetails = findViewById(R.id.threeShopDetails);
        threeKOTInvoiceDetails = findViewById(R.id.threeKOTInvoiceDetails);
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
        threeKOTRecyclerView = findViewById(R.id.threeKOTRecyclerView);
        threeRecyclerView = findViewById(R.id.threeRecyclerView);
        threeNestedScrollView = findViewById(R.id.threeNestedScrollView);
        threeKOTNestedScrollView = findViewById(R.id.threeKOTNestedScrollView);
        threeInvoiceTermsCondition = findViewById(R.id.threeInvoiceTermsCondition);
        //***************** 3 Inch Printer End ******************//

        discountTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Dialog dialog = new Dialog(activity);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
                dialog.setContentView(R.layout.update_discount_dialog);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                dialog.setCancelable(false);

                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(dialog.getWindow().getAttributes());
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

                TextInputEditText discountPercentageTxt = dialog.findViewById(R.id.discountPercentage);
                TextView addDiscountPercentageTxt = dialog.findViewById(R.id.addDiscountPercentage);
                TextView dismissDiscountPercentageTxt = dialog.findViewById(R.id.dismissDiscountPercentage);
                Spinner discountTypeSpinner = dialog.findViewById(R.id.discountTypeSpinner);

                discountTypeList = getResources().getStringArray(R.array.discount_type);
                try {
                    ArrayAdapter adapter = new ArrayAdapter(BluetoothPrint.this, android.R.layout.simple_spinner_item, discountTypeList);
                    adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                    discountTypeSpinner.setAdapter(adapter);
                    if (discountType != null) {
                        discountTypeSpinner.setSelection(adapter.getPosition(discountType));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                discountTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        discountType = discountTypeList[position];
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                discountPercentageTxt.setText(productCartResponseList.get(0).getCartDiscount());

                dismissDiscountPercentageTxt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                addDiscountPercentageTxt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!discountPercentageTxt.getText().toString().isEmpty()) {
                            //Calculation Part
                            discountTxt.setText("Discount(%)\n" + discountPercentageTxt.getText().toString());
                            float discountAmount = Float.parseFloat(discountPercentageTxt.getText().toString());
                            float subTotalAmt = 0f;
                            float shopCGST = 0f, shopSGST = 0f;
                            if (companyResponseList.get(0).getShopCGST() != null) {
                                shopCGST = subTotalAmt * (Float.parseFloat(companyResponseList.get(0).getShopCGST().trim()) / 100);
                            }

                            if (companyResponseList.get(0).getShopSGST() != null) {
                                if (!companyResponseList.get(0).getShopSGST().trim().equalsIgnoreCase("")) {
                                    shopSGST = subTotalAmt * (Float.parseFloat(companyResponseList.get(0).getShopSGST().trim()) / 100);
                                }
                            }

                            float totalShopGST = shopCGST + shopSGST;

                            float totalAmt = 0f;
                            if (companyResponseList.get(0).getGstStatus().equalsIgnoreCase("on")) {
                                if (discountType.equalsIgnoreCase("Percentage")) {
                                    totalAmt = subTotalAmt - (subTotalAmt / (100 / discountAmount)) + totalShopGST;
                                } else {
                                    totalAmt = subTotalAmt - discountAmount + totalShopGST;
                                }
                            } else {
                                if (discountType.equalsIgnoreCase("Percentage")) {
                                    totalAmt = subTotalAmt - (subTotalAmt / (100 / discountAmount));
                                } else {
                                    totalAmt = subTotalAmt - discountAmount;
                                }
                            }

                            String format = String.format(Locale.US, "%.2f", totalAmt);
                            String totalPayableAmount = "Payable Amount<br/><b>" + inr + format + "</b>";
                            totalAmountTxt.setText("Total Amount\n" + inr + format);
                            totalPayableAmountTxt.setText(Html.fromHtml(totalPayableAmount));
                            if (!productCartResponseList.isEmpty()) {
                                for (ProductCartResponse productCartResponse : productCartResponseList) {
                                    posBillingWalaDatabase.updateCartDiscount(productCartResponse.getCartId(), discountPercentageTxt.getText().toString(), discountType);
                                }
                            }

                            getCartProductList();
                            //Dialog Dismiss Part
                            dialog.dismiss();
                        } else {
                            Toast.makeText(activity, getString(R.string.toast_please_add_discount_percentage), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                dialog.show();
                dialog.getWindow().setAttributes(lp);

            }
        });

        /*try {
            if (paymentMode.equalsIgnoreCase("Cash")) {
                cashButton.setChecked(true);
                onlineButton.setChecked(false);
                bankButton.setChecked(false);
            } else if (paymentMode.equalsIgnoreCase("UPI")) {
                onlineButton.setChecked(true);
                cashButton.setChecked(false);
                bankButton.setChecked(false);
            } else if (paymentMode.equalsIgnoreCase("Bank")) {
                bankButton.setChecked(true);
                cashButton.setChecked(false);
                onlineButton.setChecked(false);
            } else {
                cashButton.setChecked(false);
                onlineButton.setChecked(false);
                bankButton.setChecked(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            cashButton.setChecked(false);
            onlineButton.setChecked(false);
            bankButton.setChecked(false);
        }*/
        binding.paymentGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int selectedId = binding.paymentGroup.getCheckedRadioButtonId();
                RadioButton radioPayButton = findViewById(selectedId);
                paymentMode = radioPayButton.getText().toString();
            }
        });

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.menuIcon) {
            setPopUpWindow();
        } else if (id == R.id.kotPrint) {
            if (!printerSettingResponseList.isEmpty()) {

                progressDialog = new ProgressDialog(activity);
                progressDialog.setMessage(getString(R.string.toast_printing_in_progress));

                if (printerSettingResponseList.get(0).getPrinterName().equalsIgnoreCase("2-Inch")) {
                    printKOT2InchBill(false);
                } else if (printerSettingResponseList.get(0).getPrinterName().equalsIgnoreCase("3-Inch")) {
                    printKOT3InchBill(false);
                }

            } else {
                Toast.makeText(activity, getString(R.string.toast_please_select_printer_from_setting), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.printInvoiceCardView) {
            if (!printerSettingResponseList.isEmpty()) {
                if (printerSettingResponseList.get(0).getCustomerUse() != null) {
                    if (printerSettingResponseList.get(0).getCustomerUse().equalsIgnoreCase("on")) {
                        final Dialog dialog = new Dialog(activity);
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
                        dialog.setContentView(R.layout.update_customer_dialog);
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        dialog.setCancelable(false);

                        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                        lp.copyFrom(dialog.getWindow().getAttributes());
                        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

                        TextView dismissCustomerTxt = dialog.findViewById(R.id.dismissCustomer);
                        TextView addCustomerTxt = dialog.findViewById(R.id.addCustomer);
                        TextInputEditText customerNameTxt = dialog.findViewById(R.id.customerName);
                        TextInputEditText customerMobileTxt = dialog.findViewById(R.id.customerMobile);
                        TextInputEditText customerAddressTxt = dialog.findViewById(R.id.customerAddress);

                        dismissCustomerTxt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });
                        addCustomerTxt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (!customerNameTxt.getText().toString().isEmpty()) {

                                    String customerName = customerNameTxt.getText().toString();
                                    String customerMobile = customerMobileTxt.getText().toString();
                                    String customerAddress = customerAddressTxt.getText().toString();

                                    dialog.dismiss();

                                    invoiceNumber = resolveInvoiceNumber();

                                    progressDialog = new ProgressDialog(activity);
                                    progressDialog.setMessage(getString(R.string.toast_printing_in_progress));

                                    if (printerSettingResponseList.get(0).getPrinterName().equalsIgnoreCase("2-Inch")) {
                                        print2InchBill(customerName, customerMobile, customerAddress);
                                    } else if (printerSettingResponseList.get(0).getPrinterName().equalsIgnoreCase("3-Inch")) {
                                        print3InchBill(customerName, customerMobile, customerAddress);
                                    }

                                } else {
                                    Toast.makeText(activity, getString(R.string.toast_please_fill_customer_name), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                        dialog.show();
                        dialog.getWindow().setAttributes(lp);
                    } else {
                        resolveInvoiceNumber();
                        if (printerSettingResponseList.get(0).getPrinterName().equalsIgnoreCase("2-Inch")) {
                            print2InchBill("", "", "");
                        } else if (printerSettingResponseList.get(0).getPrinterName().equalsIgnoreCase("3-Inch")) {
                            print3InchBill("", "", "");
                        }
                    }
                } else {
                    resolveInvoiceNumber();
                    if (printerSettingResponseList.get(0).getPrinterName().equalsIgnoreCase("2-Inch")) {
                        print2InchBill("", "", "");
                    } else if (printerSettingResponseList.get(0).getPrinterName().equalsIgnoreCase("3-Inch")) {
                        print3InchBill("", "", "");
                    }
                }
            } else {
                Toast.makeText(activity, getString(R.string.toast_please_select_printer_from_setting), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.shareInvoiceCardView) {

            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
            dialog.setContentView(R.layout.update_customer_dialog);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCancelable(false);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

            TextView dismissCustomerTxt = dialog.findViewById(R.id.dismissCustomer);
            TextView addCustomerTxt = dialog.findViewById(R.id.addCustomer);
            TextInputEditText customerNameTxt = dialog.findViewById(R.id.customerName);
            TextInputEditText customerMobileTxt = dialog.findViewById(R.id.customerMobile);
            TextInputEditText customerAddressTxt = dialog.findViewById(R.id.customerAddress);

            dismissCustomerTxt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            addCustomerTxt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!customerNameTxt.getText().toString().isEmpty()) {
                        if (!customerMobileTxt.getText().toString().isEmpty()) {
                            if (!customerAddressTxt.getText().toString().isEmpty()) {

                                String customerName = customerNameTxt.getText().toString();
                                String customerMobile = customerMobileTxt.getText().toString();
                                String customerAddress = customerAddressTxt.getText().toString();

                                dialog.dismiss();

                                invoiceNumber = resolveInvoiceNumber();

                                saveInvoice(customerName, customerMobile, customerAddress, 1);

                            } else {
                                Toast.makeText(activity, getString(R.string.toast_please_fill_customer_address), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(activity, getString(R.string.toast_please_fill_customer_mobile), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, getString(R.string.toast_please_fill_customer_name), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            dialog.show();
            dialog.getWindow().setAttributes(lp);

        }
    }

    public void printKOT2InchBill(boolean printStatus) {

        showDialog();
        try {
            String BillDetails = "";
            String billNo = resolveInvoiceNumber();
            if (cartOrderStatus.equalsIgnoreCase("table_wise")) {
                BillDetails = "<b>Bill No:</b> " + billNo + "<br/><b>Date:</b> " + invoiceDate + "<br/><b>Table No:</b> " + tableNumber;
            } else {
                BillDetails = "<b>Bill No:</b> " + billNo + "<br/><b>Date:</b> " + invoiceDate;
            }

            twoKOTInvoiceDetails.setText(Html.fromHtml(BillDetails));
            // Layout→bitmap must stay on UI; resize/dither/BT write run off UI.
            Bitmap bitmap = convertLayout(twoKOTNestedScrollView, 48);
            if (bitmap != null) {
                printKOTImage(bitmap, 48);
            } else {
                hideDialog();
            }
        } catch (Exception e) {
            Toast.makeText(activity, getString(R.string.toast_kot_print_failed), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            hideDialog();
        }
    }

    public void printKOT3InchBill(boolean printStatus) {

        showDialog();
        try {
            String BillDetails = "";
            String billNo = resolveInvoiceNumber();
            if (cartOrderStatus.equalsIgnoreCase("table_wise")) {
                BillDetails = "<b>Bill No:</b> " + billNo + "<br/><b>Date:</b> " + invoiceDate + "<br/><b>Table No:</b> " + tableNumber;
            } else {
                BillDetails = "<b>Bill No:</b> " + billNo + "<br/><b>Date:</b> " + invoiceDate;
            }

            threeKOTInvoiceDetails.setText(Html.fromHtml(BillDetails));

            Bitmap bitmap = convertLayout(threeKOTNestedScrollView, 72);
            if (bitmap != null) {
                printKOTImage(bitmap, 72);
            } else {
                hideDialog();
            }
        } catch (Exception e) {
            Toast.makeText(activity, getString(R.string.toast_kot_print_failed), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            hideDialog();
        }
    }

    public void print2InchBill(String customerName, String customerMobile, String customerAddress) {

        showDialog();
        try {
            String BillDetails = getBillDetails(customerName, customerMobile, customerAddress);
            twoInvoiceDetails.setText(BillDetails);
            twoShopPrintStatus.setText("**** Original Copy ****");

            Bitmap bitmap = convertLayout(twoNestedScrollView, 48);
            if (bitmap != null) {
                printImage(bitmap, 48, customerName, customerMobile, customerAddress);
            } else {
                Toast.makeText(activity, getString(R.string.toast_print_layout_failed_saving_bill), Toast.LENGTH_SHORT).show();
                saveInvoice(customerName, customerMobile, customerAddress, 0);
                hideDialog();
            }
        } catch (Exception e) {
            Toast.makeText(activity, getString(R.string.toast_print_failed_saving_bill), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            saveInvoice(customerName, customerMobile, customerAddress, 0);
            hideDialog();
        }
    }

    public void print3InchBill(String customerName, String customerMobile, String customerAddress) {

        showDialog();
        try {
            String BillDetails = getBillDetails(customerName, customerMobile, customerAddress);
            threeInvoiceDetails.setText(BillDetails);
            threeShopPrintStatus.setText("**** Original Copy ****");

            Bitmap bitmap = convertLayout(threeNestedScrollView, 72);
            if (bitmap != null) {
                printImage(bitmap, 72, customerName, customerMobile, customerAddress);
            } else {
                Toast.makeText(activity, getString(R.string.toast_print_layout_failed_saving_bill), Toast.LENGTH_SHORT).show();
                saveInvoice(customerName, customerMobile, customerAddress, 0);
                hideDialog();
            }
        } catch (Exception e) {
            Toast.makeText(activity, getString(R.string.toast_print_failed_saving_bill), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            saveInvoice(customerName, customerMobile, customerAddress, 0);
            hideDialog();
        }
    }

    protected void printKOTImage(Bitmap image, int effectivePrintWidth) {
        printBitmapExecutor.execute(() -> {
            String toastMsg = null;
            try {
                if (KOTWoosimPrnMng.isPrinterConnected(activity, BluetoothPrint.this)) {
                    BluetoothPrintService mService = KOTWoosimPrnMng.getServiceInstance();
                    if (mService == null) {
                        toastMsg = "KOT printer not ready";
                    } else {
                        PrintImage printImage = new PrintImage(getResizedBitmap(image, effectivePrintWidth));
                        printImage.PrepareImage(dither.floyd_steinberg, 128);
                        mService.write(printImage.getPrintImageData());
                        if (printerSettingResponseList != null && !printerSettingResponseList.isEmpty()) {
                            KOTCheckAndFeedPaper(printerSettingResponseList.get(0).getKotPrinterFeedLines());
                        }
                    }
                } else {
                    toastMsg = "KOT printer offline";
                    try {
                        runOnUiThread(() -> {
                            try {
                                new KOTWoosimPrnMng(activity, "", BluetoothPrint.this);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                toastMsg = "KOT print failed";
                e.printStackTrace();
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

    /**
     * Resize/dither/BT write off UI, then always save the bill.
     * Layout capture ({@link #convertLayout}) stays on the UI thread.
     */
    protected void printImage(Bitmap image, int effectivePrintWidth, String customerName, String customerMobile, String customerAddress) {
        printBitmapExecutor.execute(() -> {
            boolean printOk = false;
            String toastMsg = null;
            try {
                if (WoosimPrnMng.isPrinterConnected(activity, BluetoothPrint.this)) {
                    BluetoothPrintService mService = WoosimPrnMng.getServiceInstance();
                    if (mService == null) {
                        toastMsg = "Printer not ready";
                    } else {
                        PrintImage printImage = new PrintImage(getResizedBitmap(image, effectivePrintWidth));
                        printImage.PrepareImage(dither.floyd_steinberg, 128);
                        mService.write(printImage.getPrintImageData());
                        if (printerSettingResponseList != null && !printerSettingResponseList.isEmpty()) {
                            checkAndFeedPaper(printerSettingResponseList.get(0).getPrinterFeedLines());
                        }
                        printOk = true;
                    }
                } else {
                    toastMsg = "Printer offline — bill will still be saved";
                    try {
                        runOnUiThread(() -> {
                            try {
                                new WoosimPrnMng(activity, "", BluetoothPrint.this);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                toastMsg = "Print failed — bill will still be saved";
                Observability.logNonFatal(e, "bluetooth_print");
                e.printStackTrace();
            } finally {
                final boolean ok = printOk;
                final String msg = toastMsg;
                runOnUiThread(() -> {
                    if (msg != null) {
                        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                    }
                    if (!ok) {
                        Log.w("BluetoothPrint", "Invoice save triggered without successful print");
                    }
                    try {
                        saveInvoice(customerName, customerMobile, customerAddress, 0);
                    } catch (Exception e) {
                        Toast.makeText(activity, getString(R.string.toast_failed_to_save_invoice_after_print), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                    hideDialog();
                });
            }
        });
    }

    public Bitmap convertLayout(NestedScrollView nestedScrollView, int effectivePrintWidth) {
        try {
            // Measure and layout the nestedScrollView
            nestedScrollView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            nestedScrollView.layout(0, 0, nestedScrollView.getMeasuredWidth(), nestedScrollView.getMeasuredHeight());

            if (nestedScrollView.getWidth() <= 0 || nestedScrollView.getHeight() <= 0) {
                return null;
            }

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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
        try {
            if (!WoosimPrnMng.isPrinterConnected(activity, BluetoothPrint.this)) {
                return;
            }
            BluetoothPrintService mService = WoosimPrnMng.getServiceInstance();
            if (mService == null || lines == null || lines.trim().isEmpty()) {
                return;
            }
            int count = Integer.parseInt(lines.trim());
            StringBuilder lineBreaks = new StringBuilder();
            for (int i = 0; i < count; i++) {
                lineBreaks.append("\n");
            }
            mService.write(lineBreaks.toString().getBytes());
        } catch (Exception e) {
            Log.e("BluetoothPrint", "checkAndFeedPaper failed", e);
        }
    }

    public void KOTCheckAndFeedPaper(String lines) {
        try {
            if (!KOTWoosimPrnMng.isPrinterConnected(activity, BluetoothPrint.this)) {
                return;
            }
            BluetoothPrintService mService = KOTWoosimPrnMng.getServiceInstance();
            if (mService == null || lines == null || lines.trim().isEmpty()) {
                return;
            }
            int count = Integer.parseInt(lines.trim());
            StringBuilder lineBreaks = new StringBuilder();
            for (int i = 0; i < count; i++) {
                lineBreaks.append("\n");
            }
            mService.write(lineBreaks.toString().getBytes());
        } catch (Exception e) {
            Log.e("BluetoothPrint", "KOTCheckAndFeedPaper failed", e);
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

    public void setPopUpWindow() {

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.share_dialog, null);
        mypopupWindow = new PopupWindow(view, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true);

        LinearLayout saveInvoiceLayout = view.findViewById(R.id.saveInvoiceLayout);
        LinearLayout duplicateInvoicePrintLayout = view.findViewById(R.id.duplicateInvoicePrintLayout);

        duplicateInvoicePrintLayout.setVisibility(View.GONE);

        saveInvoiceLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();
                invoiceNumber = resolveInvoiceNumber();
                saveInvoice("", "", "", 0);
            }
        });

        mypopupWindow.showAsDropDown(binding.menuIcon, 0, -75);

    }

    @SuppressLint("Range")
    public void saveInvoice(String customerName, String customerMobile, String customerAddress, int printStatus) {

        if (invoiceSaveInProgress) {
            Toast.makeText(activity, getString(R.string.toast_saving_invoice), Toast.LENGTH_SHORT).show();
            return;
        }

        if (productCartResponseList == null || productCartResponseList.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_cart_is_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        if (companyResponseList == null || companyResponseList.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_company_details_missing), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!LicenseSession.isBillingAllowed(activity, posBillingWalaDatabase)) {
            String blocked = LicenseSession.billingBlockedMessage(activity, posBillingWalaDatabase);
            if (LicenceExpiredUi.isExpiredMessage(blocked)) {
                LicenceExpiredUi.show(activity);
            } else {
                LicenceExpiredUi.showInfoDialog(activity, blocked);
            }
            return;
        }

        resolveInvoiceNumber();

        final String reservedInvoiceNumber = invoiceNumber;
        final String reservedTableNumber = tableNumber;
        final String reservedCartOrderStatus = cartOrderStatus;
        final String reservedPaymentMode = paymentMode;
        final String reservedInvoiceDate = invoiceDate;
        final String reservedDiscountType = discountType;
        final int reservedPrintStatus = printStatus;
        final String reservedCustomerName = customerName;
        final String reservedCustomerMobile = customerMobile;
        final String reservedCustomerAddress = customerAddress;
        final List<ProductCartResponse> cartSnapshot = new ArrayList<>(productCartResponseList);
        final CompanyResponse company = companyResponseList.get(0);

        final float subTotalAmt;
        final float discountAmtInput;
        try {
            subTotalAmt = Float.parseFloat(subTotalTxt.getText().toString().replace("Sub Total\n" + inr, ""));
            discountAmtInput = Float.parseFloat(discountTxt.getText().toString().replace("Discount(%)\n", ""));
        } catch (Exception e) {
            Toast.makeText(activity, getString(R.string.toast_invalid_bill_amounts), Toast.LENGTH_SHORT).show();
            return;
        }

        float shopCGST = 0f, shopSGST = 0f;
        try {
            if (company.getShopCGST() != null && !company.getShopCGST().trim().isEmpty()) {
                shopCGST = subTotalAmt * (Float.parseFloat(company.getShopCGST().trim()) / 100);
            }
            if (company.getShopSGST() != null && !company.getShopSGST().trim().isEmpty()) {
                shopSGST = subTotalAmt * (Float.parseFloat(company.getShopSGST().trim()) / 100);
            }
        } catch (Exception e) {
            // keep GST at 0 if parse fails
        }
        final float totalGSTAmount = shopCGST + shopSGST;

        float discountAmount = discountAmtInput;
        if (reservedDiscountType != null && reservedDiscountType.equalsIgnoreCase("Amount")) {
            discountAmount = discountAmtInput;
        } else {
            if (discountAmtInput != 0f) {
                discountAmount = subTotalAmt / (100 / discountAmtInput);
            } else {
                discountAmount = 0f;
            }
        }
        final float finalDiscountAmount = discountAmount;
        final float finalDiscountAmtForDb = discountAmtInput;

        final float totalAmt;
        if (company.getGstStatus() != null && company.getGstStatus().equalsIgnoreCase("on")) {
            totalAmt = (subTotalAmt - finalDiscountAmount) + totalGSTAmount;
        } else {
            totalAmt = subTotalAmt - finalDiscountAmount;
        }

        final String invoiceType;
        if (reservedCartOrderStatus != null && reservedCartOrderStatus.equalsIgnoreCase("table_wise")) {
            invoiceType = "table_wise";
        } else if (reservedCartOrderStatus != null && reservedCartOrderStatus.equalsIgnoreCase("take_away")) {
            invoiceType = "take_away";
        } else {
            invoiceType = "fast_billing";
        }

        invoiceSaveInProgress = true;

        invoiceSaveExecutor.execute(() -> {
            boolean saved = false;
            boolean needsPaymentMode = false;
            Exception error = null;
            Trace saveTrace = Observability.startTrace(Observability.TRACE_SAVE_INVOICE);
            try {
                Date c = Calendar.getInstance().getTime();
                SimpleDateFormat todayDF = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String inventoryDate = todayDF.format(c);

                for (ProductCartResponse productCartResponse : cartSnapshot) {
                    List<InventoryResponse> inventoryList =
                            posBillingWalaDatabase.getInventoryDetails(productCartResponse.getProductId());
                    if (inventoryList != null && !inventoryList.isEmpty()) {
                        for (InventoryResponse inventoryResponse : inventoryList) {
                            int saleInventoryQty = Integer.parseInt(productCartResponse.getProductQuantity());
                            int oldInventoryQty = Integer.parseInt(inventoryResponse.getProductInventoryQuantity());
                            int afterSaleInventoryQuantity = Integer.parseInt(inventoryResponse.getAfterSaleInventoryQuantity());
                            int totalQty = afterSaleInventoryQuantity - saleInventoryQty;

                            posBillingWalaDatabase.addInventory(
                                    productCartResponse.getProductId(),
                                    String.valueOf(oldInventoryQty),
                                    String.valueOf(totalQty),
                                    String.valueOf(saleInventoryQty),
                                    inventoryDate,
                                    0,
                                    getRandomString(10));
                        }
                    }
                }

                posBillingWalaDatabase.saveInvoice(
                        cartSnapshot,
                        reservedTableNumber,
                        reservedCustomerName,
                        reservedCustomerMobile,
                        reservedCustomerAddress,
                        reservedInvoiceNumber,
                        subTotalAmt,
                        totalGSTAmount,
                        finalDiscountAmtForDb,
                        reservedDiscountType,
                        totalAmt,
                        reservedPaymentMode,
                        reservedInvoiceDate,
                        invoiceType,
                        getRandomString(10),
                        0);

                if (!invoiceType.equalsIgnoreCase("table_wise")) {
                    needsPaymentMode = !posBillingWalaDatabase.checkPaymentMode(reservedInvoiceNumber).isEmpty();
                }
                saved = true;
            } catch (Exception e) {
                error = e;
                Observability.logNonFatal(e, "save_invoice_db");
            } finally {
                Observability.stopTrace(saveTrace);
            }

            final boolean saveOk = saved;
            final boolean showPaymentMode = needsPaymentMode;
            final Exception saveError = error;

            runOnUiThread(() -> {
                invoiceSaveInProgress = false;
                if (isFinishing()) {
                    return;
                }
                if (!saveOk) {
                    Toast.makeText(activity, getString(R.string.toast_failed_to_save_invoice_please_try_again),
                            Toast.LENGTH_LONG).show();
                    if (saveError != null) {
                        saveError.printStackTrace();
                    }
                    return;
                }

                if (invoiceType.equalsIgnoreCase("table_wise")) {
                    if (reservedPrintStatus == 1) {
                        automaticSavePDF(reservedCustomerName, reservedCustomerMobile, reservedCustomerAddress, reservedInvoiceNumber);
                        getCartProductList();
                    } else if (reservedPrintStatus == 0) {
                        Toast.makeText(activity, getString(R.string.toast_invoice_saved), Toast.LENGTH_SHORT).show();
                        getCartProductList();
                        onCallBack();
                    }
                } else if (showPaymentMode) {
                    setPaymentMode(reservedCustomerName, reservedCustomerMobile, reservedCustomerAddress, totalAmt, reservedPrintStatus);
                } else {
                    if (reservedPrintStatus == 1) {
                        automaticSavePDF(reservedCustomerName, reservedCustomerMobile, reservedCustomerAddress, reservedInvoiceNumber);
                        getCartProductList();
                    } else if (reservedPrintStatus == 0) {
                        Toast.makeText(activity, getString(R.string.toast_invoice_saved), Toast.LENGTH_SHORT).show();
                        getCartProductList();
                        onCallBack();
                    }
                }
            });
        });
    }

    public String getRandomString(final int sizeOfRandomString) {

        String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";

        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    @Override
    public void onStart() {
        super.onStart();
        requestPermission();
        getPrinterSettingDetails();
        getCompanyDetails();
        getCartProductList();
    }

    @Override
    protected void onDestroy() {
        invoiceSaveExecutor.shutdownNow();
        printBitmapExecutor.shutdownNow();
        super.onDestroy();
    }

    public void getCompanyDetails() {

        companyResponseList = posBillingWalaDatabase.getCompanyDetails();

        if (!companyResponseList.isEmpty()) {

            twoShopName.setText(companyResponseList.get(0).getCompanyName());
            threeShopName.setText(companyResponseList.get(0).getCompanyName());
            twoKOTShopName.setText(companyResponseList.get(0).getCompanyName());
            threeKOTShopName.setText(companyResponseList.get(0).getCompanyName());

            String shopDetails = "";
            if (companyResponseList.get(0).getGstStatus() != null) {
                if (companyResponseList.get(0).getGstStatus().equalsIgnoreCase("on")) {
                    shopDetails = companyResponseList.get(0).getCompanyAddress() + "\nPH:" + companyResponseList.get(0).getCompanyMobile() + "\n" + "GSTIN: " + companyResponseList.get(0).getGstNumber();

                } else if (companyResponseList.get(0).getGstStatus().equalsIgnoreCase("off")) {
                    shopDetails = companyResponseList.get(0).getCompanyAddress() + "\nPH:" + companyResponseList.get(0).getCompanyMobile();
                }
            } else {
                shopDetails = companyResponseList.get(0).getCompanyAddress() + "\nPH:" + companyResponseList.get(0).getCompanyMobile();
            }

            if (null != companyResponseList.get(0).getCompanyFssis() && (!companyResponseList.get(0).getCompanyFssis().isEmpty())) {
                shopDetails = shopDetails + "\nFSSAI No: " + companyResponseList.get(0).getCompanyFssis();
            }

            twoShopDetails.setText(shopDetails);
            threeShopDetails.setText(shopDetails);

            if (companyResponseList.get(0).getCompanyLogo() != null) {
                String companyLogo = companyResponseList.get(0).getCompanyLogo();
                // decode base64 string
                try {
                    byte[] bytes = Base64.decode(companyLogo, Base64.DEFAULT);
                    // Initialize bitmap
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    twoCompanyLogo.setImageBitmap(bitmap);
                    threeCompanyLogo.setImageBitmap(bitmap);
                    twoKOTCompanyLogo.setImageBitmap(bitmap);
                    threeKOTCompanyLogo.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                    twoCompanyLogo.setVisibility(View.GONE);
                    threeCompanyLogo.setVisibility(View.GONE);
                    twoKOTCompanyLogo.setVisibility(View.GONE);
                    threeKOTCompanyLogo.setVisibility(View.GONE);
                }
            }

            if (companyResponseList.get(0).getPaymentLogo() != null) {
                String paymentLogo = companyResponseList.get(0).getPaymentLogo();
                // decode base64 string
                try {
                    byte[] bytes = Base64.decode(paymentLogo, Base64.DEFAULT);
                    // Initialize bitmap
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    twoQRLogo.setImageBitmap(bitmap);
                    threeQRLogo.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                    twoQRLogo.setVisibility(View.GONE);
                    threeQRLogo.setVisibility(View.GONE);
                }
            }

        }
    }

    public void getPrinterSettingDetails() {
        printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();
        if (!printerSettingResponseList.isEmpty()) {
            String bluetoothAddress = printerSettingResponseList.get(0).getBluetoothAddress() != null ? printerSettingResponseList.get(0).getBluetoothAddress() : "";
            if (!bluetoothAddress.equalsIgnoreCase("")) {
                try {
                    new WoosimPrnMng(activity, bluetoothAddress, activity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String bluetoothKOTAddress = printerSettingResponseList.get(0).getBluetoothKOTAddress() != null ? printerSettingResponseList.get(0).getBluetoothKOTAddress() : "";
            if (!bluetoothKOTAddress.equalsIgnoreCase("")) {
                try {
                    new KOTWoosimPrnMng(activity, bluetoothAddress, activity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //Company Logo
            if (printerSettingResponseList.get(0).getLogoUse() != null) {
                if (printerSettingResponseList.get(0).getLogoUse().equalsIgnoreCase("on")) {
                    twoCompanyLogo.setVisibility(View.VISIBLE);
                    twoKOTCompanyLogo.setVisibility(View.VISIBLE);
                    threeCompanyLogo.setVisibility(View.VISIBLE);
                    threeKOTCompanyLogo.setVisibility(View.VISIBLE);
                } else {
                    twoCompanyLogo.setVisibility(View.GONE);
                    threeCompanyLogo.setVisibility(View.GONE);
                    twoKOTCompanyLogo.setVisibility(View.GONE);
                    threeKOTCompanyLogo.setVisibility(View.GONE);
                }
            } else {
                twoCompanyLogo.setVisibility(View.GONE);
                threeCompanyLogo.setVisibility(View.GONE);
                twoKOTCompanyLogo.setVisibility(View.GONE);
                threeKOTCompanyLogo.setVisibility(View.GONE);
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
        } else {
            twoCompanyLogo.setVisibility(View.GONE);
            threeCompanyLogo.setVisibility(View.GONE);
            twoKOTCompanyLogo.setVisibility(View.GONE);
            threeKOTCompanyLogo.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            new WoosimPrnMng(activity, "", activity);
        } else if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == RESULT_OK) {
            String bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            new WoosimPrnMng(activity, bluetoothAddress, activity);
        } else if (requestCode == REQUEST_KOT_ENABLE_BT && resultCode == RESULT_OK) {
            new KOTWoosimPrnMng(activity, "", activity);
        } else if (requestCode == REQUEST_KOT_CONNECT_DEVICE && resultCode == RESULT_OK) {
            String bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            new KOTWoosimPrnMng(activity, bluetoothAddress, activity);
        }
    }

    public void onCallBack() {
        paymentMode = "";
        if (cartOrderStatus.equalsIgnoreCase("table_wise")) {

            Intent intent = new Intent(BluetoothPrint.this, MainActivity.class);
            intent.putExtra("invoiceRunningStatus", "printBill");
            intent.putExtra("cartOrderStatus", cartOrderStatus);
            startActivity(intent);
            finish();

        } else if (cartOrderStatus.equalsIgnoreCase("take_away")) {

            Intent intent = new Intent(BluetoothPrint.this, MainActivity.class);
            intent.putExtra("invoiceRunningStatus", "printBill");
            intent.putExtra("cartOrderStatus", cartOrderStatus);
            startActivity(intent);
            finish();

        } else {

            Intent intent = new Intent(BluetoothPrint.this, MainActivity.class);
            intent.putExtra("invoiceRunningStatus", "printBill");
            intent.putExtra("cartOrderStatus", cartOrderStatus);
            startActivity(intent);
            finish();

        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onCallBack();
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

    public void setPaymentMode(String customerName, String customerMobile, String customerAddress, float totalAmt, int printStatus) {

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.set_payment_mode_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextView continueToReport = dialog.findViewById(R.id.continueToReport);
        TextView dismissReport = dialog.findViewById(R.id.dismissReport);
        TextView totalAmount = dialog.findViewById(R.id.totalAmount);
        RadioGroup paymentGroup = dialog.findViewById(R.id.paymentGroup);

        totalAmount.setText("Total Amount: " + MainActivity.currencyName + totalAmt);
        paymentGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int selectedId = paymentGroup.getCheckedRadioButtonId();
                RadioButton radioPayButton = group.findViewById(selectedId);
                paymentMode = radioPayButton.getText().toString();
            }
        });

        dismissReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                posBillingWalaDatabase.updateInvoicePaymentMode(invoiceNumber, "Cash");
                if (printStatus == 1) {
                    automaticSavePDF(customerName, customerMobile, customerAddress, invoiceNumber);
                    getCartProductList();
                } else if (printStatus == 0) {
                    Toast.makeText(activity, getString(R.string.toast_invoice_saved), Toast.LENGTH_SHORT).show();
                    getCartProductList();
                    onCallBack();
                }
                dialog.dismiss();
            }
        });

        continueToReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!paymentMode.isEmpty()) {
                    posBillingWalaDatabase.updateInvoicePaymentMode(invoiceNumber, paymentMode);
                    if (printStatus == 1) {
                        automaticSavePDF(customerName, customerMobile, customerAddress, invoiceNumber);
                        getCartProductList();
                    } else if (printStatus == 0) {
                        Toast.makeText(activity, getString(R.string.toast_invoice_saved), Toast.LENGTH_SHORT).show();
                        getCartProductList();
                        onCallBack();
                    }
                    dialog.dismiss();
                } else {
                    Toast.makeText(BluetoothPrint.this, getString(R.string.toast_please_select_payment_mode), Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);

    }


}
