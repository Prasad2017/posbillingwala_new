package com.pos_billingwala.Extra;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.LocalSalesSnapshot;
import com.pos_billingwala.Model.ReportRankItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Local SQLite analytics for this POS device only (one licence = one branch).
 * Multi-branch rollup is available in the Owner app, not here.
 */
public final class LocalSalesAnalytics {

    private final POSBillingWalaDatabase database;

    public LocalSalesAnalytics(Context context) {
        database = new POSBillingWalaDatabase(context);
    }

    public LocalSalesSnapshot loadTodayDashboard() {
        Calendar cal = Calendar.getInstance();
        String today = formatDay(cal);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        String yesterday = formatDay(cal);

        PeriodStats todayStats = loadPeriodStats(today);
        PeriodStats yesterdayStats = loadPeriodStats(yesterday);

        LocalSalesSnapshot snapshot = new LocalSalesSnapshot();
        snapshot.setBranchLabel(BranchScopeSql.branchLabel());
        snapshot.setPeriodLabel("Today · " + snapshot.getBranchLabel());
        applyStats(snapshot, todayStats, yesterdayStats);
        snapshot.setSalesTrend(loadDailyTrend(7));
        snapshot.setRecentInvoices(loadRecentInvoices(8));
        return snapshot;
    }

    public LocalSalesSnapshot loadMonthlyOverview() {
        Calendar cal = Calendar.getInstance();
        String thisMonth = formatMonth(cal);
        cal.add(Calendar.MONTH, -1);
        String prevMonth = formatMonth(cal);

        PeriodStats current = loadMonthStats(thisMonth);
        PeriodStats previous = loadMonthStats(prevMonth);

        LocalSalesSnapshot snapshot = new LocalSalesSnapshot();
        snapshot.setBranchLabel(BranchScopeSql.branchLabel());
        snapshot.setPeriodLabel("This Month · " + snapshot.getBranchLabel());
        applyStats(snapshot, current, previous);
        snapshot.setSalesTrend(loadMonthDailyTrend(thisMonth));
        snapshot.setTopCustomers(loadTopCustomers(thisMonth, 5));
        return snapshot;
    }

    public LocalSalesSnapshot loadRecentSalesList() {
        List<InvoiceResponse> invoices = loadRecentInvoices(100);
        float total = 0f;
        for (InvoiceResponse invoice : invoices) {
            total += ReportCursorHelper.parseAmount(invoice.getTotalAmount());
        }
        LocalSalesSnapshot snapshot = new LocalSalesSnapshot();
        snapshot.setBranchLabel(BranchScopeSql.branchLabel());
        snapshot.setPeriodLabel("All Bills · " + snapshot.getBranchLabel());
        snapshot.setBillCount(invoices.size());
        snapshot.setTotalSales(total);
        snapshot.setNetSales(total);
        snapshot.setAvgBill(invoices.isEmpty() ? 0f : total / invoices.size());
        snapshot.setRecentInvoices(invoices);
        return snapshot;
    }

    private void applyStats(LocalSalesSnapshot snapshot, PeriodStats current, PeriodStats previous) {
        snapshot.setTotalSales(current.totalAmount);
        snapshot.setNetSales(current.totalAmount);
        snapshot.setBillCount(current.billCount);
        snapshot.setAvgBill(current.billCount > 0 ? current.totalAmount / current.billCount : 0f);
        snapshot.setTotalSalesTrend(formatTrend(current.totalAmount, previous.totalAmount));
        snapshot.setNetSalesTrend(formatTrend(current.totalAmount, previous.totalAmount));
        snapshot.setBillCountTrend(formatTrend(current.billCount, previous.billCount));
        snapshot.setAvgBillTrend(formatTrend(snapshot.getAvgBill(),
                previous.billCount > 0 ? previous.totalAmount / previous.billCount : 0f));
    }

    private PeriodStats loadPeriodStats(String dayPrefix) {
        BranchScopeSql.ScopeClause scope = BranchScopeSql.invoiceBranchScope();
        SQLiteDatabase db = database.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql = "SELECT SUM(CAST(totalAmount AS REAL)) AS totalAmount, COUNT(*) AS billCount FROM "
                    + POSBillingWalaDatabase.INVOICE_TABLE
                    + " WHERE invoiceDate LIKE ?" + scope.sql;
            String[] args = mergeArgs(new String[]{dayPrefix + "%"}, scope.args);
            cursor = db.rawQuery(sql, args);
            if (cursor.moveToNext()) {
                return new PeriodStats(
                        ReportCursorHelper.readFloat(cursor, "totalAmount"),
                        readInt(cursor, "billCount"));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return new PeriodStats(0f, 0);
    }

    private PeriodStats loadMonthStats(String monthPrefix) {
        return loadPeriodStats(monthPrefix);
    }

    private List<ReportRankItem> loadDailyTrend(int days) {
        List<ReportRankItem> points = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -(days - 1));
        for (int i = 0; i < days; i++) {
            String day = formatDay(cal);
            PeriodStats stats = loadPeriodStats(day);
            ReportRankItem item = new ReportRankItem();
            item.setDate(day);
            item.setTotal(String.format(Locale.US, "%.2f", stats.totalAmount));
            item.setLabel(day.length() >= 10 ? day.substring(8, 10) : day);
            points.add(item);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return points;
    }

    private List<ReportRankItem> loadMonthDailyTrend(String monthPrefix) {
        List<ReportRankItem> points = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= maxDay; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String prefix = formatDay(cal);
            if (!prefix.startsWith(monthPrefix)) {
                continue;
            }
            PeriodStats stats = loadPeriodStats(prefix);
            ReportRankItem item = new ReportRankItem();
            item.setDate(prefix);
            item.setTotal(String.format(Locale.US, "%.2f", stats.totalAmount));
            item.setLabel(String.format(Locale.US, "%02d", day));
            points.add(item);
        }
        return points;
    }

