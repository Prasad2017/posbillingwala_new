package com.pos_billingwala.Extra.dynamicui;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Reusable Dynamic UI presentation hooks (empty state, field visibility queries).
 */
public final class DynamicUiComponents {

    private DynamicUiComponents() {
    }

    public static void applyEmptyState(@Nullable View emptyRoot, boolean hasData,
                                       @Nullable CharSequence subtitle) {
        com.pos_billingwala.Extra.EmptyListUi.bind(emptyRoot, hasData, subtitle);
    }

    public static boolean shouldShowBillingField(Context context, String fieldCode) {
        return UIResolverService.resolve(context).isBillingFieldVisible(fieldCode);
    }

    public static boolean shouldShowProductField(Context context, String fieldCode) {
        return UIResolverService.resolve(context).isProductFieldVisible(fieldCode);
    }
}
