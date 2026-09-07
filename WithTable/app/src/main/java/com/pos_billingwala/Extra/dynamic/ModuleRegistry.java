package com.pos_billingwala.Extra.dynamic;

import android.content.Context;

import com.pos_billingwala.Extra.AppContexts;
import com.pos_billingwala.Extra.BarRestaurantModule;
import com.pos_billingwala.Extra.BusinessSession;
import com.pos_billingwala.Extra.BusinessTypes;
import com.pos_billingwala.Extra.CakeBakeryModule;
import com.pos_billingwala.Extra.ElectronicsModule;
import com.pos_billingwala.Extra.FashionJewelleryModule;
import com.pos_billingwala.Extra.FeatureEngine;
import com.pos_billingwala.Extra.FeatureFlags;
import com.pos_billingwala.Extra.MessModule;
import com.pos_billingwala.Extra.RentalModule;
import com.pos_billingwala.Extra.RepairModule;
import com.pos_billingwala.Extra.RestaurantFoodModule;
import com.pos_billingwala.Extra.RetailGroceryModule;
import com.pos_billingwala.Extra.SalonAppointmentModule;
import com.pos_billingwala.Extra.WeightFreshModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of Universal POS business modules (Dynamic Phase 02 / 08).
 */
public final class ModuleRegistry {

    public static final String RESTAURANT = "restaurant_food";
    public static final String BAR = "bar_restaurant";
    public static final String MESS = "mess";
    public static final String WEIGHT = "weight_fresh";
    public static final String RETAIL = "retail_grocery";
    public static final String FASHION = "fashion_jewellery";
    public static final String SALON = "salon_appointment";
    public static final String BAKERY = "cake_bakery";
    public static final String ELECTRONICS = "electronics";
    public static final String REPAIR = "repair";
    public static final String RENTAL = "rental";

    private ModuleRegistry() {
    }

    public static List<String> allModuleCodes() {
        List<String> list = new ArrayList<>();
        list.add(RESTAURANT);
        list.add(BAR);
        list.add(MESS);
        list.add(WEIGHT);
        list.add(RETAIL);
        list.add(FASHION);
        list.add(SALON);
        list.add(BAKERY);
        list.add(ELECTRONICS);
        list.add(REPAIR);
        list.add(RENTAL);
        return Collections.unmodifiableList(list);
    }

    public static boolean isEnabled(Context context, String moduleCode) {
        Context ctx = AppContexts.or(context);
        if (moduleCode == null) {
            return false;
        }
        switch (moduleCode) {
            case RESTAURANT:
                return FeatureEngine.isEnabled(ctx, FeatureFlags.DINE_IN)
                        || FeatureEngine.isEnabled(ctx, FeatureFlags.KOT)
                        || RestaurantFoodModule.isFoodHospitalityTemplate(ctx);
            case BAR:
                return BarRestaurantModule.isBarTemplate(ctx)
                        || FeatureEngine.isEnabled(ctx, FeatureFlags.BOT);
            case MESS:
                return FeatureEngine.isEnabled(ctx, FeatureFlags.MESS)
                        || MessModule.isEnabled(ctx);
            case WEIGHT:
                return FeatureEngine.isEnabled(ctx, FeatureFlags.WEIGHT_SCALE)
                        || WeightFreshModule.isEnabled(ctx);
            case RETAIL:
                return RetailGroceryModule.isRetailFamily(ctx);
            case FASHION:
                return FeatureEngine.isEnabled(ctx, FeatureFlags.VARIANTS)
                        || FashionJewelleryModule.isFashionFamily(ctx);
            case SALON:
                return FeatureEngine.isEnabled(ctx, FeatureFlags.APPOINTMENTS)
                        || SalonAppointmentModule.isEnabled(ctx);
            case BAKERY:
                return FeatureEngine.isEnabled(ctx, FeatureFlags.CUSTOM_ORDERS)
                        || CakeBakeryModule.isEnabled(ctx);
            case ELECTRONICS:
                return ElectronicsModule.isEnabled(ctx);
            case REPAIR:
                return RepairModule.isEnabled(ctx);
            case RENTAL:
                return RentalModule.isEnabled(ctx);
            default:
                return false;
        }
    }

    public static List<String> enabledModuleCodes(Context context) {
        List<String> out = new ArrayList<>();
        for (String code : allModuleCodes()) {
            if (isEnabled(context, code)) {
                out.add(code);
            }
        }
        return Collections.unmodifiableList(out);
    }

    public static Map<String, Boolean> snapshot(Context context) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (String code : allModuleCodes()) {
            map.put(code, isEnabled(context, code));
        }
        return Collections.unmodifiableMap(map);
    }

    public static String summaryLine(Context context) {
        String type = BusinessTypes.normalize(BusinessSession.getBusinessType(context));
        return "Modules (" + type + "): " + enabledModuleCodes(context);
    }
}
