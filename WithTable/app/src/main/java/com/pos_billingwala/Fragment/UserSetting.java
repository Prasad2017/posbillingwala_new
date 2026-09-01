package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.pos_billingwala.Activity.CompanyPrinterSetting;
import com.pos_billingwala.Activity.LoginMPin;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.AppLanguage;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.BusinessHours;
import com.pos_billingwala.Extra.AuthTokens;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.LoginResponse;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.NetworkToOffline.CloudSyncNav;
import com.pos_billingwala.NetworkToOffline.NetworkDataFetcher;
import com.pos_billingwala.NetworkToOffline.OfflineNetworkData;
import com.pos_billingwala.NetworkToOffline.UserSynchronizeData;
import com.pos_billingwala.Print.KOTWoosimPrnMng;
import com.pos_billingwala.Print.WoosimPrnMng;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;
import com.pos_billingwala.databinding.FragmentUserSettingBinding;
import com.pos_billingwala.databinding.ItemGroupedMenuRowBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
                    ((MainActivity) activity).navigateBack();
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
        binding.toolbar.toolbarTitle.setText(getString(R.string.user_setting_title));
        binding.toolbar.backButton.setOnClickListener(this);
        binding.appDevelopedBy.setOnClickListener(this);

        setupRow(binding.invoiceDetailsLayout, R.drawable.ic_report_invoice, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.setting_invoice_details), getString(R.string.setting_hint_invoice));
        setupRow(binding.reportLayout, R.drawable.ic_report_sales, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.setting_reports), getString(R.string.setting_hint_reports));
        setupRow(binding.masterDataLayout, R.drawable.ic_inventory, R.drawable.bg_quick_action_purple,
                R.color.deepPurple, getString(R.string.setting_master_data), getString(R.string.setting_hint_master));
        setupRow(binding.shopDetailLayout, R.drawable.ic_business, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.setting_shop_details), getString(R.string.setting_hint_shop));
        setupRow(binding.businessHoursLayout, R.drawable.ic_calendar, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.business_hours), getString(R.string.business_hours_hint));
        setupRow(binding.printerDetailLayout, R.drawable.ic_print, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.setting_printer_details), getString(R.string.setting_hint_printer));
        setupRow(binding.inventoryManagementLayout, R.drawable.ic_report_product, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.setting_inventory), getString(R.string.setting_hint_inventory));
        setupRow(binding.expenseManagementLayout, R.drawable.ic_report_expense, R.drawable.bg_quick_action_purple,
                R.color.deepPurple, getString(R.string.setting_expense), getString(R.string.setting_hint_expense));
        setupRow(binding.supportLayout, R.drawable.ic_phone, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.setting_support), getString(R.string.setting_hint_support));
        setupRow(binding.aboutLayout, R.drawable.ic_info, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.setting_about), getString(R.string.setting_hint_about));
        setupRow(binding.fetchDataLayout, R.drawable.ic_cloud_download, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.setting_fetch_data), getString(R.string.setting_hint_fetch));
        setupRow(binding.updateAppLayout, R.drawable.ic_store, R.drawable.bg_quick_action_purple,
                R.color.deepPurple, getString(R.string.setting_update_app), getString(R.string.setting_hint_update));
        setupRow(binding.synchronizeLayout, R.drawable.ic_cloud_upload, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.setting_synchronize), getString(R.string.setting_hint_sync));
        setupRow(binding.appPinLayout, R.drawable.ic_lock, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.setting_change_pin), getString(R.string.setting_hint_pin));
        setupRow(binding.languageLayout, R.drawable.ic_language, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.language_settings), getString(R.string.language_settings_subtitle));
        setupRow(binding.rateUsLayout, R.drawable.ic_star, R.drawable.bg_quick_action_purple,
                R.color.deepPurple, getString(R.string.setting_rate_us), getString(R.string.setting_hint_rate));
        setupRow(binding.shareAppLayout, R.drawable.ic_share, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.setting_share_app), getString(R.string.setting_hint_share));
        setupRow(binding.logoutLayout, R.drawable.ic_logout, R.drawable.bg_quick_action_red,
                R.color.statusExpired, getString(R.string.setting_logout), getString(R.string.setting_hint_logout));
        binding.logoutLayout.menuTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.statusExpired));

        showGroupDividers(binding.invoiceDetailsLayout, binding.reportLayout, binding.masterDataLayout);
        showGroupDividers(binding.shopDetailLayout, binding.businessHoursLayout, binding.printerDetailLayout,
                binding.inventoryManagementLayout, binding.expenseManagementLayout);
        showGroupDividers(binding.supportLayout, binding.aboutLayout, binding.fetchDataLayout,
                binding.updateAppLayout, binding.synchronizeLayout);
        showGroupDividers(binding.appPinLayout, binding.languageLayout, binding.rateUsLayout,
                binding.shareAppLayout, binding.logoutLayout);

        binding.invoiceDetailsLayout.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new OrderInvoice(), true));
        binding.reportLayout.getRoot().setOnClickListener(v -> setReportPassword());
        binding.masterDataLayout.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new MasterData(), true));
        binding.shopDetailLayout.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new CompanyDetailSetting(), true));
        binding.businessHoursLayout.getRoot().setOnClickListener(v -> showBusinessHoursDialog());
        binding.printerDetailLayout.getRoot().setOnClickListener(v ->
                startActivity(new Intent(activity, CompanyPrinterSetting.class)));
        binding.inventoryManagementLayout.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new Inventory(), true));
        binding.expenseManagementLayout.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new Expenses(), true));
        binding.supportLayout.getRoot().setOnClickListener(v -> {
            if (DetectConnection.checkInternetConnection(activity)) {
                ((MainActivity) activity).loadFragment(new SupportHub(), true);
            } else {
                Toast.makeText(activity, getString(R.string.support_online_only_notice), Toast.LENGTH_LONG).show();
                DetectConnection.noInternetConnection(activity);
            }
        });
        binding.aboutLayout.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new AboutUs(), true));
        binding.fetchDataLayout.getRoot().setOnClickListener(v -> confirmFetchData());
        binding.updateAppLayout.getRoot().setOnClickListener(v -> checkAppUpdates());
        binding.synchronizeLayout.getRoot().setOnClickListener(v ->
                CloudSyncNav.openFromUi(activity));
        binding.appPinLayout.getRoot().setOnClickListener(v -> {
            if (DetectConnection.checkInternetConnection(activity)) {
                changeAppMpin();
            } else {
                DetectConnection.noInternetConnection(activity);
            }
        });
        binding.languageLayout.getRoot().setOnClickListener(v -> showLanguagePicker());
        binding.rateUsLayout.getRoot().setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + activity.getPackageName())));
            } catch (ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getPackageName())));
            }
        });
        binding.shareAppLayout.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new ShareApp(), true));
        binding.logoutLayout.getRoot().setOnClickListener(v -> {
            if (DetectConnection.checkInternetConnection(activity)) {
                logout();
            } else {
                DetectConnection.noInternetConnection(activity);
            }
        });

        binding.languageLayout.menuSubtitle.setText(getString(
                R.string.language_current,
                AppLanguage.displayName(activity, AppLanguage.getSavedCode(activity))));
    }

    private void setupRow(ItemGroupedMenuRowBinding row, int iconRes, int bgRes, int tintColor,
                          String title, String subtitle) {
        row.menuIcon.setBackgroundResource(bgRes);
        row.menuIcon.setImageResource(iconRes);
        row.menuIcon.clearColorFilter();
        row.menuIcon.setColorFilter(ContextCompat.getColor(requireContext(), tintColor));
        row.menuTitle.setText(title);
        row.menuSubtitle.setText(subtitle);
    }

    private void showGroupDividers(ItemGroupedMenuRowBinding... rows) {
        for (int i = 1; i < rows.length; i++) {
            rows[i].rowDivider.setVisibility(View.VISIBLE);
        }
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
        if (id == R.id.backButton) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.appDevelopedBy) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse("https://thecanatech.com/"));
            startActivity(browserIntent);
        }
    }

    private void showLanguagePicker() {
        final String[] languages = new String[]{
                getString(R.string.language_english),
                getString(R.string.language_hindi),
                getString(R.string.language_marathi)
        };
        int checked = AppLanguage.selectedIndex(activity);
        BottomSheetUi.showSingleChoice(activity, getString(R.string.language_settings), languages, checked, true,
                index -> {
                    String code = AppLanguage.codeForIndex(index);
                    if (!code.equals(AppLanguage.getSavedCode(activity))) {
                        AppLanguage.setLanguage(activity, code);
                    }
                });
    }

    public void changeAppMpin() {
        View content = LayoutInflater.from(activity).inflate(R.layout.report_password_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextView continueToReport = content.findViewById(R.id.continueToReport);
        TextView dismissReport = content.findViewById(R.id.dismissReport);
        TextInputEditText reportPin = content.findViewById(R.id.reportPin);
        TextView details = content.findViewById(R.id.details);

        details.setText("Change App Login PB-PIN");
        String appPin = Common.getSavedUserData(activity, "appPin");
        reportPin.setText(appPin);
        int maxLength = 4;
        InputFilter[] fArray = new InputFilter[1];
        fArray[0] = new InputFilter.LengthFilter(maxLength);
        reportPin.setFilters(fArray);
        dismissReport.setOnClickListener(v -> sheet.dismiss());

        continueToReport.setOnClickListener(v -> {
            if (reportPin.getText().toString().length() == 4) {
                sheet.dismiss();
                updateMpin(reportPin.getText().toString());
            } else {
                reportPin.requestFocus();
                reportPin.setError("Please enter 4 digit App PIN");
            }
        });
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
                    BottomSheetUi.showAction(
                            activity,
                            getString(R.string.toast_new_version_available),
                            Html.fromHtml(strMessage),
                            "Update",
                            "Cancel",
                            R.mipmap.ic_launcher,
                            false,
                            () -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getPackageName() + "&hl=en"));
                                startActivityForResult(intent, 100);
                                activity.finish();
                            },
                            null);
                }
            } else {
                Toast.makeText(activity, getString(R.string.toast_app_update_not_available), Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(activity, getString(R.string.toast_app_failed_to_update), Toast.LENGTH_SHORT).show();
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
        View content = LayoutInflater.from(activity).inflate(R.layout.report_password_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextView continueToReport = content.findViewById(R.id.continueToReport);
        TextView dismissReport = content.findViewById(R.id.dismissReport);
        TextInputEditText reportPin = content.findViewById(R.id.reportPin);

        dismissReport.setOnClickListener(v -> sheet.dismiss());

        continueToReport.setOnClickListener(v -> {
            String pin;
            if (MainActivity.reportPin != null) {
                pin = MainActivity.reportPin;
            } else {
                pin = "9082";
            }

            if (reportPin.getText().toString().equalsIgnoreCase(pin)) {
                sheet.dismiss();
                ((MainActivity) activity).loadFragment(new ReportsHub(), true);
            } else {
                reportPin.requestFocus();
                reportPin.setError("Enter correct pin");
            }
        });
    }

    public void logout() {

        BottomSheetUi.showConfirm(
                activity,
                getString(R.string.setting_logout),
                getString(R.string.toast_do_you_want_to_logout_from_application),
                "YES",
                "NO",
                false,
                () -> {
                    if (DetectConnection.checkInternetConnection(activity)) {
                        offlineNetworkData = new OfflineNetworkData(activity, "Not-Update");
                        serverLogout();
                    } else {
                        DetectConnection.noInternetConnection(activity);
                    }
                });
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


    public void confirmFetchData() {

        BottomSheetUi.showConfirm(
                activity,
                getString(R.string.toast_do_you_want_to_confirm_to_fetch_from_clo),
                getString(R.string.toast_local_data_will_be_replaced_with_cloud_d),
                "YES",
                "NO",
                false,
                () -> {
                    if (DetectConnection.checkInternetConnection(activity)) {
                        Toast.makeText(activity, getString(R.string.toast_data_fetching_started), Toast.LENGTH_SHORT).show();
                        NetworkDataFetcher.resetAndFetchAllData(activity, posBillingWalaDatabase);
                    } else {
                        DetectConnection.noInternetConnection(activity);
                    }
                });
    }

    public void confirmSynchronizeData() {

        BottomSheetUi.showConfirm(
                activity,
                getString(R.string.toast_do_you_want_to_confirm_to_send_on_cloud),
                getString(R.string.toast_your_offline_data_will_be_send_to_the_cl),
                "YES",
                "NO",
                false,
                () -> {
                    if (DetectConnection.checkInternetConnection(activity)) {
                        UserSynchronizeData.start(activity);
                    } else {
                        DetectConnection.noInternetConnection(activity);
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getPrinterSettingDetails();
        refreshBusinessHoursLabel();
    }

    private void refreshBusinessHoursLabel() {
        if (binding == null || binding.businessHoursLayout == null) {
            return;
        }
        binding.businessHoursLayout.menuSubtitle.setText(BusinessHours.displayRange(activity));
    }

    private void showBusinessHoursDialog() {
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_business_hours, null);
        TextView openingValue = content.findViewById(R.id.openingTimeValue);
        TextView closingValue = content.findViewById(R.id.closingTimeValue);
        final int[] openingMinutes = {
                BusinessHours.isConfigured(activity) ? BusinessHours.openingMinutes(activity) : 9 * 60
        };
        final int[] closingMinutes = {
                BusinessHours.isConfigured(activity) ? BusinessHours.closingMinutes(activity) : 22 * 60
        };
        Runnable refreshTimes = () -> {
            openingValue.setText(getString(R.string.opening_time) + ": "
                    + BusinessHours.formatMinutes(openingMinutes[0]));
            closingValue.setText(getString(R.string.closing_time) + ": "
                    + BusinessHours.formatMinutes(closingMinutes[0]));
        };
        refreshTimes.run();
        openingValue.setOnClickListener(v -> pickBusinessTime(openingMinutes, refreshTimes));
        closingValue.setOnClickListener(v -> pickBusinessTime(closingMinutes, refreshTimes));

        BottomSheetUi.showCustom(
                activity,
                getString(R.string.business_hours),
                content,
                getString(R.string.ui_androidstringok),
                getString(R.string.cancel),
                true,
                () -> {
                    BusinessHours.save(activity, openingMinutes[0], closingMinutes[0]);
                    refreshBusinessHoursLabel();
                    Toast.makeText(activity, R.string.business_hours_saved, Toast.LENGTH_SHORT).show();
                    if (DetectConnection.checkInternetConnection(activity)) {
                        UserSynchronizeData.start(activity);
                    }
                },
                null);
    }

    private void pickBusinessTime(int[] minutesHolder, Runnable onPicked) {
        int hour = Math.max(0, minutesHolder[0]) / 60;
        int minute = Math.max(0, minutesHolder[0]) % 60;
        new TimePickerDialog(activity, (view, hourOfDay, minuteOfHour) -> {
            minutesHolder[0] = hourOfDay * 60 + minuteOfHour;
            onPicked.run();
        }, hour, minute, false).show();
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
                    new KOTWoosimPrnMng(activity, bluetoothKOTAddress, activity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}