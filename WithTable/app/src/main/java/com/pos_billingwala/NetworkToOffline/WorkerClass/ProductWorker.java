package com.pos_billingwala.NetworkToOffline.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class ProductWorker extends Worker {

    private final POSBillingWalaDatabase database;
    private final Context context;

    public ProductWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.database = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        try {

            setProgressAsync(new Data.Builder().putInt("progress", 10).build());

            Call<AllApiResponse> call = Api.getClient(context).getProductList(MainActivity.ownerId);
            Response<AllApiResponse> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                List<ProductResponse> productList = response.body().getProductResponseList();
                if (productList != null) {
                    for (ProductResponse product : productList) {
                        database.addProduct(
                                MainActivity.ownerId,
                                product.getCategoryId(),
                                product.getCategoryName(),
                                product.getProductCode(),
                                product.getProductName(),
                                product.getProductPrice(),
                                product.getProductUnit(),
                                product.getProductCGST(),
                                product.getProductSGST(),
                                1,
                                product.getProductNetworkStatus(),
                                product.getProductDeletedStatus(),
                                product.getSubcategoryId()
                        );
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

