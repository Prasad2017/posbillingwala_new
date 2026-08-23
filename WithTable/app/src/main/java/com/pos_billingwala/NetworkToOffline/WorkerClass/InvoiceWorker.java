package com.pos_billingwala.NetworkToOffline.WorkerClass;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class InvoiceWorker extends Worker {

    private final Context context;
    private final POSBillingWalaDatabase posBillingWalaDatabase;

    public InvoiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 40).build());
            Call<AllApiResponse> call = Api.getClient(context).getInvoiceList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                List<InvoiceResponse> invoiceResponseList = response.body().getInvoiceResponseList();
                if (invoiceResponseList != null && !invoiceResponseList.isEmpty()) {
                    for (InvoiceResponse invoiceResponse : invoiceResponseList) {
                        try {
                            posBillingWalaDatabase.addInvoice(invoiceResponse);
                        } catch (Exception e) {
                            Log.e("InvoiceWorker", "insert failed", e);
                        }
                    }
                }
                return Result.success();
            }
            return Result.retry();
        } catch (Exception e) {
            Log.e("InvoiceWorker", "doWork failed", e);
            return Result.retry();
        }
    }
}
