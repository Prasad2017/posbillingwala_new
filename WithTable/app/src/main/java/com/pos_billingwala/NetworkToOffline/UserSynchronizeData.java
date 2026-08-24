package com.pos_billingwala.NetworkToOffline;

import com.pos_billingwala.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Retrofit.Api;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Response;

@SuppressLint("StaticFieldLeak, Range")
public class UserSynchronizeData {

    //1 means data is synced and 0 means data is not synced
    public static final int NAME_SYNCED_WITH_SERVER = 1;
    public static final int NAME_NOT_SYNCED_WITH_SERVER = 0;

    public static SweetAlertDialog pDialog;
    Context context;
    POSBillingWalaDatabase posBillingWalaDatabase;
    Cursor cursor;


    public UserSynchronizeData(Context context) {
        this.context = context;
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        if (!DetectConnection.checkInternetConnection(context)) {
            dismissDialogSafely();
            DetectConnection.noInternetConnection(context);
            return;
        }

        OfflineSyncExecutor.execute(() -> {
            boolean success = false;
            try {
                uploadPendingData();
                success = true;
            } catch (Exception e) {
                Log.e("UserSynchronizeData", "upload failed", e);
            }
            final boolean uploaded = success;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (uploaded) {
                    Toast.makeText(context, context.getString(R.string.toast_offline_data_uploaded_to_server), Toast.LENGTH_SHORT).show();
                }
                dismissDialogSafely();
            });
        });
    }

    private void dismissDialogSafely() {
        try {
            if (pDialog != null && pDialog.isShowing()) {
                pDialog.dismiss();
            }
        } catch (Exception ignored) {
        }
    }

    private void uploadPendingData() {
        cursor = posBillingWalaDatabase.getUnSynchronizeCategory(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
            do {
                saveCategory(cursor.getString(cursor.getColumnIndex("categoryId")),
                        cursor.getString(cursor.getColumnIndex("categoryName")),
                        cursor.getString(cursor.getColumnIndex("categoryDeletedStatus")),
                        cursor.getString(cursor.getColumnIndex("categoryNetworkStatus")),
                        resolveFoodTypeCode(cursor));
            } while (cursor.moveToNext());
        }
        closeCursor();
        cursor = posBillingWalaDatabase.getUnSynchronizeSubcategory(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
            do {
                saveSubcategory(cursor.getString(cursor.getColumnIndex("subcategoryId")),
                        cursor.getString(cursor.getColumnIndex("categoryId")),
                        columnOrEmpty(cursor, "categoryNetworkStatus"),
                        cursor.getString(cursor.getColumnIndex("subcategoryName")),
                        cursor.getString(cursor.getColumnIndex("subcategoryDeletedStatus")),
                        cursor.getString(cursor.getColumnIndex("subcategoryNetworkStatus")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        cursor = posBillingWalaDatabase.getUnSynchronizeProduct(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
                        columnOrEmpty(cursor, "subcategoryId"));
            } while (cursor.moveToNext());
        }
        closeCursor();
        cursor = posBillingWalaDatabase.getUnSynchronizePortionMaster(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
            do {
                savePortionMaster(cursor.getString(cursor.getColumnIndex("portionMasterId")),
                        cursor.getString(cursor.getColumnIndex("portionName")),
                        cursor.getString(cursor.getColumnIndex("portionMasterDeletedStatus")),
                        cursor.getString(cursor.getColumnIndex("portionMasterNetworkStatus")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        cursor = posBillingWalaDatabase.getUnSynchronizePortion(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
        cursor = posBillingWalaDatabase.getUnSynchronizePrinterSetting(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
                        cursor.getString(cursor.getColumnIndex("bluetoothAddress")),
                        cursor.getString(cursor.getColumnIndex("bluetoothKOTAddress")),
                        cursor.getString(cursor.getColumnIndex("printerFeedLines")),
                        cursor.getString(cursor.getColumnIndex("KotPrinterFeedLines")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        cursor = posBillingWalaDatabase.getUnSynchronizeCompanyDetails(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
            do {
                saveCompanyDetails(cursor.getString(cursor.getColumnIndex("companyId")),
                        cursor.getString(cursor.getColumnIndex("companyLogo")),
                        cursor.getString(cursor.getColumnIndex("companyName")),
                        cursor.getString(cursor.getColumnIndex("cashierName")),
                        cursor.getString(cursor.getColumnIndex("companyMobile")),
                        cursor.getString(cursor.getColumnIndex("companyAddress")),
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
        cursor = posBillingWalaDatabase.getUnSynchronizeInvoice(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
                        cursor.getString(cursor.getColumnIndex("totalAmount")),
                        cursor.getString(cursor.getColumnIndex("paymentMode")),
                        cursor.getString(cursor.getColumnIndex("invoiceDate")),
                        cursor.getString(cursor.getColumnIndex("invoiceType")),
                        cursor.getString(cursor.getColumnIndex("invoiceOrderStatus")),
                        cursor.getString(cursor.getColumnIndex("invoiceNetworkStatus")));
            } while (cursor.moveToNext());
        }
        closeCursor();
        cursor = posBillingWalaDatabase.getUnSynchronizeInvoiceProduct(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
                        columnOrEmpty(cursor, "snapshotLinePrice"));
            } while (cursor.moveToNext());
        }
        closeCursor();
        cursor = posBillingWalaDatabase.getUnSynchronizeMessMember(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
        cursor = posBillingWalaDatabase.getUnSynchronizeMessMemberPayment(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
        cursor = posBillingWalaDatabase.getUnSynchronizeMessInvoice(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
        cursor = posBillingWalaDatabase.getUnSynchronizeInventory(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
        cursor = posBillingWalaDatabase.getUnSynchronizeExpenses(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
    }

    public void saveMessInvoice(String invoiceId, String memberId, String memberName, String messType, String messInvoiceDate, String messInvoiceNetworkStatus, String messInvoiceStatus) {
        if (executeCall(Api.getClient(context).saveMessInvoice(MainActivity.userId, memberName, messType, messInvoiceDate, messInvoiceNetworkStatus, messInvoiceStatus))) {
            posBillingWalaDatabase.updateSyncMessInvoice(invoiceId, NAME_SYNCED_WITH_SERVER);
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

    public void saveInvoiceProduct(String invoiceProductId, String invoiceNumber, String productName, String productPrice, String productUnit, String productCGST, String productSGST, String productQuantity, String productStatus, String invoiceProductNetworkStatus, String portionId, String portionName, String snapshotProductName, String snapshotLinePrice) {
        if (executeCall(Api.getClient(context).saveInvoiceProduct(invoiceNumber, productName, productPrice, productUnit, productCGST, productSGST, productQuantity, productStatus, invoiceProductNetworkStatus, portionId, portionName, snapshotProductName, snapshotLinePrice))) {
            posBillingWalaDatabase.updateSyncInvoiceProduct(invoiceProductId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveInvoice(String invoiceId, String noOfTable, String invoiceNumber, String customerName, String customerMobile, String customerEmail, String customerAddress, String subTotal, String totalGSTAmount,
                            String discount, String discountType, String totalAmount, String paymentMode, String invoiceDate, String invoiceType, String invoiceOrderStatus, String invoiceNetworkStatus) {
        if (executeCall(Api.getClient(context).saveInvoice(MainActivity.userId, noOfTable, invoiceNumber, customerName, customerMobile, customerEmail, customerAddress, subTotal,
                totalGSTAmount, discount, discountType, totalAmount, paymentMode, invoiceDate, invoiceType, invoiceOrderStatus, invoiceNetworkStatus))) {
            posBillingWalaDatabase.updateSyncInvoice(invoiceId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveCompanyDetails(String companyId, String companyLogo, String companyName, String cashierName, String companyMobile, String companyAddress, String currencyName, String tableStatus, String noOfTable,
                                   String countryName, String stateName, String gstStatus, String gstNumber, String panNumber, String paymentLogo, String companyFssis) {
        if (executeCall(Api.getClient(context).saveCompanyDetails(MainActivity.userId, companyLogo, companyName, cashierName, companyMobile, companyAddress, currencyName, tableStatus, noOfTable, countryName, stateName,
                gstStatus, gstNumber, panNumber, paymentLogo, companyFssis))) {
            posBillingWalaDatabase.updateSynchronizeCompanyDetails(companyId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void savePrinterSetting(String settingId, String printerName, String KOTPrinterName, String invoicePrefix, String invoiceTitle, String invoiceTermsCondition, String logoUse, String paymentUse, String customerUse, String productQuantityUpdate, String bluetoothAddress, String bluetoothKOTAddress, String printerFeedLines, String KotPrinterFeedLines) {
        if (executeCall(Api.getClient(context).savePrinterSetting(MainActivity.userId, printerName, KOTPrinterName, invoicePrefix, invoiceTitle, invoiceTermsCondition, logoUse, paymentUse, customerUse, productQuantityUpdate, bluetoothAddress, bluetoothKOTAddress, printerFeedLines, KotPrinterFeedLines))) {
            posBillingWalaDatabase.updateSynchronizePrinterSetting(settingId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveProduct(String productId, String categoryId, String categoryName, String productCode, String productName, String productPrice, String productUnit, String productCGST, String productSGST, String productNetworkStatus, String productDeletedStatus, String subcategoryId) {
        if (executeCall(Api.getClient(context).saveProduct(MainActivity.ownerId, categoryId, categoryName, productCode, productName, productPrice, productUnit, productCGST, productSGST, productNetworkStatus, productDeletedStatus, subcategoryId))) {
            posBillingWalaDatabase.updateSyncProduct(productId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveCategory(String categoryId, String categoryName, String categoryDeletedStatus, String categoryNetworkStatus, String foodTypeCode) {
        if (executeCall(Api.getClient(context).saveCategory(MainActivity.ownerId, categoryName, categoryDeletedStatus, categoryNetworkStatus, foodTypeCode))) {
            posBillingWalaDatabase.updateSyncCategory(categoryId, NAME_SYNCED_WITH_SERVER);
        }
    }

    public void saveSubcategory(String subcategoryId, String categoryId, String categoryNetworkStatus, String subcategoryName, String subcategoryDeletedStatus, String subcategoryNetworkStatus) {
        if (executeCall(Api.getClient(context).saveSubcategory(MainActivity.ownerId, categoryId, categoryNetworkStatus, subcategoryName, subcategoryDeletedStatus, subcategoryNetworkStatus))) {
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

    private boolean executeCall(Call<AllApiResponse> call) {
        try {
            Response<AllApiResponse> response = call.execute();
            return response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus());
        } catch (Exception e) {
            Log.e("UserSynchronizeData", "serverError", e);
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


}
