package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.R;

import java.io.File;

/**
 * Import / export facade.
 * Phase 14: Owner/Dealer/Admin keep Excel cloud catalog; POS adds local product CSV.
 */
public final class ImportExportEngine {

    private ImportExportEngine() {
    }

    public static boolean catalogImportAvailableOnPos() {
        return true;
    }

    public static String catalogImportHome() {
        return "POS Master Data → Catalog CSV (+ Owner/Dealer/Admin Excel for cloud)";
    }

    public static String posReportExportHome() {
        return "WithTable Utils.ReportToSpreadsheet (reports CSV/XLS path)";
    }

    public static void showCatalogMenu(Activity activity, Fragment fragment, POSBillingWalaDatabase db) {
        if (activity == null || fragment == null || db == null) {
            return;
        }
        String[] items = new String[]{
                activity.getString(R.string.catalog_csv_export),
                activity.getString(R.string.catalog_csv_import),
                activity.getString(R.string.catalog_csv_template_hint)
        };
        new AlertDialog.Builder(activity)
                .setTitle(R.string.catalog_csv_title)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        exportAndShare(activity, db);
                    } else if (which == 1) {
                        CatalogCsvHelper.pickImportFile(fragment);
                    } else {
                        Toast.makeText(activity, R.string.catalog_csv_format_toast, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void exportAndShare(Activity activity, POSBillingWalaDatabase db) {
        if (activity == null || db == null) {
            return;
        }
        AppExecutors.get().db().execute(() -> {
            try {
                File file = CatalogCsvHelper.exportCatalogCsv(activity, db);
                AppExecutors.get().main(() -> {
                    Toast.makeText(activity, R.string.catalog_csv_export_done, Toast.LENGTH_SHORT).show();
                    CatalogCsvHelper.shareFile(activity, file);
                });
            } catch (Exception e) {
                e.printStackTrace();
                AppExecutors.get().main(() -> Toast.makeText(activity,
                        R.string.catalog_csv_export_failed, Toast.LENGTH_LONG).show());
            }
        });
    }

    public static boolean handleImportActivityResult(Fragment fragment, int requestCode, int resultCode,
                                                     @Nullable Intent data, POSBillingWalaDatabase db) {
        return CatalogCsvHelper.handleImportResult(fragment, requestCode, resultCode, data, db);
    }

    public static String moduleSummary(Context context) {
        return "POS catalog CSV: " + (catalogImportAvailableOnPos() ? "on (Master Data)" : "off")
                + "\nCloud Excel: Owner / Dealer / Admin CatalogImportExportHelper"
                + "\nPOS reports export: " + posReportExportHome()
                + "\nImport is additive (no wipe)";
    }
}
