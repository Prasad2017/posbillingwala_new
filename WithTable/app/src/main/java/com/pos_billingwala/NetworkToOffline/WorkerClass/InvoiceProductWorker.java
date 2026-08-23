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
import com.pos_billingwala.Model.InvoiceProductResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class InvoiceProductWorker extends Worker {

    private final Context context;
    private final POSBillingWalaDatabase posBillingWalaDatabase;

    public InvoiceProductWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 50).build());
            Call<AllApiResponse> call = Api.getClient(context).getInvoiceProductList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                List<InvoiceProductResponse> invoiceProductResponseList = response.body().getInvoiceProductResponseList();
                if (invoiceProductResponseList != null && !invoiceProductResponseList.isEmpty()) {
                    for (InvoiceProductResponse invoiceProductResponse : invoiceProductResponseList) {
                        try {
                            posBillingWalaDatabase.addInvoiceProduct(invoiceProductResponse);
                        } catch (Exception e) {
                            Log.e("InvoiceProductWorker", "insert failed", e);
                        }
                    }
                }
                return Result.success();
            }
            return Result.retry();
        } catch (Exception e) {
            Log.e("InvoiceProductWorker", "doWork failed", e);
            return Result.retry();
        }
    }
}
