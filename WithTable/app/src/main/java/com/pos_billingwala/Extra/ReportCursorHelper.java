package com.pos_billingwala.Extra;

import android.database.Cursor;

import androidx.core.widget.NestedScrollView;

/**
 * Safe helpers for invoice/report screens — avoids NPE on null DB values.
 */
public final class ReportCursorHelper {

    private ReportCursorHelper() {
    }

    public static float readFloat(Cursor cursor, String column) {
        if (cursor == null) {
            return 0f;
        }
        int idx = cursor.getColumnIndex(column);
        if (idx < 0) {
            return 0f;
        }
        String value = cursor.getString(idx);
        return parseAmount(value);
    }

    public static float parseAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0f;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    /** Rupee discount from stored invoice discount + type (Amount vs Percentage). */
    public static float discountRupees(String discountRaw, String discountType, String subTotalRaw) {
        return amountOrPercentRupees(discountRaw, discountType, subTotalRaw);
    }

    /** Rupee packing charge from stored packing + type (Amount vs Percentage) — same rules as discount. */
    public static float packingRupees(String packingRaw, String packingType, String subTotalRaw) {
        return amountOrPercentRupees(packingRaw, packingType, subTotalRaw);
    }

    /** Convert Amount or Percentage-of-subtotal into rupees. */
    public static float amountOrPercentRupees(String raw, String type, String subTotalRaw) {
        float value = parseAmount(raw);
        if (value == 0f) {
            return 0f;
        }
        if (type != null && type.trim().equalsIgnoreCase("Amount")) {
            return value;
        }
        float subAmt = parseAmount(subTotalRaw);
        if (subAmt == 0f) {
            return 0f;
        }
        return subAmt * value / 100f;
    }

    public static String formatInvoiceDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        String trimmed = raw.trim();
        return trimmed.length() >= 10 ? trimmed.substring(0, 10) : trimmed;
    }

    public static boolean isNestedScrollAtBottom(NestedScrollView scrollView, int scrollY) {
        if (scrollView == null || scrollView.getChildCount() == 0) {
            return false;
        }
        android.view.View child = scrollView.getChildAt(0);
        if (child == null) {
            return false;
        }
        int childHeight = child.getMeasuredHeight();
        int viewHeight = scrollView.getMeasuredHeight();
        return childHeight > 0 && scrollY >= childHeight - viewHeight;
    }
}
