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
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class MessMemberPaymentWorker extends Worker {

    private final Context context;
    private final POSBillingWalaDatabase posBillingWalaDatabase;

    public MessMemberPaymentWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 80).build());
            // Call your API to get mess member payment list
            Call<AllApiResponse> call = Api.getClient(context).getMessMemberPaymentList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();  // Execute the call synchronously

            if (response.isSuccessful() && response.body() != null) {
                List<MemberResponse> memberPaymentResponseList = response.body().getMemberResponseList();
                if (!memberPaymentResponseList.isEmpty()) {
                    for (MemberResponse memberResponse : memberPaymentResponseList) {
                        // Insert each member payment into the database
                        posBillingWalaDatabase.addMessMemberPayment(memberResponse);
                    }
                }
            }

            // Return success if everything worked fine
            return Result.success();
        } catch (Exception e) {
            Log.e("MessMemberPaymentWorker", "Error fetching or inserting payment data", e);
            return Result.failure();  // Return failure if something went wrong
        }
    }
}

