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
import com.pos_billingwala.Model.CustomOrderDepositResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/** Cloud → local bakery custom-order deposits. */
public class CustomOrderDepositWorker extends Worker {

    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private final Context context;

    public CustomOrderDepositWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 97).build());
            Call<AllApiResponse> call = Api.getClient(context).getCustomOrderDepositList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                List<CustomOrderDepositResponse> list = response.body().getCustomOrderDepositResponseList();
                if (list != null) {
                    for (CustomOrderDepositResponse d : list) {
                        String localKey = d.getLocalDepositId();
                        if (localKey == null || localKey.trim().isEmpty()) {
                            localKey = d.getDepositId();
                        }
                        if (localKey == null || localKey.trim().isEmpty()) {
                            continue;
                        }
                        posBillingWalaDatabase.upsertCustomOrderDepositFromCloud(
                                localKey.trim(),
                                d.getProductName(),
                                d.getOrderNote(),
                                d.getDepositAmount(),
                                d.getDueDate(),
                                d.getPhotoFile(),
                                d.getDepositStatus(),
                                d.getCreatedAt());
                    }
                }
                setProgressAsync(new Data.Builder().putInt("progress", 100).build());
            }
            return Result.success();
        } catch (Exception e) {
            Log.e("CustomOrderDepositWorker", "Error fetching deposits", e);
            return Result.failure();
        }
    }
}
