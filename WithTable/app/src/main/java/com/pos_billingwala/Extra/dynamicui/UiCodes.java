package com.pos_billingwala.Extra.dynamicui;

/**
 * Canonical screen / widget / quick-action / field / report / settings codes
 * for the Dynamic UI Engine. One app — codes drive visibility, not duplicate UIs.
 */
public final class UiCodes {

    private UiCodes() {
    }

    // --- Screens ---
    public static final String HOME = "HOME";
    public static final String BILLING = "BILLING";
    public static final String TABLE_MANAGEMENT = "TABLE_MANAGEMENT";
    public static final String ORDERS = "ORDERS";
    public static final String TAKE_AWAY = "TAKE_AWAY";
    public static final String MESS = "MESS";
    public static final String PRODUCTS = "PRODUCTS";
    public static final String CATEGORIES = "CATEGORIES";
    public static final String STOCK = "STOCK";
    public static final String CUSTOMERS = "CUSTOMERS";
    public static final String REPORTS = "REPORTS";
    public static final String SETTINGS = "SETTINGS";
    public static final String APPOINTMENTS = "APPOINTMENTS";
    public static final String SERVICES = "SERVICES";
    public static final String STAFF = "STAFF";
    public static final String CUSTOM_ORDERS = "CUSTOM_ORDERS";
    public static final String WEIGHT_BILLING = "WEIGHT_BILLING";
    public static final String BARCODE = "BARCODE";
    public static final String MASTER_DATA = "MASTER_DATA";
    public static final String INVENTORY = "INVENTORY";
    public static final String EXPENSES = "EXPENSES";
    public static final String PRINTER = "PRINTER";
    public static final String PORTIONS = "PORTIONS";
    public static final String COMBOS = "COMBOS";

    // --- Dashboard widgets ---
    public static final String W_TODAY_SALES = "TODAY_SALES";
    public static final String W_TOTAL_SALES = "TOTAL_SALES";
    public static final String W_TOTAL_BILLS = "TOTAL_BILLS";
    public static final String W_TOTAL_PRODUCTS = "TOTAL_PRODUCTS";
    public static final String W_TOTAL_CUSTOMERS = "TOTAL_CUSTOMERS";
    public static final String W_LOW_STOCK = "LOW_STOCK";
    public static final String W_ACTIVE_TABLES = "ACTIVE_TABLES";
    public static final String W_ACTIVE_ORDERS = "ACTIVE_ORDERS";
    public static final String W_APPOINTMENTS = "APPOINTMENTS";
    public static final String W_TODAY_APPOINTMENTS = "TODAY_APPOINTMENTS";
    public static final String W_TOTAL_SERVICES = "TOTAL_SERVICES";
    public static final String W_PENDING_CUSTOM_ORDERS = "PENDING_CUSTOM_ORDERS";
    public static final String W_TOTAL_STOCK = "TOTAL_STOCK";
    public static final String W_PENDING_REPAIRS = "PENDING_REPAIRS";
    public static final String W_RENTAL_ITEMS = "RENTAL_ITEMS";
    public static final String W_SUBCATEGORIES = "SUBCATEGORIES";
    public static final String W_COMBOS = "COMBOS";

    // --- Quick actions ---
    public static final String QA_FAST_BILLING = "FAST_BILLING";
    public static final String QA_DINE_IN = "DINE_IN";
    public static final String QA_TAKE_AWAY = "TAKE_AWAY";
    public static final String QA_MESS = "MESS";
    public static final String QA_TABLES = "TABLES";
    public static final String QA_WEIGHT_BILLING = "WEIGHT_BILLING";
    public static final String QA_PRODUCTS = "PRODUCTS";
    public static final String QA_STOCK = "STOCK";
    public static final String QA_APPOINTMENTS = "APPOINTMENTS";
    public static final String QA_WALK_IN = "WALK_IN";
    public static final String QA_SERVICES = "SERVICES";
    public static final String QA_BARCODE_SCAN = "BARCODE_SCAN";
    public static final String QA_CUSTOM_ORDER = "CUSTOM_ORDER";
    public static final String QA_PRE_ORDER = "PRE_ORDER";

    // --- Billing UI fields / sections ---
    public static final String BF_PRODUCT = "PRODUCT";
    public static final String BF_CATEGORY = "CATEGORY";
    public static final String BF_BARCODE = "BARCODE";
    public static final String BF_WEIGHT = "WEIGHT";
    public static final String BF_UNIT = "UNIT";
    public static final String BF_RATE = "RATE";
    public static final String BF_TABLE = "TABLE";
    public static final String BF_CUSTOMER = "CUSTOMER";
    public static final String BF_SERVICE = "SERVICE";
    public static final String BF_STAFF = "STAFF";
    public static final String BF_APPOINTMENT = "APPOINTMENT";
    public static final String BF_VARIANT = "VARIANT";
    public static final String BF_SIZE = "SIZE";
    public static final String BF_COLOR = "COLOR";
    public static final String BF_PORTION = "PORTION";
    public static final String BF_SERIAL = "SERIAL";
    public static final String BF_CUSTOM_FIELDS = "CUSTOM_FIELDS";
    public static final String BF_DELIVERY_DATE = "DELIVERY_DATE";
    public static final String BF_ADVANCE = "ADVANCE";
    public static final String BF_KOT = "KOT";
    public static final String BF_DURATION = "DURATION";

