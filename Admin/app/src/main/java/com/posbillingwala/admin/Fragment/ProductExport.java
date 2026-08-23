package com.posbillingwala.admin.Fragment;


import static com.posbillingwala.admin.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.CustomerResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class ProductExport extends Fragment {

    public static Activity activity;
    private final int FILE_SELECTOR_CODE = 10000;
    View view;
    boolean isClicked;
    DownloadManager manager;
    @BindView(R.id.filePath)
    TextView textView;
    Uri uri;
    @BindView(R.id.customerSpinner)
    AppCompatAutoCompleteTextView appCompatAutoCompleteTextView;
    @BindView(R.id.customerProgressBar)
    ProgressBar customerProgressBar;
    List<CustomerResponse> customerResponseList = new ArrayList<>();
    String[] customerIdList, customerNameList;
    String customerId, customerName;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_product_export, container, false);
        ButterKnife.bind(this, view);

        activity = getActivity();
        MainActivity.title.setText("Export Into DB");

        MainActivity.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new Home(), false);
            }
        });

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new Home(), false);
                    return true;
                }
                return false;
            }
        });


        appCompatAutoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                customerId = customerIdList[position];
                customerName = customerNameList[position];
                Log.e("customerId", "" + customerName);
            }
        });

        return view;

    }

    @OnClick({R.id.downloadLayout, R.id.fileUpload, R.id.uploadToServer})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.downloadLayout:
                downloadFile();
                break;
            case R.id.fileUpload:
                galleryIntent();
                break;
            case R.id.uploadToServer:
                if (customerId != null) {
                    if (uri != null) {
                        readExcelFileFromAssets(activity, uri, customerId);
                    } else {
                        Toast.makeText(activity, "Please upload product excel file", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity, "Please select customer", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void createFolder() {

        File myDirectory = new File(directory_path);
        if (!myDirectory.exists()) {
            myDirectory.mkdirs();
        }

    }


    private void downloadFile() {

        Uri uri = Uri.parse("https://www.posbillingwala.com/androidApp/DemoExcel/CustomerProductList.xlsx");
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.allowScanningByMediaScanner();
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOCUMENTS, "/POS Billingwala/CustomerProductList.xlsx");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        long reference = manager.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    String fileLocation = Environment.DIRECTORY_DOCUMENTS + "/POS Billingwala/CustomerProductList.xlsx";
                    Toast.makeText(activity, "File Downloaded Successfully.\n( " + fileLocation + " )", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        activity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

    }

    // This method will help to retrieve the image
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        isClicked = true;
        if (requestCode == FILE_SELECTOR_CODE && resultCode == Activity.RESULT_OK) {
            uri = data.getData();
            if (uri == null) return;
            Log.e("filePath", uri.getPath());
            textView.setText("" + uri.getPath());
        }

    }

    public void readExcelFileFromAssets(Context context, Uri uri, String customerId) {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        try {
            InputStream inStream;
            inStream = context.getContentResolver().openInputStream(uri);
            try {
                XSSFWorkbook workbook = new XSSFWorkbook(inStream);
                XSSFSheet sheet = workbook.getSheetAt(0);
                // We now need something to iterate through the cells.
                Iterator<Row> rowIter = sheet.rowIterator();
                int rowno = 0;
                while (rowIter.hasNext()) {
                    XSSFRow myRow = (XSSFRow) rowIter.next();
                    if (rowno != 0) {
                        Iterator<Cell> cellIter = myRow.cellIterator();
                        int colno = 0;
                        String product = "", category = "", unit = "", price = "", cgst = "", sgst = "";
                        while (cellIter.hasNext()) {
                            XSSFCell myCell = (XSSFCell) cellIter.next();
                            if (colno == 0) {
                                product = myCell.toString();
                            } else if (colno == 1) {
                                category = myCell.toString();
                            } else if (colno == 2) {
                                unit = myCell.toString();
                            } else if (colno == 3) {
                                price = myCell.toString();
                            } else if (colno == 4) {
                                cgst = myCell.toString();
                            } else if (colno == 5) {
                                sgst = myCell.toString();
                            }
                            colno++;
                        }

                        String cellDetails = product + " " + category + " " + unit + " " + price + " " + cgst + " " + sgst;
                        Log.e("cellDetails", "" + cellDetails);

                        Call<AllApiResponse> call = Api.getClient().insertExportProduct(customerId, category, product, unit, price, cgst, sgst);
                        call.enqueue(new Callback<AllApiResponse>() {
                            @Override
                            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                                if (response.isSuccessful()) {
                                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                                        Log.e("insertProduct", "" + response.body().getMessage());
                                    } else {
                                        Log.e("insertFailedProduct", "" + response.body().getMessage());
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                                Log.e("serverError", "" + t.getMessage());
                            }
                        });

                    }
                    rowno++;
                }

                pDialog.dismiss();

            } catch (Exception e) {
                e.printStackTrace();
                pDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
            pDialog.dismiss();
        }
    }

    private void galleryIntent() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*");
        startActivityForResult(intent, FILE_SELECTOR_CODE);

    }

    public void onStart() {
        super.onStart();
        Log.e("onStart", "called");
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(0);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            requestPermission();
            getCustomerList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }


    private void getCustomerList() {

        customerProgressBar.setVisibility(View.VISIBLE);

        customerResponseList.clear();

        Call<AllApiResponse> call = Api.getClient().getCustomerList();
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    customerResponseList = response.body().getCustomerResponseList();
                    Log.e("customerResponseList", "" + customerResponseList.size());
                    if (customerResponseList.size() > 0) {
                        customerProgressBar.setVisibility(View.GONE);

                        customerIdList = new String[customerResponseList.size()];
                        customerNameList = new String[customerResponseList.size()];

                        for (int i = 0; i < customerResponseList.size(); i++) {
                            customerIdList[i] = customerResponseList.get(i).getId();
                            customerNameList[i] = customerResponseList.get(i).getName() + "\n[" + customerResponseList.get(i).getShopName() + "]";
                        }

                        try {
                            final ArrayAdapter adapter = new ArrayAdapter(activity, R.layout.spinner_item_layout, customerNameList);
                            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                            appCompatAutoCompleteTextView.setAdapter(adapter);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    } else {
                        customerProgressBar.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                customerProgressBar.setVisibility(View.VISIBLE);
                Log.e("serverError", "" + t.getMessage());
            }
        });

    }

    public void requestPermission() {

        Dexter.withContext(activity).withPermissions(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                // check if all permissions are granted
                if (report.areAllPermissionsGranted()) {
                    createFolder();
                }
                // check for permanent denial of any permission
                if (report.isAnyPermissionPermanentlyDenied()) {
                    // show alert dialog navigating to Settings
                    showSettingsDialog();
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                token.continuePermissionRequest();
            }
        }).withErrorListener(new PermissionRequestErrorListener() {
            @Override
            public void onError(DexterError error) {
                Toast.makeText(activity, "Error occurred! ", Toast.LENGTH_SHORT).show();
            }
        }).onSameThread().check();

    }

    private void showSettingsDialog() {

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
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

}