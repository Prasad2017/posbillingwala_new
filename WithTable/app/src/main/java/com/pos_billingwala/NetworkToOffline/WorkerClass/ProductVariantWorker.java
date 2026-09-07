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
import com.pos_billingwala.Model.ProductVariantResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/** Cloud → local fashion/jewellery product variants. */
public class ProductVariantWorker extends Worker {

    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private final Context context;

    public ProductVariantWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 99).build());
            Call<AllApiResponse> call = Api.getClient(context).getProductVariantList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                List<ProductVariantResponse> list = response.body().getProductVariantResponseList();
                if (list != null) {
                    for (ProductVariantResponse v : list) {
                        String localKey = v.getLocalVariantId();
                        if (localKey == null || localKey.trim().isEmpty()) {
                            localKey = v.getVariantId();
                        }
                        if (localKey == null || localKey.trim().isEmpty()) {
                            continue;
                        }
                        posBillingWalaDatabase.upsertProductVariantFromCloud(
                                localKey.trim(),
                                v.getProductNetworkStatus(),
                                v.getProductId(),
                                v.getVariantSize(),
                                v.getVariantColor(),
                                v.getVariantSku(),
                                v.getVariantPrice(),
                                v.getVariantDeletedStatus(),
                                v.getVariantSortOrder());
                    }
                }
                setProgressAsync(new Data.Builder().putInt("progress", 100).build());
            }
            return Result.success();
        } catch (Exception e) {
            Log.e("ProductVariantWorker", "Error fetching variants", e);
            return Result.failure();
        }
    }
}
