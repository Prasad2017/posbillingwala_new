package com.pos_billingwala.NetworkToOffline;

import com.pos_billingwala.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Retrofit.Api;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
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

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm != null ? cm.getActiveNetworkInfo() : null;

        if (activeNetwork == null
                || (activeNetwork.getType() != ConnectivityManager.TYPE_WIFI
                && activeNetwork.getType() != ConnectivityManager.TYPE_MOBILE)) {
            dismissDialogSafely();
            return;
        }

        OfflineSyncExecutor.execute(() -> {
            try {
                uploadPendingData();
            } catch (Exception e) {
                Log.e("UserSynchronizeData", "upload failed", e);
            } finally {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, context.getString(R.string.toast_offline_data_uploaded_to_server), Toast.LENGTH_SHORT).show();
                    dismissDialogSafely();
                });
            }
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
        //getting all the unSynced Category
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
                        columnOrEmpty(cursor, "subcategoryId"));
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
                        columnOrEmpty(cursor, "snapshotLinePrice"));
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
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSyncMessInvoice(invoiceId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });

    }

    public void saveMessMemberPayment(String paymentId, String memberId, String memberName, String paymentMessAmount, String paymentPaidAmount, String messTotalDays, String paymentDate, String paymentNetworkStatus, String paymentStatus) {

        Call<AllApiResponse> call = Api.getClient(context).saveMessMemberPayment(MainActivity.userId, memberId, memberName, paymentMessAmount, paymentPaidAmount, messTotalDays, paymentDate, paymentNetworkStatus, paymentStatus);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSyncMessMemberPayment(paymentId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });

    }

    public void saveMessMember(String memberId, String memberName, String memberMobileNumber, String memberAlternetMobileNumber, String memberAddress, String memberNetworkStatus, String memberStatus) {

        Call<AllApiResponse> call = Api.getClient(context).saveMessMember(MainActivity.userId, memberName, memberMobileNumber, memberAlternetMobileNumber, memberAddress, memberNetworkStatus, memberStatus);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSyncMessMember(memberId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });

    }

    public void saveExpenses(String expensesId, String expensesName, String expensesAmount, String expensesDate, String expensesNetworkStatus, String expensesStatus) {

        Call<AllApiResponse> call = Api.getClient(context).saveExpenses(MainActivity.userId, expensesName, expensesAmount, expensesDate, expensesNetworkStatus);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSyncExpenses(expensesId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });

    }

    public void saveInventory(String inventoryId, String productId, String productInventoryQuantity, String afterSaleInventoryQuantity, String saleInventoryQuantity, String inventoryDate, String inventoryNetworkStatus, String inventoryStatus) {

        Call<AllApiResponse> call = Api.getClient(context).saveInventory(MainActivity.userId, productId, productInventoryQuantity, afterSaleInventoryQuantity, saleInventoryQuantity, inventoryDate, inventoryNetworkStatus);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSyncInventory(inventoryId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });

    }

    public void saveInvoiceProduct(String invoiceProductId, String invoiceNumber, String productName, String productPrice, String productUnit, String productCGST, String productSGST, String productQuantity, String productStatus, String invoiceProductNetworkStatus, String portionId, String portionName, String snapshotProductName, String snapshotLinePrice) {

        Call<AllApiResponse> call = Api.getClient(context).saveInvoiceProduct(invoiceNumber, productName, productPrice, productUnit, productCGST, productSGST, productQuantity, productStatus, invoiceProductNetworkStatus, portionId, portionName, snapshotProductName, snapshotLinePrice);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSyncInvoiceProduct(invoiceProductId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });

    }

    public void saveInvoice(String invoiceId, String noOfTable, String invoiceNumber, String customerName, String customerMobile, String customerEmail, String customerAddress, String subTotal, String totalGSTAmount,
                            String discount, String discountType, String totalAmount, String paymentMode, String invoiceDate, String invoiceType, String invoiceOrderStatus, String invoiceNetworkStatus) {

        Call<AllApiResponse> call = Api.getClient(context).saveInvoice(MainActivity.userId, noOfTable, invoiceNumber, customerName, customerMobile, customerEmail, customerAddress, subTotal,
                totalGSTAmount, discount, discountType, totalAmount, paymentMode, invoiceDate, invoiceType, invoiceOrderStatus, invoiceNetworkStatus);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSyncInvoice(invoiceId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });
    }

    public void saveCompanyDetails(String companyId, String companyLogo, String companyName, String cashierName, String companyMobile, String companyAddress, String currencyName, String tableStatus, String noOfTable,
                                   String countryName, String stateName, String gstStatus, String gstNumber, String panNumber, String paymentLogo, String companyFssis) {

        Call<AllApiResponse> call = Api.getClient(context).saveCompanyDetails(MainActivity.userId, companyLogo, companyName, cashierName, companyMobile, companyAddress, currencyName, tableStatus, noOfTable, countryName, stateName,
                gstStatus, gstNumber, panNumber, paymentLogo, companyFssis);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSynchronizeCompanyDetails(companyId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });
    }

    public void savePrinterSetting(String settingId, String printerName, String KOTPrinterName, String invoicePrefix, String invoiceTitle, String invoiceTermsCondition, String logoUse, String paymentUse, String customerUse, String productQuantityUpdate, String bluetoothAddress, String bluetoothKOTAddress, String printerFeedLines, String KotPrinterFeedLines) {
        Call<AllApiResponse> call = Api.getClient(context).savePrinterSetting(MainActivity.userId, printerName, KOTPrinterName, invoicePrefix, invoiceTitle, invoiceTermsCondition, logoUse, paymentUse, customerUse, productQuantityUpdate, bluetoothAddress, bluetoothKOTAddress, printerFeedLines, KotPrinterFeedLines);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSynchronizePrinterSetting(settingId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });
    }

    public void saveProduct(String productId, String categoryId, String categoryName, String productCode, String productName, String productPrice, String productUnit, String productCGST, String productSGST, String productNetworkStatus, String productDeletedStatus, String subcategoryId) {

        Call<AllApiResponse> call = Api.getClient(context).saveProduct(MainActivity.ownerId, categoryId, categoryName, productCode, productName, productPrice, productUnit, productCGST, productSGST, productNetworkStatus, productDeletedStatus, subcategoryId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSyncProduct(productId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });

    }

    public void saveCategory(String categoryId, String categoryName, String categoryDeletedStatus, String categoryNetworkStatus, String foodTypeCode) {

        Call<AllApiResponse> call = Api.getClient(context).saveCategory(MainActivity.ownerId, categoryName, categoryDeletedStatus, categoryNetworkStatus, foodTypeCode);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSyncCategory(categoryId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });
    }

    public void saveSubcategory(String subcategoryId, String categoryId, String categoryNetworkStatus, String subcategoryName, String subcategoryDeletedStatus, String subcategoryNetworkStatus) {

        Call<AllApiResponse> call = Api.getClient(context).saveSubcategory(MainActivity.ownerId, categoryId, categoryNetworkStatus, subcategoryName, subcategoryDeletedStatus, subcategoryNetworkStatus);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSyncSubcategory(subcategoryId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });
    }

    public void savePortionMaster(String portionMasterId, String portionName, String portionMasterDeletedStatus,
                                  String portionMasterNetworkStatus) {

        Call<AllApiResponse> call = Api.getClient(context).savePortionMaster(
                MainActivity.ownerId, portionName, portionMasterDeletedStatus, portionMasterNetworkStatus);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSyncPortionMaster(portionMasterId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });
    }

    public void savePortion(String portionId, String productId, String productNetworkStatus, String portionName,
                            String portionPrice, String portionSortOrder, String portionDeletedStatus,
                            String portionNetworkStatus, String portionMasterId, String portionMasterNetworkStatus) {

        Call<AllApiResponse> call = Api.getClient(context).savePortion(
                MainActivity.ownerId, productId, productNetworkStatus, portionName, portionPrice, portionSortOrder,
                portionDeletedStatus, portionNetworkStatus, portionMasterId, portionMasterNetworkStatus);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus())) {
                    posBillingWalaDatabase.updateSyncPortion(portionId, NAME_SYNCED_WITH_SERVER);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }
        });
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
