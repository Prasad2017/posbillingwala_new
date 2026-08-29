package com.posbillingwala.dealer.Extra;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.posbillingwala.dealer.R;

import java.util.List;

public final class ReportUiHelper {
    private ReportUiHelper() {}

    public static String nz(String v) {
        return v == null || v.trim().isEmpty() ? "0" : v.trim();
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

    public static void fillLegend(LinearLayout container, String[] labels, String[] counts, String[] percents, int[] colors) {
        if (container == null) return;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        for (int i = 0; i < labels.length; i++) {
            LinearLayout row = (LinearLayout) inflater.inflate(R.layout.item_report_legend_row, container, false);
            android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
            dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dot.setColor(colors[i]);
            row.findViewById(R.id.legendDot).setBackground(dot);
            ((TextView) row.findViewById(R.id.legendLabel)).setText(labels[i]);
            ((TextView) row.findViewById(R.id.legendCount)).setText(nz(counts[i]));
            ((TextView) row.findViewById(R.id.legendPercent)).setText(nz(percents[i]) + "%");
            container.addView(row);
        }
    }
}
