package com.pos_billingwala.Extra.dynamicui;

import android.content.Context;

import com.pos_billingwala.Extra.BusinessTypes;
import com.pos_billingwala.Extra.FeatureEngine;
import com.pos_billingwala.Extra.FeatureFlags;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves billing UI fields/sections. Does NOT replace UniversalBillingEngine —
 * only declares what CreatePos / related UI should emphasize.
 */
final class BillingUIResolver {

    private BillingUIResolver() {
    }

    static BillingUIConfig resolve(Context context, String businessType) {
        String type = BusinessTypes.normalize(businessType);
        List<BillingFieldConfig> fields = new ArrayList<>();
        List<BillingSectionConfig> sections = new ArrayList<>();

        fields.add(f(UiCodes.BF_PRODUCT, "Product", true, 10, null));
        fields.add(f(UiCodes.BF_CATEGORY, "Category", true, 20, null));
        fields.add(f(UiCodes.BF_BARCODE, "Barcode",
                FeatureEngine.isEnabled(context, FeatureFlags.BARCODE), 30, FeatureFlags.BARCODE));
        fields.add(f(UiCodes.BF_WEIGHT, "Weight",
                FeatureEngine.isEnabled(context, FeatureFlags.WEIGHT_SCALE), 40, FeatureFlags.WEIGHT_SCALE));
        fields.add(f(UiCodes.BF_UNIT, "Unit",
                FeatureEngine.isEnabled(context, FeatureFlags.WEIGHT_SCALE), 45, FeatureFlags.WEIGHT_SCALE));
        fields.add(f(UiCodes.BF_RATE, "Rate",
                FeatureEngine.isEnabled(context, FeatureFlags.WEIGHT_SCALE), 50, FeatureFlags.WEIGHT_SCALE));
        fields.add(f(UiCodes.BF_TABLE, "Table",
                FeatureEngine.isEnabled(context, FeatureFlags.TABLES)
                        || FeatureEngine.isEnabled(context, FeatureFlags.DINE_IN), 60, FeatureFlags.DINE_IN));
        fields.add(f(UiCodes.BF_KOT, "KOT",
                FeatureEngine.isEnabled(context, FeatureFlags.KOT), 65, FeatureFlags.KOT));
        fields.add(f(UiCodes.BF_PORTION, "Portion",
                FeatureEngine.isEnabled(context, FeatureFlags.PORTIONS), 70, FeatureFlags.PORTIONS));
        fields.add(f(UiCodes.BF_VARIANT, "Variant",
                FeatureEngine.isEnabled(context, FeatureFlags.VARIANTS), 80, FeatureFlags.VARIANTS));
        fields.add(f(UiCodes.BF_SIZE, "Size",
                FeatureEngine.isEnabled(context, FeatureFlags.VARIANTS), 85, FeatureFlags.VARIANTS));
        fields.add(f(UiCodes.BF_COLOR, "Color",
                FeatureEngine.isEnabled(context, FeatureFlags.VARIANTS), 90, FeatureFlags.VARIANTS));
        fields.add(f(UiCodes.BF_CUSTOMER, "Customer",
                FeatureEngine.isEnabled(context, FeatureFlags.APPOINTMENTS)
                        || FeatureEngine.isEnabled(context, FeatureFlags.MESS)
                        || FeatureEngine.isEnabled(context, FeatureFlags.CUSTOM_ORDERS)
                        || BusinessTypes.REPAIR.equals(type)
                        || BusinessTypes.RENTAL.equals(type), 100, null));
        fields.add(f(UiCodes.BF_SERVICE, "Service",
                FeatureEngine.isEnabled(context, FeatureFlags.APPOINTMENTS), 110, FeatureFlags.APPOINTMENTS));
        fields.add(f(UiCodes.BF_STAFF, "Staff",
                FeatureEngine.isEnabled(context, FeatureFlags.APPOINTMENTS), 115, FeatureFlags.APPOINTMENTS));
        fields.add(f(UiCodes.BF_APPOINTMENT, "Appointment",
                FeatureEngine.isEnabled(context, FeatureFlags.APPOINTMENTS), 120, FeatureFlags.APPOINTMENTS));
        fields.add(f(UiCodes.BF_DURATION, "Duration",
                FeatureEngine.isEnabled(context, FeatureFlags.APPOINTMENTS), 125, FeatureFlags.APPOINTMENTS));
        fields.add(f(UiCodes.BF_SERIAL, "Serial Number",
                BusinessTypes.ELECTRONICS.equals(type) || BusinessTypes.REPAIR.equals(type)
                        || BusinessTypes.HARDWARE.equals(type), 150, null));
        fields.add(f(UiCodes.BF_CUSTOM_FIELDS, "Custom Fields",
                FeatureEngine.isEnabled(context, FeatureFlags.CUSTOM_ORDERS)
                        || BusinessTypes.RENTAL.equals(type), 130, FeatureFlags.CUSTOM_ORDERS));
        fields.add(f(UiCodes.BF_DELIVERY_DATE, "Delivery Date",
                FeatureEngine.isEnabled(context, FeatureFlags.CUSTOM_ORDERS)
                        || BusinessTypes.RENTAL.equals(type), 135, FeatureFlags.CUSTOM_ORDERS));
        fields.add(f(UiCodes.BF_ADVANCE, "Advance Payment",
                FeatureEngine.isEnabled(context, FeatureFlags.CUSTOM_ORDERS)
                        || BusinessTypes.RENTAL.equals(type), 140, FeatureFlags.CUSTOM_ORDERS));

        sections.add(new BillingSectionConfig(UiCodes.BS_CATALOG, "Catalog", true, 10,
                filter(fields, UiCodes.BF_PRODUCT, UiCodes.BF_CATEGORY, UiCodes.BF_BARCODE)));
        sections.add(new BillingSectionConfig(UiCodes.BS_CART, "Cart", true, 20, filter(fields)));
        sections.add(new BillingSectionConfig(UiCodes.BS_TABLE, "Table",
                FeatureEngine.isEnabled(context, FeatureFlags.DINE_IN), 30,
                filter(fields, UiCodes.BF_TABLE, UiCodes.BF_KOT)));
        sections.add(new BillingSectionConfig(UiCodes.BS_WEIGHT, "Weight",
                FeatureEngine.isEnabled(context, FeatureFlags.WEIGHT_SCALE), 40,
                filter(fields, UiCodes.BF_WEIGHT, UiCodes.BF_UNIT, UiCodes.BF_RATE)));
        sections.add(new BillingSectionConfig(UiCodes.BS_VARIANT, "Variants",
                FeatureEngine.isEnabled(context, FeatureFlags.VARIANTS), 50,
                filter(fields, UiCodes.BF_VARIANT, UiCodes.BF_SIZE, UiCodes.BF_COLOR)));
        sections.add(new BillingSectionConfig(UiCodes.BS_SERVICE, "Service",
                FeatureEngine.isEnabled(context, FeatureFlags.APPOINTMENTS), 60,
                filter(fields, UiCodes.BF_SERVICE, UiCodes.BF_STAFF, UiCodes.BF_APPOINTMENT, UiCodes.BF_DURATION)));
        sections.add(new BillingSectionConfig(UiCodes.BS_CUSTOM_ORDER, "Custom Order",
                FeatureEngine.isEnabled(context, FeatureFlags.CUSTOM_ORDERS)
                        || BusinessTypes.RENTAL.equals(type), 70,
                filter(fields, UiCodes.BF_CUSTOM_FIELDS, UiCodes.BF_DELIVERY_DATE, UiCodes.BF_ADVANCE)));

        return new BillingUIConfig(sections, fields);
    }

