package com.posbillingwala.admin.Extra;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.posbillingwala.admin.Model.ReportRankItem;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.databinding.IncludeReportKpiCardBinding;
import com.posbillingwala.admin.databinding.ItemReportLegendRowBinding;
import com.posbillingwala.admin.databinding.ItemReportRankRowBinding;

import java.util.ArrayList;
import java.util.List;

public final class ReportUiHelper {
    private ReportUiHelper() {}

    public static void bindKpi(IncludeReportKpiCardBinding card, String label, String value, String trend) {
        if (card == null) return;
        card.kpiLabel.setText(label);
        card.kpiValue.setText(value != null ? value : "0");
        card.kpiTrend.setText(trend != null ? trend : "");
        boolean down = trend != null && trend.trim().startsWith("-");
        card.kpiTrend.setTextColor(ContextCompat.getColor(card.getRoot().getContext(),
                down ? R.color.statusExpired : R.color.statusActive));
    }

    public static String money(String v) {
        if (v == null || v.trim().isEmpty()) return "₹ 0";
        return "₹ " + v;
    }

    public static String nz(String v) {
        return v == null || v.trim().isEmpty() ? "0" : v.trim();
    }

    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
        return ("" + p[0].charAt(0) + p[p.length - 1].charAt(0)).toUpperCase();
    }

    public static void fillRankList(LinearLayout container, List<ReportRankItem> items, boolean moneyValue) {
        if (container == null) return;
        container.removeAllViews();
        Context ctx = container.getContext();
        if (items == null || items.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText("No data");
            empty.setTextColor(ContextCompat.getColor(ctx, R.color.colorTextHint));
            empty.setPadding(0, 24, 0, 24);
            container.addView(empty);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(ctx);
        for (ReportRankItem item : items) {
            ItemReportRankRowBinding row = ItemReportRankRowBinding.inflate(inflater, container, false);
            String title = item.displayName();
            row.rankInitials.setText(initials(title));
            row.rankTitle.setText(title);
            String sub = item.getCustomerName();
            if (item.getShopName() != null && item.getCustomerName() != null
                    && !item.getShopName().equals(item.getCustomerName())) {
                sub = item.getCustomerName();
            } else if (item.getLabel() != null && item.getCount() != null) {
                sub = "";
            } else {
                sub = item.getShopName() != null ? item.getShopName() : "";
            }
            row.rankSubtitle.setText(sub != null ? sub : "");
            row.rankSubtitle.setVisibility(sub == null || sub.isEmpty() ? View.GONE : View.VISIBLE);
            row.rankValue.setText(moneyValue ? money(item.getTotalSales() != null ? item.getTotalSales() : item.getAmount())
                    : item.displayValue());
            container.addView(row.getRoot());
        }
    }

    public static void fillLegend(LinearLayout container, String[] labels, String[] counts, String[] percents, int[] colors) {
        if (container == null) return;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        for (int i = 0; i < labels.length; i++) {
            final int color = colors[i];
            ItemReportLegendRowBinding row = ItemReportLegendRowBinding.inflate(inflater, container, false);
            android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
            dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dot.setColor(color);
            row.legendDot.setBackground(dot);
            row.legendLabel.setText(labels[i]);
            row.legendCount.setText(nz(counts[i]));
            row.legendPercent.setText(nz(percents[i]) + "%");
            container.addView(row.getRoot());
        }
    }

    public static void setupDonut(PieChart chart, List<PieEntry> entries, List<Integer> colors, String center) {
        if (chart == null) return;
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(62f);
        chart.setTransparentCircleRadius(66f);
        chart.setCenterText(center != null ? center : "");
        chart.setCenterTextSize(16f);
        chart.setCenterTextColor(Color.parseColor("#1F2937"));
        chart.getLegend().setEnabled(false);
        chart.setRotationEnabled(false);
        if (entries == null || entries.isEmpty()) {
            chart.clear();
            return;
        }
        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(colors);
        ds.setDrawValues(false);
        chart.setData(new PieData(ds));
        chart.invalidate();
    }

    public static void setupLine(LineChart chart, List<ReportRankItem> points) {
        if (chart == null) return;
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.setTouchEnabled(true);
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (points != null) {
            for (int i = 0; i < points.size(); i++) {
                ReportRankItem p = points.get(i);
                float y = 0f;
                try { y = Float.parseFloat(p.getTotal() != null ? p.getTotal() : "0"); } catch (Exception ignored) {}
                entries.add(new Entry(i, y));
                String d = p.getDate() != null ? p.getDate() : "";
                labels.add(d.length() >= 10 ? d.substring(8, 10) : d);
            }
        }
        if (entries.isEmpty()) {
            chart.clear();
            return;
        }
        LineDataSet ds = new LineDataSet(entries, "Sales");
        ds.setColor(Color.parseColor("#2563EB"));
        ds.setCircleColor(Color.parseColor("#2563EB"));
        ds.setLineWidth(2.5f);
        ds.setCircleRadius(4f);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds.setDrawFilled(true);
        ds.setFillColor(Color.parseColor("#DBEAFE"));
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.setData(new LineData(ds));
        chart.invalidate();
    }

    public static void setupBars(BarChart chart, List<ReportRankItem> points) {
        if (chart == null) return;
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (points != null) {
            for (int i = 0; i < points.size(); i++) {
                ReportRankItem p = points.get(i);
                float y = 0f;
                try { y = Float.parseFloat(p.getCount() != null ? p.getCount() : (p.getTotal() != null ? p.getTotal() : "0")); } catch (Exception ignored) {}
                entries.add(new BarEntry(i, y));
                labels.add(p.getLabel() != null ? p.getLabel() : String.valueOf(i + 1));
            }
        }
        if (entries.isEmpty()) {
            chart.clear();
            return;
        }
        BarDataSet ds = new BarDataSet(entries, "");
        ds.setColor(Color.parseColor("#2563EB"));
        ds.setDrawValues(false);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setGranularity(1f);
        chart.setData(new BarData(ds));
        chart.setFitBars(true);
        chart.invalidate();
    }
}
