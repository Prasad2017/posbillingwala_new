package com.pos_billingwala.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Fragment.CreatePos;

public class HomeAllProductsWorker extends Worker {

    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private final Context context;

    public HomeAllProductsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            CreatePos.homeProductResponseList.clear();
            CreatePos.homeProductResponseList = posBillingWalaDatabase.getAllProductList(CreatePos.tableNumber, CreatePos.cartOrderStatus);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}