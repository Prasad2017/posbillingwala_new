package com.pos_billingwala.NetworkToOffline.Receiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.pos_billingwala.Activity.Login;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.AuthTokens;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.LicenceExpiredUi;
import com.pos_billingwala.Extra.LicenseSession;
import com.pos_billingwala.Fragment.Home;
import com.pos_billingwala.Model.LoginResponse;
import com.pos_billingwala.Retrofit.Api;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LicenceKeyReceiver extends BroadcastReceiver {

    //context and database helper object
    public Context context;

    public static long getUnitBetweenDates(Date startDate, Date endDate, TimeUnit unit) {
        long timeDiff = endDate.getTime() - startDate.getTime();
        return unit.convert(timeDiff, TimeUnit.MILLISECONDS);
    }

    @SuppressLint("HardwareIds")
    public static void getLicenceKeyData(Context context) {

        String m_androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String manufacturerModel = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;

        Log.e("userId", MainActivity.userId);
        Call<LoginResponse> call = Api.getClient(context).checkLicenceExpire(MainActivity.userId, m_androidId, manufacturerModel);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {

                        Date c = Calendar.getInstance().getTime();
                        System.out.println("Current time => " + c);
                        SimpleDateFormat todayDF = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String todayDate = todayDF.format(c);

                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                        Date startDate, endDate;
                        long numberOfDays = 0;
                        try {
                            startDate = dateFormat.parse(todayDate);
                            endDate = dateFormat.parse(response.body().getLicenceKeyExpireDate());
                            numberOfDays = getUnitBetweenDates(startDate, endDate, TimeUnit.DAYS);

                            // Valid through end of expiry day (aligned with server P4-1)
                            if (numberOfDays >= 0) {

                                Common.saveUserData(context, "userId", response.body().getLicenceId());
                                Common.saveUserData(context, "ownerId", response.body().getOwnerId());
                                Common.saveUserData(context, "userName", response.body().getUserName());
                                Common.saveUserData(context, "shopName", response.body().getShopName());
                                Common.saveUserData(context, "shopImage", response.body().getShopImage());
                                Common.saveUserData(context, "fastBilling", response.body().getFastBilling());
                                Common.saveUserData(context, "takeAway", response.body().getTakeAway());
                                Common.saveUserData(context, "dineIn", response.body().getDineIn());
                                Common.saveUserData(context, "mess", response.body().getMess());
                                Common.saveUserData(context, "LicenceKey", response.body().getLicenceKey());
                                Common.saveUserData(context, "LicenceKeyRegDate", response.body().getLicenceKeyRegDate());
                                Common.saveUserData(context, "LicenceKeyExpireDate", response.body().getLicenceKeyExpireDate());
                                Common.saveUserData(context, "reportPin", response.body().getReportPin());
                                Common.saveUserData(context, "totalSaleData", response.body().getTotalSaleData());
                                Common.saveUserData(context, "todaySaleData", response.body().getTodaySaleData());
                                LicenseSession.saveFromLogin(context, response.body());
                                AuthTokens.saveFromLogin(context, response.body());

                                MainActivity.fastBilling = response.body().getFastBilling();
                                MainActivity.takeAway = response.body().getTakeAway();
                                MainActivity.dineIn = response.body().getDineIn();
                                MainActivity.mess = response.body().getMess();
                                MainActivity.LicenceKeyExpireDate = response.body().getLicenceKeyExpireDate();
                                MainActivity.reportPin = response.body().getReportPin();
                                MainActivity.totalSaleData = response.body().getTotalSaleData();
                                MainActivity.todaySaleData = response.body().getTodaySaleData();

                                Home.totalLicenceDays();
                                Home.setValidationUI();

                            } else {

                                Common.saveUserData(context, "userId", "");
                                File file1 = new File("data/data/" + context.getPackageName() + "/shared_prefs/" + Common.SHARED_PREF + ".xml");
                                if (file1.exists()) {
                                    file1.delete();
                                }

                                Intent intent = new Intent(context, Login.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                intent.putExtra(LicenceExpiredUi.EXTRA_SHOW_LICENCE_EXPIRED, true);
                                context.startActivity(intent);
                                ((MainActivity) context).finish();

                            }

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else {

                        Common.saveUserData(context, "userId", "");
                        File file1 = new File("data/data/" + context.getPackageName() + "/shared_prefs/" + Common.SHARED_PREF + ".xml");
                        if (file1.exists()) {
                            file1.delete();
                        }

                        Intent intent = new Intent(context, Login.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        if (LicenceExpiredUi.isExpiredMessage(response.body().getMessage())) {
                            intent.putExtra(LicenceExpiredUi.EXTRA_SHOW_LICENCE_EXPIRED, true);
                        }
                        context.startActivity(intent);
                        ((MainActivity) context).finish();

                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                Log.e("serverError", t.getMessage());
            }

        });

    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        //if there is a network
        if (activeNetwork != null) {
            //if connected to wifi or mobile data plan
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI || activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                getLicenceKeyData(context);
            }
        }
    }

}
