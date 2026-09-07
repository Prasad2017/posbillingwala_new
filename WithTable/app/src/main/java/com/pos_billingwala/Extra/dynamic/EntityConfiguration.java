package com.pos_billingwala.Extra.dynamic;

import com.pos_billingwala.Extra.dynamicui.UIConfiguration;
import com.pos_billingwala.Extra.dynamicui.UiCodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity field set resolved from Dynamic UI (restaurant veg/kitchen, bar ml, fish weight, etc.).
 */
public final class EntityConfiguration {

    public final String entityCode;
    public final List<String> visibleFieldCodes;

    private EntityConfiguration(String entityCode, List<String> visibleFieldCodes) {
        this.entityCode = entityCode;
        this.visibleFieldCodes = visibleFieldCodes != null
                ? Collections.unmodifiableList(new ArrayList<>(visibleFieldCodes))
                : Collections.emptyList();
    }

    static EntityConfiguration forProduct(UIConfiguration ui) {
        List<String> visible = new ArrayList<>();
        if (ui == null) {
            return new EntityConfiguration(FormRegistry.FORM_PRODUCT, visible);
        }
        for (String code : FormRegistry.productFieldCodes()) {
            if (ui.isProductFieldVisible(code)) {
                visible.add(code);
            }
        }
        // Core fields always present for data integrity
        ensure(visible, UiCodes.PF_NAME);
        ensure(visible, UiCodes.PF_CATEGORY);
        ensure(visible, UiCodes.PF_PRICE);
        return new EntityConfiguration(FormRegistry.FORM_PRODUCT, visible);
    }

    static EntityConfiguration forBilling(UIConfiguration ui) {
        List<String> visible = new ArrayList<>();
        if (ui == null) {
            return new EntityConfiguration(FormRegistry.FORM_BILLING, visible);
        }
        for (String code : FormRegistry.billingFieldCodes()) {
            if (ui.isBillingFieldVisible(code)) {
                visible.add(code);
            }
        }
        ensure(visible, UiCodes.BF_PRODUCT);
        return new EntityConfiguration(FormRegistry.FORM_BILLING, visible);
    }

    public boolean includes(String fieldCode) {
        return visibleFieldCodes.contains(fieldCode);
    }

    private static void ensure(List<String> list, String code) {
        if (!list.contains(code)) {
            list.add(0, code);
        }
    }
}
