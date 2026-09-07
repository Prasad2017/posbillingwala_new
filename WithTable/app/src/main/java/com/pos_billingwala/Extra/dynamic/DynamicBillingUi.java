package com.pos_billingwala.Extra.dynamic;

import android.content.Context;

import androidx.annotation.Nullable;

import com.pos_billingwala.Extra.DynamicUiEngine;
import com.pos_billingwala.Extra.LicenseModules;
import com.pos_billingwala.Extra.RestaurantFoodModule;
import com.pos_billingwala.Extra.dynamicui.UiCodes;
import com.pos_billingwala.databinding.FragmentCreatePosBinding;

/**
 * Applies BillingUIConfiguration sections/fields to CreatePos chrome (Phase 06 deepen).
 * Does not replace UniversalBillingEngine or vertical module behavior — only gates affordances.
 */
public final class DynamicBillingUi {

    private DynamicBillingUi() {
    }

    public static boolean fieldVisible(@Nullable BillingUIConfiguration snapshot,
                                       Context context, String fieldCode) {
        if (snapshot != null) {
            return snapshot.isFieldVisible(fieldCode);
        }
        return context != null && DynamicUiEngine.isBillingFieldVisible(context, fieldCode);
    }

    public static boolean sectionVisible(@Nullable BillingUIConfiguration snapshot,
                                         Context context, String sectionCode) {
        if (snapshot != null) {
            return snapshot.isSectionVisible(sectionCode);
        }
        return context != null
                && DynamicUiEngine.resolve(context).isBillingSectionVisible(sectionCode);
    }

    /** KOT / hold chrome for table carts — module KOT ∩ BF_KOT from cart snapshot. */
    public static void applyTableActions(Context context,
                                         @Nullable FragmentCreatePosBinding binding,
                                         @Nullable BillingUIConfiguration snapshot,
                                         @Nullable String cartOrderStatus,
                                         boolean kotEnabledInSettings) {
        if (binding == null) {
            return;
        }
        boolean tableCart = RestaurantFoodModule.isTableWiseCart(cartOrderStatus);
        if (!tableCart) {
            if (binding.dineInActionBar != null) {
                LicenseModules.setVisible(binding.dineInActionBar, false);
            }
            return;
        }
        boolean showKot = kotEnabledInSettings
                && fieldVisible(snapshot, context, UiCodes.BF_KOT);
        if (binding.dineInActionBar != null) {
            LicenseModules.setVisible(binding.dineInActionBar, true);
            if (binding.viewCartButton != null) {
                LicenseModules.setVisible(binding.viewCartButton, false);
            }
            if (binding.kotButton != null) {
                LicenseModules.setVisible(binding.kotButton, showKot);
            }
        }
        if (binding.kotButton != null && binding.dineInActionBar == null) {
            LicenseModules.setVisible(binding.kotButton, showKot);
        }
        if (binding.holdButton != null) {
            LicenseModules.setVisible(binding.holdButton, true);
        }
    }

    public static boolean allowVariantPicker(@Nullable BillingUIConfiguration snapshot,
                                             Context context, boolean moduleOffers) {
        return moduleOffers && fieldVisible(snapshot, context, UiCodes.BF_VARIANT);
    }

    public static boolean allowPortionPicker(@Nullable BillingUIConfiguration snapshot,
                                             Context context, boolean moduleOffers) {
        return moduleOffers && fieldVisible(snapshot, context, UiCodes.BF_PORTION);
    }

    public static boolean allowWeightPrompt(@Nullable BillingUIConfiguration snapshot,
                                            Context context, boolean moduleOffers) {
        return moduleOffers && fieldVisible(snapshot, context, UiCodes.BF_WEIGHT);
    }

    public static boolean allowCustomOrderPrompt(@Nullable BillingUIConfiguration snapshot,
                                                 Context context, boolean moduleOffers) {
        return moduleOffers && (fieldVisible(snapshot, context, UiCodes.BF_CUSTOM_FIELDS)
                || sectionVisible(snapshot, context, UiCodes.BS_CUSTOM_ORDER));
    }

    public static boolean allowAppointmentPrompt(@Nullable BillingUIConfiguration snapshot,
                                                 Context context, boolean moduleOffers) {
        return moduleOffers && (fieldVisible(snapshot, context, UiCodes.BF_APPOINTMENT)
                || fieldVisible(snapshot, context, UiCodes.BF_SERVICE)
                || sectionVisible(snapshot, context, UiCodes.BS_SERVICE));
    }

    public static boolean allowSerialPrompt(@Nullable BillingUIConfiguration snapshot,
                                            Context context, boolean moduleOffers) {
        return moduleOffers && fieldVisible(snapshot, context, UiCodes.BF_SERIAL);
    }

    public static boolean allowRepairPrompt(@Nullable BillingUIConfiguration snapshot,
                                            Context context, boolean moduleOffers) {
        // Repair jobs reuse serial/custom field visibility when resolver marks them
        return moduleOffers && (fieldVisible(snapshot, context, UiCodes.BF_SERIAL)
                || fieldVisible(snapshot, context, UiCodes.BF_CUSTOM_FIELDS)
                || fieldVisible(snapshot, context, UiCodes.BF_CUSTOMER));
    }

    public static boolean allowRentalPrompt(@Nullable BillingUIConfiguration snapshot,
                                            Context context, boolean moduleOffers) {
        return moduleOffers && (fieldVisible(snapshot, context, UiCodes.BF_CUSTOM_FIELDS)
                || fieldVisible(snapshot, context, UiCodes.BF_ADVANCE)
                || fieldVisible(snapshot, context, UiCodes.BF_DELIVERY_DATE)
                || sectionVisible(snapshot, context, UiCodes.BS_CUSTOM_ORDER));
    }
}
