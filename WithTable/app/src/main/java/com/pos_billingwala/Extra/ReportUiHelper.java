package com.pos_billingwala.Extra;

import android.content.Context;
import android.graphics.Color;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
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
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Model.ReportRankItem;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.IncludeReportKpiCardBinding;
import com.pos_billingwala.databinding.IncludeReportInvoiceDetailHeaderBinding;
import com.pos_billingwala.databinding.IncludeReportTableHeaderBinding;
import com.pos_billingwala.databinding.ItemReportLegendRowBinding;
import com.pos_billingwala.databinding.ItemReportRankRowBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReportUiHelper {

    private ReportUiHelper() {
    }

    public static void bindKpi(IncludeReportKpiCardBinding card, String label, String value, String trend) {
        if (card == null) {
            return;
        }
        card.kpiLabel.setText(label);
        card.kpiValue.setText(value != null ? value : "0");
        card.kpiTrend.setText(trend != null ? trend : "");
        boolean down = trend != null && trend.trim().startsWith("-");
        card.kpiTrend.setTextColor(ContextCompat.getColor(card.getRoot().getContext(),
                down ? R.color.statusExpired : R.color.statusActive));
    }

    public static String money(String currency, float amount) {
        String prefix = currency != null && !currency.trim().isEmpty() ? currency.trim() : "₹";
        return prefix + " " + String.format(Locale.US, "%.2f", amount);
    }

    public static String money(String currency, String value) {
        return money(currency, ReportCursorHelper.parseAmount(value));
    }

    public static String nz(String v) {
        return v == null || v.trim().isEmpty() ? "0" : v.trim();
    }

    public static String percent(float part, float total) {
        if (total <= 0f) {
            return "0";
        }
        return String.format(Locale.US, "%.0f", (part / total) * 100f);
    }

    public static void setupTableHeader(LinearLayout container, String middleColumnTitle) {
        setupTableHeader(container, middleColumnTitle, null);
    }

    public static void setupTableHeader(LinearLayout container, String middleColumnTitle,
                                        String rightColumnTitle) {
        if (container == null) {
            return;
        }
        container.removeAllViews();
        Context ctx = container.getContext();
        IncludeReportTableHeaderBinding header = IncludeReportTableHeaderBinding.inflate(
                LayoutInflater.from(ctx), container, false);
        header.headerMiddle.setText(middleColumnTitle != null ? middleColumnTitle : "");
        if (rightColumnTitle != null) {
            header.headerAmount.setText(rightColumnTitle);
        }
        container.addView(header.getRoot());
        container.setVisibility(View.VISIBLE);
    }

    public static void setupDetailTableHeader(LinearLayout container, String dateTitle,
                                              String middleTitle, String rightTitle) {
        if (container == null) {
            return;
        }
        container.removeAllViews();
        Context ctx = container.getContext();
        IncludeReportInvoiceDetailHeaderBinding header = IncludeReportInvoiceDetailHeaderBinding.inflate(
                LayoutInflater.from(ctx), container, false);
        header.headerDate.setText(dateTitle != null ? dateTitle : "");
        header.headerInvoiceNo.setText(middleTitle != null ? middleTitle : "");
        if (rightTitle != null) {
            header.headerAmount.setText(rightTitle);
        }
        container.addView(header.getRoot());
        container.setVisibility(View.VISIBLE);
    }

    public static void hideTableHeader(LinearLayout container) {
        if (container == null) {
            return;
        }
        container.removeAllViews();
        container.setVisibility(View.GONE);
    }

    public static void fillLegend(LinearLayout container, String[] labels, String[] values,
                                  String[] percents, int[] colors) {
        fillLegend(container, labels, values, percents, colors, null);
    }

    public static void fillLegend(LinearLayout container, String[] labels, String[] values,
                                  String[] percents, int[] colors, OnLegendItemClickListener listener) {
        if (container == null) {
            return;
        }
        container.removeAllViews();
        if (labels == null || labels.length == 0) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        for (int i = 0; i < labels.length; i++) {
            final int color = colors != null && i < colors.length
                    ? colors[i] : Color.parseColor("#4862b7");
            ItemReportLegendRowBinding row = ItemReportLegendRowBinding.inflate(inflater, container, false);
            android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
            dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dot.setColor(color);
            row.legendDot.setBackground(dot);
            row.legendLabel.setText(labels[i] != null ? labels[i] : "—");
            String value = values != null && i < values.length ? values[i] : "0";
            if (value == null || value.trim().isEmpty()) {
                value = "0";
            }
            row.legendCount.setText(value.trim());
            String pct = percents != null && i < percents.length ? nz(percents[i]) : "0";
            row.legendPercent.setText(pct + "%");
            if (listener != null) {
                row.getRoot().setClickable(true);
                row.getRoot().setFocusable(true);
                row.getRoot().setBackgroundResource(android.R.color.transparent);
                row.getRoot().setForeground(
                        ContextCompat.getDrawable(container.getContext(),
                                android.R.drawable.list_selector_background));
                final int index = i;
                row.getRoot().setOnClickListener(v -> listener.onLegendItemClick(index));
            }
            container.addView(row.getRoot());
        }
    }

    public interface OnLegendItemClickListener {
        void onLegendItemClick(int index);
    }

    public static void highlightLegendRow(LinearLayout container, int selectedIndex) {
        if (container == null) {
            return;
        }
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (selectedIndex == i) {
                child.setBackgroundResource(R.drawable.bg_product_info_panel);
            } else {
                child.setBackgroundResource(android.R.color.transparent);
            }
        }
    }

    public static void clearLegendHighlight(LinearLayout container) {
        highlightLegendRow(container, -1);
    }

    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.US);
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase(Locale.US);
    }

    public static void fillRankList(LinearLayout container, List<ReportRankItem> items, boolean moneyValue) {
        if (container == null) {
            return;
        }
        container.removeAllViews();
        Context ctx = container.getContext();
        if (items == null || items.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText(R.string.ui_no_data_found);
            empty.setTextColor(ContextCompat.getColor(ctx, R.color.colorTextHint));
            empty.setPadding(0, 24, 0, 24);
            container.addView(empty);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(ctx);
        String currency = MainActivity.currencyName;
        for (ReportRankItem item : items) {
            ItemReportRankRowBinding row = ItemReportRankRowBinding.inflate(inflater, container, false);
            String title = item.displayName();
            row.rankInitials.setText(initials(title));
            row.rankTitle.setText(title);
            String sub = item.getCount() != null ? item.getCount() + " bills" : "";
            row.rankSubtitle.setText(sub);
            row.rankSubtitle.setVisibility(sub.isEmpty() ? View.GONE : View.VISIBLE);
            row.rankValue.setText(moneyValue
                    ? money(currency, item.getTotalSales() != null ? item.getTotalSales() : item.getAmount())
                    : item.displayValue());
            container.addView(row.getRoot());
        }
    }

    public static void setupLine(LineChart chart, List<ReportRankItem> points) {
        if (chart == null) {
            return;
        }
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
                entries.add(new Entry(i, ReportCursorHelper.parseAmount(p.getTotal())));
                String d = p.getDate() != null ? p.getDate() : "";
                labels.add(!d.isEmpty() && d.length() >= 10 ? d.substring(8, 10)
                        : (p.getLabel() != null ? p.getLabel() : d));
            }
        }
        if (entries.isEmpty()) {
            chart.clear();
            return;
        }
        LineDataSet ds = new LineDataSet(entries, "Sales");
        ds.setColor(Color.parseColor("#4862b7"));
        ds.setCircleColor(Color.parseColor("#4862b7"));
        ds.setLineWidth(2.5f);
        ds.setCircleRadius(4f);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds.setDrawFilled(true);
        ds.setFillColor(Color.parseColor("#E8ECF8"));
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.setData(new LineData(ds));
        configureZoomableChart(chart);
        chart.invalidate();
    }

    private static void configureZoomableChart(BarLineChartBase<?> chart) {
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(true);
        chart.setDragXEnabled(true);
        chart.setDragYEnabled(true);
        chart.setScaleXEnabled(true);
        chart.setScaleYEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        if (chart.getData() != null && chart.getData().getEntryCount() > 4) {
            chart.setVisibleXRangeMaximum(4f);
            chart.moveViewToX(0f);
        }
    }

    public static void enablePiePinchZoom(PieChart chart) {
        if (chart == null) {
            return;
        }
        final float minScale = 1f;
        final float maxScale = 2.5f;
        Context ctx = chart.getContext();
        final ScaleGestureDetector scaleDetector = new ScaleGestureDetector(ctx,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float newScale = chart.getScaleX() * detector.getScaleFactor();
                        newScale = Math.max(minScale, Math.min(newScale, maxScale));
                        chart.setScaleX(newScale);
                        chart.setScaleY(newScale);
                        return true;
                    }
                });
        final GestureDetector gestureDetector = new GestureDetector(ctx,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        resetViewScale(chart);
                        return true;
                    }
                });
        chart.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            if (event.getPointerCount() > 1) {
                scaleDetector.onTouchEvent(event);
                return true;
            }
            scaleDetector.onTouchEvent(event);
            return false;
        });
    }

    public static void resetViewScale(View view) {
        if (view != null) {
            view.setScaleX(1f);
            view.setScaleY(1f);
        }
    }

    public static void setupBars(BarChart chart, List<ReportRankItem> points) {
        if (chart == null) {
            return;
        }
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.setTouchEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (points != null) {
            for (int i = 0; i < points.size(); i++) {
                ReportRankItem p = points.get(i);
                float y = ReportCursorHelper.parseAmount(p.getCount() != null ? p.getCount()
                        : (p.getTotal() != null ? p.getTotal() : "0"));
                entries.add(new BarEntry(i, y));
                labels.add(p.getLabel() != null ? p.getLabel() : String.valueOf(i + 1));
            }
        }
        if (entries.isEmpty()) {
            chart.clear();
            return;
        }
        BarDataSet ds = new BarDataSet(entries, "");
        ds.setColor(Color.parseColor("#4862b7"));
        ds.setDrawValues(false);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setGranularity(1f);
        chart.setData(new BarData(ds));
        chart.setFitBars(true);
        configureZoomableChart(chart);
        chart.invalidate();
    }

    public static void setupDonut(PieChart chart, List<PieEntry> entries, List<Integer> colors, String center) {
        if (chart == null) {
            return;
        }
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(58f);
        chart.setTransparentCircleRadius(62f);
        chart.setDrawEntryLabels(false);
        chart.setUsePercentValues(false);
        chart.setTouchEnabled(true);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        chart.setExtraOffsets(8f, 8f, 8f, 8f);
        chart.getLegend().setEnabled(false);

        String centerText = center != null ? center.trim() : "";
        if (centerText.length() > 14) {
            chart.setCenterText("Total\n" + centerText);
            chart.setCenterTextSize(10f);
        } else {
            chart.setCenterText(centerText);
            chart.setCenterTextSize(12f);
        }
        chart.setCenterTextColor(Color.parseColor("#1F2937"));

        if (entries == null || entries.isEmpty()) {
            chart.clear();
            return;
        }
        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(colors);
        ds.setDrawValues(false);
        ds.setSliceSpace(3f);
        ds.setSelectionShift(4f);
        chart.setData(new PieData(ds));
        enablePiePinchZoom(chart);
        chart.invalidate();
    }
}
