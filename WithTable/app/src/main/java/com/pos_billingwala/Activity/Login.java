package com.pos_billingwala.Activity;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.pos_billingwala.Extra.AuthTokens;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.NetworkToOffline.CloudSyncNav;
import com.pos_billingwala.Extra.FcmTokenManager;
import com.pos_billingwala.Extra.LicenceExpiredUi;
import com.pos_billingwala.Extra.LicenceScopeGuard;
import com.pos_billingwala.Extra.LicenseModules;
import com.pos_billingwala.Extra.LicenseSession;
import com.pos_billingwala.Extra.Observability;
import com.pos_billingwala.Extra.TabletFormUi;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here
        setContentView(view); //view is set by view binding
        TabletFormUi.applyCenteredPanel(binding.loginLayout);
        File file = new File("data/data/" + getPackageName() + "/shared_prefs/" + Common.SHARED_PREF + ".xml");
        if (file.exists()) {
            Intent intent = new Intent(Login.this, MainActivity.class);
            CloudSyncNav.copyOpenFlag(getIntent(), intent);
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

        setupPrivacyPolicyLinks();

        binding.newUser.setText(Html.fromHtml(getString(R.string.new_user), Html.FROM_HTML_MODE_LEGACY));

        binding.loginCheck.setOnClickListener(this);
        binding.forgotLicenceKey.setOnClickListener(this);
        binding.newUser.setOnClickListener(this);

        initAds();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void setupPrivacyPolicyLinks() {
        String privacyPolicy = getString(R.string.login_security_notice);
        String termsLabel = getString(R.string.terms_of_service);
        String privacyLabel = getString(R.string.privacy_policy_label);
        SpannableString spannableString = new SpannableString(privacyPolicy);

        int termsStart = privacyPolicy.indexOf(termsLabel);
        if (termsStart >= 0) {
            spannableString.setSpan(createPolicyLinkSpan(), termsStart,
                    termsStart + termsLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int privacyStart = privacyPolicy.indexOf(privacyLabel);
        if (privacyStart >= 0) {
            spannableString.setSpan(createPolicyLinkSpan(), privacyStart,
                    privacyStart + privacyLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        binding.privacyPolicy.setText(spannableString);
        binding.privacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private ClickableSpan createPolicyLinkSpan() {
        return new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                openWebPage("https://posbillingwala.com/PlayStore/privacy_policy.html");
            }

            @Override
            public void updateDrawState(@NonNull TextPaint textPaint) {
                textPaint.setColor(ContextCompat.getColor(Login.this, R.color.colorPrimary));
                textPaint.setUnderlineText(true);
            }
        };
    }

    private void openWebPage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            intent.setPackage(null);
            startActivity(intent);
        }
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
                        View content = LayoutInflater.from(Login.this).inflate(R.layout.login_device_dialog, null);
                        BottomSheetDialog sheet = BottomSheetUi.showContent(Login.this, content, false);
                        TextView message = content.findViewById(R.id.message);
                        TextView txtYes = content.findViewById(R.id.yes);
                        TextView txtNo = content.findViewById(R.id.no);

                        message.setText(getString(R.string.licence_msg_device_already_registered));

                        txtYes.setOnClickListener(v -> {
                            sheet.dismiss();
                            updateLicenceKey();
                        });

                        txtNo.setOnClickListener(v -> sheet.dismiss());

                    } else if (response.body().getStatus().equalsIgnoreCase("2")) {
                        pDialog.dismiss();
                        updateLicenceKey();
                    }
                } else {
                    pDialog.dismiss();
                    Observability.log("login_check HTTP " + response.code() + " unsuccessful");
                    SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.ERROR_TYPE);
                    sweetAlertDialog.setTitleText(getString(R.string.oops_title));
                    sweetAlertDialog.setContentText(getString(R.string.something_went_wrong)
                            + "\nHTTP " + response.code());
                    sweetAlertDialog.setCancelClickListener(SweetAlertDialog::dismiss).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                String detail = Observability.logCallbackFailure(t, "login_check");
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText(getString(R.string.oops_title));
                sweetAlertDialog.setContentText(getString(R.string.something_went_wrong) + "\n" + detail);
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
                } else {
                    pDialog.dismiss();
                    Observability.log("update_licence_key HTTP " + response.code() + " unsuccessful");
                    SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.ERROR_TYPE);
                    sweetAlertDialog.setTitleText(getString(R.string.oops_title));
                    sweetAlertDialog.setContentText(getString(R.string.something_went_wrong)
                            + "\nHTTP " + response.code());
                    sweetAlertDialog.setCancelClickListener(SweetAlertDialog::dismiss).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                String detail = Observability.logCallbackFailure(t, "update_licence_key");
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText(getString(R.string.oops_title));
                sweetAlertDialog.setContentText(getString(R.string.something_went_wrong) + "\n" + detail);
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
                                LicenseModules.saveModuleFlags(Login.this,
                                        response.body().getFastBilling(),
                                        response.body().getTakeAway(),
                                        response.body().getDineIn(),
                                        response.body().getMess(),
                                        response.body().getTotalSaleData(),
                                        response.body().getTodaySaleData());
                                Common.saveUserData(Login.this, "LicenceKey", response.body().getLicenceKey());
                                Common.saveUserData(Login.this, "appPin", response.body().getMpin());
                                Common.saveUserData(Login.this, "LicenceKeyRegDate", response.body().getLicenceKeyRegDate());
                                Common.saveUserData(Login.this, "LicenceKeyExpireDate", response.body().getLicenceKeyExpireDate());
                                Common.saveUserData(Login.this, "reportPin", response.body().getReportPin());
                                LicenseSession.saveFromLogin(Login.this, response.body());
                                AuthTokens.saveFromLogin(Login.this, response.body());
                                LicenceScopeGuard.onLoginSuccess(Login.this, response.body());
                                FcmTokenManager.registerIfLoggedIn(Login.this);
                                Observability.setUserContext(
                                        response.body().getLicenceId(),
                                        response.body().getLicenceKey());

                                Intent intent = new Intent(Login.this, MainActivity.class);
                                CloudSyncNav.copyOpenFlag(getIntent(), intent);
                                startActivity(intent);
                                finishAffinity();

                            } else {
                                LicenceExpiredUi.show(Login.this);
                            }

                        } catch (ParseException e) {
                            Observability.logNonFatal(e, "check_licence_expire_date_parse");
                            pDialog.dismiss();
                            SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.ERROR_TYPE);
                            sweetAlertDialog.setTitleText(getString(R.string.oops_title));
                            sweetAlertDialog.setContentText(getString(R.string.something_went_wrong)
                                    + "\nInvalid licence expiry date from server");
                            sweetAlertDialog.setCancelClickListener(SweetAlertDialog::dismiss).show();
                        }
                    } else {
                        pDialog.dismiss();
                        LicenceExpiredUi.showForServerMessage(Login.this, response.body().getMessage());
                    }
                } else {
                    pDialog.dismiss();
                    Observability.log("check_licence_expire HTTP " + response.code()
                            + " — body empty or unsuccessful");
                    SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.ERROR_TYPE);
                    sweetAlertDialog.setTitleText(getString(R.string.oops_title));
                    sweetAlertDialog.setContentText(getString(R.string.something_went_wrong)
                            + "\nHTTP " + response.code());
                    sweetAlertDialog.setCancelClickListener(SweetAlertDialog::dismiss).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                pDialog.dismiss();
                String detail = Observability.logCallbackFailure(t, "check_licence_expire");
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText(getString(R.string.oops_title));
                sweetAlertDialog.setContentText(getString(R.string.something_went_wrong) + "\n" + detail);
                sweetAlertDialog.setCancelClickListener(SweetAlertDialog::dismiss).show();
            }

        });

    }

}