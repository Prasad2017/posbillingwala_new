package com.pos_billingwala.Fragment;

import android.app.DatePickerDialog;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.pos_billingwala.Activity.CompanyPrinterSetting;
import com.pos_billingwala.Activity.LoginMPin;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.AuthTokens;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.LoginResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.NetworkToOffline.NetworkDataFetcher;
import com.pos_billingwala.NetworkToOffline.OfflineNetworkData;
import com.pos_billingwala.NetworkToOffline.PreviousDayInvoiceSync;
import com.pos_billingwala.NetworkToOffline.UserSynchronizeData;
import com.pos_billingwala.Print.KOTWoosimPrnMng;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;
import com.pos_billingwala.databinding.FragmentUserSettingBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, HardwareIds")
public class UserSetting extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static SweetAlertDialog pDialog;
    public static List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    public static String appUrl = "Hello Sir,\n\tOne app for your business to make Easy and Powerful with billing software. Download our POSBillingwala mobile application software and increase your business.";
    public static String appLink = "https://play.google.com/store/apps/details?id=";
    public static OfflineNetworkData offlineNetworkData;
    public static FragmentUserSettingBinding binding;
    //AdView
    public AdView adView;
    View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUserSettingBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new Home(), false);
                    return true;
                }
                return false;
            }
        });

        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            binding.appVersion.setText("V " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        initAds();
        initViews();

        return view;

    }

    public void initViews() {

        binding.backToHome.setOnClickListener(this);
        binding.appDevelopedBy.setOnClickListener(this);
        binding.reportLayout.setOnClickListener(this);
        binding.invoiceDetailsLayout.setOnClickListener(this);
        binding.shopDetailLayout.setOnClickListener(this);
        binding.inventoryManagementLayout.setOnClickListener(this);
        binding.expenseManagementLayout.setOnClickListener(this);
        binding.printerDetailLayout.setOnClickListener(this);
        binding.aboutLayout.setOnClickListener(this);
        binding.fetchDataLayout.setOnClickListener(this);
        binding.syncPreviousDayLayout.setOnClickListener(this);
        binding.synchronizeLayout.setOnClickListener(this);
        binding.logoutLayout.setOnClickListener(this);
        binding.rateUsLayout.setOnClickListener(this);
        binding.updateAppLayout.setOnClickListener(this);
        binding.shareAppLayout.setOnClickListener(this);
        binding.appPinLayout.setOnClickListener(this);

    }

    public void initAds() {

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(activity, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                // on below line displaying a log that admob ads has been initialized.
                Log.i("Admob", "Admob Initialized." + initializationStatus);
            }
        });

        adView = view.findViewById(R.id.ad_view);
        // Create an ad request.
        AdRequest adRequest = new AdRequest.Builder().build();
        // Start loading the ad in the background.
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                super.onAdClicked();
            }

            @Override
            public void onAdClosed() {
                super.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.e("loadAdError", String.valueOf(loadAdError));
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
            }

            @Override
            public void onAdSwipeGestureClicked() {
                super.onAdSwipeGestureClicked();
            }
        });

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToHome) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new Home(), false);
        } else if (id == R.id.appDevelopedBy) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse("https://thecanatech.com/"));
            startActivity(browserIntent);
        } else if (id == R.id.reportLayout) {
            setReportPassword();
        } else if (id == R.id.invoiceDetailsLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new OrderInvoice(), true);
        } else if (id == R.id.shopDetailLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new CompanyDetailSetting(), true);
        } else if (id == R.id.printerDetailLayout) {
            startActivity(new Intent(activity, CompanyPrinterSetting.class));
        } else if (id == R.id.inventoryManagementLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new Inventory(), true);
        } else if (id == R.id.expenseManagementLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new Expenses(), true);
        } else if (id == R.id.aboutLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AboutUs(), true);
        } else if (id == R.id.fetchDataLayout) {
            confirmFetchData();
        } else if (id == R.id.syncPreviousDayLayout) {
            confirmSyncPreviousDay();
        } else if (id == R.id.synchronizeLayout) {
            confirmSynchronizeData();
        } else if (id == R.id.logoutLayout) {
            if (DetectConnection.checkInternetConnection(activity)) {
                logout();
            } else {
                DetectConnection.noInternetConnection(activity);
            }
        } else if (id == R.id.rateUsLayout) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + activity.getPackageName())));
            } catch (ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getPackageName())));
            }
        } else if (id == R.id.updateAppLayout) {
            checkAppUpdates();
        } else if (id == R.id.shareAppLayout) {
            // share app with your friends
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/*");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            shareIntent.putExtra(Intent.EXTRA_TEXT, appUrl + " Download POSBillingwala APP using " + appLink + getActivity().getPackageName());
            startActivity(Intent.createChooser(shareIntent, "Share Using"));
        } else if (id == R.id.appPinLayout) {
            if (DetectConnection.checkInternetConnection(activity)) {
                changeAppMpin();
            } else {
                DetectConnection.noInternetConnection(activity);
            }
        }
    }

    public void changeAppMpin() {

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.report_password_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextView continueToReport = dialog.findViewById(R.id.continueToReport);
        TextView dismissReport = dialog.findViewById(R.id.dismissReport);
        TextInputEditText reportPin = dialog.findViewById(R.id.reportPin);
        TextView details = dialog.findViewById(R.id.details);

        details.setText("Change App Login PB-PIN");
        String appPin = Common.getSavedUserData(activity, "appPin");
        reportPin.setText(appPin);
        int maxLength = 4;
        InputFilter[] fArray = new InputFilter[1];
        fArray[0] = new InputFilter.LengthFilter(maxLength);
        reportPin.setFilters(fArray);
        dismissReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        continueToReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (reportPin.getText().toString().length() == 4) {
                    dialog.dismiss();
                    updateMpin(reportPin.getText().toString());
                } else {
                    reportPin.requestFocus();
                    reportPin.setError("Please enter 4 digit App PIN");
                }
            }
        });

        dialog.show();
        Window window = dialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setAttributes(lp);

    }

    public void updateMpin(String enteredMpin) {

        pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        String licenceKey = Common.getSavedUserData(activity, "LicenceKey");
        String m_androidId = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
        String manufacturerModel = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;

        Call<LoginResponse> call = Api.getClient(activity).updateMpin(enteredMpin, licenceKey, m_androidId, manufacturerModel);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        pDialog.dismiss();
                    } else {
                        pDialog.dismiss();
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText("Oops...");
                sweetAlertDialog.setContentText("Something went wrong!");
                sweetAlertDialog.setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismiss();
                    }
                }).show();
            }
        });

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void checkAppUpdates() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(activity);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (!(activity.isFinishing())) {
                    String strMessage = "Please update our <b> POS " + getResources().getString(R.string.app_name) + "</b> app to new version to continue. Before update our app please upload your data on server. We ae not responsible for losing your data.";
                    new MaterialAlertDialogBuilder(activity, R.style.ThemeDialog)
                            .setIcon(getResources().getDrawable(R.mipmap.ic_launcher))
                            .setTitle("New version available")
                            .setCancelable(false)
                            .setMessage(Html.fromHtml(strMessage))
                            .setPositiveButton("Update", (dialog, whichButton) -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getPackageName() + "&hl=en"));
                                startActivityForResult(intent, 100);
                                activity.finish();
                            }).setNegativeButton("Cancel", (dialog, whichButton) -> dialog.dismiss()).show();
                }
            } else {
                Toast.makeText(activity, "app update not available.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(activity, "app failed to update", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 100) {

            }
        }
    }

    public void setReportPassword() {

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.report_password_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextView continueToReport = dialog.findViewById(R.id.continueToReport);
        TextView dismissReport = dialog.findViewById(R.id.dismissReport);
        TextInputEditText reportPin = dialog.findViewById(R.id.reportPin);

        dismissReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        continueToReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String pin;
                if (MainActivity.reportPin != null) {
                    pin = MainActivity.reportPin;
                } else {
                    pin = "9082";
                }

                if (reportPin.getText().toString().equalsIgnoreCase(pin)) {
                    dialog.dismiss();
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new ReportSetting(), true);
                } else {
                    reportPin.requestFocus();
                    reportPin.setError("Enter correct pin");
                }
            }
        });

        dialog.show();
        Window window = dialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setAttributes(lp);

    }

    public void logout() {

        new MaterialAlertDialogBuilder(activity, R.style.ThemeDialog)
                .setTitle("Logout")
                .setMessage("Do you want to logout from application?")
                .setCancelable(false)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        if (DetectConnection.checkInternetConnection(activity)) {
                            //Offline Receiver register
                            offlineNetworkData = new OfflineNetworkData(activity, "Not-Update");
                            serverLogout();
                        } else {
                            DetectConnection.noInternetConnection(activity);
                        }

                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();

    }

    public void serverLogout() {

        pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient(activity).serverLogout(MainActivity.LicenceKey);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {

                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("true")) {

                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();

                        //To clear data from shared preferences
                        AuthTokens.clear(activity);
                        Common.saveUserData(activity, "userId", "");
                        Common.saveUserData(activity, "ownerId", "");
                        Common.saveUserData(activity, "userName", "");
                        Common.saveUserData(activity, "shopName", "");
                        Common.saveUserData(activity, "shopImage", "");
                        Common.saveUserData(activity, "LicenceKeyRegDate", "");
                        Common.saveUserData(activity, "LicenceKeyExpireDate", "");

                        File file1 = new File("data/data/" + activity.getPackageName() + "/shared_prefs/" + Common.SHARED_PREF + ".xml");
                        if (file1.exists()) {
                            file1.delete();
                        }

                        Intent intent = new Intent(activity, LoginMPin.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        activity.finish();

                    } else {
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                pDialog.dismiss();

            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.e("logoutError", t.getMessage());
                pDialog.dismiss();
            }
        });

    }


    public void confirmSyncPreviousDay() {

        new MaterialAlertDialogBuilder(activity, R.style.ThemeDialog)
                .setTitle("Sync bills for one day")
                .setMessage("Downloads missing bills for the selected date from cloud. Local data is not wiped — only new bills are added.")
                .setCancelable(true)
                .setPositiveButton("Choose date", (dialog, which) -> {
                    dialog.dismiss();
                    showPreviousDayDatePicker();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showPreviousDayDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        DatePickerDialog picker = new DatePickerDialog(activity, (view, year, month, dayOfMonth) -> {
            String invoiceDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            runPreviousDaySync(invoiceDate);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        picker.setTitle("Select date to pull from cloud");
        picker.show();
    }

    private void runPreviousDaySync(String invoiceDate) {
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
            return;
        }

        pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Syncing " + invoiceDate);
        pDialog.setCancelable(false);
        pDialog.show();

        PreviousDayInvoiceSync.sync(activity, invoiceDate, (invoicesAdded, linesAdded, errorMessage) -> {
            if (pDialog != null && pDialog.isShowing()) {
                pDialog.dismiss();
            }
            if (errorMessage != null) {
                Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
                return;
            }
            if (invoicesAdded == 0 && linesAdded == 0) {
                Toast.makeText(activity, "No new bills for " + invoiceDate, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(activity,
                        "Added " + invoicesAdded + " bill(s), " + linesAdded + " line(s) for " + invoiceDate,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    public void confirmFetchData() {

        new MaterialAlertDialogBuilder(activity, R.style.ThemeDialog)
                .setTitle("Do you want to confirm to fetch from cloud?")
                .setMessage("Local data will be replaced with cloud data. Unsynced bills cannot be overwritten — sync them first.")
                .setCancelable(false)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        if (DetectConnection.checkInternetConnection(activity)) {
                            int unsynced = posBillingWalaDatabase.countUnsyncedInvoices();
                            if (unsynced > 0) {
                                Toast.makeText(activity,
                                        unsynced + " unsynced bill(s) found. Send to cloud first, then fetch.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            Toast.makeText(activity, "Data fetching started", Toast.LENGTH_SHORT).show();

                            pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
                            pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
                            pDialog.setTitleText("Loading");
                            pDialog.setCancelable(false);
                            pDialog.show();

                            SQLiteDatabase database = posBillingWalaDatabase.getWritableDatabase();
                            posBillingWalaDatabase.resetTables(database);
                            NetworkDataFetcher.fetchAllData(activity);

                            Toast.makeText(activity, "Data fetched successfully", Toast.LENGTH_SHORT).show();
                            pDialog.dismiss();
                        } else {
                            DetectConnection.noInternetConnection(activity);
                        }
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();

    }

    public void confirmSynchronizeData() {

        new MaterialAlertDialogBuilder(activity, R.style.ThemeDialog)
                .setTitle("Do you want to confirm to send on cloud?")
                .setMessage("Your offline data will be send to the cloud.")
                .setCancelable(false)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        if (DetectConnection.checkInternetConnection(activity)) {
                            Toast.makeText(activity, "Offline Data uploading to server", Toast.LENGTH_SHORT).show();
                            UserSynchronizeData userSynchronizeData = new UserSynchronizeData(activity);
                        } else {
                            DetectConnection.noInternetConnection(activity);
                        }

                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();

    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getPrinterSettingDetails();
    }

    @SuppressLint("MissingPermission")
    public void getPrinterSettingDetails() {
        printerSettingResponseList.clear();
        printerSettingResponseList = posBillingWalaDatabase.getPrinterSettingDetails();
        if (!printerSettingResponseList.isEmpty()) {
            PrinterSettingResponse printerSettingResponse = printerSettingResponseList.get(0);
            String bluetoothAddress = printerSettingResponse.getBluetoothAddress() != null ? printerSettingResponse.getBluetoothAddress() : "";
            if (!bluetoothAddress.equalsIgnoreCase("")) {
                try {
                    new WoosimPrnMng(activity, bluetoothAddress, activity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String bluetoothKOTAddress = printerSettingResponse.getBluetoothKOTAddress() != null ? printerSettingResponse.getBluetoothKOTAddress() : "";
            if (!bluetoothKOTAddress.equalsIgnoreCase("")) {
                try {
                    new KOTWoosimPrnMng(activity, bluetoothAddress, activity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}