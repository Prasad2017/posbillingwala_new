package com.pos_billingwala.Extra.dynamic;

import android.content.Context;

import com.pos_billingwala.Extra.DynamicUiEngine;
import com.pos_billingwala.Extra.dynamicui.UIConfiguration;
import com.pos_billingwala.Extra.dynamicui.UiCodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Form / field registry for product + billing (Dynamic Phase 04).
 */
public final class FormRegistry {

    public static final String FORM_PRODUCT = "product";
    public static final String FORM_BILLING = "billing";

    private static final List<String> PRODUCT_FIELDS = Collections.unmodifiableList(Arrays.asList(
            UiCodes.PF_NAME, UiCodes.PF_CATEGORY, UiCodes.PF_SUBCATEGORY, UiCodes.PF_SKU,
            UiCodes.PF_BARCODE, UiCodes.PF_PRICE, UiCodes.PF_TAX, UiCodes.PF_DESCRIPTION,
            UiCodes.PF_IMAGE, UiCodes.PF_VEG, UiCodes.PF_KITCHEN_ROUTE, UiCodes.PF_PREP_TIME,
            UiCodes.PF_PORTION, UiCodes.PF_ML, UiCodes.PF_BOTTLE, UiCodes.PF_BAR_ROUTE,
            UiCodes.PF_UNIT, UiCodes.PF_WEIGHT, UiCodes.PF_RATE_PER_KG, UiCodes.PF_SIZE,
            UiCodes.PF_COLOR, UiCodes.PF_VARIANTS, UiCodes.PF_BRAND, UiCodes.PF_DURATION,
            UiCodes.PF_STAFF, UiCodes.PF_SERIAL, UiCodes.PF_WARRANTY, UiCodes.PF_FLAVOUR,
            UiCodes.PF_CUSTOM_ORDER
    ));

    private static final List<String> BILLING_FIELDS = Collections.unmodifiableList(Arrays.asList(
            UiCodes.BF_PRODUCT, UiCodes.BF_CATEGORY, UiCodes.BF_BARCODE, UiCodes.BF_WEIGHT,
            UiCodes.BF_UNIT, UiCodes.BF_RATE, UiCodes.BF_TABLE, UiCodes.BF_CUSTOMER,
            UiCodes.BF_SERVICE, UiCodes.BF_STAFF, UiCodes.BF_APPOINTMENT, UiCodes.BF_VARIANT,
            UiCodes.BF_SIZE, UiCodes.BF_COLOR, UiCodes.BF_PORTION, UiCodes.BF_SERIAL,
            UiCodes.BF_CUSTOM_FIELDS, UiCodes.BF_DELIVERY_DATE, UiCodes.BF_ADVANCE,
            UiCodes.BF_KOT, UiCodes.BF_DURATION
    ));

    private FormRegistry() {
    }

    public static List<String> productFieldCodes() {
        return PRODUCT_FIELDS;
    }

    public static List<String> billingFieldCodes() {
        return BILLING_FIELDS;
    }

    public static boolean isProductFieldVisible(Context context, String fieldCode) {
        return DynamicUiEngine.isProductFieldVisible(context, fieldCode);
    }

    public static boolean isBillingFieldVisible(Context context, String fieldCode) {
        return DynamicUiEngine.isBillingFieldVisible(context, fieldCode);
    }

    public static EntityConfiguration productEntity(Context context) {
        UIConfiguration ui = DynamicUiEngine.resolve(context);
        return EntityConfiguration.forProduct(ui);
    }

    public static EntityConfiguration billingEntity(Context context) {
        UIConfiguration ui = DynamicUiEngine.resolve(context);
        return EntityConfiguration.forBilling(ui);
    }
}
