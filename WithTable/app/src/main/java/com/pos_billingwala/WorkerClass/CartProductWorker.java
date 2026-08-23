package com.pos_billingwala.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Activity.BluetoothPrint;
import com.pos_billingwala.Database.POSBillingWalaDatabase;

public class CartProductWorker extends Worker {

    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private final Context context;

    public CartProductWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            BluetoothPrint.productCartResponseList.clear();
            BluetoothPrint.productCartResponseList = posBillingWalaDatabase.getCartProductList(BluetoothPrint.tableNumber, BluetoothPrint.cartOrderStatus);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}
