package com.pos_billingwala.Extra.dynamicui;

import com.pos_billingwala.Extra.BusinessTypes;
import com.pos_billingwala.Extra.FeatureFlags;
import com.pos_billingwala.Extra.SecurityPermissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Safe default POS UI when resolution fails — restaurant-style Home / Billing / Reports / Settings.
 * Never blank screen / crash / infinite loader.
 */
final class UiFallback {

    private UiFallback() {
    }

    static UIConfiguration defaultPos() {
        List<QuickActionConfiguration> actions = Arrays.asList(
                qa(UiCodes.QA_FAST_BILLING, "Fast Billing", FeatureFlags.FAST_BILLING, "CreatePos", 10),
                qa(UiCodes.QA_DINE_IN, "Dine In", FeatureFlags.DINE_IN, "InvoiceCompanyTable", 20),
                qa(UiCodes.QA_TAKE_AWAY, "Take Away", FeatureFlags.TAKE_AWAY, "InvoiceTakeAway", 30),
                qa(UiCodes.QA_MESS, "Mess", FeatureFlags.MESS, "InvoiceMess", 40)
        );
        List<WidgetConfiguration> widgets = Arrays.asList(
                widget(UiCodes.W_TOTAL_SALES, "Total Sales", FeatureFlags.TOTAL_SALE_DATA, 10),
                widget(UiCodes.W_TODAY_SALES, "Today Sales", FeatureFlags.TODAY_SALE_DATA, 20),
                widget(UiCodes.W_TOTAL_PRODUCTS, "Products", null, 30),
                widget(UiCodes.W_SUBCATEGORIES, "Subcategories", null, 40),
                widget(UiCodes.W_COMBOS, "Combos", FeatureFlags.COMBOS, 50)
        );
        List<NavigationItemConfig> nav = Arrays.asList(
                nav(UiCodes.HOME, "Home", null, "Home", 10),
                nav(UiCodes.BILLING, "Billing", FeatureFlags.FAST_BILLING, "CreatePos", 20),
                nav(UiCodes.TABLE_MANAGEMENT, "Tables", FeatureFlags.TABLES, "InvoiceCompanyTable", 30),
                nav(UiCodes.PRODUCTS, "Products", null, "ProductMaster", 40),
                nav(UiCodes.REPORTS, "Reports", null, "ReportsHub", 50),
                nav(UiCodes.SETTINGS, "Settings", null, "UserSetting", 60)
        );
        List<ScreenConfiguration> reports = Arrays.asList(
                report(UiCodes.RP_SALES_DASHBOARD, "Sales Dashboard", null, 10),
                report(UiCodes.RP_SALES_OVERVIEW, "Sales Overview", null, 20),
                report(UiCodes.RP_INVOICE, "Invoice Report", null, 30),
                report(UiCodes.RP_SALES, "Sale Report", null, 40),
                report(UiCodes.RP_TABLES, "Table Report", FeatureFlags.DINE_IN, 50),
                report(UiCodes.RP_TAKE_AWAY, "Take Away Report", FeatureFlags.TAKE_AWAY, 60),
                report(UiCodes.RP_PAYMENT, "Payment Report", null, 70),
                report(UiCodes.RP_PRODUCT, "Product Report", null, 80),
                report(UiCodes.RP_EXPENSE, "Expense Report", null, 90)
        );
        List<SectionConfiguration> settings = Arrays.asList(
                setting(UiCodes.ST_SHOP, "Shop Details", null, SecurityPermissions.SHOP_SETTINGS, 10),
                setting(UiCodes.ST_TEMPLATE, "Business Template", null, SecurityPermissions.BUSINESS_TEMPLATE, 20),
                setting(UiCodes.ST_PRINTER, "Printer", null, SecurityPermissions.PRINTER_SETTINGS, 30),
                setting(UiCodes.ST_INVENTORY, "Inventory", FeatureFlags.INVENTORY, SecurityPermissions.INVENTORY, 40),
                setting(UiCodes.ST_MASTER, "Master Data", null, SecurityPermissions.MASTER_DATA, 50),
                setting(UiCodes.ST_REPORTS, "Reports", null, SecurityPermissions.REPORTS, 60)
        );
        List<BillingFieldConfig> billingFields = Arrays.asList(
                field(UiCodes.BF_PRODUCT, "Product", true, 10, null),
                field(UiCodes.BF_CATEGORY, "Category", true, 20, null),
                field(UiCodes.BF_TABLE, "Table", true, 30, FeatureFlags.TABLES),
                field(UiCodes.BF_PORTION, "Portion", true, 40, FeatureFlags.PORTIONS),
                field(UiCodes.BF_KOT, "KOT", true, 50, FeatureFlags.KOT)
        );
        List<ProductFieldConfig> productFields = commonProductFields();
        productFields.add(new ProductFieldConfig(UiCodes.PF_VEG, "Veg / Non Veg", true, false, 100, null));
        productFields.add(new ProductFieldConfig(UiCodes.PF_KITCHEN_ROUTE, "Kitchen Route", true, false, 110, FeatureFlags.KOT));

        return new UIConfiguration(
                "fallback",
                "restaurant_default",
                BusinessTypes.RESTAURANT,
                Arrays.asList(FeatureFlags.FAST_BILLING, FeatureFlags.DINE_IN, FeatureFlags.TAKE_AWAY,
                        FeatureFlags.MESS, FeatureFlags.TABLES, FeatureFlags.KOT, FeatureFlags.COMBOS,
                        FeatureFlags.INVENTORY, FeatureFlags.GST),
                new DashboardConfiguration(widgets),
                new NavigationConfiguration(nav),
                actions,
                new BillingUIConfig(null, billingFields),
                new FormConfiguration(new ProductFormConfig(productFields)),
                settings,
                reports,
                defaultScreens(),
                System.currentTimeMillis(),
                true,
                false
        );
    }

