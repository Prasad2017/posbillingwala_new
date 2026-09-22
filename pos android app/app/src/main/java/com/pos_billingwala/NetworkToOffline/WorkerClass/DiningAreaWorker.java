package com.pos_billingwala.NetworkToOffline.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.DiningAreaResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class DiningAreaWorker extends Worker {

    private final POSBillingWalaDatabase database;
    private final Context context;

    public DiningAreaWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.database = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 0).build());
            Call<AllApiResponse> call = Api.getClient(context).getDiningAreaList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                List<DiningAreaResponse> list = response.body().getDiningAreaResponseList();
                if (list != null) {
                    for (DiningAreaResponse area : list) {
                        database.upsertDiningAreaFromCloud(
                                area.getAreaName(),
                                area.getAreaSortOrder(),
                                area.getAreaActive(),
                                area.getAreaNetworkStatus());
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
