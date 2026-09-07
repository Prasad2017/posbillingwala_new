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
import com.pos_billingwala.Model.StaffUserResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/** Cloud → local staff roster. */
public class StaffUserWorker extends Worker {

    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private final Context context;

    public StaffUserWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 100).build());
            Call<AllApiResponse> call = Api.getClient(context).getStaffUserList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                List<StaffUserResponse> list = response.body().getStaffUserResponseList();
                if (list != null) {
                    for (StaffUserResponse s : list) {
                        String localKey = s.getLocalStaffId();
                        if (localKey == null || localKey.trim().isEmpty()) {
                            localKey = s.getStaffId();
                        }
                        if (localKey == null || localKey.trim().isEmpty()) {
                            continue;
                        }
                        posBillingWalaDatabase.upsertStaffUserFromCloud(
                                localKey.trim(),
                                s.getStaffName(),
                                s.getStaffRole(),
                                s.getStaffPin(),
                                s.getStaffActive(),
                                s.getStaffDeletedStatus(),
                                s.getCreatedAt());
                    }
                }
            }
            return Result.success();
        } catch (Exception e) {
            Log.e("StaffUserWorker", "Error fetching staff users", e);
            return Result.failure();
        }
    }
}
