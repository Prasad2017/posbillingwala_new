package com.pos_billingwala.NetworkToOffline.Receiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Extra.Observability;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.NetworkToOffline.InvoicePendingSync;
import com.pos_billingwala.NetworkToOffline.OfflineSyncExecutor;
import com.pos_billingwala.Retrofit.Api;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint({"Range", "UnsafeProtectedBroadcastReceiver"})
public class OfflineToNetworkReceiver extends BroadcastReceiver {

    //1 means data is synced and 0 means data is not synced
    public static final int NAME_SYNCED_WITH_SERVER = 1;
    public static final int NAME_NOT_SYNCED_WITH_SERVER = 0;
    Context context;
    POSBillingWalaDatabase posBillingWalaDatabase;
    Cursor cursor;

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();
        final Context appContext = context.getApplicationContext();

        if (!DetectConnection.checkInternetConnection(appContext)) {
            pendingResult.finish();
            return;
        }

        OfflineSyncExecutor.execute(() -> {
            try {
                uploadPendingData(appContext);
            } catch (Exception e) {
                Observability.logNonFatal(e, "offline_to_network_upload");
            } finally {
                pendingResult.finish();
            }
        });
    }

    private void uploadPendingData(Context context) {
        this.context = context;
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        //getting all the unSynced Category
        cursor = posBillingWalaDatabase.getUnSynchronizeCategory(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
            do {
                saveCategory(cursor.getString(cursor.getColumnIndex("categoryId")),
                        cursor.getString(cursor.getColumnIndex("categoryName")),
                        cursor.getString(cursor.getColumnIndex("categoryDeletedStatus")),
                        cursor.getString(cursor.getColumnIndex("categoryNetworkStatus")),
                        resolveFoodTypeCode(cursor),
                        columnOrEmpty(cursor, "categorySortOrder"));
            } while (cursor.moveToNext());
        }
        cursor = posBillingWalaDatabase.getUnSynchronizeSubcategory(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
        //getting all the unSynced Product
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
                        columnOrEmpty(cursor, "subcategoryId"),
                        columnOrEmpty(cursor, "openPrice"));
            } while (cursor.moveToNext());
        }
        cursor = posBillingWalaDatabase.getUnSynchronizePortionMaster(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
            do {
                savePortionMaster(cursor.getString(cursor.getColumnIndex("portionMasterId")),
                        cursor.getString(cursor.getColumnIndex("portionName")),
                        cursor.getString(cursor.getColumnIndex("portionMasterDeletedStatus")),
                        cursor.getString(cursor.getColumnIndex("portionMasterNetworkStatus")));
            } while (cursor.moveToNext());
        }
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
        cursor = posBillingWalaDatabase.getUnSynchronizeCombo(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
        cursor = posBillingWalaDatabase.getUnSynchronizeComboItem(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
        //getting all the unSynced Company Printer Setting
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
                        columnOrEmpty(cursor, "duplicateBillUse").isEmpty() ? "off" : columnOrEmpty(cursor, "duplicateBillUse"),
                        cursor.getString(cursor.getColumnIndex("bluetoothAddress")),
                        cursor.getString(cursor.getColumnIndex("bluetoothKOTAddress")),
                        cursor.getString(cursor.getColumnIndex("printerFeedLines")),
                        cursor.getString(cursor.getColumnIndex("KotPrinterFeedLines")));
            } while (cursor.moveToNext());
        }

        //getting all the unSynced Company Details
        cursor = posBillingWalaDatabase.getUnSynchronizeCompanyDetails(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
        InvoicePendingSync.uploadDeletes(context, posBillingWalaDatabase);
        //getting all the unSynced Invoice Product Details
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
                        columnOrEmpty(cursor, "snapshotLinePrice"),
                        columnOrEmpty(cursor, "invoiceItemType"),
                        posBillingWalaDatabase.resolveComboNetworkStatus(columnOrEmpty(cursor, "comboId")),
                        columnOrEmpty(cursor, "snapshotComboComponents"));
            } while (cursor.moveToNext());
        }
        cursor = posBillingWalaDatabase.getUnSynchronizeInvoiceComboItem(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
        //getting all the unSynced Invoice Details
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
        //getting all the unSynced Mess Member
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
        //getting all the unSynced Mess Member Payment
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
        //getting all the unSynced Mess Invoice
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
        //getting all the unSynced Mess QR Tokens
        cursor = posBillingWalaDatabase.getUnSynchronizeMessToken(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
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
        cursor = posBillingWalaDatabase.getUnSynchronizeMessTokenVerify(NAME_NOT_SYNCED_WITH_SERVER);
        if (cursor.moveToFirst()) {
            do {
                verifyMessToken(cursor.getString(cursor.getColumnIndex("tokenId")),
                        cursor.getString(cursor.getColumnIndex("tokenCode")),
                        cursor.getString(cursor.getColumnIndex("verifiedDate")),
                        cursor.getString(cursor.getColumnIndex("verifyNetworkStatus")));
            } while (cursor.moveToNext());
        }
        //getting all the unSynced Inventory
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
        //getting all the unSynced Expenses
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
        posBillingWalaDatabase.close();
    }

    public void saveMessInvoice(String invoiceId, String memberId, String memberName, String messType, String messInvoiceDate, String messInvoiceNetworkStatus, String messInvoiceStatus) {

        Call<AllApiResponse> call = Api.getClient(context).saveMessInvoice(MainActivity.userId, memberName, messType, messInvoiceDate, messInvoiceNetworkStatus, messInvoiceStatus);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncMessInvoice(invoiceId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void saveMessToken(String tokenId, String tokenCode, String memberId, String memberName, String memberMobile,
                              String memberType, String messType, String tokenAmount, String tokenDate,
                              String tokenNetworkStatus) {
        Call<AllApiResponse> call = Api.getClient(context).saveMessToken(
                MainActivity.userId, tokenCode, memberId, memberName, memberMobile, memberType,
                messType, tokenAmount, tokenDate, tokenNetworkStatus);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncMessToken(tokenId);
        }
}

    public void verifyMessToken(String tokenId, String tokenCode, String verifiedDate, String verifyNetworkStatus) {
        Call<AllApiResponse> call = Api.getClient(context).verifyMessToken(
                MainActivity.userId, tokenCode, verifiedDate, verifyNetworkStatus);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncMessTokenVerify(tokenId);
        }
}

    public void saveMessMemberPayment(String paymentId, String memberId, String memberName, String paymentMessAmount, String paymentPaidAmount, String messTotalDays, String paymentDate, String paymentNetworkStatus, String paymentStatus) {

        Call<AllApiResponse> call = Api.getClient(context).saveMessMemberPayment(MainActivity.userId, memberId, memberName, paymentMessAmount, paymentPaidAmount, messTotalDays, paymentDate, paymentNetworkStatus, paymentStatus);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncMessMemberPayment(paymentId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void saveMessMember(String memberId, String memberName, String memberMobileNumber, String memberAlternetMobileNumber, String memberAddress, String memberNetworkStatus, String memberStatus) {

        Call<AllApiResponse> call = Api.getClient(context).saveMessMember(MainActivity.userId, memberName, memberMobileNumber, memberAlternetMobileNumber, memberAddress, memberNetworkStatus, memberStatus);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncMessMember(memberId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void saveExpenses(String expensesId, String expensesName, String expensesAmount, String expensesDate, String expensesNetworkStatus, String expensesStatus) {

        Call<AllApiResponse> call = Api.getClient(context).saveExpenses(MainActivity.userId, expensesName, expensesAmount, expensesDate, expensesNetworkStatus);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncExpenses(expensesId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void saveInventory(String inventoryId, String productId, String productInventoryQuantity, String afterSaleInventoryQuantity, String saleInventoryQuantity, String inventoryDate, String inventoryNetworkStatus, String inventoryStatus) {

        Call<AllApiResponse> call = Api.getClient(context).saveInventory(MainActivity.userId, productId, productInventoryQuantity, afterSaleInventoryQuantity, saleInventoryQuantity, inventoryDate, inventoryNetworkStatus);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncInventory(inventoryId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void saveInvoiceProduct(String invoiceProductId, String invoiceNumber, String productName, String productPrice, String productUnit, String productCGST, String productSGST, String productQuantity, String productStatus, String invoiceProductNetworkStatus, String portionId, String portionName, String snapshotProductName, String snapshotLinePrice, String invoiceItemType, String comboNetworkStatus, String snapshotComboComponents) {

        Call<AllApiResponse> call = Api.getClient(context).saveInvoiceProduct(invoiceNumber, productName, productPrice, productUnit, productCGST, productSGST, productQuantity, productStatus, invoiceProductNetworkStatus, portionId, portionName, snapshotProductName, snapshotLinePrice, invoiceItemType, comboNetworkStatus, snapshotComboComponents);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncInvoiceProduct(invoiceProductId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void saveInvoice(String invoiceId, String noOfTable, String invoiceNumber, String customerName, String customerMobile, String customerEmail, String customerAddress, String subTotal, String totalGSTAmount,
                            String discount, String discountType, String totalAmount, String paymentMode, String invoiceDate, String invoiceType, String invoiceOrderStatus, String invoiceNetworkStatus) {

        Call<AllApiResponse> call = Api.getClient(context).saveInvoice(MainActivity.userId,
                nz(noOfTable), nz(invoiceNumber), nz(customerName), nz(customerMobile), nz(customerEmail), nz(customerAddress),
                nz(subTotal), nz(totalGSTAmount), nz(discount), nz(discountType), nz(totalAmount), nz(paymentMode),
                nz(invoiceDate), nz(invoiceType), nz(invoiceOrderStatus), nz(invoiceNetworkStatus));
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncInvoice(invoiceId, NAME_SYNCED_WITH_SERVER);
        }
}

    private static String nz(String value) {
        return value != null ? value : "";
    }

    public void saveCompanyDetails(String companyId, String companyLogo, String companyName, String cashierName, String companyMobile, String companyAddress,
                                   String shopName1, String shopName2, String addressLine1, String addressLine2, String addressLine3, String phoneNo1, String phoneNo2,
                                   String currencyName, String tableStatus, String noOfTable,
                                   String countryName, String stateName, String gstStatus, String gstNumber, String panNumber, String paymentLogo, String companyFssis) {

        Log.e("currencyName:=", currencyName);

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

        Call<AllApiResponse> call = Api.getClient(context).saveCompanyDetails(MainActivity.userId, companyLogo, companyName, cashierName, companyMobile, companyAddress,
                shopName1, shopName2, addressLine1, addressLine2, addressLine3, phoneNo1, phoneNo2,
                currencyName, tableStatus, noOfTable, countryName, stateName,
                gstStatus, gstNumber, panNumber, paymentLogo, companyFssis);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSynchronizeCompanyDetails(companyId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void savePrinterSetting(String settingId, String printerName, String KOTPrinterName, String invoicePrefix, String invoiceTitle, String invoiceTermsCondition, String logoUse, String paymentUse, String customerUse, String productQuantityUpdate, String duplicateBillUse, String bluetoothAddress, String bluetoothKOTAddress, String printerFeedLines, String KotPrinterFeedLines) {

        Call<AllApiResponse> call = Api.getClient(context).savePrinterSetting(MainActivity.userId,
                nz(printerName), nz(KOTPrinterName), nz(invoicePrefix), nz(invoiceTitle), nz(invoiceTermsCondition),
                nz(logoUse), nz(paymentUse), nz(customerUse), nz(productQuantityUpdate), nz(duplicateBillUse),
                nz(bluetoothAddress), nz(bluetoothKOTAddress), nz(printerFeedLines), nz(KotPrinterFeedLines));
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSynchronizePrinterSetting(settingId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void saveProduct(String productId, String categoryId, String categoryName, String productCode, String productName, String productPrice, String productUnit, String productCGST, String productSGST, String productNetworkStatus, String productDeletedStatus, String subcategoryId, String openPrice) {

        String openPriceValue = (openPrice == null || openPrice.trim().isEmpty()) ? "off" : openPrice;
        Call<AllApiResponse> call = Api.getClient(context).saveProduct(MainActivity.ownerId, categoryId, categoryName, productCode, productName, productPrice, productUnit, productCGST, productSGST, productNetworkStatus, productDeletedStatus, subcategoryId, openPriceValue);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncProduct(productId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void saveCategory(String categoryId, String categoryName, String categoryDeletedStatus, String categoryNetworkStatus, String foodTypeCode, String categorySortOrder) {

        Call<AllApiResponse> call = Api.getClient(context).saveCategory(MainActivity.ownerId, categoryName, categoryDeletedStatus, categoryNetworkStatus, foodTypeCode, categorySortOrder);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncCategory(categoryId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void saveSubcategory(String subcategoryId, String categoryId, String categoryNetworkStatus, String subcategoryName, String subcategoryDeletedStatus, String subcategoryNetworkStatus, String subcategorySortOrder) {

        Call<AllApiResponse> call = Api.getClient(context).saveSubcategory(MainActivity.ownerId, categoryId, categoryNetworkStatus, subcategoryName, subcategoryDeletedStatus, subcategoryNetworkStatus, subcategorySortOrder);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncSubcategory(subcategoryId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void savePortionMaster(String portionMasterId, String portionName, String portionMasterDeletedStatus,
                                  String portionMasterNetworkStatus) {

        Call<AllApiResponse> call = Api.getClient(context).savePortionMaster(
                MainActivity.ownerId, portionName, portionMasterDeletedStatus, portionMasterNetworkStatus);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncPortionMaster(portionMasterId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void savePortion(String portionId, String productId, String productNetworkStatus, String portionName,
                            String portionPrice, String portionSortOrder, String portionDeletedStatus,
                            String portionNetworkStatus, String portionMasterId, String portionMasterNetworkStatus) {

        Call<AllApiResponse> call = Api.getClient(context).savePortion(
                MainActivity.ownerId, productId, productNetworkStatus, portionName, portionPrice, portionSortOrder,
                portionDeletedStatus, portionNetworkStatus, portionMasterId, portionMasterNetworkStatus);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncPortion(portionId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void saveCombo(String comboId, String comboName, String comboCode, String comboPrice, String comboCGST,
                          String comboSGST, String comboWithGSTPrice, String comboActiveStatus,
                          String comboDeletedStatus, String comboNetworkStatus, String comboSortOrder) {
        Call<AllApiResponse> call = Api.getClient(context).saveCombo(
                MainActivity.ownerId, comboName, comboCode, comboPrice, comboCGST, comboSGST, comboWithGSTPrice,
                comboActiveStatus, comboDeletedStatus, comboNetworkStatus, comboSortOrder);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncCombo(comboId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void saveComboItem(String comboItemId, String comboId, String comboNetworkStatus, String productId,
                              String productNetworkStatus, String portionId, String portionNetworkStatus,
                              String comboItemQuantity, String comboItemSortOrder, String comboItemDeletedStatus,
                              String comboItemNetworkStatus) {
        Call<AllApiResponse> call = Api.getClient(context).saveComboItem(
                MainActivity.ownerId, comboId, comboNetworkStatus, productId, productNetworkStatus, portionId,
                portionNetworkStatus, comboItemQuantity, comboItemSortOrder, comboItemDeletedStatus,
                comboItemNetworkStatus);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncComboItem(comboItemId, NAME_SYNCED_WITH_SERVER);
        }
}

    public void saveInvoiceComboItem(String invoiceComboItemId, String invoiceNumber,
                                     String invoiceProductNetworkStatus, String comboNetworkStatus, String productId,
                                     String productNetworkStatus, String productName, String portionId,
                                     String portionNetworkStatus, String portionName, String quantity, String sortOrder,
                                     String invoiceComboItemNetworkStatus) {
        Call<AllApiResponse> call = Api.getClient(context).saveInvoiceComboItem(
                invoiceNumber, invoiceProductNetworkStatus, comboNetworkStatus, productId, productNetworkStatus,
                productName, portionId, portionNetworkStatus, portionName, quantity, sortOrder,
                invoiceComboItemNetworkStatus);
        if (executeCall(call)) {
            posBillingWalaDatabase.updateSyncInvoiceComboItem(invoiceComboItemId, NAME_SYNCED_WITH_SERVER);
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


    private boolean executeCall(Call<AllApiResponse> call) {
        try {
            Response<AllApiResponse> response = call.execute();
            boolean ok = response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus());
            if (!ok) {
                String status = response.body() != null ? response.body().getStatus() : "null_body";
                String msg = response.body() != null ? response.body().getMessage() : "";
                Observability.log("offline_to_network sync failed | HTTP " + response.code()
                        + " | status=" + status + " | msg=" + msg);
            }
            return ok;
        } catch (Exception e) {
            Observability.logNonFatal(e, "offline_to_network_sync");
            return false;
        }
    }

}