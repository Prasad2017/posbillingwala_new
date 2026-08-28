package com.posbillingwala.owner.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.owner.Activity.Login;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.AuthTokens;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentUserSettingBinding;

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

        // Initialize Ads
        initAds();

        // Set up click listeners
        binding.backToHome.setOnClickListener(this);
        binding.appDevelopedBy.setOnClickListener(this);
        binding.categoryLayout.setOnClickListener(this);
        binding.subcategoryLayout.setOnClickListener(this);
        binding.productLayout.setOnClickListener(this);
        binding.productExportLayout.setOnClickListener(this);
        binding.invoiceDetailsLayout.setOnClickListener(this);
        binding.logoutLayout.setOnClickListener(this);
        binding.reportPinLayout.setOnClickListener(this);
        binding.reportLayout.setOnClickListener(this);
        binding.aboutLayout.setOnClickListener(this);
        binding.profileLayout.setOnClickListener(this);

        return view;
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
        if (id == R.id.backToHome) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new Home(), false);
        } else if (id == R.id.appDevelopedBy) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://thecanatech.com/"));
            startActivity(browserIntent);
        } else if (id == R.id.reportLayout) {
            openReportsHub();
        } else if (id == R.id.reportPinLayout) {
            setReportPassword();
        } else if (id == R.id.categoryLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AddCustomerProductCategory(), true);
        } else if (id == R.id.subcategoryLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AddCustomerSubcategory(), true);
        } else if (id == R.id.productLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
        } else if (id == R.id.productExportLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new ProductExport(), true);
        } else if (id == R.id.invoiceDetailsLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            OrderInvoice orderInvoice = new OrderInvoice();
            Bundle bundle = new Bundle();
            bundle.putString("pageName", "setting");
            orderInvoice.setArguments(bundle);
            ((MainActivity) activity).loadFragment(orderInvoice, true);
        } else if (id == R.id.aboutLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AboutUs(), true);
        } else if (id == R.id.profileLayout) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new UserProfile(), true);
        } else if (id == R.id.logoutLayout) {
            logout();
        }
    }

    public void openReportsHub() {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.report_password_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        TextView continueToReport = dialog.findViewById(R.id.continueToReport);
        TextView dismissReport = dialog.findViewById(R.id.dismissReport);
        TextInputEditText reportPin = dialog.findViewById(R.id.reportPin);

        dismissReport.setOnClickListener(v -> dialog.dismiss());
        continueToReport.setOnClickListener(v -> {
            String pin = MainActivity.reportPin != null ? MainActivity.reportPin : "9082";
            if (reportPin.getText().toString().equalsIgnoreCase(pin)) {
                dialog.dismiss();
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new ReportsHub(), true);
            } else {
                reportPin.requestFocus();
                reportPin.setError("Enter correct pin");
            }
        });
        dialog.show();
    }

    public void setReportPassword() {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.report_password_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextView continueToReport = dialog.findViewById(R.id.continueToReport);
        TextView dismissReport = dialog.findViewById(R.id.dismissReport);
        TextInputEditText reportPin = dialog.findViewById(R.id.reportPin);

        reportPin.setText(MainActivity.reportPin);

        dismissReport.setOnClickListener(v -> dialog.dismiss());

        continueToReport.setOnClickListener(v -> {
            if (!reportPin.getText().toString().isEmpty()) {
                dialog.dismiss();
                updateReportPin(reportPin.getText().toString());
            } else {
                reportPin.requestFocus();
                reportPin.setError("Enter report pin");
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
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
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Logout")
                .setMessage("Do you want to logout from application?")
                .setCancelable(false)
                .setPositiveButton("YES", (dialogInterface, i) -> {
                    dialogInterface.dismiss();

                    AuthTokens.clear(activity);

                    File file1 = new File("data/data/" + activity.getPackageName() + "/shared_prefs/user.xml");
                    if (file1.exists()) {
                        file1.delete();
                    }

                    Intent intent = new Intent(activity, Login.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    activity.finish();
                })
                .setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}
