package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.BuildConfig;
import com.pos_billingwala.Model.ExpenseResponse;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.Model.MessInvoiceResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.Utils.ReportToSpreadsheet;
import com.pos_billingwala.Utils.RequestCodes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared Excel (.xls HTML spreadsheet) export + share for all report screens.
 */
public final class ReportExcelHelper {

    private ReportExcelHelper() {
    }

    public static void ensureExportFolder() {
        File sale = new File(RequestCodes.directory_path + "/Sale");
        if (!sale.exists()) {
            //noinspection ResultOfMethodCallIgnored
            sale.mkdirs();
        }
    }

    public static String periodSubtitle(@Nullable String period) {
        if (period == null || period.trim().isEmpty()) {
            return "All records";
        }
        return "Period: " + period.trim();
    }

    public static void exportAndShare(@NonNull Activity activity,
                                      @NonNull String reportTitle,
                                      @NonNull String relativeFilePath,
                                      @Nullable String subtitle,
                                      @NonNull List<List<String>> rows) {
        if (rows.size() <= 1) {
            Toast.makeText(activity, activity.getString(R.string.ui_no_data_found), Toast.LENGTH_SHORT).show();
            return;
        }
        ensureExportFolder();
        ReportToSpreadsheet export = new ReportToSpreadsheet(reportTitle, RequestCodes.directory_path);
        export.exportReport(rows, relativeFilePath, subtitle, new ReportToSpreadsheet.ExportListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onCompleted(String filePath) {
                Toast.makeText(activity,
                        activity.getString(R.string.toast_sales_report_successfully_exported),
                        Toast.LENGTH_SHORT).show();
                shareFile(activity, filePath, reportTitle);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void shareFile(@NonNull Activity activity, @NonNull String filePath,
                                 @NonNull String chooserTitle) {
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
        intent.setType("application/vnd.ms-excel");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        List<ResolveInfo> apps = activity.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : apps) {
            activity.grantUriPermission(info.activityInfo.packageName, uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        try {
            activity.startActivity(Intent.createChooser(intent, chooserTitle));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity,
                    activity.getString(R.string.toast_no_application_available_to_view_excel),
                    Toast.LENGTH_LONG).show();
        }
    }

    public static List<List<String>> invoiceRows(@NonNull List<InvoiceResponse> invoices,
                                                 @Nullable String amountTotalLabel) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("SR No", "Invoice Date", "Invoice Number",
                "Amount ( " + safeCurrency() + " )"));
        double total = 0;
        int sr = 1;
        for (InvoiceResponse inv : invoices) {
            if (inv == null) {
                continue;
            }
            rows.add(listOf(String.valueOf(sr++),
                    safe(inv.getInvoiceDate()),
                    safe(inv.getInvoiceNumber()),
                    safe(inv.getTotalAmount())));
            total += parseAmount(inv.getTotalAmount());
        }
        rows.add(listOf("", "", amountTotalLabel != null ? amountTotalLabel : "Total Amount",
                formatAmount(total)));
        return rows;
    }

    public static List<List<String>> discountInvoiceRows(@NonNull List<InvoiceResponse> invoices) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("SR No", "Invoice Date", "Invoice Number", "Discount Type",
                "Discount ( " + safeCurrency() + " )"));
        double total = 0;
        int sr = 1;
        for (InvoiceResponse inv : invoices) {
            if (inv == null) {
                continue;
            }
            float rupees = ReportCursorHelper.discountRupees(
                    inv.getDiscount(), inv.getDiscountType(), inv.getSubTotal());
            total += rupees;
            rows.add(listOf(String.valueOf(sr++),
                    safe(inv.getInvoiceDate()),
                    safe(inv.getInvoiceNumber()),
                    safe(inv.getDiscountType()),
                    formatAmount(rupees)));
        }
        rows.add(listOf("", "", "", "Total Amount", formatAmount(total)));
        return rows;
    }

    public static List<List<String>> expenseRows(@NonNull List<ExpenseResponse> expenses) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("SR No", "Expense Date", "Expense Name",
                "Amount ( " + safeCurrency() + " )"));
        double total = 0;
        int sr = 1;
        for (ExpenseResponse e : expenses) {
            if (e == null) {
                continue;
            }
            total += parseAmount(e.getExpenseAmount());
            rows.add(listOf(String.valueOf(sr++),
                    safe(e.getExpenseDate()),
                    safe(e.getExpenseName()),
                    safe(e.getExpenseAmount())));
        }
        rows.add(listOf("", "", "Total Amount", formatAmount(total)));
        return rows;
    }

    public static List<List<String>> productRows(@NonNull List<InvoiceProductResponse> products) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("SR No", "Product", "Qty", "Amount ( " + safeCurrency() + " )"));
        double totalQty = 0;
        double totalAmt = 0;
        int sr = 1;
        for (InvoiceProductResponse p : products) {
            if (p == null) {
                continue;
            }
            double qty = parseAmount(p.getProductQuantity());
            double price = parseAmount(p.getSnapshotLinePrice() != null && !p.getSnapshotLinePrice().trim().isEmpty()
                    ? p.getSnapshotLinePrice()
                    : p.getProductPrice());
            double amt = price * (qty > 0 ? qty : 1);
            totalQty += qty;
            totalAmt += amt;
            rows.add(listOf(String.valueOf(sr++),
                    safe(p.getProductName()),
                    safe(p.getProductQuantity()),
                    formatAmount(amt)));
        }
        rows.add(listOf("", "Total Amount", formatAmount(totalQty), formatAmount(totalAmt)));
        return rows;
    }

    public static List<List<String>> messInvoiceRows(@NonNull List<MessInvoiceResponse> list) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("SR No", "Date", "Member", "Meal Type"));
        int sr = 1;
        for (MessInvoiceResponse m : list) {
            if (m == null) {
                continue;
            }
            rows.add(listOf(String.valueOf(sr++),
                    safe(m.getMessInvoiceDate()),
                    safe(m.getMemberName()),
                    safe(m.getMessType())));
        }
        return rows;
    }

    public static List<List<String>> messReportRows(@NonNull List<com.pos_billingwala.Model.MessReportItem> list) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("SR No", "Source", "Date", "Member", "Meal Type", "Detail"));
        int sr = 1;
        for (com.pos_billingwala.Model.MessReportItem m : list) {
            if (m == null) {
                continue;
            }
            rows.add(listOf(String.valueOf(sr++),
                    m.isQr() ? "QR Token" : "Coupon",
                    safe(m.getDateTime()),
                    safe(m.getMemberName()),
                    safe(m.getMessType()),
                    safe(m.getDetail())));
        }
        return rows;
    }

    public static List<List<String>> memberRows(@NonNull List<MemberResponse> members) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("SR No", "Member Name", "Mobile", "Type"));
        int sr = 1;
        for (MemberResponse m : members) {
            if (m == null) {
                continue;
            }
            rows.add(listOf(String.valueOf(sr++),
                    safe(m.getMemberName()),
                    safe(m.getMemberMobileNumber()),
                    safe(m.getMemberType())));
        }
        return rows;
    }

    public static List<List<String>> memberPaymentRows(@NonNull List<MemberResponse> payments) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("SR No", "Date", "Member", "Paid ( " + safeCurrency() + " )", "Mess Amount"));
        double total = 0;
        int sr = 1;
        for (MemberResponse m : payments) {
            if (m == null) {
                continue;
            }
            total += parseAmount(m.getPaymentPaidAmount());
            rows.add(listOf(String.valueOf(sr++),
                    safe(m.getPaymentDate()),
                    safe(m.getMemberName()),
                    safe(m.getPaymentPaidAmount()),
                    safe(m.getPaymentMessAmount())));
        }
        rows.add(listOf("", "", "Total Amount", formatAmount(total), ""));
        return rows;
    }

    public static List<List<String>> saleSummaryRows(
            float subAmount, float gst, float discount, float total,
            float fastBilling, float table, float takeAway,
            float cash, float upi) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("Metric", "Amount ( " + safeCurrency() + " )"));
        rows.add(listOf("Sub Total", formatAmount(subAmount)));
        rows.add(listOf("GST", formatAmount(gst)));
        rows.add(listOf("Discount", formatAmount(discount)));
        rows.add(listOf("Total Sales", formatAmount(total)));
        rows.add(listOf("Fast Billing", formatAmount(fastBilling)));
        rows.add(listOf("Table", formatAmount(table)));
        rows.add(listOf("Take Away", formatAmount(takeAway)));
        rows.add(listOf("Cash", formatAmount(cash)));
        rows.add(listOf("UPI", formatAmount(upi)));
        return rows;
    }

    private static String safeCurrency() {
        return MainActivity.currencyName != null ? MainActivity.currencyName : "INR";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static double parseAmount(String raw) {
        return ReportCursorHelper.parseAmount(raw);
    }

    private static String formatAmount(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static List<String> listOf(String... values) {
        List<String> list = new ArrayList<>(values.length);
        for (String v : values) {
            list.add(v != null ? v : "");
        }
        return list;
    }
}
