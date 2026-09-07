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
import com.pos_billingwala.Model.ServiceAppointmentResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/** Cloud → local salon appointments. */
public class ServiceAppointmentWorker extends Worker {

    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private final Context context;

    public ServiceAppointmentWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 95).build());
            Call<AllApiResponse> call = Api.getClient(context).getServiceAppointmentList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                List<ServiceAppointmentResponse> list = response.body().getServiceAppointmentResponseList();
                if (list != null) {
                    for (ServiceAppointmentResponse a : list) {
                        String localKey = a.getLocalAppointmentId();
                        if (localKey == null || localKey.trim().isEmpty()) {
                            localKey = a.getAppointmentId();
                        }
                        if (localKey == null || localKey.trim().isEmpty()) {
                            continue;
                        }
                        posBillingWalaDatabase.upsertServiceAppointmentFromCloud(
                                localKey.trim(),
                                a.getProductId(),
                                a.getProductName(),
                                a.getCustomerName(),
                                a.getCustomerMobile(),
                                a.getAppointmentAt(),
                                a.getNotes(),
                                a.getAppointmentStatus(),
                                a.getStaffId(),
                                a.getStaffName());
                    }
                }
                setProgressAsync(new Data.Builder().putInt("progress", 100).build());
            }
            return Result.success();
        } catch (Exception e) {
            Log.e("ServiceAppointmentWorker", "Error fetching appointments", e);
            return Result.failure();
        }
    }
}
