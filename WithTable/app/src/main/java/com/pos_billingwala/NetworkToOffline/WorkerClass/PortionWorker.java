package com.pos_billingwala.NetworkToOffline.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class PortionWorker extends Worker {

    private final POSBillingWalaDatabase database;
    private final Context context;

    public PortionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.database = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Call<AllApiResponse> call = Api.getClient(context).getPortionList(MainActivity.ownerId);
            Response<AllApiResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                List<ProductPortionResponse> list = response.body().getPortionResponseList();
                if (list != null) {
                    for (ProductPortionResponse item : list) {
                        if (item.getPortionMasterNetworkStatus() != null
                                && !item.getPortionMasterNetworkStatus().trim().isEmpty()) {
                            database.ensurePortionMasterFromServer(
                                    item.getPortionName(),
                                    "0",
                                    item.getPortionMasterNetworkStatus());
                        }
                        database.insertProductPortion(item);
                    }
                }
            }
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}
