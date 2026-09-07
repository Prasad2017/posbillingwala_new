package com.pos_billingwala.Extra.dynamicui;

import android.content.Context;

import com.pos_billingwala.Extra.FeatureEngine;
import com.pos_billingwala.Extra.FeatureFlags;
import com.pos_billingwala.Extra.SecurityPermissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class SettingsUiResolver {

    private SettingsUiResolver() {
    }

    static List<SectionConfiguration> resolve(Context context) {
        List<SectionConfiguration> base = new ArrayList<>();
        base.add(s(UiCodes.ST_REPORTS, "Reports", null, SecurityPermissions.REPORTS, 10));
        base.add(s(UiCodes.ST_MASTER, "Master Data", null, SecurityPermissions.MASTER_DATA, 20));
        base.add(s(UiCodes.ST_SHOP, "Shop Details", null, SecurityPermissions.SHOP_SETTINGS, 30));
        base.add(s(UiCodes.ST_TEMPLATE, "Business Template", null, SecurityPermissions.BUSINESS_TEMPLATE, 40));
        base.add(s(UiCodes.ST_PRINTER, "Printer", null, SecurityPermissions.PRINTER_SETTINGS, 50));
        base.add(s(UiCodes.ST_TABLE, "Table Settings", FeatureFlags.TABLES, SecurityPermissions.SHOP_SETTINGS, 55));
        base.add(s(UiCodes.ST_KOT, "KOT Settings", FeatureFlags.KOT, SecurityPermissions.PRINTER_SETTINGS, 60));
        base.add(s(UiCodes.ST_BOT, "BOT / Bar Printer", FeatureFlags.BOT, SecurityPermissions.PRINTER_SETTINGS, 65));
        base.add(s(UiCodes.ST_PORTION, "Portion Settings", FeatureFlags.PORTIONS, SecurityPermissions.MASTER_DATA, 70));
        base.add(s(UiCodes.ST_SCALE, "Scale Settings", FeatureFlags.WEIGHT_SCALE, SecurityPermissions.SHOP_SETTINGS, 75));
        base.add(s(UiCodes.ST_WEIGHT_UNIT, "Weight Unit", FeatureFlags.WEIGHT_SCALE, SecurityPermissions.SHOP_SETTINGS, 80));
        base.add(s(UiCodes.ST_SERVICE, "Service Settings", FeatureFlags.APPOINTMENTS, SecurityPermissions.MASTER_DATA, 85));
        base.add(s(UiCodes.ST_STAFF, "Staff", FeatureFlags.APPOINTMENTS, SecurityPermissions.SET_STAFF_ROLE, 90));
        base.add(s(UiCodes.ST_APPOINTMENT, "Appointment", FeatureFlags.APPOINTMENTS, SecurityPermissions.MASTER_DATA, 95));
        base.add(s(UiCodes.ST_VARIANTS, "Variants / Size / Color", FeatureFlags.VARIANTS, SecurityPermissions.MASTER_DATA, 100));
        base.add(s(UiCodes.ST_INVENTORY, "Inventory", FeatureFlags.INVENTORY, SecurityPermissions.INVENTORY, 110));

        List<SectionConfiguration> out = new ArrayList<>();
        for (SectionConfiguration section : base) {
            boolean visible = true;
            if (section.featureRequired != null) {
                visible = FeatureEngine.isEnabled(context, section.featureRequired);
            }
            if (visible && section.permissionRequired != null) {
                visible = PermissionRule.softAllows(context, section.permissionRequired);
            }
            out.add(section.withVisible(visible));
        }
        Collections.sort(out, Comparator.comparingInt(a -> a.order));
        return out;
    }

    private static SectionConfiguration s(String code, String title, String feature, String perm, int order) {
        return new SectionConfiguration(code, title, true, order, feature, perm);
    }
}

final class ReportsUiResolver {

    private ReportsUiResolver() {
    }

    static List<ScreenConfiguration> resolve(Context context) {
        List<ScreenConfiguration> base = new ArrayList<>();
        base.add(r(UiCodes.RP_SALES_DASHBOARD, "Sales Dashboard", null, 10));
        base.add(r(UiCodes.RP_SALES_OVERVIEW, "Sales Overview", null, 20));
        base.add(r(UiCodes.RP_INVOICE, "Invoice Report", null, 30));
        base.add(r(UiCodes.RP_SALES, "Sale Report", null, 40));
        base.add(r(UiCodes.RP_TABLES, "Table Report", FeatureFlags.DINE_IN, 50));
        base.add(r(UiCodes.RP_KOT, "KOT Summary", FeatureFlags.KOT, 55));
        base.add(r(UiCodes.RP_TAKE_AWAY, "Take Away Report", FeatureFlags.TAKE_AWAY, 60));
        base.add(r(UiCodes.RP_PAYMENT, "Payment Report", null, 70));
        base.add(r(UiCodes.RP_DISCOUNT, "Discount Report", null, 80));
        base.add(r(UiCodes.RP_REFUND, "Refund Report", null, 90));
        base.add(r(UiCodes.RP_PRODUCT, "Product Report", null, 100));
        base.add(r(UiCodes.RP_COMBO, "Combo Report", FeatureFlags.COMBOS, 110));
        base.add(r(UiCodes.RP_EXPENSE, "Expense Report", null, 120));
        base.add(r(UiCodes.RP_MESS_MEMBER, "Mess Member Report", FeatureFlags.MESS, 130));
        base.add(r(UiCodes.RP_MESS, "Mess Report", FeatureFlags.MESS, 140));
        base.add(r(UiCodes.RP_WEIGHT_SALES, "Weight Sales", FeatureFlags.WEIGHT_SCALE, 150));
        base.add(r(UiCodes.RP_STOCK, "Stock Report", FeatureFlags.INVENTORY, 160));
        base.add(r(UiCodes.RP_SERVICES, "Services Report", FeatureFlags.APPOINTMENTS, 170));
        base.add(r(UiCodes.RP_STAFF, "Staff Report", FeatureFlags.APPOINTMENTS, 180));
        base.add(r(UiCodes.RP_APPOINTMENTS, "Appointments Report", FeatureFlags.APPOINTMENTS, 190));
        base.add(r(UiCodes.RP_VARIANTS, "Variants Report", FeatureFlags.VARIANTS, 200));

        List<ScreenConfiguration> out = new ArrayList<>();
        for (ScreenConfiguration screen : base) {
            boolean visible = true;
            if (screen.featureRequired != null) {
                visible = FeatureEngine.isEnabled(context, screen.featureRequired);
            }
            // Soft permission: reports always require PIN on open; still show menu for roles that can attempt
            if (visible) {
                visible = PermissionRule.softAllows(context, SecurityPermissions.REPORTS);
            }
            out.add(screen.withVisible(visible));
        }
        Collections.sort(out, Comparator.comparingInt(a -> a.order));
        return out;
    }

