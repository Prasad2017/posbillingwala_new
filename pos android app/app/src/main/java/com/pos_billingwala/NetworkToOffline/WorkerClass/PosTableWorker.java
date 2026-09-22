package com.pos_billingwala.NetworkToOffline.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.PosTableResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class PosTableWorker extends Worker {

    private final POSBillingWalaDatabase database;
    private final Context context;

    public PosTableWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.database = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 0).build());
            Call<AllApiResponse> call = Api.getClient(context).getPosTableList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                List<PosTableResponse> list = response.body().getPosTableResponseList();
                if (list != null) {
                    for (PosTableResponse table : list) {
                        database.upsertPosTableFromCloud(
                                table.getTableNumber(),
                                table.getTableName(),
                                table.getAreaId(),
                                table.getTableTypeId(),
                                table.getCapacity(),
                                table.getTableActive(),
                                table.getPositionX(),
                                table.getPositionY(),
                                table.getSortOrder(),
                                table.getStatusOverride(),
                                table.getPosTableNetworkStatus());
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
