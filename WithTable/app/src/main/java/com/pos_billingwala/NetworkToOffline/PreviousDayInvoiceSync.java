package com.pos_billingwala.NetworkToOffline;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Response;

/**
 * Pulls invoices + line items for a single calendar day from cloud without wiping local DB.
 * Uses upsert-by-network-status in {@link POSBillingWalaDatabase#addInvoice} /
 * {@link POSBillingWalaDatabase#addInvoiceProduct} so repeats are safe.
 */
public final class PreviousDayInvoiceSync {

    private static final String TAG = "PreviousDayInvoiceSync";

    public interface Callback {
        void onComplete(int invoicesAdded, int linesAdded, String errorMessage);
    }

    private PreviousDayInvoiceSync() {
    }

    public static void sync(Context context, String invoiceDate, Callback callback) {
        OfflineSyncExecutor.execute(() -> {
            int invoicesAdded = 0;
            int linesAdded = 0;
            String error = null;
            try {
                POSBillingWalaDatabase db = new POSBillingWalaDatabase(context);
                invoicesAdded = pullInvoices(context, db, invoiceDate);
                linesAdded = pullInvoiceProducts(context, db, invoiceDate);
            } catch (Exception e) {
                Log.e(TAG, "sync failed for " + invoiceDate, e);
                error = e.getMessage() != null ? e.getMessage() : "Sync failed";
            }
            deliver(context, callback, invoicesAdded, linesAdded, error);
        });
    }

    private static int pullInvoices(Context context, POSBillingWalaDatabase db, String invoiceDate) throws Exception {
        Response<AllApiResponse> response = Api.getClient(context)
                .getInvoiceListByDate(MainActivity.userId, invoiceDate)
                .execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new Exception("Could not fetch invoices from server");
        }
        List<InvoiceResponse> list = response.body().getInvoiceResponseList();
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (InvoiceResponse invoice : list) {
            invoice.setInvoiceStatus("1");
            if (db.addInvoice(invoice)) {
                added++;
            }
        }
        return added;
    }

    private static int pullInvoiceProducts(Context context, POSBillingWalaDatabase db, String invoiceDate) throws Exception {
        Response<AllApiResponse> response = Api.getClient(context)
                .getInvoiceProductListByDate(MainActivity.userId, invoiceDate)
                .execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new Exception("Could not fetch invoice lines from server");
        }
        List<InvoiceProductResponse> list = response.body().getInvoiceProductResponseList();
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (InvoiceProductResponse line : list) {
            normalizeProductSyncKey(line);
            line.setInvoiceProductStatus("1");
            if (db.addInvoiceProduct(line)) {
                added++;
            }
        }
        return added;
    }

    /** API maps network key into invoiceProductStatus — copy for local dedupe index. */
    private static void normalizeProductSyncKey(InvoiceProductResponse line) {
        String networkKey = line.getInvoiceProductNetworkStatus();
        if (networkKey == null || networkKey.isEmpty()) {
            line.setInvoiceProductNetworkStatus(line.getInvoiceProductStatus());
        }
    }

    private static void deliver(Context context, Callback callback, int invoicesAdded, int linesAdded, String error) {
        if (callback == null) {
            return;
        }
        Runnable done = () -> callback.onComplete(invoicesAdded, linesAdded, error);
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(done);
        } else {
            new Handler(Looper.getMainLooper()).post(done);
        }
    }
}