    private static BillingFieldConfig f(String code, String title, boolean visible, int order, String feature) {
        return new BillingFieldConfig(code, title, visible, order, feature);
    }

    private static List<BillingFieldConfig> filter(List<BillingFieldConfig> all, String... codes) {
        if (codes == null || codes.length == 0) {
            return new ArrayList<>(all);
        }
        List<BillingFieldConfig> out = new ArrayList<>();
        for (String code : codes) {
            for (BillingFieldConfig f : all) {
                if (f.fieldCode.equals(code)) {
                    out.add(f);
                    break;
                }
            }
        }
        return out;
    }
}

/**
 * Product form field resolver — reuses Product Engine; does not duplicate AddProduct UI.
 */
final class ProductFormResolver {

    private ProductFormResolver() {
    }

    static ProductFormConfig resolve(Context context, String businessType) {
        String type = BusinessTypes.normalize(businessType);
        List<ProductFieldConfig> fields = UiFallback.commonProductFields();

        // Apply barcode visibility from FeatureEngine
        replaceVisibility(fields, UiCodes.PF_BARCODE, FeatureEngine.isEnabled(context, FeatureFlags.BARCODE));
        replaceVisibility(fields, UiCodes.PF_TAX, FeatureEngine.isEnabled(context, FeatureFlags.GST));

        if (isFood(type)) {
            fields.add(new ProductFieldConfig(UiCodes.PF_VEG, "Veg / Non Veg", true, false, 100, null));
            fields.add(new ProductFieldConfig(UiCodes.PF_KITCHEN_ROUTE, "Kitchen Route",
                    FeatureEngine.isEnabled(context, FeatureFlags.KOT), false, 110, FeatureFlags.KOT));
            fields.add(new ProductFieldConfig(UiCodes.PF_PREP_TIME, "Preparation Time", true, false, 120, null));
            fields.add(new ProductFieldConfig(UiCodes.PF_PORTION, "Portion",
                    FeatureEngine.isEnabled(context, FeatureFlags.PORTIONS), false, 130, FeatureFlags.PORTIONS));
        }
        if (BusinessTypes.BAR_RESTAURANT.equals(type)) {
            fields.add(new ProductFieldConfig(UiCodes.PF_ML, "ML", true, false, 140, FeatureFlags.BOT));
            fields.add(new ProductFieldConfig(UiCodes.PF_BOTTLE, "Bottle Conversion", true, false, 145, FeatureFlags.BOT));
            fields.add(new ProductFieldConfig(UiCodes.PF_BAR_ROUTE, "Bar Route",
                    FeatureEngine.isEnabled(context, FeatureFlags.BOT), false, 150, FeatureFlags.BOT));
        }
        if (BusinessTypes.WEIGHT_FRESH.equals(type) || FeatureEngine.isEnabled(context, FeatureFlags.WEIGHT_SCALE)) {
            fields.add(new ProductFieldConfig(UiCodes.PF_UNIT, "Unit", true, false, 160, FeatureFlags.WEIGHT_SCALE));
            fields.add(new ProductFieldConfig(UiCodes.PF_WEIGHT, "Weight", true, false, 165, FeatureFlags.WEIGHT_SCALE));
            fields.add(new ProductFieldConfig(UiCodes.PF_RATE_PER_KG, "Rate Per KG", true, false, 170, FeatureFlags.WEIGHT_SCALE));
        }
        if (BusinessTypes.FASHION.equals(type) || BusinessTypes.JEWELLERY.equals(type)
                || FeatureEngine.isEnabled(context, FeatureFlags.VARIANTS)) {
            fields.add(new ProductFieldConfig(UiCodes.PF_SIZE, "Size", true, false, 180, FeatureFlags.VARIANTS));
            fields.add(new ProductFieldConfig(UiCodes.PF_COLOR, "Color", true, false, 185, FeatureFlags.VARIANTS));
            fields.add(new ProductFieldConfig(UiCodes.PF_VARIANTS, "Variants", true, false, 190, FeatureFlags.VARIANTS));
            fields.add(new ProductFieldConfig(UiCodes.PF_BRAND, "Brand", true, false, 195, null));
        }
        if (isSalonLike(type) || FeatureEngine.isEnabled(context, FeatureFlags.APPOINTMENTS)) {
            fields.add(new ProductFieldConfig(UiCodes.PF_DURATION, "Service Duration", true, false, 200, FeatureFlags.APPOINTMENTS));
            fields.add(new ProductFieldConfig(UiCodes.PF_STAFF, "Staff Assignment", true, false, 205, FeatureFlags.APPOINTMENTS));
        }
        if (BusinessTypes.ELECTRONICS.equals(type) || BusinessTypes.REPAIR.equals(type)
                || BusinessTypes.HARDWARE.equals(type)) {
            fields.add(new ProductFieldConfig(UiCodes.PF_SERIAL, "Serial Number", true, false, 210, null));
            fields.add(new ProductFieldConfig(UiCodes.PF_WARRANTY, "Warranty", true, false, 215, null));
        }
        if (BusinessTypes.BAKERY.equals(type) || BusinessTypes.RENTAL.equals(type)
                || FeatureEngine.isEnabled(context, FeatureFlags.CUSTOM_ORDERS)) {
            fields.add(new ProductFieldConfig(UiCodes.PF_WEIGHT, "Weight", true, false, 220, FeatureFlags.CUSTOM_ORDERS));
            fields.add(new ProductFieldConfig(UiCodes.PF_FLAVOUR, "Flavour", true, false, 225, FeatureFlags.CUSTOM_ORDERS));
            fields.add(new ProductFieldConfig(UiCodes.PF_CUSTOM_ORDER, "Custom Order Support", true, false, 230, FeatureFlags.CUSTOM_ORDERS));
        }

        return new ProductFormConfig(fields);
    }

    private static boolean isFood(String type) {
        return BusinessTypes.RESTAURANT.equals(type)
                || BusinessTypes.BAR_RESTAURANT.equals(type)
                || BusinessTypes.MESS.equals(type)
                || BusinessTypes.BAKERY.equals(type);
    }

    private static boolean isSalonLike(String type) {
        return BusinessTypes.SALON.equals(type)
                || BusinessTypes.LAUNDRY.equals(type)
                || BusinessTypes.CAR_WASH.equals(type)
                || BusinessTypes.HEALTHCARE.equals(type);
    }

    private static void replaceVisibility(List<ProductFieldConfig> fields, String code, boolean visible) {
        for (int i = 0; i < fields.size(); i++) {
            ProductFieldConfig f = fields.get(i);
            if (code.equals(f.fieldCode)) {
                fields.set(i, new ProductFieldConfig(f.fieldCode, f.title, visible, f.required, f.order, f.featureRequired));
                return;
            }
        }
    }
}
