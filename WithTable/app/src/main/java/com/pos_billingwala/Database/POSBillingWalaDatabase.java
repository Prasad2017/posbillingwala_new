package com.pos_billingwala.Database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.pos_billingwala.Extra.BranchSession;
import com.pos_billingwala.Extra.CartItemType;
import com.pos_billingwala.Extra.ComboValidator;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.Model.ComboItemDraft;
import com.pos_billingwala.Model.ComboItemResponse;
import com.pos_billingwala.Model.ComboResponse;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.ExpenseResponse;
import com.pos_billingwala.Model.FoodTypeResponse;
import com.pos_billingwala.Model.InventoryResponse;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.Model.MessInvoiceResponse;
import com.pos_billingwala.Model.MessTokenResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.Model.ProductCategoryResponse;
import com.pos_billingwala.Model.PortionMasterResponse;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.Model.ProductSubcategoryResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;


@SuppressLint("Range, Recycle")
public class POSBillingWalaDatabase extends SQLiteOpenHelper {

    // Database Name
    public static final String DATABASE_NAME = "pos_billingwala_db";
    public static final String PRODUCT_CATEGORY_TABLE = "product_category";
    public static final String FOOD_TYPE_TABLE = "food_type";
    public static final String PRODUCT_SUBCATEGORY_TABLE = "product_subcategory";
    public static final String PORTION_MASTER_TABLE = "portion_master";
    public static final String PRODUCT_PORTION_TABLE = "product_portion";
    public static final String PRODUCT_TABLE = "product";
    public static final String CART_PRODUCT_TABLE = "cart_product";
    public static final String INVOICE_TABLE = "invoice";
    public static final String INVOICE_PRODUCT_TABLE = "invoice_final_product";
    public static final String PRINTER_SETTING_TABLE = "company_printer_setting";
    public static final String COMPANY_TABLE = "company";
    public static final String INVENTORY_TABLE = "inventory";
    public static final String EXPENSES_TABLE = "expenses";
    public static final String MEMBER_TABLE = "member";
    public static final String MEMBER_PAYMENT_TABLE = "member_payment";
    public static final String MESS_INVOICE_TABLE = "mess_invoice";
    public static final String MESS_TOKEN_TABLE = "mess_token";
    public static final String COMBO_TABLE = "combo";
    public static final String COMBO_ITEM_TABLE = "combo_item";
    public static final String CART_COMBO_ITEM_TABLE = "cart_combo_item";
    public static final String INVOICE_COMBO_ITEM_TABLE = "invoice_combo_item";
    public static final String INVOICE_PRODUCT_DELETE_QUEUE_TABLE = "invoice_product_delete_queue";
    // Database Version
    public static final int DATABASE_VERSION = 25;

    /** SQL suffix: only rows for the logged-in licence branch. */
    private static String andBranchScope(String tableAlias) {
        String branchId = BranchSession.effectiveBranchId();
        if (branchId == null || branchId.isEmpty()) {
            return "";
        }
        String column = (tableAlias == null || tableAlias.isEmpty()) ? "branchId" : tableAlias + ".branchId";
        return " AND " + column + " = '" + branchId.replace("'", "''") + "'";
    }

    private static String whereBranchScope(String tableAlias) {
        String branchId = BranchSession.effectiveBranchId();
        if (branchId == null || branchId.isEmpty()) {
            return "";
        }
        String column = (tableAlias == null || tableAlias.isEmpty()) ? "branchId" : tableAlias + ".branchId";
        return " WHERE " + column + " = '" + branchId.replace("'", "''") + "'";
    }

    /** Exclude refunded bills from sales totals and payment reports. */
    public static String notRefundedClause() {
        return "IFNULL(invoiceOrderStatus,'completed') != 'refunded'";
    }

    public static String andNotRefunded() {
        return " AND " + notRefundedClause();
    }

