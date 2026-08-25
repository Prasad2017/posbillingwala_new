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
import android.text.Html;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.pos_billingwala.Adapter.DuplicateInvoiceAdapter;
import com.pos_billingwala.Adapter.DuplicateTwoPrintAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ShopHeaderBuilder;
import com.pos_billingwala.Extra.SimpleDividerItemDecoration;
import com.pos_billingwala.Fragment.CreatePos;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Print.BluetoothPrintService;
import com.pos_billingwala.Print.DeviceListActivity;
import com.pos_billingwala.Print.PrintImage;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ActivityDuplicateBluetoothPrintBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@SuppressLint("SetTextI18n, StaticFieldLeak, NonConstantResourceId")
public class DuplicateBluetoothPrint extends BaseActivity implements View.OnClickListener {

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
    public static String invoiceRunningStatus, cartOrderStatus;
    public static RadioButton cashButton, onlineButton, bankButton;
    public static Activity activity;
    public static RecyclerView cartRecyclerView;
    public static List<CompanyResponse> companyResponseList = new ArrayList<>();
    public static List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    public static List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
    public static List<InvoiceProductResponse> invoiceProductResponseList = new ArrayList<>();
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static TextView totalPayableAmountTxt, subTotalTxt, discountTxt, totalAmountTxt;
    public static RelativeLayout cartLayout;
    public static TextView noDataFound;
    public static String inr, paymentMode;
    ProgressDialog progressDialog;
    ActivityDuplicateBluetoothPrintBinding binding;
    //********************* Bluetooth Printer Start ************************//
    int PERMISSION_ALL = 1;
    int REQUEST_ENABLE_BT = 4, REQUEST_CONNECT_DEVICE = 6;
    //******************** Bluetooth Printer End ************************//


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

    @NonNull
    public static String getBillDetails() {
        String BillDetails = "";
        if (invoiceResponseList.get(0).getInvoiceType().equalsIgnoreCase("table_wise")) {
            BillDetails = "<b>Bill No:</b> " + invoiceResponseList.get(0).getInvoiceNumber() + "<br/><b>Date:</b> " + invoiceResponseList.get(0).getInvoiceDate() + "<br/><b>Table No:</b> " + invoiceResponseList.get(0).getNoOfTable();
        } else {
            BillDetails = "<b>Bill No:</b> " + invoiceResponseList.get(0).getInvoiceNumber() + "<br/><b>Date:</b> " + invoiceResponseList.get(0).getInvoiceDate();
        }

        if (!invoiceResponseList.get(0).getCustomerName().equalsIgnoreCase("")) {
            BillDetails = BillDetails + "<br/><b>Customer Name:</b> " + (invoiceResponseList.get(0).getCustomerName() != null ? invoiceResponseList.get(0).getCustomerName() : "NA") +
                    "<br/><b>Customer Mobile:</b> " + (invoiceResponseList.get(0).getCustomerMobile() != null ? invoiceResponseList.get(0).getCustomerMobile() : "NA") +
                    "<br/><b>Customer Address:</b> " + (invoiceResponseList.get(0).getCustomerAddress() != null ? invoiceResponseList.get(0).getCustomerAddress() : "NA");
        }
        return String.valueOf(Html.fromHtml(BillDetails));
    }

