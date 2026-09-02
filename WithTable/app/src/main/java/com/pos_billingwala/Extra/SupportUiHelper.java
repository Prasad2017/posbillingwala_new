package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.pos_billingwala.R;

import java.util.List;

public final class SupportUiHelper {

    private SupportUiHelper() {}

    public static LinearLayout form(Activity activity) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, ResponsiveUi.isWideLayout(activity) ? TabletUi.horizontalInsetDp(activity) : 16);
        root.setPadding(pad, pad, pad, pad);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    /** Centers form content on tablet with responsive max width. */
    public static ScrollView wrapScreen(Activity activity, LinearLayout form) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        if (ResponsiveUi.isWideLayout(activity)) {
            LinearLayout outer = new LinearLayout(activity);
            outer.setOrientation(LinearLayout.VERTICAL);
            outer.setGravity(Gravity.CENTER_HORIZONTAL);
            outer.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout.LayoutParams formLp = new LinearLayout.LayoutParams(
                    TabletUi.contentMaxWidthPx(activity),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            form.setLayoutParams(formLp);
            outer.addView(form);
            scroll.addView(outer);
        } else {
            scroll.addView(form);
        }
        return scroll;
    }

    public static TextView notice(Activity activity, LinearLayout root, String text) {
        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(activity, R.color.light_black));
        UiStyle.applyTextSize(tv, R.dimen.text_row_compact);
        tv.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
        tv.setBackgroundResource(R.drawable.button_rounded_border);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(activity, 12);
        tv.setLayoutParams(lp);
        root.addView(tv);
        return tv;
    }

    public static EditText field(Activity activity, LinearLayout root, String label, String hint) {
        TextView lbl = new TextView(activity);
        lbl.setText(label);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        UiStyle.applyTextSize(lbl, R.dimen.text_body);
        lbl.setTextColor(ContextCompat.getColor(activity, R.color.black));
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.bottomMargin = dp(activity, 4);
        lbl.setLayoutParams(ll);
        root.addView(lbl);

        EditText input = new EditText(activity);
        input.setHint(hint);
        input.setBackgroundResource(R.drawable.button_rounded_border);
        input.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(activity, 12);
        input.setLayoutParams(lp);
        root.addView(input);
        return input;
    }

    public static Button primary(Activity activity, LinearLayout root, String label) {
        Button btn = new Button(activity);
        btn.setText(label);
        btn.setAllCaps(false);
        btn.setBackgroundResource(R.drawable.button_rounded_border);
        btn.setBackgroundTintList(ContextCompat.getColorStateList(activity, R.color.colorPrimary));
        btn.setTextColor(ContextCompat.getColor(activity, R.color.white));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(activity, 8);
        btn.setLayoutParams(lp);
        root.addView(btn);
        return btn;
    }

    public static void addScreenHeader(Activity activity, LinearLayout root, String title, Runnable onBack) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(0, 0, 0, dp(activity, 8));
        TextView back = new TextView(activity);
        back.setText("←");
        UiStyle.applyTextSize(back, R.dimen.text_heading);
        back.setTypeface(Typeface.DEFAULT_BOLD);
        back.setPadding(dp(activity, 4), dp(activity, 4), dp(activity, 12), dp(activity, 4));
        back.setOnClickListener(v -> {
            if (onBack != null) onBack.run();
        });
        TextView heading = new TextView(activity);
        heading.setText(title);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        UiStyle.applyTextSize(heading, R.dimen.text_title);
        heading.setTextColor(ContextCompat.getColor(activity, R.color.black));
        bar.addView(back);
        bar.addView(heading);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(activity, 8);
        bar.setLayoutParams(lp);
        root.addView(bar, 0);
    }

    /** Places category and subject fields side-by-side on tablet. */
    public static void applyFieldPairRow(Activity activity, LinearLayout root,
                                         EditText firstField, EditText secondField) {
        if (!ResponsiveUi.isWideLayout(activity) || firstField == null || secondField == null) {
            return;
        }
        int firstLabelIdx = root.indexOfChild(firstField) - 1;
        int secondLabelIdx = root.indexOfChild(secondField) - 1;
        if (firstLabelIdx < 0 || secondLabelIdx < 0) {
            return;
        }

        View firstLabel = root.getChildAt(firstLabelIdx);
        View secondLabel = root.getChildAt(secondLabelIdx);

        root.removeView(secondField);
        root.removeView(secondLabel);
        root.removeView(firstField);
        root.removeView(firstLabel);

        int insertAt = Math.min(firstLabelIdx, secondLabelIdx);
        LinearLayout row = newSideBySideColumns(activity, firstLabel, firstField, secondLabel, secondField);
        root.addView(row, insertAt);
    }

    /** Places two action buttons in one row on tablet. */
    public static void applySideBySideButtons(Activity activity, LinearLayout root, View left, View right) {
        if (!ResponsiveUi.isWideLayout(activity) || left == null || right == null) {
            return;
        }
        root.removeView(left);
        root.removeView(right);

        int gap = dp(activity, 8);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = gap;
        row.setLayoutParams(rowLp);

        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        leftLp.setMarginEnd(gap / 2);
        left.setLayoutParams(leftLp);

        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rightLp.setMarginStart(gap / 2);
        right.setLayoutParams(rightLp);

        row.addView(left);
        row.addView(right);
        root.addView(row);
    }

    public static LinearLayout createTicketListContainer(Activity activity) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return container;
    }

    public static void clearTicketList(LinearLayout container) {
        if (container != null) {
            container.removeAllViews();
        }
    }

    /** Adds ticket cards in a responsive grid (1 / 2 / 3 columns). */
    public static void populateTicketGrid(Activity activity, LinearLayout container, List<TextView> cards) {
        if (container == null || cards == null) {
            return;
        }
        container.removeAllViews();
        int columns = TabletUi.gridColumnCount(activity);
        if (columns <= 1) {
            for (TextView card : cards) {
                applyTicketCardMargins(activity, card, dp(activity, 12));
                container.addView(card);
            }
            return;
        }

        int gap = dp(activity, 12);
        for (int i = 0; i < cards.size(); i += columns) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBaselineAligned(false);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                rowLp.topMargin = gap;
            }
            row.setLayoutParams(rowLp);

            for (int col = 0; col < columns; col++) {
                int index = i + col;
                if (index >= cards.size()) {
                    break;
                }
                TextView card = cards.get(index);
                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                if (col > 0) {
                    cardLp.setMarginStart(gap / 2);
                }
                if (col < columns - 1) {
                    cardLp.setMarginEnd(gap / 2);
                }
                card.setLayoutParams(cardLp);
                row.addView(card);
            }
            container.addView(row);
        }
    }

    public static TextView buildTicketCard(Activity activity, String ticketNo, String status,
                                           String subject, String createdAt, Runnable onClick) {
        TextView tv = new TextView(activity);
        tv.setBackgroundResource(R.drawable.button_rounded_border);
        tv.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        tv.setText(ticketNo + "  ·  " + status + "\n" + subject + "\n" + createdAt);
        tv.setTextColor(ContextCompat.getColor(activity, R.color.black));
        tv.setOnClickListener(v -> {
            if (onClick != null) {
                onClick.run();
            }
        });
        return tv;
    }

    public static void styleDetailBody(TextView body) {
        if (body == null) {
            return;
        }
        UiStyle.applyTextSize(body, R.dimen.text_body);
        body.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(body.getContext(), 8);
        lp.bottomMargin = dp(body.getContext(), 8);
        body.setLayoutParams(lp);
    }

    private static LinearLayout newSideBySideColumns(Activity activity,
                                                     View leftLabel, View leftField,
                                                     View rightLabel, View rightField) {
        int gap = dp(activity, 12);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = gap;
        row.setLayoutParams(rowLp);

        row.addView(newFieldColumn(activity, leftLabel, leftField), newColumnParams(gap, true));
        row.addView(newFieldColumn(activity, rightLabel, rightField), newColumnParams(gap, false));
        return row;
    }

    private static LinearLayout newFieldColumn(Activity activity, View label, View field) {
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(label);
        column.addView(field);
        return column;
    }

    private static LinearLayout.LayoutParams newColumnParams(int gap, boolean isLeft) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (isLeft) {
            lp.setMarginEnd(gap / 2);
        } else {
            lp.setMarginStart(gap / 2);
        }
        return lp;
    }

    private static void applyTicketCardMargins(Activity activity, TextView card, int bottomMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = bottomMargin;
        card.setLayoutParams(lp);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
