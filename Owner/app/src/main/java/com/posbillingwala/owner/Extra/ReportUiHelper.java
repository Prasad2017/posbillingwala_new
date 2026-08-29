package com.posbillingwala.owner.Extra;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Model.ReportRankItem;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.databinding.IncludeReportKpiCardBinding;
import com.posbillingwala.owner.databinding.ItemReportRankRowBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReportUiHelper {

    private ReportUiHelper() {
    }

    public static void bindKpi(IncludeReportKpiCardBinding card, String label, String value, String trend) {
        if (card == null) return;
        card.kpiLabel.setText(label);
        card.kpiValue.setText(value != null ? value : "0");
        card.kpiTrend.setText(trend != null ? trend : "");
        boolean down = trend != null && trend.trim().startsWith("-");
        card.kpiTrend.setTextColor(ContextCompat.getColor(card.getRoot().getContext(),
                down ? R.color.statusExpired : R.color.statusActive));
    }

    public static String money(String value) {
        return money(MainActivity.currency, parseAmount(value));
    }

    public static String money(float amount) {
        return money(MainActivity.currency, amount);
    }

    public static String money(String currency, float amount) {
        String prefix = currency != null && !currency.trim().isEmpty() ? currency.trim() : "₹";
        return prefix + " " + twoDecimals(amount);
    }

    public static String money(String currency, String value) {
        return money(currency, parseAmount(value));
    }

    public static String twoDecimals(String value) {
        return twoDecimals(parseAmount(value));
    }

    public static String twoDecimals(float amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    public static float parseAmount(String value) {
        if (value == null || value.trim().isEmpty()) return 0f;
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    public static void fillRankList(LinearLayout container, List<ReportRankItem> items, boolean moneyValue) {
        if (container == null) return;
        container.removeAllViews();
        Context ctx = container.getContext();
        if (items == null || items.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText(R.string.no_data_found);
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
            String sub = item.getCount() != null ? item.getCount() + " bills" : "";
            row.rankSubtitle.setText(sub);
            row.rankSubtitle.setVisibility(sub.isEmpty() ? View.GONE : View.VISIBLE);
            row.rankValue.setText(moneyValue
                    ? money(item.getTotalSales() != null ? item.getTotalSales() : item.getAmount())
                    : (item.getTotalSales() != null ? item.getTotalSales() : "0"));
            container.addView(row.getRoot());
        }
    }

    public static void setupLine(LineChart chart, List<ReportRankItem> points) {
        if (chart == null) return;
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (points != null) {
            for (int i = 0; i < points.size(); i++) {
                ReportRankItem p = points.get(i);
                entries.add(new Entry(i, parseAmount(p.getTotal())));
                String d = p.getDate() != null ? p.getDate() : "";
                labels.add(d.length() >= 10 ? d.substring(8, 10) : (p.getLabel() != null ? p.getLabel() : d));
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
        chart.invalidate();
    }

    public static void setupDonut(PieChart chart, List<PieEntry> entries, List<Integer> colors, String center) {
        if (chart == null) {
            return;
        }
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(62f);
        chart.setTransparentCircleRadius(68f);
        chart.setHoleColor(Color.WHITE);
        chart.setCenterText(center != null ? center : "");
        chart.setCenterTextSize(13f);
        chart.setCenterTextColor(Color.parseColor("#1F2937"));
        chart.getLegend().setEnabled(false);
        chart.setRotationEnabled(false);
        chart.setTouchEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        chart.setDrawEntryLabels(false);
        if (entries == null || entries.isEmpty()) {
            chart.clear();
            chart.invalidate();
            return;
        }
        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(colors);
        ds.setDrawValues(false);
        ds.setSliceSpace(2f);
        chart.setData(new PieData(ds));
        chart.invalidate();
    }

    public interface OnIndexClick {
        void onClick(int index);
    }

    public static void fillLegend(LinearLayout container, String[] labels, String[] counts, String[] percents, int[] colors) {
        fillLegend(container, labels, counts, percents, colors, null);
    }

    public static void fillLegend(LinearLayout container, String[] labels, String[] counts,
                                  String[] percents, int[] colors, OnIndexClick onClick) {
        if (container == null) {
            return;
        }
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        int n = labels == null ? 0 : labels.length;
        for (int i = 0; i < n; i++) {
            final int index = i;
            LinearLayout row = (LinearLayout) inflater.inflate(R.layout.item_report_legend_row, container, false);
            android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
            dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            if (colors != null && i < colors.length) {
                dot.setColor(colors[i]);
            }
            row.findViewById(R.id.legendDot).setBackground(dot);
            ((TextView) row.findViewById(R.id.legendLabel)).setText(labels[i]);
            ((TextView) row.findViewById(R.id.legendCount)).setText(i < counts.length ? nz(counts[i]) : "0.00");
            String pct = i < percents.length ? nz(percents[i]) : "0";
            ((TextView) row.findViewById(R.id.legendPercent)).setText(pct + "%");
            if (onClick != null) {
                row.setClickable(true);
                row.setFocusable(true);
                row.setOnClickListener(v -> onClick.onClick(index));
            }
            container.addView(row);
        }
    }

    public static void highlightLegend(LinearLayout container, int selectedIndex) {
        if (container == null) {
            return;
        }
        int highlight = ContextCompat.getColor(container.getContext(), R.color.colorPrimaryLight);
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            child.setBackgroundColor(i == selectedIndex ? highlight : Color.TRANSPARENT);
        }
    }

    public static void bindDonutSelection(PieChart chart, TextView detail, LinearLayout legend,
                                          String defaultCenter, float total, boolean moneyValues,
                                          OnIndexClick onOpen) {
        if (chart == null) {
            return;
        }
        Context ctx = chart.getContext();
        if (detail != null) {
            detail.setText(ctx.getString(R.string.home_chart_tap_hint));
            detail.setOnClickListener(v -> {
                Object tag = v.getTag();
                if (tag instanceof Integer && onOpen != null) {
                    onOpen.onClick((Integer) tag);
                }
            });
        }
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (!(e instanceof PieEntry)) {
                    return;
                }
                PieEntry pe = (PieEntry) e;
                String label = pe.getLabel() != null ? pe.getLabel() : "";
                if (label.equals(ctx.getString(R.string.home_no_chart_data))) {
                    return;
                }
                int index = h != null ? (int) h.getX() : 0;
                String amount = moneyValues ? money(pe.getValue())
                        : String.valueOf(Math.round(pe.getValue()));
                chart.setCenterText(label + "\n" + amount);
                chart.setCenterTextSize(11f);
                if (detail != null) {
                    detail.setText(label + "  " + amount + "  (" + percentOf(pe.getValue(), total) + "%)"
                            + "\n" + ctx.getString(R.string.home_chart_tap_open));
                    detail.setTag(index);
                }
                highlightLegend(legend, index);
            }

            @Override
            public void onNothingSelected() {
                chart.setCenterText(defaultCenter != null ? defaultCenter : "");
                chart.setCenterTextSize(13f);
                highlightLegend(legend, -1);
                if (detail != null) {
                    detail.setText(ctx.getString(R.string.home_chart_tap_hint));
                    detail.setTag(null);
                }
            }
        });
    }

    public static String nz(String v) {
        return v == null || v.trim().isEmpty() ? "0" : v.trim();
    }

    public static String percentOf(float part, float total) {
        if (total <= 0f) {
            return "0";
        }
        return String.valueOf(Math.round(part * 100f / total));
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase(Locale.US);
        return ("" + p[0].charAt(0) + p[p.length - 1].charAt(0)).toUpperCase(Locale.US);
    }
}