    @NonNull
    public static String getShopDetails() {
        if (companyResponseList == null || companyResponseList.isEmpty()) {
            return "";
        }
        return ShopHeaderBuilder.buildShopDetailsBlock(companyResponseList.get(0));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDuplicateBluetoothPrintBinding.inflate(getLayoutInflater());
        View view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here
        setContentView(view); //view is set by view binding

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        activity = DuplicateBluetoothPrint.this;
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        initViews();

        try {

            Intent intent = getIntent();
            if (intent != null) {
                invoiceRunningStatus = intent.getStringExtra("invoiceRunningStatus");
                cartOrderStatus = intent.getStringExtra("cartOrderStatus");
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

        binding.printInvoiceCardView.setOnClickListener(this);

        cashButton = findViewById(R.id.cash);
        onlineButton = findViewById(R.id.online);
        bankButton = findViewById(R.id.bank);

        cartRecyclerView = findViewById(R.id.cartRecyclerView);
        totalPayableAmountTxt = findViewById(R.id.payableAmount);
        subTotalTxt = findViewById(R.id.subTotal);
        discountTxt = findViewById(R.id.discount);
        totalAmountTxt = findViewById(R.id.totalProductAmount);
        noDataFound = findViewById(R.id.noDataFound);
        cartLayout = findViewById(R.id.cartLayout);
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


        if (paymentMode != null) {
            try {
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
                }
            } catch (Exception e) {
                e.printStackTrace();
                cashButton.setChecked(false);
                onlineButton.setChecked(false);
                bankButton.setChecked(false);
            }
        }

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
        if (view.getId() == R.id.printInvoiceCardView) {
            if (paymentMode != null) {
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

            } else {
                Toast.makeText(activity, getString(R.string.toast_please_select_payment_mode), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void print2InchBill() {

        showDialog();

        twoShopPrintStatus.setText("**** Duplicate Copy ****");

        Bitmap bitmap = convertLayout(twoNestedScrollView);
        if (bitmap != null) {
            printImage(bitmap, 48);
        }

        hideDialog();

    }

    public void print3InchBill() {

        showDialog();

        threeShopPrintStatus.setText("**** Duplicate Copy ****");

        Bitmap bitmap = convertLayout(threeNestedScrollView);
        if (bitmap != null) {
            printImage(bitmap, 72);
        }

        hideDialog();

    }

    protected void printImage(Bitmap image, int effectivePrintWidth) {

        if (WoosimPrnMng.isPrinterConnected(getApplicationContext(), DuplicateBluetoothPrint.this)) {

            BluetoothPrintService mService = null;
            mService = WoosimPrnMng.getServiceInstance();
            PrintImage PrintImage = new PrintImage(getResizedBitmap(image, effectivePrintWidth));
            PrintImage.PrepareImage(com.pos_billingwala.Print.PrintImage.dither.floyd_steinberg, 128);
            mService.write(PrintImage.getPrintImageData());

            checkAndFeedPaper(5);

        } else {
            //Printer not connected and send request for connecting printer
            new WoosimPrnMng(activity, "", DuplicateBluetoothPrint.this);
        }

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

    public void checkAndFeedPaper(int lines) {

        if (WoosimPrnMng.isPrinterConnected(activity, DuplicateBluetoothPrint.this)) {
            BluetoothPrintService mService = null;
            mService = WoosimPrnMng.getServiceInstance();
            byte[] normalText = {27, 33, 0};
            for (int i = 0; i < lines; i++) {
                String str = " \n";
                mService.write(normalText);
            }
        } else {
            //Printer not connected and send request for connecting printer
            new WoosimPrnMng(activity, "", DuplicateBluetoothPrint.this);
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
        getLastInvoiceList();

    }

    public void getLastInvoiceList() {

        invoiceResponseList = posBillingWalaDatabase.getLastInvoiceList(cartOrderStatus);
        if (!invoiceResponseList.isEmpty()) {

            String BillDetails = getBillDetails();

            twoInvoiceDetails.setText(Html.fromHtml(BillDetails));
            threeInvoiceDetails.setText(Html.fromHtml(BillDetails));

            getInvoiceProductDetails(invoiceResponseList.get(0).getInvoiceNumber());

        }

    }

    public void getInvoiceProductDetails(String invoiceNumber) {

        invoiceProductResponseList.clear();
        invoiceProductResponseList = posBillingWalaDatabase.getInvoiceProductList(invoiceNumber);

        float totalPerProductAmount = 0f, totalCGST = 0f, totalSGST = 0f, totalUnitPrice = 0f, totalGST = 0f, totalPerProductGST = 0f;
        int totalQty = 0;
        String discountType = "";
        if (!invoiceProductResponseList.isEmpty()) {

            //Add to Cart Purchase Product list
            DuplicateInvoiceAdapter duplicateInvoiceAdapter = new DuplicateInvoiceAdapter(activity, invoiceProductResponseList);
            cartRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));

            cartRecyclerView.setAdapter(duplicateInvoiceAdapter);
            cartRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(activity));
            //  duplicateInvoiceAdapter.notifyItemInserted(invoiceProductResponseList.size() - 1);

            //Two Inch Printer List
            twoRecyclerView.setLayoutManager(new LinearLayoutManager(activity));

            DuplicateTwoPrintAdapter adapter = new DuplicateTwoPrintAdapter(activity, invoiceProductResponseList);
            twoRecyclerView.setAdapter(adapter);

            adapter = new DuplicateTwoPrintAdapter(activity, invoiceProductResponseList);
            threeRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
            threeRecyclerView.setAdapter(adapter);

            for (InvoiceProductResponse invoiceProductResponse : invoiceProductResponseList) {

                discountType = invoiceResponseList.get(0).getDiscountType();

                float productPrice = Float.parseFloat(invoiceProductResponse.getProductPrice());
                totalUnitPrice += Float.parseFloat(invoiceProductResponse.getProductPrice());
                float productQuantity = Float.parseFloat(invoiceProductResponse.getProductQuantity());
                if (!CreatePos.companyResponseList.isEmpty()) {
                    if (CreatePos.companyResponseList.get(0).getGstStatus() != null) {
                        if (CreatePos.companyResponseList.get(0).getGstStatus().equalsIgnoreCase("On")) {
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

            float subTotalAmt = totalPerProductAmount + totalGST;

            float discountAmt = Float.parseFloat(invoiceResponseList.get(0).getDiscount());
            float totalShopGST = Float.parseFloat(invoiceResponseList.get(0).getTotalGSTAmount());

            if (discountType != null) {
                if (discountType.equalsIgnoreCase("Amount")) {
                    discountAmt = discountAmt;
                } else {
                    discountAmt = subTotalAmt / (100 / discountAmt);
                }

                twoDiscountLayout.setVisibility(View.VISIBLE);
                threeDiscountLayout.setVisibility(View.VISIBLE);

            } else {
                discountAmt = subTotalAmt / (100 / discountAmt);
                twoDiscountLayout.setVisibility(View.GONE);
                threeDiscountLayout.setVisibility(View.GONE);
            }

            float totalAmt = 0f;
            if (companyResponseList.get(0).getGstStatus().equalsIgnoreCase("on")) {
                totalAmt = (subTotalAmt - discountAmt) + totalShopGST;
            } else {
                totalAmt = subTotalAmt - discountAmt;
            }

            totalAmt = (int) Math.ceil(totalAmt);
            float totalAmount = (float) Math.ceil(totalAmt);
            if (companyResponseList.get(0).getShopCGST() != null) {
                if (!companyResponseList.get(0).getShopCGST().trim().equalsIgnoreCase("")) {

                    twoShopCGST.setText("CGST@" + companyResponseList.get(0).getShopCGST() + "%");
                    twoCGST.setText(inr + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
                    twoShopCGSTLayout.setVisibility(View.VISIBLE);

                    threeShopCGST.setText("CGST@" + companyResponseList.get(0).getShopCGST() + "%");
                    threeCGST.setText(inr + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
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

                    twoShopSGST.setText("SGST@" + companyResponseList.get(0).getShopSGST() + "%");
                    twoSGST.setText(inr + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
                    twoShopSGSTLayout.setVisibility(View.VISIBLE);

                    threeShopSGST.setText("SGST@" + companyResponseList.get(0).getShopSGST() + "%");
                    threeSGST.setText(inr + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
                    threeShopSGSTLayout.setVisibility(View.VISIBLE);

                } else {
                    twoShopSGSTLayout.setVisibility(View.GONE);
                    threeShopSGSTLayout.setVisibility(View.GONE);
                }
            } else {
                twoShopSGSTLayout.setVisibility(View.GONE);
                threeShopSGSTLayout.setVisibility(View.GONE);
            }

            discountTxt.setText("Discount(%)\n" + discountAmt);

            subTotalTxt.setText("Sub Total\n" + inr + String.format(Locale.US, "%.2f", subTotalAmt));
            String totalPayableAmount = "Payable Amount<br/><b>" + inr + String.format(Locale.US, "%.2f", totalAmount) + "</b>";
            totalAmountTxt.setText("Total Amount\n" + inr + String.format(Locale.US, "%.2f", totalAmt));
            totalPayableAmountTxt.setText(Html.fromHtml(totalPayableAmount));

            twoSubTotal.setText(inr + String.format(Locale.US, "%.2f", subTotalAmt));

            twoCGST.setText(inr + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
            twoSGST.setText(inr + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
            twoDiscount.setText(inr + String.format(Locale.US, "%.2f", discountAmt));
            twoTotalAmount.setText(inr + String.format(Locale.US, "%.2f", totalAmount));

            threeSubTotal.setText(inr + String.format(Locale.US, "%.2f", subTotalAmt));

            threeCGST.setText(inr + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
            threeSGST.setText(inr + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
            threeDiscount.setText(inr + String.format(Locale.US, "%.2f", discountAmt));
            threeTotalAmount.setText(inr + String.format(Locale.US, "%.2f", totalAmount));

            cartLayout.setVisibility(View.VISIBLE);
            noDataFound.setVisibility(View.GONE);

        } else {
            cartLayout.setVisibility(View.GONE);
            noDataFound.setVisibility(View.VISIBLE);
        }


    }

    public void getCompanyDetails() {

        companyResponseList = posBillingWalaDatabase.getCompanyDetails();

        if (!companyResponseList.isEmpty()) {

            twoShopName.setText(ShopHeaderBuilder.resolveShopName1(companyResponseList.get(0)));
            threeShopName.setText(ShopHeaderBuilder.resolveShopName1(companyResponseList.get(0)));

            String shopDetails = getShopDetails();

            twoShopDetails.setText(shopDetails);
            threeShopDetails.setText(shopDetails);

            if (companyResponseList.get(0).getCompanyLogo() != null) {
                String companyLogo = companyResponseList.get(0).getCompanyLogo();
                // decode base64 string
                byte[] bytes = Base64.decode(companyLogo, Base64.DEFAULT);
                // Initialize bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                twoCompanyLogo.setImageBitmap(bitmap);
                threeCompanyLogo.setImageBitmap(bitmap);
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
                    new WoosimPrnMng(activity, bluetoothAddress, DuplicateBluetoothPrint.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //Company Logo
            if (printerSettingResponseList.get(0).getLogoUse() != null) {
                if (printerSettingResponseList.get(0).getLogoUse().equalsIgnoreCase("on")) {
                    twoCompanyLogo.setVisibility(View.VISIBLE);
                    threeCompanyLogo.setVisibility(View.VISIBLE);
                } else {
                    twoCompanyLogo.setVisibility(View.GONE);
                    threeCompanyLogo.setVisibility(View.GONE);
                }
            } else {
                twoCompanyLogo.setVisibility(View.GONE);
                threeCompanyLogo.setVisibility(View.GONE);
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

            if (printerSettingResponseList.get(0).getInvoiceTermsCondition() != null) {
                twoInvoiceTermsCondition.setText(printerSettingResponseList.get(0).getInvoiceTermsCondition());
                threeInvoiceTermsCondition.setText(printerSettingResponseList.get(0).getInvoiceTermsCondition());
            } else {
                twoInvoiceTermsCondition.setVisibility(View.GONE);
                threeInvoiceTermsCondition.setVisibility(View.GONE);
            }

        } else {
            twoCompanyLogo.setVisibility(View.GONE);
            threeCompanyLogo.setVisibility(View.GONE);
            twoInvoiceTermsCondition.setVisibility(View.GONE);
            threeInvoiceTermsCondition.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            //bluetooth enabled and request for showing available bluetooth devices
            new WoosimPrnMng(activity, "", DuplicateBluetoothPrint.this);
        } else if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == RESULT_OK) {
            //bluetooth device selected and request pairing with device
            String bluetoothAddress = data.getExtras()
                    .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            new WoosimPrnMng(activity, bluetoothAddress, DuplicateBluetoothPrint.this);
        }
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

                        } else {

                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();
        if (cartOrderStatus.equalsIgnoreCase("table_wise")) {

            Intent intent = new Intent(DuplicateBluetoothPrint.this, MainActivity.class);
            intent.putExtra("invoiceRunningStatus", "printBill");
            intent.putExtra("cartOrderStatus", cartOrderStatus);
            startActivity(intent);
            finish();
        } else if (cartOrderStatus.equalsIgnoreCase("take_away")) {

            Intent intent = new Intent(DuplicateBluetoothPrint.this, MainActivity.class);
            intent.putExtra("invoiceRunningStatus", "printBill");
            intent.putExtra("cartOrderStatus", cartOrderStatus);
            startActivity(intent);
            finish();

        } else {

            Intent intent = new Intent(DuplicateBluetoothPrint.this, MainActivity.class);
            intent.putExtra("invoiceRunningStatus", "printBill");
            intent.putExtra("cartOrderStatus", cartOrderStatus);
            startActivity(intent);
            finish();

        }
    }

}