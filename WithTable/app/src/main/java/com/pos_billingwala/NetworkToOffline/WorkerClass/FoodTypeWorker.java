package com.pos_billingwala.NetworkToOffline.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.FoodTypeResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class FoodTypeWorker extends Worker {

    private final POSBillingWalaDatabase database;
    private final Context context;

    public FoodTypeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.database = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Call<AllApiResponse> call = Api.getClient(context).getFoodTypeList();
            Response<AllApiResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                List<FoodTypeResponse> list = response.body().getFoodTypeResponseList();
                if (list != null) {
                    for (FoodTypeResponse item : list) {
                        int sort = 0;
                        try {
                            if (item.getFoodTypeSortOrder() != null && !item.getFoodTypeSortOrder().isEmpty()) {
                                sort = Integer.parseInt(item.getFoodTypeSortOrder());
                            }
                        } catch (NumberFormatException ignored) {
                        }
                        database.upsertFoodTypeFromServer(
                                item.getFoodTypeName(),
                                item.getFoodTypeCode(),
                                sort);
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
