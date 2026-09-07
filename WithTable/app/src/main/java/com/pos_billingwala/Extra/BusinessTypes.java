package com.pos_billingwala.Extra;

/**
 * Canonical business-type ids for Universal POS templates.
 * Aliases normalize to a primary type via {@link #normalize(String)}.
 */
public final class BusinessTypes {

    // --- Primary types (Phase 02 catalog) ---
    public static final String RESTAURANT = "restaurant";
    public static final String BAR_RESTAURANT = "bar_restaurant";
    public static final String MESS = "mess";
    public static final String RETAIL = "retail";
    public static final String GROCERY = "grocery";
    public static final String WHOLESALE = "wholesale";
    public static final String WEIGHT_FRESH = "weight_fresh";
    public static final String FASHION = "fashion";
    public static final String JEWELLERY = "jewellery";
    public static final String SALON = "salon";
    public static final String BAKERY = "bakery";
    public static final String ELECTRONICS = "electronics";
    public static final String HARDWARE = "hardware";
    public static final String STATIONERY = "stationery";
    public static final String PET_SHOP = "pet_shop";
    public static final String LAUNDRY = "laundry";
    public static final String CAR_WASH = "car_wash";
    public static final String REPAIR = "repair";
    public static final String RENTAL = "rental";
    public static final String HEALTHCARE = "healthcare";
    public static final String CUSTOM = "custom";

    /** Live POS default until a shop picks another template. */
    public static final String DEFAULT = RESTAURANT;

    private BusinessTypes() {
    }

    /**
     * Normalize raw ids / aliases to a primary type.
     * Unknown values are kept lowercased so custom types still round-trip.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return DEFAULT;
        }
        String value = raw.trim().toLowerCase().replace(' ', '_').replace('-', '_');
        if (value.isEmpty()) {
            return DEFAULT;
        }
        switch (value) {
            case "food":
            case "food_hospitality":
            case "hotel":
            case "cafe":
            case "restaurant_food":
                return RESTAURANT;
            case "bar":
            case "beverage":
            case "bar_beverage":
            case "bar_and_restaurant":
                return BAR_RESTAURANT;
            case "mess_qr":
            case "tiffin":
                return MESS;
            case "shop":
            case "general_store":
            case "kirana":
                return RETAIL;
            case "supermarket":
                return GROCERY;
            case "fish":
            case "meat":
            case "chicken":
            case "vegetable":
            case "fruit":
            case "veg":
            case "fresh":
            case "weight":
            case "weight_based":
                return WEIGHT_FRESH;
            case "clothing":
            case "footwear":
            case "apparel":
                return FASHION;
            case "jewelry":
            case "jeweller":
                return JEWELLERY;
            case "beauty":
            case "spa":
            case "salon_beauty":
                return SALON;
            case "cake":
            case "cake_shop":
            case "bakery_cake":
                return BAKERY;
            case "mobile":
            case "mobile_shop":
                return ELECTRONICS;
            case "clinic":
            case "healthcare_ready":
                return HEALTHCARE;
            case "service":
            case "custom_business":
                return CUSTOM;
            default:
                return value;
        }
    }
}
