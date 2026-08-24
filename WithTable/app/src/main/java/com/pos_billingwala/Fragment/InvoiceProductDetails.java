package com.pos_billingwala.Fragment;

import static com.pos_billingwala.Utils.RequestCodes.directory_path;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.InvoiceProductAdapter;
import com.pos_billingwala.BuildConfig;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ShopHeaderBuilder;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentInvoiceProductDetailsBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@SuppressLint("SetTextI18n")
public class InvoiceProductDetails extends Fragment implements View.OnClickListener {

    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static List<InvoiceProductResponse> invoiceProductResponseList = new ArrayList<>();
    public static List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
    public static List<CompanyResponse> companyResponseList = new ArrayList<>();
    public static List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    public static InvoiceProductAdapter adapter;
    public static String invoiceId;
    public static TextView invoiceShopName, invoiceShopDetails, invoiceInvoiceDetails, invoiceSubTotal, invoiceShopCGST, invoiceCGST,
            invoiceShopSGST, invoiceSGST, invoiceDiscount, invoiceTotalAmount;
    public static LinearLayout invoiceShopCGSTLayout, invoiceShopSGSTLayout;
    public static RecyclerView invoiceRecyclerView;
    public static NestedScrollView invoiceNestedScrollView;
    public static Activity activity;
    View view;
    FragmentInvoiceProductDetailsBinding binding;


    @NonNull
    public static String getBillDetails() {
        String BillDetails = "";
        if (invoiceResponseList.get(0).getInvoiceType().equalsIgnoreCase("table_wise")) {
            BillDetails = "<b>Bill No:</b> " + invoiceResponseList.get(0).getInvoiceNumber() + "<br/><b>Date:</b> " + invoiceResponseList.get(0).getInvoiceDate() + "<br/><b>Table No:</b> " + invoiceResponseList.get(0).getNoOfTable();
        } else {
            BillDetails = "<b>Bill No:</b> " + invoiceResponseList.get(0).getInvoiceNumber() + "<br/><b>Date:</b> " + invoiceResponseList.get(0).getInvoiceDate();
        }
        return BillDetails;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInvoiceProductDetailsBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();


        Bundle bundle = getArguments();
        if (bundle != null) {
            invoiceId = bundle.getString("invoiceId");
        }

        initViews();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).goBackTo(new OrderInvoice(), true);
                    return true;
                }
                return false;
            }
        });

        binding.backToInvoice.setOnClickListener(this);
        binding.shareIcon.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToInvoice) {
            ((MainActivity) activity).goBackTo(new OrderInvoice(), true);
        } else if (id == R.id.shareIcon) {
            createPdf();
        }
    }

    public void initViews() {

        //***************** 2 Inch Printer Start ******************//
        invoiceShopName = view.findViewById(R.id.invoiceShopName);
        invoiceShopDetails = view.findViewById(R.id.invoiceShopDetails);
        invoiceInvoiceDetails = view.findViewById(R.id.invoiceInvoiceDetails);
        invoiceSubTotal = view.findViewById(R.id.invoiceSubTotal);
        invoiceShopCGST = view.findViewById(R.id.invoiceShopCGST);
        invoiceCGST = view.findViewById(R.id.invoiceCGST);
        invoiceShopSGST = view.findViewById(R.id.invoiceShopSGST);
        invoiceSGST = view.findViewById(R.id.invoiceSGST);
        invoiceDiscount = view.findViewById(R.id.invoiceDiscount);
        invoiceTotalAmount = view.findViewById(R.id.invoiceTotalAmount);
        invoiceShopCGSTLayout = view.findViewById(R.id.invoiceShopCGSTLayout);
        invoiceShopSGSTLayout = view.findViewById(R.id.invoiceShopSGSTLayout);
        invoiceRecyclerView = view.findViewById(R.id.invoiceRecyclerView);
        invoiceNestedScrollView = view.findViewById(R.id.invoiceNestedScrollView);
        //***************** 2 Inch Printer End ******************//

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

        Bitmap bitmap = convertLayout(invoiceNestedScrollView);

        if (bitmap != null) {

            try {

                Bitmap bitmap1 = getResizedBitmap(bitmap, 48);

                File file = new File(directory_path + "/" + invoiceNumber + ".png");
                FileOutputStream out = new FileOutputStream(file);
                bitmap1.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            openGeneratedPDF(invoiceNumber);

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

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getCompanyDetails();
        getPrinterSettingDetails();
        getInvoiceDetails();
    }

    public void getInvoiceDetails() {

        invoiceResponseList = posBillingWalaDatabase.getInvoiceDetails(invoiceId);
        if (!invoiceResponseList.isEmpty()) {

            String BillDetails = getBillDetails();

            invoiceInvoiceDetails.setText(Html.fromHtml(BillDetails));

            getInvoiceProductDetails(invoiceResponseList.get(0).getInvoiceNumber());

        }

    }

    public void getCompanyDetails() {
        companyResponseList = posBillingWalaDatabase.getCompanyDetails();
        if (!companyResponseList.isEmpty()) {

            invoiceShopName.setText(ShopHeaderBuilder.resolveShopName1(companyResponseList.get(0)));
            invoiceShopDetails.setText(ShopHeaderBuilder.buildShopDetailsBlock(companyResponseList.get(0)));

            invoiceShopCGST.setText("CGST @" + companyResponseList.get(0).getShopCGST() + "%");
            invoiceShopSGST.setText("SGST @" + companyResponseList.get(0).getShopSGST() + "%");

        }
    }

    public void getPrinterSettingDetails() {
        printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();

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
            if (companyResponseList.get(0).getGstStatus().equalsIgnoreCase("on")) {
                totalAmt = (subTotalAmt - discountAmt) + totalShopGST;
            } else {
                totalAmt = subTotalAmt - discountAmt;
            }
            invoiceSubTotal.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", subTotalAmt));

            invoiceCGST.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
            invoiceSGST.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
            invoiceDiscount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", discountAmt));
            invoiceTotalAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalAmt));

        }


    }


}