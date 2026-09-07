package com.pos_billingwala.Extra.dynamicui;

import android.content.Context;

import com.pos_billingwala.Extra.FeatureFlags;
import com.pos_billingwala.Extra.SecurityPermissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class DashboardResolver {

    private DashboardResolver() {
    }

    static DashboardConfiguration resolve(Context context) {
        List<WidgetConfiguration> base = new ArrayList<>();
        base.add(w(UiCodes.W_TOTAL_SALES, "Total Sales", FeatureFlags.TOTAL_SALE_DATA, 10, 10));
        base.add(w(UiCodes.W_TODAY_SALES, "Today Sales", FeatureFlags.TODAY_SALE_DATA, 20, 20));
        base.add(w(UiCodes.W_TOTAL_BILLS, "Total Bills", null, 25, 25));
        base.add(w(UiCodes.W_TOTAL_PRODUCTS, "Products", null, 30, 30));
        base.add(w(UiCodes.W_SUBCATEGORIES, "Subcategories", null, 35, 35));
        base.add(w(UiCodes.W_COMBOS, "Combos", FeatureFlags.COMBOS, 40, 40));
        base.add(w(UiCodes.W_TOTAL_CUSTOMERS, "Customers", FeatureFlags.MESS, 45, 45));
        base.add(w(UiCodes.W_LOW_STOCK, "Low Stock", FeatureFlags.INVENTORY, 50, 50));
        base.add(w(UiCodes.W_TOTAL_STOCK, "Stock", FeatureFlags.INVENTORY, 55, 55));
        base.add(w(UiCodes.W_ACTIVE_TABLES, "Active Tables", FeatureFlags.TABLES, 60, 60));
        base.add(w(UiCodes.W_ACTIVE_ORDERS, "Active Orders", FeatureFlags.DINE_IN, 65, 65));
        base.add(w(UiCodes.W_APPOINTMENTS, "Appointments", FeatureFlags.APPOINTMENTS, 70, 70));
        base.add(w(UiCodes.W_TODAY_APPOINTMENTS, "Today Appointments", FeatureFlags.APPOINTMENTS, 75, 75));
        base.add(w(UiCodes.W_TOTAL_SERVICES, "Services", FeatureFlags.APPOINTMENTS, 80, 80));
        base.add(w(UiCodes.W_PENDING_CUSTOM_ORDERS, "Pending Custom Orders", FeatureFlags.CUSTOM_ORDERS, 85, 85));
        base.add(w(UiCodes.W_PENDING_REPAIRS, "Pending Repairs", null, 90, 90));
        base.add(w(UiCodes.W_RENTAL_ITEMS, "Rental Items", null, 95, 95));

        List<WidgetConfiguration> out = new ArrayList<>();
        for (WidgetConfiguration widget : base) {
            boolean visible = evaluate(context, widget);
            // Repair / rental widgets when those modules are active
            if (UiCodes.W_PENDING_REPAIRS.equals(widget.widgetCode)) {
                visible = com.pos_billingwala.Extra.RepairModule.isEnabled(context);
            }
            if (UiCodes.W_RENTAL_ITEMS.equals(widget.widgetCode)) {
                visible = com.pos_billingwala.Extra.RentalModule.isEnabled(context);
            }
            if (UiCodes.W_TOTAL_BILLS.equals(widget.widgetCode)) {
                // KPI reserved; Home still uses sale cards — keep config ready, hide until wired
                visible = false;
            }
            out.add(widget.withVisible(visible));
        }
        Collections.sort(out, Comparator.comparingInt(a -> a.position));
        return new DashboardConfiguration(out);
    }

    private static boolean evaluate(Context context, WidgetConfiguration widget) {
        VisibilityRule rule = new VisibilityRule(
                widget.featureRequired, widget.permissionRequired, null, true);
        return rule.evaluate(context);
    }

    private static WidgetConfiguration w(String code, String title, String feature, int pos, int priority) {
        return new WidgetConfiguration(code, title, null, feature, null, true, pos, priority, "on_resume");
    }
}

final class QuickActionResolver {

    private QuickActionResolver() {
    }

    static List<QuickActionConfiguration> resolve(Context context) {
        List<QuickActionConfiguration> base = new ArrayList<>();
        base.add(qa(UiCodes.QA_FAST_BILLING, "Fast Billing", FeatureFlags.FAST_BILLING, "CreatePos", 10));
        base.add(qa(UiCodes.QA_DINE_IN, "Dine In", FeatureFlags.DINE_IN, "InvoiceCompanyTable", 20));
        base.add(qa(UiCodes.QA_TAKE_AWAY, "Take Away", FeatureFlags.TAKE_AWAY, "InvoiceTakeAway", 30));
        base.add(qa(UiCodes.QA_MESS, "Mess", FeatureFlags.MESS, "InvoiceMess", 40));
        base.add(qa(UiCodes.QA_TABLES, "Tables", FeatureFlags.TABLES, "InvoiceCompanyTable", 50));
        base.add(qa(UiCodes.QA_WEIGHT_BILLING, "Weight Billing", FeatureFlags.WEIGHT_SCALE, "CreatePos", 60));
        base.add(qa(UiCodes.QA_BARCODE_SCAN, "Barcode Scan", FeatureFlags.BARCODE, "CreatePos", 70));
        base.add(qa(UiCodes.QA_APPOINTMENTS, "Appointments", FeatureFlags.APPOINTMENTS, "MasterData", 80));
        base.add(qa(UiCodes.QA_WALK_IN, "Walk In", FeatureFlags.APPOINTMENTS, "CreatePos", 85));
        base.add(qa(UiCodes.QA_SERVICES, "Services", FeatureFlags.APPOINTMENTS, "ProductMaster", 90));
        base.add(qa(UiCodes.QA_CUSTOM_ORDER, "Custom Order", FeatureFlags.CUSTOM_ORDERS, "CreatePos", 100));
        base.add(qa(UiCodes.QA_PRE_ORDER, "Pre Order", FeatureFlags.CUSTOM_ORDERS, "CreatePos", 105));
        base.add(qa(UiCodes.QA_PRODUCTS, "Products", null, "ProductMaster", 110));
        base.add(qa(UiCodes.QA_STOCK, "Stock", FeatureFlags.INVENTORY, "Inventory", 120));

        List<QuickActionConfiguration> out = new ArrayList<>();
        for (QuickActionConfiguration action : base) {
            boolean visible = QuickActionGuard.allows(context, action);
            out.add(action.withVisible(visible));
        }
        Collections.sort(out, Comparator.comparingInt(a -> a.order));
        return out;
    }

    private static QuickActionConfiguration qa(String code, String title, String feature, String target, int order) {
        return new QuickActionConfiguration(code, title, null, feature, SecurityPermissions.BILL, target, order, true);
    }
}

final class QuickActionGuard {
    private QuickActionGuard() {
    }

    static boolean allows(Context context, QuickActionConfiguration action) {
        if (action == null) {
            return false;
        }
        // Products without feature: always show for elevated / cashier master access soft
        if (action.featureRequired == null) {
            return PermissionRule.softAllows(context, action.permissionRequired);
        }
        return VisibilityRule.featureAndPermission(action.featureRequired, action.permissionRequired)
                .evaluate(context);
    }
}
