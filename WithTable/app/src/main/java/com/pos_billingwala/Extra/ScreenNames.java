package com.pos_billingwala.Extra;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Maps Activity/Fragment simple names to Admin-friendly screen labels. */
public final class ScreenNames {

    private static final Map<String, String> MAP = new HashMap<>();

    static {
        MAP.put("Login", "Login Screen");
        MAP.put("LoginMPin", "Login Screen");
        MAP.put("MainActivity", "Dashboard");
        MAP.put("Home", "Dashboard");
        MAP.put("BluetoothPrint", "Billing Screen");
        MAP.put("TakeAway", "Billing Screen");
        MAP.put("DineIn", "Billing Screen");
        MAP.put("FastBilling", "Billing Screen");
        MAP.put("Mess", "Mess Billing");
        MAP.put("Cart", "Cart");
        MAP.put("Payment", "Payment");
        MAP.put("KOT", "KOT");
        MAP.put("ProductList", "Product List");
        MAP.put("AllProductList", "Product List");
        MAP.put("ProductDetails", "Product Details");
        MAP.put("CustomerList", "Customer List");
        MAP.put("CustomerDetails", "Customer Details");
        MAP.put("CompanyDetailSetting", "Settings");
        MAP.put("InvoiceDiscountReport", "Reports");
        MAP.put("InvoiceRefundReport", "Reports");
        MAP.put("InvoiceDetailsBluetoothPrint", "Invoice Details");
        MAP.put("EditInvoice", "Edit Bill");
        MAP.put("ReportsHub", "Reports");
        MAP.put("UserSetting", "Settings");
        MAP.put("CloudSyncStatus", "Cloud Sync");
        MAP.put("PrinterSetting", "Printer Settings");
        MAP.put("Report", "Reports");
        MAP.put("SplashScreen", "Splash");
        MAP.put("Register", "Registration");
    }

    private ScreenNames() {
    }

    public static String friendly(String activityName, String fragmentName) {
        if (fragmentName != null && !fragmentName.isEmpty()) {
            String f = MAP.get(fragmentName);
            if (f != null) {
                return f;
            }
            return humanize(fragmentName);
        }
        if (activityName != null && !activityName.isEmpty()) {
            String a = MAP.get(activityName);
            if (a != null) {
                return a;
            }
            return humanize(activityName);
        }
        return "Unknown Screen";
    }

    private static String humanize(String className) {
        if (className == null || className.isEmpty()) {
            return "Unknown Screen";
        }
        String spaced = className
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replace('_', ' ')
                .trim();
        if (!spaced.toLowerCase(Locale.US).endsWith("screen")
                && !spaced.toLowerCase(Locale.US).endsWith("list")
                && !spaced.toLowerCase(Locale.US).endsWith("details")) {
            return spaced;
        }
        return spaced;
    }
}
