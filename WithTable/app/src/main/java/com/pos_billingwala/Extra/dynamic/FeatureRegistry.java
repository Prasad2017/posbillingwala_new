package com.pos_billingwala.Extra.dynamic;

import android.content.Context;
import android.view.View;

import com.pos_billingwala.Extra.FeatureEngine;
import com.pos_billingwala.Extra.FeatureFlags;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public feature registry over {@link FeatureFlags} + {@link FeatureEngine}.
 */
public final class FeatureRegistry {

    private static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
            FeatureFlags.FAST_BILLING,
            FeatureFlags.DINE_IN,
            FeatureFlags.TAKE_AWAY,
            FeatureFlags.MESS,
            FeatureFlags.TOTAL_SALE_DATA,
            FeatureFlags.TODAY_SALE_DATA,
            FeatureFlags.PORTIONS,
            FeatureFlags.TABLES,
            FeatureFlags.KOT,
            FeatureFlags.BOT,
            FeatureFlags.COMBOS,
            FeatureFlags.INVENTORY,
            FeatureFlags.GST,
            FeatureFlags.BARCODE,
            FeatureFlags.WEIGHT_SCALE,
            FeatureFlags.VARIANTS,
            FeatureFlags.APPOINTMENTS,
            FeatureFlags.CUSTOM_ORDERS,
            FeatureFlags.WHOLESALE_PRICING,
            FeatureFlags.OPEN_PRICE
    ));

    private FeatureRegistry() {
    }

    public static List<String> allCodes() {
        return ALL;
    }

    public static boolean isEnabled(Context context, String featureFlag) {
        return FeatureEngine.isEnabled(context, featureFlag);
    }

    public static void setVisible(Context context, View view, String featureFlag) {
        FeatureEngine.setVisible(context, view, featureFlag);
    }

    public static String disabledReason(Context context, String featureFlag) {
        return FeatureEngine.disabledReason(context, featureFlag);
    }

    public static Map<String, Boolean> snapshot(Context context) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (String code : ALL) {
            map.put(code, isEnabled(context, code));
        }
        return Collections.unmodifiableMap(map);
    }
}
