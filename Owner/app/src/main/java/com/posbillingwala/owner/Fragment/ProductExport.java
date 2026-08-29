package com.posbillingwala.owner.Fragment;

import static com.posbillingwala.owner.Utils.RequestCodes.directory_path;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.CatalogImportError;
import com.posbillingwala.owner.Model.CatalogImportPreviewResponse;
import com.posbillingwala.owner.Model.CatalogImportSummary;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.Utils.CatalogImportExportHelper;
import com.posbillingwala.owner.Utils.CatalogFileHelper;
import com.posbillingwala.owner.databinding.FragmentProductExportBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
    String customerId;
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
        customerId = MainActivity.userId;

        binding.toolbar.toolbarTitle.setText("Catalog Import / Export");
        binding.toolbar.backButton.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new UserSetting(), true);
        });
        binding.catalogCustomerScope.setText(CatalogImportExportHelper.customerCatalogLabel(activity));

        Bundle bundle = getArguments();
        if (bundle != null && bundle.getString("importType") != null) {
            selectedImportType = bundle.getString("importType");
        }

        binding.importTypeSpinner.setItems(importTypeLabels);
        int typeIndex = 0;
        for (int i = 0; i < importTypeValues.length; i++) {
            if (importTypeValues[i].equals(selectedImportType)) {
                typeIndex = i;
                break;
            }
        }
        binding.importTypeSpinner.setSelectedIndex(typeIndex);
        binding.importTypeSpinner.setOnItemSelectedListener((position, item) -> {
            selectedImportType = importTypeValues[position];
            updateTypeLabels();
        });
        updateTypeLabels();

        binding.downloadLayout.setOnClickListener(this);
        binding.exportLayout.setOnClickListener(this);
        binding.fileUpload.setOnClickListener(this);
        binding.uploadToServer.setOnClickListener(this);
        binding.viewHistoryLayout.setOnClickListener(v -> openImportHistory());

        return view;
    }

    @Override
    public void onClick(View clickedView) {
        int id = clickedView.getId();
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
            Toast.makeText(activity, "Unable to resolve account", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(activity, "Unable to resolve account", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(activity, "Unable to resolve account", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress("Exporting...");
        CatalogFileHelper.downloadCatalogFile(
                activity,
                "catalogExport.php?customerId=" + customerId + "&type=" + selectedImportType,
                selectedImportType + "_export.xlsx",
                new CatalogFileHelper.DownloadCallback() {
                    @Override
                    public void onSuccess(File savedFile) {
                        activity.runOnUiThread(() -> {
                            dismissProgress();
                            Toast.makeText(activity, "Export completed.\n" + savedFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
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
            Toast.makeText(activity, "Unable to resolve account", Toast.LENGTH_SHORT).show();
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

            Api.getClient().catalogImportValidate(customerBody, importTypeBody, filePart)
                    .enqueue(new Callback<CatalogImportPreviewResponse>() {
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

        titleView.setText(getSelectedTypeLabel() + " Import Preview");
        summaryView.setText("Total: " + summary.getTotal()
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

        Api.getClient().catalogImportConfirm(customerId, preview.getImportSessionId())
                .enqueue(new Callback<CatalogImportPreviewResponse>() {
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

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.drawerLayout.closeDrawers();
        customerId = MainActivity.userId;
        if (DetectConnection.checkInternetConnection(activity)) {
            requestPermission();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
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
