package com.pos_billingwala.Activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.pos_billingwala.Extra.AuthTokens;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.LicenceExpiredUi;
import com.pos_billingwala.Extra.LicenceScopeGuard;
import com.pos_billingwala.Extra.LicenseModules;
import com.pos_billingwala.Extra.LicenseSession;
import com.pos_billingwala.Extra.Observability;
import com.pos_billingwala.Model.LoginResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;
import com.pos_billingwala.databinding.ActivityLoginMpinBinding;

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


@SuppressLint("HardwareIds")
public class LoginMPin extends BaseActivity implements View.OnClickListener {

    //AdView
    public AdView adView;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    SweetAlertDialog pDialog;
    String m_androidId, manufacturerModel;
    ActivityLoginMpinBinding binding;


    public static long getUnitBetweenDates(Date startDate, Date endDate, TimeUnit unit) {
        long timeDiff = endDate.getTime() - startDate.getTime();
        return unit.convert(timeDiff, TimeUnit.MILLISECONDS);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginMpinBinding.inflate(getLayoutInflater());
        View view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here
        setContentView(view); //view is set by view binding
        File file = new File("data/data/" + getPackageName() + "/shared_prefs/" + Common.SHARED_PREF + ".xml");
        if (file.exists()) {
            Intent intent = new Intent(LoginMPin.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        m_androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        manufacturerModel = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;

        binding.otp1.setSelection(binding.otp1.getText().toString().length());
        binding.otp2.setSelection(binding.otp2.getText().toString().length());
        binding.otp3.setSelection(binding.otp3.getText().toString().length());
        binding.otp4.setSelection(binding.otp4.getText().toString().length());

        String privacyPolicy = "By providing licence key, I hereby agree and accept the Terms of service and Privacy Policy in use of the POS Billingwala app.";
        SpannableString spannableString = new SpannableString(privacyPolicy);

        // creating clickable span to be implemented as a link
        ClickableSpan clickableSpan1 = new ClickableSpan() {
            public void onClick(@NonNull View widget) {

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.posbillingwala.com/PlayStore/privacy_policy.html"));
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

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.posbillingwala.com/PlayStore/privacy_policy.html"));
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

        initAds();

        binding.otp1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 1) {
                    binding.otp2.requestFocus();
                    binding.otp1.setBackgroundDrawable(getDrawable(R.drawable.fill_button_rounded_border));
                } else {
                    binding.otp1.requestFocus();
                    binding.otp1.setBackgroundDrawable(getDrawable(R.drawable.unfill_button_rounded_border));
                }
            }
        });
        binding.otp2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 1) {
                    binding.otp3.requestFocus();
                    binding.otp2.setBackgroundDrawable(getDrawable(R.drawable.fill_button_rounded_border));
                } else {
                    binding.otp1.requestFocus();
                    binding.otp2.setBackgroundDrawable(getDrawable(R.drawable.unfill_button_rounded_border));
                }
            }
        });
        binding.otp3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 1) {
                    binding.otp4.requestFocus();
                    binding.otp3.setBackgroundDrawable(getDrawable(R.drawable.fill_button_rounded_border));
                } else {
                    binding.otp2.requestFocus();
                    binding.otp3.setBackgroundDrawable(getDrawable(R.drawable.unfill_button_rounded_border));
                }
            }
        });
        binding.otp4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 1) {

                    binding.otp4.requestFocus();
                    binding.otp4.setBackgroundDrawable(getDrawable(R.drawable.fill_button_rounded_border));

                } else {
                    binding.otp3.requestFocus();
                    binding.otp4.setBackgroundDrawable(getDrawable(R.drawable.unfill_button_rounded_border));
                }

                hideKeyboard(binding.otp4);

            }
        });

        binding.backToPage.setOnClickListener(this);
        binding.loginMpin.setOnClickListener(this);

    }

    protected void hideKeyboard(View view) {
        InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        in.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void initAds() {

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(LoginMPin.this, new OnInitializationCompleteListener() {
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
        if (view.getId() == R.id.backToPage) {
            finish();
        } else if (view.getId() == R.id.loginMpin) {
            loginMpin();
        }
    }

    public void loginMpin() {

        pDialog = new SweetAlertDialog(LoginMPin.this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.loading));
        pDialog.setCancelable(false);
        pDialog.show();

        String enteredMpin = binding.otp1.getText().toString() + binding.otp2.getText().toString() + binding.otp3.getText().toString() + binding.otp4.getText().toString();
        String licenceKey = Common.getSavedUserData(LoginMPin.this, "LicenceKey");

        Call<LoginResponse> call = Api.getClient(this).loginMpin(enteredMpin, licenceKey, m_androidId, manufacturerModel);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
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

                                Toast.makeText(LoginMPin.this, R.string.welcome_pos_billingwala, Toast.LENGTH_SHORT).show();

                                Common.saveUserData(LoginMPin.this, "firstLogin", "firstLogin");

                                Common.saveUserData(LoginMPin.this, "userId", response.body().getLicenceId());
                                Common.saveUserData(LoginMPin.this, "ownerId", response.body().getOwnerId());
                                Common.saveUserData(LoginMPin.this, "userName", response.body().getUserName());
                                Common.saveUserData(LoginMPin.this, "shopName", response.body().getShopName());
                                Common.saveUserData(LoginMPin.this, "shopImage", response.body().getShopImage());
                                LicenseModules.saveModuleFlags(LoginMPin.this,
                                        response.body().getFastBilling(),
                                        response.body().getTakeAway(),
                                        response.body().getDineIn(),
                                        response.body().getMess(),
                                        response.body().getTotalSaleData(),
                                        response.body().getTodaySaleData());
                                Common.saveUserData(LoginMPin.this, "LicenceKey", response.body().getLicenceKey());
                                Common.saveUserData(LoginMPin.this, "appPin", response.body().getMpin());
                                Common.saveUserData(LoginMPin.this, "LicenceKeyRegDate", response.body().getLicenceKeyRegDate());
                                Common.saveUserData(LoginMPin.this, "LicenceKeyExpireDate", response.body().getLicenceKeyExpireDate());
                                Common.saveUserData(LoginMPin.this, "reportPin", response.body().getReportPin());
                                LicenseSession.saveFromLogin(LoginMPin.this, response.body());
                                AuthTokens.saveFromLogin(LoginMPin.this, response.body());
                                LicenceScopeGuard.onLoginSuccess(LoginMPin.this, response.body());

                                Intent intent = new Intent(LoginMPin.this, MainActivity.class);
                                startActivity(intent);
                                finishAffinity();

                            } else {
                                LicenceExpiredUi.show(LoginMPin.this);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else if (response.body().getStatus().equalsIgnoreCase("0")) {
                        pDialog.dismiss();
                        LicenceExpiredUi.showForServerMessage(LoginMPin.this, response.body().getMessage());
                    } else if (response.body().getStatus().equalsIgnoreCase("3")) {
                        pDialog.dismiss();
                        final Dialog dialog = new Dialog(LoginMPin.this);
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
                        LicenceExpiredUi.showForServerMessage(LoginMPin.this, response.body().getMessage());
                    } else if (response.body().getStatus().equalsIgnoreCase("4")) {
                        pDialog.dismiss();
                        LicenceExpiredUi.showForServerMessage(LoginMPin.this, response.body().getMessage());
                    }
                    pDialog.dismiss();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                String detail = Observability.logCallbackFailure(t, "login_mpin");
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(LoginMPin.this, SweetAlertDialog.ERROR_TYPE);
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

    public void updateMpin() {

        pDialog = new SweetAlertDialog(LoginMPin.this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.loading));
        pDialog.setCancelable(false);
        pDialog.show();

        String enteredMpin = binding.otp1.getText().toString() + binding.otp2.getText().toString() + binding.otp3.getText().toString() + binding.otp4.getText().toString();
        String licenceKey = Common.getSavedUserData(LoginMPin.this, "LicenceKey");


        Call<LoginResponse> call = Api.getClient(this).updateMpin(enteredMpin, licenceKey, m_androidId, manufacturerModel);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        pDialog.dismiss();
                        loginMpin();
                    } else {
                        pDialog.dismiss();
                        LicenceExpiredUi.showForServerMessage(LoginMPin.this, response.body().getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                String detail = Observability.logCallbackFailure(t, "update_mpin");
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(LoginMPin.this, SweetAlertDialog.ERROR_TYPE);
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

        pDialog = new SweetAlertDialog(LoginMPin.this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.loading));
        pDialog.setCancelable(false);
        pDialog.show();

        String licenceKey = Common.getSavedUserData(LoginMPin.this, "LicenceKey");

        Call<LoginResponse> call = Api.getClient(this).updateLicenceKey(licenceKey, m_androidId, manufacturerModel);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        pDialog.dismiss();
                        updateMpin();
                    } else {
                        pDialog.dismiss();
                        LicenceExpiredUi.showForServerMessage(LoginMPin.this, response.body().getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                String detail = Observability.logCallbackFailure(t, "update_licence_key_mpin");
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(LoginMPin.this, SweetAlertDialog.ERROR_TYPE);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}