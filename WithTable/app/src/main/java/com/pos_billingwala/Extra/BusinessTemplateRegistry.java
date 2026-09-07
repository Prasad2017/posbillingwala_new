package com.pos_billingwala.Extra;

import android.content.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Built-in templates. Default restaurant+mess matches today's live POS.
 */
public final class BusinessTemplateRegistry {

    public static final String TEMPLATE_RESTAURANT_DEFAULT = "restaurant_default";
    public static final String TEMPLATE_BAR_RESTAURANT = "bar_restaurant_default";
    public static final String TEMPLATE_MESS_FOCUSED = "mess_focused";
    public static final String TEMPLATE_RETAIL_DEFAULT = "retail_default";
    public static final String TEMPLATE_GROCERY_DEFAULT = "grocery_default";
    public static final String TEMPLATE_WEIGHT_FRESH = "weight_fresh_default";
    public static final String TEMPLATE_WHOLESALE_DEFAULT = "wholesale_default";
    public static final String TEMPLATE_FASHION_DEFAULT = "fashion_default";
    public static final String TEMPLATE_JEWELLERY_DEFAULT = "jewellery_default";
    public static final String TEMPLATE_SALON_DEFAULT = "salon_default";
    public static final String TEMPLATE_BAKERY_DEFAULT = "bakery_default";

    private BusinessTemplateRegistry() {
    }

    public static BusinessTemplate resolve(Context context) {
        BusinessTemplate custom = BusinessTemplateJson.parse(BusinessConfigStore.getTemplateJson(context));
        if (custom != null) {
            return custom;
        }
        String type = BusinessSession.getBusinessType(context);
        String templateId = BusinessSession.getTemplateId(context);
        BusinessTemplate byId = findById(templateId);
        if (byId != null) {
            return byId;
        }
        return forBusinessType(type);
    }

    public static BusinessTemplate forBusinessType(String businessType) {
        String type = BusinessTypes.normalize(businessType);
        switch (type) {
            case BusinessTypes.BAR_RESTAURANT:
                return barRestaurant();
            case BusinessTypes.MESS:
                return messFocused();
            case BusinessTypes.RETAIL:
            case BusinessTypes.ELECTRONICS:
            case BusinessTypes.HARDWARE:
            case BusinessTypes.STATIONERY:
            case BusinessTypes.PET_SHOP:
            case BusinessTypes.RENTAL:
                return retailDefault();
            case BusinessTypes.GROCERY:
                return groceryDefault();
            case BusinessTypes.WEIGHT_FRESH:
                return weightFreshDefault();
            case BusinessTypes.WHOLESALE:
                return wholesaleDefault();
            case BusinessTypes.FASHION:
                return fashionDefault();
            case BusinessTypes.JEWELLERY:
                return jewelleryDefault();
            case BusinessTypes.SALON:
            case BusinessTypes.LAUNDRY:
            case BusinessTypes.CAR_WASH:
            case BusinessTypes.REPAIR:
            case BusinessTypes.HEALTHCARE:
                return salonDefault();
            case BusinessTypes.BAKERY:
                return bakeryDefault();
            case BusinessTypes.RESTAURANT:
            case BusinessTypes.CUSTOM:
            default:
                return restaurantDefault();
        }
    }

    public static BusinessTemplate findById(String templateId) {
        if (templateId == null || templateId.trim().isEmpty()) {
            return null;
        }
        switch (templateId.trim()) {
            case TEMPLATE_RESTAURANT_DEFAULT:
                return restaurantDefault();
            case TEMPLATE_BAR_RESTAURANT:
                return barRestaurant();
            case TEMPLATE_MESS_FOCUSED:
                return messFocused();
            case TEMPLATE_RETAIL_DEFAULT:
                return retailDefault();
            case TEMPLATE_GROCERY_DEFAULT:
                return groceryDefault();
            case TEMPLATE_WEIGHT_FRESH:
                return weightFreshDefault();
            case TEMPLATE_WHOLESALE_DEFAULT:
                return wholesaleDefault();
            case TEMPLATE_FASHION_DEFAULT:
                return fashionDefault();
            case TEMPLATE_JEWELLERY_DEFAULT:
                return jewelleryDefault();
            case TEMPLATE_SALON_DEFAULT:
                return salonDefault();
            case TEMPLATE_BAKERY_DEFAULT:
                return bakeryDefault();
            default:
                return null;
        }
    }

    /** Current production behaviour: Fast / Dine-In / Takeaway / Mess + F&B catalog tools. */
    public static BusinessTemplate restaurantDefault() {
        return new BusinessTemplate(
                TEMPLATE_RESTAURANT_DEFAULT,
                BusinessTypes.RESTAURANT,
                "Restaurant / Food",
                features(
                        FeatureFlags.FAST_BILLING,
                        FeatureFlags.DINE_IN,
                        FeatureFlags.TAKE_AWAY,
                        FeatureFlags.MESS,
                        FeatureFlags.TOTAL_SALE_DATA,
                        FeatureFlags.TODAY_SALE_DATA,
                        FeatureFlags.PORTIONS,
                        FeatureFlags.TABLES,
                        FeatureFlags.KOT,
                        FeatureFlags.COMBOS,
                        FeatureFlags.INVENTORY,
                        FeatureFlags.GST
                )
        );
    }

