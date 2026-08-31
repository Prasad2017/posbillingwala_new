package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable tablet form and menu layout helpers (sw600dp+).
 */
public final class TabletFormUi {

    private TabletFormUi() {
    }

    /** Pairs direct children into two-column rows (e.g. TextInputLayouts). */
    public static void applyTwoColumnFields(@Nullable Activity activity, @Nullable LinearLayout container) {
        if (activity == null || !TabletUi.isTablet(activity) || container == null) {
            return;
        }
        int count = container.getChildCount();
        if (count <= 1) {
            return;
        }

        List<View> children = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            children.add(container.getChildAt(i));
        }
        container.removeAllViews();

        float density = activity.getResources().getDisplayMetrics().density;
        int gap = (int) (12 * density);
        int halfGap = gap / 2;

        for (int i = 0; i < children.size(); i += 2) {
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                rowParams.topMargin = gap;
            }

            if (i + 1 < children.size()) {
                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setBaselineAligned(false);
                row.setLayoutParams(rowParams);

                View left = children.get(i);
                View right = children.get(i + 1);

                LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                leftParams.setMarginEnd(halfGap);
                left.setLayoutParams(leftParams);

                LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                rightParams.setMarginStart(halfGap);
                right.setLayoutParams(rightParams);

                row.addView(left);
                row.addView(right);
                container.addView(row);
            } else {
                View single = children.get(i);
                single.setLayoutParams(rowParams);
                container.addView(single);
            }
        }
    }

    /** Splits CardView sections into two balanced columns. */
    public static void applyTwoColumnCards(@Nullable Activity activity, @Nullable LinearLayout container,
                                           @NonNull View[] leftCards, @NonNull View[] rightCards) {
        if (activity == null || !TabletUi.isTablet(activity) || container == null) {
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

        LinearLayout leftColumn = new LinearLayout(activity);
        leftColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        leftParams.setMarginEnd(gap);
        leftColumn.setLayoutParams(leftParams);

        LinearLayout rightColumn = new LinearLayout(activity);
        rightColumn.setOrientation(LinearLayout.VERTICAL);
        rightColumn.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        addCard(leftColumn, leftCards, gap);
        addCard(rightColumn, rightCards, gap);

        row.addView(leftColumn);
        row.addView(rightColumn);
        container.addView(row);
    }

    /**
     * Splits a vertical menu (row + divider pairs) into two columns on tablet.
     * Expects clickable rows as horizontal LinearLayouts with ids.
     */
    public static void applyTwoColumnMenu(@Nullable Activity activity, @Nullable LinearLayout container) {
        if (activity == null || !TabletUi.isTablet(activity) || container == null) {
            return;
        }

        List<View[]> entries = new ArrayList<>();
        int index = 0;
        while (index < container.getChildCount()) {
            View child = container.getChildAt(index);
            if (child instanceof LinearLayout && child.getId() != View.NO_ID) {
                View divider = null;
                int next = index + 1;
                if (next < container.getChildCount()) {
                    View maybeDivider = container.getChildAt(next);
                    if (isMenuDivider(maybeDivider)) {
                        divider = maybeDivider;
                        index = next + 1;
                    } else {
                        index++;
                    }
                } else {
                    index++;
                }
                entries.add(new View[]{child, divider});
            } else {
                index++;
            }
        }
        if (entries.size() <= 1) {
            return;
        }

        container.removeAllViews();

        float density = activity.getResources().getDisplayMetrics().density;
        int gap = (int) (12 * density);
        int mid = (entries.size() + 1) / 2;

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout leftColumn = new LinearLayout(activity);
        leftColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        leftParams.setMarginEnd(gap);
        leftColumn.setLayoutParams(leftParams);

        LinearLayout rightColumn = new LinearLayout(activity);
        rightColumn.setOrientation(LinearLayout.VERTICAL);
        rightColumn.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        for (int i = 0; i < entries.size(); i++) {
            LinearLayout column = i < mid ? leftColumn : rightColumn;
            addMenuEntry(column, entries.get(i), i == 0 || i == mid ? 0 : gap);
        }

        row.addView(leftColumn);
        row.addView(rightColumn);
        container.addView(row);
    }

    /** About screen: brand hero left, contact + disclaimer right; footer stays full width. */
    public static void applyAboutLayout(@Nullable Activity activity, @Nullable LinearLayout container) {
        if (activity == null || !TabletUi.isTablet(activity) || container == null
                || container.getChildCount() < 4) {
            return;
        }

        View brandHero = container.getChildAt(0);
        View contactLabel = container.getChildAt(1);
        View contactCard = container.getChildAt(2);
        View disclaimer = container.getChildAt(3);

        List<View> footer = new ArrayList<>();
        for (int i = 4; i < container.getChildCount(); i++) {
            footer.add(container.getChildAt(i));
        }

        container.removeAllViews();

        float density = activity.getResources().getDisplayMetrics().density;
        int gap = (int) (16 * density);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout left = new LinearLayout(activity);
        left.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.42f);
        leftParams.setMarginEnd(gap);
        left.setLayoutParams(leftParams);
        addSection(left, brandHero, 0);

        LinearLayout right = new LinearLayout(activity);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.58f));
        addSection(right, contactLabel, 0);
        addSection(right, contactCard, gap / 2);
        addSection(right, disclaimer, gap);

        row.addView(left);
        row.addView(right);
        container.addView(row);

        for (int i = 0; i < footer.size(); i++) {
            addSection(container, footer.get(i), gap);
        }
    }

    /** Centers auth panels on tablet using responsive max width for the device class. */
    public static void applyCenteredPanel(@NonNull View panel) {
        applyCenteredPanel(panel, TabletUi.formPanelMaxWidthDp(panel.getContext()));
    }

    /** Centers auth panels on tablet (login, register, MPIN). */
    public static void applyCenteredPanel(@NonNull View panel, int maxWidthDp) {
        Context ctx = panel.getContext();
        if (!TabletUi.isTablet(ctx) || maxWidthDp == Integer.MAX_VALUE) {
            return;
        }
        ViewGroup.LayoutParams lp = panel.getLayoutParams();
        if (lp == null) {
            return;
        }
        int maxPx = (int) (maxWidthDp * ctx.getResources().getDisplayMetrics().density);
        lp.width = maxPx;
        if (lp instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) lp).gravity = Gravity.CENTER_HORIZONTAL;
        } else if (lp instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) lp).gravity = Gravity.CENTER_HORIZONTAL;
        }
        panel.setLayoutParams(lp);
    }

    /** Checkout: cart lines left, payment controls right (BluetoothPrint). */
    public static void applyCartPaymentSplit(@Nullable Activity activity, @Nullable RelativeLayout cartLayout,
                                           @Nullable View cartSection, @Nullable View paymentSection) {
        if (activity == null || !TabletUi.isTablet(activity) || cartLayout == null
                || cartSection == null || paymentSection == null) {
            return;
        }
        View footer = cartLayout.findViewById(R.id.cartAmountLayout);
        if (footer == null) {
            return;
        }
        ViewGroup.LayoutParams cartParams = cartSection.getLayoutParams();
        ViewGroup.LayoutParams paymentParams = paymentSection.getLayoutParams();
        if (!(cartParams instanceof RelativeLayout.LayoutParams)
                || !(paymentParams instanceof RelativeLayout.LayoutParams)) {
            return;
        }

        cartLayout.removeView(cartSection);
        cartLayout.removeView(paymentSection);

        float density = activity.getResources().getDisplayMetrics().density;
        int gap = (int) (12 * density);

        LinearLayout row = new LinearLayout(activity);
        row.setId(View.generateViewId());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        RelativeLayout.LayoutParams rowParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rowParams.addRule(RelativeLayout.ABOVE, R.id.cartAmountLayout);
        rowParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        row.setLayoutParams(rowParams);

        LinearLayout left = new LinearLayout(activity);
        left.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.58f);
        leftParams.setMarginEnd(gap);
        left.setLayoutParams(leftParams);

        LinearLayout right = new LinearLayout(activity);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.42f));

        RelativeLayout.LayoutParams cartLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        cartSection.setLayoutParams(cartLp);

        LinearLayout.LayoutParams paymentLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paymentSection.setLayoutParams(paymentLp);

        addSection(left, cartSection, 0);
        addSection(right, paymentSection, 0);

        row.addView(left);
        row.addView(right);
        cartLayout.addView(row);
    }

    /** Edit bill: line items left, totals/actions in a right sidebar. */
    public static void applyEditBillSplit(@Nullable Activity activity, @NonNull View productList,
                                          @NonNull View sidePanel, int sidePanelWidthDp) {
        if (activity == null || !TabletUi.isTablet(activity)) {
            return;
        }
        float density = activity.getResources().getDisplayMetrics().density;
        int sideWidth = (int) (sidePanelWidthDp * density);

        ViewGroup.LayoutParams panelLp = sidePanel.getLayoutParams();
        if (panelLp instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams panelParams = (RelativeLayout.LayoutParams) panelLp;
            panelParams.width = sideWidth;
            panelParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            panelParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            panelParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            panelParams.addRule(RelativeLayout.BELOW, com.pos_billingwala.R.id.appBarLayout);
            sidePanel.setLayoutParams(panelParams);
        }

        ViewGroup.LayoutParams listLp = productList.getLayoutParams();
        if (listLp instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams listParams = (RelativeLayout.LayoutParams) listLp;
            listParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            listParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            listParams.addRule(RelativeLayout.START_OF, sidePanel.getId());
            listParams.removeRule(RelativeLayout.ABOVE);
            listParams.addRule(RelativeLayout.BELOW, com.pos_billingwala.R.id.appBarLayout);
            productList.setLayoutParams(listParams);
        }
    }

    /** Widen and center on-screen receipt preview on tablet. */
    public static void applyCenteredReceiptPreview(@NonNull View receiptScroll, int widthDp) {
        Context ctx = receiptScroll.getContext();
        if (!TabletUi.isTablet(ctx)) {
            return;
        }
        int widthPx = (int) (widthDp * ctx.getResources().getDisplayMetrics().density);
        ViewGroup.LayoutParams lp = receiptScroll.getLayoutParams();
        if (lp == null) {
            return;
        }
        lp.width = widthPx;
        if (lp instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) lp;
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            params.removeRule(RelativeLayout.CENTER_IN_PARENT);
        }
        receiptScroll.setLayoutParams(lp);
    }

    private static boolean isMenuDivider(View view) {
        if (view == null) {
            return false;
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        return view.getId() == View.NO_ID
                && params != null
                && params.height > 0
                && params.height <= view.getResources().getDisplayMetrics().density * 2;
    }

    private static void addCard(LinearLayout column, View[] cards, int gap) {
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) {
                addSection(column, cards[i], i == 0 ? 0 : gap);
            }
        }
    }

    private static void addMenuEntry(LinearLayout column, View[] entry, int topMargin) {
        addSection(column, entry[0], topMargin);
        if (entry[1] != null) {
            addSection(column, entry[1], (int) (6 * column.getResources().getDisplayMetrics().density));
        }
    }

    private static void addSection(LinearLayout column, View section, int topMarginPx) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = topMarginPx;
        section.setLayoutParams(params);
        column.addView(section);
    }
}
