package com.pos_billingwala.NetworkToOffline.WorkerClass;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.BusinessTemplateEngine;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.BusinessTemplateSyncResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/** Cloud → local business template selection. */
public class BusinessTemplateWorker extends Worker {

    private final Context context;

    public BusinessTemplateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 100).build());
            Call<AllApiResponse> call = Api.getClient(context).getBusinessTemplate(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                if (com.pos_billingwala.Extra.BusinessSession.hasUnsyncedLocalTemplateChange(context)) {
                    return Result.success();
                }
                List<BusinessTemplateSyncResponse> list =
                        response.body().getBusinessTemplateResponseList();
                if (list != null && !list.isEmpty()) {
                    BusinessTemplateSyncResponse t = list.get(0);
                    if (t != null) {
                        BusinessTemplateEngine.applyFromCloud(
                                context,
                                t.getBusinessType(),
                                t.getBusinessTemplateId(),
                                t.getBusinessTemplateJson());
                    }
                }
            }
            return Result.success();
        } catch (Exception e) {
            Log.e("BusinessTemplateWorker", "Error fetching business template", e);
            return Result.failure();
        }
    }
}
