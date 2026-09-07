package com.posbillingwala.dealer.Extra;

/**
 * Suggested licence module flags for a business template (matches POS registry).
 */
public final class LicenceModuleDefaults {

    public static final class Modules {
        public final boolean fastBilling;
        public final boolean takeAway;
        public final boolean dineIn;
        public final boolean mess;

        public Modules(boolean fastBilling, boolean takeAway, boolean dineIn, boolean mess) {
            this.fastBilling = fastBilling;
            this.takeAway = takeAway;
            this.dineIn = dineIn;
            this.mess = mess;
        }
    }

    private LicenceModuleDefaults() {
    }

    public static Modules forTemplate(String businessType, String templateId) {
        String type = businessType != null ? businessType.trim().toLowerCase() : "";
        String tid = templateId != null ? templateId.trim().toLowerCase() : "";

        if ("mess".equals(type) || "mess_focused".equals(tid)) {
            return new Modules(true, false, false, true);
        }
        if ("restaurant".equals(type) || "bar_restaurant".equals(type) || "custom".equals(type)
                || "restaurant_default".equals(tid) || "bar_restaurant_default".equals(tid)) {
            return new Modules(true, true, true, true);
        }
        if ("salon".equals(type) || "laundry".equals(type) || "car_wash".equals(type)
                || "repair".equals(type) || "healthcare".equals(type)
                || "salon_default".equals(tid)) {
            return new Modules(true, false, false, false);
        }
        return new Modules(true, false, false, false);
    }
}
