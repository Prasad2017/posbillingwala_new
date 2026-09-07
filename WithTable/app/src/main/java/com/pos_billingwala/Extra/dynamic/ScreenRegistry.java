package com.pos_billingwala.Extra.dynamic;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.pos_billingwala.Extra.DynamicUiEngine;
import com.pos_billingwala.Extra.dynamicui.UiCodes;
import com.pos_billingwala.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Screen visibility registry wrapping Dynamic UI screen codes.
 */
public final class ScreenRegistry {

    private static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
            UiCodes.HOME,
            UiCodes.BILLING,
            UiCodes.TABLE_MANAGEMENT,
            UiCodes.ORDERS,
            UiCodes.TAKE_AWAY,
            UiCodes.MESS,
            UiCodes.PRODUCTS,
            UiCodes.CATEGORIES,
            UiCodes.STOCK,
            UiCodes.CUSTOMERS,
            UiCodes.REPORTS,
            UiCodes.SETTINGS,
            UiCodes.APPOINTMENTS,
            UiCodes.SERVICES,
            UiCodes.STAFF,
            UiCodes.CUSTOM_ORDERS,
            UiCodes.WEIGHT_BILLING,
            UiCodes.BARCODE,
            UiCodes.MASTER_DATA,
            UiCodes.INVENTORY,
            UiCodes.EXPENSES,
            UiCodes.PRINTER,
            UiCodes.PORTIONS,
            UiCodes.COMBOS
    ));

    private ScreenRegistry() {
    }

    public static List<String> allCodes() {
        return ALL;
    }

    public static boolean isVisible(Context context, String screenCode) {
        return DynamicUiEngine.isScreenVisible(context, screenCode);
    }

    public static void apply(Context context, @Nullable View view, String screenCode) {
        DynamicUiEngine.applyScreen(context, view, screenCode);
    }

    /**
     * Soft open-gate: returns false (and toasts) when the screen is not visible for this shop/role.
     */
    public static boolean guardOpen(Context context, String screenCode) {
        if (isVisible(context, screenCode)) {
            return true;
        }
        if (context != null) {
            Toast.makeText(context, R.string.toast_screen_not_available, Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
