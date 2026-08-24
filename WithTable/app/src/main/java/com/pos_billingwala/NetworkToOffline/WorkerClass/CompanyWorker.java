package com.pos_billingwala.NetworkToOffline.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class CompanyWorker extends Worker {

    private final POSBillingWalaDatabase database;
    private final Context context;

    public CompanyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.database = new POSBillingWalaDatabase(context);
        this.context = context;
    }


    @NonNull
    @Override
    public Result doWork() {

        try {
            setProgressAsync(new Data.Builder().putInt("progress", 20).build());

            Call<AllApiResponse> call = Api.getClient(context).getCompanyList(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                List<CompanyResponse> companyResponseList = response.body().getCompanyResponseList();
                if (!companyResponseList.isEmpty()) {
                    for (CompanyResponse companyResponse : companyResponseList) {
                        database.addCompanyDetails(companyResponse.getCompanyLogo(),
                                companyResponse.getShopName1() != null && !companyResponse.getShopName1().trim().isEmpty()
                                        ? companyResponse.getShopName1() : companyResponse.getCompanyName(),
                                companyResponse.getShopName2(),
                                companyResponse.getCashierName(),
                                companyResponse.getPhoneNo1() != null && !companyResponse.getPhoneNo1().trim().isEmpty()
                                        ? companyResponse.getPhoneNo1() : companyResponse.getCompanyMobile(),
                                companyResponse.getPhoneNo2(),
                                companyResponse.getAddressLine1() != null && !companyResponse.getAddressLine1().trim().isEmpty()
                                        ? companyResponse.getAddressLine1() : companyResponse.getCompanyAddress(),
                                companyResponse.getAddressLine2(),
                                companyResponse.getAddressLine3(),
                                companyResponse.getCurrencyName(),
                                companyResponse.getTableStatus(), companyResponse.getNoOfTable(), companyResponse.getCountryName(), companyResponse.getStateName(), companyResponse.getGstStatus(), companyResponse.getGstNumber(),
                                companyResponse.getShopCGST(), companyResponse.getShopSGST(), companyResponse.getPanNumber(), companyResponse.getCompanyFssis(), 1, companyResponse.getPaymentLogo());
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
