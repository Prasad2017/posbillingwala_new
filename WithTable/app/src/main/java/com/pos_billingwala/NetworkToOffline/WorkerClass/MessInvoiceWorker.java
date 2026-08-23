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
import com.pos_billingwala.Model.MessInvoiceResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class MessInvoiceWorker extends Worker {

    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private final Context context;

    public MessInvoiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 60).build());
            // Call your API to get mess invoice list
            Call<AllApiResponse> call = Api.getClient(context).getMessInvoiceList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute(); // Execute synchronously

            if (response.isSuccessful() && response.body() != null) {
                List<MessInvoiceResponse> messInvoiceResponseList = response.body().getMessInvoiceResponseList();
                if (!messInvoiceResponseList.isEmpty()) {
                    for (MessInvoiceResponse messInvoiceResponse : messInvoiceResponseList) {
                        try {
                            // Add each mess invoice to the database
                            posBillingWalaDatabase.addMessInvoice(messInvoiceResponse);
                        } catch (Exception e) {
                            Log.e("MessInvoiceWorker", "Error inserting invoice: ", e);
                        }
                    }
                }
            }

            // Return success if everything worked fine
            return Result.success();
        } catch (Exception e) {
            Log.e("MessInvoiceWorker", "Error fetching or inserting invoice data", e);
            return Result.failure(); // Return failure if something went wrong
        }
    }
}

