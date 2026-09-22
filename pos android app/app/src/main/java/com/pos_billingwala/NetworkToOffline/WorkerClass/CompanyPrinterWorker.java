package com.pos_billingwala.NetworkToOffline.WorkerClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class CompanyPrinterWorker extends Worker {

    private final POSBillingWalaDatabase database;
    private final Context context;

    public CompanyPrinterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.database = new POSBillingWalaDatabase(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            setProgressAsync(new Data.Builder().putInt("progress", 30).build());

            Call<AllApiResponse> call = Api.getClient(context).getCompanyPrinterSetting(MainActivity.userId);
            Response<AllApiResponse> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                List<PrinterSettingResponse> printerSettingResponseList = response.body().getPrinterSettingResponseList();
                if (!printerSettingResponseList.isEmpty()) {
                    for (PrinterSettingResponse printerSettingResponse : printerSettingResponseList) {
                        database.addCompanyPrinterSetting(
                                printerSettingResponse.getPrinterName(),
                                printerSettingResponse.getKOTPrinterName(),
                                printerSettingResponse.getInvoicePrefix(),
                                printerSettingResponse.getInvoiceTitle(),
                                printerSettingResponse.getLogoUse(),
                                printerSettingResponse.getPaymentUse(),
                                printerSettingResponse.getCustomerUse(),
                                printerSettingResponse.getProductQuantityUpdate(),
                                printerSettingResponse.getDuplicateBillUse() != null
                                        ? printerSettingResponse.getDuplicateBillUse() : "off",
                                printerSettingResponse.getInvoiceTermsCondition(),
                                printerSettingResponse.getBluetoothAddress(),
                                printerSettingResponse.getBluetoothKOTAddress(),
                                printerSettingResponse.getPrinterFeedLines(),
                                printerSettingResponse.getKotPrinterFeedLines(),
                                1);
                        // Apply KOT settings from cloud without flipping sync status again
                        java.util.List<PrinterSettingResponse> local =
                                database.getPrinterSettingDetails();
                        if (local != null && !local.isEmpty()) {
                            database.applyKotSettingsFromCloud(
                                    local.get(0).getSettingId(),
                                    printerSettingResponse.getKotEnable(),
                                    printerSettingResponse.getKotPrefix(),
                                    printerSettingResponse.getKotCopies(),
                                    printerSettingResponse.getKotAutoPrint(),
                                    printerSettingResponse.getKotPreview());
                        }
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
