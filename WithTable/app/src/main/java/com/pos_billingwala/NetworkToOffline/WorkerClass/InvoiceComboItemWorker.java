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
import com.pos_billingwala.Model.ComboItemResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class InvoiceComboItemWorker extends Worker {

    private final Context context;
    private final POSBillingWalaDatabase posBillingWalaDatabase;

    public InvoiceComboItemWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 55).build());
            Call<AllApiResponse> call = Api.getClient(context).getInvoiceComboItemList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                List<ComboItemResponse> list = response.body().getInvoiceComboItemResponseList();
                if (list != null && !list.isEmpty()) {
                    posBillingWalaDatabase.addInvoiceComboItemsBatchFromCloud(list);
                }
                return Result.success();
            }
            return Result.retry();
        } catch (Exception e) {
            Log.e("InvoiceComboItemWorker", "doWork failed", e);
            return Result.retry();
        }
    }
}
