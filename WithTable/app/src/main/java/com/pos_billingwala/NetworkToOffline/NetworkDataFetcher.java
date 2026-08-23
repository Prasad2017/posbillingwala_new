package com.pos_billingwala.NetworkToOffline;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.lifecycle.LifecycleOwner;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.pos_billingwala.NetworkToOffline.WorkerClass.CategoryWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.CompanyPrinterWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.CompanyWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.ExpensesWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.FoodTypeWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.InventoryWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.InvoiceProductWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.InvoiceWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.MessInvoiceWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.MessMemberPaymentWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.MessMemberWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.PortionWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.ProductWorker;
import com.pos_billingwala.NetworkToOffline.WorkerClass.SubcategoryWorker;

import java.util.Arrays;
import java.util.List;

public class NetworkDataFetcher {

    public static ProgressDialog progressDialog;

    public static void fetchAllData(Context context) {

        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Loading...");
        progressDialog.setMessage("Fetching data...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String dbPath = context.getDatabasePath("pos_billingwala_db").getAbsolutePath();
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbPath, null);
        db.setLockingEnabled(false);

        OneTimeWorkRequest foodTypeRequest = new OneTimeWorkRequest.Builder(FoodTypeWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        OneTimeWorkRequest categoryRequest = new OneTimeWorkRequest.Builder(CategoryWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        OneTimeWorkRequest subcategoryRequest = new OneTimeWorkRequest.Builder(SubcategoryWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        OneTimeWorkRequest productRequest = new OneTimeWorkRequest.Builder(ProductWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        OneTimeWorkRequest portionRequest = new OneTimeWorkRequest.Builder(PortionWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        OneTimeWorkRequest companyRequest = new OneTimeWorkRequest.Builder(CompanyWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        OneTimeWorkRequest companyPrinterRequest = new OneTimeWorkRequest.Builder(CompanyPrinterWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        OneTimeWorkRequest invoiceRequest = new OneTimeWorkRequest.Builder(InvoiceWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        OneTimeWorkRequest invoiceProductRequest = new OneTimeWorkRequest.Builder(InvoiceProductWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        OneTimeWorkRequest messMemberRequest = new OneTimeWorkRequest.Builder(MessMemberWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        OneTimeWorkRequest messInvoiceRequest = new OneTimeWorkRequest.Builder(MessInvoiceWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        OneTimeWorkRequest messMemberPaymentRequest = new OneTimeWorkRequest.Builder(MessMemberPaymentWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        OneTimeWorkRequest inventoryRequest = new OneTimeWorkRequest.Builder(InventoryWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        OneTimeWorkRequest expensesRequest = new OneTimeWorkRequest.Builder(ExpensesWorker.class).setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()).build();

        // Create a list of WorkRequests to execute sequentially
        List<OneTimeWorkRequest> workRequests = Arrays.asList(
                companyRequest,
                companyPrinterRequest,
                invoiceRequest,
                invoiceProductRequest,
                messMemberRequest,
                messInvoiceRequest,
                messMemberPaymentRequest,
                inventoryRequest,
                expensesRequest);

        // Execute tasks in sequence
        WorkManager.getInstance(context)
                .beginWith(foodTypeRequest)
                .then(categoryRequest)
                .then(subcategoryRequest)
                .then(productRequest)
                .then(portionRequest)
                .then(workRequests)
                .enqueue();

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(expensesRequest.getId()).observe((LifecycleOwner) context, workInfo -> {
            if (workInfo != null && workInfo.getState().isFinished()) {
                // Task completed, dismiss the progress dialog
                progressDialog.dismiss();
            } else {
                // Update progress based on the worker's progress data
                if (workInfo != null && workInfo.getProgress().getKeyValueMap().containsKey("progress")) {
                    int progress = workInfo.getProgress().getInt("progress", 0);
                    progressDialog.setProgress(progress);  // Update progress
                }
            }
        });

    }

}
