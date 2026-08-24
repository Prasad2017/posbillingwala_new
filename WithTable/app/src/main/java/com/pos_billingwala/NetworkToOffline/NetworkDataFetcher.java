package com.pos_billingwala.NetworkToOffline;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.pos_billingwala.Extra.AppExecutors;
import com.pos_billingwala.NetworkToOffline.WorkerClass.CategoryWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.ComboItemWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.ComboWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.CompanyPrinterWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.CompanyWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.ExpensesWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.FoodTypeWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.InventoryWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.InvoiceComboItemWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.InvoiceProductWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.InvoiceWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.MessInvoiceWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.MessMemberPaymentWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.MessMemberWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.PortionMasterWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.PortionWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.ProductWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.SubcategoryWorker;
import com.pos_billingwala.R;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cloud → local fetch. Runs as a unique WorkManager chain so large downloads
 * stay off the UI thread and progress dismisses only when ALL workers finish.
 */
public class NetworkDataFetcher {

    public static final String FETCH_UNIQUE_NAME = "pos_cloud_fetch";
    public static final String FETCH_TAG = "pos_cloud_fetch_tag";

    public static ProgressDialog progressDialog;
    private static final AtomicBoolean observing = new AtomicBoolean(false);
    /** True after the current unique-work chain has been seen as ENQUEUED/RUNNING. */
    private static final AtomicBoolean sawActiveFetch = new AtomicBoolean(false);

    public static void fetchAllData(Context context) {
        showProgress(context);
        enqueueFetchChain(context);
        observeFetchCompletion(context);
    }

    /**
     * Preferred entry: reset local tables on DB thread, then start fetch.
     * Keeps UI responsive for large shops.
     */
    public static void resetAndFetchAllData(@NonNull Context context,
                                           @NonNull com.pos_billingwala.Database.POSBillingWalaDatabase database) {
        showProgress(context);
        updateProgressMessage("Preparing local database...");
        AppExecutors.get().db().execute(() -> {
            try {
                android.database.sqlite.SQLiteDatabase db = database.getWritableDatabase();
                database.resetTables(db);
            } catch (Exception e) {
                e.printStackTrace();
            }
            AppExecutors.get().main(() -> {
                updateProgressMessage("Fetching data from cloud...");
                enqueueFetchChain(context);
                observeFetchCompletion(context);
            });
        });
    }

