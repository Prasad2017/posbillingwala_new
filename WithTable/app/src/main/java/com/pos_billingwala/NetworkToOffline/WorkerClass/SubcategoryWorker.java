package com.pos_billingwala.NetworkToOffline.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.ProductSubcategoryResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class SubcategoryWorker extends Worker {

    private final POSBillingWalaDatabase database;
    private final Context context;

    public SubcategoryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.database = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Call<AllApiResponse> call = Api.getClient(context).getSubcategoryList(MainActivity.ownerId);
            Response<AllApiResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                List<ProductSubcategoryResponse> list = response.body().getSubcategoryResponseList();
                if (list != null) {
                    for (ProductSubcategoryResponse item : list) {
                        int sortOrder = -1;
                        try {
                            if (item.getSubcategorySortOrder() != null && !item.getSubcategorySortOrder().trim().isEmpty()) {
                                sortOrder = Integer.parseInt(item.getSubcategorySortOrder().trim());
                            }
                        } catch (NumberFormatException ignored) {
                        }
                        database.insertProductSubcategory(
                                item.getCategoryId(),
                                item.getSubcategoryName(),
                                item.getSubcategoryDeletedStatus(),
                                item.getSubcategoryNetworkStatus(),
                                1,
                                sortOrder);
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
