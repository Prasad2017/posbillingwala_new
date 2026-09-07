package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import com.pos_billingwala.Extra.BusinessTemplateEngine;
import com.pos_billingwala.Extra.BusinessTypeInfo;
import com.pos_billingwala.Extra.DialogUi;
import com.pos_billingwala.Extra.SalonAppointmentModule;
import com.pos_billingwala.Extra.SecurityPermissions;
import com.pos_billingwala.Extra.StaffRole;
import com.pos_billingwala.Extra.CakeBakeryModule;
import com.pos_billingwala.Extra.WeightFreshModule;
import com.pos_billingwala.Model.StaffUserResponse;
import com.pos_billingwala.Extra.UniversalPosModules;
import com.pos_billingwala.Extra.AuthTokens;
import com.pos_billingwala.Extra.FeatureEngine;
import com.pos_billingwala.Extra.FeatureFlags;
import com.pos_billingwala.Extra.FcmTokenManager;
import com.pos_billingwala.Extra.InventoryStockEngine;
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
    private static final int REQUEST_SCALE_DEVICE = 514;

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
        refreshBusinessTemplateRow();
        setupRow(binding.businessHoursLayout, R.drawable.ic_calendar, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.business_hours), getString(R.string.business_hours_hint));
        setupRow(binding.printerDetailLayout, R.drawable.ic_print, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.setting_printer_details), getString(R.string.setting_hint_printer));
        refreshScaleRow();
        setupRow(binding.inventoryManagementLayout, R.drawable.ic_report_product, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.setting_inventory), getString(R.string.setting_hint_inventory));
        FeatureEngine.setVisible(activity, binding.inventoryManagementLayout.getRoot(), FeatureFlags.INVENTORY);
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
        refreshStaffRoleRow();
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
        showGroupDividers(binding.shopDetailLayout, binding.businessTemplateLayout, binding.businessHoursLayout,
                binding.printerDetailLayout, binding.scaleLayout, binding.inventoryManagementLayout,
                binding.expenseManagementLayout);
        showGroupDividers(binding.supportLayout, binding.aboutLayout, binding.fetchDataLayout,
                binding.updateAppLayout, binding.synchronizeLayout);
        showGroupDividers(binding.appPinLayout, binding.staffRoleLayout, binding.languageLayout, binding.rateUsLayout,
                binding.shareAppLayout, binding.logoutLayout);

        binding.invoiceDetailsLayout.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new OrderInvoice(), true));
        binding.reportLayout.getRoot().setOnClickListener(v ->
                SecurityPermissions.runAuthorized(activity, SecurityPermissions.REPORTS,
                        getString(R.string.setting_reports),
                        () -> ((MainActivity) activity).loadFragment(new ReportsHub(), true)));
        binding.masterDataLayout.getRoot().setOnClickListener(v ->
                SecurityPermissions.runAuthorized(activity, SecurityPermissions.MASTER_DATA,
                        getString(R.string.setting_master_data),
                        () -> ((MainActivity) activity).loadFragment(new MasterData(), true)));
        binding.masterDataLayout.getRoot().setOnLongClickListener(v -> {
            boolean appt = SalonAppointmentModule.isEnabled(activity);
            boolean bakery = CakeBakeryModule.isEnabled(activity);
            if (!appt && !bakery) {
                return false;
            }
            if (appt && bakery) {
                String[] items = new String[]{
                        activity.getString(R.string.appointment_calendar_title),
                        activity.getString(R.string.deposit_ledger_title)
                };
                BottomSheetUi.showSingleChoice(activity, getString(R.string.setting_master_data),
                        items, -1, true, index -> {
                            if (index == 0) {
                                SalonAppointmentModule.showUpcomingDialog(activity,
                                        new POSBillingWalaDatabase(activity));
                            } else if (index == 1) {
                                CakeBakeryModule.showDepositLedger(activity);
                            }
                        });
                return true;
            }
            if (appt) {
                SalonAppointmentModule.showUpcomingDialog(activity, new POSBillingWalaDatabase(activity));
                return true;
            }
            CakeBakeryModule.showDepositLedger(activity);
            return true;
        });
        binding.shopDetailLayout.getRoot().setOnClickListener(v ->
                SecurityPermissions.runAuthorized(activity, SecurityPermissions.SHOP_SETTINGS,
                        getString(R.string.setting_shop_details),
                        () -> ((MainActivity) activity).loadFragment(new CompanyDetailSetting(), true)));
        binding.businessTemplateLayout.getRoot().setOnClickListener(v ->
                SecurityPermissions.runAuthorized(activity, SecurityPermissions.BUSINESS_TEMPLATE,
                        getString(R.string.setting_business_template),
                        this::showBusinessTemplateDialog));
        binding.businessHoursLayout.getRoot().setOnClickListener(v ->
                SecurityPermissions.runAuthorized(activity, SecurityPermissions.SHOP_SETTINGS,
                        getString(R.string.business_hours),
                        this::showBusinessHoursDialog));
        binding.printerDetailLayout.getRoot().setOnClickListener(v ->
                SecurityPermissions.runAuthorized(activity, SecurityPermissions.PRINTER_SETTINGS,
                        getString(R.string.setting_printer_details),
                        () -> startActivity(new Intent(activity, CompanyPrinterSetting.class))));
        binding.scaleLayout.getRoot().setOnClickListener(v ->
                SecurityPermissions.runAuthorized(activity, SecurityPermissions.SHOP_SETTINGS,
                        getString(R.string.setting_bluetooth_scale),
                        this::showScaleSheet));
        binding.inventoryManagementLayout.getRoot().setOnClickListener(v ->
                SecurityPermissions.runAuthorized(activity, SecurityPermissions.INVENTORY,
                        getString(R.string.setting_inventory),
                        () -> InventoryStockEngine.openHub(activity)));
        binding.expenseManagementLayout.getRoot().setOnClickListener(v ->
                SecurityPermissions.runAuthorized(activity, SecurityPermissions.EXPENSE,
                        getString(R.string.setting_expense),
                        () -> ((MainActivity) activity).loadFragment(new Expenses(), true)));
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
        binding.fetchDataLayout.getRoot().setOnClickListener(v ->
                SecurityPermissions.runAuthorized(activity, SecurityPermissions.SYNC_FETCH,
                        getString(R.string.setting_fetch_data),
                        this::confirmFetchData));
        binding.updateAppLayout.getRoot().setOnClickListener(v -> checkAppUpdates());
        binding.synchronizeLayout.getRoot().setOnClickListener(v ->
                SecurityPermissions.runAuthorized(activity, SecurityPermissions.SYNC_FETCH,
                        getString(R.string.setting_synchronize),
                        () -> CloudSyncNav.openFromUi(activity)));
        binding.appPinLayout.getRoot().setOnClickListener(v -> {
            if (DetectConnection.checkInternetConnection(activity)) {
                SecurityPermissions.runAuthorized(activity, SecurityPermissions.CHANGE_APP_PIN,
                        getString(R.string.setting_change_pin),
                        this::changeAppMpin);
            } else {
                DetectConnection.noInternetConnection(activity);
            }
        });
        binding.staffRoleLayout.getRoot().setOnClickListener(v ->
                SecurityPermissions.runAuthorized(activity, SecurityPermissions.SET_STAFF_ROLE,
                        getString(R.string.staff_role_pin_title),
                        this::showStaffSessionSheet));
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
        if (requestCode == REQUEST_SCALE_DEVICE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                String addr = data.getExtras().getString(
                        com.pos_billingwala.Print.DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                if (addr != null && !addr.trim().isEmpty()) {
                    WeightFreshModule.setScaleAddress(activity, addr);
                    Toast.makeText(activity, R.string.toast_scale_saved, Toast.LENGTH_SHORT).show();
                    refreshScaleRow();
                }
            }
            return;
        }
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 100) {

            }
        }
    }

    private void refreshScaleRow() {
        if (binding == null || binding.scaleLayout == null || activity == null) {
            return;
        }
        boolean show = WeightFreshModule.isEnabled(activity);
        binding.scaleLayout.getRoot().setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            return;
        }
        String mac = WeightFreshModule.getScaleAddress(activity);
        String hint = mac.isEmpty()
                ? getString(R.string.setting_hint_bluetooth_scale_none)
                : getString(R.string.setting_hint_bluetooth_scale_set, mac);
        setupRow(binding.scaleLayout, R.drawable.ic_inventory, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.setting_bluetooth_scale), hint);
    }

    private void showScaleSheet() {
        if (activity == null) {
            return;
        }
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Runnable> actions = new ArrayList<>();
        labels.add(getString(R.string.scale_pick_device));
        actions.add(() -> {
            try {
                startActivityForResult(
                        new Intent(activity, com.pos_billingwala.Print.DeviceListActivity.class),
                        REQUEST_SCALE_DEVICE);
            } catch (Exception e) {
                Toast.makeText(activity, R.string.toast_scale_pick_failed, Toast.LENGTH_SHORT).show();
            }
        });
        if (WeightFreshModule.hasScaleAddress(activity)) {
            labels.add(getString(R.string.scale_test_read));
            actions.add(this::testScaleRead);
            labels.add(getString(R.string.scale_clear_device));
            actions.add(() -> {
                WeightFreshModule.setScaleAddress(activity, "");
                refreshScaleRow();
                Toast.makeText(activity, R.string.toast_scale_cleared, Toast.LENGTH_SHORT).show();
            });
        }
        BottomSheetUi.showSingleChoice(activity, getString(R.string.setting_bluetooth_scale),
                labels.toArray(new String[0]), -1, true, index -> {
                    if (index >= 0 && index < actions.size()) {
                        actions.get(index).run();
                    }
                });
    }

    private void testScaleRead() {
        Toast.makeText(activity, R.string.toast_scale_reading, Toast.LENGTH_SHORT).show();
        WeightFreshModule.readScaleWeightAsync(activity, new WeightFreshModule.ScaleWeightCallback() {
            @Override
            public void onWeight(float kg) {
                Toast.makeText(activity,
                        getString(R.string.toast_scale_weight, String.format(java.util.Locale.US, "%.3f", kg)),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                int res = R.string.toast_scale_failed;
                if ("bluetooth_off".equals(message)) {
                    res = R.string.toast_scale_bluetooth_off;
                } else if ("no_weight".equals(message)) {
                    res = R.string.toast_scale_no_weight;
                } else if ("scale_not_configured".equals(message)) {
                    res = R.string.toast_scale_not_configured;
                }
                Toast.makeText(activity, res, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void setReportPassword() {
        SecurityPermissions.runAuthorized(activity, SecurityPermissions.REPORTS,
                getString(R.string.setting_reports),
                () -> ((MainActivity) activity).loadFragment(new ReportsHub(), true));
    }

    private void refreshStaffRoleRow() {
        if (binding == null || binding.staffRoleLayout == null || activity == null) {
            return;
        }
        setupRow(binding.staffRoleLayout, R.drawable.ic_lock, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.setting_staff_role),
                SecurityPermissions.sessionLabel(activity) + " — " + getString(R.string.setting_hint_staff_role));
    }

    private void showStaffSessionSheet() {
        if (activity == null) {
            return;
        }
        POSBillingWalaDatabase db = new POSBillingWalaDatabase(activity);
        List<StaffUserResponse> roster = db.getActiveStaffUsers();
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        labels.add(getString(R.string.staff_manage_roster));
        labels.add(getString(R.string.staff_quick_device_role));
        for (StaffUserResponse s : roster) {
            StaffRole role = StaffRole.fromId(s.getStaffRole());
            String mark = "";
            String activeId = SecurityPermissions.getActiveStaffId(activity);
            if (s.getStaffId() != null && s.getStaffId().equals(activeId)) {
                mark = " ✓";
            }
            labels.add(s.getStaffName() + " · " + role.getLabel() + mark);
        }
        BottomSheetUi.showSingleChoice(activity, getString(R.string.staff_session_title),
                labels.toArray(new String[0]), -1, true, index -> {
                    if (index == 0) {
                        showManageStaffRoster(db);
                    } else if (index == 1) {
                        showStaffRolePicker();
                    } else {
                        int rosterIndex = index - 2;
                        if (rosterIndex >= 0 && rosterIndex < roster.size()) {
                            confirmActivateStaff(roster.get(rosterIndex));
                        }
                    }
                });
    }

    private void confirmActivateStaff(StaffUserResponse staff) {
        if (activity == null || staff == null) {
            return;
        }
        String pin = staff.getStaffPin();
        if (pin == null || pin.trim().isEmpty()) {
            SecurityPermissions.activateStaff(activity, staff);
            refreshStaffRoleRow();
            Toast.makeText(activity, R.string.staff_role_applied, Toast.LENGTH_SHORT).show();
            return;
        }
        View content = LayoutInflater.from(activity).inflate(R.layout.report_password_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);
        TextView details = content.findViewById(R.id.details);
        if (details != null) {
            details.setText(getString(R.string.staff_personal_pin_title, staff.getStaffName()));
        }
        TextInputEditText reportPin = content.findViewById(R.id.reportPin);
        content.findViewById(R.id.dismissReport).setOnClickListener(v -> sheet.dismiss());
        content.findViewById(R.id.continueToReport).setOnClickListener(v -> {
            if (SecurityPermissions.matchesStaffPin(staff, reportPin.getText())
                    || SecurityPermissions.matchesReportPin(activity, reportPin.getText())) {
                sheet.dismiss();
                SecurityPermissions.activateStaff(activity, staff);
                refreshStaffRoleRow();
                Toast.makeText(activity, R.string.staff_role_applied, Toast.LENGTH_SHORT).show();
            } else {
                reportPin.requestFocus();
                reportPin.setError(getString(R.string.toast_enter_correct_pin));
            }
        });
    }

    private void showManageStaffRoster(POSBillingWalaDatabase db) {
        if (activity == null || db == null) {
            return;
        }
        List<StaffUserResponse> roster = db.getActiveStaffUsers();
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        labels.add(getString(R.string.staff_add_member));
        for (StaffUserResponse s : roster) {
            labels.add(s.getStaffName() + " · " + StaffRole.fromId(s.getStaffRole()).getLabel());
        }
        BottomSheetUi.showSingleChoice(activity, getString(R.string.staff_manage_roster),
                labels.toArray(new String[0]), -1, true, index -> {
                    if (index == 0) {
                        showAddOrEditStaffDialog(db, null);
                    } else {
                        int rosterIndex = index - 1;
                        if (rosterIndex >= 0 && rosterIndex < roster.size()) {
                            showStaffMemberActions(db, roster.get(rosterIndex));
                        }
                    }
                });
    }

    private void showStaffMemberActions(POSBillingWalaDatabase db, StaffUserResponse staff) {
        if (activity == null || staff == null) {
            return;
        }
        String[] actions = new String[]{
                getString(R.string.staff_edit_member),
                getString(R.string.staff_remove_member)
        };
        BottomSheetUi.showSingleChoice(activity, staff.getStaffName(), actions, -1, true, index -> {
            if (index == 0) {
                showAddOrEditStaffDialog(db, staff);
            } else if (index == 1) {
                BottomSheetUi.showConfirm(activity,
                        getString(R.string.staff_remove_member),
                        getString(R.string.staff_remove_confirm, staff.getStaffName()),
                        "YES", "NO", false, () -> {
                            db.softDeleteStaffUser(staff.getStaffId());
                            String activeId = SecurityPermissions.getActiveStaffId(activity);
                            if (staff.getStaffId() != null && staff.getStaffId().equals(activeId)) {
                                SecurityPermissions.setStaffRole(activity, StaffRole.OWNER);
                            }
                            refreshStaffRoleRow();
                            Toast.makeText(activity, R.string.staff_removed, Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    private void showAddOrEditStaffDialog(POSBillingWalaDatabase db, @Nullable StaffUserResponse existing) {
        if (activity == null || db == null) {
            return;
        }
        android.widget.LinearLayout box = new android.widget.LinearLayout(activity);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, pad / 2);
        android.widget.EditText name = new android.widget.EditText(activity);
        name.setHint(getString(R.string.staff_name_hint));
        name.setSingleLine(true);
        android.widget.EditText pin = new android.widget.EditText(activity);
        pin.setHint(getString(R.string.staff_pin_hint));
        pin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        pin.setSingleLine(true);
        android.widget.Spinner roleSpinner = new android.widget.Spinner(activity);
        StaffRole[] roles = StaffRole.values();
        String[] roleLabels = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            roleLabels[i] = roles[i].getLabel();
        }
        roleSpinner.setAdapter(new android.widget.ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, roleLabels));
        if (existing != null) {
            name.setText(existing.getStaffName());
            // Never show stored hash/plaintext in the edit field — leave blank to keep.
            pin.setHint(getString(R.string.staff_pin_keep_hint));
            StaffRole current = StaffRole.fromId(existing.getStaffRole());
            for (int i = 0; i < roles.length; i++) {
                if (roles[i] == current) {
                    roleSpinner.setSelection(i);
                    break;
                }
            }
        }
        box.addView(name);
        box.addView(roleSpinner);
        box.addView(pin);
        new AlertDialog.Builder(activity)
                .setTitle(existing == null ? R.string.staff_add_member : R.string.staff_edit_member)
                .setView(box)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String n = name.getText() != null ? name.getText().toString().trim() : "";
                    if (n.isEmpty()) {
                        Toast.makeText(activity, R.string.staff_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int sel = roleSpinner.getSelectedItemPosition();
                    if (sel < 0 || sel >= roles.length) {
                        sel = 0;
                    }
                    String roleId = roles[sel].getId();
                    String pinVal = pin.getText() != null ? pin.getText().toString().trim() : "";
                    if (existing == null) {
                        db.addStaffUser(n, roleId, pinVal);
                        Toast.makeText(activity, R.string.staff_added, Toast.LENGTH_SHORT).show();
                    } else {
                        // Blank PIN on edit = keep existing hashed/plaintext PIN.
                        db.updateStaffUser(existing.getStaffId(), n, roleId,
                                pinVal.isEmpty() ? null : pinVal);
                        String activeId = SecurityPermissions.getActiveStaffId(activity);
                        if (existing.getStaffId() != null && existing.getStaffId().equals(activeId)) {
                            StaffUserResponse refreshed = db.getStaffUserById(existing.getStaffId());
                            if (refreshed != null) {
                                SecurityPermissions.activateStaff(activity, refreshed);
                            }
                        }
                        Toast.makeText(activity, R.string.staff_updated, Toast.LENGTH_SHORT).show();
                    }
                    refreshStaffRoleRow();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showStaffRolePicker() {
        if (activity == null) {
            return;
        }
        StaffRole[] roles = StaffRole.values();
        String[] labels = new String[roles.length];
        int checked = 0;
        StaffRole current = SecurityPermissions.getStaffRole(activity);
        for (int i = 0; i < roles.length; i++) {
            labels[i] = roles[i].getLabel();
            if (roles[i] == current) {
                checked = i;
            }
        }
        BottomSheetUi.showSingleChoice(activity, getString(R.string.staff_role_dialog_title), labels, checked, true,
                index -> {
                    SecurityPermissions.setStaffRole(activity, roles[index]);
                    refreshStaffRoleRow();
                    Toast.makeText(activity, R.string.staff_role_applied, Toast.LENGTH_SHORT).show();
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
                        FcmTokenManager.clearOnLogout(activity);
                        AuthTokens.clear(activity);
                        SecurityPermissions.clearStaffRoleOnLogout(activity);
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
        refreshBusinessTemplateRow();
        refreshStaffRoleRow();
        refreshScaleRow();
    }

    private void refreshBusinessTemplateRow() {
        if (binding == null || binding.businessTemplateLayout == null || activity == null) {
            return;
        }
        String subtitle = BusinessTemplateEngine.summaryLine(activity);
        setupRow(binding.businessTemplateLayout, R.drawable.ic_business, R.drawable.bg_quick_action_purple,
                R.color.deepPurple, getString(R.string.setting_business_template), subtitle);
    }

    private void showBusinessTemplateDialog() {
        if (activity == null) {
            return;
        }
        String message = BusinessTemplateEngine.summaryLine(activity)
                + "\n\n"
                + BusinessTemplateEngine.featureSummary(activity)
                + "\n\n"
                + getString(R.string.business_template_note);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.business_template_dialog_title)
                .setMessage(message)
                .setPositiveButton(R.string.business_template_change, (d, which) -> showBusinessTemplatePicker())
                .setNeutralButton(R.string.business_template_diagnostics, (d, which) -> showUniversalDiagnostics())
                .setNegativeButton(android.R.string.ok, null)
                .create();
        dialog.show();
        DialogUi.applyTabletWindow(dialog);
    }

    private void showUniversalDiagnostics() {
        if (activity == null) {
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.business_template_diagnostics)
                .setMessage(UniversalPosModules.fullDiagnostics(activity))
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.show();
        DialogUi.applyTabletWindow(dialog);
    }

    private void showBusinessTemplatePicker() {
        if (activity == null) {
            return;
        }
        List<BusinessTypeInfo> types = BusinessTemplateEngine.listSelectableTypes();
        String[] labels = new String[types.size()];
        for (int i = 0; i < types.size(); i++) {
            BusinessTypeInfo info = types.get(i);
            labels[i] = info.pickerLabel() + "\n" + info.getEngineNotes();
        }
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.business_template_pick_title)
                .setItems(labels, (d, which) -> {
                    BusinessTypeInfo selected = types.get(which);
                    if (BusinessTemplateEngine.applyBusinessType(activity, selected.getTypeId())) {
                        refreshBusinessTemplateRow();
                        Home.setValidationUI();
                        Toast.makeText(activity, R.string.business_template_applied, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
        DialogUi.applyTabletWindow(dialog);
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