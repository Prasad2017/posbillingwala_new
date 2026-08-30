package com.posbillingwala.owner.Activity;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.posbillingwala.owner.Extra.AuthTokens;
import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.Extra.Common;
import com.posbillingwala.owner.Model.LoginResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.ActivityLoginBinding;

import java.io.File;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Login extends AppCompatActivity implements View.OnClickListener {

    public ActivityLoginBinding binding;
    public SharedPreferences pref;
    public SharedPreferences.Editor editor;
    public SweetAlertDialog pDialog;
    public AdView adView;

    public static long getUnitBetweenDates(Date startDate, Date endDate, TimeUnit unit) {
        long timeDiff = endDate.getTime() - startDate.getTime();
        return unit.convert(timeDiff, TimeUnit.MILLISECONDS);
    }

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Api.bindContext(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        File file = new File("data/data/" + getPackageName() + "/shared_prefs/user.xml");
        if (file.exists() && AuthTokens.hasValidSession(this)) {
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (file.exists() && !AuthTokens.hasValidSession(this)) {
            // Prefs exist but token expired — stay on login to get a fresh token once
            AuthTokens.clear(this);
        }

        if (binding.mobileNumber.getText() != null) {
            binding.mobileNumber.setSelection(binding.mobileNumber.getText().toString().length());
        }

        String privacyPolicy = "By providing licence key, I hereby agree and accept the Terms of service and Privacy Policy in use of the POS Billingwala app.";
        SpannableString spannableString = new SpannableString(privacyPolicy);

        ClickableSpan clickableSpan1 = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://posbillingwala.com/PlayStore/privacy_policy.html"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    intent.setPackage(null);
                    startActivity(intent);
                }
            }

            @Override
            public void updateDrawState(TextPaint textPaint) {
                textPaint.setColor(getResources().getColor(R.color.colorPrimaryDark));
            }
        };

        ClickableSpan clickableSpan2 = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://posbillingwala.com/PlayStore/privacy_policy.html"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    intent.setPackage(null);
                    startActivity(intent);
                }
            }

            @Override
            public void updateDrawState(TextPaint textPaint) {
                textPaint.setColor(getResources().getColor(R.color.colorPrimaryDark));
            }
        };

        spannableString.setSpan(clickableSpan1, 56, 72, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(clickableSpan2, 77, 91, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        binding.privacyPolicy.setText(spannableString);
        binding.privacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());

        initAds();

        binding.loginCheck.setOnClickListener(this);
        binding.forgotmobileNumber.setOnClickListener(this);
        binding.newUser.setOnClickListener(this);

    }

    public void initAds() {
        MobileAds.initialize(Login.this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                Log.i("Admob", "Admob Initialized." + initializationStatus);
            }
        });

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
        if (id == R.id.loginCheck) {
            if (!binding.mobileNumber.getText().toString().trim().isEmpty()) {
                if (binding.mobileNumber.getText().toString().length() == 10) {
                    loginCheck();
                } else {
                    Toast.makeText(Login.this, "Enter valid 10 digit mobile number", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(Login.this, "Enter valid mobile number", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.forgotmobileNumber) {
            Snackbar snackbar = Snackbar.make(binding.loginLayout, "Please contact our dealer or customer care", Snackbar.LENGTH_LONG);
            snackbar.show();
        } else if (id == R.id.newUser) {
            signUp();
        }
    }

    public void signUp() {
        View dialogView = LayoutInflater.from(Login.this).inflate(R.layout.new_user_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(Login.this, dialogView, true);
        if (sheet == null) {
            return;
        }

        TextView contactUser = dialogView.findViewById(R.id.contactUser);
        TextView supportMessage = dialogView.findViewById(R.id.supportMessage);
        if (supportMessage != null) {
            supportMessage.setText(getString(R.string.new_user_support_team, getString(R.string.support_phone_display)));
        }
        contactUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheet.dismiss();
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + getString(R.string.support_phone_dial)));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }

    public void loginCheck() {
        pDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<LoginResponse> call = Api.getClient().loginCheck(binding.mobileNumber.getText().toString().trim(), "");
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        pref = getSharedPreferences("user", Context.MODE_PRIVATE);
                        editor = pref.edit();
                        editor.putString("UserLogin", "UserLoginSuccessful");
                        editor.commit();

                        Toast.makeText(Login.this, "Welcome to POS Billingwala", Toast.LENGTH_SHORT).show();

                        Common.saveUserData(Login.this, "userId", "" + response.body().getUserId());
                        Common.saveUserData(Login.this, "reportPin", "" + response.body().getReportPin());
                        AuthTokens.saveFromLogin(Login.this, response.body());
                        if (response.body().getAuthToken() == null || response.body().getAuthToken().trim().isEmpty()) {
                            Snackbar.make(binding.loginLayout,
                                    "Login token missing. Update Owner APIs on the server, then login again.",
                                    Snackbar.LENGTH_LONG).show();
                        }

                        Intent intent = new Intent(Login.this, MainActivity.class);
                        startActivity(intent);
                        finishAffinity();
                    } else if (response.body().getStatus().equalsIgnoreCase("0")) {
                        Snackbar snackbar = Snackbar.make(binding.loginLayout, "" + response.body().getMessage(), Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.ERROR_TYPE);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adView != null) {
            adView.destroy();
        }
    }
}
