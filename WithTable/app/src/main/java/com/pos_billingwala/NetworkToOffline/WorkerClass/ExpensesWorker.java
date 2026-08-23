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
import com.pos_billingwala.Model.ExpenseResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class ExpensesWorker extends Worker {

    private final POSBillingWalaDatabase posBillingWalaDatabase;
    private final Context context;

    public ExpensesWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 90).build());
            // Call the API to get expenses list
            Call<AllApiResponse> call = Api.getClient(context).getExpensesList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute(); // Execute the call synchronously

            if (response.isSuccessful() && response.body() != null) {
                List<ExpenseResponse> expenseResponseList = response.body().getExpenseResponseList();
                if (!expenseResponseList.isEmpty()) {
                    for (ExpenseResponse expenseResponse : expenseResponseList) {
                        // Add the expense to the database
                        posBillingWalaDatabase.addExpenses(
                                expenseResponse.getExpenseName(),
                                expenseResponse.getExpenseAmount(),
                                expenseResponse.getExpenseDate(),
                                1,
                                expenseResponse.getExpenseNetworkStatus()
                        );
                    }
                    setProgressAsync(new Data.Builder().putInt("progress", 100).build());
                }
            }

            // Return success if everything worked fine
            return Result.success();
        } catch (Exception e) {
            Log.e("ExpensesWorker", "Error fetching or inserting expenses data", e);
            return Result.failure(); // Return failure if something went wrong
        }
    }
}

