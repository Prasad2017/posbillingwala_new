package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.BuildConfig;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Local POS product catalog CSV import/export (no Apache POI).
 * Additive import only — never deletes existing products.
 */
public final class CatalogCsvHelper {

    public static final int REQUEST_IMPORT_CSV = 9241;
    public static final String HEADER =
            "categoryName,productCode,productName,productPrice,productUnit,productCGST,productSGST,openPrice";

    public static final class ImportResult {
        public int inserted;
        public int updated;
        public int skipped;
    }

    private CatalogCsvHelper() {
    }

    public static File exportCatalogCsv(Context context, POSBillingWalaDatabase db) throws Exception {
        if (context == null || db == null) {
            throw new IllegalArgumentException("missing context/db");
        }
        List<ProductResponse> products = db.getAllProductList("", "fast_billing");
        File dir = new File(context.getExternalFilesDir(null), "catalog");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("cannot create catalog dir");
        }
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File out = new File(dir, "pos_catalog_" + stamp + ".csv");
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8))) {
            writer.write('\ufeff'); // Excel-friendly BOM
            writer.write(HEADER);
            writer.newLine();
            if (products != null) {
                for (ProductResponse p : products) {
                    if (p == null) {
                        continue;
                    }
                    writer.write(csv(p.getCategoryName()));
                    writer.write(',');
                    writer.write(csv(p.getProductCode()));
                    writer.write(',');
                    writer.write(csv(p.getProductName()));
                    writer.write(',');
                    writer.write(csv(p.getProductPrice()));
                    writer.write(',');
                    writer.write(csv(p.getProductUnit()));
                    writer.write(',');
                    writer.write(csv(p.getProductCGST()));
                    writer.write(',');
                    writer.write(csv(p.getProductSGST()));
                    writer.write(',');
                    writer.write(csv(p.isOpenPrice() ? "on" : "off"));
                    writer.newLine();
                }
            }
        }
        return out;
    }

    public static void shareFile(Activity activity, File file) {
        if (activity == null || file == null || !file.exists()) {
            return;
        }
        Uri uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/csv");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(Intent.createChooser(share, activity.getString(R.string.catalog_csv_share_title)));
    }

    public static void pickImportFile(Fragment fragment) {
        if (fragment == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] mime = new String[]{"text/csv", "text/comma-separated-values", "text/plain", "application/csv"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mime);
        fragment.startActivityForResult(Intent.createChooser(intent,
                fragment.getString(R.string.catalog_csv_import_pick)), REQUEST_IMPORT_CSV);
    }

    public static ImportResult importCatalogCsv(Context context, POSBillingWalaDatabase db, Uri uri)
            throws Exception {
        ImportResult result = new ImportResult();
        if (context == null || db == null || uri == null) {
            return result;
        }
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("\ufeff")) {
                    line = line.substring(1);
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (!headerSkipped) {
                    headerSkipped = true;
                    if (line.toLowerCase(Locale.US).contains("productname")
                            || line.toLowerCase(Locale.US).contains("categoryname")) {
                        continue;
                    }
                }
                List<String> cols = parseCsvLine(line);
                if (cols.size() < 3) {
                    result.skipped++;
                    continue;
                }
                String categoryName = col(cols, 0);
                String productCode = col(cols, 1);
                String productName = col(cols, 2);
                String price = col(cols, 3);
                String unit = col(cols, 4);
                String cgst = col(cols, 5);
                String sgst = col(cols, 6);
                String openPrice = col(cols, 7);
                if (productName.isEmpty() || categoryName.isEmpty()) {
                    result.skipped++;
                    continue;
                }
                if (price.isEmpty()) {
                    price = "0";
                }
                if (unit.isEmpty()) {
                    unit = "NOS";
                }
                if (cgst.isEmpty()) {
                    cgst = "0";
                }
                if (sgst.isEmpty()) {
                    sgst = "0";
                }
                if (!"on".equalsIgnoreCase(openPrice)) {
                    openPrice = "off";
                }
                String categoryId = db.ensureCategoryIdByName(categoryName);
                if (categoryId == null || categoryId.isEmpty()) {
                    result.skipped++;
                    continue;
                }
                ProductResponse existing = null;
                if (!productCode.isEmpty()) {
                    existing = db.findActiveProductByExactCode(productCode);
                }
                if (existing == null) {
                    existing = db.findActiveProductByNameAndCategory(productName, categoryName);
                }
                String userId = com.pos_billingwala.Activity.MainActivity.userId;
                if (existing != null && existing.getProductId() != null) {
                    db.updateProduct(userId, existing.getProductId(), categoryId, categoryName,
                            productCode, productName, price, unit, cgst, sgst, 0, null, openPrice);
                    result.updated++;
                } else {
                    String network = randomKey(10);
                    db.addProduct(userId, categoryId, categoryName, productCode, productName, price,
                            unit, cgst, sgst, 0, network, "0", null, openPrice);
                    result.inserted++;
                }
            }
        }
        return result;
    }

    public static boolean handleImportResult(Fragment fragment, int requestCode, int resultCode,
                                             @Nullable Intent data, POSBillingWalaDatabase db) {
        if (requestCode != REQUEST_IMPORT_CSV || resultCode != Activity.RESULT_OK
                || data == null || data.getData() == null || fragment == null) {
            return false;
        }
        Activity activity = fragment.getActivity();
        if (activity == null) {
            return true;
        }
        final Uri uri = data.getData();
        AppExecutors.get().db().execute(() -> {
            try {
                ImportResult result = importCatalogCsv(activity, db, uri);
                AppExecutors.get().main(() -> Toast.makeText(activity,
                        activity.getString(R.string.catalog_csv_import_done,
                                result.inserted, result.updated, result.skipped),
                        Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                e.printStackTrace();
                AppExecutors.get().main(() -> Toast.makeText(activity,
                        activity.getString(R.string.catalog_csv_import_failed),
                        Toast.LENGTH_LONG).show());
            }
        });
        return true;
    }

    private static String col(List<String> cols, int index) {
        if (index >= cols.size() || cols.get(index) == null) {
            return "";
        }
        return cols.get(index).trim();
    }

    private static String csv(String raw) {
        String v = raw == null ? "" : raw.trim();
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static String randomKey(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random r = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
