package com.pos_billingwala.Extra.dynamicui;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic navigation service — one implementation for all business types.
 */
public final class DynamicNavigationService {

    private DynamicNavigationService() {
    }

    public static List<String> visibleCodes(Context context) {
        UIConfiguration cfg = UIResolverService.resolve(context);
        List<String> codes = new ArrayList<>();
        for (NavigationItemConfig item : cfg.navigationConfig.visibleItems()) {
            codes.add(item.code);
        }
        return codes;
    }

    public static boolean isVisible(Context context, String navCode) {
        UIConfiguration cfg = UIResolverService.resolve(context);
        for (NavigationItemConfig item : cfg.navigationConfig.items) {
            if (item.code.equals(navCode)) {
                return item.visible;
            }
        }
        return false;
    }
}
