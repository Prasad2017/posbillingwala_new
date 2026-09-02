package com.pos_billingwala.Extra;

import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

import com.pos_billingwala.R;

/**
 * Window-size–based responsive helpers. Uses current {@code screenWidthDp} (available window
 * width), not device model or fixed pixel sizes.
 */
public final class ResponsiveUi {

    /** Minimum window width to apply multi-column / side-panel layouts (landscape phones). */
    public static final int WIDE_WINDOW_MIN_WIDTH_DP = 480;

    /** Minimum window width for a persistent side cart in POS screens. */
    public static final int SIDE_CART_MIN_WIDTH_DP = 520;

    /** Preferred minimum product card width when calculating grid columns. */
    public static final int PRODUCT_CARD_MIN_WIDTH_DP = 150;

    public static final int GRID_MIN_COLUMNS_PHONE = 2;
    public static final int GRID_MAX_COLUMNS = 6;

    private ResponsiveUi() {
    }

    /** Current window width in dp (handles multi-window and foldables). */
    public static int windowWidthDp(@NonNull Context context) {
        return context.getResources().getConfiguration().screenWidthDp;
    }

    public static int windowHeightDp(@NonNull Context context) {
        return context.getResources().getConfiguration().screenHeightDp;
    }

    public static boolean isLandscape(@NonNull Configuration configuration) {
        return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static boolean isLandscape(@NonNull Context context) {
        return isLandscape(context.getResources().getConfiguration());
    }

    /**
     * Wide enough for multi-column forms, split dashboards, or side cart — tablet class
     * OR current window width ≥ {@link #WIDE_WINDOW_MIN_WIDTH_DP}.
     */
    public static boolean isWideLayout(@NonNull Context context) {
        return TabletUi.isTablet(context) || windowWidthDp(context) >= WIDE_WINDOW_MIN_WIDTH_DP;
    }

    /** True when a persistent right-side cart panel should be shown (POS). */
    public static boolean useSideCartPanel(@NonNull Context context) {
        return windowWidthDp(context) >= SIDE_CART_MIN_WIDTH_DP;
    }

    /**
     * Grid columns from available width and minimum card width.
     *
     * @param availableWidthPx measured RecyclerView width in pixels; if ≤ 0, uses window width
     */
    public static int gridColumnCount(@NonNull Context context, int availableWidthPx) {
        int minCardDp = context.getResources().getInteger(R.integer.product_grid_min_card_width_dp);
        int minCols = context.getResources().getInteger(R.integer.product_grid_min_columns);
        int maxCols = context.getResources().getInteger(R.integer.product_grid_max_columns);

        int widthDp;
        if (availableWidthPx > 0) {
            float density = context.getResources().getDisplayMetrics().density;
            widthDp = Math.round(availableWidthPx / density);
        } else {
            widthDp = windowWidthDp(context);
        }
        return gridColumnCountForWidthDp(widthDp, minCardDp, minCols, maxCols);
    }

    public static int gridColumnCount(@NonNull Context context) {
        return gridColumnCount(context, 0);
    }

    public static int gridColumnCountForWidthDp(int widthDp, int minCardWidthDp, int minCols, int maxCols) {
        if (widthDp <= 0 || minCardWidthDp <= 0) {
            return Math.max(1, minCols);
        }
        int cols = Math.max(1, widthDp / minCardWidthDp);
        return Math.max(minCols, Math.min(maxCols, cols));
    }

    public static int gridColumnCountForWidthPx(@NonNull Context context, int availableWidthPx,
                                                int minCardWidthDp) {
        float density = context.getResources().getDisplayMetrics().density;
        int widthDp = Math.round(availableWidthPx / density);
        int minCols = context.getResources().getInteger(R.integer.product_grid_min_columns);
        int maxCols = context.getResources().getInteger(R.integer.product_grid_max_columns);
        return gridColumnCountForWidthDp(widthDp, minCardWidthDp, minCols, maxCols);
    }
}
