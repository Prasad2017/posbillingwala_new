package com.pos_billingwala.NetworkToOffline;

import com.pos_billingwala.R;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Extra.ErrorLogUploader;
import com.pos_billingwala.Extra.Observability;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.NetworkToOffline.WorkerClass.UserSynchronizeWorker;
import com.pos_billingwala.Retrofit.Api;

import retrofit2.Call;
import retrofit2.Response;

@SuppressLint("StaticFieldLeak, Range")
public class UserSynchronizeData {

    //1 means data is synced and 0 means data is not synced
    public static final int NAME_SYNCED_WITH_SERVER = 1;
    public static final int NAME_NOT_SYNCED_WITH_SERVER = 0;

    public interface ProgressCallback {
        void onProgress(String title);
    }

    Context context;
    POSBillingWalaDatabase posBillingWalaDatabase;
    Cursor cursor;
    private ProgressCallback progressCallback;

    /** Starts cloud upload in a foreground WorkManager job (survives background / screen off). */
    public UserSynchronizeData(Context context) {
        start(context);
    }

    public static void start(Context context) {
        start(context, true);
    }

    public static void start(Context context, boolean showToast) {
        if (context == null) {
            return;
        }
        Context app = context.getApplicationContext();
        if (!DetectConnection.checkInternetConnection(app)) {
            DetectConnection.noInternetConnection(context);
            return;
        }
        requestNotificationPermission(context);
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(UserSynchronizeWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();
        WorkManager.getInstance(app).enqueueUniqueWork(
                UserSynchronizeWorker.UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request);
        Log.i("UserSynchronizeData", "enqueued unique work " + UserSynchronizeWorker.UNIQUE_NAME);
        if (showToast) {
            Toast.makeText(context, context.getString(R.string.sync_started_in_background), Toast.LENGTH_LONG).show();
        }
    }

    public static UserSynchronizeData forBackground(Context context, ProgressCallback callback) {
        UserSynchronizeData sync = new UserSynchronizeData(context, true);
        sync.progressCallback = callback;
        return sync;
    }

    @SuppressWarnings("unused")
    private UserSynchronizeData(Context context, boolean background) {
        this.context = context.getApplicationContext();
        posBillingWalaDatabase = new POSBillingWalaDatabase(this.context);
        ensureSession(this.context);
    }

    static void ensureSession(Context context) {
        if (MainActivity.userId == null || MainActivity.userId.trim().isEmpty()) {
            MainActivity.userId = Common.getSavedUserData(context, "userId");
        }
        if (MainActivity.ownerId == null || MainActivity.ownerId.trim().isEmpty()) {
            MainActivity.ownerId = Common.getSavedUserData(context, "ownerId");
        }
    }

    private static void requestNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (!(context instanceof Activity)) {
            return;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions((Activity) context,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 4101);
    }

    private void setTableProgress(String tableKey, String title) {
        CloudSyncTracker.setCurrentTable(tableKey, title);
        CloudSyncTracker.refresh(context);
        if (progressCallback != null) {
            progressCallback.onProgress(title);
        }
    }

    public void runUpload() {
        uploadPendingData();
    }

    private void uploadPendingData() {
        setTableProgress(CloudSyncTracker.KEY_CATEGORIES, context.getString(R.string.sync_progress_categories));
        cursor = posBillingWalaDatabase.getUnSynchronizeCategory(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveCategory(cursor.getString(cursor.getColumnIndex("categoryId")),
                        cursor.getString(cursor.getColumnIndex("categoryName")),
                        cursor.getString(cursor.getColumnIndex("categoryDeletedStatus")),
                        cursor.getString(cursor.getColumnIndex("categoryNetworkStatus")),
                        resolveFoodTypeCode(cursor),
                        columnOrEmpty(cursor, "categorySortOrder"));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_SUBCATEGORIES, context.getString(R.string.sync_progress_subcategories));
        cursor = posBillingWalaDatabase.getUnSynchronizeSubcategory(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveSubcategory(cursor.getString(cursor.getColumnIndex("subcategoryId")),
                        cursor.getString(cursor.getColumnIndex("categoryId")),
                        columnOrEmpty(cursor, "categoryNetworkStatus"),
                        cursor.getString(cursor.getColumnIndex("subcategoryName")),
                        cursor.getString(cursor.getColumnIndex("subcategoryDeletedStatus")),
                        cursor.getString(cursor.getColumnIndex("subcategoryNetworkStatus")),
                        columnOrEmpty(cursor, "subcategorySortOrder"));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_PRODUCTS, context.getString(R.string.sync_progress_products));
        cursor = posBillingWalaDatabase.getUnSynchronizeProduct(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveProduct(cursor.getString(cursor.getColumnIndex("productId")),
                        cursor.getString(cursor.getColumnIndex("categoryId")),
                        cursor.getString(cursor.getColumnIndex("categoryName")),
                        cursor.getString(cursor.getColumnIndex("productCode")),
                        cursor.getString(cursor.getColumnIndex("productName")),
                        cursor.getString(cursor.getColumnIndex("productPrice")),
                        cursor.getString(cursor.getColumnIndex("productUnit")),
                        cursor.getString(cursor.getColumnIndex("productCGST")),
                        cursor.getString(cursor.getColumnIndex("productSGST")),
                        cursor.getString(cursor.getColumnIndex("productNetworkStatus")),
                        cursor.getString(cursor.getColumnIndex("productDeletedStatus")),
                        columnOrEmpty(cursor, "subcategoryId"),
                        columnOrEmpty(cursor, "openPrice"));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_PORTION_MASTER, context.getString(R.string.sync_progress_portion_master));
        cursor = posBillingWalaDatabase.getUnSynchronizePortionMaster(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                savePortionMaster(cursor.getString(cursor.getColumnIndex("portionMasterId")),
                        cursor.getString(cursor.getColumnIndex("portionName")),
                        cursor.getString(cursor.getColumnIndex("portionMasterDeletedStatus")),
                        cursor.getString(cursor.getColumnIndex("portionMasterNetworkStatus")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_PORTIONS, context.getString(R.string.sync_progress_portions));
        cursor = posBillingWalaDatabase.getUnSynchronizePortion(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                savePortion(cursor.getString(cursor.getColumnIndex("portionId")),
                        cursor.getString(cursor.getColumnIndex("productId")),
                        columnOrEmpty(cursor, "productNetworkStatus"),
                        cursor.getString(cursor.getColumnIndex("portionName")),
                        cursor.getString(cursor.getColumnIndex("portionPrice")),
                        columnOrEmpty(cursor, "portionSortOrder"),
                        cursor.getString(cursor.getColumnIndex("portionDeletedStatus")),
                        cursor.getString(cursor.getColumnIndex("portionNetworkStatus")),
                        columnOrEmpty(cursor, "portionMasterId"),
                        columnOrEmpty(cursor, "portionMasterNetworkStatus"));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_COMBOS, context.getString(R.string.sync_progress_combos));
        cursor = posBillingWalaDatabase.getUnSynchronizeCombo(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveCombo(cursor.getString(cursor.getColumnIndex("comboId")),
                        cursor.getString(cursor.getColumnIndex("comboName")),
                        columnOrEmpty(cursor, "comboCode"),
                        cursor.getString(cursor.getColumnIndex("comboPrice")),
                        columnOrEmpty(cursor, "comboCGST"),
                        columnOrEmpty(cursor, "comboSGST"),
                        columnOrEmpty(cursor, "comboWithGSTPrice"),
                        columnOrEmpty(cursor, "comboActiveStatus"),
                        columnOrEmpty(cursor, "comboDeletedStatus"),
                        columnOrEmpty(cursor, "comboNetworkStatus"),
                        columnOrEmpty(cursor, "comboSortOrder"));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_COMBO_ITEMS, context.getString(R.string.sync_progress_combo_items));
        cursor = posBillingWalaDatabase.getUnSynchronizeComboItem(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveComboItem(cursor.getString(cursor.getColumnIndex("comboItemId")),
                        columnOrEmpty(cursor, "comboId"),
                        columnOrEmpty(cursor, "comboNetworkStatus"),
                        columnOrEmpty(cursor, "productId"),
                        columnOrEmpty(cursor, "productNetworkStatus"),
                        columnOrEmpty(cursor, "portionId"),
                        columnOrEmpty(cursor, "portionNetworkStatus"),
                        columnOrEmpty(cursor, "comboItemQuantity"),
                        columnOrEmpty(cursor, "comboItemSortOrder"),
                        columnOrEmpty(cursor, "comboItemDeletedStatus"),
                        columnOrEmpty(cursor, "comboItemNetworkStatus"));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_PRINTER, context.getString(R.string.sync_progress_printer));
        cursor = posBillingWalaDatabase.getUnSynchronizePrinterSetting(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                savePrinterSetting(cursor.getString(cursor.getColumnIndex("settingId")),
                        cursor.getString(cursor.getColumnIndex("printerName")),
                        cursor.getString(cursor.getColumnIndex("KOTPrinterName")),
                        cursor.getString(cursor.getColumnIndex("invoicePrefix")),
                        cursor.getString(cursor.getColumnIndex("invoiceTitle")),
                        cursor.getString(cursor.getColumnIndex("invoiceTermsCondition")),
                        cursor.getString(cursor.getColumnIndex("logoUse")),
                        cursor.getString(cursor.getColumnIndex("paymentUse")),
                        cursor.getString(cursor.getColumnIndex("customerUse")),
                        cursor.getString(cursor.getColumnIndex("productQuantityUpdate")),
                        columnOrEmpty(cursor, "duplicateBillUse").isEmpty() ? "off" : columnOrEmpty(cursor, "duplicateBillUse"),
                        cursor.getString(cursor.getColumnIndex("bluetoothAddress")),
                        cursor.getString(cursor.getColumnIndex("bluetoothKOTAddress")),
                        cursor.getString(cursor.getColumnIndex("printerFeedLines")),
                        cursor.getString(cursor.getColumnIndex("KotPrinterFeedLines")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_COMPANY, context.getString(R.string.sync_progress_company));
        cursor = posBillingWalaDatabase.getUnSynchronizeCompanyDetails(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveCompanyDetails(cursor.getString(cursor.getColumnIndex("companyId")),
                        cursor.getString(cursor.getColumnIndex("companyLogo")),
                        cursor.getString(cursor.getColumnIndex("companyName")),
                        cursor.getString(cursor.getColumnIndex("cashierName")),
                        cursor.getString(cursor.getColumnIndex("companyMobile")),
                        cursor.getString(cursor.getColumnIndex("companyAddress")),
                        columnOrEmpty(cursor, "shopName1"),
                        columnOrEmpty(cursor, "shopName2"),
                        columnOrEmpty(cursor, "addressLine1"),
                        columnOrEmpty(cursor, "addressLine2"),
                        columnOrEmpty(cursor, "addressLine3"),
                        columnOrEmpty(cursor, "phoneNo1"),
                        columnOrEmpty(cursor, "phoneNo2"),
                        cursor.getString(cursor.getColumnIndex("currencyName")),
                        cursor.getString(cursor.getColumnIndex("tableStatus")),
                        cursor.getString(cursor.getColumnIndex("noOfTable")),
                        cursor.getString(cursor.getColumnIndex("countryName")),
                        cursor.getString(cursor.getColumnIndex("stateName")),
                        cursor.getString(cursor.getColumnIndex("gstStatus")),
                        cursor.getString(cursor.getColumnIndex("gstNumber")),
                        cursor.getString(cursor.getColumnIndex("panNumber")),
                        cursor.getString(cursor.getColumnIndex("paymentLogo")),
                        cursor.getString(cursor.getColumnIndex("companyFssis")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_INVOICE_DELETES, context.getString(R.string.sync_progress_invoice_deletes));
        InvoicePendingSync.uploadDeletes(context, posBillingWalaDatabase);
        setTableProgress(CloudSyncTracker.KEY_INVOICE_ITEMS, context.getString(R.string.sync_progress_invoice_items));
        cursor = posBillingWalaDatabase.getUnSynchronizeInvoiceProduct(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveInvoiceProduct(cursor.getString(cursor.getColumnIndex("invoiceProductId")),
                        cursor.getString(cursor.getColumnIndex("invoiceNumber")),
                        cursor.getString(cursor.getColumnIndex("productName")),
                        cursor.getString(cursor.getColumnIndex("productPrice")),
                        cursor.getString(cursor.getColumnIndex("productUnit")),
                        cursor.getString(cursor.getColumnIndex("productCGST")),
                        cursor.getString(cursor.getColumnIndex("productSGST")),
                        cursor.getString(cursor.getColumnIndex("productQuantity")),
                        cursor.getString(cursor.getColumnIndex("productStatus")),
                        cursor.getString(cursor.getColumnIndex("invoiceProductNetworkStatus")),
                        columnOrEmpty(cursor, "portionId"),
                        columnOrEmpty(cursor, "portionName"),
                        columnOrEmpty(cursor, "snapshotProductName"),
                        columnOrEmpty(cursor, "snapshotLinePrice"),
                        columnOrEmpty(cursor, "invoiceItemType"),
                        posBillingWalaDatabase.resolveComboNetworkStatus(columnOrEmpty(cursor, "comboId")),
                        columnOrEmpty(cursor, "snapshotComboComponents"));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_INVOICE_COMBO_ITEMS, context.getString(R.string.sync_progress_invoice_combo_items));
        cursor = posBillingWalaDatabase.getUnSynchronizeInvoiceComboItem(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveInvoiceComboItem(cursor.getString(cursor.getColumnIndex("invoiceComboItemId")),
                        columnOrEmpty(cursor, "invoiceNumber"),
                        columnOrEmpty(cursor, "invoiceProductNetworkStatus"),
                        columnOrEmpty(cursor, "comboNetworkStatus"),
                        columnOrEmpty(cursor, "productId"),
                        columnOrEmpty(cursor, "productNetworkStatus"),
                        columnOrEmpty(cursor, "productNameSnapshot"),
                        columnOrEmpty(cursor, "portionId"),
                        columnOrEmpty(cursor, "portionNetworkStatus"),
                        columnOrEmpty(cursor, "portionNameSnapshot"),
                        columnOrEmpty(cursor, "quantity"),
                        columnOrEmpty(cursor, "sortOrder"),
                        columnOrEmpty(cursor, "invoiceComboItemNetworkStatus"));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_INVOICES, context.getString(R.string.sync_progress_invoices));
        cursor = posBillingWalaDatabase.getUnSynchronizeInvoice(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveInvoice(cursor.getString(cursor.getColumnIndex("invoiceId")),
                        cursor.getString(cursor.getColumnIndex("noOfTable")),
                        cursor.getString(cursor.getColumnIndex("invoiceNumber")),
                        cursor.getString(cursor.getColumnIndex("customerName")),
                        cursor.getString(cursor.getColumnIndex("customerMobile")),
                        cursor.getString(cursor.getColumnIndex("customerEmail")),
                        cursor.getString(cursor.getColumnIndex("customerAddress")),
                        cursor.getString(cursor.getColumnIndex("subTotal")),
                        cursor.getString(cursor.getColumnIndex("totalGSTAmount")),
                        cursor.getString(cursor.getColumnIndex("discount")),
                        cursor.getString(cursor.getColumnIndex("discountType")),
                        columnOrEmpty(cursor, "packingCharge"),
                        columnOrEmpty(cursor, "packingChargeType"),
                        cursor.getString(cursor.getColumnIndex("totalAmount")),
                        cursor.getString(cursor.getColumnIndex("paymentMode")),
                        columnOrEmpty(cursor, "cashAmount"),
                        columnOrEmpty(cursor, "upiAmount"),
                        cursor.getString(cursor.getColumnIndex("invoiceDate")),
                        cursor.getString(cursor.getColumnIndex("invoiceType")),
                        cursor.getString(cursor.getColumnIndex("invoiceOrderStatus")),
                        cursor.getString(cursor.getColumnIndex("invoiceNetworkStatus")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_MESS_MEMBERS, context.getString(R.string.sync_progress_mess_members));
        cursor = posBillingWalaDatabase.getUnSynchronizeMessMember(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveMessMember(cursor.getString(cursor.getColumnIndex("memberId")),
                        cursor.getString(cursor.getColumnIndex("memberName")),
                        cursor.getString(cursor.getColumnIndex("memberMobileNumber")),
                        cursor.getString(cursor.getColumnIndex("memberAlternetMobileNumber")),
                        cursor.getString(cursor.getColumnIndex("memberAddress")),
                        cursor.getString(cursor.getColumnIndex("memberNetworkStatus")),
                        cursor.getString(cursor.getColumnIndex("memberStatus")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_MESS_PAYMENTS, context.getString(R.string.sync_progress_mess_payments));
        cursor = posBillingWalaDatabase.getUnSynchronizeMessMemberPayment(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveMessMemberPayment(cursor.getString(cursor.getColumnIndex("paymentId")),
                        cursor.getString(cursor.getColumnIndex("memberId")),
                        cursor.getString(cursor.getColumnIndex("memberName")),
                        cursor.getString(cursor.getColumnIndex("paymentMessAmount")),
                        cursor.getString(cursor.getColumnIndex("paymentPaidAmount")),
                        cursor.getString(cursor.getColumnIndex("messTotalDays")),
                        cursor.getString(cursor.getColumnIndex("paymentDate")),
                        cursor.getString(cursor.getColumnIndex("paymentNetworkStatus")),
                        cursor.getString(cursor.getColumnIndex("paymentStatus")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_MESS_INVOICES, context.getString(R.string.sync_progress_mess_invoices));
        cursor = posBillingWalaDatabase.getUnSynchronizeMessInvoice(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveMessInvoice(cursor.getString(cursor.getColumnIndex("invoiceId")),
                        cursor.getString(cursor.getColumnIndex("memberId")),
                        cursor.getString(cursor.getColumnIndex("memberName")),
                        cursor.getString(cursor.getColumnIndex("messType")),
                        cursor.getString(cursor.getColumnIndex("messInvoiceDate")),
                        cursor.getString(cursor.getColumnIndex("messInvoiceNetworkStatus")),
                        cursor.getString(cursor.getColumnIndex("messInvoiceStatus")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_MESS_TOKENS, context.getString(R.string.sync_progress_mess_tokens));
        cursor = posBillingWalaDatabase.getUnSynchronizeMessToken(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveMessToken(cursor.getString(cursor.getColumnIndex("tokenId")),
                        cursor.getString(cursor.getColumnIndex("tokenCode")),
                        cursor.getString(cursor.getColumnIndex("memberId")),
                        cursor.getString(cursor.getColumnIndex("memberName")),
                        cursor.getString(cursor.getColumnIndex("memberMobile")),
                        cursor.getString(cursor.getColumnIndex("memberType")),
                        cursor.getString(cursor.getColumnIndex("messType")),
                        cursor.getString(cursor.getColumnIndex("tokenAmount")),
                        cursor.getString(cursor.getColumnIndex("tokenDate")),
                        cursor.getString(cursor.getColumnIndex("tokenNetworkStatus")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        cursor = posBillingWalaDatabase.getUnSynchronizeMessTokenVerify(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                verifyMessToken(cursor.getString(cursor.getColumnIndex("tokenId")),
                        cursor.getString(cursor.getColumnIndex("tokenCode")),
                        cursor.getString(cursor.getColumnIndex("verifiedDate")),
                        cursor.getString(cursor.getColumnIndex("verifyNetworkStatus")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_INVENTORY, context.getString(R.string.sync_progress_inventory));
        cursor = posBillingWalaDatabase.getUnSynchronizeInventory(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveInventory(cursor.getString(cursor.getColumnIndex("inventoryId")),
                        cursor.getString(cursor.getColumnIndex("productId")),
                        cursor.getString(cursor.getColumnIndex("productInventoryQuantity")),
                        cursor.getString(cursor.getColumnIndex("afterSaleInventoryQuantity")),
                        cursor.getString(cursor.getColumnIndex("saleInventoryQuantity")),
                        cursor.getString(cursor.getColumnIndex("inventoryDate")),
                        cursor.getString(cursor.getColumnIndex("inventoryNetworkStatus")),
                        cursor.getString(cursor.getColumnIndex("inventoryStatus")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_EXPENSES, context.getString(R.string.sync_progress_expenses));
        cursor = posBillingWalaDatabase.getUnSynchronizeExpenses(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                saveExpenses(cursor.getString(cursor.getColumnIndex("expensesId")),
                        cursor.getString(cursor.getColumnIndex("expensesName")),
                        cursor.getString(cursor.getColumnIndex("expensesAmount")),
                        cursor.getString(cursor.getColumnIndex("expensesDate")),
                        cursor.getString(cursor.getColumnIndex("expensesNetworkStatus")),
                        cursor.getString(cursor.getColumnIndex("expensesStatus")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        setTableProgress(CloudSyncTracker.KEY_ERROR_LOGS, context.getString(R.string.sync_progress_error_logs));
        CloudSyncTracker.addUploaded(ErrorLogUploader.flushPending(context));
        CloudSyncTracker.refresh(context);
    }

    public void saveMessInvoice(String invoiceId, String memberId, String memberName, String messType, String messInvoiceDate, String messInvoiceNetworkStatus, String messInvoiceStatus) {
        if (executeCall(Api.getClient(context).saveMessInvoice(MainActivity.userId, memberName, messType, messInvoiceDate, messInvoiceNetworkStatus, messInvoiceStatus))) {
            posBillingWalaDatabase.updateSyncMessInvoice(invoiceId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveMessToken(String tokenId, String tokenCode, String memberId, String memberName, String memberMobile,
                              String memberType, String messType, String tokenAmount, String tokenDate,
                              String tokenNetworkStatus) {
        if (executeCall(Api.getClient(context).saveMessToken(
                MainActivity.userId, tokenCode, memberId, memberName, memberMobile, memberType,
                messType, tokenAmount, tokenDate, tokenNetworkStatus))) {
            posBillingWalaDatabase.updateSyncMessToken(tokenId);
        }
    }

    public void verifyMessToken(String tokenId, String tokenCode, String verifiedDate, String verifyNetworkStatus) {
        if (executeCall(Api.getClient(context).verifyMessToken(
                MainActivity.userId, tokenCode, verifiedDate, verifyNetworkStatus))) {
            posBillingWalaDatabase.updateSyncMessTokenVerify(tokenId);
        }
    }

    public void saveMessMemberPayment(String paymentId, String memberId, String memberName, String paymentMessAmount, String paymentPaidAmount, String messTotalDays, String paymentDate, String paymentNetworkStatus, String paymentStatus) {
        if (executeCall(Api.getClient(context).saveMessMemberPayment(MainActivity.userId, memberId, memberName, paymentMessAmount, paymentPaidAmount, messTotalDays, paymentDate, paymentNetworkStatus, paymentStatus))) {
            posBillingWalaDatabase.updateSyncMessMemberPayment(paymentId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveMessMember(String memberId, String memberName, String memberMobileNumber, String memberAlternetMobileNumber, String memberAddress, String memberNetworkStatus, String memberStatus) {
        if (executeCall(Api.getClient(context).saveMessMember(MainActivity.userId, memberName, memberMobileNumber, memberAlternetMobileNumber, memberAddress, memberNetworkStatus, memberStatus))) {
            posBillingWalaDatabase.updateSyncMessMember(memberId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveExpenses(String expensesId, String expensesName, String expensesAmount, String expensesDate, String expensesNetworkStatus, String expensesStatus) {
        if (executeCall(Api.getClient(context).saveExpenses(MainActivity.userId, expensesName, expensesAmount, expensesDate, expensesNetworkStatus))) {
            posBillingWalaDatabase.updateSyncExpenses(expensesId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveInventory(String inventoryId, String productId, String productInventoryQuantity, String afterSaleInventoryQuantity, String saleInventoryQuantity, String inventoryDate, String inventoryNetworkStatus, String inventoryStatus) {
        if (executeCall(Api.getClient(context).saveInventory(MainActivity.userId, productId, productInventoryQuantity, afterSaleInventoryQuantity, saleInventoryQuantity, inventoryDate, inventoryNetworkStatus))) {
            posBillingWalaDatabase.updateSyncInventory(inventoryId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveInvoiceProduct(String invoiceProductId, String invoiceNumber, String productName, String productPrice, String productUnit, String productCGST, String productSGST, String productQuantity, String productStatus, String invoiceProductNetworkStatus, String portionId, String portionName, String snapshotProductName, String snapshotLinePrice, String invoiceItemType, String comboNetworkStatus, String snapshotComboComponents) {
        if (executeCall(Api.getClient(context).saveInvoiceProduct(invoiceNumber, productName, productPrice, productUnit, productCGST, productSGST, productQuantity, productStatus, invoiceProductNetworkStatus, portionId, portionName, snapshotProductName, snapshotLinePrice, invoiceItemType, comboNetworkStatus, snapshotComboComponents))) {
            posBillingWalaDatabase.updateSyncInvoiceProduct(invoiceProductId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveInvoice(String invoiceId, String noOfTable, String invoiceNumber, String customerName, String customerMobile, String customerEmail, String customerAddress, String subTotal, String totalGSTAmount,
                            String discount, String discountType, String packingCharge, String packingChargeType, String totalAmount, String paymentMode, String cashAmount, String upiAmount, String invoiceDate, String invoiceType, String invoiceOrderStatus, String invoiceNetworkStatus) {
        if (executeCall(Api.getClient(context).saveInvoice(MainActivity.userId,
                nz(noOfTable), nz(invoiceNumber), nz(customerName), nz(customerMobile), nz(customerEmail), nz(customerAddress),
                nz(subTotal), nz(totalGSTAmount), nz(discount), nz(discountType),
                nz(packingCharge).isEmpty() ? "0" : nz(packingCharge),
                nz(packingChargeType).isEmpty() ? "Percentage" : nz(packingChargeType),
                nz(totalAmount), nz(paymentMode), nz(cashAmount), nz(upiAmount),
                nz(invoiceDate), nz(invoiceType), nz(invoiceOrderStatus), nz(invoiceNetworkStatus)))) {
            posBillingWalaDatabase.updateSyncInvoice(invoiceId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveCompanyDetails(String companyId, String companyLogo, String companyName, String cashierName, String companyMobile, String companyAddress,
                                   String shopName1, String shopName2, String addressLine1, String addressLine2, String addressLine3, String phoneNo1, String phoneNo2,
                                   String currencyName, String tableStatus, String noOfTable,
                                   String countryName, String stateName, String gstStatus, String gstNumber, String panNumber, String paymentLogo, String companyFssis) {
        if (shopName1 == null || shopName1.trim().isEmpty()) {
            shopName1 = companyName;
        }
        if (phoneNo1 == null || phoneNo1.trim().isEmpty()) {
            phoneNo1 = companyMobile;
        }
        if ((addressLine1 == null || addressLine1.trim().isEmpty())
                && (addressLine2 == null || addressLine2.trim().isEmpty())
                && (addressLine3 == null || addressLine3.trim().isEmpty())) {
            addressLine1 = companyAddress;
        }
        if (executeCall(Api.getClient(context).saveCompanyDetails(MainActivity.userId, companyLogo, companyName, cashierName, companyMobile, companyAddress,
                shopName1, shopName2, addressLine1, addressLine2, addressLine3, phoneNo1, phoneNo2,
                currencyName, tableStatus, noOfTable, countryName, stateName,
                gstStatus, gstNumber, panNumber, paymentLogo, companyFssis))) {
            posBillingWalaDatabase.updateSynchronizeCompanyDetails(companyId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void savePrinterSetting(String settingId, String printerName, String KOTPrinterName, String invoicePrefix, String invoiceTitle, String invoiceTermsCondition, String logoUse, String paymentUse, String customerUse, String productQuantityUpdate, String duplicateBillUse, String bluetoothAddress, String bluetoothKOTAddress, String printerFeedLines, String KotPrinterFeedLines) {
        if (executeCall(Api.getClient(context).savePrinterSetting(MainActivity.userId,
                nz(printerName), nz(KOTPrinterName), nz(invoicePrefix), nz(invoiceTitle), nz(invoiceTermsCondition),
                nz(logoUse), nz(paymentUse), nz(customerUse), nz(productQuantityUpdate), nz(duplicateBillUse),
                nz(bluetoothAddress), nz(bluetoothKOTAddress), nz(printerFeedLines), nz(KotPrinterFeedLines)))) {
            posBillingWalaDatabase.updateSynchronizePrinterSetting(settingId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveProduct(String productId, String categoryId, String categoryName, String productCode, String productName, String productPrice, String productUnit, String productCGST, String productSGST, String productNetworkStatus, String productDeletedStatus, String subcategoryId, String openPrice) {
        String openPriceValue = (openPrice == null || openPrice.trim().isEmpty()) ? "off" : openPrice;
        if (executeCall(Api.getClient(context).saveProduct(MainActivity.ownerId, categoryId, categoryName, productCode, productName, productPrice, productUnit, productCGST, productSGST, productNetworkStatus, productDeletedStatus, subcategoryId, openPriceValue))) {
            posBillingWalaDatabase.updateSyncProduct(productId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveCategory(String categoryId, String categoryName, String categoryDeletedStatus, String categoryNetworkStatus, String foodTypeCode, String categorySortOrder) {
        if (executeCall(Api.getClient(context).saveCategory(MainActivity.ownerId, categoryName, categoryDeletedStatus, categoryNetworkStatus, foodTypeCode, categorySortOrder))) {
            posBillingWalaDatabase.updateSyncCategory(categoryId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveSubcategory(String subcategoryId, String categoryId, String categoryNetworkStatus, String subcategoryName, String subcategoryDeletedStatus, String subcategoryNetworkStatus, String subcategorySortOrder) {
        if (executeCall(Api.getClient(context).saveSubcategory(MainActivity.ownerId, categoryId, categoryNetworkStatus, subcategoryName, subcategoryDeletedStatus, subcategoryNetworkStatus, subcategorySortOrder))) {
            posBillingWalaDatabase.updateSyncSubcategory(subcategoryId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void savePortionMaster(String portionMasterId, String portionName, String portionMasterDeletedStatus,
                                  String portionMasterNetworkStatus) {
        if (executeCall(Api.getClient(context).savePortionMaster(
                MainActivity.ownerId, portionName, portionMasterDeletedStatus, portionMasterNetworkStatus))) {
            posBillingWalaDatabase.updateSyncPortionMaster(portionMasterId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void savePortion(String portionId, String productId, String productNetworkStatus, String portionName,
                            String portionPrice, String portionSortOrder, String portionDeletedStatus,
                            String portionNetworkStatus, String portionMasterId, String portionMasterNetworkStatus) {
        if (executeCall(Api.getClient(context).savePortion(
                MainActivity.ownerId, productId, productNetworkStatus, portionName, portionPrice, portionSortOrder,
                portionDeletedStatus, portionNetworkStatus, portionMasterId, portionMasterNetworkStatus))) {
            posBillingWalaDatabase.updateSyncPortion(portionId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveCombo(String comboId, String comboName, String comboCode, String comboPrice, String comboCGST,
                          String comboSGST, String comboWithGSTPrice, String comboActiveStatus,
                          String comboDeletedStatus, String comboNetworkStatus, String comboSortOrder) {
        if (executeCall(Api.getClient(context).saveCombo(
                MainActivity.ownerId, comboName, comboCode, comboPrice, comboCGST, comboSGST, comboWithGSTPrice,
                comboActiveStatus, comboDeletedStatus, comboNetworkStatus, comboSortOrder))) {
            posBillingWalaDatabase.updateSyncCombo(comboId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveComboItem(String comboItemId, String comboId, String comboNetworkStatus, String productId,
                              String productNetworkStatus, String portionId, String portionNetworkStatus,
                              String comboItemQuantity, String comboItemSortOrder, String comboItemDeletedStatus,
                              String comboItemNetworkStatus) {
        if (executeCall(Api.getClient(context).saveComboItem(
                MainActivity.ownerId, comboId, comboNetworkStatus, productId, productNetworkStatus, portionId,
                portionNetworkStatus, comboItemQuantity, comboItemSortOrder, comboItemDeletedStatus,
                comboItemNetworkStatus))) {
            posBillingWalaDatabase.updateSyncComboItem(comboItemId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveInvoiceComboItem(String invoiceComboItemId, String invoiceNumber,
                                     String invoiceProductNetworkStatus, String comboNetworkStatus, String productId,
                                     String productNetworkStatus, String productName, String portionId,
                                     String portionNetworkStatus, String portionName, String quantity, String sortOrder,
                                     String invoiceComboItemNetworkStatus) {
        if (executeCall(Api.getClient(context).saveInvoiceComboItem(
                invoiceNumber, invoiceProductNetworkStatus, comboNetworkStatus, productId, productNetworkStatus,
                productName, portionId, portionNetworkStatus, portionName, quantity, sortOrder,
                invoiceComboItemNetworkStatus))) {
            posBillingWalaDatabase.updateSyncInvoiceComboItem(invoiceComboItemId, NAME_SYNCED_WITH_SERVER);
        }
    }

    private boolean executeCall(Call<AllApiResponse> call) {
        try {
            Response<AllApiResponse> response = call.execute();
            boolean ok = response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus());
            if (ok) {
                CloudSyncTracker.addUploaded(1);
            }
            if (!ok) {
                String status = response.body() != null ? response.body().getStatus() : "null_body";
                String msg = response.body() != null ? response.body().getMessage() : "";
                Observability.log("user_synchronize sync failed | HTTP " + response.code()
                        + " | status=" + status + " | msg=" + msg);
            }
            return ok;
        } catch (Exception e) {
            Observability.logNonFatal(e, "user_synchronize_data");
            return false;
        }
    }

    private void closeCursor() {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    private String resolveFoodTypeCode(android.database.Cursor cursor) {
        String foodTypeId = columnOrEmpty(cursor, "foodTypeId");
        if (foodTypeId.isEmpty()) {
            return "";
        }
        try {
            return posBillingWalaDatabase.getFoodTypeCodeById(Long.parseLong(foodTypeId));
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private static String columnOrEmpty(android.database.Cursor cursor, String column) {
        int idx = cursor.getColumnIndex(column);
        if (idx < 0) {
            return "";
        }
        String value = cursor.getString(idx);
        return value != null ? value : "";
    }

    /** Retrofit omits null @Field values — always send "" so PHP 8 never sees missing keys. */
    private static String nz(String value) {
        return value != null ? value : "";
    }


}
