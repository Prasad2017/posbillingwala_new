package com.pos_billingwala.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Fragment.CreatePos;

public class HomeProductsWorker extends Worker {

    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private final Context context;

    public HomeProductsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            CreatePos.productResponseList.clear();
            CreatePos.productResponseList = posBillingWalaDatabase.getHomeProductList(
                    CreatePos.categoryName,
                    CreatePos.tableNumber,
                    CreatePos.cartOrderStatus,
                    CreatePos.selectedSubcategoryId);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}