package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.FoodTypeResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Model.ProductCartResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Bar + Restaurant module on top of {@link RestaurantFoodModule}.
 * Reuses food catalog, tables, KOT, portions. Adds BOT capability and beverage→bar ticket routing
 * (see {@link TicketRoute} / {@link UniversalPrinterEngine} for live BOT print split).
 */
public final class BarRestaurantModule {

    public static final String DEFAULT_BOT_PREFIX = "BOT-";

    private BarRestaurantModule() {
    }

    public static boolean isBarTemplate(Context context) {
        return BusinessTypes.BAR_RESTAURANT.equals(
                BusinessTypes.normalize(BusinessSession.getBusinessType(context)));
    }

    public static boolean canBot(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.BOT);
    }

    public static boolean canBot(Context context, POSBillingWalaDatabase db) {
        return FeatureEngine.isEnabled(context, FeatureFlags.BOT, db);
    }

    public static boolean canBot(POSBillingWalaDatabase db) {
        return FeatureEngine.isEnabled(null, FeatureFlags.BOT, db);
    }

    /**
     * Split kitchen/bar tickets when BOT is enabled for this shop.
     * When false, all tickets stay on the existing KOT path (current production behaviour).
     */
    public static boolean shouldSplitKitchenAndBar(Context context, POSBillingWalaDatabase db) {
        return canBot(context, db);
    }

    public static TicketRoute routeForFoodTypeCode(String foodTypeCode) {
        if (foodTypeCode != null
                && FoodTypeResponse.CODE_BEVERAGE.equalsIgnoreCase(foodTypeCode.trim())) {
            return TicketRoute.BAR_BOT;
        }
        return TicketRoute.KITCHEN_KOT;
    }

    /**
     * Effective route for printing: if BOT split is off, always kitchen KOT.
     */
    public static TicketRoute effectiveRoute(Context context, POSBillingWalaDatabase db, String foodTypeCode) {
        if (!shouldSplitKitchenAndBar(context, db)) {
            return TicketRoute.KITCHEN_KOT;
        }
        return routeForFoodTypeCode(foodTypeCode);
    }

    public static String botPrefix(POSBillingWalaDatabase db) {
        if (db == null) {
            return DEFAULT_BOT_PREFIX;
        }
        List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
        if (list == null || list.isEmpty()) {
            return DEFAULT_BOT_PREFIX;
        }
        String prefix = list.get(0).getBotPrefix();
        if (prefix == null || prefix.trim().isEmpty()) {
            return DEFAULT_BOT_PREFIX;
        }
        return prefix.trim();
    }

    /**
     * Partition cart/KOT lines into kitchen vs bar using product category food type.
     * Combos / unknown types stay on kitchen (safe default).
     */
    public static void partitionCartByRoute(POSBillingWalaDatabase db,
                                            List<ProductCartResponse> all,
                                            List<ProductCartResponse> kitchenOut,
                                            List<ProductCartResponse> barOut) {
        if (kitchenOut == null || barOut == null) {
            return;
        }
        kitchenOut.clear();
        barOut.clear();
        if (all == null || all.isEmpty()) {
            return;
        }
        for (ProductCartResponse line : all) {
            if (line == null) {
                continue;
            }
            if (line.getCartItemType() != null
                    && "COMBO".equalsIgnoreCase(line.getCartItemType().trim())) {
                kitchenOut.add(line);
                continue;
            }
            String foodTypeCode = db != null ? db.getFoodTypeCodeForProductId(line.getProductId()) : null;
            if (routeForFoodTypeCode(foodTypeCode) == TicketRoute.BAR_BOT) {
                barOut.add(line);
            } else {
                kitchenOut.add(line);
            }
        }
    }

    public static List<ProductCartResponse> copyLines(List<ProductCartResponse> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    /**
     * Dedicated bar printer MAC when set; otherwise falls back via UniversalPrinterEngine (BOT→KOT→bill).
     */
    public static String resolveBotPrinterAddress(POSBillingWalaDatabase db) {
        return UniversalPrinterEngine.addressForRole(db, com.pos_billingwala.Extra.dynamic.PrinterRole.BOT);
    }

    public static boolean hasDedicatedBotPrinter(POSBillingWalaDatabase db) {
        if (db == null) {
            return false;
        }
        List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
        if (list == null || list.isEmpty()) {
            return false;
        }
        String bot = list.get(0).getBluetoothBotAddress();
        return bot != null && !bot.trim().isEmpty();
    }

    public static void setBotEnabled(POSBillingWalaDatabase db, boolean enabled) {
        if (db == null) {
            return;
        }
        List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
        if (list == null || list.isEmpty()) {
            return;
        }
        String settingId = list.get(0).getSettingId();
        String prefix = botPrefix(db);
        String botAddr = list.get(0).getBluetoothBotAddress();
        db.updateBotSettings(settingId, enabled ? "on" : "off", prefix,
                botAddr != null ? botAddr : "");
    }

    public static String moduleSummary(Context context) {
        boolean bar = isBarTemplate(context);
        boolean bot = canBot(context);
        POSBillingWalaDatabase db = context != null ? new POSBillingWalaDatabase(context) : null;
        return "Bar+Restaurant template: " + (bar ? "yes" : "no")
                + "\nBOT split: " + (bot ? "on (beverage → bar)" : "off (all → kitchen KOT)")
                + "\nBOT printer: " + (hasDedicatedBotPrinter(db) ? "dedicated BT" : "same as KOT")
                + "\nFood type beverage code: " + FoodTypeResponse.CODE_BEVERAGE
                + "\nReuses: tables, KOT, portions, combos, Fast/Dine/Takeaway";
    }
}
