package com.pos_billingwala.Extra.dynamicui;

import android.content.Context;

import com.pos_billingwala.Extra.FeatureFlags;
import com.pos_billingwala.Extra.SecurityPermissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Builds navigation from system defaults + template features (single implementation for all businesses).
 */
final class NavigationResolver {

    private NavigationResolver() {
    }

    static NavigationConfiguration resolve(Context context, List<String> templateFeatures) {
        List<NavigationItemConfig> base = new ArrayList<>();
        base.add(item(UiCodes.HOME, "Home", null, null, "Home", 10));
        base.add(item(UiCodes.BILLING, "Billing", FeatureFlags.FAST_BILLING, SecurityPermissions.BILL, "CreatePos", 20));
        base.add(item(UiCodes.TABLE_MANAGEMENT, "Tables", FeatureFlags.DINE_IN, SecurityPermissions.BILL, "InvoiceCompanyTable", 30));
        base.add(item(UiCodes.ORDERS, "Orders", FeatureFlags.DINE_IN, SecurityPermissions.BILL, "InvoiceCompanyTable", 35));
        base.add(item(UiCodes.TAKE_AWAY, "Take Away", FeatureFlags.TAKE_AWAY, SecurityPermissions.BILL, "InvoiceTakeAway", 40));
        base.add(item(UiCodes.MESS, "Mess", FeatureFlags.MESS, SecurityPermissions.BILL, "InvoiceMess", 45));
        base.add(item(UiCodes.WEIGHT_BILLING, "Weight Billing", FeatureFlags.WEIGHT_SCALE, SecurityPermissions.BILL, "CreatePos", 50));
        base.add(item(UiCodes.APPOINTMENTS, "Appointments", FeatureFlags.APPOINTMENTS, SecurityPermissions.BILL, "MasterData", 55));
        base.add(item(UiCodes.CUSTOM_ORDERS, "Custom Orders", FeatureFlags.CUSTOM_ORDERS, SecurityPermissions.BILL, "MasterData", 60));
        base.add(item(UiCodes.BARCODE, "Barcode", FeatureFlags.BARCODE, SecurityPermissions.BILL, "CreatePos", 65));
        base.add(item(UiCodes.PRODUCTS, "Products", null, SecurityPermissions.MASTER_DATA, "ProductMaster", 70));
        base.add(item(UiCodes.STOCK, "Stock", FeatureFlags.INVENTORY, SecurityPermissions.INVENTORY, "Inventory", 75));
        base.add(item(UiCodes.SERVICES, "Services", FeatureFlags.APPOINTMENTS, SecurityPermissions.MASTER_DATA, "ProductMaster", 80));
        base.add(item(UiCodes.CUSTOMERS, "Customers", FeatureFlags.MESS, SecurityPermissions.MESS_MEMBERS, "MessMemberList", 85));
        base.add(item(UiCodes.REPORTS, "Reports", null, SecurityPermissions.REPORTS, "ReportsHub", 90));
        base.add(item(UiCodes.SETTINGS, "Settings", null, null, "UserSetting", 100));

        List<NavigationItemConfig> resolved = new ArrayList<>();
        for (NavigationItemConfig item : base) {
            boolean visible = NavigationGuard.allows(context, item, templateFeatures);
            resolved.add(item.withVisible(visible));
        }
        Collections.sort(resolved, Comparator.comparingInt(a -> a.order));
        return new NavigationConfiguration(resolved);
    }

    private static NavigationItemConfig item(String code, String title, String feature, String perm,
                                             String target, int order) {
        return new NavigationItemConfig(code, title, null, feature, perm, target, order, true);
    }
}

/**
 * Guards navigation items using feature ∩ permission (soft UI).
 */
final class NavigationGuard {
    private NavigationGuard() {
    }

    static boolean allows(Context context, NavigationItemConfig item, List<String> templateFeatures) {
        if (item == null) {
            return false;
        }
        if (item.featureRequired != null) {
            if (templateFeatures != null && !templateFeatures.contains(item.featureRequired)) {
                return false;
            }
            VisibilityRule rule = VisibilityRule.featureAndPermission(
                    item.featureRequired, item.permissionRequired);
            return rule.evaluate(context);
        }
        if (item.permissionRequired != null) {
            return PermissionRule.softAllows(context, item.permissionRequired);
        }
        return true;
    }
}
