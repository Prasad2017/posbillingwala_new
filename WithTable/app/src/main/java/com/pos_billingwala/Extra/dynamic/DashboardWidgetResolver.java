package com.pos_billingwala.Extra.dynamic;

import android.content.Context;

import com.pos_billingwala.Extra.DynamicUiEngine;

/**
 * Dashboard widget resolver facade (Phase 11).
 */
public final class DashboardWidgetResolver {

    private DashboardWidgetResolver() {
    }

    public static boolean isVisible(Context context, String widgetCode) {
        return DynamicUiEngine.isWidgetVisible(context, widgetCode);
    }
}