    private static ScreenConfiguration r(String code, String title, String feature, int order) {
        return new ScreenConfiguration(code, title, null, feature, SecurityPermissions.REPORTS, null, true, order);
    }
}

final class ScreenVisibilityResolver {

    private ScreenVisibilityResolver() {
    }

    static List<ScreenConfiguration> resolve(Context context) {
        List<ScreenConfiguration> base = new ArrayList<>();
        base.add(sc(UiCodes.HOME, "Home", null, null, 10));
        base.add(sc(UiCodes.BILLING, "Billing", FeatureFlags.FAST_BILLING, SecurityPermissions.BILL, 20));
        base.add(sc(UiCodes.TABLE_MANAGEMENT, "Table Management", FeatureFlags.TABLES, SecurityPermissions.BILL, 30));
        base.add(sc(UiCodes.ORDERS, "Orders", FeatureFlags.DINE_IN, SecurityPermissions.BILL, 35));
        base.add(sc(UiCodes.TAKE_AWAY, "Take Away", FeatureFlags.TAKE_AWAY, SecurityPermissions.BILL, 40));
        base.add(sc(UiCodes.MESS, "Mess", FeatureFlags.MESS, SecurityPermissions.BILL, 45));
        base.add(sc(UiCodes.WEIGHT_BILLING, "Weight Billing", FeatureFlags.WEIGHT_SCALE, SecurityPermissions.BILL, 50));
        base.add(sc(UiCodes.PRODUCTS, "Products", null, SecurityPermissions.MASTER_DATA, 60));
        base.add(sc(UiCodes.CATEGORIES, "Categories", null, SecurityPermissions.MASTER_DATA, 65));
        base.add(sc(UiCodes.MASTER_DATA, "Master Data", null, SecurityPermissions.MASTER_DATA, 68));
        base.add(sc(UiCodes.PORTIONS, "Portions", FeatureFlags.PORTIONS, SecurityPermissions.MASTER_DATA, 70));
        base.add(sc(UiCodes.COMBOS, "Combos", FeatureFlags.COMBOS, SecurityPermissions.MASTER_DATA, 75));
        base.add(sc(UiCodes.STOCK, "Stock", FeatureFlags.INVENTORY, SecurityPermissions.INVENTORY, 80));
        base.add(sc(UiCodes.INVENTORY, "Inventory", FeatureFlags.INVENTORY, SecurityPermissions.INVENTORY, 85));
        base.add(sc(UiCodes.APPOINTMENTS, "Appointments", FeatureFlags.APPOINTMENTS, SecurityPermissions.MASTER_DATA, 90));
        base.add(sc(UiCodes.CUSTOM_ORDERS, "Custom Orders", FeatureFlags.CUSTOM_ORDERS, SecurityPermissions.MASTER_DATA, 95));
        base.add(sc(UiCodes.REPORTS, "Reports", null, SecurityPermissions.REPORTS, 100));
        base.add(sc(UiCodes.SETTINGS, "Settings", null, null, 110));
        base.add(sc(UiCodes.PRINTER, "Printer", null, SecurityPermissions.PRINTER_SETTINGS, 120));
        base.add(sc(UiCodes.EXPENSES, "Expenses", null, SecurityPermissions.EXPENSE, 130));
        base.add(sc(UiCodes.CUSTOMERS, "Customers", FeatureFlags.MESS, SecurityPermissions.MESS_MEMBERS, 140));
        base.add(sc(UiCodes.BARCODE, "Barcode", FeatureFlags.BARCODE, SecurityPermissions.BILL, 150));
        base.add(sc(UiCodes.SERVICES, "Services", FeatureFlags.APPOINTMENTS, SecurityPermissions.MASTER_DATA, 160));
        base.add(sc(UiCodes.STAFF, "Staff", FeatureFlags.APPOINTMENTS, SecurityPermissions.MASTER_DATA, 170));

        List<ScreenConfiguration> out = new ArrayList<>();
        for (ScreenConfiguration screen : base) {
            VisibilityRule rule = new VisibilityRule(
                    screen.featureRequired, screen.permissionRequired, screen.businessRule, true);
            out.add(screen.withVisible(rule.evaluate(context)));
        }
        Collections.sort(out, Comparator.comparingInt(a -> a.order));
        return out;
    }

    private static ScreenConfiguration sc(String code, String title, String feature, String perm, int order) {
        return new ScreenConfiguration(code, title, null, feature, perm, null, true, order);
    }
}