    public boolean hasInvoicesForOtherBranch(String branchId) {
        if (branchId == null || branchId.isEmpty()) {
            return false;
        }
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM " + INVOICE_TABLE
                        + " WHERE branchId IS NOT NULL AND TRIM(branchId) != '' AND branchId != ? LIMIT 1",
                new String[]{branchId});
        boolean found = cursor.moveToFirst();
        cursor.close();
        db.close();
        return found;
    }

    public void purgeLocalDataNotMatchingBranch(String branchId) {
        if (branchId == null || branchId.isEmpty()) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String notThisBranch = "branchId IS NULL OR TRIM(branchId) = '' OR branchId != ?";
            db.execSQL(
                    "DELETE FROM " + INVOICE_PRODUCT_TABLE
                            + " WHERE invoiceNumber IN (SELECT invoiceNumber FROM " + INVOICE_TABLE
                            + " WHERE " + notThisBranch + ")",
                    new Object[]{branchId});
            db.delete(INVOICE_TABLE, notThisBranch, new String[]{branchId});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void claimUnscopedRowsForBranch(String branchId) {
        if (branchId == null || branchId.isEmpty()) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("branchId", branchId);
        db.update(INVOICE_TABLE, values, "branchId IS NULL OR TRIM(branchId) = ''", null);
        db.update(INVOICE_PRODUCT_TABLE, values, "branchId IS NULL OR TRIM(branchId) = ''", null);
        db.close();
    }

    /**********************************************  QUERY START PART  **********************************************/

    public final String FOOD_TYPE_QUERY = "CREATE TABLE IF NOT EXISTS " + FOOD_TYPE_TABLE
            + "(foodTypeId INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " foodTypeName VARCHAR NOT NULL,"
            + " foodTypeCode VARCHAR NOT NULL,"
            + " foodTypeSortOrder INTEGER DEFAULT 0,"
            + " foodTypeStatus TINYINT DEFAULT 1)";

    public final String PRODUCT_CATEGORY_QUERY = "CREATE TABLE IF NOT EXISTS " + PRODUCT_CATEGORY_TABLE
            + "(categoryId INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " categoryName VARCHAR,"
            + " foodTypeId INTEGER,"
            + " categorySortOrder INTEGER DEFAULT 0,"
            + " categoryDeletedStatus VARCHAR,"
            + " categoryNetworkStatus VARCHAR,"
            + " categoryStatus TINYINT)";

    public final String PRODUCT_SUBCATEGORY_QUERY = "CREATE TABLE IF NOT EXISTS " + PRODUCT_SUBCATEGORY_TABLE
            + "(subcategoryId INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " categoryId INTEGER,"
            + " subcategoryName VARCHAR,"
            + " subcategorySortOrder INTEGER DEFAULT 0,"
            + " subcategoryDeletedStatus VARCHAR DEFAULT '0',"
            + " subcategoryNetworkStatus VARCHAR,"
            + " subcategoryStatus TINYINT DEFAULT 0)";

    public final String PORTION_MASTER_QUERY = "CREATE TABLE IF NOT EXISTS " + PORTION_MASTER_TABLE
            + "(portionMasterId INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " portionName VARCHAR NOT NULL,"
            + " portionMasterDeletedStatus VARCHAR DEFAULT '0',"
            + " portionMasterNetworkStatus VARCHAR,"
            + " portionMasterStatus TINYINT DEFAULT 0)";

    public final String PRODUCT_PORTION_QUERY = "CREATE TABLE IF NOT EXISTS " + PRODUCT_PORTION_TABLE
            + "(portionId INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " productId INTEGER NOT NULL,"
            + " portionMasterId INTEGER,"
            + " portionName VARCHAR NOT NULL,"
            + " portionPrice VARCHAR NOT NULL,"
            + " portionSortOrder INTEGER DEFAULT 0,"
            + " portionDeletedStatus VARCHAR DEFAULT '0',"
            + " portionNetworkStatus VARCHAR,"
            + " portionStatus TINYINT DEFAULT 1)";

    public final String PRODUCT_QUERY = "CREATE TABLE IF NOT EXISTS " + PRODUCT_TABLE
            + "(productId INTEGER PRIMARY KEY AUTOINCREMENT, userId VARCHAR, categoryId VARCHAR, categoryName VARCHAR,"
            + " subcategoryId INTEGER,"
            + " productCode VARCHAR, productName VARCHAR, productPrice VARCHAR, openPrice VARCHAR,"
            + "productUnit VARCHAR, productCGST VARCHAR, productSGST VARCHAR, productWithGSTPrice VARCHAR,"
            + " productDeletedStatus VARCHAR, productNetworkStatus VARCHAR, productStatus TINYINT)";

    public final String CART_PRODUCT_QUERY = "CREATE TABLE IF NOT EXISTS " + CART_PRODUCT_TABLE
            + "(cartId INTEGER PRIMARY KEY AUTOINCREMENT, userId VARCHAR, productId VARCHAR, productName VARCHAR,"
            + " productOldPrice VARCHAR, productNewPrice VARCHAR, productUnit VARCHAR, productCGST VARCHAR,"
            + " productSGST VARCHAR, productQuantity VARCHAR, cartDiscount VARCHAR, cartDiscountType VARCHAR,"
            + " noOfTable VARCHAR, cartOrderStatus VARCHAR, cartStatus TINYINT,"
            + " portionId VARCHAR, portionName VARCHAR, snapshotProductName VARCHAR, snapshotLinePrice VARCHAR)";

    public final String INVOICE_QUERY = "CREATE TABLE IF NOT EXISTS " + INVOICE_TABLE + "(invoiceId INTEGER PRIMARY KEY AUTOINCREMENT, userId VARCHAR, noOfTable VARCHAR, invoiceNumber VARCHAR, customerName VARCHAR, customerMobile VARCHAR, customerEmail VARCHAR, " + "customerAddress VARCHAR, invoiceDate VARCHAR, subTotal VARCHAR, totalGSTAmount, discount VARCHAR, discountType VARCHAR, totalAmount VARCHAR, paymentMode VARCHAR, invoiceOrderStatus VARCHAR, invoiceType VARCHAR, invoiceNetworkStatus VARCHAR, invoiceStatus TINYINT)";

    public final String INVOICE_PRODUCT_QUERY = "CREATE TABLE IF NOT EXISTS " + INVOICE_PRODUCT_TABLE
            + "(invoiceProductId INTEGER PRIMARY KEY AUTOINCREMENT, invoiceNumber VARCHAR, productName VARCHAR,"
            + " productPrice VARCHAR, productUnit VARCHAR, productCGST VARCHAR, productSGST VARCHAR,"
            + " productQuantity VARCHAR, productStatus VARCHAR, invoiceProductNetworkStatus VARCHAR,"
            + " invoiceProductStatus TINYINT,"
            + " portionId VARCHAR, portionName VARCHAR, snapshotProductName VARCHAR, snapshotLinePrice VARCHAR)";

    public final String PRINTER_SETTING_QUERY = "CREATE TABLE IF NOT EXISTS " + PRINTER_SETTING_TABLE + "(settingId INTEGER PRIMARY KEY AUTOINCREMENT, printerName VARCHAR, invoicePrefix VARCHAR, invoiceTitle VARCHAR, invoiceTermsCondition VARCHAR, logoUse VARCHAR, paymentUse VARCHAR, customerUse VARCHAR, productQuantityUpdate VARCHAR, duplicateBillUse VARCHAR, bluetoothAddress VARCHAR, bluetoothKOTAddress VARCHAR, KOTPrinterName VARCHAR, printerFeedLines VARCHAR, KotPrinterFeedLines VARCHAR, settingStatus TINYINT)";

    public final String COMPANY_QUERY = "CREATE TABLE IF NOT EXISTS " + COMPANY_TABLE + "(companyId INTEGER PRIMARY KEY AUTOINCREMENT, companyName VARCHAR, cashierName VARCHAR, companyMobile VARCHAR, " + "companyAddress VARCHAR, shopName1 VARCHAR, shopName2 VARCHAR, addressLine1 VARCHAR, addressLine2 VARCHAR, addressLine3 VARCHAR, phoneNo1 VARCHAR, phoneNo2 VARCHAR, currencyName VARCHAR, countryName VARCHAR, stateName VARCHAR, tableStatus VARCHAR, noOfTable VARCHAR,gstStatus VARCHAR, gstNumber VARCHAR, shopCGST VARCHAR, shopSGST VARCHAR, panNumber VARCHAR, companyFssis VARCHAR, companyLogo VARCHAR, paymentLogo VARCHAR, companyStatus TINYINT)";

    public final String INVENTORY_QUERY = "CREATE TABLE IF NOT EXISTS " + INVENTORY_TABLE + "(inventoryId INTEGER PRIMARY KEY AUTOINCREMENT, productId VARCHAR, productInventoryQuantity VARCHAR, afterSaleInventoryQuantity VARCHAR, saleInventoryQuantity VARCHAR, inventoryDate VARCHAR, inventoryNetworkStatus VARCHAR, inventoryStatus TINYINT)";

    public final String EXPENSES_QUERY = "CREATE TABLE IF NOT EXISTS " + EXPENSES_TABLE + "(expensesId INTEGER PRIMARY KEY AUTOINCREMENT, expensesName VARCHAR, expensesAmount VARCHAR, expensesDate VARCHAR, expensesNetworkStatus VARCHAR, expensesStatus TINYINT)";

    public final String MEMBER_QUERY = "CREATE TABLE IF NOT EXISTS " + MEMBER_TABLE + "(memberId INTEGER PRIMARY KEY AUTOINCREMENT, memberName VARCHAR, memberAddress VARCHAR, memberMobileNumber VARCHAR, memberAlternetMobileNumber VARCHAR, memberNetworkStatus VARCHAR, memberStatus TINYINT)";
    public final String MEMBER_PAYMENT_QUERY = "CREATE TABLE IF NOT EXISTS " + MEMBER_PAYMENT_TABLE + "(paymentId INTEGER PRIMARY KEY AUTOINCREMENT, memberId VARCHAR, memberName VARCHAR, paymentMessAmount VARCHAR, paymentPaidAmount VARCHAR, messTotalDays VARCHAR, paymentDate VARCHAR, paymentNetworkStatus VARCHAR, paymentStatus TINYINT)";
    public final String MESS_INVOICE_QUERY = "CREATE TABLE IF NOT EXISTS " + MESS_INVOICE_TABLE + "(invoiceId INTEGER PRIMARY KEY AUTOINCREMENT, memberId VARCHAR, memberName VARCHAR, messType VARCHAR, messInvoiceDate VARCHAR, messInvoiceNetworkStatus VARCHAR, messInvoiceStatus TINYINT)";

    public final String MESS_TOKEN_QUERY = "CREATE TABLE IF NOT EXISTS " + MESS_TOKEN_TABLE
            + "(tokenId INTEGER PRIMARY KEY AUTOINCREMENT, tokenCode VARCHAR NOT NULL UNIQUE, memberId VARCHAR, memberName VARCHAR,"
            + " memberMobile VARCHAR, memberType VARCHAR, messType VARCHAR, tokenAmount VARCHAR, tokenDate VARCHAR,"
            + " verifiedDate VARCHAR, tokenNetworkStatus VARCHAR, tokenState VARCHAR DEFAULT 'active',"
            + " tokenStatus TINYINT DEFAULT 0, verifyNetworkStatus VARCHAR, verifyStatus TINYINT DEFAULT 0)";

    public final String COMBO_QUERY = "CREATE TABLE IF NOT EXISTS " + COMBO_TABLE
            + "(comboId INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " comboName VARCHAR NOT NULL,"
            + " comboCode VARCHAR,"
            + " comboPrice VARCHAR NOT NULL,"
            + " comboCGST VARCHAR,"
            + " comboSGST VARCHAR,"
            + " comboWithGSTPrice VARCHAR,"
            + " comboActiveStatus VARCHAR DEFAULT '1',"
            + " comboDeletedStatus VARCHAR DEFAULT '0',"
            + " comboNetworkStatus VARCHAR,"
            + " comboStatus TINYINT DEFAULT 0,"
            + " comboSortOrder INTEGER DEFAULT 0)";

    public final String COMBO_ITEM_QUERY = "CREATE TABLE IF NOT EXISTS " + COMBO_ITEM_TABLE
            + "(comboItemId INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " comboId INTEGER NOT NULL,"
            + " productId INTEGER NOT NULL,"
            + " portionId INTEGER,"
            + " comboItemQuantity VARCHAR NOT NULL DEFAULT '1',"
            + " comboItemSortOrder INTEGER DEFAULT 0,"
            + " comboItemDeletedStatus VARCHAR DEFAULT '0',"
            + " comboItemNetworkStatus VARCHAR,"
            + " comboItemStatus TINYINT DEFAULT 0)";

    public final String CART_COMBO_ITEM_QUERY = "CREATE TABLE IF NOT EXISTS " + CART_COMBO_ITEM_TABLE
            + "(cartComboItemId INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " cartId INTEGER NOT NULL,"
            + " comboId INTEGER,"
            + " productId VARCHAR,"
            + " productNameSnapshot VARCHAR,"
            + " portionId VARCHAR,"
            + " portionNameSnapshot VARCHAR,"
            + " quantity VARCHAR NOT NULL DEFAULT '1',"
            + " sortOrder INTEGER DEFAULT 0)";

    public final String INVOICE_COMBO_ITEM_QUERY = "CREATE TABLE IF NOT EXISTS " + INVOICE_COMBO_ITEM_TABLE
            + "(invoiceComboItemId INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " invoiceNumber VARCHAR,"
            + " invoiceProductNetworkStatus VARCHAR,"
            + " comboId VARCHAR,"
            + " comboNetworkStatus VARCHAR,"
            + " productId VARCHAR,"
            + " productNameSnapshot VARCHAR,"
            + " portionId VARCHAR,"
            + " portionNameSnapshot VARCHAR,"
            + " quantity VARCHAR NOT NULL DEFAULT '1',"
            + " sortOrder INTEGER DEFAULT 0,"
            + " invoiceComboItemNetworkStatus VARCHAR,"
            + " invoiceComboItemStatus TINYINT DEFAULT 0)";

    public final String INVOICE_PRODUCT_DELETE_QUEUE_QUERY = "CREATE TABLE IF NOT EXISTS "
            + INVOICE_PRODUCT_DELETE_QUEUE_TABLE
            + "(deleteId INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " invoiceNumber VARCHAR,"
            + " invoiceProductNetworkStatus VARCHAR)";

    /**********************************************  QUERY END PART  **********************************************/

    /********************************************** Alter Query  **********************************************/
    public final String ALTER_COMPANY_QUERY = "ALTER TABLE " + COMPANY_TABLE + " ADD COLUMN companyLogo VARCHAR";
    public final String ALTER_COMPANY_QR_QUERY = "ALTER TABLE " + COMPANY_TABLE + " ADD COLUMN paymentLogo VARCHAR";
    public final String ALTER_COMPANY_SHOP_NAME_1_QUERY = "ALTER TABLE " + COMPANY_TABLE + " ADD COLUMN shopName1 VARCHAR";
    public final String ALTER_COMPANY_SHOP_NAME_2_QUERY = "ALTER TABLE " + COMPANY_TABLE + " ADD COLUMN shopName2 VARCHAR";
    public final String ALTER_COMPANY_ADDRESS_LINE_1_QUERY = "ALTER TABLE " + COMPANY_TABLE + " ADD COLUMN addressLine1 VARCHAR";
    public final String ALTER_COMPANY_ADDRESS_LINE_2_QUERY = "ALTER TABLE " + COMPANY_TABLE + " ADD COLUMN addressLine2 VARCHAR";
    public final String ALTER_COMPANY_ADDRESS_LINE_3_QUERY = "ALTER TABLE " + COMPANY_TABLE + " ADD COLUMN addressLine3 VARCHAR";
    public final String ALTER_COMPANY_PHONE_NO_1_QUERY = "ALTER TABLE " + COMPANY_TABLE + " ADD COLUMN phoneNo1 VARCHAR";
    public final String ALTER_COMPANY_PHONE_NO_2_QUERY = "ALTER TABLE " + COMPANY_TABLE + " ADD COLUMN phoneNo2 VARCHAR";
    public final String ALTER_INVENTORY_QUERY = "ALTER TABLE " + INVENTORY_TABLE + " ADD COLUMN saleInventoryQuantity VARCHAR";
    public final String ALTER_PRODUCT_QUERY = "ALTER TABLE " + PRODUCT_TABLE + " ADD COLUMN productCode VARCHAR";
    public final String ALTER_CATEGORY_DELETED_QUERY = "ALTER TABLE " + PRODUCT_CATEGORY_TABLE + " ADD COLUMN categoryDeletedStatus VARCHAR";
    public final String ALTER_PRODUCT_DELETED_QUERY = "ALTER TABLE " + PRODUCT_TABLE + " ADD COLUMN productDeletedStatus VARCHAR";
    public final String ALTER_PRINTER_LOGO_SETTING_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN logoUse VARCHAR";
    public final String ALTER_PRINTER_QR_SETTING_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN paymentUse VARCHAR";
    public final String ALTER_CUSTOMER_SETTING_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN customerUse VARCHAR";
    public final String ALTER_PRINTER_BLUETOOTH_SETTING_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN bluetoothAddress VARCHAR";
    public final String ALTER_PRINTER_KOT_BLUETOOTH_SETTING_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN bluetoothKOTAddress VARCHAR";
    public final String ALTER_PRINTER_KOT_BLUETOOTH_NAME_SETTING_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN KOTPrinterName VARCHAR";
    public final String ALTER_PRINTER_PRODUCT_QUANTITY_SETTING_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN productQuantityUpdate VARCHAR";
    public final String ALTER_PRINTER_FEED_LINES_SETTING_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN printerFeedLines VARCHAR";
    public final String ALTER_KOT_PRINTER_FEED_LINES_SETTING_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN KotPrinterFeedLines VARCHAR";
    public final String ALTER_PRINTER_DUPLICATE_BILL_SETTING_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN duplicateBillUse VARCHAR";
    public final String ALTER_PRINTER_BILL_CONN_TYPE_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN billConnectionType VARCHAR";
    public final String ALTER_PRINTER_KOT_CONN_TYPE_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN kotConnectionType VARCHAR";
    public final String ALTER_PRINTER_BILL_IP_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN billPrinterIp VARCHAR";
    public final String ALTER_PRINTER_KOT_IP_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN kotPrinterIp VARCHAR";
    public final String ALTER_PRINTER_BILL_PORT_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN billPrinterPort VARCHAR";
    public final String ALTER_PRINTER_KOT_PORT_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN kotPrinterPort VARCHAR";
    public final String ALTER_PRINTER_BILL_USB_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN billUsbDeviceKey VARCHAR";
    public final String ALTER_PRINTER_KOT_USB_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN kotUsbDeviceKey VARCHAR";
    public final String ALTER_PRINTER_SUPPORTS_CUTTER_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN supportsCutter VARCHAR";
    public final String ALTER_PRINTER_SUPPORTS_DRAWER_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN supportsCashDrawer VARCHAR";
    public final String ALTER_PRINTER_AUTO_CUT_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN autoCut VARCHAR";
    public final String ALTER_PRINTER_AUTO_DRAWER_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN autoOpenCashDrawer VARCHAR";
    public final String ALTER_PRINTER_DRAWER_MODE_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN drawerOpenMode VARCHAR";
    public final String ALTER_PRINTER_DRAWER_PIN_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN drawerPin VARCHAR";
    public final String ALTER_PRINTER_DRAWER_PULSE_ON_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN drawerPulseOn VARCHAR";
    public final String ALTER_PRINTER_DRAWER_PULSE_OFF_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN drawerPulseOff VARCHAR";
    public final String ALTER_PRINTER_CUT_COMMAND_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN cutCommand VARCHAR";
    public final String ALTER_PRINTER_MODEL_QUERY = "ALTER TABLE " + PRINTER_SETTING_TABLE + " ADD COLUMN printerModel VARCHAR";
    public final String ALTER_CATEGORY_FOOD_TYPE_QUERY = "ALTER TABLE " + PRODUCT_CATEGORY_TABLE + " ADD COLUMN foodTypeId INTEGER";
    public final String ALTER_CATEGORY_SORT_ORDER_QUERY = "ALTER TABLE " + PRODUCT_CATEGORY_TABLE + " ADD COLUMN categorySortOrder INTEGER DEFAULT 0";
    public final String ALTER_SUBCATEGORY_SORT_ORDER_QUERY = "ALTER TABLE " + PRODUCT_SUBCATEGORY_TABLE + " ADD COLUMN subcategorySortOrder INTEGER DEFAULT 0";
    public final String ALTER_PRODUCT_SUBCATEGORY_QUERY = "ALTER TABLE " + PRODUCT_TABLE + " ADD COLUMN subcategoryId INTEGER";
    public final String ALTER_PRODUCT_OPEN_PRICE_QUERY = "ALTER TABLE " + PRODUCT_TABLE + " ADD COLUMN openPrice VARCHAR";
    public final String ALTER_PRODUCT_PORTION_MASTER_QUERY = "ALTER TABLE " + PRODUCT_PORTION_TABLE + " ADD COLUMN portionMasterId INTEGER";
    public final String ALTER_CART_PORTION_ID_QUERY = "ALTER TABLE " + CART_PRODUCT_TABLE + " ADD COLUMN portionId VARCHAR";
    public final String ALTER_CART_PORTION_NAME_QUERY = "ALTER TABLE " + CART_PRODUCT_TABLE + " ADD COLUMN portionName VARCHAR";
    public final String ALTER_CART_SNAPSHOT_PRODUCT_NAME_QUERY = "ALTER TABLE " + CART_PRODUCT_TABLE + " ADD COLUMN snapshotProductName VARCHAR";
    public final String ALTER_CART_SNAPSHOT_LINE_PRICE_QUERY = "ALTER TABLE " + CART_PRODUCT_TABLE + " ADD COLUMN snapshotLinePrice VARCHAR";
    public final String ALTER_INVOICE_LINE_PORTION_ID_QUERY = "ALTER TABLE " + INVOICE_PRODUCT_TABLE + " ADD COLUMN portionId VARCHAR";
    public final String ALTER_INVOICE_LINE_PORTION_NAME_QUERY = "ALTER TABLE " + INVOICE_PRODUCT_TABLE + " ADD COLUMN portionName VARCHAR";
    public final String ALTER_INVOICE_LINE_SNAPSHOT_PRODUCT_NAME_QUERY = "ALTER TABLE " + INVOICE_PRODUCT_TABLE + " ADD COLUMN snapshotProductName VARCHAR";
    public final String ALTER_INVOICE_LINE_SNAPSHOT_LINE_PRICE_QUERY = "ALTER TABLE " + INVOICE_PRODUCT_TABLE + " ADD COLUMN snapshotLinePrice VARCHAR";
    public final String ALTER_INVOICE_ORG_QUERY = "ALTER TABLE " + INVOICE_TABLE + " ADD COLUMN organizationId VARCHAR";
    public final String ALTER_INVOICE_BRANCH_QUERY = "ALTER TABLE " + INVOICE_TABLE + " ADD COLUMN branchId VARCHAR";
    public final String ALTER_INVOICE_DEVICE_QUERY = "ALTER TABLE " + INVOICE_TABLE + " ADD COLUMN deviceId VARCHAR";
    public final String ALTER_INVOICE_PRODUCT_ORG_QUERY = "ALTER TABLE " + INVOICE_PRODUCT_TABLE + " ADD COLUMN organizationId VARCHAR";
    public final String ALTER_INVOICE_PRODUCT_BRANCH_QUERY = "ALTER TABLE " + INVOICE_PRODUCT_TABLE + " ADD COLUMN branchId VARCHAR";
    public final String ALTER_INVOICE_PRODUCT_DEVICE_QUERY = "ALTER TABLE " + INVOICE_PRODUCT_TABLE + " ADD COLUMN deviceId VARCHAR";
    public final String ALTER_INVENTORY_ORG_QUERY = "ALTER TABLE " + INVENTORY_TABLE + " ADD COLUMN organizationId VARCHAR";
    public final String ALTER_INVENTORY_BRANCH_QUERY = "ALTER TABLE " + INVENTORY_TABLE + " ADD COLUMN branchId VARCHAR";
    public final String ALTER_INVENTORY_DEVICE_QUERY = "ALTER TABLE " + INVENTORY_TABLE + " ADD COLUMN deviceId VARCHAR";
    public final String ALTER_EXPENSES_ORG_QUERY = "ALTER TABLE " + EXPENSES_TABLE + " ADD COLUMN organizationId VARCHAR";
    public final String ALTER_EXPENSES_BRANCH_QUERY = "ALTER TABLE " + EXPENSES_TABLE + " ADD COLUMN branchId VARCHAR";
    public final String ALTER_EXPENSES_DEVICE_QUERY = "ALTER TABLE " + EXPENSES_TABLE + " ADD COLUMN deviceId VARCHAR";
    public final String ALTER_CART_ITEM_TYPE_QUERY = "ALTER TABLE " + CART_PRODUCT_TABLE + " ADD COLUMN cartItemType VARCHAR DEFAULT 'PRODUCT'";
    public final String ALTER_CART_COMBO_ID_QUERY = "ALTER TABLE " + CART_PRODUCT_TABLE + " ADD COLUMN comboId VARCHAR";
    public final String ALTER_CART_SNAPSHOT_COMBO_COMPONENTS_QUERY = "ALTER TABLE " + CART_PRODUCT_TABLE + " ADD COLUMN snapshotComboComponents VARCHAR";
    public final String ALTER_INVOICE_ITEM_TYPE_QUERY = "ALTER TABLE " + INVOICE_PRODUCT_TABLE + " ADD COLUMN invoiceItemType VARCHAR DEFAULT 'PRODUCT'";
    public final String ALTER_INVOICE_COMBO_ID_QUERY = "ALTER TABLE " + INVOICE_PRODUCT_TABLE + " ADD COLUMN comboId VARCHAR";
    public final String ALTER_INVOICE_SNAPSHOT_COMBO_COMPONENTS_QUERY = "ALTER TABLE " + INVOICE_PRODUCT_TABLE + " ADD COLUMN snapshotComboComponents VARCHAR";

    /********************************************** Alter Query  ***********************************************/


    public POSBillingWalaDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(FOOD_TYPE_QUERY);
        db.execSQL(PRODUCT_CATEGORY_QUERY);
        db.execSQL(PRODUCT_SUBCATEGORY_QUERY);
        db.execSQL(PORTION_MASTER_QUERY);
        db.execSQL(PRODUCT_PORTION_QUERY);
        db.execSQL(PRODUCT_QUERY);
        db.execSQL(CART_PRODUCT_QUERY);
        db.execSQL(INVOICE_QUERY);
        db.execSQL(INVOICE_PRODUCT_QUERY);
        db.execSQL(PRINTER_SETTING_QUERY);
        db.execSQL(COMPANY_QUERY);
        db.execSQL(INVENTORY_QUERY);
        db.execSQL(EXPENSES_QUERY);
        db.execSQL(MEMBER_QUERY);
        db.execSQL(MEMBER_PAYMENT_QUERY);
        db.execSQL(MESS_INVOICE_QUERY);
        db.execSQL(MESS_TOKEN_QUERY);
        db.execSQL(COMBO_QUERY);
        db.execSQL(COMBO_ITEM_QUERY);
        db.execSQL(CART_COMBO_ITEM_QUERY);
        db.execSQL(INVOICE_COMBO_ITEM_QUERY);
        db.execSQL(INVOICE_PRODUCT_DELETE_QUEUE_QUERY);
        ensureFoodTypeCatalog(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Additive only — never DROP production tables
        ensureAdditiveSchema(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            try {
                ensureAdditiveSchema(db);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Applies all known ADD COLUMN migrations and sync unique indexes.
     * Safe to run repeatedly; never drops tables or deletes customer data
     * except duplicate sync-key rows when creating unique indexes.
     */
    public void ensureAdditiveSchema(SQLiteDatabase db) {
        addColumnIfNotExists(db, COMPANY_TABLE, "companyLogo", ALTER_COMPANY_QUERY);
        addColumnIfNotExists(db, COMPANY_TABLE, "paymentLogo", ALTER_COMPANY_QR_QUERY);
        addColumnIfNotExists(db, COMPANY_TABLE, "shopName1", ALTER_COMPANY_SHOP_NAME_1_QUERY);
        addColumnIfNotExists(db, COMPANY_TABLE, "shopName2", ALTER_COMPANY_SHOP_NAME_2_QUERY);
        addColumnIfNotExists(db, COMPANY_TABLE, "addressLine1", ALTER_COMPANY_ADDRESS_LINE_1_QUERY);
        addColumnIfNotExists(db, COMPANY_TABLE, "addressLine2", ALTER_COMPANY_ADDRESS_LINE_2_QUERY);
        addColumnIfNotExists(db, COMPANY_TABLE, "addressLine3", ALTER_COMPANY_ADDRESS_LINE_3_QUERY);
        addColumnIfNotExists(db, COMPANY_TABLE, "phoneNo1", ALTER_COMPANY_PHONE_NO_1_QUERY);
        addColumnIfNotExists(db, COMPANY_TABLE, "phoneNo2", ALTER_COMPANY_PHONE_NO_2_QUERY);
        migrateCompanyStructuredFields(db);
        addColumnIfNotExists(db, INVENTORY_TABLE, "saleInventoryQuantity", ALTER_INVENTORY_QUERY);
        addColumnIfNotExists(db, PRODUCT_TABLE, "productCode", ALTER_PRODUCT_QUERY);
        addColumnIfNotExists(db, PRODUCT_CATEGORY_TABLE, "categoryDeletedStatus", ALTER_CATEGORY_DELETED_QUERY);
        addColumnIfNotExists(db, PRODUCT_TABLE, "productDeletedStatus", ALTER_PRODUCT_DELETED_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "logoUse", ALTER_PRINTER_LOGO_SETTING_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "paymentUse", ALTER_PRINTER_QR_SETTING_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "customerUse", ALTER_CUSTOMER_SETTING_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "bluetoothAddress", ALTER_PRINTER_BLUETOOTH_SETTING_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "bluetoothKOTAddress", ALTER_PRINTER_KOT_BLUETOOTH_SETTING_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "KOTPrinterName", ALTER_PRINTER_KOT_BLUETOOTH_NAME_SETTING_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "productQuantityUpdate", ALTER_PRINTER_PRODUCT_QUANTITY_SETTING_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "printerFeedLines", ALTER_PRINTER_FEED_LINES_SETTING_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "KotPrinterFeedLines", ALTER_KOT_PRINTER_FEED_LINES_SETTING_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "duplicateBillUse", ALTER_PRINTER_DUPLICATE_BILL_SETTING_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "billConnectionType", ALTER_PRINTER_BILL_CONN_TYPE_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "kotConnectionType", ALTER_PRINTER_KOT_CONN_TYPE_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "billPrinterIp", ALTER_PRINTER_BILL_IP_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "kotPrinterIp", ALTER_PRINTER_KOT_IP_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "billPrinterPort", ALTER_PRINTER_BILL_PORT_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "kotPrinterPort", ALTER_PRINTER_KOT_PORT_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "billUsbDeviceKey", ALTER_PRINTER_BILL_USB_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "kotUsbDeviceKey", ALTER_PRINTER_KOT_USB_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "supportsCutter", ALTER_PRINTER_SUPPORTS_CUTTER_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "supportsCashDrawer", ALTER_PRINTER_SUPPORTS_DRAWER_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "autoCut", ALTER_PRINTER_AUTO_CUT_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "autoOpenCashDrawer", ALTER_PRINTER_AUTO_DRAWER_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "drawerOpenMode", ALTER_PRINTER_DRAWER_MODE_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "drawerPin", ALTER_PRINTER_DRAWER_PIN_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "drawerPulseOn", ALTER_PRINTER_DRAWER_PULSE_ON_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "drawerPulseOff", ALTER_PRINTER_DRAWER_PULSE_OFF_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "cutCommand", ALTER_PRINTER_CUT_COMMAND_QUERY);
        addColumnIfNotExists(db, PRINTER_SETTING_TABLE, "printerModel", ALTER_PRINTER_MODEL_QUERY);
        // Phase 3 catalog foundation (additive only)
        db.execSQL(FOOD_TYPE_QUERY);
        db.execSQL(PRODUCT_SUBCATEGORY_QUERY);
        db.execSQL(PORTION_MASTER_QUERY);
        db.execSQL(PRODUCT_PORTION_QUERY);
        addColumnIfNotExists(db, PRODUCT_CATEGORY_TABLE, "foodTypeId", ALTER_CATEGORY_FOOD_TYPE_QUERY);
        addColumnIfNotExists(db, PRODUCT_CATEGORY_TABLE, "categorySortOrder", ALTER_CATEGORY_SORT_ORDER_QUERY);
        addColumnIfNotExists(db, PRODUCT_SUBCATEGORY_TABLE, "subcategorySortOrder", ALTER_SUBCATEGORY_SORT_ORDER_QUERY);
        backfillCategorySortOrders(db);
        backfillSubcategorySortOrders(db);
        addColumnIfNotExists(db, PRODUCT_TABLE, "subcategoryId", ALTER_PRODUCT_SUBCATEGORY_QUERY);
        addColumnIfNotExists(db, PRODUCT_TABLE, "openPrice", ALTER_PRODUCT_OPEN_PRICE_QUERY);
        addColumnIfNotExists(db, PRODUCT_PORTION_TABLE, "portionMasterId", ALTER_PRODUCT_PORTION_MASTER_QUERY);
        migrateProductPortionsToPortionMaster(db);
        addColumnIfNotExists(db, CART_PRODUCT_TABLE, "portionId", ALTER_CART_PORTION_ID_QUERY);
        addColumnIfNotExists(db, CART_PRODUCT_TABLE, "portionName", ALTER_CART_PORTION_NAME_QUERY);
        addColumnIfNotExists(db, CART_PRODUCT_TABLE, "snapshotProductName", ALTER_CART_SNAPSHOT_PRODUCT_NAME_QUERY);
        addColumnIfNotExists(db, CART_PRODUCT_TABLE, "snapshotLinePrice", ALTER_CART_SNAPSHOT_LINE_PRICE_QUERY);
        addColumnIfNotExists(db, INVOICE_PRODUCT_TABLE, "portionId", ALTER_INVOICE_LINE_PORTION_ID_QUERY);
        addColumnIfNotExists(db, INVOICE_PRODUCT_TABLE, "portionName", ALTER_INVOICE_LINE_PORTION_NAME_QUERY);
        addColumnIfNotExists(db, INVOICE_PRODUCT_TABLE, "snapshotProductName", ALTER_INVOICE_LINE_SNAPSHOT_PRODUCT_NAME_QUERY);
        addColumnIfNotExists(db, INVOICE_PRODUCT_TABLE, "snapshotLinePrice", ALTER_INVOICE_LINE_SNAPSHOT_LINE_PRICE_QUERY);
        addColumnIfNotExists(db, INVOICE_TABLE, "organizationId", ALTER_INVOICE_ORG_QUERY);
        addColumnIfNotExists(db, INVOICE_TABLE, "branchId", ALTER_INVOICE_BRANCH_QUERY);
        addColumnIfNotExists(db, INVOICE_TABLE, "deviceId", ALTER_INVOICE_DEVICE_QUERY);
        addColumnIfNotExists(db, INVOICE_PRODUCT_TABLE, "organizationId", ALTER_INVOICE_PRODUCT_ORG_QUERY);
        addColumnIfNotExists(db, INVOICE_PRODUCT_TABLE, "branchId", ALTER_INVOICE_PRODUCT_BRANCH_QUERY);
        addColumnIfNotExists(db, INVOICE_PRODUCT_TABLE, "deviceId", ALTER_INVOICE_PRODUCT_DEVICE_QUERY);
        addColumnIfNotExists(db, INVENTORY_TABLE, "organizationId", ALTER_INVENTORY_ORG_QUERY);
        addColumnIfNotExists(db, INVENTORY_TABLE, "branchId", ALTER_INVENTORY_BRANCH_QUERY);
        addColumnIfNotExists(db, INVENTORY_TABLE, "deviceId", ALTER_INVENTORY_DEVICE_QUERY);
        addColumnIfNotExists(db, EXPENSES_TABLE, "organizationId", ALTER_EXPENSES_ORG_QUERY);
        addColumnIfNotExists(db, EXPENSES_TABLE, "branchId", ALTER_EXPENSES_BRANCH_QUERY);
        addColumnIfNotExists(db, EXPENSES_TABLE, "deviceId", ALTER_EXPENSES_DEVICE_QUERY);
        ensureFoodTypeCatalog(db);
        db.execSQL(MESS_TOKEN_QUERY);
        db.execSQL(COMBO_QUERY);
        db.execSQL(COMBO_ITEM_QUERY);
        db.execSQL(CART_COMBO_ITEM_QUERY);
        db.execSQL(INVOICE_COMBO_ITEM_QUERY);
        addColumnIfNotExists(db, CART_PRODUCT_TABLE, "cartItemType", ALTER_CART_ITEM_TYPE_QUERY);
        addColumnIfNotExists(db, CART_PRODUCT_TABLE, "comboId", ALTER_CART_COMBO_ID_QUERY);
        addColumnIfNotExists(db, CART_PRODUCT_TABLE, "snapshotComboComponents", ALTER_CART_SNAPSHOT_COMBO_COMPONENTS_QUERY);
        addColumnIfNotExists(db, INVOICE_PRODUCT_TABLE, "invoiceItemType", ALTER_INVOICE_ITEM_TYPE_QUERY);
        addColumnIfNotExists(db, INVOICE_PRODUCT_TABLE, "comboId", ALTER_INVOICE_COMBO_ID_QUERY);
        addColumnIfNotExists(db, INVOICE_PRODUCT_TABLE, "snapshotComboComponents", ALTER_INVOICE_SNAPSHOT_COMBO_COMPONENTS_QUERY);
        db.execSQL(INVOICE_PRODUCT_DELETE_QUEUE_QUERY);
        ensureUniqueSyncIndexes(db);
    }

    /**
     * Production-safe: dedupe by sync key then create UNIQUE indexes (no DROP TABLE).
     * Prevents local duplicate bills/lines when cloud pull or sync retries re-insert.
     */
    public void ensureUniqueSyncIndexes(SQLiteDatabase db) {
        try {
            dedupeByNetworkStatus(db, INVOICE_TABLE, "invoiceId", "invoiceNetworkStatus");
            dedupeByNetworkStatus(db, INVOICE_PRODUCT_TABLE, "invoiceProductId", "invoiceProductNetworkStatus");
            dedupeByNetworkStatus(db, PRODUCT_PORTION_TABLE, "portionId", "portionNetworkStatus");
            dedupeByNetworkStatus(db, PORTION_MASTER_TABLE, "portionMasterId", "portionMasterNetworkStatus");
            dedupeProductPortionByMaster(db);

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_invoice_network_status ON "
                    + INVOICE_TABLE + "(invoiceNetworkStatus) "
                    + "WHERE invoiceNetworkStatus IS NOT NULL AND invoiceNetworkStatus != ''");

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_invoice_product_network_status ON "
                    + INVOICE_PRODUCT_TABLE + "(invoiceProductNetworkStatus) "
                    + "WHERE invoiceProductNetworkStatus IS NOT NULL AND invoiceProductNetworkStatus != ''");

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_portion_network_status ON "
                    + PRODUCT_PORTION_TABLE + "(portionNetworkStatus) "
                    + "WHERE portionNetworkStatus IS NOT NULL AND portionNetworkStatus != ''");

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_portion_master_network_status ON "
                    + PORTION_MASTER_TABLE + "(portionMasterNetworkStatus) "
                    + "WHERE portionMasterNetworkStatus IS NOT NULL AND portionMasterNetworkStatus != ''");

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_product_portion_master ON "
                    + PRODUCT_PORTION_TABLE + "(productId, portionMasterId) "
                    + "WHERE portionMasterId IS NOT NULL AND portionMasterId > 0 "
                    + "AND portionDeletedStatus = '0'");

            // Billing search indexes (additive; LIKE with leading % still scans, but filters/sorts improve)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_product_name ON " + PRODUCT_TABLE + "(productName)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_product_code ON " + PRODUCT_TABLE + "(productCode)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_product_deleted_status ON " + PRODUCT_TABLE + "(productDeletedStatus)");

            // Report paging / date-order queries
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoice_date ON " + INVOICE_TABLE + "(invoiceDate)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoice_type ON " + INVOICE_TABLE + "(invoiceType)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoice_payment_mode ON " + INVOICE_TABLE + "(paymentMode)");

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_food_type_code ON " + FOOD_TYPE_TABLE + "(foodTypeCode)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_category_food_type ON " + PRODUCT_CATEGORY_TABLE + "(foodTypeId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_subcategory_category ON " + PRODUCT_SUBCATEGORY_TABLE + "(categoryId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_product_subcategory ON " + PRODUCT_TABLE + "(subcategoryId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_portion_product ON " + PRODUCT_PORTION_TABLE + "(productId)");

            dedupeByNetworkStatus(db, COMBO_TABLE, "comboId", "comboNetworkStatus");
            dedupeByNetworkStatus(db, COMBO_ITEM_TABLE, "comboItemId", "comboItemNetworkStatus");
            dedupeByNetworkStatus(db, INVOICE_COMBO_ITEM_TABLE, "invoiceComboItemId", "invoiceComboItemNetworkStatus");

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_combo_network_status ON "
                    + COMBO_TABLE + "(comboNetworkStatus) "
                    + "WHERE comboNetworkStatus IS NOT NULL AND comboNetworkStatus != ''");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_combo_item_network_status ON "
                    + COMBO_ITEM_TABLE + "(comboItemNetworkStatus) "
                    + "WHERE comboItemNetworkStatus IS NOT NULL AND comboItemNetworkStatus != ''");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_combo_item_combo ON " + COMBO_ITEM_TABLE + "(comboId)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_combo_item_product_portion ON "
                    + COMBO_ITEM_TABLE + "(comboId, productId, IFNULL(portionId, '')) "
                    + "WHERE comboItemDeletedStatus = '0'");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_combo_deleted_status ON " + COMBO_TABLE + "(comboDeletedStatus)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_combo_code ON " + COMBO_TABLE + "(comboCode)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_combo_name ON " + COMBO_TABLE + "(comboName)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_cart_combo_item_cart ON " + CART_COMBO_ITEM_TABLE + "(cartId)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_invoice_combo_item_network_status ON "
                    + INVOICE_COMBO_ITEM_TABLE + "(invoiceComboItemNetworkStatus) "
                    + "WHERE invoiceComboItemNetworkStatus IS NOT NULL AND invoiceComboItemNetworkStatus != ''");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoice_combo_item_invoice ON "
                    + INVOICE_COMBO_ITEM_TABLE + "(invoiceNumber)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoice_combo_item_parent ON "
                    + INVOICE_COMBO_ITEM_TABLE + "(invoiceProductNetworkStatus)");
        } catch (Exception e) {
            // Index may fail if unexpected duplicates remain; upsert helpers still protect inserts
            e.printStackTrace();
        }
    }

    /**
     * Seeds Food + Beverage types and maps legacy categories with no foodTypeId to Food.
     * Idempotent — safe on every open/upgrade.
     */
    public void ensureFoodTypeCatalog(SQLiteDatabase db) {
        try {
            insertFoodTypeIfMissing(db, "Food", FoodTypeResponse.CODE_FOOD, 1);
            insertFoodTypeIfMissing(db, "Beverage", FoodTypeResponse.CODE_BEVERAGE, 2);

            long foodId = getFoodTypeIdByCode(db, FoodTypeResponse.CODE_FOOD);
            if (foodId > 0) {
                db.execSQL(
                        "UPDATE " + PRODUCT_CATEGORY_TABLE
                                + " SET foodTypeId = " + foodId
                                + " WHERE foodTypeId IS NULL");
            }
            mapLikelyBeverageCategories(db);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Assign stable sort values for legacy rows that still have 0. */
    private void backfillCategorySortOrders(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT categoryId FROM " + PRODUCT_CATEGORY_TABLE
                            + " WHERE IFNULL(categorySortOrder, 0) = 0"
                            + " ORDER BY categoryId ASC",
                    null);
            int order = 1;
            Cursor maxCursor = db.rawQuery(
                    "SELECT IFNULL(MAX(categorySortOrder), 0) FROM " + PRODUCT_CATEGORY_TABLE, null);
            if (maxCursor.moveToFirst()) {
                order = Math.max(1, maxCursor.getInt(0) + 1);
            }
            maxCursor.close();
            while (cursor.moveToNext()) {
                ContentValues values = new ContentValues();
                values.put("categorySortOrder", order++);
                db.update(PRODUCT_CATEGORY_TABLE, values, "categoryId=?",
                        new String[]{cursor.getString(0)});
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void backfillSubcategorySortOrders(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT subcategoryId, categoryId FROM " + PRODUCT_SUBCATEGORY_TABLE
                            + " WHERE IFNULL(subcategorySortOrder, 0) = 0"
                            + " ORDER BY categoryId ASC, subcategoryId ASC",
                    null);
            String lastCategoryId = null;
            int order = 1;
            while (cursor.moveToNext()) {
                String categoryId = cursor.getString(1);
                if (lastCategoryId == null || !lastCategoryId.equals(categoryId)) {
                    lastCategoryId = categoryId;
                    order = 1;
                    Cursor maxCursor = db.rawQuery(
                            "SELECT IFNULL(MAX(subcategorySortOrder), 0) FROM " + PRODUCT_SUBCATEGORY_TABLE
                                    + " WHERE categoryId = ?",
                            new String[]{categoryId != null ? categoryId : ""});
                    if (maxCursor.moveToFirst()) {
                        order = Math.max(1, maxCursor.getInt(0) + 1);
                    }
                    maxCursor.close();
                }
                ContentValues values = new ContentValues();
                values.put("subcategorySortOrder", order++);
                db.update(PRODUCT_SUBCATEGORY_TABLE, values, "subcategoryId=?",
                        new String[]{cursor.getString(0)});
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Maps common beverage category names from legacy Food default to Beverage.
     * Only touches rows still on the default Food type — never overwrites explicit Beverage assignments.
     */
    private void mapLikelyBeverageCategories(SQLiteDatabase db) {
        long beverageId = getFoodTypeIdByCode(db, FoodTypeResponse.CODE_BEVERAGE);
        long foodId = getFoodTypeIdByCode(db, FoodTypeResponse.CODE_FOOD);
        if (beverageId <= 0 || foodId <= 0) {
            return;
        }
        db.execSQL(
                "UPDATE " + PRODUCT_CATEGORY_TABLE
                        + " SET foodTypeId = " + beverageId
                        + " WHERE foodTypeId = " + foodId
                        + " AND categoryDeletedStatus = '0'"
                        + " AND ("
                        + " LOWER(categoryName) LIKE '%beverage%'"
                        + " OR LOWER(categoryName) LIKE '%drink%'"
                        + " OR LOWER(categoryName) LIKE '%juice%'"
                        + " OR LOWER(categoryName) LIKE '%mocktail%'"
                        + " OR LOWER(categoryName) LIKE '%cocktail%'"
                        + " OR LOWER(categoryName) LIKE '%tea%'"
                        + " OR LOWER(categoryName) LIKE '%coffee%'"
                        + " OR LOWER(categoryName) LIKE '%shake%'"
                        + " OR LOWER(categoryName) LIKE '%lassi%'"
                        + " OR LOWER(categoryName) LIKE '%soda%'"
                        + " OR LOWER(categoryName) LIKE '%soft%'"
                        + " OR LOWER(categoryName) LIKE '%cold%'"
                        + " OR LOWER(categoryName) LIKE '%water%'"
                        + " OR LOWER(categoryName) LIKE '%milk%'"
                        + ")");
    }

    public String getFoodTypeNameById(long foodTypeId) {
        if (foodTypeId <= 0) {
            return "";
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT foodTypeName FROM " + FOOD_TYPE_TABLE + " WHERE foodTypeId = ? LIMIT 1",
                    new String[]{String.valueOf(foodTypeId)});
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }

    private void insertFoodTypeIfMissing(SQLiteDatabase db, String name, String code, int sortOrder) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT foodTypeId FROM " + FOOD_TYPE_TABLE + " WHERE foodTypeCode = ? LIMIT 1",
                    new String[]{code});
            if (cursor.moveToFirst()) {
                return;
            }
            ContentValues values = new ContentValues();
            values.put("foodTypeName", name);
            values.put("foodTypeCode", code);
            values.put("foodTypeSortOrder", sortOrder);
            values.put("foodTypeStatus", 1);
            db.insert(FOOD_TYPE_TABLE, null, values);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public long getFoodTypeIdByCode(String code) {
        SQLiteDatabase db = this.getReadableDatabase();
        return getFoodTypeIdByCode(db, code);
    }

    private long getFoodTypeIdByCode(SQLiteDatabase db, String code) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT foodTypeId FROM " + FOOD_TYPE_TABLE + " WHERE foodTypeCode = ? LIMIT 1",
                    new String[]{code});
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public long getDefaultFoodTypeId() {
        long foodId = getFoodTypeIdByCode(FoodTypeResponse.CODE_FOOD);
        if (foodId > 0) {
            return foodId;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ensureFoodTypeCatalog(db);
        return getFoodTypeIdByCode(db, FoodTypeResponse.CODE_FOOD);
    }

    public String getFoodTypeCodeById(long foodTypeId) {
        if (foodTypeId <= 0) {
            return "";
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT foodTypeCode FROM " + FOOD_TYPE_TABLE + " WHERE foodTypeId = ? LIMIT 1",
                    new String[]{String.valueOf(foodTypeId)});
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }

    public List<FoodTypeResponse> getFoodTypeList() {
        List<FoodTypeResponse> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + FOOD_TYPE_TABLE
                            + " WHERE foodTypeStatus = 1 ORDER BY foodTypeSortOrder ASC, foodTypeId ASC",
                    null);
            while (cursor.moveToNext()) {
                FoodTypeResponse item = new FoodTypeResponse();
                item.setFoodTypeId(cursor.getString(cursor.getColumnIndex("foodTypeId")));
                item.setFoodTypeName(cursor.getString(cursor.getColumnIndex("foodTypeName")));
                item.setFoodTypeCode(cursor.getString(cursor.getColumnIndex("foodTypeCode")));
                item.setFoodTypeSortOrder(cursor.getString(cursor.getColumnIndex("foodTypeSortOrder")));
                item.setFoodTypeStatus(cursor.getString(cursor.getColumnIndex("foodTypeStatus")));
                list.add(item);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    public List<ProductCategoryResponse> getCategoryListByFoodType(String foodTypeId) {
        List<ProductCategoryResponse> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql;
            String[] args;
            if (foodTypeId == null || foodTypeId.trim().isEmpty()) {
                sql = "SELECT * FROM " + PRODUCT_CATEGORY_TABLE
                        + " WHERE categoryDeletedStatus = '0'"
                        + " ORDER BY IFNULL(categorySortOrder, 0) ASC, categoryId ASC";
                args = null;
            } else {
                sql = "SELECT * FROM " + PRODUCT_CATEGORY_TABLE
                        + " WHERE categoryDeletedStatus = '0' AND foodTypeId = ?"
                        + " ORDER BY IFNULL(categorySortOrder, 0) ASC, categoryId ASC";
                args = new String[]{foodTypeId};
            }
            cursor = db.rawQuery(sql, args);
            while (cursor.moveToNext()) {
                ProductCategoryResponse productCategoryResponse = new ProductCategoryResponse();
                productCategoryResponse.setCategoryId(cursor.getString(cursor.getColumnIndex("categoryId")));
                productCategoryResponse.setCategoryName(cursor.getString(cursor.getColumnIndex("categoryName")));
                productCategoryResponse.setCategoryDeletedStatus(cursor.getString(cursor.getColumnIndex("categoryDeletedStatus")));
                productCategoryResponse.setCategoryNetworkStatus(cursor.getString(cursor.getColumnIndex("categoryNetworkStatus")));
                productCategoryResponse.setFoodTypeId(cursor.getString(cursor.getColumnIndex("foodTypeId")));
                int sortCol = cursor.getColumnIndex("categorySortOrder");
                if (sortCol >= 0 && !cursor.isNull(sortCol)) {
                    productCategoryResponse.setCategorySortOrder(cursor.getString(sortCol));
                }
                list.add(productCategoryResponse);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    private void dedupeByNetworkStatus(SQLiteDatabase db, String table, String idColumn, String networkColumn) {
        db.execSQL(
                "DELETE FROM " + table + " WHERE " + idColumn + " IN ("
                        + "SELECT t." + idColumn + " FROM " + table + " t "
                        + "INNER JOIN ("
                        + "  SELECT " + networkColumn + " AS netKey, MIN(" + idColumn + ") AS keepId "
                        + "  FROM " + table + " "
                        + "  WHERE " + networkColumn + " IS NOT NULL AND " + networkColumn + " != '' "
                        + "  GROUP BY " + networkColumn + " HAVING COUNT(*) > 1"
                        + ") d ON t." + networkColumn + " = d.netKey AND t." + idColumn + " != d.keepId"
                        + ")"
        );
    }

    private void dedupeProductPortionByMaster(SQLiteDatabase db) {
        try {
            db.execSQL(
                    "DELETE FROM " + PRODUCT_PORTION_TABLE + " WHERE portionId IN ("
                            + "SELECT t.portionId FROM " + PRODUCT_PORTION_TABLE + " t "
                            + "INNER JOIN ("
                            + "  SELECT productId, portionMasterId, MIN(portionId) AS keepId "
                            + "  FROM " + PRODUCT_PORTION_TABLE + " "
                            + "  WHERE portionMasterId IS NOT NULL AND portionMasterId > 0 "
                            + "  AND portionDeletedStatus = '0' "
                            + "  GROUP BY productId, portionMasterId HAVING COUNT(*) > 1"
                            + ") d ON t.productId = d.productId AND t.portionMasterId = d.portionMasterId "
                            + "AND t.portionId != d.keepId"
                            + ")"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * One-time migration: create portion_master rows from distinct product_portion names and link them.
     */
    private void migrateProductPortionsToPortionMaster(SQLiteDatabase db) {
        try {
            Cursor names = null;
            try {
                names = db.rawQuery(
                        "SELECT DISTINCT TRIM(portionName) AS pname FROM " + PRODUCT_PORTION_TABLE
                                + " WHERE portionName IS NOT NULL AND TRIM(portionName) != ''",
                        null);
                while (names.moveToNext()) {
                    String name = names.getString(0);
                    if (name != null && !name.trim().isEmpty()) {
                        ensureLocalPortionMasterId(db, name.trim(), null);
                    }
                }
            } finally {
                if (names != null) {
                    names.close();
                }
            }

            Cursor unlinked = null;
            try {
                unlinked = db.rawQuery(
                        "SELECT portionId, portionName FROM " + PRODUCT_PORTION_TABLE
                                + " WHERE portionMasterId IS NULL OR portionMasterId = 0",
                        null);
                while (unlinked.moveToNext()) {
                    String portionId = unlinked.getString(unlinked.getColumnIndex("portionId"));
                    String name = unlinked.getString(unlinked.getColumnIndex("portionName"));
                    if (name == null || name.trim().isEmpty()) {
                        continue;
                    }
                    long masterId = ensureLocalPortionMasterId(db, name.trim(), null);
                    if (masterId > 0) {
                        ContentValues values = new ContentValues();
                        values.put("portionMasterId", masterId);
                        db.update(PRODUCT_PORTION_TABLE, values, "portionId=?", new String[]{portionId});
                    }
                }
            } finally {
                if (unlinked != null) {
                    unlinked.close();
                }
            }
            dedupeProductPortionByMaster(db);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long ensureLocalPortionMasterId(SQLiteDatabase db, String portionName, String networkStatus) {
        if (portionName == null || portionName.trim().isEmpty()) {
            return -1;
        }
        Cursor byName = null;
        try {
            byName = db.rawQuery(
                    "SELECT portionMasterId FROM " + PORTION_MASTER_TABLE
                            + " WHERE LOWER(TRIM(portionName)) = LOWER(?) AND portionMasterDeletedStatus = '0' LIMIT 1",
                    new String[]{portionName.trim()});
            if (byName.moveToFirst()) {
                return byName.getLong(0);
            }
        } finally {
            if (byName != null) {
                byName.close();
            }
        }
        if (networkStatus != null && !networkStatus.trim().isEmpty()) {
            Cursor byNet = null;
            try {
                byNet = db.rawQuery(
                        "SELECT portionMasterId FROM " + PORTION_MASTER_TABLE
                                + " WHERE portionMasterNetworkStatus = ? LIMIT 1",
                        new String[]{networkStatus});
                if (byNet.moveToFirst()) {
                    return byNet.getLong(0);
                }
            } finally {
                if (byNet != null) {
                    byNet.close();
                }
            }
        }
        ContentValues values = new ContentValues();
        values.put("portionName", portionName.trim());
        values.put("portionMasterDeletedStatus", "0");
        if (networkStatus != null && !networkStatus.trim().isEmpty()) {
            values.put("portionMasterNetworkStatus", networkStatus);
        } else {
            values.put("portionMasterNetworkStatus", generateLocalNetworkKey());
        }
        values.put("portionMasterStatus",
                (networkStatus == null || networkStatus.trim().isEmpty()) ? 0 : 1);
        return db.insert(PORTION_MASTER_TABLE, null, values);
    }

    private String generateLocalNetworkKey() {
        String chars = "0123456789qwertyuiopasdfghjklzxcvbnm";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public boolean invoiceNetworkStatusExists(String invoiceNetworkStatus) {
        if (invoiceNetworkStatus == null || invoiceNetworkStatus.trim().isEmpty()) {
            return false;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT 1 FROM " + INVOICE_TABLE + " WHERE invoiceNetworkStatus = ? LIMIT 1",
                    new String[]{invoiceNetworkStatus});
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean invoiceProductNetworkStatusExists(String invoiceProductNetworkStatus) {
        if (invoiceProductNetworkStatus == null || invoiceProductNetworkStatus.trim().isEmpty()) {
            return false;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT 1 FROM " + INVOICE_PRODUCT_TABLE + " WHERE invoiceProductNetworkStatus = ? LIMIT 1",
                    new String[]{invoiceProductNetworkStatus});
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * One-time safe copy of legacy store fields into structured columns.
     * Never overwrites non-empty structured values. Never drops legacy columns.
     */
    public void migrateCompanyStructuredFields(SQLiteDatabase db) {
        try {
            db.execSQL("UPDATE " + COMPANY_TABLE
                    + " SET shopName1 = companyName"
                    + " WHERE (shopName1 IS NULL OR TRIM(shopName1) = '')"
                    + " AND companyName IS NOT NULL AND TRIM(companyName) != ''");
            db.execSQL("UPDATE " + COMPANY_TABLE
                    + " SET addressLine1 = companyAddress"
                    + " WHERE (addressLine1 IS NULL OR TRIM(addressLine1) = '')"
                    + " AND (addressLine2 IS NULL OR TRIM(addressLine2) = '')"
                    + " AND (addressLine3 IS NULL OR TRIM(addressLine3) = '')"
                    + " AND companyAddress IS NOT NULL AND TRIM(companyAddress) != ''");
            db.execSQL("UPDATE " + COMPANY_TABLE
                    + " SET phoneNo1 = companyMobile"
                    + " WHERE (phoneNo1 IS NULL OR TRIM(phoneNo1) = '')"
                    + " AND companyMobile IS NOT NULL AND TRIM(companyMobile) != ''");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addColumnIfNotExists(String tableName, String columnName, String query) {
        SQLiteDatabase db = this.getWritableDatabase();
        addColumnIfNotExists(db, tableName, columnName, query);
    }

    public void addColumnIfNotExists(SQLiteDatabase db, String tableName, String columnName, String query) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            boolean columnExists = false;
            while (cursor.moveToNext()) {
                String existingColumnName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                if (existingColumnName.equals(columnName)) {
                    columnExists = true;
                    break;
                }
            }
            if (!columnExists) {
                db.execSQL(query);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void upGradeDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ensureAdditiveSchema(db);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean insertProductCategory(String categoryName, int categoryStatus, String categoryDeletedStatus, String categoryNetworkStatus) {
        return insertProductCategory(categoryName, categoryStatus, categoryDeletedStatus, categoryNetworkStatus, 0, -1);
    }

    public boolean insertProductCategory(String categoryName, int categoryStatus, String categoryDeletedStatus,
                                         String categoryNetworkStatus, long foodTypeId) {
        return insertProductCategory(categoryName, categoryStatus, categoryDeletedStatus, categoryNetworkStatus, foodTypeId, -1);
    }

    public boolean insertProductCategory(String categoryName, int categoryStatus, String categoryDeletedStatus,
                                         String categoryNetworkStatus, long foodTypeId, int categorySortOrder) {

        SQLiteDatabase db = this.getWritableDatabase();
        if (categoryNetworkStatus != null && !categoryNetworkStatus.trim().isEmpty()) {
            Cursor existing = null;
            try {
                existing = db.rawQuery(
                        "SELECT categoryId FROM " + PRODUCT_CATEGORY_TABLE
                                + " WHERE categoryNetworkStatus = ? LIMIT 1",
                        new String[]{categoryNetworkStatus});
                if (existing.moveToFirst()) {
                    String existingId = existing.getString(0);
                    ContentValues update = new ContentValues();
                    update.put("categoryName", categoryName);
                    update.put("categoryDeletedStatus", categoryDeletedStatus);
                    if (foodTypeId > 0) {
                        update.put("foodTypeId", foodTypeId);
                    }
                    if (categorySortOrder >= 0) {
                        Cursor statusCursor = null;
                        try {
                            statusCursor = db.rawQuery(
                                    "SELECT categoryStatus FROM " + PRODUCT_CATEGORY_TABLE
                                            + " WHERE categoryId = ? LIMIT 1",
                                    new String[]{existingId});
                            boolean dirty = false;
                            if (statusCursor.moveToFirst()) {
                                String status = statusCursor.getString(0);
                                dirty = "0".equals(status);
                            }
                            if (!dirty) {
                                update.put("categorySortOrder", categorySortOrder);
                            }
                        } finally {
                            if (statusCursor != null) {
                                statusCursor.close();
                            }
                        }
                    }
                    db.update(PRODUCT_CATEGORY_TABLE, update, "categoryId=?",
                            new String[]{existingId});
                    db.close();
                    return false;
                }
            } finally {
                if (existing != null) {
                    existing.close();
                }
            }
        }

        ContentValues contentValues = new ContentValues();

        if (foodTypeId <= 0) {
            foodTypeId = getDefaultFoodTypeId();
        }

        int sortOrder = categorySortOrder;
        if (sortOrder < 0) {
            sortOrder = getNextCategorySortOrder(db);
        }

        contentValues.put("categoryName", categoryName);
        contentValues.put("categoryStatus", categoryStatus);
        contentValues.put("categoryDeletedStatus", categoryDeletedStatus);
        contentValues.put("categoryNetworkStatus", categoryNetworkStatus);
        contentValues.put("foodTypeId", foodTypeId);
        contentValues.put("categorySortOrder", sortOrder);

        db.insert(PRODUCT_CATEGORY_TABLE, null, contentValues);
        db.close();

        return true;

    }

    private int getNextCategorySortOrder(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT IFNULL(MAX(categorySortOrder), 0) FROM " + PRODUCT_CATEGORY_TABLE, null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) + 1;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 1;
    }

    public boolean insertProductSubcategory(String categoryId, String subcategoryName,
                                            String subcategoryDeletedStatus, String subcategoryNetworkStatus,
                                            int subcategoryStatus) {
        return insertProductSubcategory(categoryId, subcategoryName, subcategoryDeletedStatus,
                subcategoryNetworkStatus, subcategoryStatus, -1);
    }

    public boolean insertProductSubcategory(String categoryId, String subcategoryName,
                                            String subcategoryDeletedStatus, String subcategoryNetworkStatus,
                                            int subcategoryStatus, int subcategorySortOrder) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (subcategoryNetworkStatus != null && !subcategoryNetworkStatus.trim().isEmpty()) {
            Cursor existing = null;
            try {
                existing = db.rawQuery(
                        "SELECT subcategoryId FROM " + PRODUCT_SUBCATEGORY_TABLE
                                + " WHERE subcategoryNetworkStatus = ? LIMIT 1",
                        new String[]{subcategoryNetworkStatus});
                if (existing.moveToFirst()) {
                    String existingId = existing.getString(0);
                    ContentValues update = new ContentValues();
                    update.put("categoryId", categoryId);
                    update.put("subcategoryName", subcategoryName);
                    update.put("subcategoryDeletedStatus",
                            subcategoryDeletedStatus != null ? subcategoryDeletedStatus : "0");
                    if (subcategorySortOrder >= 0) {
                        Cursor statusCursor = null;
                        try {
                            statusCursor = db.rawQuery(
                                    "SELECT subcategoryStatus FROM " + PRODUCT_SUBCATEGORY_TABLE
                                            + " WHERE subcategoryId = ? LIMIT 1",
                                    new String[]{existingId});
                            boolean dirty = false;
                            if (statusCursor.moveToFirst()) {
                                String status = statusCursor.getString(0);
                                dirty = "0".equals(status);
                            }
                            if (!dirty) {
                                update.put("subcategorySortOrder", subcategorySortOrder);
                            }
                        } finally {
                            if (statusCursor != null) {
                                statusCursor.close();
                            }
                        }
                    }
                    db.update(PRODUCT_SUBCATEGORY_TABLE, update, "subcategoryId=?",
                            new String[]{existingId});
                    db.close();
                    return false;
                }
            } finally {
                if (existing != null) {
                    existing.close();
                }
            }
        }
        ContentValues values = new ContentValues();
        values.put("categoryId", categoryId);
        values.put("subcategoryName", subcategoryName);
        values.put("subcategoryDeletedStatus",
                subcategoryDeletedStatus != null ? subcategoryDeletedStatus : "0");
        values.put("subcategoryNetworkStatus", subcategoryNetworkStatus);
        values.put("subcategoryStatus", subcategoryStatus);
        int sortOrder = subcategorySortOrder;
        if (sortOrder < 0) {
            sortOrder = getNextSubcategorySortOrder(db, categoryId);
        }
        values.put("subcategorySortOrder", sortOrder);
        long rowId = db.insert(PRODUCT_SUBCATEGORY_TABLE, null, values);
        db.close();
        return rowId != -1;
    }

    private int getNextSubcategorySortOrder(SQLiteDatabase db, String categoryId) {
        Cursor cursor = null;
        try {
            if (categoryId == null || categoryId.trim().isEmpty()) {
                cursor = db.rawQuery(
                        "SELECT IFNULL(MAX(subcategorySortOrder), 0) FROM " + PRODUCT_SUBCATEGORY_TABLE, null);
            } else {
                cursor = db.rawQuery(
                        "SELECT IFNULL(MAX(subcategorySortOrder), 0) FROM " + PRODUCT_SUBCATEGORY_TABLE
                                + " WHERE categoryId = ?",
                        new String[]{categoryId});
            }
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) + 1;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 1;
    }

    public void updateCategorySortOrders(List<String> categoryIdsInOrder) {
        if (categoryIdsInOrder == null || categoryIdsInOrder.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            int order = 1;
            for (String categoryId : categoryIdsInOrder) {
                if (categoryId == null || categoryId.trim().isEmpty() || "ALL".equals(categoryId)) {
                    continue;
                }
                ContentValues values = new ContentValues();
                values.put("categorySortOrder", order++);
                values.put("categoryStatus", 0);
                db.update(PRODUCT_CATEGORY_TABLE, values, "categoryId=?", new String[]{categoryId});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void updateSubcategorySortOrders(List<String> subcategoryIdsInOrder) {
        if (subcategoryIdsInOrder == null || subcategoryIdsInOrder.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            int order = 1;
            for (String subcategoryId : subcategoryIdsInOrder) {
                if (subcategoryId == null || subcategoryId.trim().isEmpty()) {
                    continue;
                }
                ContentValues values = new ContentValues();
                values.put("subcategorySortOrder", order++);
                values.put("subcategoryStatus", 0);
                db.update(PRODUCT_SUBCATEGORY_TABLE, values, "subcategoryId=?", new String[]{subcategoryId});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public boolean subcategoryNetworkStatusExists(String subcategoryNetworkStatus) {
        if (subcategoryNetworkStatus == null || subcategoryNetworkStatus.trim().isEmpty()) {
            return false;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT 1 FROM " + PRODUCT_SUBCATEGORY_TABLE + " WHERE subcategoryNetworkStatus = ? LIMIT 1",
                    new String[]{subcategoryNetworkStatus});
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void upsertFoodTypeFromServer(String foodTypeName, String foodTypeCode, int sortOrder) {
        if (foodTypeCode == null || foodTypeCode.trim().isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT foodTypeId FROM " + FOOD_TYPE_TABLE + " WHERE foodTypeCode = ? LIMIT 1",
                    new String[]{foodTypeCode});
            ContentValues values = new ContentValues();
            values.put("foodTypeName", foodTypeName);
            values.put("foodTypeSortOrder", sortOrder);
            values.put("foodTypeStatus", 1);
            if (cursor.moveToFirst()) {
                db.update(FOOD_TYPE_TABLE, values, "foodTypeCode=?", new String[]{foodTypeCode});
            } else {
                values.put("foodTypeCode", foodTypeCode);
                db.insert(FOOD_TYPE_TABLE, null, values);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void updateProductSubcategory(String subcategoryId, String subcategoryName, int subcategoryStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("subcategoryName", subcategoryName);
        values.put("subcategoryStatus", 0);
        db.update(PRODUCT_SUBCATEGORY_TABLE, values, "subcategoryId=?", new String[]{subcategoryId});
        ContentValues productDirty = new ContentValues();
        productDirty.put("productStatus", 0);
        db.update(PRODUCT_TABLE, productDirty, "subcategoryId=?", new String[]{subcategoryId});
        db.close();
    }

    public void deleteProductSubcategory(String subcategoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("subcategoryDeletedStatus", "1");
        values.put("subcategoryStatus", 0);
        db.update(PRODUCT_SUBCATEGORY_TABLE, values, "subcategoryId=?", new String[]{subcategoryId});
        ContentValues productDirty = new ContentValues();
        productDirty.put("productStatus", 0);
        db.update(PRODUCT_TABLE, productDirty, "subcategoryId=?", new String[]{subcategoryId});
        db.close();
    }

    public String getSubcategoryNameById(String subcategoryId) {
        if (subcategoryId == null || subcategoryId.trim().isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT subcategoryName FROM " + PRODUCT_SUBCATEGORY_TABLE
                            + " WHERE subcategoryId = ? AND IFNULL(subcategoryDeletedStatus, '0') = '0' LIMIT 1",
                    new String[]{subcategoryId});
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public List<ProductSubcategoryResponse> getProductSubcategoryList(String categoryId) {
        List<ProductSubcategoryResponse> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            if (categoryId == null || categoryId.trim().isEmpty()) {
                cursor = db.rawQuery(
                        "SELECT * FROM " + PRODUCT_SUBCATEGORY_TABLE
                                + " WHERE subcategoryDeletedStatus = '0' ORDER BY subcategorySortOrder ASC, subcategoryId ASC",
                        null);
            } else {
                cursor = db.rawQuery(
                        "SELECT * FROM " + PRODUCT_SUBCATEGORY_TABLE
                                + " WHERE subcategoryDeletedStatus = '0' AND categoryId = ?"
                                + " ORDER BY subcategorySortOrder ASC, subcategoryId ASC",
                        new String[]{categoryId});
            }
            while (cursor.moveToNext()) {
                ProductSubcategoryResponse item = new ProductSubcategoryResponse();
                item.setSubcategoryId(cursor.getString(cursor.getColumnIndex("subcategoryId")));
                item.setCategoryId(cursor.getString(cursor.getColumnIndex("categoryId")));
                item.setSubcategoryName(cursor.getString(cursor.getColumnIndex("subcategoryName")));
                item.setSubcategoryDeletedStatus(cursor.getString(cursor.getColumnIndex("subcategoryDeletedStatus")));
                item.setSubcategoryNetworkStatus(cursor.getString(cursor.getColumnIndex("subcategoryNetworkStatus")));
                item.setSubcategoryStatus(cursor.getString(cursor.getColumnIndex("subcategoryStatus")));
                int sortCol = cursor.getColumnIndex("subcategorySortOrder");
                if (sortCol >= 0 && !cursor.isNull(sortCol)) {
                    item.setSubcategorySortOrder(cursor.getString(sortCol));
                }
                list.add(item);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    public List<ProductSubcategoryResponse> getProductSubcategoryNameList(String categoryId, String subcategoryName) {
        List<ProductSubcategoryResponse> list = new ArrayList<>();
        if (categoryId == null || subcategoryName == null || subcategoryName.trim().isEmpty()) {
            return list;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + PRODUCT_SUBCATEGORY_TABLE
                            + " WHERE categoryId = ? AND subcategoryName = ?"
                            + " AND subcategoryDeletedStatus = '0'",
                    new String[]{categoryId, subcategoryName.trim()});
            while (cursor.moveToNext()) {
                ProductSubcategoryResponse item = new ProductSubcategoryResponse();
                item.setSubcategoryId(cursor.getString(cursor.getColumnIndex("subcategoryId")));
                item.setCategoryId(cursor.getString(cursor.getColumnIndex("categoryId")));
                item.setSubcategoryName(cursor.getString(cursor.getColumnIndex("subcategoryName")));
                list.add(item);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    public boolean portionMasterNetworkStatusExists(String portionMasterNetworkStatus) {
        if (portionMasterNetworkStatus == null || portionMasterNetworkStatus.trim().isEmpty()) {
            return false;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT 1 FROM " + PORTION_MASTER_TABLE + " WHERE portionMasterNetworkStatus = ? LIMIT 1",
                    new String[]{portionMasterNetworkStatus});
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private PortionMasterResponse mapPortionMaster(Cursor cursor) {
        PortionMasterResponse item = new PortionMasterResponse();
        item.setPortionMasterId(cursor.getString(cursor.getColumnIndex("portionMasterId")));
        item.setPortionName(cursor.getString(cursor.getColumnIndex("portionName")));
        item.setPortionMasterDeletedStatus(cursor.getString(cursor.getColumnIndex("portionMasterDeletedStatus")));
        item.setPortionMasterNetworkStatus(cursor.getString(cursor.getColumnIndex("portionMasterNetworkStatus")));
        item.setPortionMasterStatus(cursor.getString(cursor.getColumnIndex("portionMasterStatus")));
        return item;
    }

    public boolean insertPortionMaster(String portionName, String portionMasterDeletedStatus,
                                       String portionMasterNetworkStatus, int portionMasterStatus) {
        if (portionMasterNetworkStatusExists(portionMasterNetworkStatus)) {
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("portionName", portionName);
        values.put("portionMasterDeletedStatus",
                portionMasterDeletedStatus != null ? portionMasterDeletedStatus : "0");
        values.put("portionMasterNetworkStatus", portionMasterNetworkStatus);
        values.put("portionMasterStatus", portionMasterStatus);
        long rowId = db.insertWithOnConflict(PORTION_MASTER_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return rowId != -1;
    }

    public void updatePortionMaster(String portionMasterId, String portionName, int portionMasterStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("portionName", portionName);
        values.put("portionMasterStatus", 0);
        db.update(PORTION_MASTER_TABLE, values, "portionMasterId=?", new String[]{portionMasterId});
        ContentValues linkValues = new ContentValues();
        linkValues.put("portionName", portionName);
        linkValues.put("portionStatus", 0);
        db.update(PRODUCT_PORTION_TABLE, linkValues, "portionMasterId=?", new String[]{portionMasterId});
        db.close();
    }

    public void deletePortionMaster(String portionMasterId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("portionMasterDeletedStatus", "1");
        values.put("portionMasterStatus", 0);
        db.update(PORTION_MASTER_TABLE, values, "portionMasterId=?", new String[]{portionMasterId});
        ContentValues linkValues = new ContentValues();
        linkValues.put("portionStatus", 0);
        db.update(PRODUCT_PORTION_TABLE, linkValues, "portionMasterId=?", new String[]{portionMasterId});
        db.close();
    }

    public List<PortionMasterResponse> getPortionMasterList() {
        List<PortionMasterResponse> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + PORTION_MASTER_TABLE
                            + " WHERE portionMasterDeletedStatus = '0' ORDER BY portionName ASC, portionMasterId ASC",
                    null);
            while (cursor.moveToNext()) {
                list.add(mapPortionMaster(cursor));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    public PortionMasterResponse getPortionMasterById(String portionMasterId) {
        if (portionMasterId == null || portionMasterId.trim().isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + PORTION_MASTER_TABLE + " WHERE portionMasterId = ? LIMIT 1",
                    new String[]{portionMasterId});
            if (cursor.moveToFirst()) {
                return mapPortionMaster(cursor);
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<PortionMasterResponse> getPortionMasterByName(String portionName) {
        List<PortionMasterResponse> list = new ArrayList<>();
        if (portionName == null || portionName.trim().isEmpty()) {
            return list;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + PORTION_MASTER_TABLE
                            + " WHERE LOWER(TRIM(portionName)) = LOWER(?) AND portionMasterDeletedStatus = '0'",
                    new String[]{portionName.trim()});
            while (cursor.moveToNext()) {
                list.add(mapPortionMaster(cursor));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    public int countUsageOnProducts(String portionMasterId) {
        if (portionMasterId == null || portionMasterId.trim().isEmpty()) {
            return 0;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + PRODUCT_PORTION_TABLE
                            + " WHERE portionMasterId = ? AND portionDeletedStatus = '0'",
                    new String[]{portionMasterId});
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void ensurePortionMasterFromServer(String portionName, String portionMasterDeletedStatus,
                                            String portionMasterNetworkStatus) {
        if (portionMasterNetworkStatus == null || portionMasterNetworkStatus.trim().isEmpty()) {
            return;
        }
        if (portionMasterNetworkStatusExists(portionMasterNetworkStatus)) {
            return;
        }
        String deleted = portionMasterDeletedStatus != null ? portionMasterDeletedStatus : "0";
        insertPortionMaster(
                portionName != null ? portionName : "",
                deleted,
                portionMasterNetworkStatus,
                1);
    }

    public String resolveLocalPortionMasterId(String portionMasterId, String portionName,
                                              String portionMasterNetworkStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = ensureLocalPortionMasterId(db, portionName, portionMasterNetworkStatus);
        db.close();
        return id > 0 ? String.valueOf(id) : portionMasterId;
    }

    public boolean portionNetworkStatusExists(String portionNetworkStatus) {
        if (portionNetworkStatus == null || portionNetworkStatus.trim().isEmpty()) {
            return false;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT 1 FROM " + PRODUCT_PORTION_TABLE + " WHERE portionNetworkStatus = ? LIMIT 1",
                    new String[]{portionNetworkStatus});
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private ProductPortionResponse mapProductPortion(Cursor cursor) {
        ProductPortionResponse item = new ProductPortionResponse();
        item.setPortionId(cursor.getString(cursor.getColumnIndex("portionId")));
        item.setProductId(cursor.getString(cursor.getColumnIndex("productId")));
        item.setPortionName(cursor.getString(cursor.getColumnIndex("portionName")));
        item.setPortionPrice(cursor.getString(cursor.getColumnIndex("portionPrice")));
        item.setPortionSortOrder(cursor.getString(cursor.getColumnIndex("portionSortOrder")));
        item.setPortionDeletedStatus(cursor.getString(cursor.getColumnIndex("portionDeletedStatus")));
        item.setPortionNetworkStatus(cursor.getString(cursor.getColumnIndex("portionNetworkStatus")));
        item.setPortionStatus(cursor.getString(cursor.getColumnIndex("portionStatus")));
        int masterCol = cursor.getColumnIndex("portionMasterId");
        if (masterCol >= 0 && !cursor.isNull(masterCol)) {
            item.setPortionMasterId(cursor.getString(masterCol));
        }
        return item;
    }

    /**
     * Inserts or updates a product portion. Upserts on (productId, portionMasterId) when master is set.
     */
    public boolean insertProductPortion(String productId, String portionMasterId, String portionName,
                                        String portionPrice, int portionSortOrder, String portionDeletedStatus,
                                        String portionNetworkStatus, int portionStatus) {
        String deleted = portionDeletedStatus != null ? portionDeletedStatus : "0";
        if (portionMasterId != null && !portionMasterId.trim().isEmpty()) {
            ProductPortionResponse existing = getProductPortionByMasterId(productId, portionMasterId);
            if (existing != null) {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("portionName", portionName);
                values.put("portionPrice", portionPrice);
                values.put("portionSortOrder", portionSortOrder);
                values.put("portionDeletedStatus", deleted);
                if (portionNetworkStatus != null && !portionNetworkStatus.trim().isEmpty()) {
                    values.put("portionNetworkStatus", portionNetworkStatus);
                }
                values.put("portionStatus", portionStatus);
                db.update(PRODUCT_PORTION_TABLE, values, "portionId=?", new String[]{existing.getPortionId()});
                db.close();
                if (portionStatus == 0) {
                    markProductUnsynced(productId);
                }
                return true;
            }
        }
        if (portionNetworkStatusExists(portionNetworkStatus)) {
            if (portionStatus == 0) {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("portionName", portionName);
                values.put("portionPrice", portionPrice);
                values.put("portionSortOrder", portionSortOrder);
                values.put("portionDeletedStatus", deleted);
                values.put("portionStatus", 0);
                putOptionalColumn(values, "portionMasterId", portionMasterId);
                db.update(PRODUCT_PORTION_TABLE, values, "portionNetworkStatus=?",
                        new String[]{portionNetworkStatus});
                db.close();
                markProductUnsynced(productId);
                return true;
            }
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("productId", productId);
        putOptionalColumn(values, "portionMasterId", portionMasterId);
        values.put("portionName", portionName);
        values.put("portionPrice", portionPrice);
        values.put("portionSortOrder", portionSortOrder);
        values.put("portionDeletedStatus", deleted);
        values.put("portionNetworkStatus", portionNetworkStatus);
        values.put("portionStatus", portionStatus);
        long rowId = db.insertWithOnConflict(PRODUCT_PORTION_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if (rowId == -1 && portionMasterId != null && !portionMasterId.trim().isEmpty()) {
            values.remove("productId");
            values.remove("portionNetworkStatus");
            values.put("portionStatus", 0);
            db.update(PRODUCT_PORTION_TABLE, values, "productId=? AND portionMasterId=? AND portionDeletedStatus='0'",
                    new String[]{productId, portionMasterId});
        }
        db.close();
        if (portionStatus == 0) {
            markProductUnsynced(productId);
        }
        return true;
    }

    /**
     * Legacy entry point without portion master — resolves master by name when possible.
     */
    public boolean insertProductPortion(String productId, String portionName, String portionPrice,
                                        int portionSortOrder, String portionDeletedStatus,
                                        String portionNetworkStatus, int portionStatus) {
        String masterId = null;
        List<PortionMasterResponse> masters = getPortionMasterByName(portionName);
        if (!masters.isEmpty()) {
            masterId = masters.get(0).getPortionMasterId();
        }
        return insertProductPortion(productId, masterId, portionName, portionPrice, portionSortOrder,
                portionDeletedStatus, portionNetworkStatus, portionStatus);
    }

    public boolean insertProductPortion(ProductPortionResponse portion) {
        if (portion == null) {
            return false;
        }
        int sort = 0;
        try {
            if (portion.getPortionSortOrder() != null && !portion.getPortionSortOrder().isEmpty()) {
                sort = Integer.parseInt(portion.getPortionSortOrder());
            }
        } catch (NumberFormatException ignored) {
        }
        int status = 1;
        try {
            if (portion.getPortionStatus() != null && !portion.getPortionStatus().isEmpty()) {
                status = Integer.parseInt(portion.getPortionStatus());
            }
        } catch (NumberFormatException ignored) {
        }
        String masterId = portion.getPortionMasterId();
        String masterName = portion.getPortionName();
        if ((masterId == null || masterId.trim().isEmpty())
                && portion.getPortionMasterNetworkStatus() != null
                && !portion.getPortionMasterNetworkStatus().trim().isEmpty()) {
            ensurePortionMasterFromServer(masterName, "0", portion.getPortionMasterNetworkStatus());
            masterId = resolveLocalPortionMasterId(null, masterName, portion.getPortionMasterNetworkStatus());
        }
        return insertProductPortion(
                portion.getProductId(),
                masterId,
                portion.getPortionName(),
                portion.getPortionPrice(),
                sort,
                portion.getPortionDeletedStatus(),
                portion.getPortionNetworkStatus(),
                status);
    }

    public void updateProductPortion(String portionId, String portionName, String portionPrice, int portionSortOrder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("portionName", portionName);
        values.put("portionPrice", portionPrice);
        values.put("portionSortOrder", portionSortOrder);
        values.put("portionStatus", 0);
        db.update(PRODUCT_PORTION_TABLE, values, "portionId=?", new String[]{portionId});
        String productId = productIdForPortion(db, portionId);
        db.close();
        markProductUnsynced(productId);
    }

    public void updateProductPortionPriceAndSort(String portionId, String portionPrice, int portionSortOrder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("portionPrice", portionPrice);
        values.put("portionSortOrder", portionSortOrder);
        values.put("portionStatus", 0);
        db.update(PRODUCT_PORTION_TABLE, values, "portionId=?", new String[]{portionId});
        String productId = productIdForPortion(db, portionId);
        db.close();
        markProductUnsynced(productId);
    }

    public void deleteProductPortion(String portionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("portionDeletedStatus", "1");
        values.put("portionStatus", 0);
        db.update(PRODUCT_PORTION_TABLE, values, "portionId=?", new String[]{portionId});
        String productId = productIdForPortion(db, portionId);
        db.close();
        markProductUnsynced(productId);
    }

    private String productIdForPortion(SQLiteDatabase db, String portionId) {
        if (portionId == null || portionId.trim().isEmpty()) {
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT productId FROM " + PRODUCT_PORTION_TABLE + " WHERE portionId = ? LIMIT 1",
                    new String[]{portionId});
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public void markProductUnsynced(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("productStatus", 0);
        db.update(PRODUCT_TABLE, values, "productId=?", new String[]{productId});
        db.close();
    }

    public int countActiveProductPortions(String productId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + PRODUCT_PORTION_TABLE
                            + " WHERE productId = ? AND portionDeletedStatus = '0'",
                    new String[]{productId});
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean hasProductPortions(String productId) {
        return countActiveProductPortions(productId) > 0;
    }

    public List<ProductPortionResponse> getProductPortionList(String productId) {
        List<ProductPortionResponse> list = new ArrayList<>();
        if (productId == null || productId.trim().isEmpty()) {
            return list;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + PRODUCT_PORTION_TABLE
                            + " WHERE productId = ? AND portionDeletedStatus = '0'"
                            + " ORDER BY portionSortOrder ASC, portionId ASC",
                    new String[]{productId});
            while (cursor.moveToNext()) {
                list.add(mapProductPortion(cursor));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    public List<ProductPortionResponse> getProductPortionNameList(String productId, String portionName) {
        List<ProductPortionResponse> list = new ArrayList<>();
        if (productId == null || portionName == null || portionName.trim().isEmpty()) {
            return list;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + PRODUCT_PORTION_TABLE
                            + " WHERE productId = ? AND portionName = ?"
                            + " AND portionDeletedStatus = '0'",
                    new String[]{productId, portionName.trim()});
            while (cursor.moveToNext()) {
                list.add(mapProductPortion(cursor));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    public ProductPortionResponse getProductPortionByMasterId(String productId, String portionMasterId) {
        if (productId == null || productId.trim().isEmpty()
                || portionMasterId == null || portionMasterId.trim().isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + PRODUCT_PORTION_TABLE
                            + " WHERE productId = ? AND portionMasterId = ? AND portionDeletedStatus = '0' LIMIT 1",
                    new String[]{productId, portionMasterId});
            if (cursor.moveToFirst()) {
                return mapProductPortion(cursor);
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public ProductPortionResponse getProductPortionById(String portionId) {
        if (portionId == null || portionId.trim().isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + PRODUCT_PORTION_TABLE + " WHERE portionId = ? LIMIT 1",
                    new String[]{portionId});
            if (cursor.moveToFirst()) {
                return mapProductPortion(cursor);
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Billing helper: use base product price when no portions exist; otherwise first active portion price.
     * Full portion-picker UX comes in P3-5.
     */
    public String getEffectiveProductPrice(String productId) {
        List<ProductPortionResponse> portions = getProductPortionList(productId);
        if (portions.isEmpty()) {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery(
                        "SELECT productPrice FROM " + PRODUCT_TABLE + " WHERE productId = ? LIMIT 1",
                        new String[]{productId});
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return "0";
        }
        return portions.get(0).getPortionPrice();
    }

    public void addProduct(String userId, String categoryId, String categoryName, String productCode, String productName, String productPrice, String unitName, String productCGST, String productSGST, int productStatus, String productNetworkStatus, String productDeletedStatus) {
        addProduct(userId, categoryId, categoryName, productCode, productName, productPrice, unitName, productCGST, productSGST, productStatus, productNetworkStatus, productDeletedStatus, null, "off");
    }

    public void addProduct(String userId, String categoryId, String categoryName, String productCode, String productName, String productPrice, String unitName, String productCGST, String productSGST, int productStatus, String productNetworkStatus, String productDeletedStatus, String subcategoryId) {
        addProduct(userId, categoryId, categoryName, productCode, productName, productPrice, unitName, productCGST, productSGST, productStatus, productNetworkStatus, productDeletedStatus, subcategoryId, "off");
    }

    public void addProduct(String userId, String categoryId, String categoryName, String productCode, String productName, String productPrice, String unitName, String productCGST, String productSGST, int productStatus, String productNetworkStatus, String productDeletedStatus, String subcategoryId, String openPrice) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        float productWithGSTPrice = 0f, productCGSTAmount = 0f, productSGSTAmount = 0f;
        if (productCGST != null) {
            productCGSTAmount = Float.parseFloat(!productCGST.isEmpty() ? productCGST : "0");
        }

        if (productSGST != null) {
            productSGSTAmount = Float.parseFloat(!productSGST.isEmpty() ? productSGST : "0");
        }

        productWithGSTPrice = Float.parseFloat(productPrice) + ((Float.parseFloat(productPrice)) * ((productCGSTAmount + productSGSTAmount) / 100));

        contentValues.put("categoryId", categoryId);
        contentValues.put("categoryName", categoryName);
        contentValues.put("productCode", productCode);
        contentValues.put("productName", productName);
        contentValues.put("productPrice", productPrice);
        contentValues.put("openPrice", openPrice != null && !openPrice.isEmpty() ? openPrice : "off");
        contentValues.put("productUnit", unitName);
        contentValues.put("productCGST", productCGST);
        contentValues.put("productSGST", productSGST);
        contentValues.put("productWithGSTPrice", String.valueOf(productWithGSTPrice));
        contentValues.put("productStatus", productStatus);
        contentValues.put("productDeletedStatus", productDeletedStatus);
        contentValues.put("productNetworkStatus", productNetworkStatus);
        putOptionalColumn(contentValues, "subcategoryId", subcategoryId);

        db.insert(PRODUCT_TABLE, null, contentValues);

        db.close();

    }

    public long addProductAndReturnId(String userId, String categoryId, String categoryName, String productCode,
                                    String productName, String productPrice, String unitName, String productCGST,
                                    String productSGST, int productStatus, String productNetworkStatus,
                                    String productDeletedStatus, String subcategoryId) {
        return addProductAndReturnId(userId, categoryId, categoryName, productCode, productName, productPrice,
                unitName, productCGST, productSGST, productStatus, productNetworkStatus, productDeletedStatus,
                subcategoryId, "off");
    }

    public long addProductAndReturnId(String userId, String categoryId, String categoryName, String productCode,
                                    String productName, String productPrice, String unitName, String productCGST,
                                    String productSGST, int productStatus, String productNetworkStatus,
                                    String productDeletedStatus, String subcategoryId, String openPrice) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        float productWithGSTPrice = 0f, productCGSTAmount = 0f, productSGSTAmount = 0f;
        if (productCGST != null) {
            productCGSTAmount = Float.parseFloat(!productCGST.isEmpty() ? productCGST : "0");
        }
        if (productSGST != null) {
            productSGSTAmount = Float.parseFloat(!productSGST.isEmpty() ? productSGST : "0");
        }
        productWithGSTPrice = Float.parseFloat(productPrice)
                + ((Float.parseFloat(productPrice)) * ((productCGSTAmount + productSGSTAmount) / 100));

        contentValues.put("categoryId", categoryId);
        contentValues.put("categoryName", categoryName);
        contentValues.put("productCode", productCode);
        contentValues.put("productName", productName);
        contentValues.put("productPrice", productPrice);
        contentValues.put("openPrice", openPrice != null && !openPrice.isEmpty() ? openPrice : "off");
        contentValues.put("productUnit", unitName);
        contentValues.put("productCGST", productCGST);
        contentValues.put("productSGST", productSGST);
        contentValues.put("productWithGSTPrice", String.valueOf(productWithGSTPrice));
        contentValues.put("productStatus", productStatus);
        contentValues.put("productDeletedStatus", productDeletedStatus);
        contentValues.put("productNetworkStatus", productNetworkStatus);
        putOptionalColumn(contentValues, "subcategoryId", subcategoryId);

        long rowId = db.insert(PRODUCT_TABLE, null, contentValues);
        db.close();
        return rowId;
    }

    public String getProductIdByNetworkStatus(String productNetworkStatus) {
        if (productNetworkStatus == null || productNetworkStatus.trim().isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT productId FROM " + PRODUCT_TABLE + " WHERE productNetworkStatus = ? LIMIT 1",
                    new String[]{productNetworkStatus});
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean addToCart(String userId, ProductResponse productResponse, String productChangePrice, String productQuantity, String noOfTable, String cartDiscount, String cartOrderStatus) {
        return addToCart(userId, productResponse, productChangePrice, productQuantity, noOfTable, cartDiscount, cartOrderStatus, null, null);
    }

    public boolean addToCart(String userId, ProductResponse productResponse, String productChangePrice, String productQuantity,
                             String noOfTable, String cartDiscount, String cartOrderStatus,
                             String portionId, String portionName) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("userId", userId);
        contentValues.put("productId", productResponse.getProductId());
        contentValues.put("productName", productResponse.getProductName());
        contentValues.put("productOldPrice", productResponse.getProductPrice());
        contentValues.put("productNewPrice", productChangePrice);
        contentValues.put("productUnit", productResponse.getProductUnit());
        contentValues.put("productCGST", productResponse.getProductCGST());
        contentValues.put("productSGST", productResponse.getProductSGST());
        contentValues.put("productQuantity", productQuantity);
        contentValues.put("noOfTable", noOfTable);
        contentValues.put("cartDiscount", cartDiscount);
        contentValues.put("cartOrderStatus", cartOrderStatus);
        contentValues.put("cartStatus", 0);
        contentValues.put("snapshotProductName", productResponse.getProductName());
        contentValues.put("snapshotLinePrice", productChangePrice);
        putOptionalColumn(contentValues, "portionId", portionId);
        putOptionalColumn(contentValues, "portionName", portionName);
        contentValues.put("cartItemType", CartItemType.PRODUCT);

        db.insert(CART_PRODUCT_TABLE, null, contentValues);
        db.close();

        return true;

    }

    private void putOptionalColumn(ContentValues values, String column, String value) {
        if (value != null && !value.trim().isEmpty()) {
            values.put(column, value);
        }
    }

    private void mapCartLineSnapshots(Cursor cursor, ProductCartResponse item) {
        mapStringColumn(cursor, "portionId", item::setPortionId);
        mapStringColumn(cursor, "portionName", item::setPortionName);
        mapStringColumn(cursor, "snapshotProductName", item::setSnapshotProductName);
        mapStringColumn(cursor, "snapshotLinePrice", item::setSnapshotLinePrice);
        mapStringColumn(cursor, "cartItemType", item::setCartItemType);
        mapStringColumn(cursor, "comboId", item::setComboId);
        mapStringColumn(cursor, "snapshotComboComponents", item::setSnapshotComboComponents);
        int openPriceCol = cursor.getColumnIndex("openPrice");
        if (openPriceCol >= 0 && !cursor.isNull(openPriceCol)) {
            item.setOpenPrice(cursor.getString(openPriceCol));
        } else {
            item.setOpenPrice("off");
        }
    }

    private void mapInvoiceLineSnapshots(Cursor cursor, InvoiceProductResponse item) {
        mapStringColumn(cursor, "portionId", item::setPortionId);
        mapStringColumn(cursor, "portionName", item::setPortionName);
        mapStringColumn(cursor, "snapshotProductName", item::setSnapshotProductName);
        mapStringColumn(cursor, "snapshotLinePrice", item::setSnapshotLinePrice);
        mapStringColumn(cursor, "invoiceItemType", item::setInvoiceItemType);
        mapStringColumn(cursor, "comboId", item::setComboId);
        mapStringColumn(cursor, "snapshotComboComponents", item::setSnapshotComboComponents);
    }

    private interface StringColumnConsumer {
        void accept(String value);
    }

    private void mapStringColumn(Cursor cursor, String column, StringColumnConsumer consumer) {
        int idx = cursor.getColumnIndex(column);
        if (idx >= 0 && !cursor.isNull(idx)) {
            consumer.accept(cursor.getString(idx));
        }
    }

    public boolean addCompanyPrinterSetting(String printerName, String KOTPrinterName, String invoicePrefix, String invoiceTitle, String logoUse, String paymentUse, String customerUse, String productQuantityUpdate, String duplicateBillUse, String invoiceTermsCondition, String bluetoothAddress, String bluetoothKOTAddress, String printerFeedLines, String KotPrinterFeedLines, int settingStatus) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("printerName", printerName);
        contentValues.put("KOTPrinterName", KOTPrinterName);
        contentValues.put("invoicePrefix", invoicePrefix);
        contentValues.put("invoiceTitle", invoiceTitle);
        contentValues.put("logoUse", logoUse);
        contentValues.put("paymentUse", paymentUse);
        contentValues.put("customerUse", customerUse);
        contentValues.put("productQuantityUpdate", productQuantityUpdate);
        contentValues.put("duplicateBillUse", duplicateBillUse != null ? duplicateBillUse : "off");
        contentValues.put("invoiceTermsCondition", invoiceTermsCondition);
        contentValues.put("bluetoothAddress", bluetoothAddress);
        contentValues.put("bluetoothKOTAddress", bluetoothKOTAddress);
        contentValues.put("printerFeedLines", printerFeedLines);
        contentValues.put("KotPrinterFeedLines", KotPrinterFeedLines);
        contentValues.put("settingStatus", settingStatus);

        db.insert(PRINTER_SETTING_TABLE, null, contentValues);
        db.close();

        return true;
    }

    public void updateCartDiscount(String cartId, String cartDiscount, String cartDiscountType) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("cartDiscount", cartDiscount);
        contentValues.put("cartDiscountType", cartDiscountType);

        db.update(CART_PRODUCT_TABLE, contentValues, "cartId=?", new String[]{cartId});
        db.close();

    }

    public void updateCompanyPrinterSetting(String settingId, String printerName, String KOTPrinterName, String invoicePrefix, String invoiceTitle, String logoUse, String paymentUse, String customerUse, String productQuantityUpdate, String duplicateBillUse, String invoiceTermsCondition, String bluetoothAddress, String bluetoothKOTAddress, String printerFeedLines, String KotPrinterFeedLines, int settingStatus) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("printerName", printerName);
        contentValues.put("KOTPrinterName", KOTPrinterName);
        contentValues.put("invoicePrefix", invoicePrefix);
        contentValues.put("invoiceTitle", invoiceTitle);
        contentValues.put("logoUse", logoUse);
        contentValues.put("paymentUse", paymentUse);
        contentValues.put("customerUse", customerUse);
        contentValues.put("productQuantityUpdate", productQuantityUpdate);
        contentValues.put("duplicateBillUse", duplicateBillUse != null ? duplicateBillUse : "off");
        contentValues.put("bluetoothAddress", bluetoothAddress);
        contentValues.put("bluetoothKOTAddress", bluetoothKOTAddress);
        contentValues.put("printerFeedLines", printerFeedLines);
        contentValues.put("KotPrinterFeedLines", KotPrinterFeedLines);
        contentValues.put("invoiceTermsCondition", invoiceTermsCondition);
        contentValues.put("settingStatus", 0);

        db.update(PRINTER_SETTING_TABLE, contentValues, "settingId=?", new String[]{settingId});
        db.close();

    }

    /**
     * Saves local-only printer transport / cutter / cash-drawer settings.
     * Does not affect cloud sync payload for legacy fields.
     */
    public void updatePrinterTransportSettings(String settingId,
                                               String billConnectionType, String kotConnectionType,
                                               String billPrinterIp, String kotPrinterIp,
                                               String billPrinterPort, String kotPrinterPort,
                                               String billUsbDeviceKey, String kotUsbDeviceKey,
                                               String supportsCutter, String supportsCashDrawer,
                                               String autoCut, String autoOpenCashDrawer,
                                               String drawerOpenMode, String drawerPin,
                                               String drawerPulseOn, String drawerPulseOff,
                                               String cutCommand, String printerModel) {
        if (settingId == null || settingId.trim().isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("billConnectionType", billConnectionType != null ? billConnectionType : "BLUETOOTH");
        contentValues.put("kotConnectionType", kotConnectionType != null ? kotConnectionType : "BLUETOOTH");
        contentValues.put("billPrinterIp", billPrinterIp != null ? billPrinterIp : "");
        contentValues.put("kotPrinterIp", kotPrinterIp != null ? kotPrinterIp : "");
        contentValues.put("billPrinterPort", billPrinterPort != null && !billPrinterPort.isEmpty() ? billPrinterPort : "9100");
        contentValues.put("kotPrinterPort", kotPrinterPort != null && !kotPrinterPort.isEmpty() ? kotPrinterPort : "9100");
        contentValues.put("billUsbDeviceKey", billUsbDeviceKey != null ? billUsbDeviceKey : "");
        contentValues.put("kotUsbDeviceKey", kotUsbDeviceKey != null ? kotUsbDeviceKey : "");
        contentValues.put("supportsCutter", supportsCutter != null ? supportsCutter : "on");
        contentValues.put("supportsCashDrawer", supportsCashDrawer != null ? supportsCashDrawer : "off");
        contentValues.put("autoCut", autoCut != null ? autoCut : "on");
        contentValues.put("autoOpenCashDrawer", autoOpenCashDrawer != null ? autoOpenCashDrawer : "on");
        contentValues.put("drawerOpenMode", drawerOpenMode != null ? drawerOpenMode : "CASH_ONLY");
        contentValues.put("drawerPin", drawerPin != null ? drawerPin : "0");
        contentValues.put("drawerPulseOn", drawerPulseOn != null ? drawerPulseOn : "25");
        contentValues.put("drawerPulseOff", drawerPulseOff != null ? drawerPulseOff : "120");
        contentValues.put("cutCommand", cutCommand != null ? cutCommand : "FULL");
        contentValues.put("printerModel", printerModel != null ? printerModel : "");
        db.update(PRINTER_SETTING_TABLE, contentValues, "settingId=?", new String[]{settingId});
        db.close();
    }

    public boolean addCompanyDetails(String companyLogo, String companyName, String cashierName, String companyMobile, String companyAddress, String currencyName, String tableStatus, String noOfTable, String countryName,
                                     String stateName, String gstStatus, String gstNumber, String shopCGST, String shopSGST, String panNumber, String companyFssis, int companyStatus, String paymentLogo) {
        // Legacy callers: map single address/mobile/name into structured primary fields
        return addCompanyDetails(companyLogo, companyName, "", cashierName, companyMobile, "", companyAddress, "", "", currencyName, tableStatus, noOfTable, countryName,
                stateName, gstStatus, gstNumber, shopCGST, shopSGST, panNumber, companyFssis, companyStatus, paymentLogo);
    }

    public boolean addCompanyDetails(String companyLogo, String shopName1, String shopName2, String cashierName, String phoneNo1, String phoneNo2,
                                     String addressLine1, String addressLine2, String addressLine3, String currencyName, String tableStatus, String noOfTable, String countryName,
                                     String stateName, String gstStatus, String gstNumber, String shopCGST, String shopSGST, String panNumber, String companyFssis, int companyStatus, String paymentLogo) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = putCompanyContentValues(companyLogo, shopName1, shopName2, cashierName, phoneNo1, phoneNo2,
                addressLine1, addressLine2, addressLine3, currencyName, tableStatus, noOfTable, countryName,
                stateName, gstStatus, gstNumber, shopCGST, shopSGST, panNumber, companyFssis, companyStatus, paymentLogo);
        db.insert(COMPANY_TABLE, null, contentValues);
        db.close();
        return true;
    }

    private ContentValues putCompanyContentValues(String companyLogo, String shopName1, String shopName2, String cashierName, String phoneNo1, String phoneNo2,
                                                  String addressLine1, String addressLine2, String addressLine3, String currencyName, String tableStatus, String noOfTable, String countryName,
                                                  String stateName, String gstStatus, String gstNumber, String shopCGST, String shopSGST, String panNumber, String companyFssis, int companyStatus, String paymentLogo) {
        ContentValues contentValues = new ContentValues();
        String resolvedShopName1 = shopName1 != null ? shopName1.trim() : "";
        String resolvedPhone1 = phoneNo1 != null ? phoneNo1.trim() : "";
        String resolvedAddress1 = addressLine1 != null ? addressLine1.trim() : "";
        String resolvedAddress2 = addressLine2 != null ? addressLine2.trim() : "";
        String resolvedAddress3 = addressLine3 != null ? addressLine3.trim() : "";
        String legacyAddress = joinAddressLines(resolvedAddress1, resolvedAddress2, resolvedAddress3);

        contentValues.put("companyName", resolvedShopName1);
        contentValues.put("shopName1", resolvedShopName1);
        contentValues.put("shopName2", shopName2 != null ? shopName2.trim() : "");
        contentValues.put("cashierName", cashierName);
        contentValues.put("companyMobile", resolvedPhone1);
        contentValues.put("phoneNo1", resolvedPhone1);
        contentValues.put("phoneNo2", phoneNo2 != null ? phoneNo2.trim() : "");
        contentValues.put("companyAddress", legacyAddress);
        contentValues.put("addressLine1", resolvedAddress1);
        contentValues.put("addressLine2", resolvedAddress2);
        contentValues.put("addressLine3", resolvedAddress3);
        contentValues.put("currencyName", currencyName);
        contentValues.put("tableStatus", tableStatus);
        contentValues.put("noOfTable", noOfTable);
        contentValues.put("countryName", countryName);
        contentValues.put("stateName", stateName);
        contentValues.put("gstStatus", gstStatus);
        contentValues.put("gstNumber", gstNumber);
        contentValues.put("shopCGST", shopCGST);
        contentValues.put("shopSGST", shopSGST);
        contentValues.put("panNumber", panNumber);
        contentValues.put("companyFssis", companyFssis);
        contentValues.put("companyLogo", companyLogo);
        contentValues.put("companyStatus", companyStatus);
        contentValues.put("paymentLogo", paymentLogo);
        return contentValues;
    }

    private static String joinAddressLines(String line1, String line2, String line3) {
        StringBuilder composed = new StringBuilder();
        appendAddressPart(composed, line1);
        appendAddressPart(composed, line2);
        appendAddressPart(composed, line3);
        return composed.toString();
    }

    private static void appendAddressPart(StringBuilder builder, String part) {
        if (part == null) {
            return;
        }
        String trimmed = part.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(trimmed);
    }

    public List<InvoiceResponse> checkPaymentMode(String invoiceNumber) {

        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + INVOICE_TABLE + " WHERE invoiceNumber='" + invoiceNumber + "' AND paymentMode=''" + andBranchScope(null), null);
        InvoiceResponse invoiceResponse;
        while (cursor.moveToNext()) {
            invoiceResponse = new InvoiceResponse();
            invoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
            invoiceResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            invoiceResponse.setInvoiceNumber(cursor.getString(cursor.getColumnIndex("invoiceNumber")));
            invoiceResponse.setCustomerName(cursor.getString(cursor.getColumnIndex("customerName")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerMobile")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerEmail")));
            invoiceResponse.setCustomerAddress(cursor.getString(cursor.getColumnIndex("customerAddress")));
            invoiceResponse.setSubTotal(cursor.getString(cursor.getColumnIndex("subTotal")));
            invoiceResponse.setTotalGSTAmount(cursor.getString(cursor.getColumnIndex("totalGSTAmount")));
            invoiceResponse.setDiscount(cursor.getString(cursor.getColumnIndex("discount")));
            invoiceResponse.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
            invoiceResponse.setPaymentMode(cursor.getString(cursor.getColumnIndex("paymentMode")));
            invoiceResponse.setInvoiceDate(cursor.getString(cursor.getColumnIndex("invoiceDate")));
            invoiceResponse.setInvoiceOrderStatus(cursor.getString(cursor.getColumnIndex("invoiceOrderStatus")));
            invoiceResponse.setInvoiceNetworkStatus(cursor.getString(cursor.getColumnIndex("invoiceNetworkStatus")));
            invoiceResponse.setInvoiceType(cursor.getString(cursor.getColumnIndex("invoiceType")));
            invoiceResponse.setInvoiceStatus(cursor.getString(cursor.getColumnIndex("invoiceStatus")));
            invoiceResponseList.add(invoiceResponse);
        }
        db.close();
        return invoiceResponseList;

    }

    public List<InvoiceResponse> checkTablePaymentMode(String tableNumber) {

        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + INVOICE_TABLE + " WHERE noOfTable='" + tableNumber + "' AND paymentMode=''" + andBranchScope(null), null);
        InvoiceResponse invoiceResponse;
        while (cursor.moveToNext()) {
            invoiceResponse = new InvoiceResponse();
            invoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
            invoiceResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            invoiceResponse.setInvoiceNumber(cursor.getString(cursor.getColumnIndex("invoiceNumber")));
            invoiceResponse.setCustomerName(cursor.getString(cursor.getColumnIndex("customerName")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerMobile")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerEmail")));
            invoiceResponse.setCustomerAddress(cursor.getString(cursor.getColumnIndex("customerAddress")));
            invoiceResponse.setSubTotal(cursor.getString(cursor.getColumnIndex("subTotal")));
            invoiceResponse.setTotalGSTAmount(cursor.getString(cursor.getColumnIndex("totalGSTAmount")));
            invoiceResponse.setDiscount(cursor.getString(cursor.getColumnIndex("discount")));
            invoiceResponse.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
            invoiceResponse.setPaymentMode(cursor.getString(cursor.getColumnIndex("paymentMode")));
            invoiceResponse.setInvoiceDate(cursor.getString(cursor.getColumnIndex("invoiceDate")));
            invoiceResponse.setInvoiceOrderStatus(cursor.getString(cursor.getColumnIndex("invoiceOrderStatus")));
            invoiceResponse.setInvoiceNetworkStatus(cursor.getString(cursor.getColumnIndex("invoiceNetworkStatus")));
            invoiceResponse.setInvoiceType(cursor.getString(cursor.getColumnIndex("invoiceType")));
            invoiceResponse.setInvoiceStatus(cursor.getString(cursor.getColumnIndex("invoiceStatus")));
            invoiceResponseList.add(invoiceResponse);
        }
        db.close();
        return invoiceResponseList;

    }

    public void updateInvoicePaymentMode(String invoiceNumber, String paymentMode) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("paymentMode", paymentMode);
        contentValues.put("invoiceStatus", "0");

        db.update(INVOICE_TABLE, contentValues, "invoiceNumber=?", new String[]{invoiceNumber});

    }

    public void updateInvoiceTablePaymentMode(String invoiceNumber, String tableNumber, String paymentMode) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("paymentMode", paymentMode);
        contentValues.put("noOfTable", tableNumber);
        contentValues.put("invoiceStatus", "0");

        db.update(INVOICE_TABLE, contentValues, "invoiceNumber=?", new String[]{invoiceNumber});

    }

    public void refundInvoice(String invoiceNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("invoiceOrderStatus", "refunded");
        contentValues.put("invoiceStatus", "0");
        db.update(INVOICE_TABLE, contentValues, "invoiceNumber=?", new String[]{invoiceNumber});
        db.close();
    }

    public void updateInvoiceHeader(String invoiceNumber, String subTotal, String totalGSTAmount,
                                    String discount, String discountType, String totalAmount, String paymentMode) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("subTotal", subTotal);
        contentValues.put("totalGSTAmount", totalGSTAmount);
        contentValues.put("discount", discount);
        contentValues.put("discountType", discountType);
        contentValues.put("totalAmount", totalAmount);
        contentValues.put("paymentMode", paymentMode);
        contentValues.put("invoiceStatus", "0");
        db.update(INVOICE_TABLE, contentValues, "invoiceNumber=?", new String[]{invoiceNumber});
        db.close();
    }

    public void updateInvoiceProductQuantity(String invoiceProductId, String productQuantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("productQuantity", productQuantity);
        contentValues.put("invoiceProductStatus", 0);
        db.update(INVOICE_PRODUCT_TABLE, contentValues, "invoiceProductId=?", new String[]{invoiceProductId});
        db.close();
    }

    public void deleteInvoiceProduct(String invoiceProductId) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT invoiceNumber, invoiceProductNetworkStatus FROM " + INVOICE_PRODUCT_TABLE
                        + " WHERE invoiceProductId=?",
                new String[]{invoiceProductId});
        String invoiceNumber = null;
        String networkStatus = null;
        if (cursor.moveToFirst()) {
            invoiceNumber = cursor.getString(0);
            networkStatus = cursor.getString(1);
        }
        cursor.close();
        if (networkStatus != null && !networkStatus.trim().isEmpty()) {
            ContentValues queue = new ContentValues();
            queue.put("invoiceNumber", invoiceNumber != null ? invoiceNumber : "");
            queue.put("invoiceProductNetworkStatus", networkStatus);
            db.insert(INVOICE_PRODUCT_DELETE_QUEUE_TABLE, null, queue);
            db.delete(INVOICE_COMBO_ITEM_TABLE, "invoiceProductNetworkStatus=?", new String[]{networkStatus});
        }
        db.delete(INVOICE_PRODUCT_TABLE, "invoiceProductId=?", new String[]{invoiceProductId});
        if (invoiceNumber != null && !invoiceNumber.trim().isEmpty()) {
            ContentValues invoiceDirty = new ContentValues();
            invoiceDirty.put("invoiceStatus", "0");
            db.update(INVOICE_TABLE, invoiceDirty, "invoiceNumber=?", new String[]{invoiceNumber});
        }
        db.close();
    }

    public Cursor getPendingInvoiceProductDeletes() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + INVOICE_PRODUCT_DELETE_QUEUE_TABLE, null);
    }

    public void removePendingInvoiceProductDelete(String deleteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(INVOICE_PRODUCT_DELETE_QUEUE_TABLE, "deleteId=?", new String[]{deleteId});
        db.close();
    }

    public boolean saveInvoice(List<ProductCartResponse> productCartResponseList, String noOfTable, String customerName, String customerMobile,
                               String customerAddress, String invoiceNumber, float subtotal, float totalGSTAmount, float discount, String discountType, float totalAmount,
                               String paymentMode, String invoiceDate, String invoiceType, String invoiceNetworkStatus, int invoiceStatus) {

        // Idempotent local save: same sync key must not create a second bill
        if (invoiceNetworkStatusExists(invoiceNetworkStatus)) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("noOfTable", noOfTable);
        contentValues.put("invoiceNumber", invoiceNumber);
        contentValues.put("customerName", customerName);
        contentValues.put("customerMobile", customerMobile);
        contentValues.put("customerAddress", customerAddress);
        contentValues.put("subTotal", String.valueOf(subtotal));
        contentValues.put("totalGSTAmount", String.valueOf(totalGSTAmount));
        contentValues.put("discount", String.valueOf(discount));
        contentValues.put("discountType", discountType);
        contentValues.put("totalAmount", String.valueOf(totalAmount));
        contentValues.put("paymentMode", paymentMode);
        contentValues.put("invoiceDate", invoiceDate);
        contentValues.put("invoiceOrderStatus", "completed");
        contentValues.put("invoiceNetworkStatus", invoiceNetworkStatus);
        contentValues.put("invoiceType", invoiceType);
        contentValues.put("invoiceStatus", String.valueOf(invoiceStatus));
        BranchSession.applyScope(contentValues);

        long rowId = db.insertWithOnConflict(INVOICE_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
        if (rowId == -1) {
            db.close();
            return false;
        }
        insertInvoiceProduct(productCartResponseList, invoiceNumber);
        db.close();

        return true;

    }

    public boolean insertInvoiceProduct(List<ProductCartResponse> productCartResponseList, String invoiceNumber) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        for (ProductCartResponse productCartResponse : productCartResponseList) {

            contentValues.clear();
            String snapshotBaseName = productCartResponse.getSnapshotProductName();
            if (snapshotBaseName == null || snapshotBaseName.trim().isEmpty()) {
                snapshotBaseName = productCartResponse.getProductName();
            }
            String linePrice = productCartResponse.getResolvedLinePrice();

            contentValues.put("invoiceNumber", invoiceNumber);
            contentValues.put("productName", productCartResponse.getDisplayLineName());
            contentValues.put("productPrice", linePrice);
            contentValues.put("productUnit", productCartResponse.getProductUnit());
            contentValues.put("productCGST", productCartResponse.getProductCGST());
            contentValues.put("productSGST", productCartResponse.getProductSGST());
            contentValues.put("productQuantity", productCartResponse.getProductQuantity());
            contentValues.put("productStatus", "completed");
            contentValues.put("invoiceProductNetworkStatus", getRandomString(10));
            contentValues.put("invoiceProductStatus", 0);
            contentValues.put("snapshotProductName", snapshotBaseName);
            contentValues.put("snapshotLinePrice", linePrice);
            putOptionalColumn(contentValues, "portionId", productCartResponse.getPortionId());
            putOptionalColumn(contentValues, "portionName", productCartResponse.getPortionName());
            String itemType = CartItemType.normalize(productCartResponse.getCartItemType());
            contentValues.put("invoiceItemType", itemType);
            putOptionalColumn(contentValues, "comboId", productCartResponse.getComboId());
            putOptionalColumn(contentValues, "snapshotComboComponents", productCartResponse.getSnapshotComboComponents());
            BranchSession.applyScope(contentValues);

            db.insert(INVOICE_PRODUCT_TABLE, null, contentValues);
            String lineNetworkStatus = contentValues.getAsString("invoiceProductNetworkStatus");
            if (CartItemType.isCombo(itemType)) {
                copyCartComboItemsToInvoice(db, productCartResponse.getCartId(), invoiceNumber, lineNetworkStatus);
            } else {
                updateInventoryQuantity(productCartResponse.getProductId());
            }
            // Remove billed lines from cart on the same DB (do not close mid-insert).
            String cartId = productCartResponse.getCartId();
            if (cartId != null && !cartId.trim().isEmpty()) {
                db.delete(CART_COMBO_ITEM_TABLE, "cartId = ?", new String[]{cartId});
                db.delete(CART_PRODUCT_TABLE, "cartId = ?", new String[]{cartId});
            }

        }

        return true;

    }

    public void updateInventoryQuantity(String productId) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("inventoryStatus", 0);

        db.update(INVENTORY_TABLE, contentValues, "productId=?", new String[]{productId});

    }

    public String getRandomString(final int sizeOfRandomString) {

        String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";

        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    public void updateCartStatus(String cartId) {
        // After billing, remove the line from the active cart (and any combo components).
        deleteCartProduct(cartId);
    }

    public void updateCompanyDetails(String companyLogo, String companyId, String companyName, String cashierName, String companyMobile, String companyAddress, String currencyName, String tableStatus, String noOfTable, String countryName,
                                     String stateName, String gstStatus, String gstNumber, String shopCGST, String shopSGST, String panNumber, String companyFssis, int companyStatus, String paymentLogo) {
        updateCompanyDetails(companyLogo, companyId, companyName, "", cashierName, companyMobile, "", companyAddress, "", "", currencyName, tableStatus, noOfTable, countryName,
                stateName, gstStatus, gstNumber, shopCGST, shopSGST, panNumber, companyFssis, companyStatus, paymentLogo);
    }

    public void updateCompanyDetails(String companyLogo, String companyId, String shopName1, String shopName2, String cashierName, String phoneNo1, String phoneNo2,
                                     String addressLine1, String addressLine2, String addressLine3, String currencyName, String tableStatus, String noOfTable, String countryName,
                                     String stateName, String gstStatus, String gstNumber, String shopCGST, String shopSGST, String panNumber, String companyFssis, int companyStatus, String paymentLogo) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = putCompanyContentValues(companyLogo, shopName1, shopName2, cashierName, phoneNo1, phoneNo2,
                addressLine1, addressLine2, addressLine3, currencyName, tableStatus, noOfTable, countryName,
                stateName, gstStatus, gstNumber, shopCGST != null ? shopCGST.trim() : "", shopSGST != null ? shopSGST.trim() : "", panNumber, companyFssis, companyStatus, paymentLogo);
        contentValues.put("companyStatus", 0);
        db.update(COMPANY_TABLE, contentValues, "companyId=?", new String[]{companyId});
        db.close();
    }

    public void updateCart(String cartId, String productQuantity, String productAmount) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("productQuantity", productQuantity);
        contentValues.put("productOldPrice", productAmount);
        contentValues.put("productNewPrice", productAmount);
        contentValues.put("snapshotLinePrice", productAmount);

        db.update(CART_PRODUCT_TABLE, contentValues, "cartId=?", new String[]{cartId});
        db.close();

    }

    public void updateCategory(String categoryId, String categoryName, int categoryStatus) {
        updateCategory(categoryId, categoryName, categoryStatus, -1);
    }

    public void updateCategory(String categoryId, String categoryName, int categoryStatus, long foodTypeId) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("categoryName", categoryName);
        contentValues.put("categoryStatus", 0);
        if (foodTypeId > 0) {
            contentValues.put("foodTypeId", foodTypeId);
        }

        db.update(PRODUCT_CATEGORY_TABLE, contentValues, "categoryId=?", new String[]{categoryId});
        ContentValues productDirty = new ContentValues();
        productDirty.put("categoryName", categoryName);
        productDirty.put("productStatus", 0);
        db.update(PRODUCT_TABLE, productDirty, "categoryId=?", new String[]{categoryId});
        db.close();

    }

    public void updateProduct(String userId, String productId, String categoryId, String categoryName, String productCode, String productName, String productPrice, String unitName, String productCGST, String productSGST, int productStatus) {
        updateProduct(userId, productId, categoryId, categoryName, productCode, productName, productPrice, unitName, productCGST, productSGST, productStatus, null, "off");
    }

    public void updateProduct(String userId, String productId, String categoryId, String categoryName, String productCode, String productName, String productPrice, String unitName, String productCGST, String productSGST, int productStatus, String subcategoryId) {
        updateProduct(userId, productId, categoryId, categoryName, productCode, productName, productPrice, unitName, productCGST, productSGST, productStatus, subcategoryId, "off");
    }

    public void updateProduct(String userId, String productId, String categoryId, String categoryName, String productCode, String productName, String productPrice, String unitName, String productCGST, String productSGST, int productStatus, String subcategoryId, String openPrice) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        float productWithGSTPrice = 0f, productCGSTAmount = 0f, productSGSTAmount = 0f;
        if (productCGST != null) {
            productCGSTAmount = Float.parseFloat(!productCGST.isEmpty() ? productCGST : "0");
        }

        if (productSGST != null) {
            productSGSTAmount = Float.parseFloat(!productSGST.isEmpty() ? productSGST : "0");
        }

        productWithGSTPrice = Float.parseFloat(productPrice) + ((Float.parseFloat(productPrice)) * ((productCGSTAmount + productSGSTAmount) / 100));


        contentValues.put("userId", userId);
        contentValues.put("categoryId", categoryId);
        contentValues.put("categoryName", categoryName);
        contentValues.put("productCode", productCode);
        contentValues.put("productName", productName);
        contentValues.put("productPrice", productPrice);
        contentValues.put("openPrice", openPrice != null && !openPrice.isEmpty() ? openPrice : "off");
        contentValues.put("productUnit", unitName);
        contentValues.put("productCGST", productCGST);
        contentValues.put("productSGST", productSGST);
        contentValues.put("productWithGSTPrice", String.valueOf(productWithGSTPrice));
        contentValues.put("productStatus", 0);
        putOptionalColumn(contentValues, "subcategoryId", subcategoryId);

        db.update(PRODUCT_TABLE, contentValues, "productId=?", new String[]{productId});
        db.close();

    }

    public List<ProductCategoryResponse> getProductCategoryList() {

        List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + PRODUCT_CATEGORY_TABLE
                        + " WHERE categoryDeletedStatus = '0'"
                        + " ORDER BY IFNULL(categorySortOrder, 0) ASC, categoryId ASC",
                null);
        ProductCategoryResponse productCategoryResponse;
        while (cursor.moveToNext()) {
            productCategoryResponse = new ProductCategoryResponse();
            productCategoryResponse.setCategoryId(cursor.getString(cursor.getColumnIndex("categoryId")));
            productCategoryResponse.setCategoryName(cursor.getString(cursor.getColumnIndex("categoryName")));
            productCategoryResponse.setCategoryNetworkStatus(cursor.getString(cursor.getColumnIndex("categoryNetworkStatus")));
            productCategoryResponse.setCategoryDeletedStatus(cursor.getString(cursor.getColumnIndex("categoryDeletedStatus")));
            int foodTypeCol = cursor.getColumnIndex("foodTypeId");
            if (foodTypeCol >= 0 && !cursor.isNull(foodTypeCol)) {
                productCategoryResponse.setFoodTypeId(cursor.getString(foodTypeCol));
            }
            int sortCol = cursor.getColumnIndex("categorySortOrder");
            if (sortCol >= 0 && !cursor.isNull(sortCol)) {
                productCategoryResponse.setCategorySortOrder(cursor.getString(sortCol));
            }
            productCategoryResponseList.add(productCategoryResponse);
        }
        db.close();
        return productCategoryResponseList;
    }

    public List<ProductCategoryResponse> getProductCategoryNameList(String categoryName) {

        List<ProductCategoryResponse> productCategoryResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + PRODUCT_CATEGORY_TABLE + " WHERE categoryName = '" + categoryName + "' GROUP BY categoryName", null);
        ProductCategoryResponse productCategoryResponse;
        while (cursor.moveToNext()) {
            productCategoryResponse = new ProductCategoryResponse();
            productCategoryResponse.setCategoryId(cursor.getString(cursor.getColumnIndex("categoryId")));
            productCategoryResponse.setCategoryName(cursor.getString(cursor.getColumnIndex("categoryName")));
            productCategoryResponse.setCategoryNetworkStatus(cursor.getString(cursor.getColumnIndex("categoryNetworkStatus")));
            int foodTypeCol = cursor.getColumnIndex("foodTypeId");
            if (foodTypeCol >= 0 && !cursor.isNull(foodTypeCol)) {
                productCategoryResponse.setFoodTypeId(cursor.getString(foodTypeCol));
            }
            productCategoryResponseList.add(productCategoryResponse);
        }
        db.close();
        return productCategoryResponseList;
    }


    public List<ProductResponse> getHomeProductList(String categoryName, String tableNumber, String cartOrderStatus) {
        return getHomeProductList(categoryName, tableNumber, cartOrderStatus, null);
    }

    public List<ProductResponse> getHomeProductList(String categoryName, String tableNumber, String cartOrderStatus, String subcategoryId) {

        List<ProductResponse> productResponseList = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        String safeTable = tableNumber != null ? tableNumber : "";
        String safeOrder = cartOrderStatus != null ? cartOrderStatus : "";
        boolean allCategories = categoryName == null || categoryName.trim().isEmpty();

        String sql = "SELECT product.* FROM " + PRODUCT_TABLE + " product "
                + "LEFT JOIN " + PRODUCT_CATEGORY_TABLE + " ON " + PRODUCT_CATEGORY_TABLE + ".categoryName = product.categoryName "
                + "WHERE IFNULL(product.productDeletedStatus, '0') = '0'";

        List<String> argList = new ArrayList<>();
        if (!allCategories) {
            sql += " AND product.categoryName = ?";
            argList.add(categoryName);
        }
        if (subcategoryId != null && !subcategoryId.trim().isEmpty()) {
            sql += " AND product.subcategoryId = ?";
            argList.add(subcategoryId);
        }
        sql += " GROUP BY product.productName";
        String[] args = argList.toArray(new String[0]);

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, args);
            while (cursor.moveToNext()) {
                ProductResponse productResponse = mapHomeProductRow(cursor);

                String productId = productResponse.getProductId();
                Cursor cartCursor = null;
                try {
                    cartCursor = db.rawQuery(
                            "SELECT SUM(CAST(productQuantity AS REAL)) AS totalQty FROM " + CART_PRODUCT_TABLE
                                    + " WHERE productId = ? AND noOfTable = ? AND cartOrderStatus = ?",
                            new String[]{productId, safeTable, safeOrder});
                    if (cartCursor.moveToFirst()) {
                        int qtyIdx = cartCursor.getColumnIndex("totalQty");
                        if (qtyIdx >= 0 && !cartCursor.isNull(qtyIdx)) {
                            double totalQty = cartCursor.getDouble(qtyIdx);
                            if (totalQty > 0d) {
                                productResponse.setProductCartQuantity(String.valueOf((int) totalQty));
                            }
                        }
                    }
                } finally {
                    if (cartCursor != null) {
                        cartCursor.close();
                    }
                }

                productResponseList.add(productResponse);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return productResponseList;

    }

    private ProductResponse mapHomeProductRow(Cursor cursor) {
        ProductResponse productResponse = new ProductResponse();
        productResponse.setProductId(cursor.getString(cursor.getColumnIndex("productId")));
        productResponse.setCategoryId(cursor.getString(cursor.getColumnIndex("categoryId")));
        productResponse.setCategoryName(cursor.getString(cursor.getColumnIndex("categoryName")));
        productResponse.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
        productResponse.setProductCode(cursor.getString(cursor.getColumnIndex("productCode")));
        productResponse.setProductPrice(cursor.getString(cursor.getColumnIndex("productPrice")));
        productResponse.setProductUnit(cursor.getString(cursor.getColumnIndex("productUnit")));
        productResponse.setProductCGST(cursor.getString(cursor.getColumnIndex("productCGST")));
        productResponse.setProductSGST(cursor.getString(cursor.getColumnIndex("productSGST")));
        productResponse.setProductStatus(cursor.getString(cursor.getColumnIndex("productStatus")));
        int openPriceCol = cursor.getColumnIndex("openPrice");
        if (openPriceCol >= 0 && !cursor.isNull(openPriceCol)) {
            productResponse.setOpenPrice(cursor.getString(openPriceCol));
        } else {
            productResponse.setOpenPrice("off");
        }
        int subcategoryCol = cursor.getColumnIndex("subcategoryId");
        if (subcategoryCol >= 0 && !cursor.isNull(subcategoryCol)) {
            productResponse.setSubcategoryId(cursor.getString(subcategoryCol));
        }
        return productResponse;
    }

    public List<ProductCartResponse> getCartProductDetails(String productId, String tableNumber, String cartOrderStatus) {
        return getCartProductDetails(productId, null, tableNumber, cartOrderStatus);
    }

    public List<ProductCartResponse> getCartProductDetails(String productId, String portionId, String tableNumber, String cartOrderStatus) {

        List<ProductCartResponse> productCartResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor;
        if (portionId != null && !portionId.trim().isEmpty()) {
            cursor = db.rawQuery(
                    "SELECT cart_product.*, IFNULL(product.openPrice, 'off') AS openPrice FROM " + CART_PRODUCT_TABLE
                            + " LEFT JOIN " + PRODUCT_TABLE + " ON product.productId = cart_product.productId"
                            + " WHERE cart_product.productId = ? AND cart_product.portionId = ?"
                            + " AND cart_product.noOfTable = ? AND cart_product.cartOrderStatus = ?",
                    new String[]{productId, portionId, tableNumber, cartOrderStatus});
        } else {
            cursor = db.rawQuery(
                    "SELECT cart_product.*, IFNULL(product.openPrice, 'off') AS openPrice FROM " + CART_PRODUCT_TABLE
                            + " LEFT JOIN " + PRODUCT_TABLE + " ON product.productId = cart_product.productId"
                            + " WHERE cart_product.productId = ? AND (cart_product.portionId IS NULL OR cart_product.portionId = '')"
                            + " AND cart_product.noOfTable = ? AND cart_product.cartOrderStatus = ?",
                    new String[]{productId, tableNumber, cartOrderStatus});
        }
        ProductCartResponse productResponse;
        while (cursor.moveToNext()) {
            productResponse = new ProductCartResponse();
            productResponse.setCartId(cursor.getString(cursor.getColumnIndex("cartId")));
            productResponse.setProductId(cursor.getString(cursor.getColumnIndex("productId")));
            productResponse.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
            productResponse.setProductOldPrice(cursor.getString(cursor.getColumnIndex("productOldPrice")));
            productResponse.setProductNewPrice(cursor.getString(cursor.getColumnIndex("productNewPrice")));
            productResponse.setProductUnit(cursor.getString(cursor.getColumnIndex("productUnit")));
            productResponse.setProductQuantity(cursor.getString(cursor.getColumnIndex("productQuantity")));
            productResponse.setProductCGST(cursor.getString(cursor.getColumnIndex("productCGST")));
            productResponse.setProductSGST(cursor.getString(cursor.getColumnIndex("productSGST")));
            productResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            productResponse.setCartDiscount(cursor.getString(cursor.getColumnIndex("cartDiscount")));
            productResponse.setCartDiscountType(cursor.getString(cursor.getColumnIndex("cartDiscountType")));
            productResponse.setCartOrderStatus(cursor.getString(cursor.getColumnIndex("cartOrderStatus")));
            productResponse.setCartStatus(cursor.getString(cursor.getColumnIndex("cartStatus")));
            mapCartLineSnapshots(cursor, productResponse);
            productCartResponseList.add(productResponse);
        }
        cursor.close();
        db.close();
        return productCartResponseList;

    }

    public List<ProductCartResponse> getCartProductList(String tableNumber, String cartOrderStatus) {

        List<ProductCartResponse> productCartResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT cart_product.*, IFNULL(product.openPrice, 'off') AS openPrice FROM " + CART_PRODUCT_TABLE
                        + " LEFT JOIN " + PRODUCT_TABLE + " ON product.productId = cart_product.productId"
                        + " WHERE cart_product.noOfTable = ? AND cart_product.cartOrderStatus = ?",
                new String[]{tableNumber, cartOrderStatus});
        ProductCartResponse productResponse;
        while (cursor.moveToNext()) {
            productResponse = new ProductCartResponse();
            productResponse.setCartId(cursor.getString(cursor.getColumnIndex("cartId")));
            productResponse.setProductId(cursor.getString(cursor.getColumnIndex("productId")));
            productResponse.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
            productResponse.setProductOldPrice(cursor.getString(cursor.getColumnIndex("productOldPrice")));
            productResponse.setProductNewPrice(cursor.getString(cursor.getColumnIndex("productNewPrice")));
            productResponse.setProductUnit(cursor.getString(cursor.getColumnIndex("productUnit")));
            productResponse.setProductQuantity(cursor.getString(cursor.getColumnIndex("productQuantity")));
            productResponse.setProductCGST(cursor.getString(cursor.getColumnIndex("productCGST")));
            productResponse.setProductSGST(cursor.getString(cursor.getColumnIndex("productSGST")));
            productResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            productResponse.setCartDiscount(cursor.getString(cursor.getColumnIndex("cartDiscount")));
            productResponse.setCartDiscountType(cursor.getString(cursor.getColumnIndex("cartDiscountType")));
            productResponse.setCartOrderStatus(cursor.getString(cursor.getColumnIndex("cartOrderStatus")));
            productResponse.setCartStatus(cursor.getString(cursor.getColumnIndex("cartStatus")));
            mapCartLineSnapshots(cursor, productResponse);
            productCartResponseList.add(productResponse);
        }
        db.close();
        return productCartResponseList;

    }

    public List<ProductResponse> getAllProductList(String tableNumber, String cartOrderStatus) {

        List<ProductResponse> productResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT product.*, ps.subcategoryName AS subcategoryName "
                        + "FROM " + PRODUCT_TABLE + " product "
                        + "LEFT JOIN " + PRODUCT_CATEGORY_TABLE + " ON " + PRODUCT_CATEGORY_TABLE + ".categoryName = product.categoryName "
                        + "LEFT JOIN " + PRODUCT_SUBCATEGORY_TABLE + " ps ON product.subcategoryId = ps.subcategoryId "
                        + "AND IFNULL(ps.subcategoryDeletedStatus, '0') = '0' "
                        + "WHERE IFNULL(product.categoryName, '') != '' AND IFNULL(product.productDeletedStatus, '0') = '0' "
                        + "GROUP BY product.productName ORDER BY product.productId",
                null);
        ProductResponse productResponse;
        while (cursor.moveToNext()) {
            productResponse = new ProductResponse();
            String productId = cursor.getString(cursor.getColumnIndex("productId"));

            Cursor cartCursor = db.rawQuery("SELECT * FROM cart_product LEFT JOIN product ON product.productId = cart_product.productId WHERE cart_product.productId = '" + productId + "' AND noOfTable = '" + tableNumber + "' AND cart_product.cartOrderStatus = '" + cartOrderStatus + "'", null);
            while (cartCursor.moveToNext()) {
                productResponse.setProductCartQuantity(cartCursor.getString(cartCursor.getColumnIndex("productQuantity")));
            }
            productResponse.setProductId(cursor.getString(cursor.getColumnIndex("productId")));
            productResponse.setCategoryId(cursor.getString(cursor.getColumnIndex("categoryId")));
            productResponse.setCategoryName(cursor.getString(cursor.getColumnIndex("categoryName")));
            productResponse.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
            productResponse.setProductCode(cursor.getString(cursor.getColumnIndex("productCode")));
            productResponse.setProductPrice(cursor.getString(cursor.getColumnIndex("productPrice")));
            productResponse.setProductUnit(cursor.getString(cursor.getColumnIndex("productUnit")));
            productResponse.setProductCGST(cursor.getString(cursor.getColumnIndex("productCGST")));
            productResponse.setProductSGST(cursor.getString(cursor.getColumnIndex("productSGST")));
            productResponse.setProductStatus(cursor.getString(cursor.getColumnIndex("productStatus")));
            int openPriceAllCol = cursor.getColumnIndex("openPrice");
            if (openPriceAllCol >= 0 && !cursor.isNull(openPriceAllCol)) {
                productResponse.setOpenPrice(cursor.getString(openPriceAllCol));
            } else {
                productResponse.setOpenPrice("off");
            }
            int subcategoryIdCol = cursor.getColumnIndex("subcategoryId");
            if (subcategoryIdCol >= 0 && !cursor.isNull(subcategoryIdCol)) {
                productResponse.setSubcategoryId(cursor.getString(subcategoryIdCol));
            }
            int subcategoryNameCol = cursor.getColumnIndex("subcategoryName");
            if (subcategoryNameCol >= 0 && !cursor.isNull(subcategoryNameCol)) {
                productResponse.setSubcategoryName(cursor.getString(subcategoryNameCol));
            }
            productResponseList.add(productResponse);
        }
        db.close();
        return productResponseList;

    }

    /**
     * Fast billing search: parameterized SQL, single cart JOIN, limited results.
     * Matches name / code / category / price (same fields as previous in-memory search).
     */
    public List<ProductResponse> searchProducts(String query, String tableNumber, String cartOrderStatus) {
        List<ProductResponse> productResponseList = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return productResponseList;
        }

        String trimmed = query.trim();
        String contains = "%" + escapeLike(trimmed) + "%";
        String prefix = escapeLike(trimmed) + "%";
        String safeTable = tableNumber != null ? tableNumber : "";
        String safeOrder = cartOrderStatus != null ? cartOrderStatus : "";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql = "SELECT p.productId, p.categoryId, p.categoryName, p.productName, p.productCode, "
                    + "p.productPrice, p.productUnit, p.productCGST, p.productSGST, p.productStatus, "
                    + "c.productQuantity AS productCartQuantity "
                    + "FROM " + PRODUCT_TABLE + " p "
                    + "LEFT JOIN " + CART_PRODUCT_TABLE + " c "
                    + "ON c.productId = CAST(p.productId AS TEXT) AND c.noOfTable = ? AND c.cartOrderStatus = ? "
                    + "WHERE IFNULL(p.productDeletedStatus, '0') = '0' "
                    + "AND IFNULL(p.categoryName, '') != '' "
                    + "AND ("
                    + "  IFNULL(p.productName, '') LIKE ? ESCAPE '\\' "
                    + "  OR IFNULL(p.productCode, '') LIKE ? ESCAPE '\\' "
                    + "  OR IFNULL(p.categoryName, '') LIKE ? ESCAPE '\\' "
                    + "  OR IFNULL(p.productPrice, '') LIKE ? ESCAPE '\\' "
                    + ") "
                    + "GROUP BY p.productName "
                    + "ORDER BY "
                    + "  CASE "
                    + "    WHEN IFNULL(p.productCode, '') LIKE ? ESCAPE '\\' THEN 0 "
                    + "    WHEN IFNULL(p.productName, '') LIKE ? ESCAPE '\\' THEN 1 "
                    + "    ELSE 2 "
                    + "  END, "
                    + "  p.productName "
                    + "LIMIT 100";

            cursor = db.rawQuery(sql, new String[]{
                    safeTable, safeOrder,
                    contains, contains, contains, contains,
                    prefix, prefix
            });

            while (cursor.moveToNext()) {
                ProductResponse productResponse = new ProductResponse();
                productResponse.setProductId(cursor.getString(cursor.getColumnIndex("productId")));
                productResponse.setCategoryId(cursor.getString(cursor.getColumnIndex("categoryId")));
                productResponse.setCategoryName(cursor.getString(cursor.getColumnIndex("categoryName")));
                productResponse.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
                productResponse.setProductCode(cursor.getString(cursor.getColumnIndex("productCode")));
                productResponse.setProductPrice(cursor.getString(cursor.getColumnIndex("productPrice")));
                productResponse.setProductUnit(cursor.getString(cursor.getColumnIndex("productUnit")));
                productResponse.setProductCGST(cursor.getString(cursor.getColumnIndex("productCGST")));
                productResponse.setProductSGST(cursor.getString(cursor.getColumnIndex("productSGST")));
                productResponse.setProductStatus(cursor.getString(cursor.getColumnIndex("productStatus")));
                int openPriceSearchCol = cursor.getColumnIndex("openPrice");
                if (openPriceSearchCol >= 0 && !cursor.isNull(openPriceSearchCol)) {
                    productResponse.setOpenPrice(cursor.getString(openPriceSearchCol));
                } else {
                    productResponse.setOpenPrice("off");
                }
                int cartQtyIdx = cursor.getColumnIndex("productCartQuantity");
                if (cartQtyIdx >= 0 && !cursor.isNull(cartQtyIdx)) {
                    productResponse.setProductCartQuantity(cursor.getString(cartQtyIdx));
                }
                productResponseList.add(productResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return productResponseList;
    }

    private static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public List<ProductResponse> getAllDESCProductList() {

        List<ProductResponse> productResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM product LEFT JOIN product_category ON product_category.categoryName = product.categoryName GROUP BY product.productName ORDER BY product.productId DESC LIMIT 1", null);
        ProductResponse productResponse;
        while (cursor.moveToNext()) {
            productResponse = new ProductResponse();
            productResponse.setProductId(cursor.getString(cursor.getColumnIndex("productId")));
            productResponse.setCategoryId(cursor.getString(cursor.getColumnIndex("categoryId")));
            productResponse.setCategoryName(cursor.getString(cursor.getColumnIndex("categoryName")));
            productResponse.setProductCode(cursor.getString(cursor.getColumnIndex("productCode")));
            productResponse.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
            productResponse.setProductPrice(cursor.getString(cursor.getColumnIndex("productPrice")));
            productResponse.setProductUnit(cursor.getString(cursor.getColumnIndex("productUnit")));
            productResponse.setProductCGST(cursor.getString(cursor.getColumnIndex("productCGST")));
            productResponse.setProductSGST(cursor.getString(cursor.getColumnIndex("productSGST")));
            productResponse.setProductStatus(cursor.getString(cursor.getColumnIndex("productStatus")));
            productResponseList.add(productResponse);
        }
        db.close();
        return productResponseList;

    }

    public List<ProductResponse> getProductDetail(String productId) {

        List<ProductResponse> productResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM product LEFT JOIN product_category ON product_category.categoryName = product.categoryName WHERE product.productId= '" + productId + "'", null);
        ProductResponse productResponse;
        while (cursor.moveToNext()) {
            productResponse = new ProductResponse();
            productResponse.setProductId(cursor.getString(cursor.getColumnIndex("productId")));
            productResponse.setCategoryId(cursor.getString(cursor.getColumnIndex("categoryId")));
            productResponse.setCategoryName(cursor.getString(cursor.getColumnIndex("categoryName")));
            productResponse.setProductCode(cursor.getString(cursor.getColumnIndex("productCode")));
            productResponse.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
            productResponse.setProductPrice(cursor.getString(cursor.getColumnIndex("productPrice")));
            productResponse.setProductUnit(cursor.getString(cursor.getColumnIndex("productUnit")));
            productResponse.setProductCGST(cursor.getString(cursor.getColumnIndex("productCGST")));
            productResponse.setProductSGST(cursor.getString(cursor.getColumnIndex("productSGST")));
            productResponse.setProductStatus(cursor.getString(cursor.getColumnIndex("productStatus")));
            int openPriceCol = cursor.getColumnIndex("openPrice");
            if (openPriceCol >= 0 && !cursor.isNull(openPriceCol)) {
                productResponse.setOpenPrice(cursor.getString(openPriceCol));
            } else {
                productResponse.setOpenPrice("off");
            }
            int subcategoryCol = cursor.getColumnIndex("subcategoryId");
            if (subcategoryCol >= 0 && !cursor.isNull(subcategoryCol)) {
                productResponse.setSubcategoryId(cursor.getString(subcategoryCol));
            }
            productResponseList.add(productResponse);
        }
        db.close();
        return productResponseList;

    }

    public List<CompanyResponse> getCompanyDetails() {

        List<CompanyResponse> companyResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + COMPANY_TABLE, null);
        CompanyResponse companyResponse;
        while (cursor.moveToNext()) {
            companyResponse = new CompanyResponse();
            companyResponse.setCompanyId(cursor.getString(cursor.getColumnIndex("companyId")));
            companyResponse.setCompanyName(cursor.getString(cursor.getColumnIndex("companyName")));
            companyResponse.setCashierName(cursor.getString(cursor.getColumnIndex("cashierName")));
            companyResponse.setCompanyMobile(cursor.getString(cursor.getColumnIndex("companyMobile")));
            companyResponse.setCompanyAddress(cursor.getString(cursor.getColumnIndex("companyAddress")));
            int shopName1Idx = cursor.getColumnIndex("shopName1");
            if (shopName1Idx >= 0) {
                companyResponse.setShopName1(cursor.getString(shopName1Idx));
            }
            int shopName2Idx = cursor.getColumnIndex("shopName2");
            if (shopName2Idx >= 0) {
                companyResponse.setShopName2(cursor.getString(shopName2Idx));
            }
            int addressLine1Idx = cursor.getColumnIndex("addressLine1");
            if (addressLine1Idx >= 0) {
                companyResponse.setAddressLine1(cursor.getString(addressLine1Idx));
            }
            int addressLine2Idx = cursor.getColumnIndex("addressLine2");
            if (addressLine2Idx >= 0) {
                companyResponse.setAddressLine2(cursor.getString(addressLine2Idx));
            }
            int addressLine3Idx = cursor.getColumnIndex("addressLine3");
            if (addressLine3Idx >= 0) {
                companyResponse.setAddressLine3(cursor.getString(addressLine3Idx));
            }
            int phoneNo1Idx = cursor.getColumnIndex("phoneNo1");
            if (phoneNo1Idx >= 0) {
                companyResponse.setPhoneNo1(cursor.getString(phoneNo1Idx));
            }
            int phoneNo2Idx = cursor.getColumnIndex("phoneNo2");
            if (phoneNo2Idx >= 0) {
                companyResponse.setPhoneNo2(cursor.getString(phoneNo2Idx));
            }
            companyResponse.setCurrencyName(cursor.getString(cursor.getColumnIndex("currencyName")));
            companyResponse.setTableStatus(cursor.getString(cursor.getColumnIndex("tableStatus")));
            companyResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            companyResponse.setCountryName(cursor.getString(cursor.getColumnIndex("countryName")));
            companyResponse.setStateName(cursor.getString(cursor.getColumnIndex("stateName")));
            companyResponse.setGstStatus(cursor.getString(cursor.getColumnIndex("gstStatus")));
            companyResponse.setGstNumber(cursor.getString(cursor.getColumnIndex("gstNumber")));
            companyResponse.setShopCGST(cursor.getString(cursor.getColumnIndex("shopCGST")));
            companyResponse.setShopSGST(cursor.getString(cursor.getColumnIndex("shopSGST")));
            companyResponse.setPanNumber(cursor.getString(cursor.getColumnIndex("panNumber")));
            companyResponse.setCompanyFssis(cursor.getString(cursor.getColumnIndex("companyFssis")));
            companyResponse.setCompanyLogo(cursor.getString(cursor.getColumnIndex("companyLogo")));
            companyResponse.setPaymentLogo(cursor.getString(cursor.getColumnIndex("paymentLogo")));
            companyResponse.setCompanyStatus(cursor.getString(cursor.getColumnIndex("companyStatus")));
            companyResponseList.add(companyResponse);
        }
        db.close();
        return companyResponseList;

    }

    public List<PrinterSettingResponse> getPrinterSettingDetails() {

        List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + PRINTER_SETTING_TABLE, null);
        PrinterSettingResponse printerSettingResponse;
        while (cursor.moveToNext()) {
            printerSettingResponse = new PrinterSettingResponse();
            printerSettingResponse.setSettingId(cursor.getString(cursor.getColumnIndex("settingId")));
            printerSettingResponse.setPrinterName(cursor.getString(cursor.getColumnIndex("printerName")));
            printerSettingResponse.setInvoicePrefix(cursor.getString(cursor.getColumnIndex("invoicePrefix")));
            printerSettingResponse.setInvoiceTitle(cursor.getString(cursor.getColumnIndex("invoiceTitle")));
            printerSettingResponse.setInvoiceTermsCondition(cursor.getString(cursor.getColumnIndex("invoiceTermsCondition")));
            printerSettingResponse.setPrinterFeedLines(cursor.getString(cursor.getColumnIndex("printerFeedLines")));
            printerSettingResponse.setKotPrinterFeedLines(cursor.getString(cursor.getColumnIndex("KotPrinterFeedLines")));
            try {
                printerSettingResponse.setKOTPrinterName(cursor.getString(cursor.getColumnIndex("KOTPrinterName")));
                printerSettingResponse.setLogoUse(cursor.getString(cursor.getColumnIndex("logoUse")));
                printerSettingResponse.setPaymentUse(cursor.getString(cursor.getColumnIndex("paymentUse")));
                printerSettingResponse.setCustomerUse(cursor.getString(cursor.getColumnIndex("customerUse")));
                printerSettingResponse.setProductQuantityUpdate(cursor.getString(cursor.getColumnIndex("productQuantityUpdate")));
                int duplicateBillIdx = cursor.getColumnIndex("duplicateBillUse");
                if (duplicateBillIdx >= 0) {
                    printerSettingResponse.setDuplicateBillUse(cursor.getString(duplicateBillIdx));
                } else {
                    printerSettingResponse.setDuplicateBillUse("off");
                }
                printerSettingResponse.setBluetoothAddress(cursor.getString(cursor.getColumnIndex("bluetoothAddress")));
                printerSettingResponse.setBluetoothKOTAddress(cursor.getString(cursor.getColumnIndex("bluetoothKOTAddress")));
                mapStringColumn(cursor, "billConnectionType", printerSettingResponse::setBillConnectionType);
                mapStringColumn(cursor, "kotConnectionType", printerSettingResponse::setKotConnectionType);
                mapStringColumn(cursor, "billPrinterIp", printerSettingResponse::setBillPrinterIp);
                mapStringColumn(cursor, "kotPrinterIp", printerSettingResponse::setKotPrinterIp);
                mapStringColumn(cursor, "billPrinterPort", printerSettingResponse::setBillPrinterPort);
                mapStringColumn(cursor, "kotPrinterPort", printerSettingResponse::setKotPrinterPort);
                mapStringColumn(cursor, "billUsbDeviceKey", printerSettingResponse::setBillUsbDeviceKey);
                mapStringColumn(cursor, "kotUsbDeviceKey", printerSettingResponse::setKotUsbDeviceKey);
                mapStringColumn(cursor, "supportsCutter", printerSettingResponse::setSupportsCutter);
                mapStringColumn(cursor, "supportsCashDrawer", printerSettingResponse::setSupportsCashDrawer);
                mapStringColumn(cursor, "autoCut", printerSettingResponse::setAutoCut);
                mapStringColumn(cursor, "autoOpenCashDrawer", printerSettingResponse::setAutoOpenCashDrawer);
                mapStringColumn(cursor, "drawerOpenMode", printerSettingResponse::setDrawerOpenMode);
                mapStringColumn(cursor, "drawerPin", printerSettingResponse::setDrawerPin);
                mapStringColumn(cursor, "drawerPulseOn", printerSettingResponse::setDrawerPulseOn);
                mapStringColumn(cursor, "drawerPulseOff", printerSettingResponse::setDrawerPulseOff);
                mapStringColumn(cursor, "cutCommand", printerSettingResponse::setCutCommand);
                mapStringColumn(cursor, "printerModel", printerSettingResponse::setPrinterModel);
            } catch (Exception e) {
                e.printStackTrace();
            }
            printerSettingResponse.setSettingStatus(cursor.getString(cursor.getColumnIndex("settingStatus")));
            printerSettingResponseList.add(printerSettingResponse);
        }
        db.close();
        return printerSettingResponseList;

    }

    public void deleteCategory(String categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("categoryDeletedStatus", "1");
        contentValues.put("categoryStatus", "0");
        db.update(PRODUCT_CATEGORY_TABLE, contentValues, "categoryId = ?", new String[]{categoryId});
        ContentValues productDirty = new ContentValues();
        productDirty.put("productStatus", 0);
        db.update(PRODUCT_TABLE, productDirty, "categoryId=?", new String[]{categoryId});
        db.close();
    }

    public void deleteCartProduct(String cartId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(CART_COMBO_ITEM_TABLE, "cartId = ?", new String[]{cartId});
        db.delete(CART_PRODUCT_TABLE, "cartId = ?", new String[]{cartId});
        db.close();
    }

    public void clearCart(String tableNumber, String cartOrderStatus) {
        if (tableNumber == null || cartOrderStatus == null) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL(
                    "DELETE FROM " + CART_COMBO_ITEM_TABLE
                            + " WHERE cartId IN (SELECT cartId FROM " + CART_PRODUCT_TABLE
                            + " WHERE noOfTable = ? AND cartOrderStatus = ?)",
                    new String[]{tableNumber, cartOrderStatus});
            db.delete(CART_PRODUCT_TABLE, "noOfTable = ? AND cartOrderStatus = ?",
                    new String[]{tableNumber, cartOrderStatus});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void deleteProduct(String productId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("productDeletedStatus", "1");
        contentValues.put("productStatus", "0");
        db.update(PRODUCT_TABLE, contentValues, "productId = ?", new String[]{productId});
        ContentValues portionDirty = new ContentValues();
        portionDirty.put("portionStatus", 0);
        db.update(PRODUCT_PORTION_TABLE, portionDirty, "productId=?", new String[]{productId});
        ContentValues comboItemDirty = new ContentValues();
        comboItemDirty.put("comboItemStatus", 0);
        db.update(COMBO_ITEM_TABLE, comboItemDirty, "productId=?", new String[]{productId});
        db.execSQL("UPDATE " + COMBO_TABLE + " SET comboStatus = 0 WHERE comboId IN (SELECT comboId FROM "
                + COMBO_ITEM_TABLE + " WHERE productId=?)", new String[]{productId});
        db.close();
        /*db.delete(PRODUCT_TABLE, "productId = ?", new String[]{productId});
        db.close();*/
    }

    /**
     * Clears local business data for a cloud fetch without DROP TABLE.
     * Schema is preserved (production-safe; no destructive migration).
     */
    public void resetTables(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.delete(PRODUCT_CATEGORY_TABLE, null, null);
            db.delete(PRODUCT_SUBCATEGORY_TABLE, null, null);
            db.delete(PRODUCT_PORTION_TABLE, null, null);
            db.delete(PRODUCT_TABLE, null, null);
            db.delete(COMBO_ITEM_TABLE, null, null);
            db.delete(COMBO_TABLE, null, null);
            db.delete(CART_COMBO_ITEM_TABLE, null, null);
            db.delete(CART_PRODUCT_TABLE, null, null);
            db.delete(INVOICE_COMBO_ITEM_TABLE, null, null);
            db.delete(INVOICE_TABLE, null, null);
            db.delete(INVOICE_PRODUCT_TABLE, null, null);
            db.delete(COMPANY_TABLE, null, null);
            db.delete(PRINTER_SETTING_TABLE, null, null);
            db.delete(MEMBER_TABLE, null, null);
            db.delete(MEMBER_PAYMENT_TABLE, null, null);
            db.delete(MESS_INVOICE_TABLE, null, null);
            db.delete(INVENTORY_TABLE, null, null);
            db.delete(EXPENSES_TABLE, null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Count invoices not yet confirmed synced to server (invoiceStatus = 0). */
    public int countUnsyncedInvoices() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + INVOICE_TABLE + " WHERE invoiceStatus = 0" + andBranchScope(null), null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public Cursor getUnSynchronizeCategory(int status) {

        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + PRODUCT_CATEGORY_TABLE + " WHERE categoryStatus = '" + status + "' ";
        Cursor cursor = db.rawQuery(sql, null);
        return cursor;
    }

    public void updateSyncCategory(String categoryId, int categoryStatus) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("categoryStatus", categoryStatus);
        db.update(PRODUCT_CATEGORY_TABLE, contentValues, "categoryId=?", new String[]{categoryId});
        db.close();

    }

    public Cursor getUnSynchronizeProduct(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + PRODUCT_TABLE + " WHERE productStatus = '" + status + "' ";
        Cursor cursor = db.rawQuery(sql, null);
        return cursor;
    }

    /** Counts rows waiting to upload. Uses COUNT(*) so the sync screen cannot miss pending edits. */
    public int countUnsyncedRows(String table, String statusColumn) {
        if (table == null || statusColumn == null) {
            return 0;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + table
                            + " WHERE " + statusColumn + " = 0 OR " + statusColumn + " = '0'",
                    null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } catch (Exception e) {
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public int countAllRows(String table) {
        if (table == null) {
            return 0;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } catch (Exception e) {
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void updateSyncProduct(String productId, int productStatus) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("productStatus", productStatus);
        db.update(PRODUCT_TABLE, contentValues, "productId=?", new String[]{productId});
        db.close();

    }

    public Cursor getUnSynchronizeSubcategory(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT s.*, c.categoryNetworkStatus FROM " + PRODUCT_SUBCATEGORY_TABLE + " s "
                + "LEFT JOIN " + PRODUCT_CATEGORY_TABLE + " c ON c.categoryId = s.categoryId "
                + "WHERE s.subcategoryStatus = '" + status + "'";
        return db.rawQuery(sql, null);
    }

    public void updateSyncSubcategory(String subcategoryId, int subcategoryStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("subcategoryStatus", subcategoryStatus);
        db.update(PRODUCT_SUBCATEGORY_TABLE, contentValues, "subcategoryId=?", new String[]{subcategoryId});
        db.close();
    }

    public Cursor getUnSynchronizePortionMaster(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + PORTION_MASTER_TABLE + " WHERE portionMasterStatus = '" + status + "'",
                null);
    }

    public void updateSyncPortionMaster(String portionMasterId, int portionMasterStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("portionMasterStatus", portionMasterStatus);
        db.update(PORTION_MASTER_TABLE, contentValues, "portionMasterId=?", new String[]{portionMasterId});
        db.close();
    }

    public Cursor getUnSynchronizePortion(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT pp.*, p.productNetworkStatus, pm.portionMasterNetworkStatus FROM "
                + PRODUCT_PORTION_TABLE + " pp "
                + "LEFT JOIN " + PRODUCT_TABLE + " p ON p.productId = pp.productId "
                + "LEFT JOIN " + PORTION_MASTER_TABLE + " pm ON pm.portionMasterId = pp.portionMasterId "
                + "WHERE pp.portionStatus = '" + status + "'";
        return db.rawQuery(sql, null);
    }

    public void updateSyncPortion(String portionId, int portionStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("portionStatus", portionStatus);
        db.update(PRODUCT_PORTION_TABLE, contentValues, "portionId=?", new String[]{portionId});
        db.close();
    }

    public Cursor getUnSynchronizePrinterSetting(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + PRINTER_SETTING_TABLE + " WHERE settingStatus = '" + status + "' ";
        Cursor cursor = db.rawQuery(sql, null);
        return cursor;
    }

    public void updateSynchronizePrinterSetting(String settingId, int settingStatus) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("settingStatus", settingStatus);
        db.update(PRINTER_SETTING_TABLE, contentValues, "settingId=?", new String[]{settingId});
        db.close();

    }

    public Cursor getUnSynchronizeCompanyDetails(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + COMPANY_TABLE + " WHERE companyStatus = '" + status + "' ";
        Cursor cursor = db.rawQuery(sql, null);
        return cursor;
    }

    public void updateSynchronizeCompanyDetails(String companyId, int companyStatus) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("companyStatus", companyStatus);
        db.update(COMPANY_TABLE, contentValues, "companyId=?", new String[]{companyId});
        db.close();

    }

    public Cursor getUnSynchronizeInvoice(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + INVOICE_TABLE + " WHERE invoiceStatus = '" + status + "' " + andBranchScope(null);
        Cursor cursor = db.rawQuery(sql, null);
        return cursor;
    }

    public void updateSyncInvoice(String invoiceId, int invoiceStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("invoiceStatus", invoiceStatus);
        db.update(INVOICE_TABLE, contentValues, "invoiceId=?", new String[]{invoiceId});
        db.close();
    }

    public Cursor getUnSynchronizeInvoiceProduct(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + INVOICE_PRODUCT_TABLE + " WHERE invoiceProductStatus = '" + status + "' ";
        Cursor cursor = db.rawQuery(sql, null);
        return cursor;
    }

    public void updateSyncInvoiceProduct(String invoiceProductId, int invoiceProductStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("invoiceProductStatus", invoiceProductStatus);
        db.update(INVOICE_PRODUCT_TABLE, contentValues, "invoiceProductId=?", new String[]{invoiceProductId});
        db.close();
    }

    public boolean addInvoice(InvoiceResponse invoiceResponse) {

        String networkStatus = invoiceResponse.getInvoiceNetworkStatus();
        if (invoiceNetworkStatusExists(networkStatus)) {
            // Already present from a prior pull/sync — do not insert a duplicate bill
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("noOfTable", invoiceResponse.getNoOfTable());
        contentValues.put("invoiceNumber", invoiceResponse.getInvoiceNumber());
        contentValues.put("customerName", invoiceResponse.getCustomerName());
        contentValues.put("customerMobile", invoiceResponse.getCustomerMobile());
        contentValues.put("customerAddress", invoiceResponse.getCustomerAddress());
        contentValues.put("subTotal", invoiceResponse.getSubTotal());
        contentValues.put("totalGSTAmount", invoiceResponse.getTotalGSTAmount());
        contentValues.put("discount", invoiceResponse.getDiscount());
        contentValues.put("discountType", invoiceResponse.getDiscountType());
        contentValues.put("totalAmount", invoiceResponse.getTotalAmount());
        contentValues.put("paymentMode", invoiceResponse.getPaymentMode());
        contentValues.put("invoiceDate", invoiceResponse.getInvoiceDate());
        contentValues.put("invoiceOrderStatus", "completed");
        contentValues.put("invoiceNetworkStatus", networkStatus);
        contentValues.put("invoiceType", invoiceResponse.getInvoiceType());
        contentValues.put("invoiceStatus", invoiceResponse.getInvoiceStatus());
        putOptionalColumn(contentValues, "organizationId", invoiceResponse.getOrganizationId());
        putOptionalColumn(contentValues, "branchId", invoiceResponse.getBranchId());
        putOptionalColumn(contentValues, "deviceId", invoiceResponse.getDeviceId());
        if (!contentValues.containsKey("organizationId")) {
            BranchSession.applyScope(contentValues);
        }

        long rowId = db.insertWithOnConflict(INVOICE_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();

        return rowId != -1;

    }

    public boolean addInvoiceProduct(InvoiceProductResponse invoiceProductResponse) {

        String networkStatus = invoiceProductResponse.getInvoiceProductNetworkStatus();
        if (invoiceProductNetworkStatusExists(networkStatus)) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("invoiceNumber", invoiceProductResponse.getInvoiceNumber());
        contentValues.put("productName", invoiceProductResponse.getProductName());
        contentValues.put("productPrice", invoiceProductResponse.getProductPrice());
        contentValues.put("productUnit", invoiceProductResponse.getProductUnit());
        contentValues.put("productCGST",
                invoiceProductResponse.getProductCGST() != null ? invoiceProductResponse.getProductCGST() : "");
        contentValues.put("productSGST",
                invoiceProductResponse.getProductSGST() != null ? invoiceProductResponse.getProductSGST() : "");
        contentValues.put("productQuantity", invoiceProductResponse.getProductQuantity());
        contentValues.put("productStatus", invoiceProductResponse.getProductStatus());
        contentValues.put("invoiceProductNetworkStatus", networkStatus);
        contentValues.put("invoiceProductStatus", invoiceProductResponse.getInvoiceProductStatus());
        putOptionalColumn(contentValues, "portionId", invoiceProductResponse.getPortionId());
        putOptionalColumn(contentValues, "portionName", invoiceProductResponse.getPortionName());
        contentValues.put("invoiceItemType", CartItemType.normalize(invoiceProductResponse.getInvoiceItemType()));
        putOptionalColumn(contentValues, "comboId", invoiceProductResponse.getComboId());
        putOptionalColumn(contentValues, "snapshotComboComponents", invoiceProductResponse.getSnapshotComboComponents());
        if (invoiceProductResponse.getSnapshotProductName() != null
                && !invoiceProductResponse.getSnapshotProductName().trim().isEmpty()) {
            contentValues.put("snapshotProductName", invoiceProductResponse.getSnapshotProductName());
        }
        if (invoiceProductResponse.getSnapshotLinePrice() != null
                && !invoiceProductResponse.getSnapshotLinePrice().trim().isEmpty()) {
            contentValues.put("snapshotLinePrice", invoiceProductResponse.getSnapshotLinePrice());
        }

        long rowId = db.insertWithOnConflict(INVOICE_PRODUCT_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);

        return rowId != -1;

    }

    public int getInvoicePaymentModeCount(String invoiceDate, String paymentMode) {

        SQLiteDatabase db = this.getReadableDatabase();
        int totalCount = 0;
        String sql;
        if (!invoiceDate.isEmpty()) {
            sql = "SELECT COUNT(invoiceId) as totalCount FROM " + INVOICE_TABLE + " WHERE paymentMode = '" + paymentMode + "' AND invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null);
        } else {
            sql = "SELECT COUNT(invoiceId) as totalCount FROM " + INVOICE_TABLE + " WHERE paymentMode = '" + paymentMode + "'" + andBranchScope(null);
        }
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            totalCount = Integer.parseInt(cursor.getString(cursor.getColumnIndex("totalCount")));
        }
        db.close();
        return totalCount;
    }

    public int getInvoiceCount(String invoiceDate) {

        SQLiteDatabase db = this.getReadableDatabase();
        int totalCount = 0;
        String sql;
        if (!invoiceDate.isEmpty()) {
            sql = "SELECT COUNT(invoiceId) as totalCount FROM " + INVOICE_TABLE + " WHERE invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null);
        } else {
            sql = "SELECT COUNT(invoiceId) as totalCount FROM " + INVOICE_TABLE + whereBranchScope(null);
        }
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            totalCount = Integer.parseInt(cursor.getString(cursor.getColumnIndex("totalCount")));
        }
        db.close();
        return totalCount;
    }

    /** Total saved invoices (used for trial soft gate). */
    public int getTotalInvoiceCount() {
        return getInvoiceCount("");
    }

    public float getInvoicePaymentModeTotal(String invoiceDate, String paymentMode) {

        SQLiteDatabase db = this.getReadableDatabase();
        float totalAmount = 0;
        String sql;
        if (!invoiceDate.isEmpty()) {
            sql = "SELECT SUM(totalAmount) as totalAmount FROM " + INVOICE_TABLE + " WHERE paymentMode = '" + paymentMode + "' AND invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null) + andNotRefunded();
        } else {
            sql = "SELECT SUM(totalAmount) as totalAmount FROM " + INVOICE_TABLE + " WHERE paymentMode = '" + paymentMode + "'" + andBranchScope(null) + andNotRefunded();
        }
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            totalAmount = ReportCursorHelper.readFloat(cursor, "totalAmount");
        }
        cursor.close();
        db.close();
        return totalAmount;
    }

    public float getInvoiceTotal(String invoiceDate) {

        SQLiteDatabase db = this.getReadableDatabase();
        float totalAmount = 0;
        String sql;
        if (!invoiceDate.isEmpty()) {
            sql = "SELECT SUM(totalAmount) as totalAmount FROM " + INVOICE_TABLE + " WHERE invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null) + andNotRefunded();
        } else {
            String scope = whereBranchScope(null);
            if (scope.isEmpty()) {
                sql = "SELECT SUM(totalAmount) as totalAmount FROM " + INVOICE_TABLE + " WHERE " + notRefundedClause();
            } else {
                sql = "SELECT SUM(totalAmount) as totalAmount FROM " + INVOICE_TABLE + scope + andNotRefunded();
            }
        }
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            totalAmount = ReportCursorHelper.readFloat(cursor, "totalAmount");
        }
        cursor.close();
        db.close();
        return totalAmount;
    }

    public List<InvoiceResponse> getInvoicePaymentModeList(String invoiceDate, int offset, String paymentMode) {

        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql;
        if (!invoiceDate.isEmpty()) {
            sql = "SELECT * FROM " + INVOICE_TABLE + " WHERE paymentMode = '" + paymentMode + "' AND invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null) + andNotRefunded() + " ORDER BY invoiceDate DESC LIMIT " + offset + ", 25";
        } else {
            sql = "SELECT * FROM " + INVOICE_TABLE + " WHERE paymentMode = '" + paymentMode + "'" + andBranchScope(null) + andNotRefunded() + " ORDER BY invoiceDate DESC LIMIT " + offset + ", 25";
        }

        Cursor cursor = db.rawQuery(sql, null);
        InvoiceResponse invoiceResponse;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                invoiceResponse = new InvoiceResponse();
                invoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
                invoiceResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
                invoiceResponse.setInvoiceNumber(cursor.getString(cursor.getColumnIndex("invoiceNumber")));
                invoiceResponse.setCustomerName(cursor.getString(cursor.getColumnIndex("customerName")));
                invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerMobile")));
                invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerEmail")));
                invoiceResponse.setCustomerAddress(cursor.getString(cursor.getColumnIndex("customerAddress")));
                invoiceResponse.setSubTotal(cursor.getString(cursor.getColumnIndex("subTotal")));
                invoiceResponse.setTotalGSTAmount(cursor.getString(cursor.getColumnIndex("totalGSTAmount")));
                invoiceResponse.setDiscount(cursor.getString(cursor.getColumnIndex("discount")));
                invoiceResponse.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
                invoiceResponse.setPaymentMode(cursor.getString(cursor.getColumnIndex("paymentMode")));
                invoiceResponse.setInvoiceDate(cursor.getString(cursor.getColumnIndex("invoiceDate")));
                invoiceResponse.setInvoiceOrderStatus(cursor.getString(cursor.getColumnIndex("invoiceOrderStatus")));
                invoiceResponse.setInvoiceNetworkStatus(cursor.getString(cursor.getColumnIndex("invoiceNetworkStatus")));
                invoiceResponse.setInvoiceType(cursor.getString(cursor.getColumnIndex("invoiceType")));
                invoiceResponse.setInvoiceStatus(cursor.getString(cursor.getColumnIndex("invoiceStatus")));
                invoiceResponseList.add(invoiceResponse);
            } while (cursor.moveToNext());
            db.close();
        }
        return invoiceResponseList;

    }

    public List<InvoiceResponse> getInvoiceList(String invoiceDate, int offset) {

        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql;
        if (!invoiceDate.isEmpty()) {
            sql = "SELECT * FROM " + INVOICE_TABLE + " WHERE invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null) + " ORDER BY invoiceDate DESC LIMIT " + offset + ", 25";
        } else {
            sql = "SELECT * FROM " + INVOICE_TABLE + whereBranchScope(null) + " ORDER BY invoiceDate DESC LIMIT " + offset + ", 25";
        }

        Cursor cursor = db.rawQuery(sql, null);
        InvoiceResponse invoiceResponse;
        while (cursor.moveToNext()) {
            invoiceResponse = new InvoiceResponse();
            invoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
            invoiceResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            invoiceResponse.setInvoiceNumber(cursor.getString(cursor.getColumnIndex("invoiceNumber")));
            invoiceResponse.setCustomerName(cursor.getString(cursor.getColumnIndex("customerName")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerMobile")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerEmail")));
            invoiceResponse.setCustomerAddress(cursor.getString(cursor.getColumnIndex("customerAddress")));
            invoiceResponse.setSubTotal(cursor.getString(cursor.getColumnIndex("subTotal")));
            invoiceResponse.setTotalGSTAmount(cursor.getString(cursor.getColumnIndex("totalGSTAmount")));
            invoiceResponse.setDiscount(cursor.getString(cursor.getColumnIndex("discount")));
            invoiceResponse.setDiscountType(cursor.getString(cursor.getColumnIndex("discountType")));
            invoiceResponse.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
            invoiceResponse.setPaymentMode(cursor.getString(cursor.getColumnIndex("paymentMode")));
            invoiceResponse.setInvoiceDate(cursor.getString(cursor.getColumnIndex("invoiceDate")));
            invoiceResponse.setInvoiceOrderStatus(cursor.getString(cursor.getColumnIndex("invoiceOrderStatus")));
            invoiceResponse.setInvoiceNetworkStatus(cursor.getString(cursor.getColumnIndex("invoiceNetworkStatus")));
            invoiceResponse.setInvoiceType(cursor.getString(cursor.getColumnIndex("invoiceType")));
            invoiceResponse.setInvoiceStatus(cursor.getString(cursor.getColumnIndex("invoiceStatus")));
            invoiceResponseList.add(invoiceResponse);
        }
        db.close();
        return invoiceResponseList;

    }

    public List<InvoiceResponse> getDateReportList(String invoiceDate, int offset) {

        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql;
        if (invoiceDate != null) {
            sql = "SELECT * FROM " + INVOICE_TABLE + " WHERE invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null) + " ORDER BY invoiceId DESC LIMIT " + offset + ", 25";
        } else {
            sql = "SELECT * FROM " + INVOICE_TABLE + whereBranchScope(null) + " ORDER BY invoiceId DESC LIMIT " + offset + ", 25";
        }

        Cursor cursor = db.rawQuery(sql, null);
        InvoiceResponse invoiceResponse;
        while (cursor.moveToNext()) {
            invoiceResponse = new InvoiceResponse();
            invoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
            invoiceResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            invoiceResponse.setInvoiceNumber(cursor.getString(cursor.getColumnIndex("invoiceNumber")));
            invoiceResponse.setCustomerName(cursor.getString(cursor.getColumnIndex("customerName")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerMobile")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerEmail")));
            invoiceResponse.setCustomerAddress(cursor.getString(cursor.getColumnIndex("customerAddress")));
            invoiceResponse.setSubTotal(cursor.getString(cursor.getColumnIndex("subTotal")));
            invoiceResponse.setTotalGSTAmount(cursor.getString(cursor.getColumnIndex("totalGSTAmount")));
            invoiceResponse.setDiscount(cursor.getString(cursor.getColumnIndex("discount")));
            invoiceResponse.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
            invoiceResponse.setPaymentMode(cursor.getString(cursor.getColumnIndex("paymentMode")));
            invoiceResponse.setInvoiceDate(cursor.getString(cursor.getColumnIndex("invoiceDate")));
            invoiceResponse.setInvoiceOrderStatus(cursor.getString(cursor.getColumnIndex("invoiceOrderStatus")));
            invoiceResponse.setInvoiceNetworkStatus(cursor.getString(cursor.getColumnIndex("invoiceNetworkStatus")));
            invoiceResponse.setInvoiceType(cursor.getString(cursor.getColumnIndex("invoiceType")));
            invoiceResponse.setInvoiceStatus(cursor.getString(cursor.getColumnIndex("invoiceStatus")));
            invoiceResponseList.add(invoiceResponse);
        }
        db.close();
        return invoiceResponseList;

    }

    public int getInvoiceTableCount(String invoiceDate, String table_wise) {

        SQLiteDatabase db = this.getReadableDatabase();
        int totalCount = 0;
        String sql;
        if (!invoiceDate.isEmpty()) {
            sql = "SELECT COUNT(invoiceId) as totalCount FROM " + INVOICE_TABLE + " WHERE invoiceType = '" + table_wise + "' AND invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null);
        } else {
            sql = "SELECT COUNT(invoiceId) as totalCount FROM " + INVOICE_TABLE + " WHERE invoiceType = '" + table_wise + "'" + andBranchScope(null);
        }
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            totalCount = Integer.parseInt(cursor.getString(cursor.getColumnIndex("totalCount")));
        }
        db.close();
        return totalCount;
    }

    public float getInvoiceTableTotal(String invoiceDate, String table_wise) {

        SQLiteDatabase db = this.getReadableDatabase();
        float totalAmount = 0;
        String sql;
        if (!invoiceDate.isEmpty()) {
            sql = "SELECT SUM(totalAmount) as totalAmount FROM " + INVOICE_TABLE + " WHERE invoiceType = '" + table_wise + "' AND invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null);
        } else {
            sql = "SELECT SUM(totalAmount) as totalAmount FROM " + INVOICE_TABLE + " WHERE invoiceType = '" + table_wise + "'" + andBranchScope(null);
        }
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            totalAmount = ReportCursorHelper.readFloat(cursor, "totalAmount");
        }
        cursor.close();
        db.close();
        return totalAmount;
    }

    public List<InvoiceResponse> getInvoiceTableReportList(String invoiceDate, String table_wise, int offset) {

        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql;
        if (!invoiceDate.isEmpty()) {
            sql = "SELECT SUM(totalAmount) as totalAmount, noOfTable, invoiceType FROM  " + INVOICE_TABLE + " WHERE invoiceType = '" + table_wise + "' AND invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null) + " GROUP BY noOfTable LIMIT " + offset + ", 25";
        } else {
            sql = "SELECT SUM(totalAmount) as totalAmount, noOfTable, invoiceType FROM  " + INVOICE_TABLE + " WHERE invoiceType = '" + table_wise + "'" + andBranchScope(null) + " GROUP BY noOfTable LIMIT " + offset + ", 25";
        }
        Cursor cursor = db.rawQuery(sql, null);
        InvoiceResponse invoiceResponse;
        while (cursor.moveToNext()) {
            invoiceResponse = new InvoiceResponse();
            invoiceResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            invoiceResponse.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
            invoiceResponse.setInvoiceType(cursor.getString(cursor.getColumnIndex("invoiceType")));
            invoiceResponseList.add(invoiceResponse);
        }
        db.close();
        return invoiceResponseList;

    }

    public List<InvoiceResponse> getDateTableReport(String table_wise, String invoiceDate) {

        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT SUM(totalAmount) as totalAmount, noOfTable, invoiceType FROM  " + INVOICE_TABLE + " WHERE invoiceType = '" + table_wise + "' AND invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null) + " GROUP BY noOfTable", null);
        InvoiceResponse invoiceResponse;
        while (cursor.moveToNext()) {
            invoiceResponse = new InvoiceResponse();
            invoiceResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            invoiceResponse.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
            invoiceResponse.setInvoiceType(cursor.getString(cursor.getColumnIndex("invoiceType")));
            invoiceResponseList.add(invoiceResponse);
        }
        db.close();
        return invoiceResponseList;

    }

    public List<InvoiceResponse> getInvoiceDetails(String invoiceId) {

        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + INVOICE_TABLE + " WHERE invoiceId = '" + invoiceId + "'" + andBranchScope(null), null);
        InvoiceResponse invoiceResponse;
        while (cursor.moveToNext()) {
            invoiceResponse = new InvoiceResponse();
            invoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
            invoiceResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            invoiceResponse.setInvoiceNumber(cursor.getString(cursor.getColumnIndex("invoiceNumber")));
            invoiceResponse.setCustomerName(cursor.getString(cursor.getColumnIndex("customerName")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerMobile")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerEmail")));
            invoiceResponse.setCustomerAddress(cursor.getString(cursor.getColumnIndex("customerAddress")));
            invoiceResponse.setSubTotal(cursor.getString(cursor.getColumnIndex("subTotal")));
            invoiceResponse.setTotalGSTAmount(cursor.getString(cursor.getColumnIndex("totalGSTAmount")));
            invoiceResponse.setDiscount(cursor.getString(cursor.getColumnIndex("discount")));
            invoiceResponse.setDiscountType(cursor.getString(cursor.getColumnIndex("discountType")));
            invoiceResponse.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
            invoiceResponse.setPaymentMode(cursor.getString(cursor.getColumnIndex("paymentMode")));
            invoiceResponse.setInvoiceDate(cursor.getString(cursor.getColumnIndex("invoiceDate")));
            invoiceResponse.setInvoiceOrderStatus(cursor.getString(cursor.getColumnIndex("invoiceOrderStatus")));
            invoiceResponse.setInvoiceNetworkStatus(cursor.getString(cursor.getColumnIndex("invoiceNetworkStatus")));
            invoiceResponse.setInvoiceType(cursor.getString(cursor.getColumnIndex("invoiceType")));
            invoiceResponse.setInvoiceStatus(cursor.getString(cursor.getColumnIndex("invoiceStatus")));
            invoiceResponseList.add(invoiceResponse);
        }
        db.close();
        return invoiceResponseList;

    }

    private String discountInvoiceWhere(String invoiceDate) {
        String where = " WHERE CAST(IFNULL(discount,'0') AS REAL) > 0";
        if (invoiceDate != null && !invoiceDate.trim().isEmpty()) {
            where += " AND invoiceDate LIKE '%" + invoiceDate.replace("'", "''") + "%'";
        }
        return where + andNotRefunded() + andBranchScope(null);
    }

    private String refundInvoiceWhere(String invoiceDate) {
        String where = " WHERE LOWER(IFNULL(invoiceOrderStatus,'')) = 'refunded'";
        if (invoiceDate != null && !invoiceDate.trim().isEmpty()) {
            where += " AND invoiceDate LIKE '%" + invoiceDate.replace("'", "''") + "%'";
        }
        return where + andBranchScope(null);
    }

    public List<InvoiceResponse> getDiscountInvoiceList(String invoiceDate, int offset, int limit) {
        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        int safeLimit = limit > 0 ? limit : 25;
        String sql = "SELECT * FROM " + INVOICE_TABLE + discountInvoiceWhere(invoiceDate)
                + " ORDER BY invoiceDate DESC LIMIT " + offset + ", " + safeLimit;
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            InvoiceResponse invoiceResponse = new InvoiceResponse();
            invoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
            invoiceResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            invoiceResponse.setInvoiceNumber(cursor.getString(cursor.getColumnIndex("invoiceNumber")));
            invoiceResponse.setSubTotal(cursor.getString(cursor.getColumnIndex("subTotal")));
            invoiceResponse.setDiscount(cursor.getString(cursor.getColumnIndex("discount")));
            invoiceResponse.setDiscountType(cursor.getString(cursor.getColumnIndex("discountType")));
            invoiceResponse.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
            invoiceResponse.setInvoiceDate(cursor.getString(cursor.getColumnIndex("invoiceDate")));
            invoiceResponse.setInvoiceType(cursor.getString(cursor.getColumnIndex("invoiceType")));
            invoiceResponseList.add(invoiceResponse);
        }
        cursor.close();
        db.close();
        return invoiceResponseList;
    }

    public List<InvoiceResponse> getRefundInvoiceList(String invoiceDate, int offset, int limit) {
        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        int safeLimit = limit > 0 ? limit : 25;
        String sql = "SELECT * FROM " + INVOICE_TABLE + refundInvoiceWhere(invoiceDate)
                + " ORDER BY invoiceDate DESC LIMIT " + offset + ", " + safeLimit;
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            InvoiceResponse invoiceResponse = new InvoiceResponse();
            invoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
            invoiceResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            invoiceResponse.setInvoiceNumber(cursor.getString(cursor.getColumnIndex("invoiceNumber")));
            invoiceResponse.setSubTotal(cursor.getString(cursor.getColumnIndex("subTotal")));
            invoiceResponse.setDiscount(cursor.getString(cursor.getColumnIndex("discount")));
            invoiceResponse.setDiscountType(cursor.getString(cursor.getColumnIndex("discountType")));
            invoiceResponse.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
            invoiceResponse.setInvoiceDate(cursor.getString(cursor.getColumnIndex("invoiceDate")));
            invoiceResponse.setInvoiceType(cursor.getString(cursor.getColumnIndex("invoiceType")));
            invoiceResponse.setInvoiceOrderStatus(cursor.getString(cursor.getColumnIndex("invoiceOrderStatus")));
            invoiceResponseList.add(invoiceResponse);
        }
        cursor.close();
        db.close();
        return invoiceResponseList;
    }

    public List<InvoiceProductResponse> getInvoiceProductList(String invoiceNumber) {

        List<InvoiceProductResponse> productResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + INVOICE_PRODUCT_TABLE + " WHERE invoiceNumber ='" + invoiceNumber + "'", null);
        InvoiceProductResponse invoiceProductResponse;
        while (cursor.moveToNext()) {
            invoiceProductResponse = new InvoiceProductResponse();
            invoiceProductResponse.setInvoiceProductId(cursor.getString(cursor.getColumnIndex("invoiceProductId")));
            invoiceProductResponse.setInvoiceNumber(cursor.getString(cursor.getColumnIndex("invoiceNumber")));
            invoiceProductResponse.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
            invoiceProductResponse.setProductPrice(cursor.getString(cursor.getColumnIndex("productPrice")));
            invoiceProductResponse.setProductUnit(cursor.getString(cursor.getColumnIndex("productUnit")));
            invoiceProductResponse.setProductQuantity(cursor.getString(cursor.getColumnIndex("productQuantity")));
            invoiceProductResponse.setProductCGST(cursor.getString(cursor.getColumnIndex("productCGST")));
            invoiceProductResponse.setProductSGST(cursor.getString(cursor.getColumnIndex("productSGST")));
            invoiceProductResponse.setProductStatus(cursor.getString(cursor.getColumnIndex("productStatus")));
            invoiceProductResponse.setInvoiceProductStatus(cursor.getString(cursor.getColumnIndex("invoiceProductStatus")));
            invoiceProductResponse.setInvoiceProductNetworkStatus(cursor.getString(cursor.getColumnIndex("invoiceProductNetworkStatus")));
            mapInvoiceLineSnapshots(cursor, invoiceProductResponse);
            productResponseList.add(invoiceProductResponse);
        }
        db.close();
        return productResponseList;

    }

    public List<InvoiceProductResponse> getReportProductList(String orderBy) {

        List<InvoiceProductResponse> productResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT productName, COUNT(productQuantity) as productQuantity FROM invoice_final_product GROUP BY productName ORDER BY productQuantity " + orderBy, null);
        InvoiceProductResponse invoiceProductResponse;
        while (cursor.moveToNext()) {
            invoiceProductResponse = new InvoiceProductResponse();
            invoiceProductResponse.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
            invoiceProductResponse.setProductQuantity(cursor.getString(cursor.getColumnIndex("productQuantity")));
            productResponseList.add(invoiceProductResponse);
        }

        db.close();
        return productResponseList;

    }

    public boolean addExpenses(String expenseName, String expenseAmount, String expensesDate,
                               int expenseStatus, String expenseNetworkStatus) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("expensesName", expenseName);
        contentValues.put("expensesAmount", expenseAmount);
        contentValues.put("expensesDate", expensesDate);
        contentValues.put("expensesStatus", String.valueOf(expenseStatus));
        contentValues.put("expensesNetworkStatus", expenseNetworkStatus);
        BranchSession.applyScope(contentValues);

        db.insert(EXPENSES_TABLE, null, contentValues);

        return true;

    }

    public List<ExpenseResponse> getExpenseList() {

        List<ExpenseResponse> expenseResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + EXPENSES_TABLE, null);
        ExpenseResponse expenseResponse;
        while (cursor.moveToNext()) {
            expenseResponse = new ExpenseResponse();
            expenseResponse.setExpenseId(cursor.getString(cursor.getColumnIndex("expensesId")));
            expenseResponse.setExpenseName(cursor.getString(cursor.getColumnIndex("expensesName")));
            expenseResponse.setExpenseAmount(cursor.getString(cursor.getColumnIndex("expensesAmount")));
            expenseResponse.setExpenseDate(cursor.getString(cursor.getColumnIndex("expensesDate")));
            expenseResponse.setExpenseNetworkStatus(cursor.getString(cursor.getColumnIndex("expensesNetworkStatus")));
            expenseResponse.setExpenseStatus(cursor.getString(cursor.getColumnIndex("expensesStatus")));
            expenseResponseList.add(expenseResponse);
        }

        db.close();
        return expenseResponseList;

    }

    public boolean addInventory(String productId, String productQuantity, String
            afterSaleInventoryQuantity, String saleInventoryQuantity, String inventoryDate,
                                int inventoryStatus, String inventoryNetworkStatus) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("productId", productId);
        contentValues.put("productInventoryQuantity", productQuantity);
        contentValues.put("afterSaleInventoryQuantity", afterSaleInventoryQuantity);
        contentValues.put("saleInventoryQuantity", saleInventoryQuantity);
        contentValues.put("inventoryDate", inventoryDate);
        contentValues.put("inventoryStatus", String.valueOf(inventoryStatus));
        contentValues.put("inventoryNetworkStatus", inventoryNetworkStatus);
        BranchSession.applyScope(contentValues);

        db.insert(INVENTORY_TABLE, null, contentValues);

        return true;

    }

    public List<InventoryResponse> getInventoryList() {

        List<InventoryResponse> inventoryResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT inventory.*, product.productName FROM inventory LEFT JOIN product ON inventory.productId = product.productId", null);
        InventoryResponse inventoryResponse;
        while (cursor.moveToNext()) {
            inventoryResponse = new InventoryResponse();
            inventoryResponse.setInventoryId(cursor.getString(cursor.getColumnIndex("inventoryId")));
            inventoryResponse.setProductId(cursor.getString(cursor.getColumnIndex("productId")));
            inventoryResponse.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
            inventoryResponse.setProductInventoryQuantity(cursor.getString(cursor.getColumnIndex("productInventoryQuantity")));
            inventoryResponse.setAfterSaleInventoryQuantity(cursor.getString(cursor.getColumnIndex("afterSaleInventoryQuantity")));
            inventoryResponse.setSaleInventoryQuantity(cursor.getString(cursor.getColumnIndex("saleInventoryQuantity")));
            inventoryResponse.setInventoryDate(cursor.getString(cursor.getColumnIndex("inventoryDate")));
            inventoryResponse.setInventoryNetworkStatus(cursor.getString(cursor.getColumnIndex("inventoryNetworkStatus")));
            inventoryResponse.setInventoryStatus(cursor.getString(cursor.getColumnIndex("inventoryStatus")));
            inventoryResponseList.add(inventoryResponse);
        }

        db.close();
        return inventoryResponseList;

    }

    public List<InventoryResponse> getLowInventoryList() {

        List<InventoryResponse> inventoryResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT inventory.*, product.productName FROM inventory LEFT JOIN product ON inventory.productId = product.productId WHERE inventory.afterSaleInventoryQuantity < 6 ORDER BY inventory.afterSaleInventoryQuantity DESC", null);
        InventoryResponse inventoryResponse;
        while (cursor.moveToNext()) {
            inventoryResponse = new InventoryResponse();
            inventoryResponse.setInventoryId(cursor.getString(cursor.getColumnIndex("inventoryId")));
            inventoryResponse.setProductId(cursor.getString(cursor.getColumnIndex("productId")));
            inventoryResponse.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
            inventoryResponse.setProductInventoryQuantity(cursor.getString(cursor.getColumnIndex("productInventoryQuantity")));
            inventoryResponse.setAfterSaleInventoryQuantity(cursor.getString(cursor.getColumnIndex("afterSaleInventoryQuantity")));
            inventoryResponse.setInventoryDate(cursor.getString(cursor.getColumnIndex("inventoryDate")));
            inventoryResponse.setInventoryNetworkStatus(cursor.getString(cursor.getColumnIndex("inventoryNetworkStatus")));
            inventoryResponse.setInventoryStatus(cursor.getString(cursor.getColumnIndex("inventoryStatus")));
            inventoryResponseList.add(inventoryResponse);
        }

        db.close();
        return inventoryResponseList;

    }

    public List<InventoryResponse> getInventoryDetails(String productId) {

        List<InventoryResponse> inventoryResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM inventory LEFT JOIN product ON inventory.productId = product.productId WHERE inventory.productId = '" + productId + "' ORDER BY `inventoryId` DESC LIMIT 1", null);
        InventoryResponse inventoryResponse;
        while (cursor.moveToNext()) {
            inventoryResponse = new InventoryResponse();
            inventoryResponse.setInventoryId(cursor.getString(cursor.getColumnIndex("inventoryId")));
            inventoryResponse.setProductId(cursor.getString(cursor.getColumnIndex("productId")));
            inventoryResponse.setProductInventoryQuantity(cursor.getString(cursor.getColumnIndex("productInventoryQuantity")));
            inventoryResponse.setAfterSaleInventoryQuantity(cursor.getString(cursor.getColumnIndex("afterSaleInventoryQuantity")));
            inventoryResponse.setSaleInventoryQuantity(cursor.getString(cursor.getColumnIndex("saleInventoryQuantity")));
            inventoryResponse.setInventoryDate(cursor.getString(cursor.getColumnIndex("inventoryDate")));
            inventoryResponse.setInventoryNetworkStatus(cursor.getString(cursor.getColumnIndex("inventoryNetworkStatus")));
            inventoryResponse.setInventoryStatus(cursor.getString(cursor.getColumnIndex("inventoryStatus")));
            inventoryResponseList.add(inventoryResponse);
        }

        db.close();
        return inventoryResponseList;

    }

    public void updateInventory(String productId, String productQuantity, String
            afterSaleInventoryQuantity, String saleInventoryQuantity, int inventoryStatus) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("productInventoryQuantity", productQuantity);
        contentValues.put("afterSaleInventoryQuantity", afterSaleInventoryQuantity);
        contentValues.put("saleInventoryQuantity", saleInventoryQuantity);
        contentValues.put("inventoryStatus", "0");

        db.update(INVENTORY_TABLE, contentValues, "productId=?", new String[]{productId});
        db.close();
    }

    public Cursor getUnSynchronizeInventory(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + INVENTORY_TABLE + " WHERE inventoryStatus = '" + status + "' ";
        Cursor cursor = db.rawQuery(sql, null);
        return cursor;
    }

    public void updateSyncInventory(String inventoryId, int invoiceProductStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("inventoryStatus", invoiceProductStatus);
        db.update(INVENTORY_TABLE, contentValues, "inventoryId=?", new String[]{inventoryId});
        db.close();
    }


    public Cursor getUnSynchronizeExpenses(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + EXPENSES_TABLE + " WHERE expensesStatus = '" + status + "' ";
        Cursor cursor = db.rawQuery(sql, null);
        return cursor;
    }

    public void updateSyncExpenses(String expensesId, int invoiceProductStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("expensesStatus", invoiceProductStatus);
        db.update(EXPENSES_TABLE, contentValues, "expensesId=?", new String[]{expensesId});
        db.close();
    }

    public List<ProductCartResponse> getTakeWayCartList(String cartOrderStatus) {

        List<ProductCartResponse> productCartResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + CART_PRODUCT_TABLE + " WHERE cartOrderStatus = '" + cartOrderStatus + "' GROUP BY noOfTable ORDER BY noOfTable ASC", null);
        ProductCartResponse productResponse;
        while (cursor.moveToNext()) {
            productResponse = new ProductCartResponse();
            productResponse.setCartId(cursor.getString(cursor.getColumnIndex("cartId")));
            productResponse.setProductId(cursor.getString(cursor.getColumnIndex("productId")));
            productResponse.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
            productResponse.setProductOldPrice(cursor.getString(cursor.getColumnIndex("productOldPrice")));
            productResponse.setProductNewPrice(cursor.getString(cursor.getColumnIndex("productNewPrice")));
            productResponse.setProductUnit(cursor.getString(cursor.getColumnIndex("productUnit")));
            productResponse.setProductQuantity(cursor.getString(cursor.getColumnIndex("productQuantity")));
            productResponse.setProductCGST(cursor.getString(cursor.getColumnIndex("productCGST")));
            productResponse.setProductSGST(cursor.getString(cursor.getColumnIndex("productSGST")));
            productResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            productResponse.setCartDiscount(cursor.getString(cursor.getColumnIndex("cartDiscount")));
            productResponse.setCartDiscountType(cursor.getString(cursor.getColumnIndex("cartDiscountType")));
            productResponse.setCartOrderStatus(cursor.getString(cursor.getColumnIndex("cartOrderStatus")));
            productResponse.setCartStatus(cursor.getString(cursor.getColumnIndex("cartStatus")));
            mapCartLineSnapshots(cursor, productResponse);
            productCartResponseList.add(productResponse);
        }
        db.close();
        return productCartResponseList;

    }

    public int getTableReportInvoiceCount(String invoiceDate, String noOfTable, String cartOrderStatus) {

        SQLiteDatabase db = this.getReadableDatabase();
        int totalCount = 0;
        String sql;
        if (!invoiceDate.isEmpty()) {
            sql = "SELECT COUNT(invoiceId) as totalCount FROM " + INVOICE_TABLE + " WHERE noOfTable = '" + noOfTable + "' AND invoiceType = '" + cartOrderStatus + "' AND invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null);
        } else {
            sql = "SELECT COUNT(invoiceId) as totalCount FROM " + INVOICE_TABLE + " WHERE noOfTable = '" + noOfTable + "' AND invoiceType = '" + cartOrderStatus + "'" + andBranchScope(null);
        }
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            totalCount = Integer.parseInt(cursor.getString(cursor.getColumnIndex("totalCount")));
        }
        db.close();
        return totalCount;
    }

    public float getTableReportInvoiceTotal(String invoiceDate, String noOfTable, String cartOrderStatus) {

        SQLiteDatabase db = this.getReadableDatabase();
        float totalAmount = 0;
        String sql;
        if (!invoiceDate.isEmpty()) {
            sql = "SELECT SUM(totalAmount) as totalAmount FROM " + INVOICE_TABLE + " WHERE noOfTable = '" + noOfTable + "' AND invoiceType = '" + cartOrderStatus + "' AND invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null);
        } else {
            sql = "SELECT SUM(totalAmount) as totalAmount FROM " + INVOICE_TABLE + " WHERE noOfTable = '" + noOfTable + "' AND invoiceType = '" + cartOrderStatus + "'" + andBranchScope(null);
        }
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            totalAmount = ReportCursorHelper.readFloat(cursor, "totalAmount");
        }
        cursor.close();
        db.close();
        return totalAmount;
    }

    public List<InvoiceResponse> getTableReportList(String invoiceDate, String noOfTable, String cartOrderStatus, int offset) {

        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql;
        if (!invoiceDate.isEmpty()) {
            sql = "SELECT * FROM " + INVOICE_TABLE + " WHERE noOfTable = '" + noOfTable + "' AND invoiceType = '" + cartOrderStatus + "' AND invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null) + " ORDER BY invoiceDate DESC LIMIT " + offset + ", 25";
        } else {
            sql = "SELECT * FROM " + INVOICE_TABLE + " WHERE noOfTable = '" + noOfTable + "' AND invoiceType = '" + cartOrderStatus + "'" + andBranchScope(null) + " ORDER BY invoiceDate DESC LIMIT " + offset + ", 25";
        }
        Cursor cursor = db.rawQuery(sql, null);
        InvoiceResponse invoiceResponse;
        while (cursor.moveToNext()) {
            invoiceResponse = new InvoiceResponse();
            invoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
            invoiceResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            invoiceResponse.setInvoiceNumber(cursor.getString(cursor.getColumnIndex("invoiceNumber")));
            invoiceResponse.setCustomerName(cursor.getString(cursor.getColumnIndex("customerName")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerMobile")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerEmail")));
            invoiceResponse.setCustomerAddress(cursor.getString(cursor.getColumnIndex("customerAddress")));
            invoiceResponse.setSubTotal(cursor.getString(cursor.getColumnIndex("subTotal")));
            invoiceResponse.setTotalGSTAmount(cursor.getString(cursor.getColumnIndex("totalGSTAmount")));
            invoiceResponse.setDiscount(cursor.getString(cursor.getColumnIndex("discount")));
            invoiceResponse.setDiscountType(cursor.getString(cursor.getColumnIndex("discountType")));
            invoiceResponse.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
            invoiceResponse.setPaymentMode(cursor.getString(cursor.getColumnIndex("paymentMode")));
            invoiceResponse.setInvoiceDate(cursor.getString(cursor.getColumnIndex("invoiceDate")));
            invoiceResponse.setInvoiceOrderStatus(cursor.getString(cursor.getColumnIndex("invoiceOrderStatus")));
            invoiceResponse.setInvoiceNetworkStatus(cursor.getString(cursor.getColumnIndex("invoiceNetworkStatus")));
            invoiceResponse.setInvoiceType(cursor.getString(cursor.getColumnIndex("invoiceType")));
            invoiceResponse.setInvoiceStatus(cursor.getString(cursor.getColumnIndex("invoiceStatus")));
            invoiceResponseList.add(invoiceResponse);
        }
        db.close();
        return invoiceResponseList;
    }

    public List<InvoiceResponse> getDateTableReportList(String noOfTable, String
            cartOrderStatus, String invoiceDate) {

        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + INVOICE_TABLE + " WHERE noOfTable = '" + noOfTable + "' AND invoiceType = '" + cartOrderStatus + "' AND invoiceDate LIKE '%" + invoiceDate + "%'" + andBranchScope(null) + " ORDER BY invoiceId DESC", null);
        InvoiceResponse invoiceResponse;
        while (cursor.moveToNext()) {
            invoiceResponse = new InvoiceResponse();
            invoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
            invoiceResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            invoiceResponse.setInvoiceNumber(cursor.getString(cursor.getColumnIndex("invoiceNumber")));
            invoiceResponse.setCustomerName(cursor.getString(cursor.getColumnIndex("customerName")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerMobile")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerEmail")));
            invoiceResponse.setCustomerAddress(cursor.getString(cursor.getColumnIndex("customerAddress")));
            invoiceResponse.setSubTotal(cursor.getString(cursor.getColumnIndex("subTotal")));
            invoiceResponse.setTotalGSTAmount(cursor.getString(cursor.getColumnIndex("totalGSTAmount")));
            invoiceResponse.setDiscount(cursor.getString(cursor.getColumnIndex("discount")));
            invoiceResponse.setDiscountType(cursor.getString(cursor.getColumnIndex("discountType")));
            invoiceResponse.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
            invoiceResponse.setPaymentMode(cursor.getString(cursor.getColumnIndex("paymentMode")));
            invoiceResponse.setInvoiceDate(cursor.getString(cursor.getColumnIndex("invoiceDate")));
            invoiceResponse.setInvoiceOrderStatus(cursor.getString(cursor.getColumnIndex("invoiceOrderStatus")));
            invoiceResponse.setInvoiceNetworkStatus(cursor.getString(cursor.getColumnIndex("invoiceNetworkStatus")));
            invoiceResponse.setInvoiceType(cursor.getString(cursor.getColumnIndex("invoiceType")));
            invoiceResponse.setInvoiceStatus(cursor.getString(cursor.getColumnIndex("invoiceStatus")));
            invoiceResponseList.add(invoiceResponse);
        }
        db.close();
        return invoiceResponseList;
    }


    public void clearInvoice() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(INVOICE_TABLE, null, null);
            db.delete(INVOICE_PRODUCT_TABLE, null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }


    public List<InvoiceResponse> getLastInvoiceList(String cartOrderStatus) {

        List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + INVOICE_TABLE + " WHERE invoiceType = '" + cartOrderStatus + "'" + andBranchScope(null) + " ORDER BY invoiceId DESC LIMIT 1", null);
        InvoiceResponse invoiceResponse;
        while (cursor.moveToNext()) {
            invoiceResponse = new InvoiceResponse();
            invoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
            invoiceResponse.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            invoiceResponse.setInvoiceNumber(cursor.getString(cursor.getColumnIndex("invoiceNumber")));
            invoiceResponse.setCustomerName(cursor.getString(cursor.getColumnIndex("customerName")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerMobile")));
            invoiceResponse.setCustomerMobile(cursor.getString(cursor.getColumnIndex("customerEmail")));
            invoiceResponse.setCustomerAddress(cursor.getString(cursor.getColumnIndex("customerAddress")));
            invoiceResponse.setSubTotal(cursor.getString(cursor.getColumnIndex("subTotal")));
            invoiceResponse.setTotalGSTAmount(cursor.getString(cursor.getColumnIndex("totalGSTAmount")));
            invoiceResponse.setDiscount(cursor.getString(cursor.getColumnIndex("discount")));
            invoiceResponse.setDiscountType(cursor.getString(cursor.getColumnIndex("discountType")));
            invoiceResponse.setTotalAmount(cursor.getString(cursor.getColumnIndex("totalAmount")));
            invoiceResponse.setPaymentMode(cursor.getString(cursor.getColumnIndex("paymentMode")));
            invoiceResponse.setInvoiceDate(cursor.getString(cursor.getColumnIndex("invoiceDate")));
            invoiceResponse.setInvoiceOrderStatus(cursor.getString(cursor.getColumnIndex("invoiceOrderStatus")));
            invoiceResponse.setInvoiceNetworkStatus(cursor.getString(cursor.getColumnIndex("invoiceNetworkStatus")));
            invoiceResponse.setInvoiceType(cursor.getString(cursor.getColumnIndex("invoiceType")));
            invoiceResponse.setInvoiceStatus(cursor.getString(cursor.getColumnIndex("invoiceStatus")));
            invoiceResponseList.add(invoiceResponse);
        }
        db.close();
        return invoiceResponseList;
    }

    public List<InvoiceProductResponse> getReportDateWiseProductList(String invoiceDate, String
            orderBy) {
        return getReportDateWiseProductList(invoiceDate, orderBy, null);
    }

    public List<InvoiceProductResponse> getReportDateWiseProductList(String invoiceDate, String
            orderBy, String invoiceItemType) {

        List<InvoiceProductResponse> productResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String typeFilter = "";
        if (invoiceItemType != null && !invoiceItemType.trim().isEmpty()) {
            typeFilter = " AND IFNULL(invoice_final_product.invoiceItemType, 'PRODUCT') = '"
                    + invoiceItemType.replace("'", "") + "' ";
        }
        String sql = "SELECT invoice_final_product.productName, SUM(invoice_final_product.productQuantity) AS productQuantity FROM invoice_final_product LEFT JOIN invoice ON invoice.invoiceNumber = invoice_final_product.invoiceNumber WHERE invoice.invoiceDate LIKE '%" + invoiceDate + "%'" + typeFilter + andBranchScope("invoice") + " GROUP BY invoice_final_product.productName ORDER BY invoice_final_product.productQuantity " + orderBy;
        Cursor cursor = db.rawQuery(sql, null);
        InvoiceProductResponse invoiceProductResponse;
        while (cursor.moveToNext()) {
            invoiceProductResponse = new InvoiceProductResponse();
            invoiceProductResponse.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
            invoiceProductResponse.setProductQuantity(cursor.getString(cursor.getColumnIndex("productQuantity")));
            productResponseList.add(invoiceProductResponse);
        }

        db.close();
        return productResponseList;

    }

    public List<MemberResponse> getMemberList() {

        List<MemberResponse> memberResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM member LEFT JOIN member_payment ON member_payment.memberId = member.memberId GROUP BY member_payment.memberId", null);
        MemberResponse memberResponse;
        while (cursor.moveToNext()) {
            memberResponse = new MemberResponse();
            memberResponse.setMemberId(cursor.getString(cursor.getColumnIndex("memberId")));
            String memberId = cursor.getString(cursor.getColumnIndex("memberId"));
            memberResponse.setMemberName(cursor.getString(cursor.getColumnIndex("memberName")));
            memberResponse.setMemberMobileNumber(cursor.getString(cursor.getColumnIndex("memberMobileNumber")));
            memberResponse.setMemberAlternetMobileNumber(cursor.getString(cursor.getColumnIndex("memberAlternetMobileNumber")));
            memberResponse.setMemberAddress(cursor.getString(cursor.getColumnIndex("memberAddress")));
            memberResponse.setMessTotalDays(cursor.getString(cursor.getColumnIndex("messTotalDays")));
            memberResponse.setPaymentMessAmount(cursor.getString(cursor.getColumnIndex("paymentMessAmount")));
            memberResponse.setPaymentDate(cursor.getString(cursor.getColumnIndex("paymentDate")));
            Cursor cursor1 = db.rawQuery("SELECT SUM(paymentPaidAmount) as paymentPaidAmount FROM member_payment WHERE memberId='" + memberId + "'", null);
            while (cursor1.moveToNext()) {
                memberResponse.setPaymentPaidAmount(cursor1.getString(cursor1.getColumnIndex("paymentPaidAmount")));
            }
            memberResponseList.add(memberResponse);
        }

        db.close();
        return memberResponseList;

    }

    public List<MemberResponse> getMemberPaymentList(String paymentDate) {

        List<MemberResponse> memberResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM member LEFT JOIN member_payment ON member_payment.memberId = member.memberId WHERE member_payment.paymentDate LIKE '%" + paymentDate + "%' GROUP BY member_payment.memberId", null);
        MemberResponse memberResponse;
        while (cursor.moveToNext()) {
            memberResponse = new MemberResponse();
            memberResponse.setMemberId(cursor.getString(cursor.getColumnIndex("memberId")));
            String memberId = cursor.getString(cursor.getColumnIndex("memberId"));
            memberResponse.setMemberName(cursor.getString(cursor.getColumnIndex("memberName")));
            memberResponse.setMemberMobileNumber(cursor.getString(cursor.getColumnIndex("memberMobileNumber")));
            memberResponse.setMemberAlternetMobileNumber(cursor.getString(cursor.getColumnIndex("memberAlternetMobileNumber")));
            memberResponse.setMemberAddress(cursor.getString(cursor.getColumnIndex("memberAddress")));
            memberResponse.setMessTotalDays(cursor.getString(cursor.getColumnIndex("messTotalDays")));
            memberResponse.setPaymentMessAmount(cursor.getString(cursor.getColumnIndex("paymentMessAmount")));
            memberResponse.setPaymentDate(cursor.getString(cursor.getColumnIndex("paymentDate")));
            Cursor cursor1 = db.rawQuery("SELECT SUM(paymentPaidAmount) as paymentPaidAmount FROM member_payment WHERE memberId='" + memberId + "'", null);
            while (cursor1.moveToNext()) {
                memberResponse.setPaymentPaidAmount(cursor1.getString(cursor1.getColumnIndex("paymentPaidAmount")));
            }
            memberResponseList.add(memberResponse);
        }

        db.close();
        return memberResponseList;

    }

    public void insertMessMember(String ownerId, String memberName, String
            memberMobileNumber, String memberAlternetMobileNumber, String memberAddress,
                                 String paymentMessAmount, String paymentPaidAmount, String messDays,
                                 int memberStatus, String memberNetworkStatus) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("memberName", memberName);
        contentValues.put("memberMobileNumber", memberMobileNumber);
        contentValues.put("memberAlternetMobileNumber", memberAlternetMobileNumber);
        contentValues.put("memberAddress", memberAddress);
        contentValues.put("memberStatus", memberStatus);
        contentValues.put("memberNetworkStatus", memberNetworkStatus);

        db.insert(MEMBER_TABLE, null, contentValues);
        db.close();

        getLastInsertedMemberDetails(memberName, paymentMessAmount, paymentPaidAmount, messDays, memberStatus, memberNetworkStatus);

    }

    public void getLastInsertedMemberDetails(String memberName, String
            paymentMessAmount, String paymentPaidAmount, String messDays, int memberStatus, String
                                                     memberNetworkStatus) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM member ORDER BY `memberId` DESC LIMIT 1", null);

        while (cursor.moveToNext()) {
            String memberId = cursor.getString(cursor.getColumnIndex("memberId"));
            insertMessMemberPayment(memberId, memberName, paymentMessAmount, paymentPaidAmount, messDays, memberStatus, memberNetworkStatus);
        }

        db.close();

    }

    public void updateMessMember(String memberId, String memberName, String
            memberMobileNumber, String memberAlternetMobileNumber, String memberAddress,
                                 int memberStatus) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("memberName", memberName);
        contentValues.put("memberMobileNumber", memberMobileNumber);
        contentValues.put("memberAlternetMobileNumber", memberAlternetMobileNumber);
        contentValues.put("memberAddress", memberAddress);
        contentValues.put("memberStatus", 0);

        db.update(MEMBER_TABLE, contentValues, "memberId=?", new String[]{memberId});
        db.close();

    }

    public void insertMessMemberPayment(String memberId, String memberName, String
            paymentMessAmount, String paymentPaidAmount, String messDays, int paymentStatus, String
                                                paymentNetworkStatus) {

        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String paymentDate = df.format(c);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("memberId", memberId);
        contentValues.put("memberName", memberName);
        contentValues.put("paymentMessAmount", paymentMessAmount);
        contentValues.put("paymentPaidAmount", paymentPaidAmount);
        contentValues.put("messTotalDays", messDays);
        contentValues.put("paymentDate", paymentDate);
        contentValues.put("paymentStatus", paymentStatus);
        contentValues.put("paymentNetworkStatus", paymentNetworkStatus);

        db.insert(MEMBER_PAYMENT_TABLE, null, contentValues);
        db.close();

    }

    public boolean saveMessInvoice(String memberId, String memberName, String messType, String
            messInvoiceDate, String messInvoiceNetworkStatus, int messInvoiceStatus) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("memberId", memberId);
        contentValues.put("memberName", memberName);
        contentValues.put("messType", messType);
        contentValues.put("messInvoiceDate", messInvoiceDate);
        contentValues.put("messInvoiceNetworkStatus", messInvoiceNetworkStatus);
        contentValues.put("messInvoiceStatus", String.valueOf(messInvoiceStatus));

        db.insert(MESS_INVOICE_TABLE, null, contentValues);
        db.close();

        return true;

    }

    public List<MessInvoiceResponse> gerMessInvoiceUserWiseList(String
                                                                        memberMobileNumber, String paymentDate) {

        List<MessInvoiceResponse> messInvoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM mess_invoice WHERE memberName='" + memberMobileNumber + "' AND messInvoiceDate LIKE '%" + paymentDate + "%'", null);
        MessInvoiceResponse messInvoiceResponse;
        while (cursor.moveToNext()) {
            messInvoiceResponse = new MessInvoiceResponse();
            messInvoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
            messInvoiceResponse.setMemberId(cursor.getString(cursor.getColumnIndex("memberId")));
            messInvoiceResponse.setMemberName(cursor.getString(cursor.getColumnIndex("memberName")));
            messInvoiceResponse.setMessType(cursor.getString(cursor.getColumnIndex("messType")));
            messInvoiceResponse.setMessInvoiceDate(cursor.getString(cursor.getColumnIndex("messInvoiceDate")));
            messInvoiceResponse.setMessInvoiceNetworkStatus(cursor.getString(cursor.getColumnIndex("messInvoiceNetworkStatus")));
            messInvoiceResponse.setMessInvoiceStatus(cursor.getString(cursor.getColumnIndex("messInvoiceStatus")));

            messInvoiceResponseList.add(messInvoiceResponse);
        }

        db.close();
        return messInvoiceResponseList;

    }

    public void addMessMember(MemberResponse memberResponse) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("memberId", memberResponse.getMemberId());
        contentValues.put("memberName", memberResponse.getMemberName());
        contentValues.put("memberMobileNumber", memberResponse.getMemberMobileNumber());
        contentValues.put("memberAlternetMobileNumber", memberResponse.getMemberAlternetMobileNumber());
        contentValues.put("memberAddress", memberResponse.getMemberAddress());
        contentValues.put("memberStatus", memberResponse.getMemberStatus());
        contentValues.put("memberNetworkStatus", memberResponse.getMemberNetworkStatus());

        db.insert(MEMBER_TABLE, null, contentValues);
        db.close();

    }

    public void addMessMemberPayment(MemberResponse memberResponse) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("memberId", memberResponse.getMemberId());
        contentValues.put("memberName", memberResponse.getMemberName());
        contentValues.put("paymentMessAmount", memberResponse.getPaymentMessAmount());
        contentValues.put("paymentPaidAmount", memberResponse.getPaymentPaidAmount());
        contentValues.put("messTotalDays", memberResponse.getMessTotalDays());
        contentValues.put("paymentDate", memberResponse.getPaymentDate());
        contentValues.put("paymentStatus", memberResponse.getPaymentStatus());
        contentValues.put("paymentNetworkStatus", memberResponse.getPaymentNetworkStatus());

        db.insert(MEMBER_PAYMENT_TABLE, null, contentValues);
        db.close();

    }

    public void addMessInvoice(MessInvoiceResponse messInvoiceResponse) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("memberId", messInvoiceResponse.getMemberId());
        contentValues.put("memberName", messInvoiceResponse.getMemberName());
        contentValues.put("messType", messInvoiceResponse.getMessType());
        contentValues.put("messInvoiceDate", messInvoiceResponse.getMessInvoiceDate());
        contentValues.put("messInvoiceNetworkStatus", messInvoiceResponse.getMessInvoiceNetworkStatus());
        contentValues.put("messInvoiceStatus", messInvoiceResponse.getMessInvoiceStatus());

        db.insert(MESS_INVOICE_TABLE, null, contentValues);
        db.close();

    }

    public void updateSyncMessMember(String memberId, int memberStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("memberStatus", memberStatus);
        db.update(MEMBER_TABLE, contentValues, "memberId=?", new String[]{memberId});
        db.close();
    }

    public void updateSyncMessMemberPayment(String paymentId, int paymentStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("paymentStatus", paymentStatus);
        db.update(MEMBER_PAYMENT_TABLE, contentValues, "paymentId=?", new String[]{paymentId});
        db.close();
    }

    public void updateSyncMessInvoice(String invoiceId, int messInvoiceStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("messInvoiceStatus", messInvoiceStatus);
        db.update(MESS_INVOICE_TABLE, contentValues, "invoiceId=?", new String[]{invoiceId});
        db.close();
    }

    public Cursor getUnSynchronizeMessMember(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + MEMBER_TABLE + " WHERE memberStatus = '" + status + "' ";
        Cursor cursor = db.rawQuery(sql, null);
        return cursor;
    }

    public Cursor getUnSynchronizeMessMemberPayment(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + MEMBER_PAYMENT_TABLE + " WHERE paymentStatus = '" + status + "' ";
        Cursor cursor = db.rawQuery(sql, null);
        return cursor;
    }

    public Cursor getUnSynchronizeMessInvoice(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + MESS_INVOICE_TABLE + " WHERE messInvoiceStatus = '" + status + "' ";
        Cursor cursor = db.rawQuery(sql, null);
        return cursor;
    }

    public List<MessInvoiceResponse> getInvoiceMessInvoiceReportList() {

        List<MessInvoiceResponse> messInvoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM mess_invoice", null);
        MessInvoiceResponse messInvoiceResponse;
        while (cursor.moveToNext()) {
            messInvoiceResponse = new MessInvoiceResponse();
            messInvoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
            messInvoiceResponse.setMemberId(cursor.getString(cursor.getColumnIndex("memberId")));
            messInvoiceResponse.setMemberName(cursor.getString(cursor.getColumnIndex("memberName")));
            messInvoiceResponse.setMessType(cursor.getString(cursor.getColumnIndex("messType")));
            messInvoiceResponse.setMessInvoiceDate(cursor.getString(cursor.getColumnIndex("messInvoiceDate")));
            messInvoiceResponse.setMessInvoiceNetworkStatus(cursor.getString(cursor.getColumnIndex("messInvoiceNetworkStatus")));
            messInvoiceResponse.setMessInvoiceStatus(cursor.getString(cursor.getColumnIndex("messInvoiceStatus")));

            messInvoiceResponseList.add(messInvoiceResponse);
        }

        db.close();
        return messInvoiceResponseList;

    }

    public List<MessInvoiceResponse> getInvoiceMessInvoiceDateWiseReportList(String invoiceDate) {

        List<MessInvoiceResponse> messInvoiceResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM mess_invoice WHERE messInvoiceDate LIKE '%" + invoiceDate + "%'", null);
        MessInvoiceResponse messInvoiceResponse;
        while (cursor.moveToNext()) {
            messInvoiceResponse = new MessInvoiceResponse();
            messInvoiceResponse.setInvoiceId(cursor.getString(cursor.getColumnIndex("invoiceId")));
            messInvoiceResponse.setMemberId(cursor.getString(cursor.getColumnIndex("memberId")));
            messInvoiceResponse.setMemberName(cursor.getString(cursor.getColumnIndex("memberName")));
            messInvoiceResponse.setMessType(cursor.getString(cursor.getColumnIndex("messType")));
            messInvoiceResponse.setMessInvoiceDate(cursor.getString(cursor.getColumnIndex("messInvoiceDate")));
            messInvoiceResponse.setMessInvoiceNetworkStatus(cursor.getString(cursor.getColumnIndex("messInvoiceNetworkStatus")));
            messInvoiceResponse.setMessInvoiceStatus(cursor.getString(cursor.getColumnIndex("messInvoiceStatus")));

            messInvoiceResponseList.add(messInvoiceResponse);
        }

        db.close();
        return messInvoiceResponseList;

    }

    public boolean saveMessToken(String tokenCode, String memberId, String memberName, String memberMobile,
                                 String memberType, String messType, String tokenAmount, String tokenDate,
                                 String tokenNetworkStatus, String tokenState, int tokenStatus, int verifyStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("tokenCode", tokenCode);
        contentValues.put("memberId", memberId);
        contentValues.put("memberName", memberName);
        contentValues.put("memberMobile", memberMobile);
        contentValues.put("memberType", memberType);
        contentValues.put("messType", messType);
        contentValues.put("tokenAmount", tokenAmount);
        contentValues.put("tokenDate", tokenDate);
        contentValues.put("tokenNetworkStatus", tokenNetworkStatus);
        contentValues.put("tokenState", tokenState);
        contentValues.put("tokenStatus", tokenStatus);
        contentValues.put("verifyStatus", verifyStatus);
        db.insert(MESS_TOKEN_TABLE, null, contentValues);
        db.close();
        return true;
    }

    public MessTokenResponse getMessTokenByCode(String tokenCode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + MESS_TOKEN_TABLE + " WHERE tokenCode = ? LIMIT 1",
                new String[]{tokenCode});
        MessTokenResponse token = null;
        if (cursor.moveToFirst()) {
            token = cursorToMessToken(cursor);
        }
        cursor.close();
        db.close();
        return token;
    }

    public void markMessTokenVerified(String tokenId, String verifiedDate, String verifyNetworkStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("tokenState", "verified");
        contentValues.put("verifiedDate", verifiedDate);
        contentValues.put("verifyNetworkStatus", verifyNetworkStatus);
        contentValues.put("verifyStatus", 0);
        db.update(MESS_TOKEN_TABLE, contentValues, "tokenId=?", new String[]{tokenId});
        db.close();
    }

    public void updateSyncMessToken(String tokenId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("tokenStatus", 1);
        db.update(MESS_TOKEN_TABLE, contentValues, "tokenId=?", new String[]{tokenId});
        db.close();
    }

    public void updateSyncMessTokenVerify(String tokenId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("verifyStatus", 1);
        db.update(MESS_TOKEN_TABLE, contentValues, "tokenId=?", new String[]{tokenId});
        db.close();
    }

    public Cursor getUnSynchronizeMessToken(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + MESS_TOKEN_TABLE + " WHERE tokenStatus = ?",
                new String[]{String.valueOf(status)});
    }

    public Cursor getUnSynchronizeMessTokenVerify(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + MESS_TOKEN_TABLE
                        + " WHERE tokenState = 'verified' AND verifyStatus = ?",
                new String[]{String.valueOf(status)});
    }

    public int countPendingMessTokens() {
        int tokens = countUnsyncedRows(MESS_TOKEN_TABLE, "tokenStatus");
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + MESS_TOKEN_TABLE
                            + " WHERE tokenState = 'verified' AND (verifyStatus = 0 OR verifyStatus = '0')",
                    null);
            if (cursor.moveToFirst()) {
                return tokens + cursor.getInt(0);
            }
            return tokens;
        } catch (Exception e) {
            return tokens;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void addMessToken(MessTokenResponse messTokenResponse) {
        if (messTokenResponse == null || messTokenResponse.getTokenCode() == null) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor existing = db.rawQuery("SELECT tokenId FROM " + MESS_TOKEN_TABLE + " WHERE tokenCode = ? LIMIT 1",
                new String[]{messTokenResponse.getTokenCode()});
        boolean hasRow = existing.moveToFirst();
        existing.close();
        if (hasRow) {
            db.close();
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("tokenCode", messTokenResponse.getTokenCode());
        contentValues.put("memberId", messTokenResponse.getMemberId());
        contentValues.put("memberName", messTokenResponse.getMemberName());
        contentValues.put("memberMobile", messTokenResponse.getMemberMobile());
        contentValues.put("memberType", messTokenResponse.getMemberType());
        contentValues.put("messType", messTokenResponse.getMessType());
        contentValues.put("tokenAmount", messTokenResponse.getTokenAmount());
        contentValues.put("tokenDate", messTokenResponse.getTokenDate());
        contentValues.put("verifiedDate", messTokenResponse.getVerifiedDate());
        contentValues.put("tokenNetworkStatus", messTokenResponse.getTokenNetworkStatus());
        contentValues.put("tokenState", messTokenResponse.getTokenState());
        contentValues.put("tokenStatus", 1);
        contentValues.put("verifyNetworkStatus", messTokenResponse.getVerifyNetworkStatus());
        contentValues.put("verifyStatus", messTokenResponse.getVerifiedDate() != null ? 1 : 0);
        db.insert(MESS_TOKEN_TABLE, null, contentValues);
        db.close();
    }

    private MessTokenResponse cursorToMessToken(Cursor cursor) {
        MessTokenResponse token = new MessTokenResponse();
        token.setTokenId(cursor.getString(cursor.getColumnIndex("tokenId")));
        token.setTokenCode(cursor.getString(cursor.getColumnIndex("tokenCode")));
        token.setMemberId(cursor.getString(cursor.getColumnIndex("memberId")));
        token.setMemberName(cursor.getString(cursor.getColumnIndex("memberName")));
        token.setMemberMobile(cursor.getString(cursor.getColumnIndex("memberMobile")));
        token.setMemberType(cursor.getString(cursor.getColumnIndex("memberType")));
        token.setMessType(cursor.getString(cursor.getColumnIndex("messType")));
        token.setTokenAmount(cursor.getString(cursor.getColumnIndex("tokenAmount")));
        token.setTokenDate(cursor.getString(cursor.getColumnIndex("tokenDate")));
        token.setVerifiedDate(cursor.getString(cursor.getColumnIndex("verifiedDate")));
        token.setTokenNetworkStatus(cursor.getString(cursor.getColumnIndex("tokenNetworkStatus")));
        token.setTokenState(cursor.getString(cursor.getColumnIndex("tokenState")));
        token.setTokenStatus(cursor.getString(cursor.getColumnIndex("tokenStatus")));
        token.setVerifyNetworkStatus(cursor.getString(cursor.getColumnIndex("verifyNetworkStatus")));
        token.setVerifyStatus(cursor.getString(cursor.getColumnIndex("verifyStatus")));
        return token;
    }


    public List<MemberResponse> getMemberDetails(String memberId) {

        List<MemberResponse> memberResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM member WHERE memberId = '" + memberId + "'", null);
        MemberResponse memberResponse;
        while (cursor.moveToNext()) {
            memberResponse = new MemberResponse();
            memberResponse.setMemberId(cursor.getString(cursor.getColumnIndex("memberId")));
            memberResponse.setMemberName(cursor.getString(cursor.getColumnIndex("memberName")));
            memberResponse.setMemberMobileNumber(cursor.getString(cursor.getColumnIndex("memberMobileNumber")));
            memberResponse.setMemberAlternetMobileNumber(cursor.getString(cursor.getColumnIndex("memberAlternetMobileNumber")));
            memberResponse.setMemberAddress(cursor.getString(cursor.getColumnIndex("memberAddress")));
            memberResponseList.add(memberResponse);
        }

        db.close();
        return memberResponseList;

    }

    public List<MemberResponse> getInvoiceMemberList() {

        List<MemberResponse> memberResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM member", null);
        MemberResponse memberResponse;
        while (cursor.moveToNext()) {
            memberResponse = new MemberResponse();
            memberResponse.setMemberId(cursor.getString(cursor.getColumnIndex("memberId")));
            memberResponse.setMemberName(cursor.getString(cursor.getColumnIndex("memberName")));
            memberResponse.setMemberMobileNumber(cursor.getString(cursor.getColumnIndex("memberMobileNumber")));
            memberResponse.setMemberAlternetMobileNumber(cursor.getString(cursor.getColumnIndex("memberAlternetMobileNumber")));
            memberResponse.setMemberAddress(cursor.getString(cursor.getColumnIndex("memberAddress")));
            memberResponseList.add(memberResponse);
        }

        db.close();
        return memberResponseList;

    }


    public List<MemberResponse> getMemberPaymentDetails(String memberId, String paymentDate) {

        List<MemberResponse> memberResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM member LEFT JOIN member_payment ON member_payment.memberId = member.memberId WHERE member_payment.memberId = '" + memberId + "' AND member_payment.paymentDate LIKE '%" + paymentDate + "%' GROUP BY member_payment.memberId", null);
        MemberResponse memberResponse;
        while (cursor.moveToNext()) {
            memberResponse = new MemberResponse();
            memberResponse.setMemberId(cursor.getString(cursor.getColumnIndex("memberId")));
            memberResponse.setMemberName(cursor.getString(cursor.getColumnIndex("memberName")));
            memberResponse.setMemberMobileNumber(cursor.getString(cursor.getColumnIndex("memberMobileNumber")));
            memberResponse.setMemberAlternetMobileNumber(cursor.getString(cursor.getColumnIndex("memberAlternetMobileNumber")));
            memberResponse.setMemberAddress(cursor.getString(cursor.getColumnIndex("memberAddress")));
            memberResponse.setMessTotalDays(cursor.getString(cursor.getColumnIndex("messTotalDays")));
            memberResponse.setPaymentMessAmount(cursor.getString(cursor.getColumnIndex("paymentMessAmount")));
            memberResponse.setPaymentDate(cursor.getString(cursor.getColumnIndex("paymentDate")));
            Cursor cursor1 = db.rawQuery("SELECT SUM(paymentPaidAmount) as paymentPaidAmount FROM member_payment WHERE memberId='" + memberId + "'", null);
            while (cursor1.moveToNext()) {
                memberResponse.setPaymentPaidAmount(cursor1.getString(cursor1.getColumnIndex("paymentPaidAmount")));
            }
            memberResponseList.add(memberResponse);
        }

        db.close();
        return memberResponseList;

    }


    public void updateMessPendingPayment(String memberId, String memberName, String
            messDays, String messAmount, String paidAmount, int paymentStatus, String
                                                 paymentNetworkStatus) {

        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String paymentDate = df.format(c);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("memberId", memberId);
        contentValues.put("memberName", memberName);
        contentValues.put("paymentMessAmount", messAmount);
        contentValues.put("paymentPaidAmount", paidAmount);
        contentValues.put("messTotalDays", messDays);
        contentValues.put("paymentDate", paymentDate);
        contentValues.put("paymentStatus", paymentStatus);
        contentValues.put("paymentNetworkStatus", paymentNetworkStatus);

        db.insert(MEMBER_PAYMENT_TABLE, null, contentValues);
        db.close();


    }

    public List<MemberResponse> getInvoiceMemberPaymentReportList(String memberId) {

        List<MemberResponse> memberResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM member_payment WHERE memberId = '" + memberId + "'", null);
        MemberResponse memberResponse;
        while (cursor.moveToNext()) {
            memberResponse = new MemberResponse();
            memberResponse.setMemberId(cursor.getString(cursor.getColumnIndex("memberId")));
            memberResponse.setMemberName(cursor.getString(cursor.getColumnIndex("memberName")));
            memberResponse.setMessTotalDays(cursor.getString(cursor.getColumnIndex("messTotalDays")));
            memberResponse.setPaymentMessAmount(cursor.getString(cursor.getColumnIndex("paymentMessAmount")));
            memberResponse.setPaymentDate(cursor.getString(cursor.getColumnIndex("paymentDate")));
            memberResponse.setPaymentPaidAmount(cursor.getString(cursor.getColumnIndex("paymentPaidAmount")));

            memberResponseList.add(memberResponse);
        }

        db.close();
        return memberResponseList;

    }

    public List<ExpenseResponse> getDateWiseExpenseList(String expenseDate) {

        List<ExpenseResponse> expenseResponseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + EXPENSES_TABLE + " WHERE expensesDate LIKE '%" + expenseDate + "%'", null);
        ExpenseResponse expenseResponse;
        while (cursor.moveToNext()) {
            expenseResponse = new ExpenseResponse();
            expenseResponse.setExpenseId(cursor.getString(cursor.getColumnIndex("expensesId")));
            expenseResponse.setExpenseName(cursor.getString(cursor.getColumnIndex("expensesName")));
            expenseResponse.setExpenseAmount(cursor.getString(cursor.getColumnIndex("expensesAmount")));
            expenseResponse.setExpenseDate(cursor.getString(cursor.getColumnIndex("expensesDate")));
            expenseResponse.setExpenseNetworkStatus(cursor.getString(cursor.getColumnIndex("expensesNetworkStatus")));
            expenseResponse.setExpenseStatus(cursor.getString(cursor.getColumnIndex("expensesStatus")));
            expenseResponseList.add(expenseResponse);
        }

        db.close();
        return expenseResponseList;

    }

    public void deleteMember(MemberResponse memberResponse) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(MEMBER_TABLE, "memberId = ?", new String[]{memberResponse.getMemberId()});
        db.close();
    }

    /********************************************** Combo CRUD  **********************************************/

    public long insertCombo(ComboResponse combo) {
        if (combo == null) {
            return -1;
        }
        String networkStatus = combo.getComboNetworkStatus();
        if (networkStatus != null && !networkStatus.trim().isEmpty()) {
            String existingId = getComboIdByNetworkStatus(networkStatus);
            if (existingId != null) {
                updateComboFromResponse(existingId, combo);
                try {
                    return Long.parseLong(existingId);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = comboContentValues(combo);
        long id = db.insert(COMBO_TABLE, null, values);
        db.close();
        return id;
    }

    public void updateComboFromResponse(String comboId, ComboResponse combo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = comboContentValues(combo);
        db.update(COMBO_TABLE, values, "comboId=?", new String[]{comboId});
        db.close();
    }

    private ContentValues comboContentValues(ComboResponse combo) {
        ContentValues values = new ContentValues();
        String price = combo.getComboPrice() != null ? combo.getComboPrice() : "0";
        String cgst = combo.getComboCGST() != null ? combo.getComboCGST() : "";
        String sgst = combo.getComboSGST() != null ? combo.getComboSGST() : "";
        float cgstAmt = 0f;
        float sgstAmt = 0f;
        try {
            cgstAmt = Float.parseFloat(cgst.isEmpty() ? "0" : cgst);
        } catch (NumberFormatException ignored) {
        }
        try {
            sgstAmt = Float.parseFloat(sgst.isEmpty() ? "0" : sgst);
        } catch (NumberFormatException ignored) {
        }
        float withGst = 0f;
        try {
            withGst = Float.parseFloat(price) + (Float.parseFloat(price) * ((cgstAmt + sgstAmt) / 100f));
        } catch (NumberFormatException ignored) {
        }
        values.put("comboName", combo.getComboName());
        values.put("comboCode", combo.getComboCode());
        values.put("comboPrice", price);
        values.put("comboCGST", cgst);
        values.put("comboSGST", sgst);
        values.put("comboWithGSTPrice", String.valueOf(withGst));
        values.put("comboActiveStatus", combo.getComboActiveStatus() != null ? combo.getComboActiveStatus() : "1");
        values.put("comboDeletedStatus", combo.getComboDeletedStatus() != null ? combo.getComboDeletedStatus() : "0");
        putOptionalColumn(values, "comboNetworkStatus", combo.getComboNetworkStatus());
        if (combo.getComboStatus() != null && !combo.getComboStatus().trim().isEmpty()) {
            values.put("comboStatus", combo.getComboStatus());
        } else {
            values.put("comboStatus", 0);
        }
        int sort = 0;
        try {
            if (combo.getComboSortOrder() != null && !combo.getComboSortOrder().trim().isEmpty()) {
                sort = Integer.parseInt(combo.getComboSortOrder().trim());
            }
        } catch (NumberFormatException ignored) {
        }
        values.put("comboSortOrder", sort);
        return values;
    }

    public void updateCombo(String comboId, String comboName, String comboCode, String comboPrice,
                            String comboCGST, String comboSGST, String comboActiveStatus, int comboStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ComboResponse combo = new ComboResponse();
        combo.setComboName(comboName);
        combo.setComboCode(comboCode);
        combo.setComboPrice(comboPrice);
        combo.setComboCGST(comboCGST);
        combo.setComboSGST(comboSGST);
        combo.setComboActiveStatus(comboActiveStatus);
        combo.setComboStatus("0");
        ComboResponse existing = getComboDetail(comboId);
        if (existing != null) {
            combo.setComboDeletedStatus(existing.getComboDeletedStatus());
            combo.setComboNetworkStatus(existing.getComboNetworkStatus());
            combo.setComboSortOrder(existing.getComboSortOrder());
        }
        db.update(COMBO_TABLE, comboContentValues(combo), "comboId=?", new String[]{comboId});
        db.close();
    }

    public void setComboActiveStatus(String comboId, boolean active) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("comboActiveStatus", active ? "1" : "0");
        values.put("comboStatus", 0);
        db.update(COMBO_TABLE, values, "comboId=?", new String[]{comboId});
        db.close();
    }

    public void deleteCombo(String comboId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("comboDeletedStatus", "1");
        values.put("comboActiveStatus", "0");
        values.put("comboStatus", 0);
        db.update(COMBO_TABLE, values, "comboId=?", new String[]{comboId});
        ContentValues items = new ContentValues();
        items.put("comboItemDeletedStatus", "1");
        items.put("comboItemStatus", 0);
        db.update(COMBO_ITEM_TABLE, items, "comboId=?", new String[]{comboId});
        db.close();
    }

    public String getComboIdByNetworkStatus(String comboNetworkStatus) {
        if (comboNetworkStatus == null || comboNetworkStatus.trim().isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT comboId FROM " + COMBO_TABLE + " WHERE comboNetworkStatus = ? LIMIT 1",
                new String[]{comboNetworkStatus});
        String id = null;
        if (cursor.moveToFirst()) {
            id = cursor.getString(0);
        }
        cursor.close();
        return id;
    }

    public ComboResponse getComboDetail(String comboId) {
        if (comboId == null || comboId.trim().isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + COMBO_TABLE + " WHERE comboId = ?", new String[]{comboId});
        ComboResponse combo = null;
        if (cursor.moveToFirst()) {
            combo = mapCombo(cursor);
        }
        cursor.close();
        return combo;
    }

    public List<ComboResponse> getComboList(boolean activeOnly, boolean includeDeleted) {
        List<ComboResponse> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + COMBO_TABLE + " WHERE 1=1");
        if (!includeDeleted) {
            sql.append(" AND IFNULL(comboDeletedStatus, '0') = '0'");
        }
        if (activeOnly) {
            sql.append(" AND IFNULL(comboActiveStatus, '1') = '1'");
        }
        sql.append(" ORDER BY comboSortOrder ASC, comboId ASC");
        Cursor cursor = db.rawQuery(sql.toString(), null);
        while (cursor.moveToNext()) {
            list.add(mapCombo(cursor));
        }
        cursor.close();
        return list;
    }

    public List<ComboResponse> getPosComboList(String tableNumber, String cartOrderStatus) {
        List<ComboResponse> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String safeTable = tableNumber != null ? tableNumber : "";
        String safeOrder = cartOrderStatus != null ? cartOrderStatus : "";
        Cursor cursor = db.rawQuery(
                "SELECT c.*, cart.productQuantity AS comboCartQuantity FROM " + COMBO_TABLE + " c "
                        + "LEFT JOIN " + CART_PRODUCT_TABLE + " cart "
                        + "ON cart.comboId = CAST(c.comboId AS TEXT) AND cart.cartItemType = 'COMBO' "
                        + "AND cart.noOfTable = ? AND cart.cartOrderStatus = ? "
                        + "WHERE IFNULL(c.comboDeletedStatus, '0') = '0' "
                        + "AND IFNULL(c.comboActiveStatus, '1') = '1' "
                        + "ORDER BY c.comboSortOrder ASC, c.comboId ASC",
                new String[]{safeTable, safeOrder});
        while (cursor.moveToNext()) {
            ComboResponse combo = mapCombo(cursor);
            int qtyIdx = cursor.getColumnIndex("comboCartQuantity");
            if (qtyIdx >= 0 && !cursor.isNull(qtyIdx)) {
                combo.setComboCartQuantity(cursor.getString(qtyIdx));
            }
            list.add(combo);
        }
        cursor.close();
        return list;
    }

    public List<ComboResponse> searchCombos(String query, String tableNumber, String cartOrderStatus) {
        List<ComboResponse> list = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return list;
        }
        String trimmed = query.trim();
        String contains = "%" + escapeLike(trimmed) + "%";
        String prefix = escapeLike(trimmed) + "%";
        String safeTable = tableNumber != null ? tableNumber : "";
        String safeOrder = cartOrderStatus != null ? cartOrderStatus : "";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT c.*, cart.productQuantity AS comboCartQuantity FROM " + COMBO_TABLE + " c "
                        + "LEFT JOIN " + CART_PRODUCT_TABLE + " cart "
                        + "ON cart.comboId = CAST(c.comboId AS TEXT) AND cart.cartItemType = 'COMBO' "
                        + "AND cart.noOfTable = ? AND cart.cartOrderStatus = ? "
                        + "WHERE IFNULL(c.comboDeletedStatus, '0') = '0' "
                        + "AND IFNULL(c.comboActiveStatus, '1') = '1' "
                        + "AND (IFNULL(c.comboName, '') LIKE ? ESCAPE '\\' OR IFNULL(c.comboCode, '') LIKE ? ESCAPE '\\') "
                        + "ORDER BY CASE WHEN IFNULL(c.comboCode, '') LIKE ? ESCAPE '\\' THEN 0 "
                        + "WHEN IFNULL(c.comboName, '') LIKE ? ESCAPE '\\' THEN 1 ELSE 2 END, c.comboName "
                        + "LIMIT 100",
                new String[]{safeTable, safeOrder, contains, contains, prefix, prefix});
        while (cursor.moveToNext()) {
            ComboResponse combo = mapCombo(cursor);
            int qtyIdx = cursor.getColumnIndex("comboCartQuantity");
            if (qtyIdx >= 0 && !cursor.isNull(qtyIdx)) {
                combo.setComboCartQuantity(cursor.getString(qtyIdx));
            }
            list.add(combo);
        }
        cursor.close();
        return list;
    }

    public ComboResponse getLatestCombo() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + COMBO_TABLE + " ORDER BY comboId DESC LIMIT 1", null);
        ComboResponse combo = null;
        if (cursor.moveToFirst()) {
            combo = mapCombo(cursor);
        }
        cursor.close();
        return combo;
    }

    private ComboResponse mapCombo(Cursor cursor) {
        ComboResponse combo = new ComboResponse();
        combo.setComboId(cursor.getString(cursor.getColumnIndex("comboId")));
        combo.setComboName(cursor.getString(cursor.getColumnIndex("comboName")));
        combo.setComboCode(cursor.getString(cursor.getColumnIndex("comboCode")));
        combo.setComboPrice(cursor.getString(cursor.getColumnIndex("comboPrice")));
        combo.setComboCGST(cursor.getString(cursor.getColumnIndex("comboCGST")));
        combo.setComboSGST(cursor.getString(cursor.getColumnIndex("comboSGST")));
        combo.setComboWithGSTPrice(cursor.getString(cursor.getColumnIndex("comboWithGSTPrice")));
        combo.setComboActiveStatus(cursor.getString(cursor.getColumnIndex("comboActiveStatus")));
        combo.setComboDeletedStatus(cursor.getString(cursor.getColumnIndex("comboDeletedStatus")));
        combo.setComboNetworkStatus(cursor.getString(cursor.getColumnIndex("comboNetworkStatus")));
        combo.setComboStatus(cursor.getString(cursor.getColumnIndex("comboStatus")));
        combo.setComboSortOrder(cursor.getString(cursor.getColumnIndex("comboSortOrder")));
        return combo;
    }

    public List<ComboItemResponse> getComboItemList(String comboId) {
        List<ComboItemResponse> list = new ArrayList<>();
        if (comboId == null) {
            return list;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT ci.*, p.productName, pp.portionName FROM " + COMBO_ITEM_TABLE + " ci "
                        + "LEFT JOIN " + PRODUCT_TABLE + " p ON p.productId = ci.productId "
                        + "LEFT JOIN " + PRODUCT_PORTION_TABLE + " pp ON pp.portionId = ci.portionId "
                        + "WHERE ci.comboId = ? AND IFNULL(ci.comboItemDeletedStatus, '0') = '0' "
                        + "ORDER BY ci.comboItemSortOrder ASC, ci.comboItemId ASC",
                new String[]{comboId});
        while (cursor.moveToNext()) {
            list.add(mapComboItem(cursor));
        }
        cursor.close();
        return list;
    }

    private ComboItemResponse mapComboItem(Cursor cursor) {
        ComboItemResponse item = new ComboItemResponse();
        mapStringColumn(cursor, "comboItemId", item::setComboItemId);
        mapStringColumn(cursor, "comboId", item::setComboId);
        mapStringColumn(cursor, "productId", item::setProductId);
        mapStringColumn(cursor, "portionId", item::setPortionId);
        mapStringColumn(cursor, "comboItemQuantity", item::setComboItemQuantity);
        mapStringColumn(cursor, "comboItemSortOrder", item::setComboItemSortOrder);
        mapStringColumn(cursor, "comboItemDeletedStatus", item::setComboItemDeletedStatus);
        mapStringColumn(cursor, "comboItemNetworkStatus", item::setComboItemNetworkStatus);
        mapStringColumn(cursor, "comboItemStatus", item::setComboItemStatus);
        mapStringColumn(cursor, "productName", item::setProductName);
        mapStringColumn(cursor, "portionName", item::setPortionName);
        mapStringColumn(cursor, "comboNetworkStatus", item::setComboNetworkStatus);
        mapStringColumn(cursor, "productNetworkStatus", item::setProductNetworkStatus);
        mapStringColumn(cursor, "portionNetworkStatus", item::setPortionNetworkStatus);
        return item;
    }

    public boolean saveComboItems(String comboId, List<ComboItemDraft> drafts) {
        if (comboId == null) {
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor existing = db.rawQuery(
                    "SELECT comboItemId FROM " + COMBO_ITEM_TABLE
                            + " WHERE comboId = ? AND IFNULL(comboItemDeletedStatus, '0') = '0'",
                    new String[]{comboId});
            java.util.HashSet<String> keepIds = new java.util.HashSet<>();
            if (drafts != null) {
                for (int i = 0; i < drafts.size(); i++) {
                    ComboItemDraft draft = drafts.get(i);
                    if (draft == null) {
                        continue;
                    }
                    ContentValues values = new ContentValues();
                    values.put("comboId", comboId);
                    values.put("productId", draft.getProductId());
                    if (draft.getPortionId() != null && !draft.getPortionId().trim().isEmpty()) {
                        values.put("portionId", draft.getPortionId());
                    } else {
                        values.putNull("portionId");
                    }
                    values.put("comboItemQuantity", draft.getQuantity() != null ? draft.getQuantity() : "1");
                    values.put("comboItemSortOrder", i);
                    values.put("comboItemDeletedStatus", "0");
                    values.put("comboItemStatus", 0);
                    if (draft.getComboItemNetworkStatus() == null || draft.getComboItemNetworkStatus().trim().isEmpty()) {
                        values.put("comboItemNetworkStatus", getRandomString(10));
                    } else {
                        values.put("comboItemNetworkStatus", draft.getComboItemNetworkStatus());
                    }
                    if (draft.getComboItemId() != null && !draft.getComboItemId().trim().isEmpty()) {
                        db.update(COMBO_ITEM_TABLE, values, "comboItemId=?", new String[]{draft.getComboItemId()});
                        keepIds.add(draft.getComboItemId());
                    } else {
                        long newId = db.insert(COMBO_ITEM_TABLE, null, values);
                        if (newId > 0) {
                            keepIds.add(String.valueOf(newId));
                        }
                    }
                }
            }
            while (existing.moveToNext()) {
                String id = existing.getString(0);
                if (!keepIds.contains(id)) {
                    ContentValues soft = new ContentValues();
                    soft.put("comboItemDeletedStatus", "1");
                    soft.put("comboItemStatus", 0);
                    db.update(COMBO_ITEM_TABLE, soft, "comboItemId=?", new String[]{id});
                }
            }
            existing.close();
            ContentValues comboDirty = new ContentValues();
            comboDirty.put("comboStatus", 0);
            db.update(COMBO_TABLE, comboDirty, "comboId=?", new String[]{comboId});
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public void upsertComboItemFromServer(ComboItemResponse item) {
        if (item == null) {
            return;
        }
        String comboId = item.getComboId();
        if ((comboId == null || comboId.trim().isEmpty()) && item.getComboNetworkStatus() != null) {
            comboId = getComboIdByNetworkStatus(item.getComboNetworkStatus());
        }
        String productId = item.getProductId();
        if ((productId == null || productId.trim().isEmpty()) && item.getProductNetworkStatus() != null) {
            productId = getProductIdByNetworkStatus(item.getProductNetworkStatus());
        }
        String portionId = item.getPortionId();
        if ((portionId == null || portionId.trim().isEmpty()) && item.getPortionNetworkStatus() != null
                && !item.getPortionNetworkStatus().trim().isEmpty()) {
            portionId = getPortionIdByNetworkStatus(item.getPortionNetworkStatus());
        }
        if (comboId == null || productId == null) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("comboId", comboId);
        values.put("productId", productId);
        if (portionId != null && !portionId.trim().isEmpty()) {
            values.put("portionId", portionId);
        } else {
            values.putNull("portionId");
        }
        values.put("comboItemQuantity", item.getComboItemQuantity() != null ? item.getComboItemQuantity() : "1");
        int sort = 0;
        try {
            if (item.getComboItemSortOrder() != null && !item.getComboItemSortOrder().trim().isEmpty()) {
                sort = Integer.parseInt(item.getComboItemSortOrder().trim());
            }
        } catch (NumberFormatException ignored) {
        }
        values.put("comboItemSortOrder", sort);
        values.put("comboItemDeletedStatus", item.getComboItemDeletedStatus() != null ? item.getComboItemDeletedStatus() : "0");
        values.put("comboItemNetworkStatus", item.getComboItemNetworkStatus());
        values.put("comboItemStatus", 1);
        if (item.getComboItemNetworkStatus() != null && !item.getComboItemNetworkStatus().trim().isEmpty()) {
            Cursor existing = db.rawQuery("SELECT comboItemId FROM " + COMBO_ITEM_TABLE + " WHERE comboItemNetworkStatus = ? LIMIT 1",
                    new String[]{item.getComboItemNetworkStatus()});
            if (existing.moveToFirst()) {
                db.update(COMBO_ITEM_TABLE, values, "comboItemId=?", new String[]{existing.getString(0)});
                existing.close();
                return;
            }
            existing.close();
        }
        db.insert(COMBO_ITEM_TABLE, null, values);
    }

    public String getPortionIdByNetworkStatus(String portionNetworkStatus) {
        if (portionNetworkStatus == null || portionNetworkStatus.trim().isEmpty()) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT portionId FROM " + PRODUCT_PORTION_TABLE + " WHERE portionNetworkStatus = ? LIMIT 1",
                new String[]{portionNetworkStatus});
        String id = null;
        if (cursor.moveToFirst()) {
            id = cursor.getString(0);
        }
        cursor.close();
        return id;
    }

    public boolean isProductActive(String productId) {
        if (productId == null) {
            return false;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT productDeletedStatus FROM " + PRODUCT_TABLE + " WHERE productId = ? LIMIT 1",
                new String[]{productId});
        boolean active = false;
        if (cursor.moveToFirst()) {
            String deleted = cursor.getString(0);
            active = deleted == null || "0".equals(deleted);
        }
        cursor.close();
        return active;
    }

    public boolean portionBelongsToProduct(String productId, String portionId) {
        if (productId == null || portionId == null || portionId.trim().isEmpty()) {
            return false;
        }
        ProductPortionResponse portion = getProductPortionById(portionId);
        return portion != null && productId.equals(portion.getProductId());
    }

    public List<ProductCartResponse> getCartComboDetails(String comboId, String tableNumber, String cartOrderStatus) {
        List<ProductCartResponse> list = new ArrayList<>();
        if (comboId == null) {
            return list;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + CART_PRODUCT_TABLE
                        + " WHERE comboId = ? AND cartItemType = 'COMBO' AND noOfTable = ? AND cartOrderStatus = ?",
                new String[]{comboId, tableNumber, cartOrderStatus});
        while (cursor.moveToNext()) {
            ProductCartResponse item = new ProductCartResponse();
            item.setCartId(cursor.getString(cursor.getColumnIndex("cartId")));
            item.setProductId(cursor.getString(cursor.getColumnIndex("productId")));
            item.setProductName(cursor.getString(cursor.getColumnIndex("productName")));
            item.setProductOldPrice(cursor.getString(cursor.getColumnIndex("productOldPrice")));
            item.setProductNewPrice(cursor.getString(cursor.getColumnIndex("productNewPrice")));
            item.setProductUnit(cursor.getString(cursor.getColumnIndex("productUnit")));
            item.setProductQuantity(cursor.getString(cursor.getColumnIndex("productQuantity")));
            item.setProductCGST(cursor.getString(cursor.getColumnIndex("productCGST")));
            item.setProductSGST(cursor.getString(cursor.getColumnIndex("productSGST")));
            item.setNoOfTable(cursor.getString(cursor.getColumnIndex("noOfTable")));
            item.setCartDiscount(cursor.getString(cursor.getColumnIndex("cartDiscount")));
            item.setCartDiscountType(cursor.getString(cursor.getColumnIndex("cartDiscountType")));
            item.setCartOrderStatus(cursor.getString(cursor.getColumnIndex("cartOrderStatus")));
            item.setCartStatus(cursor.getString(cursor.getColumnIndex("cartStatus")));
            mapCartLineSnapshots(cursor, item);
            list.add(item);
        }
        cursor.close();
        return list;
    }

    public long addComboToCart(String userId, ComboResponse combo, String quantity,
                               String noOfTable, String cartDiscount, String cartOrderStatus) {
        if (combo == null) {
            return -1;
        }
        List<ComboItemResponse> components = getComboItemList(combo.getComboId());
        java.util.ArrayList<String> snapshotLines = new java.util.ArrayList<>();
        for (ComboItemResponse component : components) {
            snapshotLines.add(component.getSnapshotLine());
        }
        String snapshot = ComboValidator.buildComponentSnapshot(snapshotLines);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userId", userId);
        values.put("productId", "");
        values.put("productName", combo.getComboName());
        values.put("productOldPrice", combo.getComboPrice());
        values.put("productNewPrice", combo.getComboPrice());
        values.put("productUnit", "Combo");
        values.put("productCGST", combo.getComboCGST() != null ? combo.getComboCGST() : "");
        values.put("productSGST", combo.getComboSGST() != null ? combo.getComboSGST() : "");
        values.put("productQuantity", quantity);
        values.put("noOfTable", noOfTable);
        values.put("cartDiscount", cartDiscount);
        values.put("cartOrderStatus", cartOrderStatus);
        values.put("cartStatus", 0);
        values.put("snapshotProductName", combo.getComboName());
        values.put("snapshotLinePrice", combo.getComboPrice());
        values.put("cartItemType", CartItemType.COMBO);
        values.put("comboId", combo.getComboId());
        values.put("snapshotComboComponents", snapshot);
        long cartId = db.insert(CART_PRODUCT_TABLE, null, values);
        if (cartId > 0) {
            int sort = 0;
            for (ComboItemResponse component : components) {
                ContentValues child = new ContentValues();
                child.put("cartId", cartId);
                child.put("comboId", combo.getComboId());
                child.put("productId", component.getProductId());
                child.put("productNameSnapshot", component.getProductName());
                putOptionalColumn(child, "portionId", component.getPortionId());
                putOptionalColumn(child, "portionNameSnapshot", component.getPortionName());
                child.put("quantity", component.getComboItemQuantity() != null ? component.getComboItemQuantity() : "1");
                child.put("sortOrder", sort++);
                db.insert(CART_COMBO_ITEM_TABLE, null, child);
            }
        }
        db.close();
        return cartId;
    }

    public List<ComboItemResponse> getCartComboItems(String cartId) {
        List<ComboItemResponse> list = new ArrayList<>();
        if (cartId == null) {
            return list;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + CART_COMBO_ITEM_TABLE + " WHERE cartId = ? ORDER BY sortOrder ASC, cartComboItemId ASC",
                new String[]{cartId});
        while (cursor.moveToNext()) {
            ComboItemResponse item = new ComboItemResponse();
            mapStringColumn(cursor, "cartId", item::setCartId);
            mapStringColumn(cursor, "comboId", item::setComboId);
            mapStringColumn(cursor, "productId", item::setProductId);
            mapStringColumn(cursor, "productNameSnapshot", item::setProductName);
            mapStringColumn(cursor, "portionId", item::setPortionId);
            mapStringColumn(cursor, "portionNameSnapshot", item::setPortionName);
            mapStringColumn(cursor, "quantity", item::setComboItemQuantity);
            mapStringColumn(cursor, "sortOrder", item::setComboItemSortOrder);
            list.add(item);
        }
        cursor.close();
        return list;
    }

    private void copyCartComboItemsToInvoice(SQLiteDatabase db, String cartId, String invoiceNumber,
                                             String invoiceProductNetworkStatus) {
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + CART_COMBO_ITEM_TABLE + " WHERE cartId = ? ORDER BY sortOrder ASC",
                new String[]{cartId != null ? cartId : ""});
        while (cursor.moveToNext()) {
            ContentValues values = new ContentValues();
            values.put("invoiceNumber", invoiceNumber);
            values.put("invoiceProductNetworkStatus", invoiceProductNetworkStatus);
            int comboIdx = cursor.getColumnIndex("comboId");
            String comboId = (comboIdx >= 0 && !cursor.isNull(comboIdx)) ? cursor.getString(comboIdx) : null;
            putOptionalColumn(values, "comboId", comboId);
            if (comboId != null) {
                Cursor comboCursor = db.rawQuery(
                        "SELECT comboNetworkStatus FROM " + COMBO_TABLE + " WHERE comboId = ? LIMIT 1",
                        new String[]{comboId});
                if (comboCursor.moveToFirst()) {
                    putOptionalColumn(values, "comboNetworkStatus", comboCursor.getString(0));
                }
                comboCursor.close();
            }
            mapStringColumn(cursor, "productId", v -> values.put("productId", v));
            mapStringColumn(cursor, "productNameSnapshot", v -> values.put("productNameSnapshot", v));
            mapStringColumn(cursor, "portionId", v -> values.put("portionId", v));
            mapStringColumn(cursor, "portionNameSnapshot", v -> values.put("portionNameSnapshot", v));
            mapStringColumn(cursor, "quantity", v -> values.put("quantity", v));
            mapStringColumn(cursor, "sortOrder", v -> values.put("sortOrder", v));
            values.put("invoiceComboItemNetworkStatus", getRandomString(10));
            values.put("invoiceComboItemStatus", 0);
            db.insert(INVOICE_COMBO_ITEM_TABLE, null, values);
        }
        cursor.close();
    }

    public Cursor getUnSynchronizeCombo(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + COMBO_TABLE + " WHERE comboStatus = '" + status + "'", null);
    }

    public void updateSyncCombo(String comboId, int comboStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("comboStatus", comboStatus);
        db.update(COMBO_TABLE, values, "comboId=?", new String[]{comboId});
        db.close();
    }

    public Cursor getUnSynchronizeComboItem(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT ci.*, c.comboNetworkStatus, p.productNetworkStatus, pp.portionNetworkStatus FROM "
                        + COMBO_ITEM_TABLE + " ci "
                        + "LEFT JOIN " + COMBO_TABLE + " c ON c.comboId = ci.comboId "
                        + "LEFT JOIN " + PRODUCT_TABLE + " p ON p.productId = ci.productId "
                        + "LEFT JOIN " + PRODUCT_PORTION_TABLE + " pp ON pp.portionId = ci.portionId "
                        + "WHERE ci.comboItemStatus = '" + status + "'",
                null);
    }

    public void updateSyncComboItem(String comboItemId, int comboItemStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("comboItemStatus", comboItemStatus);
        db.update(COMBO_ITEM_TABLE, values, "comboItemId=?", new String[]{comboItemId});
        db.close();
    }

    public Cursor getUnSynchronizeInvoiceComboItem(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT ici.*, p.productNetworkStatus, pp.portionNetworkStatus FROM "
                        + INVOICE_COMBO_ITEM_TABLE + " ici "
                        + "LEFT JOIN " + PRODUCT_TABLE + " p ON CAST(p.productId AS TEXT) = CAST(ici.productId AS TEXT) "
                        + "LEFT JOIN " + PRODUCT_PORTION_TABLE + " pp ON CAST(pp.portionId AS TEXT) = CAST(ici.portionId AS TEXT) "
                        + "WHERE ici.invoiceComboItemStatus = '" + status + "'",
                null);
    }

    public void updateSyncInvoiceComboItem(String invoiceComboItemId, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("invoiceComboItemStatus", status);
        db.update(INVOICE_COMBO_ITEM_TABLE, values, "invoiceComboItemId=?", new String[]{invoiceComboItemId});
        db.close();
    }

    public String resolveComboNetworkStatus(String comboId) {
        ComboResponse combo = getComboDetail(comboId);
        if (combo == null || combo.getComboNetworkStatus() == null) {
            return "";
        }
        return combo.getComboNetworkStatus();
    }

    public boolean addInvoiceComboItem(ComboItemResponse item) {
        if (item == null || item.getInvoiceComboItemNetworkStatus() == null
                || item.getInvoiceComboItemNetworkStatus().trim().isEmpty()) {
            return false;
        }
        String comboId = item.getComboId();
        if ((comboId == null || comboId.trim().isEmpty()) && item.getComboNetworkStatus() != null) {
            comboId = getComboIdByNetworkStatus(item.getComboNetworkStatus());
        }
        String productId = item.getProductId();
        if ((productId == null || productId.trim().isEmpty()) && item.getProductNetworkStatus() != null) {
            productId = getProductIdByNetworkStatus(item.getProductNetworkStatus());
        }
        String portionId = item.getPortionId();
        if ((portionId == null || portionId.trim().isEmpty()) && item.getPortionNetworkStatus() != null
                && !item.getPortionNetworkStatus().trim().isEmpty()) {
            portionId = getPortionIdByNetworkStatus(item.getPortionNetworkStatus());
        }
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor existing = db.rawQuery(
                "SELECT invoiceComboItemId FROM " + INVOICE_COMBO_ITEM_TABLE
                        + " WHERE invoiceComboItemNetworkStatus = ? LIMIT 1",
                new String[]{item.getInvoiceComboItemNetworkStatus()});
        if (existing.moveToFirst()) {
            existing.close();
            return false;
        }
        existing.close();
        ContentValues values = new ContentValues();
        values.put("invoiceNumber", item.getInvoiceNumber());
        values.put("invoiceProductNetworkStatus", item.getInvoiceProductNetworkStatus());
        putOptionalColumn(values, "comboId", comboId);
        putOptionalColumn(values, "comboNetworkStatus", item.getComboNetworkStatus());
        putOptionalColumn(values, "productId", productId);
        putOptionalColumn(values, "productNameSnapshot", item.getProductName());
        putOptionalColumn(values, "portionId", portionId);
        putOptionalColumn(values, "portionNameSnapshot", item.getPortionName());
        values.put("quantity", item.getComboItemQuantity() != null ? item.getComboItemQuantity() : "1");
        values.put("invoiceComboItemNetworkStatus", item.getInvoiceComboItemNetworkStatus());
        values.put("invoiceComboItemStatus", item.getInvoiceComboItemStatus() != null ? item.getInvoiceComboItemStatus() : "1");
        long id = db.insertWithOnConflict(INVOICE_COMBO_ITEM_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return id != -1;
    }

    public String buildComboComponentSnapshot(String comboId) {
        List<ComboItemResponse> items = getComboItemList(comboId);
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        for (ComboItemResponse item : items) {
            lines.add(item.getSnapshotLine());
        }
        return ComboValidator.buildComponentSnapshot(lines);
    }

    /**
     * Fast cloud-fetch insert for large product lists (one transaction, no per-row open/close).
     */
    public void addProductsBatchFromCloud(String ownerId, List<ProductResponse> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ProductResponse product : products) {
                if (product == null) {
                    continue;
                }
                ContentValues contentValues = new ContentValues();
                float productCGSTAmount = 0f, productSGSTAmount = 0f;
                try {
                    if (product.getProductCGST() != null && !product.getProductCGST().isEmpty()) {
                        productCGSTAmount = Float.parseFloat(product.getProductCGST());
                    }
                    if (product.getProductSGST() != null && !product.getProductSGST().isEmpty()) {
                        productSGSTAmount = Float.parseFloat(product.getProductSGST());
                    }
                } catch (Exception ignored) {
                }
                float price = 0f;
                try {
                    price = Float.parseFloat(product.getProductPrice() != null ? product.getProductPrice() : "0");
                } catch (Exception ignored) {
                }
                float productWithGSTPrice = price + (price * ((productCGSTAmount + productSGSTAmount) / 100));

                contentValues.put("categoryId", product.getCategoryId());
                contentValues.put("categoryName", product.getCategoryName());
                contentValues.put("productCode", product.getProductCode());
                contentValues.put("productName", product.getProductName());
                contentValues.put("productPrice", product.getProductPrice());
                contentValues.put("openPrice", product.getOpenPrice());
                contentValues.put("productUnit", product.getProductUnit());
                contentValues.put("productCGST", product.getProductCGST());
                contentValues.put("productSGST", product.getProductSGST());
                contentValues.put("productWithGSTPrice", String.valueOf(productWithGSTPrice));
                contentValues.put("productStatus", 1);
                contentValues.put("productDeletedStatus", product.getProductDeletedStatus());
                contentValues.put("productNetworkStatus", product.getProductNetworkStatus());
                putOptionalColumn(contentValues, "subcategoryId", product.getSubcategoryId());
                db.insert(PRODUCT_TABLE, null, contentValues);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Fast cloud-fetch insert for large invoice lists.
     * Uses CONFLICT_IGNORE instead of per-row existence SELECTs.
     */
    public void addInvoicesBatchFromCloud(List<InvoiceResponse> invoices) {
        if (invoices == null || invoices.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (InvoiceResponse invoiceResponse : invoices) {
                if (invoiceResponse == null) {
                    continue;
                }
                String networkStatus = invoiceResponse.getInvoiceNetworkStatus();
                ContentValues contentValues = new ContentValues();
                contentValues.put("noOfTable", invoiceResponse.getNoOfTable());
                contentValues.put("invoiceNumber", invoiceResponse.getInvoiceNumber());
                contentValues.put("customerName", invoiceResponse.getCustomerName());
                contentValues.put("customerMobile", invoiceResponse.getCustomerMobile());
                contentValues.put("customerAddress", invoiceResponse.getCustomerAddress());
                contentValues.put("subTotal", invoiceResponse.getSubTotal());
                contentValues.put("totalGSTAmount", invoiceResponse.getTotalGSTAmount());
                contentValues.put("discount", invoiceResponse.getDiscount());
                contentValues.put("discountType", invoiceResponse.getDiscountType());
                contentValues.put("totalAmount", invoiceResponse.getTotalAmount());
                contentValues.put("paymentMode", invoiceResponse.getPaymentMode());
                contentValues.put("invoiceDate", invoiceResponse.getInvoiceDate());
                contentValues.put("invoiceOrderStatus", "completed");
                contentValues.put("invoiceNetworkStatus", networkStatus);
                contentValues.put("invoiceType", invoiceResponse.getInvoiceType());
                contentValues.put("invoiceStatus", invoiceResponse.getInvoiceStatus());
                putOptionalColumn(contentValues, "organizationId", invoiceResponse.getOrganizationId());
                putOptionalColumn(contentValues, "branchId", invoiceResponse.getBranchId());
                putOptionalColumn(contentValues, "deviceId", invoiceResponse.getDeviceId());
                if (!contentValues.containsKey("organizationId")) {
                    BranchSession.applyScope(contentValues);
                }
                db.insertWithOnConflict(INVOICE_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Fast cloud-fetch insert for large invoice-line lists.
     */
    public void addInvoiceProductsBatchFromCloud(List<InvoiceProductResponse> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (InvoiceProductResponse invoiceProductResponse : lines) {
                if (invoiceProductResponse == null) {
                    continue;
                }
                String networkStatus = invoiceProductResponse.getInvoiceProductNetworkStatus();
                ContentValues contentValues = new ContentValues();
                contentValues.put("invoiceNumber", invoiceProductResponse.getInvoiceNumber());
                contentValues.put("productName", invoiceProductResponse.getProductName());
                contentValues.put("productPrice", invoiceProductResponse.getProductPrice());
                contentValues.put("productUnit", invoiceProductResponse.getProductUnit());
                contentValues.put("productCGST",
                        invoiceProductResponse.getProductCGST() != null ? invoiceProductResponse.getProductCGST() : "");
                contentValues.put("productSGST",
                        invoiceProductResponse.getProductSGST() != null ? invoiceProductResponse.getProductSGST() : "");
                contentValues.put("productQuantity", invoiceProductResponse.getProductQuantity());
                contentValues.put("productStatus", invoiceProductResponse.getProductStatus());
                contentValues.put("invoiceProductNetworkStatus", networkStatus);
                contentValues.put("invoiceProductStatus", invoiceProductResponse.getInvoiceProductStatus());
                putOptionalColumn(contentValues, "portionId", invoiceProductResponse.getPortionId());
                putOptionalColumn(contentValues, "portionName", invoiceProductResponse.getPortionName());
                contentValues.put("invoiceItemType", CartItemType.normalize(invoiceProductResponse.getInvoiceItemType()));
                putOptionalColumn(contentValues, "comboId", invoiceProductResponse.getComboId());
                putOptionalColumn(contentValues, "snapshotComboComponents", invoiceProductResponse.getSnapshotComboComponents());
                if (invoiceProductResponse.getSnapshotProductName() != null
                        && !invoiceProductResponse.getSnapshotProductName().trim().isEmpty()) {
                    contentValues.put("snapshotProductName", invoiceProductResponse.getSnapshotProductName());
                }
                if (invoiceProductResponse.getSnapshotLinePrice() != null
                        && !invoiceProductResponse.getSnapshotLinePrice().trim().isEmpty()) {
                    contentValues.put("snapshotLinePrice", invoiceProductResponse.getSnapshotLinePrice());
                }
                db.insertWithOnConflict(INVOICE_PRODUCT_TABLE, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Batch upsert combo components during cloud fetch (one transaction).
     */
    public void upsertComboItemsBatchFromCloud(List<ComboItemResponse> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ComboItemResponse item : items) {
                upsertComboItemFromServer(item);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Batch insert invoice combo lines during cloud fetch (one transaction).
     */
    public void addInvoiceComboItemsBatchFromCloud(List<ComboItemResponse> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ComboItemResponse item : items) {
                if (item == null) {
                    continue;
                }
                item.setInvoiceComboItemStatus("1");
                addInvoiceComboItem(item);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

}
