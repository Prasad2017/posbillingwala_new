package com.pos_billingwala.Extra.dynamic;

import android.content.Context;

import com.pos_billingwala.Extra.dynamicui.DynamicNavigationService;

import java.util.List;

/**
 * Public navigation registry (Phase 03) over DynamicNavigationService.
 */
public final class NavigationRegistry {

    private NavigationRegistry() {
    }

    public static List<String> visibleCodes(Context context) {
        return DynamicNavigationService.visibleCodes(context);
    }

    public static boolean isVisible(Context context, String navCode) {
        return DynamicNavigationService.isVisible(context, navCode);
    }
}
