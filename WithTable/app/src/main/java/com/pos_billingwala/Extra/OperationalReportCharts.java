package com.pos_billingwala.Extra;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.Model.ReportRankItem;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentOperationalReportBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OperationalReportCharts {

    private static final int[] TYPE_COLORS = {
            Color.parseColor("#2563EB"),
            Color.parseColor("#16A34A"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#8B5CF6"),
            Color.parseColor("#DC2626")
    };

    private OperationalReportCharts() {
    }

    public static List<ReportRankItem> groupedBreakdown(POSBillingWalaDatabase db, String column,
                                                        String dateFilter) {
        return groupedBreakdown(db, column, dateFilter, null);
    }

    public static List<ReportRankItem> groupedBreakdown(POSBillingWalaDatabase db, String column,
                                                        String dateFilter, String extraWhere) {
        List<ReportRankItem> items = new ArrayList<>();
        if (db == null || column == null || column.trim().isEmpty()) {
            return items;
        }
        SQLiteDatabase database = db.getReadableDatabase();
        try {
            String sql = "SELECT " + column + " as grp, SUM(totalAmount) as totalAmount, "
                    + "COUNT(invoiceId) as totalCount FROM " + POSBillingWalaDatabase.INVOICE_TABLE;
            boolean hasWhere = false;
            if (dateFilter != null && !dateFilter.trim().isEmpty()) {
                sql += " WHERE invoiceDate LIKE '%" + dateFilter.trim() + "%'";
                hasWhere = true;
            }
            if (extraWhere != null && !extraWhere.trim().isEmpty()) {
                sql += hasWhere ? " AND " : " WHERE ";
                sql += extraWhere.trim();
            }
            sql += " GROUP BY " + column + " ORDER BY totalAmount DESC";
            Cursor cursor = database.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                ReportRankItem item = new ReportRankItem();
                String label = cursor.getString(cursor.getColumnIndexOrThrow("grp"));
                item.setLabel(formatGroupLabel(column, label));
                item.setTotal(String.valueOf(ReportCursorHelper.readFloat(cursor, "totalAmount")));
                item.setCount(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("totalCount"))));
                items.add(item);
            }
            cursor.close();
        } finally {
            database.close();
        }
        return items;
    }

    public static void bindListSummary(FragmentOperationalReportBinding binding, Context context,
                                         int billCount, float totalAmount, List<ReportRankItem> breakdown,
                                         String donutTitle, String barTitle, String periodLabel) {
        if (binding == null || context == null) {
            return;
        }
        String currency = MainActivity.currencyName;
        binding.dateChip.setText(periodLabel != null && !periodLabel.isEmpty()
                ? periodLabel : context.getString(R.string.ui_all_records));
        ReportUiHelper.bindKpi(binding.kpi1, context.getString(R.string.ui_total_bills),
                String.valueOf(billCount), "");
        ReportUiHelper.bindKpi(binding.kpi2, context.getString(R.string.ui_total_amount),
                ReportUiHelper.money(currency, totalAmount), "");
        float avg = billCount > 0 ? totalAmount / billCount : 0f;
        ReportUiHelper.bindKpi(binding.kpi3, context.getString(R.string.ui_avg_bill),
                ReportUiHelper.money(currency, avg), "");
        ReportUiHelper.bindKpi(binding.kpi4, context.getString(R.string.ui_categories),
                String.valueOf(breakdown != null ? breakdown.size() : 0), "");
        bindBreakdownCharts(binding, breakdown, donutTitle, barTitle, currency);
    }

    public static void bindSaleSummary(FragmentOperationalReportBinding binding, Context context,
                                       float subAmount, float gstAmount, float discount, float totalAmount,
                                       float fastBilling, float tableAmount, float takeAwayAmount,
                                       String periodLabel) {
        bindSaleSummary(binding, context, subAmount, gstAmount, discount, totalAmount,
                fastBilling, tableAmount, takeAwayAmount, periodLabel, -1f, -1f);
    }

    public static void bindSaleSummary(FragmentOperationalReportBinding binding, Context context,
                                       float subAmount, float gstAmount, float discount, float totalAmount,
                                       float fastBilling, float tableAmount, float takeAwayAmount,
                                       String periodLabel, float cashTotal, float upiTotal) {
        if (binding == null || context == null) {
            return;
        }
        String currency = MainActivity.currencyName;
        binding.dateChip.setText(periodLabel != null && !periodLabel.isEmpty()
                ? periodLabel : context.getString(R.string.ui_all_records));
        binding.cardList.setVisibility(View.GONE);

        ReportUiHelper.bindKpi(binding.kpi1, context.getString(R.string.ui_sub_amount),
                ReportUiHelper.money(currency, subAmount), "");
        ReportUiHelper.bindKpi(binding.kpi2, context.getString(R.string.ui_csgt_sgst),
                ReportUiHelper.money(currency, gstAmount), "");
        ReportUiHelper.bindKpi(binding.kpi3, context.getString(R.string.ui_total_discount),
                ReportUiHelper.money(currency, discount), "");
        ReportUiHelper.bindKpi(binding.kpi4, context.getString(R.string.ui_total_amount),
                ReportUiHelper.money(currency, totalAmount), "");

        binding.donutTitle.setText(context.getString(R.string.ui_billing_wise_details));
        binding.barTitle.setText(context.getString(R.string.ui_amount_breakdown));

        List<ReportRankItem> typeItems = new ArrayList<>();
        if (fastBilling > 0f) {
            ReportRankItem item = new ReportRankItem();
            item.setLabel(context.getString(R.string.fast_billing));
            item.setTotal(String.valueOf(fastBilling));
            typeItems.add(item);
        }
        if (tableAmount > 0f) {
            ReportRankItem item = new ReportRankItem();
            item.setLabel(context.getString(R.string.dine_in));
            item.setTotal(String.valueOf(tableAmount));
            typeItems.add(item);
        }
        if (takeAwayAmount > 0f) {
            ReportRankItem item = new ReportRankItem();
            item.setLabel(context.getString(R.string.take_away));
            item.setTotal(String.valueOf(takeAwayAmount));
            typeItems.add(item);
        }
        bindBreakdownCharts(binding, typeItems,
                context.getString(R.string.ui_billing_wise_details),
                context.getString(R.string.ui_amount_breakdown), currency);

        List<ReportRankItem> amountBars = new ArrayList<>();
        ReportRankItem sub = new ReportRankItem();
        sub.setLabel(context.getString(R.string.ui_sub_amount));
        sub.setCount(String.valueOf(subAmount));
        amountBars.add(sub);
        ReportRankItem gst = new ReportRankItem();
        gst.setLabel(context.getString(R.string.ui_csgt_sgst));
        gst.setCount(String.valueOf(gstAmount));
        amountBars.add(gst);
        ReportRankItem disc = new ReportRankItem();
        disc.setLabel(context.getString(R.string.ui_total_discount));
        disc.setCount(String.valueOf(discount));
        amountBars.add(disc);
        ReportRankItem total = new ReportRankItem();
        total.setLabel(context.getString(R.string.ui_total_amount));
        total.setCount(String.valueOf(totalAmount));
        amountBars.add(total);
        if (cashTotal >= 0f) {
            ReportRankItem cash = new ReportRankItem();
            cash.setLabel(context.getString(R.string.ui_total_cash));
            cash.setCount(String.valueOf(cashTotal));
            amountBars.add(cash);
        }
        if (upiTotal >= 0f) {
            ReportRankItem upi = new ReportRankItem();
            upi.setLabel(context.getString(R.string.ui_total_upi));
            upi.setCount(String.valueOf(upiTotal));
            amountBars.add(upi);
        }
        ReportUiHelper.setupBars(binding.chartBar, amountBars);

        float typeSum = fastBilling + tableAmount + takeAwayAmount;
        wireChartSelection(binding, typeItems, currency, typeSum,
                ReportUiHelper.money(currency, typeSum), amountBars, totalAmount);
    }

    private static void bindBreakdownCharts(FragmentOperationalReportBinding binding,
                                            List<ReportRankItem> breakdown, String donutTitle,
                                            String barTitle, String currency) {
        binding.donutTitle.setText(donutTitle != null ? donutTitle : "");
        binding.barTitle.setText(barTitle != null ? barTitle : "");

        if (breakdown == null || breakdown.isEmpty()) {
            binding.cardDonut.setVisibility(View.GONE);
            binding.cardBar.setVisibility(View.GONE);
            return;
        }

        binding.cardDonut.setVisibility(View.VISIBLE);
        binding.cardBar.setVisibility(View.VISIBLE);

        float sum = 0f;
        for (ReportRankItem item : breakdown) {
            sum += ReportCursorHelper.parseAmount(item.getTotal());
        }

        List<PieEntry> entries = new ArrayList<>();
        String[] labels = new String[breakdown.size()];
        String[] values = new String[breakdown.size()];
        String[] percents = new String[breakdown.size()];
        int[] colors = new int[breakdown.size()];

        for (int i = 0; i < breakdown.size(); i++) {
            ReportRankItem item = breakdown.get(i);
            float amount = ReportCursorHelper.parseAmount(item.getTotal());
            String label = item.getLabel() != null ? item.getLabel() : "—";
            entries.add(new PieEntry(amount));
            labels[i] = label;
            values[i] = ReportUiHelper.money(currency, amount);
            percents[i] = ReportUiHelper.percent(amount, sum);
            colors[i] = TYPE_COLORS[i % TYPE_COLORS.length];
        }

        List<Integer> colorList = new ArrayList<>();
        for (int color : colors) {
            colorList.add(color);
        }
        final float totalSum = sum;
        final String pieCenter = ReportUiHelper.money(currency, sum);
        ReportUiHelper.setupDonut(binding.chartDonut, entries, colorList, pieCenter);
        ReportUiHelper.fillLegend(binding.legendContainer, labels, values, percents, colors,
                index -> selectBreakdownItem(binding, breakdown, index, currency, totalSum,
                        pieCenter, true, true));
        ReportUiHelper.setupBars(binding.chartBar, breakdown);
        wireChartSelection(binding, breakdown, currency, totalSum, pieCenter, breakdown, totalSum);
    }

    private static void wireChartSelection(FragmentOperationalReportBinding binding,
                                           List<ReportRankItem> pieItems, String currency,
                                           float pieSum, String pieCenterText,
                                           List<ReportRankItem> barItems, float barSum) {
        if (binding == null || pieItems == null || pieItems.isEmpty()) {
            return;
        }
        Context context = binding.getRoot().getContext();
        showChartTapHint(binding);

        binding.chartDonut.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                selectBreakdownItem(binding, pieItems, (int) h.getX(), currency, pieSum, pieCenterText,
                        true, true);
            }

            @Override
            public void onNothingSelected() {
                clearBreakdownSelection(binding, pieCenterText);
            }
        });

        if (barItems != null && !barItems.isEmpty()) {
            boolean sameAsPie = pieItems == barItems;
            binding.chartBar.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
                    selectBreakdownItem(binding, barItems, (int) e.getX(), currency, barSum, pieCenterText,
                            sameAsPie, sameAsPie);
                }

                @Override
                public void onNothingSelected() {
                    clearBreakdownSelection(binding, pieCenterText);
                }
            });
        } else {
            binding.chartBar.setOnChartValueSelectedListener(null);
        }
    }

    private static void showChartTapHint(FragmentOperationalReportBinding binding) {
        if (binding.chartSelectionDetail == null) {
            return;
        }
        Context context = binding.getRoot().getContext();
        binding.chartSelectionDetail.setText(context.getString(R.string.ui_chart_tap_hint));
        binding.chartSelectionDetail.setTextColor(
                ContextCompat.getColor(context, R.color.colorTextSecondary));
    }

    private static boolean applyingChartSelection;

    private static void selectBreakdownItem(FragmentOperationalReportBinding binding,
                                            List<ReportRankItem> items, int index,
                                            String currency, float sum, String pieCenterText,
                                            boolean syncLegend, boolean updatePieCenter) {
        if (applyingChartSelection || binding == null || items == null
                || index < 0 || index >= items.size()) {
            return;
        }
        applyingChartSelection = true;
        try {
            applyBreakdownSelection(binding, items, index, currency, sum, pieCenterText,
                    syncLegend, updatePieCenter);
        } finally {
            applyingChartSelection = false;
        }
    }

    private static void applyBreakdownSelection(FragmentOperationalReportBinding binding,
                                                List<ReportRankItem> items, int index,
                                                String currency, float sum, String pieCenterText,
                                                boolean syncLegend, boolean updatePieCenter) {
        if (binding == null || items == null || index < 0 || index >= items.size()) {
            return;
        }
        Context context = binding.getRoot().getContext();
        ReportRankItem item = items.get(index);
        float amount = itemAmount(item);
        String label = item.getLabel() != null ? item.getLabel() : "—";
        String percent = ReportUiHelper.percent(amount, sum);

        StringBuilder detail = new StringBuilder(label);
        detail.append("\n").append(ReportUiHelper.money(currency, amount))
                .append(" · ").append(percent).append("%");
        if (item.getCount() != null && !item.getCount().trim().isEmpty()
                && item.getTotal() != null && !item.getTotal().trim().isEmpty()) {
            detail.append("\n")
                    .append(context.getString(R.string.ui_chart_bills_count, item.getCount().trim()));
        }
        binding.chartSelectionDetail.setText(detail.toString());
        binding.chartSelectionDetail.setTextColor(
                ContextCompat.getColor(context, R.color.colorTextPrimary));

        if (updatePieCenter) {
            String center = label + "\n" + ReportUiHelper.money(currency, amount) + "\n" + percent + "%";
            binding.chartDonut.setCenterText(center);
            binding.chartDonut.setCenterTextSize(11f);
            binding.chartDonut.highlightValue((float) index, 0, false);
        } else {
            binding.chartDonut.highlightValues(null);
        }
        binding.chartBar.highlightValue((float) index, 0, false);

        if (syncLegend) {
            ReportUiHelper.highlightLegendRow(binding.legendContainer, index);
        } else {
            ReportUiHelper.clearLegendHighlight(binding.legendContainer);
        }
    }

    private static void clearBreakdownSelection(FragmentOperationalReportBinding binding, String pieCenterText) {
        if (applyingChartSelection || binding == null) {
            return;
        }
        applyingChartSelection = true;
        try {
            showChartTapHint(binding);
            binding.chartDonut.setCenterText(pieCenterText != null ? pieCenterText : "");
            binding.chartDonut.setCenterTextSize(12f);
            binding.chartDonut.highlightValues(null);
            binding.chartBar.highlightValues(null);
            ReportUiHelper.clearLegendHighlight(binding.legendContainer);
        } finally {
            applyingChartSelection = false;
        }
    }

    private static float itemAmount(ReportRankItem item) {
        if (item == null) {
            return 0f;
        }
        float fromTotal = ReportCursorHelper.parseAmount(item.getTotal());
        if (fromTotal > 0f) {
            return fromTotal;
        }
        return ReportCursorHelper.parseAmount(item.getCount());
    }

    private static String formatGroupLabel(String column, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "Other";
        }
        String value = raw.trim();
        if ("invoiceType".equals(column)) {
            if ("table_wise".equalsIgnoreCase(value)) {
                return "Dine In";
            }
            if ("take_away".equalsIgnoreCase(value)) {
                return "Take Away";
            }
            if ("fast_billing".equalsIgnoreCase(value)) {
                return "Fast Billing";
            }
        }
        return value;
    }

    public static String formatPeriodLabel(String dateFilter) {
        if (dateFilter == null || dateFilter.trim().isEmpty()) {
            return "All Records";
        }
        return dateFilter.trim();
    }

    public static List<ReportRankItem> expenseBreakdown(POSBillingWalaDatabase db, String dateFilter) {
        List<ReportRankItem> items = new ArrayList<>();
        if (db == null) {
            return items;
        }
        SQLiteDatabase database = db.getReadableDatabase();
        try {
            String sql = "SELECT expensesName as grp, SUM(expensesAmount) as totalAmount, "
                    + "COUNT(expensesId) as totalCount FROM " + POSBillingWalaDatabase.EXPENSES_TABLE;
            if (dateFilter != null && !dateFilter.trim().isEmpty()) {
                sql += " WHERE expensesDate LIKE '%" + dateFilter.trim() + "%'";
            }
            sql += " GROUP BY expensesName ORDER BY totalAmount DESC";
            Cursor cursor = database.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                ReportRankItem item = new ReportRankItem();
                item.setLabel(cursor.getString(cursor.getColumnIndexOrThrow("grp")));
                item.setTotal(String.valueOf(ReportCursorHelper.readFloat(cursor, "totalAmount")));
                item.setCount(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("totalCount"))));
                items.add(item);
            }
            cursor.close();
        } finally {
            database.close();
        }
        return items;
    }

    public static List<ReportRankItem> fromProducts(List<InvoiceProductResponse> products, int maxItems) {
        List<ReportRankItem> items = new ArrayList<>();
        if (products == null) {
            return items;
        }
        int limit = maxItems > 0 ? Math.min(maxItems, products.size()) : products.size();
        for (int i = 0; i < limit; i++) {
            InvoiceProductResponse product = products.get(i);
            if (product == null) {
                continue;
            }
            ReportRankItem item = new ReportRankItem();
            item.setLabel(product.getDisplayLineName());
            float qty = ReportCursorHelper.parseAmount(product.getProductQuantity());
            float price = ReportCursorHelper.parseAmount(product.getProductPrice());
            item.setTotal(String.valueOf(price * qty));
            item.setCount(product.getProductQuantity());
            items.add(item);
        }
        return items;
    }
}