    private static List<ScreenConfiguration> defaultScreens() {
        List<ScreenConfiguration> list = new ArrayList<>();
        list.add(screen(UiCodes.HOME, "Home", null, null, 10));
        list.add(screen(UiCodes.BILLING, "Billing", FeatureFlags.FAST_BILLING, SecurityPermissions.BILL, 20));
        list.add(screen(UiCodes.TABLE_MANAGEMENT, "Tables", FeatureFlags.TABLES, SecurityPermissions.BILL, 30));
        list.add(screen(UiCodes.PRODUCTS, "Products", null, SecurityPermissions.MASTER_DATA, 40));
        list.add(screen(UiCodes.REPORTS, "Reports", null, SecurityPermissions.REPORTS, 50));
        list.add(screen(UiCodes.SETTINGS, "Settings", null, null, 60));
        return list;
    }

    static List<ProductFieldConfig> commonProductFields() {
        List<ProductFieldConfig> list = new ArrayList<>();
        list.add(new ProductFieldConfig(UiCodes.PF_NAME, "Name", true, true, 10, null));
        list.add(new ProductFieldConfig(UiCodes.PF_CATEGORY, "Category", true, true, 20, null));
        list.add(new ProductFieldConfig(UiCodes.PF_SUBCATEGORY, "Subcategory", true, false, 30, null));
        list.add(new ProductFieldConfig(UiCodes.PF_SKU, "SKU", true, false, 40, null));
        list.add(new ProductFieldConfig(UiCodes.PF_BARCODE, "Barcode", true, false, 50, FeatureFlags.BARCODE));
        list.add(new ProductFieldConfig(UiCodes.PF_PRICE, "Price", true, true, 60, null));
        list.add(new ProductFieldConfig(UiCodes.PF_TAX, "Tax", true, false, 70, FeatureFlags.GST));
        list.add(new ProductFieldConfig(UiCodes.PF_DESCRIPTION, "Description", true, false, 80, null));
        list.add(new ProductFieldConfig(UiCodes.PF_IMAGE, "Image", true, false, 90, null));
        return list;
    }

    private static QuickActionConfiguration qa(String code, String title, String feature, String target, int order) {
        return new QuickActionConfiguration(code, title, null, feature, SecurityPermissions.BILL, target, order, true);
    }

    private static WidgetConfiguration widget(String code, String title, String feature, int pos) {
        return new WidgetConfiguration(code, title, null, feature, null, true, pos, pos, "on_resume");
    }

    private static NavigationItemConfig nav(String code, String title, String feature, String target, int order) {
        return new NavigationItemConfig(code, title, null, feature, null, target, order, true);
    }

    private static ScreenConfiguration report(String code, String title, String feature, int order) {
        return new ScreenConfiguration(code, title, null, feature, SecurityPermissions.REPORTS, null, true, order);
    }

    private static SectionConfiguration setting(String code, String title, String feature, String perm, int order) {
        return new SectionConfiguration(code, title, true, order, feature, perm);
    }

    private static BillingFieldConfig field(String code, String title, boolean vis, int order, String feature) {
        return new BillingFieldConfig(code, title, vis, order, feature);
    }

    private static ScreenConfiguration screen(String code, String title, String feature, String perm, int order) {
        return new ScreenConfiguration(code, title, null, feature, perm, null, true, order);
    }
}
