package com.pos_billingwala.Extra.dynamic;

import android.content.Context;
import android.view.View;

import com.pos_billingwala.Extra.DynamicUiEngine;
import com.pos_billingwala.Extra.dynamicui.UiCodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Settings section registry (Dynamic Phase 02 / 09).
 */
public final class SettingsRegistry {

    private static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
            UiCodes.ST_SHOP,
            UiCodes.ST_TEMPLATE,
            UiCodes.ST_PRINTER,
            UiCodes.ST_KOT,
            UiCodes.ST_TABLE,
            UiCodes.ST_BOT,
            UiCodes.ST_PORTION,
            UiCodes.ST_SCALE,
            UiCodes.ST_WEIGHT_UNIT,
            UiCodes.ST_SERVICE,
            UiCodes.ST_STAFF,
            UiCodes.ST_APPOINTMENT,
            UiCodes.ST_VARIANTS,
            UiCodes.ST_INVENTORY,
            UiCodes.ST_MASTER,
            UiCodes.ST_REPORTS
    ));

    private SettingsRegistry() {
    }

    public static List<String> allSectionCodes() {
        return ALL;
    }

    public static boolean isSectionVisible(Context context, String sectionCode) {
        return DynamicUiEngine.isSettingsSectionVisible(context, sectionCode);
    }

    public static void applySection(Context context, View view, String sectionCode) {
        DynamicUiEngine.applySettingsSection(context, view, sectionCode);
    }
}
