package com.pos_billingwala.Extra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Full business-type catalog for Universal POS (Phase 02).
 * Priority 1–3 types are selectable; Priority 3 maps to nearest live template.
 */
public final class BusinessTypeCatalog {

    private static final List<BusinessTypeInfo> ALL;
    private static final Map<String, BusinessTypeInfo> BY_ID;

    static {
        List<BusinessTypeInfo> list = new ArrayList<>();
        // Priority 1 — ship / reuse now
        list.add(info(BusinessTypes.RESTAURANT, "Restaurant / Food", "Food & Hospitality",
                BusinessTypeReadiness.LIVE, 1,
                BusinessTemplateRegistry.TEMPLATE_RESTAURANT_DEFAULT,
                "Tables, KOT, portions, combos, takeaway, mess"));
        list.add(info(BusinessTypes.BAR_RESTAURANT, "Bar + Restaurant", "Bar & Beverage",
                BusinessTypeReadiness.LIVE, 1,
                BusinessTemplateRegistry.TEMPLATE_BAR_RESTAURANT,
                "Restaurant engines + BOT split print (optional dedicated BOT Bluetooth)"));
        list.add(info(BusinessTypes.MESS, "Mess / Tiffin", "Food & Hospitality",
                BusinessTypeReadiness.LIVE, 1,
                BusinessTemplateRegistry.TEMPLATE_MESS_FOCUSED,
                "Membership + QR tokens (existing mess domain)"));
        list.add(info(BusinessTypes.RETAIL, "Retail / General Shop", "Retail",
                BusinessTypeReadiness.LIVE, 1,
                BusinessTemplateRegistry.TEMPLATE_RETAIL_DEFAULT,
                "Fast billing + inventory + barcode (Enter/camera)"));
        list.add(info(BusinessTypes.GROCERY, "Grocery / Kirana", "Retail",
                BusinessTypeReadiness.LIVE, 1,
                BusinessTemplateRegistry.TEMPLATE_GROCERY_DEFAULT,
                "Retail template + barcode/inventory focus"));
        list.add(info(BusinessTypes.WEIGHT_FRESH, "Weight / Fresh (Fish, Meat, Veg, Fruit)", "Fresh & Weight",
                BusinessTypeReadiness.LIVE, 1,
                BusinessTemplateRegistry.TEMPLATE_WEIGHT_FRESH,
                "Fast billing + weight keypad + optional BT scale"));
        list.add(info(BusinessTypes.WHOLESALE, "Wholesale", "Retail",
                BusinessTypeReadiness.LIVE, 1,
                BusinessTemplateRegistry.TEMPLATE_WHOLESALE_DEFAULT,
                "Fast billing + qty price tiers (cloud sync)"));

        // Priority 2 — verticals shipped
        list.add(info(BusinessTypes.FASHION, "Clothing / Footwear", "Fashion",
                BusinessTypeReadiness.LIVE, 2,
                BusinessTemplateRegistry.TEMPLATE_FASHION_DEFAULT,
                "Size/color variants + cloud sync (doc 10)"));
        list.add(info(BusinessTypes.JEWELLERY, "Jewellery", "Fashion",
                BusinessTypeReadiness.LIVE, 2,
                BusinessTemplateRegistry.TEMPLATE_JEWELLERY_DEFAULT,
                "Variants + cloud sync (doc 10)"));
        list.add(info(BusinessTypes.SALON, "Salon / Beauty / Spa", "Services",
                BusinessTypeReadiness.LIVE, 2,
                BusinessTemplateRegistry.TEMPLATE_SALON_DEFAULT,
                "Appointments calendar + cloud sync (doc 11)"));
        list.add(info(BusinessTypes.BAKERY, "Bakery / Cake Shop", "Food & Hospitality",
                BusinessTypeReadiness.LIVE, 2,
                BusinessTemplateRegistry.TEMPLATE_BAKERY_DEFAULT,
                "Custom orders + deposit + bill photo (doc 12)"));

        // Priority 3 — selectable; map to nearest live template
        list.add(info(BusinessTypes.ELECTRONICS, "Electronics / Mobile", "Retail",
                BusinessTypeReadiness.PARTIAL, 3,
                BusinessTemplateRegistry.TEMPLATE_RETAIL_DEFAULT,
                "Retail + serial/IMEI prompt on add-to-cart"));
        list.add(info(BusinessTypes.HARDWARE, "Hardware", "Retail",
                BusinessTypeReadiness.PARTIAL, 3,
                BusinessTemplateRegistry.TEMPLATE_RETAIL_DEFAULT,
                "Maps to retail; serial prompt when electronics module maps hardware"));
        list.add(info(BusinessTypes.STATIONERY, "Stationery", "Retail",
                BusinessTypeReadiness.PARTIAL, 3,
                BusinessTemplateRegistry.TEMPLATE_RETAIL_DEFAULT,
                "Maps to retail template"));
        list.add(info(BusinessTypes.PET_SHOP, "Pet Shop", "Retail",
                BusinessTypeReadiness.PARTIAL, 3,
                BusinessTemplateRegistry.TEMPLATE_RETAIL_DEFAULT,
                "Maps to retail template"));
        list.add(info(BusinessTypes.LAUNDRY, "Laundry", "Services",
                BusinessTypeReadiness.PARTIAL, 3,
                BusinessTemplateRegistry.TEMPLATE_SALON_DEFAULT,
                "Maps to salon appointment engines"));
        list.add(info(BusinessTypes.CAR_WASH, "Car Wash", "Services",
                BusinessTypeReadiness.PARTIAL, 3,
                BusinessTemplateRegistry.TEMPLATE_SALON_DEFAULT,
                "Maps to salon appointment engines"));
        list.add(info(BusinessTypes.REPAIR, "Repair", "Services",
                BusinessTypeReadiness.PARTIAL, 3,
                BusinessTemplateRegistry.TEMPLATE_SALON_DEFAULT,
                "Salon template + repair job note/serial on billing"));
        list.add(info(BusinessTypes.RENTAL, "Rental", "Services",
                BusinessTypeReadiness.PARTIAL, 3,
                BusinessTemplateRegistry.TEMPLATE_RETAIL_DEFAULT,
                "Retail + custom-order deposits; return date prompt on billing"));
        list.add(info(BusinessTypes.HEALTHCARE, "Healthcare-ready", "Services",
                BusinessTypeReadiness.PARTIAL, 3,
                BusinessTemplateRegistry.TEMPLATE_SALON_DEFAULT,
                "Not clinical software — service appointments via salon template"));
        list.add(info(BusinessTypes.CUSTOM, "Custom Business", "Custom",
                BusinessTypeReadiness.PARTIAL, 3,
                BusinessTemplateRegistry.TEMPLATE_RESTAURANT_DEFAULT,
                "Starts from restaurant template; optional custom JSON (cloud sync)"));

        ALL = Collections.unmodifiableList(list);
        Map<String, BusinessTypeInfo> map = new LinkedHashMap<>();
        for (BusinessTypeInfo info : ALL) {
            map.put(info.getTypeId(), info);
        }
        BY_ID = Collections.unmodifiableMap(map);
    }

    private BusinessTypeCatalog() {
    }

    public static List<BusinessTypeInfo> all() {
        return ALL;
    }

    /** Types shown in Settings / platform pickers (Priority 1–3). */
    public static List<BusinessTypeInfo> selectable() {
        return ALL;
    }

    public static BusinessTypeInfo get(String typeId) {
        String normalized = BusinessTypes.normalize(typeId);
        BusinessTypeInfo info = BY_ID.get(normalized);
        if (info != null) {
            return info;
        }
        return new BusinessTypeInfo(normalized, normalized, "Custom",
                BusinessTypeReadiness.PLANNED, 99,
                BusinessTemplateRegistry.TEMPLATE_RESTAURANT_DEFAULT,
                "Unlisted type — restaurant template fallback");
    }

    public static BusinessTypeInfo forCurrent(android.content.Context context) {
        return get(BusinessSession.getBusinessType(context));
    }

    private static BusinessTypeInfo info(String id, String name, String category,
                                         BusinessTypeReadiness readiness, int priority,
                                         String templateId, String notes) {
        return new BusinessTypeInfo(id, name, category, readiness, priority, templateId, notes);
    }
}
