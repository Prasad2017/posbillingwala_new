package com.pos_billingwala.Activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.pos_billingwala.Extra.AppLanguage;
import com.pos_billingwala.Extra.AuthTokens;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.LicenceExpiredUi;
import com.pos_billingwala.Extra.LicenseSession;
import com.pos_billingwala.Model.LoginResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;
import com.pos_billingwala.databinding.ActivityLoginBinding;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("NonConstantResourceId, HardwareIds")
public class Login extends BaseActivity implements View.OnClickListener {

    //AdView
    public AdView adView;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    String m_androidId, manufacturerModel;
    SweetAlertDialog pDialog;
    ActivityLoginBinding binding;

    private final ActivityResultLauncher<Intent> registerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                String licenceKey = result.getData().getStringExtra(Register.EXTRA_LICENCE_KEY);
                if (licenceKey == null || licenceKey.isEmpty()) {
                    return;
                }
                binding.licenceKey.setText(licenceKey);
                if (result.getData().getBooleanExtra(Register.EXTRA_AUTO_LOGIN, false)) {
                    loginCheck();
                }
            });

    public static long getUnitBetweenDates(Date startDate, Date endDate, TimeUnit unit) {
        long timeDiff = endDate.getTime() - startDate.getTime();
        return unit.convert(timeDiff, TimeUnit.MILLISECONDS);
    }

    public void setScreenSizeSmall() {
        Configuration configuration = getResources().getConfiguration();
        configuration.fontScale = (float) 1; //0.85 small size, 1 normal size, 1,15 big etc
        AppLanguage.preserveLocaleOnConfig(this, configuration);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        configuration.densityDpi = (int) getResources().getDisplayMetrics().xdpi;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here
        setContentView(view); //view is set by view binding
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setScreenSizeSmall();

        File file = new File("data/data/" + getPackageName() + "/shared_prefs/" + Common.SHARED_PREF + ".xml");
        if (file.exists()) {
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        if (getIntent().getBooleanExtra(LicenceExpiredUi.EXTRA_SHOW_LICENCE_EXPIRED, false)) {
            binding.getRoot().post(() -> LicenceExpiredUi.show(Login.this));
        }

        binding.licenceKey.setSelection(binding.licenceKey.getText().toString().length());

        m_androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        manufacturerModel = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;

        String privacyPolicy = "By providing licence key, I hereby agree and accept the Terms of service and Privacy Policy in use of the POS Billingwala app.";
        SpannableString spannableString = new SpannableString(privacyPolicy);

        // creating clickable span to be implemented as a link
        ClickableSpan clickableSpan1 = new ClickableSpan() {
            public void onClick(@NonNull View widget) {

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.posbillingwala.com/PlayStore/privacy_policy.html"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    // Chrome browser presumably not installed so allow user to choose instead
                    intent.setPackage(null);
                    startActivity(intent);
                }

            }

            @Override
            public void updateDrawState(final TextPaint textPaint) {
                textPaint.setColor(getResources().getColor(R.color.colorPrimaryDark));
            }

        };

        // creating clickable span to be implemented as a link
        ClickableSpan clickableSpan2 = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.posbillingwala.com/PlayStore/privacy_policy.html"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                } catch (Exception ex) {
                    // Chrome browser presumably not installed so allow user to choose instead
                    intent.setPackage(null);
                    startActivity(intent);
                }

            }

            @Override
            public void updateDrawState(final TextPaint textPaint) {
                textPaint.setColor(getResources().getColor(R.color.colorPrimaryDark));
            }

        };

        // setting the part of string to be act as a link
        spannableString.setSpan(clickableSpan1, 56, 72, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(clickableSpan2, 77, 91, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        binding.privacyPolicy.setText(spannableString);
        binding.privacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());

        binding.newUser.setText(Html.fromHtml(getString(R.string.new_user), Html.FROM_HTML_MODE_LEGACY));

        binding.loginCheck.setOnClickListener(this);
        binding.forgotLicenceKey.setOnClickListener(this);
        binding.newUser.setOnClickListener(this);

        initAds();

    }

    public void initAds() {

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(Login.this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                // on below line displaying a log that admob ads has been initialized.
                Log.i("Admob", "Admob Initialized." + initializationStatus);
            }
        });

        adView = findViewById(R.id.ad_view);
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
        if (id == R.id.loginCheck) {
            if (!binding.licenceKey.getText().toString().trim().isEmpty()) {
                loginCheck();
            } else {
                Toast.makeText(Login.this, R.string.enter_valid_licence_key, Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.forgotLicenceKey) {
            LicenceExpiredUi.showInfoDialog(
                    Login.this,
                    getString(R.string.forgot_licence_key_message, getString(R.string.support_phone_display)));
        } else if (id == R.id.newUser) {
            signUp();
        }
    }

    public void signUp() {
        registerLauncher.launch(new Intent(Login.this, Register.class));
    }

    public void loginCheck() {

        pDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.loading));
        pDialog.setCancelable(false);
        pDialog.show();

        Call<LoginResponse> call = Api.getClient(this).loginCheck(binding.licenceKey.getText().toString().trim(), m_androidId);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        checkLicenceExpire(response.body().getLicenceId());
                    } else if (response.body().getStatus().equalsIgnoreCase("0")) {
                        pDialog.dismiss();
                        LicenceExpiredUi.showForServerMessage(Login.this, response.body().getMessage());
                    } else if (response.body().getStatus().equalsIgnoreCase("3")) {
                        pDialog.dismiss();
                        final Dialog dialog = new Dialog(Login.this);
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
                        dialog.setContentView(R.layout.login_device_dialog);
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                        dialog.setCancelable(false);
                        TextView message = dialog.findViewById(R.id.message);
                        TextView txtYes = dialog.findViewById(R.id.yes);
                        TextView txtNo = dialog.findViewById(R.id.no);

                        message.setText(getString(R.string.licence_msg_device_already_registered));

                        txtYes.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                                updateLicenceKey();
                            }
                        });

                        txtNo.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });

                        dialog.show();

                    } else if (response.body().getStatus().equalsIgnoreCase("2")) {
                        pDialog.dismiss();
                        updateLicenceKey();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText(getString(R.string.oops_title));
                sweetAlertDialog.setContentText(getString(R.string.something_went_wrong));
                sweetAlertDialog.setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismiss();
                    }
                }).show();
            }
        });

    }

    public void updateLicenceKey() {

        pDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.loading));
        pDialog.setCancelable(false);
        pDialog.show();

        Call<LoginResponse> call = Api.getClient(this).updateLicenceKey(binding.licenceKey.getText().toString().trim(), m_androidId, manufacturerModel);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        pDialog.dismiss();
                        loginCheck();
                    } else {
                        pDialog.dismiss();
                        LicenceExpiredUi.showForServerMessage(Login.this, response.body().getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText(getString(R.string.oops_title));
                sweetAlertDialog.setContentText(getString(R.string.something_went_wrong));
                sweetAlertDialog.setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismiss();
                    }
                }).show();
            }
        });

    }

    public void checkLicenceExpire(String userId) {

        Call<LoginResponse> call = Api.getClient(this).checkLicenceExpire(userId, m_androidId, manufacturerModel);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {

                        pDialog.dismiss();

                        pref = getSharedPreferences("user", Context.MODE_PRIVATE);
                        editor = pref.edit();
                        editor.putString("UserLogin", "UserLoginSuccessful");
                        editor.commit();

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

                                Toast.makeText(Login.this, R.string.welcome_pos_billingwala, Toast.LENGTH_SHORT).show();

                                Common.saveUserData(Login.this, "firstLogin", "firstLogin");

                                Common.saveUserData(Login.this, "userId", response.body().getLicenceId());
                                Common.saveUserData(Login.this, "ownerId", response.body().getOwnerId());
                                Common.saveUserData(Login.this, "userName", response.body().getUserName());
                                Common.saveUserData(Login.this, "shopName", response.body().getShopName());
                                Common.saveUserData(Login.this, "shopImage", response.body().getShopImage());
                                Common.saveUserData(Login.this, "fastBilling", response.body().getFastBilling());
                                Common.saveUserData(Login.this, "takeAway", response.body().getTakeAway());
                                Common.saveUserData(Login.this, "dineIn", response.body().getDineIn());
                                Common.saveUserData(Login.this, "mess", response.body().getMess());
                                Common.saveUserData(Login.this, "LicenceKey", response.body().getLicenceKey());
                                Common.saveUserData(Login.this, "appPin", response.body().getMpin());
                                Common.saveUserData(Login.this, "LicenceKeyRegDate", response.body().getLicenceKeyRegDate());
                                Common.saveUserData(Login.this, "LicenceKeyExpireDate", response.body().getLicenceKeyExpireDate());
                                Common.saveUserData(Login.this, "reportPin", response.body().getReportPin());
                                Common.saveUserData(Login.this, "totalSaleData", response.body().getTotalSaleData());
                                Common.saveUserData(Login.this, "todaySaleData", response.body().getTodaySaleData());
                                LicenseSession.saveFromLogin(Login.this, response.body());
                                AuthTokens.saveFromLogin(Login.this, response.body());

                                Intent intent = new Intent(Login.this, MainActivity.class);
                                startActivity(intent);
                                finishAffinity();

                            } else {
                                LicenceExpiredUi.show(Login.this);
                            }

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else {
                        pDialog.dismiss();
                        LicenceExpiredUi.showForServerMessage(Login.this, response.body().getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e("serverError", t.getMessage());
                pDialog.dismiss();
            }

        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

}