package com.pos_billingwala.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Fragment.CreatePos;

public class ProductCategoryWorker extends Worker {

    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private final Context context;

    public ProductCategoryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            CreatePos.productCategoryResponseList.clear();
            String foodTypeId = CreatePos.selectedFoodTypeId;
            if (foodTypeId == null || foodTypeId.trim().isEmpty()) {
                foodTypeId = String.valueOf(posBillingWalaDatabase.getDefaultFoodTypeId());
                CreatePos.selectedFoodTypeId = foodTypeId;
            }
            CreatePos.productCategoryResponseList = posBillingWalaDatabase.getCategoryListByFoodType(foodTypeId);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}
