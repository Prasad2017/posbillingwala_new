package com.pos_billingwala.Extra;

/**
 * Feature flag keys for the Universal POS Feature Engine.
 * Licence-backed flags map to existing {@link LicenseModules} prefs.
 * Template capabilities are gated by business template ∩ licence ∩ company toggles.
 */
public final class FeatureFlags {

    // --- Licence-backed (existing Home / KPI modules) ---
    public static final String FAST_BILLING = "fast_billing";
    public static final String DINE_IN = "dine_in";
    public static final String TAKE_AWAY = "take_away";
    public static final String MESS = "mess";
    public static final String TOTAL_SALE_DATA = "total_sale_data";
    public static final String TODAY_SALE_DATA = "today_sale_data";

    // --- Template capabilities ---
    public static final String PORTIONS = "portions";
    public static final String TABLES = "tables";
    public static final String KOT = "kot";
    public static final String BOT = "bot";
    public static final String COMBOS = "combos";
    public static final String INVENTORY = "inventory";
    /** Shop GST toggle ({@code company.gstStatus}); bill math still reads company row directly. */
    public static final String GST = "gst";
    public static final String BARCODE = "barcode";
    public static final String WEIGHT_SCALE = "weight_scale";
    public static final String VARIANTS = "variants";
    public static final String APPOINTMENTS = "appointments";
    public static final String CUSTOM_ORDERS = "custom_orders";
    public static final String WHOLESALE_PRICING = "wholesale_pricing";
    /** Product-level only ({@code product.openPrice}); not a shop toggle. */
    public static final String OPEN_PRICE = "open_price";

    private FeatureFlags() {
    }
}
