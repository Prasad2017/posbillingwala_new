package com.pos_billingwala.Extra;

import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

/**
 * Responsive breakpoints for POS layouts across phones and tablets.
 * <ul>
 *   <li>Phone: smallest width &lt; 600dp</li>
 *   <li>Tablet (7"): ≥ 600dp ({@code layout-sw600dp})</li>
 *   <li>Large tablet (10"): ≥ 720dp</li>
 *   <li>Expanded tablet / desktop: ≥ 840dp</li>
 * </ul>
 */
public final class TabletUi {

    public static final int TABLET_SMALLEST_WIDTH_DP = 600;
    public static final int LARGE_TABLET_SMALLEST_WIDTH_DP = 720;
    public static final int EXPANDED_TABLET_SMALLEST_WIDTH_DP = 840;

    private TabletUi() {
    }

    public static int smallestScreenWidthDp(@NonNull Context context) {
        return context.getResources().getConfiguration().smallestScreenWidthDp;
    }

    public static boolean isTablet(@NonNull Context context) {
        return smallestScreenWidthDp(context) >= TABLET_SMALLEST_WIDTH_DP;
    }

    public static boolean isLargeTablet(@NonNull Context context) {
        return smallestScreenWidthDp(context) >= LARGE_TABLET_SMALLEST_WIDTH_DP;
    }

    public static boolean isExpandedTablet(@NonNull Context context) {
        return smallestScreenWidthDp(context) >= EXPANDED_TABLET_SMALLEST_WIDTH_DP;
    }

    public static boolean isLandscape(@NonNull Configuration configuration) {
        return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /** Max width for centered auth / support / settings forms. */
    public static int formPanelMaxWidthDp(@NonNull Context context) {
        int sw = smallestScreenWidthDp(context);
        if (sw >= EXPANDED_TABLET_SMALLEST_WIDTH_DP) {
            return 720;
        }
        if (sw >= LARGE_TABLET_SMALLEST_WIDTH_DP) {
            return 640;
        }
        if (sw >= TABLET_SMALLEST_WIDTH_DP) {
            return 560;
        }
        return Integer.MAX_VALUE;
    }

    /** Max width for bottom sheets and device-picker dialogs on tablet. */
    public static int bottomSheetMaxWidthDp(@NonNull Context context) {
        int sw = smallestScreenWidthDp(context);
        if (sw >= EXPANDED_TABLET_SMALLEST_WIDTH_DP) {
            return 600;
        }
        if (sw >= LARGE_TABLET_SMALLEST_WIDTH_DP) {
            return 560;
        }
        if (sw >= TABLET_SMALLEST_WIDTH_DP) {
            return 520;
        }
        return Integer.MAX_VALUE;
    }

    /** Grid columns for product cards, ticket lists, order tiles, etc. */
    public static int gridColumnCount(@NonNull Context context) {
        int sw = smallestScreenWidthDp(context);
        if (sw >= EXPANDED_TABLET_SMALLEST_WIDTH_DP) {
            return 3;
        }
        if (sw >= TABLET_SMALLEST_WIDTH_DP) {
            return 2;
        }
        return 1;
    }

    public static int horizontalInsetDp(@NonNull Context context) {
        if (isExpandedTablet(context)) {
            return 32;
        }
        if (isLargeTablet(context)) {
            return 28;
        }
        if (isTablet(context)) {
            return 24;
        }
        return 16;
    }

    public static int contentMaxWidthPx(@NonNull Context context) {
        return dpToPx(context, formPanelMaxWidthDp(context));
    }

    public static int dpToPx(@NonNull Context context, int dp) {
        if (dp == Integer.MAX_VALUE) {
            return context.getResources().getDisplayMetrics().widthPixels;
        }
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
