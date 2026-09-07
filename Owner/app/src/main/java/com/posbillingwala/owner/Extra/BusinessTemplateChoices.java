package com.posbillingwala.owner.Extra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Full business-type / template picker list (Priority 1–3).
 * Priority 3 types map to nearest live template engines.
 */
public final class BusinessTemplateChoices {

    public static final class Item {
        public final String businessType;
        public final String templateId;
        public final String label;

        public Item(String businessType, String templateId, String label) {
            this.businessType = businessType;
            this.templateId = templateId;
            this.label = label;
        }
    }

    private static final List<Item> ALL;

    static {
        List<Item> list = new ArrayList<>();
        list.add(new Item("restaurant", "restaurant_default", "Restaurant / Food [Live]"));
        list.add(new Item("bar_restaurant", "bar_restaurant_default", "Bar + Restaurant [Live]"));
        list.add(new Item("mess", "mess_focused", "Mess / Tiffin [Live]"));
        list.add(new Item("retail", "retail_default", "Retail / General Shop [Live]"));
        list.add(new Item("grocery", "grocery_default", "Grocery / Kirana [Live]"));
        list.add(new Item("weight_fresh", "weight_fresh_default", "Weight / Fresh [Live]"));
        list.add(new Item("wholesale", "wholesale_default", "Wholesale [Live]"));
        list.add(new Item("fashion", "fashion_default", "Clothing / Footwear [Live]"));
        list.add(new Item("jewellery", "jewellery_default", "Jewellery [Live]"));
        list.add(new Item("salon", "salon_default", "Salon / Beauty / Spa [Live]"));
        list.add(new Item("bakery", "bakery_default", "Bakery / Cake Shop [Live]"));
        list.add(new Item("electronics", "retail_default", "Electronics / Mobile [Partial → retail]"));
        list.add(new Item("hardware", "retail_default", "Hardware [Partial → retail]"));
        list.add(new Item("stationery", "retail_default", "Stationery [Partial → retail]"));
        list.add(new Item("pet_shop", "retail_default", "Pet Shop [Partial → retail]"));
        list.add(new Item("rental", "retail_default", "Rental [Partial → retail]"));
        list.add(new Item("laundry", "salon_default", "Laundry [Partial → salon]"));
        list.add(new Item("car_wash", "salon_default", "Car Wash [Partial → salon]"));
        list.add(new Item("repair", "salon_default", "Repair [Partial → salon]"));
        list.add(new Item("healthcare", "salon_default", "Healthcare-ready [Partial → salon]"));
        list.add(new Item("custom", "restaurant_default", "Custom Business [Partial → restaurant]"));
        ALL = Collections.unmodifiableList(list);
    }

    private BusinessTemplateChoices() {
    }

    public static List<Item> all() {
        return ALL;
    }
}
