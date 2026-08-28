package com.posbillingwala.owner.Extra;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
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
        if (value == null || value.trim().isEmpty()) return MainActivity.currency + " 0";
        return MainActivity.currency + " " + value;
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

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase(Locale.US);
        return ("" + p[0].charAt(0) + p[p.length - 1].charAt(0)).toUpperCase(Locale.US);
    }
}
