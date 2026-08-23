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

public class MessMemberWorker extends Worker {

    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private final Context context;

    public MessMemberWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 60).build());
            // Call your API to get mess member list
            Call<AllApiResponse> call = Api.getClient(context).getMessMemberList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();  // Execute the call synchronously

            if (response.isSuccessful() && response.body() != null) {
                List<MemberResponse> memberResponseList = response.body().getMemberResponseList();
                if (!memberResponseList.isEmpty()) {
                    for (MemberResponse memberResponse : memberResponseList) {
                        // Insert each member into the database
                        posBillingWalaDatabase.addMessMember(memberResponse);
                    }
                }
            }

            // You can trigger the next step (getting the payment list) here
            // If you need to chain workers, you can return Result.success() here and enqueue another worker for the next task.

            return Result.success();  // Return success if everything worked fine
        } catch (Exception e) {
            Log.e("MessMemberWorker", "Error fetching or inserting data", e);
            return Result.failure();  // Return failure if something went wrong
        }
    }
}

