package com.pos_billingwala.NetworkToOffline.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.ProductCategoryResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class CategoryWorker extends Worker {

    private final POSBillingWalaDatabase database;
    private final Context context;

    public CategoryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.database = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 0).build());

            Call<AllApiResponse> call = Api.getClient(context).getCategoryList(MainActivity.ownerId);
            Response<AllApiResponse> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                List<ProductCategoryResponse> categoryList = response.body().getProductCategoryResponseList();
                if (categoryList != null) {
                    for (ProductCategoryResponse category : categoryList) {
                        long foodTypeId = 0;
                        if (category.getFoodTypeCode() != null && !category.getFoodTypeCode().trim().isEmpty()) {
                            foodTypeId = database.getFoodTypeIdByCode(category.getFoodTypeCode());
                        }
                        int sortOrder = -1;
                        try {
                            if (category.getCategorySortOrder() != null && !category.getCategorySortOrder().trim().isEmpty()) {
                                sortOrder = Integer.parseInt(category.getCategorySortOrder().trim());
                            }
                        } catch (NumberFormatException ignored) {
                        }
                        database.insertProductCategory(category.getCategoryName(), 1, category.getCategoryDeletedStatus(),
                                category.getCategoryNetworkStatus(), foodTypeId, sortOrder);
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

