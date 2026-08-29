package com.posbillingwala.dealer.Fragment;


import static com.posbillingwala.dealer.Extra.Common.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Extra.BottomSheetUi;
import com.posbillingwala.dealer.Extra.DetectConnection;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.Model.CatalogImportError;
import com.posbillingwala.dealer.Model.CatalogImportPreviewResponse;
import com.posbillingwala.dealer.Model.CatalogImportSummary;
import com.posbillingwala.dealer.Model.CustomerResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.Utils.CatalogFileHelper;
import com.posbillingwala.dealer.databinding.FragmentProductExportBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class ProductExport extends Fragment implements View.OnClickListener {

    public static Activity activity;
    private final int FILE_SELECTOR_CODE = 10000;
    View view;
    FragmentProductExportBinding binding;
    boolean isClicked;
    List<CustomerResponse> customerResponseList = new ArrayList<>();
    String[] customerIdList, customerNameList;
    String customerId, customerName;
    String presetCustomerId;
    String presetImportType;
    Uri uri;
    String selectedFileName = "";
    String selectedImportType = "products";
    final String[] importTypeValues = {"products", "categories", "subcategories", "portions"};
    final String[] importTypeLabels = {"Products", "Categories", "Sub Categories", "Portions"};
    CatalogImportPreviewResponse lastPreview;
    SweetAlertDialog progressDialog;
    boolean importInProgress;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProductExportBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        if (MainActivity.title != null
                && (MainActivity.title.getText() == null
                || MainActivity.title.getText().toString().trim().isEmpty())) {
            MainActivity.title.setText("Product Import / Export");
        }

        MainActivity.back.setOnClickListener(v ->
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack());

        binding.customerSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                customerId = customerIdList[position];
                customerName = customerNameList[position];
            }
        });

        binding.importTypeSpinner.setItems(importTypeLabels);
        binding.importTypeSpinner.setSelectedIndex(0);
        binding.importTypeSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                selectedImportType = importTypeValues[position];
                updateTypeLabels();
            }
        });
        updateTypeLabels();

        binding.downloadLayout.setOnClickListener(this);
        binding.exportLayout.setOnClickListener(this);
        binding.fileUpload.setOnClickListener(this);
        binding.uploadToServer.setOnClickListener(this);
        binding.viewHistoryLayout.setOnClickListener(v -> openImportHistory());

        Bundle bundle = getArguments();
        if (bundle != null) {
            presetCustomerId = bundle.getString("customerId");
            presetImportType = bundle.getString("importType");
            if (presetImportType != null) {
                selectedImportType = presetImportType;
                for (int i = 0; i < importTypeValues.length; i++) {
                    if (importTypeValues[i].equals(selectedImportType)) {
                        binding.importTypeSpinner.setSelectedIndex(i);
                        break;
                    }
                }
                updateTypeLabels();
            }
        }

        return view;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.downloadLayout) {
            downloadTemplate();
        } else if (id == R.id.exportLayout) {
            exportProducts();
        } else if (id == R.id.fileUpload) {
            galleryIntent();
        } else if (id == R.id.uploadToServer) {
            validateImport();
        }
    }

    private void openImportHistory() {
        if (customerId == null) {
            Toast.makeText(activity, "Please select customer", Toast.LENGTH_SHORT).show();
            return;
        }
        CatalogImportHistory history = new CatalogImportHistory();
        Bundle bundle = new Bundle();
        bundle.putString("customerId", customerId);
        bundle.putString("importType", selectedImportType);
        bundle.putString("typeLabel", getSelectedTypeLabel());
        history.setArguments(bundle);
        ((MainActivity) activity).loadFragment(history, true);
    }

    private void updateTypeLabels() {
        String label = getSelectedTypeLabel();
        binding.downloadLabel.setText("Download " + label + " Demo Excel");
        binding.exportLabel.setText("Export " + label);
    }

    private String getSelectedTypeLabel() {
        for (int i = 0; i < importTypeValues.length; i++) {
            if (importTypeValues[i].equals(selectedImportType)) {
                return importTypeLabels[i];
            }
        }
        return "Products";
    }

    private void downloadTemplate() {
        if (customerId == null) {
            Toast.makeText(activity, "Please select customer", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress("Preparing demo Excel...");
        CatalogFileHelper.downloadCatalogFile(
                activity,
                "catalogImportTemplate.php?customerId=" + customerId + "&type=" + selectedImportType,
                selectedImportType + "_template.xlsx",
                new CatalogFileHelper.DownloadCallback() {
                    @Override
                    public void onSuccess(File savedFile) {
                        activity.runOnUiThread(() -> {
                            dismissProgress();
                            Toast.makeText(activity, "Demo Excel downloaded:\n" + savedFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        activity.runOnUiThread(() -> {
                            dismissProgress();
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void exportProducts() {
        if (customerId == null) {
            Toast.makeText(activity, "Please select customer", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress("Exporting products...");
        CatalogFileHelper.downloadCatalogFile(
                activity,
                "catalogExport.php?customerId=" + customerId + "&type=" + selectedImportType,
                selectedImportType + "_export.xlsx",
                new CatalogFileHelper.DownloadCallback() {
                    @Override
                    public void onSuccess(File savedFile) {
                        activity.runOnUiThread(() -> {
                            dismissProgress();
                            Toast.makeText(activity, "Products exported successfully.\n" + savedFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        activity.runOnUiThread(() -> {
                            dismissProgress();
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void validateImport() {
        if (customerId == null) {
            Toast.makeText(activity, "Please select customer", Toast.LENGTH_SHORT).show();
            return;
        }
        if (uri == null) {
            Toast.makeText(activity, "Please select a valid Excel (.xlsx) file.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress("Uploading & validating...");
        try {
            File tempFile = copyUriToTempFile(uri);
            RequestBody customerBody = RequestBody.create(customerId, MediaType.parse("text/plain"));
            RequestBody importTypeBody = RequestBody.create(selectedImportType, MediaType.parse("text/plain"));
            RequestBody fileBody = RequestBody.create(tempFile, MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("import_file", selectedFileName, fileBody);

            Call<CatalogImportPreviewResponse> call = Api.getClient().catalogImportValidate(customerBody, importTypeBody, filePart);
            call.enqueue(new Callback<CatalogImportPreviewResponse>() {
                @Override
                public void onResponse(Call<CatalogImportPreviewResponse> call, Response<CatalogImportPreviewResponse> response) {
                    dismissProgress();
                    if (response.isSuccessful() && response.body() != null) {
                        lastPreview = response.body();
                        showPreviewDialog(lastPreview);
                    } else {
                        Toast.makeText(activity, "Unable to validate file. Please check the template.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<CatalogImportPreviewResponse> call, Throwable t) {
                    dismissProgress();
                    Toast.makeText(activity, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            dismissProgress();
            Toast.makeText(activity, "Please select a valid Excel (.xlsx) file.", Toast.LENGTH_SHORT).show();
        }
    }

    private File copyUriToTempFile(Uri fileUri) throws Exception {
        InputStream inputStream = activity.getContentResolver().openInputStream(fileUri);
        if (inputStream == null) {
            throw new Exception("Unable to read file");
        }
        selectedFileName = queryDisplayName(fileUri);
        if (selectedFileName == null || selectedFileName.isEmpty()) {
            selectedFileName = "import.xlsx";
        }
        File tempFile = new File(activity.getCacheDir(), "catalog_import_" + System.currentTimeMillis() + ".xlsx");
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        outputStream.close();
        inputStream.close();
        return tempFile;
    }

    private String queryDisplayName(Uri fileUri) {
        try (android.database.Cursor cursor = activity.getContentResolver().query(fileUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void showPreviewDialog(CatalogImportPreviewResponse preview) {
        CatalogImportSummary summary = preview.getSummary();
        if (summary == null) {
            Toast.makeText(activity, preview.getMessage() != null ? preview.getMessage() : "Validation failed", Toast.LENGTH_LONG).show();
            return;
        }

        View sheetView = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_catalog_import_preview, null);
        BottomSheetDialog sheet = new BottomSheetDialog(activity);
        sheet.setContentView(sheetView);

        TextView titleView = sheetView.findViewById(R.id.previewTitle);
        TextView summaryView = sheetView.findViewById(R.id.previewSummary);
        TextView errorsLabel = sheetView.findViewById(R.id.previewErrorsLabel);
        View errorsScroll = sheetView.findViewById(R.id.previewErrorsScroll);
        TextView errorsView = sheetView.findViewById(R.id.previewErrors);
        TextView btnCancel = sheetView.findViewById(R.id.btnPreviewCancel);
        TextView btnErrorExcel = sheetView.findViewById(R.id.btnPreviewErrorExcel);
        TextView btnImport = sheetView.findViewById(R.id.btnPreviewImport);

        titleView.setText("Import Preview");
        summaryView.setText("Customer: " + (preview.getCustomerName() != null ? preview.getCustomerName() : customerName)
                + "\nTotal: " + summary.getTotal()
                + "\nValid: " + summary.getValid()
                + "\nNew: " + summary.getNewCount()
                + "\nUpdates: " + summary.getUpdated()
                + "\nErrors: " + summary.getErrors());

        if (preview.getErrors() != null && !preview.getErrors().isEmpty()) {
            StringBuilder errors = new StringBuilder();
            int shown = 0;
            for (CatalogImportError error : preview.getErrors()) {
                errors.append("Row ").append(error.getRow()).append(": ").append(error.getMessage());
                if (++shown >= 10) {
                    errors.append("\n...");
                    break;
                }
                errors.append("\n");
            }
            errorsLabel.setVisibility(View.VISIBLE);
            errorsScroll.setVisibility(View.VISIBLE);
            errorsView.setText(errors.toString().trim());
        }

        sheetView.findViewById(R.id.closePreviewSheet).setOnClickListener(v -> sheet.dismiss());
        btnCancel.setOnClickListener(v -> sheet.dismiss());

        if (summary.getErrors() > 0 && preview.getImportSessionId() != null) {
            btnErrorExcel.setVisibility(View.VISIBLE);
            btnErrorExcel.setOnClickListener(v -> {
                sheet.dismiss();
                downloadErrorExcel(preview.getImportSessionId());
            });
        }
        if (summary.getValid() > 0 && preview.getImportSessionId() != null) {
            btnImport.setVisibility(View.VISIBLE);
            btnImport.setText("Import " + summary.getValid());
            btnImport.setOnClickListener(v -> {
                sheet.dismiss();
                confirmImport(preview);
            });
        }

        sheet.setOnShowListener(d -> {
            View bottomSheet = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        sheet.show();
        BottomSheetUi.applyFullWidth(sheet);
    }

    private void downloadErrorExcel(String sessionId) {
        showProgress("Downloading error report...");
        CatalogFileHelper.downloadCatalogFile(
                activity,
                "catalogImportErrorExcel.php?customerId=" + customerId + "&importSessionId=" + sessionId,
                "Import_Errors.xlsx",
                new CatalogFileHelper.DownloadCallback() {
                    @Override
                    public void onSuccess(File savedFile) {
                        activity.runOnUiThread(() -> {
                            dismissProgress();
                            Toast.makeText(activity, "Error Excel saved:\n" + savedFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        activity.runOnUiThread(() -> {
                            dismissProgress();
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void confirmImport(CatalogImportPreviewResponse preview) {
        if (importInProgress || preview.getImportSessionId() == null) {
            return;
        }
        importInProgress = true;
        showProgress("Importing...");

        Call<CatalogImportPreviewResponse> call = Api.getClient().catalogImportConfirm(customerId, preview.getImportSessionId());
        call.enqueue(new Callback<CatalogImportPreviewResponse>() {
            @Override
            public void onResponse(Call<CatalogImportPreviewResponse> call, Response<CatalogImportPreviewResponse> response) {
                importInProgress = false;
                dismissProgress();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    CatalogImportSummary summary = response.body().getSummary();
                    int total = summary != null ? summary.getCreated() + summary.getUpdated() : 0;
                    new SweetAlertDialog(activity, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Import Complete")
                            .setContentText(total + " " + getSelectedTypeLabel().toLowerCase() + " imported successfully.")
                            .show();
                } else {
                    Toast.makeText(activity, "Unable to import the catalog. Please try again.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<CatalogImportPreviewResponse> call, Throwable t) {
                importInProgress = false;
                dismissProgress();
                Toast.makeText(activity, "Import failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showProgress(String message) {
        dismissProgress();
        progressDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        progressDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        progressDialog.setTitleText(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismissWithAnimation();
        }
    }

    private void createFolder() {
        File myDirectory = new File(directory_path);
        if (!myDirectory.exists()) {
            myDirectory.mkdirs();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        isClicked = true;
        if (requestCode == FILE_SELECTOR_CODE && resultCode == Activity.RESULT_OK) {
            uri = data.getData();
            if (uri == null) return;
            selectedFileName = queryDisplayName(uri);
            binding.filePath.setText(selectedFileName != null ? selectedFileName : uri.getLastPathSegment());
        }
    }

    private void galleryIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, FILE_SELECTOR_CODE);
    }

    public void onStart() {
        super.onStart();
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
        binding.customerProgressBar.setVisibility(View.VISIBLE);
        customerResponseList.clear();

        Call<AllApiResponse> call = Api.getClient().getCustomerList(MainActivity.userId, "customer");
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    customerResponseList = response.body().getCustomerResponseList();
                    if (customerResponseList.size() > 0) {
                        binding.customerProgressBar.setVisibility(View.GONE);
                        customerIdList = new String[customerResponseList.size()];
                        customerNameList = new String[customerResponseList.size()];
                        for (int i = 0; i < customerResponseList.size(); i++) {
                            customerIdList[i] = customerResponseList.get(i).getId();
                            customerNameList[i] = customerResponseList.get(i).getName() + "\n[" + customerResponseList.get(i).getShopName() + "]";
                        }
                        try {
                            ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, customerNameList);
                            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                            binding.customerSpinner.setAdapter(adapter);
                            if (presetCustomerId != null) {
                                for (int i = 0; i < customerIdList.length; i++) {
                                    if (presetCustomerId.equals(customerIdList[i])) {
                                        binding.customerSpinner.setSelectedIndex(i);
                                        customerId = customerIdList[i];
                                        customerName = customerNameList[i];
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e("ProductExport", "spinner", e);
                        }
                    } else {
                        binding.customerProgressBar.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                binding.customerProgressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    public void requestPermission() {
        Dexter.withContext(activity).withPermissions(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new MultiplePermissionsListener() {
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
        }).withErrorListener(new PermissionRequestErrorListener() {
            @Override
            public void onError(DexterError error) {
                Toast.makeText(activity, "Error occurred!", Toast.LENGTH_SHORT).show();
            }
        }).onSameThread().check();
    }

    private void showSettingsDialog() {
        BottomSheetUi.showConfirm(activity, "Need Permissions",
                "This app needs permission to use this feature. You can grant them in app settings.",
                "GOTO SETTINGS", "Cancel", true, this::openSettings);
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri settingsUri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(settingsUri);
        startActivityForResult(intent, 101);
    }
}
