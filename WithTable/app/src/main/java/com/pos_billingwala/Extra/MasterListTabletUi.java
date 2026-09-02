package com.pos_billingwala.Extra;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Splits a simple master screen into form (left) + list (right) on tablet.
 */
public final class MasterListTabletUi {

    private MasterListTabletUi() {
    }

    public static void applyFormListSplit(Activity activity, LinearLayout container,
                                          View formSection, View listSection) {
        if (activity == null || !ResponsiveUi.isWideLayout(activity) || container == null
                || formSection == null || listSection == null) {
            return;
        }
        container.removeAllViews();

        float density = activity.getResources().getDisplayMetrics().density;
        int gap = (int) (12 * density);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout left = new LinearLayout(activity);
        left.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.38f);
        leftParams.setMarginEnd(gap);
        left.setLayoutParams(leftParams);

        LinearLayout right = new LinearLayout(activity);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.62f));

        addSection(left, formSection, 0);
        addSection(right, listSection, 0);

        row.addView(left);
        row.addView(right);
        container.addView(row);
    }

    public static int listColumnCount(Activity activity) {
        return activity != null ? TabletUi.gridColumnCount(activity) : 1;
    }

    /** Keeps a header (e.g. product info) full width; splits form and list below on tablet. */
    public static void applyFormListSplitBelowHeader(Activity activity, LinearLayout container,
                                                     View header, View formSection, View listSection) {
        if (activity == null || !ResponsiveUi.isWideLayout(activity) || container == null
                || formSection == null || listSection == null) {
            return;
        }
        container.removeView(formSection);
        container.removeView(listSection);

        float density = activity.getResources().getDisplayMetrics().density;
        int gap = (int) (12 * density);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = gap;
        row.setLayoutParams(rowParams);

        LinearLayout left = new LinearLayout(activity);
        left.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.38f);
        leftParams.setMarginEnd(gap);
        left.setLayoutParams(leftParams);

        LinearLayout right = new LinearLayout(activity);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.62f));

        addSection(left, formSection, 0);
        addSection(right, listSection, 0);

        row.addView(left);
        row.addView(right);

        int insertIndex = header != null ? container.indexOfChild(header) + 1 : container.getChildCount();
        container.addView(row, insertIndex);
    }

    private static void addSection(LinearLayout column, View section, int topMarginPx) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = topMarginPx;
        section.setLayoutParams(params);
        column.addView(section);
    }
}
