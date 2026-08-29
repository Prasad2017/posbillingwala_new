package com.posbillingwala.owner.Utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.posbillingwala.owner.Extra.Common;
import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Fragment.CatalogImportHistory;
import com.posbillingwala.owner.Model.CatalogImportError;
import com.posbillingwala.owner.Model.CatalogImportPreviewResponse;
import com.posbillingwala.owner.Model.CatalogImportSummary;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import cn.pedant.SweetAlert.SweetAlertDialog;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Reusable catalog Excel import/export actions for master screens.
 */
public class CatalogImportExportHelper {

    private static final int FILE_REQUEST_BASE = 9100;

    private final Fragment fragment;
    private final Activity activity;
    private final String customerId;
    private final String importType;
    private final String typeLabel;
    private final Runnable refreshCallback;
    private final int fileRequestCode;

    private SweetAlertDialog progressDialog;
    private boolean importInProgress;
    private Uri selectedUri;
    private String selectedFileName = "";

    public CatalogImportExportHelper(Fragment fragment, String customerId, String importType, String typeLabel, Runnable refreshCallback) {
        this.fragment = fragment;
        this.activity = fragment.getActivity();
        this.customerId = customerId;
        this.importType = importType;
        this.typeLabel = typeLabel;
        this.refreshCallback = refreshCallback;
        this.fileRequestCode = FILE_REQUEST_BASE + Math.abs(importType.hashCode() % 100);
    }

    public static String customerCatalogLabel(Activity activity) {
        String shop = activity != null ? Common.getSavedUserData(activity, "shopName") : "";
        String name = activity != null ? Common.getSavedUserData(activity, "customerName") : "";
        String label = shop != null && !shop.trim().isEmpty() ? shop.trim()
                : (name != null && !name.trim().isEmpty() ? name.trim() : "this customer");
        return "Customer catalog · " + label + " (all outlets)";
    }

    public void bindBar(View barRoot) {
        if (barRoot == null || customerId == null || customerId.isEmpty()) {
            return;
        }
        TextView demoBtn = barRoot.findViewById(R.id.catalogDemoBtn);
        TextView importBtn = barRoot.findViewById(R.id.catalogImportBtn);
        TextView exportBtn = barRoot.findViewById(R.id.catalogExportBtn);
        TextView historyBtn = barRoot.findViewById(R.id.catalogHistoryBtn);
        TextView scope = barRoot.findViewById(R.id.catalogCustomerScope);
        if (scope != null) {
            scope.setText(customerCatalogLabel(activity));
        }

        if (demoBtn != null) {
            demoBtn.setOnClickListener(v -> downloadTemplate());
        }
        if (importBtn != null) {
            importBtn.setOnClickListener(v -> pickImportFile());
        }
        if (exportBtn != null) {
            exportBtn.setOnClickListener(v -> exportData());
        }
        if (historyBtn != null) {
            historyBtn.setOnClickListener(v -> openHistory());
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != fileRequestCode || resultCode != Activity.RESULT_OK || data == null) {
            return false;
        }
        selectedUri = data.getData();
        if (selectedUri != null) {
            selectedFileName = queryDisplayName(selectedUri);
            validateImport();
        }
        return true;
    }

    private void pickImportFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        fragment.startActivityForResult(intent, fileRequestCode);
    }

    private void downloadTemplate() {
        showProgress("Preparing demo Excel...");
        CatalogFileHelper.downloadCatalogFile(
                activity,
                "catalogImportTemplate.php?customerId=" + customerId + "&type=" + importType,
                importType + "_template.xlsx",
                new CatalogFileHelper.DownloadCallback() {
                    @Override
                    public void onSuccess(File savedFile) {
                        activity.runOnUiThread(() -> {
                            dismissProgress();
                            Toast.makeText(activity, typeLabel + " demo Excel downloaded.", Toast.LENGTH_LONG).show();
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

    private void exportData() {
        showProgress("Exporting " + typeLabel.toLowerCase() + "...");
        CatalogFileHelper.downloadCatalogFile(
                activity,
                "catalogExport.php?customerId=" + customerId + "&type=" + importType,
                importType + "_export.xlsx",
                new CatalogFileHelper.DownloadCallback() {
                    @Override
                    public void onSuccess(File savedFile) {
                        activity.runOnUiThread(() -> {
                            dismissProgress();
                            Toast.makeText(activity, typeLabel + " exported successfully.", Toast.LENGTH_LONG).show();
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

    private void openHistory() {
        CatalogImportHistory history = new CatalogImportHistory();
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString("customerId", customerId);
        bundle.putString("importType", importType);
        bundle.putString("typeLabel", typeLabel);
        history.setArguments(bundle);
        ((MainActivity) activity).loadFragment(history, true);
    }

    private void validateImport() {
        showProgress("Validating...");
        try {
            File tempFile = copyUriToTempFile(selectedUri);
            RequestBody customerBody = RequestBody.create(customerId, MediaType.parse("text/plain"));
            RequestBody importTypeBody = RequestBody.create(importType, MediaType.parse("text/plain"));
            RequestBody fileBody = RequestBody.create(tempFile, MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("import_file", selectedFileName, fileBody);

            Api.getClient().catalogImportValidate(customerBody, importTypeBody, filePart)
                    .enqueue(new Callback<CatalogImportPreviewResponse>() {
                        @Override
                        public void onResponse(Call<CatalogImportPreviewResponse> call, Response<CatalogImportPreviewResponse> response) {
                            dismissProgress();
                            if (response.isSuccessful() && response.body() != null) {
                                showPreviewDialog(response.body());
                            } else {
                                Toast.makeText(activity, "Unable to validate file.", Toast.LENGTH_LONG).show();
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

        titleView.setText(typeLabel + " Import Preview");
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
        showProgress("Downloading errors...");
        CatalogFileHelper.downloadCatalogFile(
                activity,
                "catalogImportErrorExcel.php?customerId=" + customerId + "&importSessionId=" + sessionId,
                "Import_Errors.xlsx",
                new CatalogFileHelper.DownloadCallback() {
                    @Override
                    public void onSuccess(File savedFile) {
                        activity.runOnUiThread(() -> {
                            dismissProgress();
                            Toast.makeText(activity, "Error report saved.", Toast.LENGTH_SHORT).show();
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
                                    .setTitleText("Done")
                                    .setContentText(total + " " + typeLabel.toLowerCase() + " imported.")
                                    .show();
                            if (refreshCallback != null) {
                                refreshCallback.run();
                            }
                        } else {
                            Toast.makeText(activity, "Import failed.", Toast.LENGTH_LONG).show();
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

    private File copyUriToTempFile(Uri fileUri) throws Exception {
        InputStream inputStream = activity.getContentResolver().openInputStream(fileUri);
        if (inputStream == null) {
            throw new Exception("Unable to read file");
        }
        selectedFileName = queryDisplayName(fileUri);
        if (selectedFileName == null || selectedFileName.isEmpty()) {
            selectedFileName = importType + "_import.xlsx";
        }
        File tempFile = new File(activity.getCacheDir(), "catalog_" + System.currentTimeMillis() + ".xlsx");
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
}
