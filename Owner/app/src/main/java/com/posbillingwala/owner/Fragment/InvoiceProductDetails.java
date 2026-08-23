package com.posbillingwala.owner.Fragment;


import static com.posbillingwala.owner.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
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
import android.provider.Settings;
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Adapter.InvoiceProductAdapter;
import com.posbillingwala.owner.BuildConfig;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.InvoiceProductResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentInvoiceProductDetailsBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class InvoiceProductDetails extends Fragment implements View.OnClickListener {

    public static List<InvoiceProductResponse> invoiceProductResponseList = new ArrayList<>();
    public static InvoiceProductAdapter adapter;
    public static String invoiceId;
    public static TextView twoShopName, twoShopDetails, twoInvoiceDetails, twoSubTotal, twoShopCGST, twoCGST,
            twoShopSGST, twoSGST, twoDiscount, twoTotalAmount;
    public static LinearLayout twoShopCGSTLayout, twoShopSGSTLayout;
    public static RecyclerView twoRecyclerView;
    public static NestedScrollView twoNestedScrollView;
    public static Activity activity;
    public static String inr;
    View view;
    FragmentInvoiceProductDetailsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentInvoiceProductDetailsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        activity = getActivity();

        Bundle bundle = getArguments();
        if (bundle != null) {
            invoiceId = bundle.getString("invoiceId");
        }

        initViews();

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new OrderInvoice(), true);
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
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new OrderInvoice(), true);
        } else if (id == R.id.shareIcon) {
            if (!invoiceProductResponseList.isEmpty()) {
                createPdf();
            }
        }
    }

    public void initViews() {

        //***************** 2 Inch Printer Start ******************//
        twoShopName = view.findViewById(R.id.twoShopName);
        twoShopDetails = view.findViewById(R.id.twoShopDetails);
        twoInvoiceDetails = view.findViewById(R.id.twoInvoiceDetails);
        twoSubTotal = view.findViewById(R.id.twoSubTotal);
        twoShopCGST = view.findViewById(R.id.twoShopCGST);
        twoCGST = view.findViewById(R.id.twoCGST);
        twoShopSGST = view.findViewById(R.id.twoShopSGST);
        twoSGST = view.findViewById(R.id.twoSGST);
        twoDiscount = view.findViewById(R.id.twoDiscount);
        twoTotalAmount = view.findViewById(R.id.twoTotalAmount);
        twoShopCGSTLayout = view.findViewById(R.id.twoShopCGSTLayout);
        twoShopSGSTLayout = view.findViewById(R.id.twoShopSGSTLayout);
        twoRecyclerView = view.findViewById(R.id.twoRecyclerView);
        twoNestedScrollView = view.findViewById(R.id.twoNestedScrollView);
        //***************** 2 Inch Printer End ******************//

    }

    public void createPdf() {

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        String invoiceNumber = invoiceProductResponseList.get(0).getInvoiceNumber();
        String[] separated = invoiceNumber.split("/");
        try {
            invoiceNumber = separated[2];
            invoiceNumber = "SalesInvoice_" + invoiceNumber;
        } catch (Exception e) {
            e.printStackTrace();
            invoiceNumber = separated[1];
            invoiceNumber = "SalesInvoice_" + invoiceNumber;
        }

        String BillDetails = "";
        if (invoiceProductResponseList.get(0).getInvoiceType().equalsIgnoreCase("table_wise")) {
            BillDetails = "<b>Bill No:</b> " + invoiceProductResponseList.get(0).getInvoiceNumber() + "<br/><b>Date:</b> " + invoiceProductResponseList.get(0).getInvoiceDate() + "<br/><b>Table No:</b> " + invoiceProductResponseList.get(0).getNoOfTable();
        } else {
            BillDetails = "<b>Bill No:</b> " + invoiceProductResponseList.get(0).getInvoiceNumber() + "<br/><b>Date:</b> " + invoiceProductResponseList.get(0).getInvoiceDate();
        }

        twoInvoiceDetails.setText(Html.fromHtml(BillDetails));

        Bitmap bitmap = convertLayout(twoNestedScrollView);

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

        startActivity(Intent.createChooser(intentShareFile, "Share Invoice"));

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
        int reqWidth = (int) Math.round(effectivePrintWidth * 8);
        int width = bm.getWidth();
        int height = bm.getHeight();
        if (width == reqWidth) {
            return bm;
        } else if (width < reqWidth && width > 16) {
            int diff = width % 8;
            if (diff != 0) {
                newWidth = width - diff;
                newHeight = (int) (width - diff) * height / width;
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
            newHeight = (int) reqWidth * height / width;
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
        if (DetectConnection.checkInternetConnection(activity)) {
            getInvoiceProductList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
        requestPermission();
    }


    public void getInvoiceProductList() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getInvoiceProductList(invoiceId);
        call.enqueue(new Callback<AllApiResponse>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    invoiceProductResponseList.clear();
                    invoiceProductResponseList = response.body().getInvoiceProductResponseList();

                    float totalPerProductAmount = 0f, totalCGST = 0f, totalSGST = 0f, totalUnitPrice = 0f, totalGST = 0f, totalPerProductGST = 0f;
                    int totalQty = 0;
                    String discountType = "";

                    if (!invoiceProductResponseList.isEmpty()) {

                        inr = invoiceProductResponseList.get(0).getCurrencyName();

                        twoRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
                        twoRecyclerView.setHasFixedSize(true);
                        adapter = new InvoiceProductAdapter(activity, invoiceProductResponseList);
                        twoRecyclerView.setAdapter(adapter);

                        String BillDetails = "";
                        if (invoiceProductResponseList.get(0).getInvoiceType().equalsIgnoreCase("table_wise")) {
                            BillDetails = "<b>Bill No:</b> " + invoiceProductResponseList.get(0).getInvoiceNumber() + "<br/><b>Date:</b> " + invoiceProductResponseList.get(0).getInvoiceDate() + "<br/><b>Table No:</b> " + invoiceProductResponseList.get(0).getNoOfTable();
                        } else {
                            BillDetails = "<b>Bill No:</b> " + invoiceProductResponseList.get(0).getInvoiceNumber() + "<br/><b>Date:</b> " + invoiceProductResponseList.get(0).getInvoiceDate();
                        }

                        twoInvoiceDetails.setText(Html.fromHtml(BillDetails));

                        twoShopName.setText(invoiceProductResponseList.get(0).getCompanyName());

                        String shopDetails = "";
                        if (invoiceProductResponseList.get(0).getGstStatus() != null) {
                            if (invoiceProductResponseList.get(0).getGstStatus().equalsIgnoreCase("on")) {
                                shopDetails = invoiceProductResponseList.get(0).getCompanyAddress() + "\n" + "GSTIN: " + invoiceProductResponseList.get(0).getGstNumber();

                            } else if (invoiceProductResponseList.get(0).getGstStatus().equalsIgnoreCase("off")) {
                                shopDetails = invoiceProductResponseList.get(0).getCompanyAddress();
                            }
                        } else {
                            shopDetails = invoiceProductResponseList.get(0).getCompanyAddress();
                        }

                        if (null != invoiceProductResponseList.get(0).getCompanyFssis() && (invoiceProductResponseList.get(0).getCompanyFssis().length() > 0) && !(invoiceProductResponseList.get(0).getCompanyFssis().isEmpty())) {
                            shopDetails = shopDetails + "FSSAI No: " + invoiceProductResponseList.get(0).getCompanyFssis();
                        }

                        twoShopDetails.setText(shopDetails);

                        twoShopCGST.setText("CGST @" + invoiceProductResponseList.get(0).getShopCGST() + "%");
                        twoShopSGST.setText("SGST @" + invoiceProductResponseList.get(0).getShopSGST() + "%");

                        for (InvoiceProductResponse invoiceProductResponse : invoiceProductResponseList) {

                            discountType = invoiceProductResponseList.get(0).getDiscountType();

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

                        float discountAmt = Float.parseFloat(invoiceProductResponseList.get(0).getDiscount());
                        float totalShopGST = Float.parseFloat(invoiceProductResponseList.get(0).getTotalGSTAmount());

                        Log.e("discountAmt", "" + discountAmt);
                        Log.e("discountType", "" + discountType);

                        if (discountType != null) {
                            if (discountType.equalsIgnoreCase("Amount")) {
                                discountAmt = discountAmt;
                            } else {
                                discountAmt = subTotalAmt / (100 / discountAmt);
                            }
                        } else {
                            discountAmt = subTotalAmt / (100 / discountAmt);
                        }

                        Log.e("subTotalAmt", "" + subTotalAmt);
                        Log.e("discountAmt", "" + discountAmt);
                        Log.e("totalShopGST", "" + totalShopGST);
                        float totalAmt = 0f;
                        if (invoiceProductResponseList.get(0).getGstStatus().equalsIgnoreCase("on")) {
                            totalAmt = (subTotalAmt - discountAmt) + totalShopGST;
                        } else {
                            totalAmt = subTotalAmt - discountAmt;
                        }
                        Log.e("totalAmt", "" + totalAmt);

                        twoSubTotal.setText(inr + String.format(Locale.US, "%.2f", subTotalAmt));

                        twoCGST.setText(inr + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
                        twoSGST.setText(inr + String.format(Locale.US, "%.2f", (totalShopGST / 2)));
                        twoDiscount.setText(inr + String.format(Locale.US, "%.2f", discountAmt));
                        twoTotalAmount.setText(inr + String.format(Locale.US, "%.2f", totalAmt));


                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Log.e("serverError", t.getMessage());
            }
        });

    }

    public void createFolder() {

        File myDirectory = new File(directory_path);
        if (!myDirectory.exists()) {
            myDirectory.mkdirs();
        }

    }

    public void requestPermission() {

        Dexter.withContext(activity)
                .withPermissions(Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT
                ).withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            createFolder();
                        } else {

                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

    }

    public void showSettingsDialog() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                openSettings();
            }

        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();

    }

    // navigating user to app settings
    public void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }


}