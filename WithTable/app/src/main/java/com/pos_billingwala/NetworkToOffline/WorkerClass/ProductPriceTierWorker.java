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
import com.pos_billingwala.Model.ProductPriceTierResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/** Cloud → local wholesale price tiers. */
public class ProductPriceTierWorker extends Worker {

    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private final Context context;

    public ProductPriceTierWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 98).build());
            Call<AllApiResponse> call = Api.getClient(context).getProductPriceTierList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                List<ProductPriceTierResponse> list = response.body().getProductPriceTierResponseList();
                if (list != null) {
                    for (ProductPriceTierResponse t : list) {
                        String localKey = t.getLocalTierId();
                        if (localKey == null || localKey.trim().isEmpty()) {
                            localKey = t.getTierId();
                        }
                        if (localKey == null || localKey.trim().isEmpty()) {
                            continue;
                        }
                        posBillingWalaDatabase.upsertProductPriceTierFromCloud(
                                localKey.trim(),
                                t.getProductNetworkStatus(),
                                t.getProductId(),
                                t.getMinQty(),
                                t.getTierPrice(),
                                t.getTierLabel(),
                                t.getTierDeletedStatus());
                    }
                }
                setProgressAsync(new Data.Builder().putInt("progress", 100).build());
            }
            return Result.success();
        } catch (Exception e) {
            Log.e("ProductPriceTierWorker", "Error fetching price tiers", e);
            return Result.failure();
        }
    }
}