    public static BusinessTemplate barRestaurant() {
        Set<String> flags = new LinkedHashSet<>(restaurantDefault().getEnabledFeatures());
        flags.add(FeatureFlags.BOT);
        return new BusinessTemplate(
                TEMPLATE_BAR_RESTAURANT,
                BusinessTypes.BAR_RESTAURANT,
                "Bar + Restaurant",
                flags
        );
    }

    public static BusinessTemplate messFocused() {
        return new BusinessTemplate(
                TEMPLATE_MESS_FOCUSED,
                BusinessTypes.MESS,
                "Mess / Tiffin",
                features(
                        FeatureFlags.FAST_BILLING,
                        FeatureFlags.MESS,
                        FeatureFlags.TOTAL_SALE_DATA,
                        FeatureFlags.TODAY_SALE_DATA,
                        FeatureFlags.PORTIONS,
                        FeatureFlags.INVENTORY,
                        FeatureFlags.GST
                )
        );
    }

    public static BusinessTemplate retailDefault() {
        return new BusinessTemplate(
                TEMPLATE_RETAIL_DEFAULT,
                BusinessTypes.RETAIL,
                "Retail / General Shop",
                features(
                        FeatureFlags.FAST_BILLING,
                        FeatureFlags.TOTAL_SALE_DATA,
                        FeatureFlags.TODAY_SALE_DATA,
                        FeatureFlags.BARCODE,
                        FeatureFlags.INVENTORY,
                        FeatureFlags.GST
                )
        );
    }

    public static BusinessTemplate groceryDefault() {
        return new BusinessTemplate(
                TEMPLATE_GROCERY_DEFAULT,
                BusinessTypes.GROCERY,
                "Grocery / Kirana",
                features(
                        FeatureFlags.FAST_BILLING,
                        FeatureFlags.TOTAL_SALE_DATA,
                        FeatureFlags.TODAY_SALE_DATA,
                        FeatureFlags.BARCODE,
                        FeatureFlags.WEIGHT_SCALE,
                        FeatureFlags.INVENTORY,
                        FeatureFlags.GST
                )
        );
    }

    public static BusinessTemplate weightFreshDefault() {
        return new BusinessTemplate(
                TEMPLATE_WEIGHT_FRESH,
                BusinessTypes.WEIGHT_FRESH,
                "Weight / Fresh",
                features(
                        FeatureFlags.FAST_BILLING,
                        FeatureFlags.TOTAL_SALE_DATA,
                        FeatureFlags.TODAY_SALE_DATA,
                        FeatureFlags.WEIGHT_SCALE,
                        FeatureFlags.INVENTORY,
                        FeatureFlags.GST
                )
        );
    }

    public static BusinessTemplate wholesaleDefault() {
        return new BusinessTemplate(
                TEMPLATE_WHOLESALE_DEFAULT,
                BusinessTypes.WHOLESALE,
                "Wholesale",
                features(
                        FeatureFlags.FAST_BILLING,
                        FeatureFlags.TOTAL_SALE_DATA,
                        FeatureFlags.TODAY_SALE_DATA,
                        FeatureFlags.BARCODE,
                        FeatureFlags.WHOLESALE_PRICING,
                        FeatureFlags.INVENTORY,
                        FeatureFlags.GST
                )
        );
    }

    public static BusinessTemplate fashionDefault() {
        return new BusinessTemplate(
                TEMPLATE_FASHION_DEFAULT,
                BusinessTypes.FASHION,
                "Clothing / Footwear",
                features(
                        FeatureFlags.FAST_BILLING,
                        FeatureFlags.TOTAL_SALE_DATA,
                        FeatureFlags.TODAY_SALE_DATA,
                        FeatureFlags.BARCODE,
                        FeatureFlags.VARIANTS,
                        FeatureFlags.INVENTORY,
                        FeatureFlags.GST
                )
        );
    }

    public static BusinessTemplate jewelleryDefault() {
        return new BusinessTemplate(
                TEMPLATE_JEWELLERY_DEFAULT,
                BusinessTypes.JEWELLERY,
                "Jewellery",
                features(
                        FeatureFlags.FAST_BILLING,
                        FeatureFlags.TOTAL_SALE_DATA,
                        FeatureFlags.TODAY_SALE_DATA,
                        FeatureFlags.VARIANTS,
                        FeatureFlags.INVENTORY,
                        FeatureFlags.GST
                )
        );
    }

    public static BusinessTemplate salonDefault() {
        return new BusinessTemplate(
                TEMPLATE_SALON_DEFAULT,
                BusinessTypes.SALON,
                "Salon / Beauty / Spa",
                features(
                        FeatureFlags.FAST_BILLING,
                        FeatureFlags.TOTAL_SALE_DATA,
                        FeatureFlags.TODAY_SALE_DATA,
                        FeatureFlags.APPOINTMENTS,
                        FeatureFlags.GST
                )
        );
    }

    public static BusinessTemplate bakeryDefault() {
        return new BusinessTemplate(
                TEMPLATE_BAKERY_DEFAULT,
                BusinessTypes.BAKERY,
                "Bakery / Cake Shop",
                features(
                        FeatureFlags.FAST_BILLING,
                        FeatureFlags.TAKE_AWAY,
                        FeatureFlags.TOTAL_SALE_DATA,
                        FeatureFlags.TODAY_SALE_DATA,
                        FeatureFlags.CUSTOM_ORDERS,
                        FeatureFlags.PORTIONS,
                        FeatureFlags.INVENTORY,
                        FeatureFlags.GST
                )
        );
    }

    private static Set<String> features(String... flags) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(flags)));
    }
}
