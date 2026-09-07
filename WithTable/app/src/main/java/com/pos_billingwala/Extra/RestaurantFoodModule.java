package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.FoodTypeResponse;

/**
 * Restaurant / Food module facade over the live WithTable F&amp;B path.
 * Catalog: Food Type → Category → optional Subcategory → Product → Portion (+ Combos).
 * Billing modes: Fast / Dine-In (tables + KOT) / Takeaway; Mess is a sibling domain.
 * <p>
 * Extract-only Phase 05 — does not rewrite CreatePos or saveInvoice.
 */
public final class RestaurantFoodModule {

    public static final String CATALOG_HIERARCHY =
            "Food Type → Category → Subcategory → Product → Portion";

    private RestaurantFoodModule() {
    }

    /**
     * Templates that expect the F&amp;B catalog (food/beverage types, portions, KOT family).
     */
    public static boolean isFoodHospitalityTemplate(Context context) {
        String type = BusinessSession.getBusinessType(context);
        switch (BusinessTypes.normalize(type)) {
            case BusinessTypes.RESTAURANT:
            case BusinessTypes.BAR_RESTAURANT:
            case BusinessTypes.MESS:
            case BusinessTypes.BAKERY:
                return true;
            default:
                return false;
        }
    }

    public static boolean canFastBilling(Context context) {
        return UniversalBillingEngine.canStart(context, BillingMode.FAST);
    }

    public static boolean canDineIn(Context context) {
        return UniversalBillingEngine.canStart(context, BillingMode.TABLE);
    }

    public static boolean canTakeAway(Context context) {
        return UniversalBillingEngine.canStart(context, BillingMode.TAKEAWAY);
    }

    public static boolean canMess(Context context) {
        return MessModule.isEnabled(context);
    }

    public static boolean canKot(Context context, POSBillingWalaDatabase db) {
        return FeatureEngine.isEnabled(context, FeatureFlags.KOT, db);
    }

    public static boolean canKot(POSBillingWalaDatabase db) {
        return FeatureEngine.isEnabled(null, FeatureFlags.KOT, db);
    }

    public static boolean canPortions(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.PORTIONS);
    }

    public static boolean canCombos(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.COMBOS);
    }

    public static boolean canTablesFloor(Context context, POSBillingWalaDatabase db) {
        return FeatureEngine.isEnabled(context, FeatureFlags.TABLES, db);
    }

    public static boolean isTableWiseCart(String cartOrderStatus) {
        return UniversalBillingEngine.isTableWise(cartOrderStatus);
    }

    public static String cartOrderTable() {
        return BillingMode.TABLE.getWireValue();
    }

    public static String foodTypeCodeFood() {
        return FoodTypeResponse.CODE_FOOD;
    }

    public static String foodTypeCodeBeverage() {
        return FoodTypeResponse.CODE_BEVERAGE;
    }

    /**
     * Portion picker: if the product has portion rows, always offer them (correct price).
     * {@link FeatureFlags#PORTIONS} gates Master Data UI, not existing product data.
     */
    public static boolean shouldOfferPortionPicker(boolean productHasPortions) {
        return productHasPortions;
    }

    public static String moduleSummary(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append(CATALOG_HIERARCHY);
        sb.append("\nFast: ").append(canFastBilling(context) ? "on" : "off");
        sb.append(" · Dine-In: ").append(canDineIn(context) ? "on" : "off");
        sb.append(" · Takeaway: ").append(canTakeAway(context) ? "on" : "off");
        sb.append(" · Mess: ").append(canMess(context) ? "on" : "off");
        sb.append("\nPortions: ").append(canPortions(context) ? "on" : "off");
        sb.append(" · Combos: ").append(canCombos(context) ? "on" : "off");
        sb.append(" · KOT: ").append(canKot(context, null) ? "on" : "off");
        return sb.toString();
    }
}
