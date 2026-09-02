package com.pos_billingwala.Extra;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

/**
 * Print preview and checkout layout helpers for wide windows (tablet + landscape phone).
 */
public final class TabletPrintUi {

    private TabletPrintUi() {
    }

    /** No-op: orientation is user-controlled ({@code fullUser} in manifest). */
    public static void applyLandscape(@Nullable Activity activity) {
        // intentionally empty — do not force landscape on any device class
    }

    /** Reprint / duplicate bill checkout — cart left, payment right. */
    public static void applyCheckoutTablet(@Nullable Activity activity, @Nullable RelativeLayout cartLayout,
                                           @Nullable View cartSection, @Nullable View paymentSection) {
        TabletFormUi.applyCartPaymentSplit(activity, cartLayout, cartSection, paymentSection);
    }

    /** Widen a centered receipt or token preview for on-screen reading. */
    public static void applyReceiptPreviewTablet(@Nullable View receiptScroll, int widthDp) {
        if (receiptScroll != null) {
            TabletFormUi.applyCenteredReceiptPreview(receiptScroll, widthDp);
        }
    }

    /** Center a short form (mess scan, walk-in token, etc.). */
    public static void applyCenteredForm(@Nullable Activity activity, @Nullable View formRoot) {
        if (formRoot != null) {
            TabletFormUi.applyCenteredPanel(formRoot);
        }
    }

    /** Center a short form with explicit max width (dp). */
    public static void applyCenteredForm(@Nullable Activity activity, @Nullable View formRoot, int maxWidthDp) {
        if (formRoot != null) {
            TabletFormUi.applyCenteredPanel(formRoot, maxWidthDp);
        }
    }

    /** Walk-in token: centered form with name and mobile side-by-side. */
    public static void applyWalkInFormTablet(@Nullable Activity activity,
                                             @Nullable LinearLayout formContainer) {
        if (activity == null || formContainer == null || !ResponsiveUi.isWideLayout(activity)
                || formContainer.getChildCount() < 6) {
            return;
        }
        TabletFormUi.applyCenteredPanel(formContainer);

        View hint = formContainer.getChildAt(0);
        View name = formContainer.getChildAt(1);
        View mobile = formContainer.getChildAt(2);
        View amount = formContainer.getChildAt(3);
        View messTypeHint = formContainer.getChildAt(4);
        View issueButton = formContainer.getChildAt(5);

        formContainer.removeAllViews();

        float density = activity.getResources().getDisplayMetrics().density;
        int gap = (int) (12 * density);

        formContainer.addView(hint);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = gap;
        row.setLayoutParams(rowParams);

        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        leftParams.setMarginEnd(gap / 2);
        name.setLayoutParams(leftParams);

        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rightParams.setMarginStart(gap / 2);
        mobile.setLayoutParams(rightParams);

        row.addView(name);
        row.addView(mobile);
        formContainer.addView(row);

        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        amountParams.topMargin = gap;
        amount.setLayoutParams(amountParams);
        formContainer.addView(amount);

        LinearLayout.LayoutParams mealParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mealParams.topMargin = gap;
        messTypeHint.setLayoutParams(mealParams);
        formContainer.addView(messTypeHint);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = gap;
        issueButton.setLayoutParams(buttonParams);
        formContainer.addView(issueButton);
    }
}
