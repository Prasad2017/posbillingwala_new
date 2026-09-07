package com.pos_billingwala.Extra;

import android.content.Context;

/**
 * Product / service catalog engine facade over live Master Data.
 * Phase 13: Food Type → Category → Subcategory → Product → Portion (+ Combo).
 */
public final class ProductServiceEngine {

    public static final String HIERARCHY =
            "Food Type → Category → Subcategory → Product → Portion (+ Combo)";

    private ProductServiceEngine() {
    }

    public static boolean canPortions(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.PORTIONS);
    }

    public static boolean canCombos(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.COMBOS);
    }

    public static boolean canOpenPrice(Context context) {
        // Product-level flag; template may not list it — allow when F&B/retail inventory on
        return FeatureEngine.isEnabled(context, FeatureFlags.INVENTORY)
                || RestaurantFoodModule.isFoodHospitalityTemplate(context)
                || RetailGroceryModule.isRetailFamily(context);
    }

    public static String moduleSummary(Context context) {
        return HIERARCHY
                + "\nPortions UI: " + (canPortions(context) ? "on" : "off")
                + " · Combos: " + (canCombos(context) ? "on" : "off")
                + "\nEntry: Settings → Master Data";
    }
}
