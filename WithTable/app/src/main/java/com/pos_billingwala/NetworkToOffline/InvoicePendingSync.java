package com.pos_billingwala.NetworkToOffline;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Extra.Observability;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Retrofit.Api;

import retrofit2.Response;

@SuppressLint("Range")
public final class InvoicePendingSync {

    private static final int NAME_SYNCED_WITH_SERVER = 1;
    private static final int NAME_NOT_SYNCED_WITH_SERVER = 0;

    private InvoicePendingSync() {
    }

    /** Background upload after local bill edit/refund (no progress dialog). */
    public static void syncPendingInvoiceChanges(Context context) {
        if (context == null || !DetectConnection.checkInternetConnection(context)) {
            return;
        }
        OfflineSyncExecutor.execute(() -> {
            POSBillingWalaDatabase database = new POSBillingWalaDatabase(context);
            try {
                uploadDeletes(context, database);
                uploadPendingInvoiceProducts(context, database);
                uploadPendingInvoices(context, database);
            } catch (Exception e) {
                Observability.logNonFatal(e, "invoice_pending_sync");
            }
        });
    }

    public static void uploadDeletes(Context context, POSBillingWalaDatabase database) {
        if (context == null || database == null) {
            return;
        }
        Cursor cursor = null;
        try {
            cursor = database.getPendingInvoiceProductDeletes();
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }
            do {
                String deleteId = cursor.getString(cursor.getColumnIndex("deleteId"));
                String invoiceNumber = cursor.getString(cursor.getColumnIndex("invoiceNumber"));
                String networkStatus = cursor.getString(cursor.getColumnIndex("invoiceProductNetworkStatus"));
                if (executeDelete(context, invoiceNumber, networkStatus)) {
                    database.removePendingInvoiceProductDelete(deleteId);
                }
            } while (cursor.moveToNext());
        } finally {
            closeCursor(cursor);
        }
    }

    private static void uploadPendingInvoiceProducts(Context context, POSBillingWalaDatabase database) {
        Cursor cursor = null;
        try {
            cursor = database.getUnSynchronizeInvoiceProduct(NAME_NOT_SYNCED_WITH_SERVER);
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }
            do {
                String invoiceProductId = cursor.getString(cursor.getColumnIndex("invoiceProductId"));
                if (executeProductSave(context, database, cursor) && invoiceProductId != null) {
                    database.updateSyncInvoiceProduct(invoiceProductId, NAME_SYNCED_WITH_SERVER);
                }
            } while (cursor.moveToNext());
        } finally {
            closeCursor(cursor);
        }
    }

    private static void uploadPendingInvoices(Context context, POSBillingWalaDatabase database) {
        Cursor cursor = null;
        try {
            cursor = database.getUnSynchronizeInvoice(NAME_NOT_SYNCED_WITH_SERVER);
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }
            do {
                String invoiceId = cursor.getString(cursor.getColumnIndex("invoiceId"));
                if (executeInvoiceSave(context, cursor) && invoiceId != null) {
                    database.updateSyncInvoice(invoiceId, NAME_SYNCED_WITH_SERVER);
                }
            } while (cursor.moveToNext());
        } finally {
            closeCursor(cursor);
        }
    }

    private static boolean executeDelete(Context context, String invoiceNumber, String networkStatus) {
        try {
            Response<AllApiResponse> response = Api.getClient(context)
                    .deleteInvoiceProduct(MainActivity.userId, invoiceNumber, networkStatus)
                    .execute();
            boolean ok = response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus());
            if (ok) {
                CloudSyncTracker.addUploaded(1);
            }
            return ok;
        } catch (Exception e) {
            Observability.logNonFatal(e, "invoice_product_delete_sync");
            return false;
        }
    }

    private static boolean executeProductSave(Context context, POSBillingWalaDatabase database, Cursor cursor) {
        try {
            Response<AllApiResponse> response = Api.getClient(context).saveInvoiceProduct(
                    columnOrEmpty(cursor, "invoiceNumber"),
                    columnOrEmpty(cursor, "productName"),
                    columnOrEmpty(cursor, "productPrice"),
                    columnOrEmpty(cursor, "productUnit"),
                    columnOrEmpty(cursor, "productCGST"),
                    columnOrEmpty(cursor, "productSGST"),
                    columnOrEmpty(cursor, "productQuantity"),
                    columnOrEmpty(cursor, "productStatus"),
                    columnOrEmpty(cursor, "invoiceProductNetworkStatus"),
                    columnOrEmpty(cursor, "portionId"),
                    columnOrEmpty(cursor, "portionName"),
                    columnOrEmpty(cursor, "snapshotProductName"),
                    columnOrEmpty(cursor, "snapshotLinePrice"),
                    columnOrEmpty(cursor, "invoiceItemType"),
                    database.resolveComboNetworkStatus(columnOrEmpty(cursor, "comboId")),
                    columnOrEmpty(cursor, "snapshotComboComponents")).execute();
            return response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus());
        } catch (Exception e) {
            Observability.logNonFatal(e, "invoice_product_sync");
            return false;
        }
    }

    private static boolean executeInvoiceSave(Context context, Cursor cursor) {
        try {
            Response<AllApiResponse> response = Api.getClient(context).saveInvoice(
                    MainActivity.userId,
                    columnOrEmpty(cursor, "noOfTable"),
                    columnOrEmpty(cursor, "invoiceNumber"),
                    columnOrEmpty(cursor, "customerName"),
                    columnOrEmpty(cursor, "customerMobile"),
                    columnOrEmpty(cursor, "customerEmail"),
                    columnOrEmpty(cursor, "customerAddress"),
                    columnOrEmpty(cursor, "subTotal"),
                    columnOrEmpty(cursor, "totalGSTAmount"),
                    columnOrEmpty(cursor, "discount"),
                    columnOrEmpty(cursor, "discountType"),
                    columnOrEmpty(cursor, "packingCharge").isEmpty() ? "0" : columnOrEmpty(cursor, "packingCharge"),
                    columnOrEmpty(cursor, "packingChargeType").isEmpty() ? "Percentage" : columnOrEmpty(cursor, "packingChargeType"),
                    columnOrEmpty(cursor, "totalAmount"),
                    columnOrEmpty(cursor, "paymentMode"),
                    columnOrEmpty(cursor, "cashAmount"),
                    columnOrEmpty(cursor, "upiAmount"),
                    columnOrEmpty(cursor, "invoiceDate"),
                    columnOrEmpty(cursor, "invoiceType"),
                    columnOrEmpty(cursor, "invoiceOrderStatus"),
                    columnOrEmpty(cursor, "invoiceNetworkStatus")).execute();
            return response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus());
        } catch (Exception e) {
            Observability.logNonFatal(e, "invoice_header_sync");
            return false;
        }
    }

    private static String columnOrEmpty(Cursor cursor, String column) {
        int idx = cursor.getColumnIndex(column);
        if (idx < 0) {
            return "";
        }
        String value = cursor.getString(idx);
        return value != null ? value : "";
    }

    private static void closeCursor(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }
}
