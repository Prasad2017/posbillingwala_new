package com.pos_billingwala.NetworkToOffline.WorkerClass;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.InventoryResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class InventoryWorker extends Worker {

    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private final Context context;

    public InventoryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 85).build());
            // Call the API to get inventory list
            Call<AllApiResponse> call = Api.getClient(context).getInventoryList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute(); // Execute the call synchronously

            if (response.isSuccessful() && response.body() != null) {
                List<InventoryResponse> inventoryResponseList = response.body().getInventoryResponseList();
                if (!inventoryResponseList.isEmpty()) {
                    for (InventoryResponse inventoryResponse : inventoryResponseList) {
                        // Insert each inventory record into the database
                        posBillingWalaDatabase.addInventory(
                                inventoryResponse.getProductId(),
                                inventoryResponse.getProductInventoryQuantity(),
                                inventoryResponse.getAfterSaleInventoryQuantity(),
                                inventoryResponse.getSaleInventoryQuantity(),
                                inventoryResponse.getInventoryDate(),
                                1,
                                inventoryResponse.getInventoryNetworkStatus()
                        );
                    }
                }
            }

            // Return success if everything worked fine
            return Result.success();
        } catch (Exception e) {
            Log.e("InventoryWorker", "Error fetching or inserting inventory data", e);
            return Result.failure(); // Return failure if something went wrong
        }
    }
}

