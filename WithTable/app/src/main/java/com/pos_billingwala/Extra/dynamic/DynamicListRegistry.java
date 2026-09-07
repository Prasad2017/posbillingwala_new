package com.pos_billingwala.Extra.dynamic;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.pos_billingwala.Extra.DynamicUiEngine;
import com.pos_billingwala.Extra.FeatureEngine;
import com.pos_billingwala.Extra.FeatureFlags;
import com.pos_billingwala.Extra.dynamicui.UiCodes;
import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dynamic list / column facade (Phase 04/05/11).
 * Does not rewrite adapters — exposes which columns/filters ProductMaster & Inventory should emphasize.
 */
public final class DynamicListRegistry {

    public static final String COL_NAME = "NAME";
    public static final String COL_CODE = "CODE";
    public static final String COL_PRICE = "PRICE";
    public static final String COL_CATEGORY = "CATEGORY";
    public static final String COL_UNIT = "UNIT";
    public static final String COL_TAX = "TAX";
    public static final String COL_STOCK_QTY = "STOCK_QTY";

    private DynamicListRegistry() {
    }

    public static List<String> productColumns(Context context) {
        List<String> cols = new ArrayList<>();
        cols.add(COL_NAME);
        cols.add(COL_CATEGORY);
        if (FormRegistry.isProductFieldVisible(context, UiCodes.PF_SKU)
                || FormRegistry.isProductFieldVisible(context, UiCodes.PF_BARCODE)) {
            cols.add(COL_CODE);
        }
        if (FormRegistry.isProductFieldVisible(context, UiCodes.PF_PRICE)) {
            cols.add(COL_PRICE);
        }
        cols.add(COL_UNIT);
        if (FeatureEngine.isEnabled(context, FeatureFlags.GST)
                && FormRegistry.isProductFieldVisible(context, UiCodes.PF_TAX)) {
            cols.add(COL_TAX);
        }
        return Collections.unmodifiableList(cols);
    }

    public static List<String> inventoryColumns(Context context) {
        List<String> cols = new ArrayList<>();
        cols.add(COL_NAME);
        cols.add(COL_STOCK_QTY);
        if (FormRegistry.isProductFieldVisible(context, UiCodes.PF_SKU)
                || FormRegistry.isProductFieldVisible(context, UiCodes.PF_BARCODE)) {
            cols.add(COL_CODE);
        }
        return Collections.unmodifiableList(cols);
    }

    public static boolean showProductCode(Context context) {
        return productColumns(context).contains(COL_CODE);
    }

    public static void applyProductSearchHint(Context context, @Nullable TextView search) {
        if (search == null || context == null) {
            return;
        }
        if (DynamicUiEngine.isBillingFieldVisible(context, UiCodes.BF_BARCODE)
                || FormRegistry.isProductFieldVisible(context, UiCodes.PF_BARCODE)) {
            search.setHint(context.getString(R.string.ui_search_or_scan_barcode));
        }
    }
}