    private static void showProgress(Context context) {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception ignored) {
        }
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Loading...");
        progressDialog.setMessage("Fetching data...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private static void updateProgressMessage(String message) {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.setMessage(message);
            }
        } catch (Exception ignored) {
        }
    }

    private static OneTimeWorkRequest tagged(Class<? extends androidx.work.ListenableWorker> workerClass) {
        return new OneTimeWorkRequest.Builder(workerClass)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .addTag(FETCH_TAG)
                .build();
    }

    private static void enqueueFetchChain(Context context) {
        sawActiveFetch.set(false);
        OneTimeWorkRequest foodTypeRequest = tagged(FoodTypeWorker.class);
        OneTimeWorkRequest categoryRequest = tagged(CategoryWorker.class);
        OneTimeWorkRequest subcategoryRequest = tagged(SubcategoryWorker.class);
        OneTimeWorkRequest productRequest = tagged(ProductWorker.class);
        OneTimeWorkRequest portionMasterRequest = tagged(PortionMasterWorker.class);
        OneTimeWorkRequest portionRequest = tagged(PortionWorker.class);
        OneTimeWorkRequest comboRequest = tagged(ComboWorker.class);
        OneTimeWorkRequest comboItemRequest = tagged(ComboItemWorker.class);
        OneTimeWorkRequest companyRequest = tagged(CompanyWorker.class);
        OneTimeWorkRequest companyPrinterRequest = tagged(CompanyPrinterWorker.class);
        OneTimeWorkRequest invoiceRequest = tagged(InvoiceWorker.class);
        OneTimeWorkRequest invoiceProductRequest = tagged(InvoiceProductWorker.class);
        OneTimeWorkRequest invoiceComboItemRequest = tagged(InvoiceComboItemWorker.class);
        OneTimeWorkRequest messMemberRequest = tagged(MessMemberWorker.class);
        OneTimeWorkRequest messInvoiceRequest = tagged(MessInvoiceWorker.class);
        OneTimeWorkRequest messMemberPaymentRequest = tagged(MessMemberPaymentWorker.class);
        OneTimeWorkRequest inventoryRequest = tagged(InventoryWorker.class);
        OneTimeWorkRequest expensesRequest = tagged(ExpensesWorker.class);

        // Sequential chain avoids SQLite contention on large invoice/product pulls
        WorkManager.getInstance(context)
                .beginUniqueWork(FETCH_UNIQUE_NAME, ExistingWorkPolicy.REPLACE, foodTypeRequest)
                .then(categoryRequest)
                .then(subcategoryRequest)
                .then(productRequest)
                .then(portionMasterRequest)
                .then(portionRequest)
                .then(comboRequest)
                .then(comboItemRequest)
                .then(companyRequest)
                .then(companyPrinterRequest)
                .then(invoiceRequest)
                .then(invoiceProductRequest)
                .then(invoiceComboItemRequest)
                .then(messMemberRequest)
                .then(messInvoiceRequest)
                .then(messMemberPaymentRequest)
                .then(inventoryRequest)
                .then(expensesRequest)
                .enqueue();
    }

    private static void observeFetchCompletion(Context context) {
        if (!(context instanceof LifecycleOwner)) {
            pollUntilFinished(context);
            return;
        }
        LifecycleOwner owner = (LifecycleOwner) context;
        // Unique-work LiveData tracks only the current chain (not stale tagged history).
        androidx.lifecycle.LiveData<List<WorkInfo>> liveData =
                WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(FETCH_UNIQUE_NAME);
        liveData.removeObservers(owner);
        observing.set(true);
        liveData.observe(owner, workInfos -> {
            if (workInfos == null || workInfos.isEmpty()) {
                return;
            }
            boolean anyRunning = false;
            boolean anyFailed = false;
            int finished = 0;
            for (WorkInfo info : workInfos) {
                WorkInfo.State state = info.getState();
                if (state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
                        || state == WorkInfo.State.BLOCKED) {
                    anyRunning = true;
                }
                if (state.isFinished()) {
                    finished++;
                }
                if (state == WorkInfo.State.FAILED || state == WorkInfo.State.CANCELLED) {
                    anyFailed = true;
                }
            }
            updateProgressMessage("Fetching data... (" + finished + "/" + workInfos.size() + ")");
            if (anyRunning) {
                sawActiveFetch.set(true);
            }
            // Wait until we have seen active work, then all workers finished — avoids
            // dismissing on a stale finished unique-work snapshot before REPLACE enqueues.
            if (sawActiveFetch.get() && !anyRunning && finished >= workInfos.size()) {
                observing.set(false);
                sawActiveFetch.set(false);
                liveData.removeObservers(owner);
                dismissProgress();
                Toast.makeText(context,
                        anyFailed
                                ? context.getString(R.string.something_went_wrong)
                                : context.getString(R.string.toast_data_fetched_successfully),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void pollUntilFinished(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable check = new Runnable() {
            @Override
            public void run() {
                try {
                    List<WorkInfo> workInfos = WorkManager.getInstance(context)
                            .getWorkInfosForUniqueWork(FETCH_UNIQUE_NAME).get();
                    if (workInfos == null || workInfos.isEmpty()) {
                        handler.postDelayed(this, 1000);
                        return;
                    }
                    boolean anyRunning = false;
                    boolean anyFailed = false;
                    for (WorkInfo info : workInfos) {
                        WorkInfo.State state = info.getState();
                        if (!state.isFinished()) {
                            anyRunning = true;
                        }
                        if (state == WorkInfo.State.FAILED || state == WorkInfo.State.CANCELLED) {
                            anyFailed = true;
                        }
                    }
                    if (anyRunning) {
                        sawActiveFetch.set(true);
                        handler.postDelayed(this, 1000);
                        return;
                    }
                    if (!sawActiveFetch.get()) {
                        handler.postDelayed(this, 1000);
                        return;
                    }
                    sawActiveFetch.set(false);
                    dismissProgress();
                    Toast.makeText(context,
                            anyFailed
                                    ? context.getString(R.string.something_went_wrong)
                                    : context.getString(R.string.toast_data_fetched_successfully),
                            Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    dismissProgress();
                }
            }
        };
        handler.postDelayed(check, 800);
    }

    private static void dismissProgress() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception ignored) {
        }
        progressDialog = null;
    }
}
