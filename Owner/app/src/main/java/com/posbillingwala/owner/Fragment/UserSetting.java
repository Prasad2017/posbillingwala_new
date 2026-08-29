package com.posbillingwala.owner.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.owner.Activity.Login;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.AuthTokens;
import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentUserSettingBinding;
import com.posbillingwala.owner.databinding.ItemGroupedMenuRowBinding;

import java.io.File;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserSetting extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public FragmentUserSettingBinding binding;
    public AdView adView;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserSettingBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        activity = getActivity();

        // Set up key listener for back navigation
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("tag", "onKey Back listener is working!!!");
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new Home(), false);
                return true;
            }
            return false;
        });

        // Set app version
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

        setupRow(binding.profileLayout, R.drawable.ic_person, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.setting_profile), getString(R.string.setting_hint_profile));
        setupRow(binding.reportPinLayout, R.drawable.ic_lock, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.setting_report_pin), getString(R.string.setting_hint_report_pin));
        setupRow(binding.categoryLayout, R.drawable.ic_category, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.setting_category), getString(R.string.setting_hint_category));
        setupRow(binding.subcategoryLayout, R.drawable.ic_folder, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.setting_subcategory), getString(R.string.setting_hint_subcategory));
        setupRow(binding.productLayout, R.drawable.ic_report_product, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.setting_product), getString(R.string.setting_hint_product));
        setupRow(binding.productExportLayout, R.drawable.ic_cloud_download, R.drawable.bg_quick_action_purple,
                R.color.deepPurple, getString(R.string.setting_product_export), getString(R.string.setting_hint_export));
        setupRow(binding.reportLayout, R.drawable.ic_report_sales, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.setting_reports), getString(R.string.setting_hint_reports));
        setupRow(binding.invoiceDetailsLayout, R.drawable.ic_report_invoice, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.setting_invoice_details), getString(R.string.setting_hint_invoice));
        setupRow(binding.aboutLayout, R.drawable.ic_info, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.setting_about), getString(R.string.setting_hint_about));
        setupRow(binding.logoutLayout, R.drawable.ic_logout, R.drawable.bg_quick_action_red,
                R.color.statusExpired, getString(R.string.setting_logout), getString(R.string.setting_hint_logout));
        binding.logoutLayout.menuTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.statusExpired));

        showGroupDividers(binding.profileLayout, binding.reportPinLayout);
        showGroupDividers(binding.categoryLayout, binding.subcategoryLayout, binding.productLayout, binding.productExportLayout);
        showGroupDividers(binding.reportLayout, binding.invoiceDetailsLayout);
        showGroupDividers(binding.aboutLayout, binding.logoutLayout);

        binding.profileLayout.getRoot().setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new UserProfile(), true);
        });
        binding.reportPinLayout.getRoot().setOnClickListener(v -> setReportPassword());
        binding.categoryLayout.getRoot().setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AddCustomerProductCategory(), true);
        });
        binding.subcategoryLayout.getRoot().setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AddCustomerSubcategory(), true);
        });
        binding.productLayout.getRoot().setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
        });
        binding.productExportLayout.getRoot().setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new ProductExport(), true);
        });
        binding.reportLayout.getRoot().setOnClickListener(v -> openReportsHub());
        binding.invoiceDetailsLayout.getRoot().setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            OrderInvoice orderInvoice = new OrderInvoice();
            Bundle bundle = new Bundle();
            bundle.putString("pageName", "setting");
            orderInvoice.setArguments(bundle);
            ((MainActivity) activity).loadFragment(orderInvoice, true);
        });
        binding.aboutLayout.getRoot().setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AboutUs(), true);
        });
        binding.logoutLayout.getRoot().setOnClickListener(v -> logout());
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
        MobileAds.initialize(activity, initializationStatus -> Log.i("Admob", "Admob Initialized." + initializationStatus));

        adView = binding.adView;
        AdRequest adRequest = new AdRequest.Builder().build();
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
                Log.e("loadAdError", "" + loadAdError);
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
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new Home(), false);
        } else if (id == R.id.appDevelopedBy) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://thecanatech.com/"));
            startActivity(browserIntent);
        }
    }

    public void openReportsHub() {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.report_password_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, dialogView, false);
        if (sheet == null) {
            return;
        }

        TextView continueToReport = dialogView.findViewById(R.id.continueToReport);
        TextView dismissReport = dialogView.findViewById(R.id.dismissReport);
        TextInputEditText reportPin = dialogView.findViewById(R.id.reportPin);

        dismissReport.setOnClickListener(v -> sheet.dismiss());
        continueToReport.setOnClickListener(v -> {
            String pin = MainActivity.reportPin != null ? MainActivity.reportPin : "9082";
            if (reportPin.getText().toString().equalsIgnoreCase(pin)) {
                sheet.dismiss();
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new ReportsHub(), true);
            } else {
                reportPin.requestFocus();
                reportPin.setError("Enter correct pin");
            }
        });
    }

    public void setReportPassword() {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.report_password_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, dialogView, false);
        if (sheet == null) {
            return;
        }

        TextView continueToReport = dialogView.findViewById(R.id.continueToReport);
        TextView dismissReport = dialogView.findViewById(R.id.dismissReport);
        TextInputEditText reportPin = dialogView.findViewById(R.id.reportPin);

        reportPin.setText(MainActivity.reportPin);

        dismissReport.setOnClickListener(v -> sheet.dismiss());

        continueToReport.setOnClickListener(v -> {
            if (!reportPin.getText().toString().isEmpty()) {
                sheet.dismiss();
                updateReportPin(reportPin.getText().toString());
            } else {
                reportPin.requestFocus();
                reportPin.setError("Enter report pin");
            }
        });
    }

    public void updateReportPin(String reportPin) {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().updateReportPin(MainActivity.userId, reportPin);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful()) {
                    if ("1".equalsIgnoreCase(response.body().getStatus())) {
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText("Oops...");
                sweetAlertDialog.setContentText("Something went wrong!");
                sweetAlertDialog.setCancelClickListener(SweetAlertDialog::dismiss).show();
            }
        });
    }

    public void logout() {
        BottomSheetUi.showConfirm(activity, "Logout", "Do you want to logout from application?",
                "YES", "NO", false, () -> {
                    AuthTokens.clear(activity);

                    File file1 = new File("data/data/" + activity.getPackageName() + "/shared_prefs/user.xml");
                    if (file1.exists()) {
                        file1.delete();
                    }

                    Intent intent = new Intent(activity, Login.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    activity.finish();
                });
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}