    public static final String BS_CATALOG = "SECTION_CATALOG";
    public static final String BS_CART = "SECTION_CART";
    public static final String BS_TABLE = "SECTION_TABLE";
    public static final String BS_WEIGHT = "SECTION_WEIGHT";
    public static final String BS_VARIANT = "SECTION_VARIANT";
    public static final String BS_SERVICE = "SECTION_SERVICE";
    public static final String BS_CUSTOM_ORDER = "SECTION_CUSTOM_ORDER";

    // --- Product form fields ---
    public static final String PF_NAME = "NAME";
    public static final String PF_CATEGORY = "CATEGORY";
    public static final String PF_SUBCATEGORY = "SUBCATEGORY";
    public static final String PF_SKU = "SKU";
    public static final String PF_BARCODE = "BARCODE";
    public static final String PF_PRICE = "PRICE";
    public static final String PF_TAX = "TAX";
    public static final String PF_DESCRIPTION = "DESCRIPTION";
    public static final String PF_IMAGE = "IMAGE";
    public static final String PF_VEG = "VEG_NON_VEG";
    public static final String PF_KITCHEN_ROUTE = "KITCHEN_ROUTE";
    public static final String PF_PREP_TIME = "PREP_TIME";
    public static final String PF_PORTION = "PORTION";
    public static final String PF_ML = "ML";
    public static final String PF_BOTTLE = "BOTTLE_CONVERSION";
    public static final String PF_BAR_ROUTE = "BAR_ROUTE";
    public static final String PF_UNIT = "UNIT";
    public static final String PF_WEIGHT = "WEIGHT";
    public static final String PF_RATE_PER_KG = "RATE_PER_KG";
    public static final String PF_SIZE = "SIZE";
    public static final String PF_COLOR = "COLOR";
    public static final String PF_VARIANTS = "VARIANTS";
    public static final String PF_BRAND = "BRAND";
    public static final String PF_DURATION = "SERVICE_DURATION";
    public static final String PF_STAFF = "STAFF_ASSIGNMENT";
    public static final String PF_SERIAL = "SERIAL_NUMBER";
    public static final String PF_WARRANTY = "WARRANTY";
    public static final String PF_FLAVOUR = "FLAVOUR";
    public static final String PF_CUSTOM_ORDER = "CUSTOM_ORDER_SUPPORT";

    // --- Settings sections ---
    public static final String ST_SHOP = "SHOP_DETAILS";
    public static final String ST_TEMPLATE = "BUSINESS_TEMPLATE";
    public static final String ST_PRINTER = "PRINTER";
    public static final String ST_KOT = "KOT_SETTINGS";
    public static final String ST_TABLE = "TABLE_SETTINGS";
    public static final String ST_BOT = "BOT_SETTINGS";
    public static final String ST_PORTION = "PORTION_SETTINGS";
    public static final String ST_SCALE = "SCALE_SETTINGS";
    public static final String ST_WEIGHT_UNIT = "WEIGHT_UNIT";
    public static final String ST_SERVICE = "SERVICE_SETTINGS";
    public static final String ST_STAFF = "STAFF_SETTINGS";
    public static final String ST_APPOINTMENT = "APPOINTMENT_SETTINGS";
    public static final String ST_VARIANTS = "VARIANT_SETTINGS";
    public static final String ST_INVENTORY = "INVENTORY_SETTINGS";
    public static final String ST_MASTER = "MASTER_DATA";
    public static final String ST_REPORTS = "REPORTS_ENTRY";

    // --- Reports ---
    public static final String RP_SALES = "SALES";
    public static final String RP_SALES_DASHBOARD = "SALES_DASHBOARD";
    public static final String RP_SALES_OVERVIEW = "SALES_OVERVIEW";
    public static final String RP_INVOICE = "INVOICE";
    public static final String RP_TABLES = "TABLES";
    public static final String RP_TAKE_AWAY = "TAKE_AWAY";
    public static final String RP_KOT = "KOT_SUMMARY";
    public static final String RP_PAYMENT = "PAYMENT";
    public static final String RP_DISCOUNT = "DISCOUNT";
    public static final String RP_REFUND = "REFUND";
    public static final String RP_PRODUCT = "PRODUCT";
    public static final String RP_COMBO = "COMBO";
    public static final String RP_EXPENSE = "EXPENSE";
    public static final String RP_MESS = "MESS";
    public static final String RP_MESS_MEMBER = "MESS_MEMBER";
    public static final String RP_WEIGHT_SALES = "WEIGHT_SALES";
    public static final String RP_STOCK = "STOCK";
    public static final String RP_SERVICES = "SERVICES";
    public static final String RP_STAFF = "STAFF";
    public static final String RP_APPOINTMENTS = "APPOINTMENTS";
    public static final String RP_VARIANTS = "VARIANTS";
}
