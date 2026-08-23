package com.posbillingwala.owner.Fragment;

import static com.posbillingwala.owner.Utils.RequestCodes.directory_path;

import android.Manifest;
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
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentProductExportBinding;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductExport extends Fragment {

    public static Activity activity;
    public final int FILE_SELECTOR_CODE = 10000;
    public FragmentProductExportBinding binding;
    public boolean isClicked;
    public DownloadManager manager;
    public Uri uri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentProductExportBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        activity = getActivity();

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new UserSetting(), true);
                    return true;
                }
                return false;
            }
        });

        binding.backToSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new UserSetting(), true);
            }
        });

        binding.downloadLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadFile();
            }
        });

        binding.fileUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                galleryIntent();
            }
        });

        binding.uploadToServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uri != null) {
                    readExcelFileFromAssets(activity, uri, MainActivity.userId);
                } else {
                    Toast.makeText(activity, "Please upload product excel file", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    public void createFolder() {
        File myDirectory = new File(directory_path);
        if (!myDirectory.exists()) {
            myDirectory.mkdirs();
        }
    }

    public void downloadFile() {
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        isClicked = true;
        if (requestCode == FILE_SELECTOR_CODE && resultCode == Activity.RESULT_OK) {
            uri = data.getData();
            if (uri == null) return;
            binding.filePath.setText(uri.getPath());
        }
    }

    public void readExcelFileFromAssets(Context context, Uri uri, String customerId) {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        try {
            InputStream inStream = context.getContentResolver().openInputStream(uri);
            try {
                XSSFWorkbook workbook = new XSSFWorkbook(inStream);
                XSSFSheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIter = sheet.rowIterator();
                int rowno = 0;
                while (rowIter.hasNext()) {
                    XSSFRow myRow = (XSSFRow) rowIter.next();
                    if (rowno != 0) {
                        Iterator<Cell> cellIter = myRow.cellIterator();
                        int colno = 0;
                        String productCode = "", product = "", category = "", unit = "", price = "", cgst = "", sgst = "";
                        while (cellIter.hasNext()) {
                            XSSFCell myCell = (XSSFCell) cellIter.next();
                            switch (colno) {
                                case 0:
                                    productCode = myCell.toString();
                                    break;
                                case 1:
                                    product = myCell.toString();
                                    break;
                                case 2:
                                    category = myCell.toString();
                                    break;
                                case 3:
                                    unit = myCell.toString();
                                    break;
                                case 4:
                                    price = myCell.toString();
                                    break;
                                case 5:
                                    cgst = myCell.toString();
                                    break;
                                case 6:
                                    sgst = myCell.toString();
                                    break;
                            }
                            colno++;
                        }

                        String cellDetails = productCode + " " + product + " " + category + " " + unit + " " + price + " " + cgst + " " + sgst;
                        Log.e("cellDetails", cellDetails);

                        Call<AllApiResponse> call = Api.getClient().insertExportProduct(customerId, category, productCode, product, unit, price, cgst, sgst);
                        call.enqueue(new Callback<AllApiResponse>() {
                            @Override
                            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                                if (response.isSuccessful()) {
                                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                                        Log.e("insertProduct", response.body().getMessage());
                                    } else {
                                        Log.e("insertFailedProduct", response.body().getMessage());
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                                Log.e("serverError", t.getMessage());
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

    public void galleryIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*");
        startActivityForResult(intent, FILE_SELECTOR_CODE);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e("onStart", "called");
        requestPermission();
    }

    public void requestPermission() {
        Dexter.withContext(activity).withPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            createFolder();
                        }
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(activity, "Error occurred! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
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

    public void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }
}