    private List<ReportRankItem> loadTopCustomers(String monthPrefix, int limit) {
        List<ReportRankItem> items = new ArrayList<>();
        BranchScopeSql.ScopeClause scope = BranchScopeSql.invoiceBranchScope();
        SQLiteDatabase db = database.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql = "SELECT customerName, SUM(CAST(totalAmount AS REAL)) AS totalSales, COUNT(*) AS billCount FROM "
                    + POSBillingWalaDatabase.INVOICE_TABLE
                    + " WHERE invoiceDate LIKE ? AND customerName IS NOT NULL AND TRIM(customerName) != '' "
                    + scope.sql
                    + " GROUP BY customerName ORDER BY totalSales DESC LIMIT " + limit;
            cursor = db.rawQuery(sql, mergeArgs(new String[]{monthPrefix + "%"}, scope.args));
            while (cursor.moveToNext()) {
                ReportRankItem item = new ReportRankItem();
                item.setCustomerName(cursor.getString(cursor.getColumnIndex("customerName")));
                item.setTotalSales(String.format(Locale.US, "%.2f",
                        ReportCursorHelper.readFloat(cursor, "totalSales")));
                item.setCount(String.valueOf(readInt(cursor, "billCount")));
                items.add(item);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        if (items.isEmpty()) {
            try {
                db = database.getReadableDatabase();
                String sql = "SELECT paymentMode AS customerName, SUM(CAST(totalAmount AS REAL)) AS totalSales, COUNT(*) AS billCount FROM "
                        + POSBillingWalaDatabase.INVOICE_TABLE
                        + " WHERE invoiceDate LIKE ? AND paymentMode IS NOT NULL AND TRIM(paymentMode) != '' "
                        + scope.sql
                        + " GROUP BY paymentMode ORDER BY totalSales DESC LIMIT " + limit;
                cursor = db.rawQuery(sql, mergeArgs(new String[]{monthPrefix + "%"}, scope.args));
                while (cursor.moveToNext()) {
                    ReportRankItem item = new ReportRankItem();
                    item.setCustomerName(cursor.getString(cursor.getColumnIndex("customerName")));
                    item.setTotalSales(String.format(Locale.US, "%.2f",
                            ReportCursorHelper.readFloat(cursor, "totalSales")));
                    item.setCount(String.valueOf(readInt(cursor, "billCount")));
                    items.add(item);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                db.close();
            }
        }
        return items;
    }

    private List<InvoiceResponse> loadRecentInvoices(int limit) {
        List<InvoiceResponse> list = new ArrayList<>();
        BranchScopeSql.ScopeClause scope = BranchScopeSql.invoiceBranchScope();
        SQLiteDatabase db = database.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql = "SELECT * FROM " + POSBillingWalaDatabase.INVOICE_TABLE
                    + " WHERE 1=1" + scope.sql
                    + " ORDER BY invoiceDate DESC LIMIT " + limit;
            cursor = db.rawQuery(sql, scope.args);
            while (cursor.moveToNext()) {
                InvoiceResponse invoice = new InvoiceResponse();
                invoice.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
                invoice.setInvoiceNumber(cursor.getString(cursor.getColumnIndex("invoiceNumber")));
                invoice.setCustomerName(cursor.getString(cursor.getColumnIndex("customerName")));
                invoice.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
                invoice.setPaymentMode(cursor.getString(cursor.getColumnIndex("paymentMode")));
                invoice.setInvoiceDate(cursor.getString(cursor.getColumnIndex("invoiceDate")));
                list.add(invoice);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return list;
    }

    private static String[] mergeArgs(String[] base, String[] extra) {
        String[] merged = new String[base.length + extra.length];
        System.arraycopy(base, 0, merged, 0, base.length);
        System.arraycopy(extra, 0, merged, base.length, extra.length);
        return merged;
    }

    private static int readInt(Cursor cursor, String column) {
        int idx = cursor.getColumnIndex(column);
        if (idx < 0) {
            return 0;
        }
        try {
            return cursor.getInt(idx);
        } catch (Exception e) {
            return (int) ReportCursorHelper.readFloat(cursor, column);
        }
    }

    private static String formatDay(Calendar cal) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    private static String formatMonth(Calendar cal) {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(cal.getTime());
    }

    private static String formatTrend(float current, float previous) {
        if (previous <= 0f) {
            return current > 0f ? "+100%" : "0%";
        }
        float pct = ((current - previous) / previous) * 100f;
        return String.format(Locale.US, "%+.0f%%", pct);
    }

    private static final class PeriodStats {
        final float totalAmount;
        final int billCount;

        PeriodStats(float totalAmount, int billCount) {
            this.totalAmount = totalAmount;
            this.billCount = billCount;
        }
    }
}
