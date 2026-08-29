package com.posbillingwala.dealer.Activity;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;

import com.posbillingwala.dealer.Extra.BottomSheetUi;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.posbillingwala.dealer.AppUpdate.UpdateManager;
import com.posbillingwala.dealer.AppUpdate.UpdateManagerConstant;
import com.posbillingwala.dealer.Extra.AuthTokens;
import com.posbillingwala.dealer.Extra.Common;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.databinding.ActivityLoginBinding;

import java.io.File;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, HardwareIds")
public class Login extends AppCompatActivity implements View.OnClickListener {

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    String m_androidId, manufacturerModel;
    // Declare the UpdateManager
    UpdateManager mUpdateManager;
    ActivityLoginBinding binding;
    //AdView
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Api.bindContext(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        File file = new File("data/data/" + getPackageName() + "/shared_prefs/user.xml");
        if (file.exists() && AuthTokens.hasValidSession(this)) {
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (file.exists() && !AuthTokens.hasValidSession(this)) {
            AuthTokens.clear(this);
        }

        binding.mobileNumber.setSelection(binding.mobileNumber.getText().toString().length());
        binding.password.setSelection(binding.password.getText().toString().length());

        m_androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        manufacturerModel = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;

        String privacyPolicy = "By providing mobile number and password, I hereby agree and accept the Terms of service and Privacy Policy in use of the POS Billingwala Dealer app.";
        SpannableString spannableString = new SpannableString(privacyPolicy);

        // creating clickable span to be implemented as a link
        ClickableSpan clickableSpan1 = new ClickableSpan() {
            public void onClick(View widget) {

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://posbillingwala.com/PlayStore/privacy_policy.html"));
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
            public void onClick(View widget) {

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://posbillingwala.com/PlayStore/privacy_policy.html"));
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

        // setting the part of string to be act as a link
        spannableString.setSpan(clickableSpan1, 64, 80, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(clickableSpan2, 85, 99, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        binding.privacyPolicy.setText(spannableString);
        binding.privacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());

        initAds();

        checkUpdateApp();

        binding.ivPassShow.setOnClickListener(this);
        binding.loginCheck.setOnClickListener(this);
        binding.forgotPassword.setOnClickListener(this);
        binding.newUser.setOnClickListener(this);

    }

    private void checkUpdateApp() {

        // Initialize the Update Manager with the Activity and the Update Mode
        mUpdateManager = UpdateManager.Builder(this);
        mUpdateManager.addUpdateInfoListener(new UpdateManager.UpdateInfoListener() {
            @Override
            public void onReceiveVersionCode(final int code) {

            }

            @Override
            public void onReceiveStalenessDays(final int days) {

            }
        });
        mUpdateManager.addFlexibleUpdateDownloadListener(new UpdateManager.FlexibleUpdateDownloadListener() {
            @Override
            public void onDownloadProgress(final long bytesDownloaded, final long totalBytes) {

            }
        });

        callFlexibleUpdate();

    }

    private void initAds() {

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

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.ivPassShow) {
            if (binding.password.getTransformationMethod().equals(PasswordTransformationMethod.getInstance())) {
                binding.ivPassShow.setImageResource(R.drawable.ic_hide);
                binding.password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                binding.ivPassShow.setImageResource(R.drawable.ic_look);
                binding.password.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            binding.password.setSelection(binding.password.getText().toString().length());
        } else if (id == R.id.loginCheck) {
            String mobile = binding.mobileNumber.getText().toString().trim();
            String password = binding.password.getText().toString();
            if (mobile.isEmpty()) {
                binding.mobileNumber.setError("Please enter mobile number");
                return;
            }
            if (mobile.length() != 10) {
                Toast.makeText(this, "Enter valid 10 digit mobile number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty()) {
                binding.password.setError("Please enter password");
                return;
            }
            loginDealer(mobile, password);
        } else if (id == R.id.forgotPassword) {
            showForgotPasswordDialog();
        } else if (id == R.id.newUser) {
            signUp();
        }
    }

    private void loginDealer(String mobile, String password) {

        SweetAlertDialog pDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().loginDealer(mobile, password);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if ("true".equalsIgnoreCase(response.body().getStatus())
                            || "1".equalsIgnoreCase(response.body().getStatus())) {

                        pref = getSharedPreferences("user", Context.MODE_PRIVATE);
                        editor = pref.edit();
                        editor.putString("UserLogin", "UserLoginSuccessful");
                        editor.commit();

                        Common.saveUserData(Login.this, "userId", response.body().getUserId());
                        AuthTokens.saveFromLogin(Login.this, response.body());

                        Intent intent = new Intent(Login.this, MainActivity.class);
                        startActivity(intent);
                        finishAffinity();

                    } else {
                        String message = response.body().getMessage();
                        Toast.makeText(Login.this,
                                message != null ? message : "Login failed",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Login.this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
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

    private void showForgotPasswordDialog() {
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_forgot_password, null);
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        sheet.setContentView(sheetView);

        TextInputEditText mobileField = sheetView.findViewById(R.id.forgotMobileNumber);
        TextInputEditText aadhaarField = sheetView.findViewById(R.id.forgotAadhaarNumber);
        TextInputEditText newPassField = sheetView.findViewById(R.id.forgotNewPassword);
        TextInputEditText confirmField = sheetView.findViewById(R.id.forgotConfirmPassword);
        TextView contactSupport = sheetView.findViewById(R.id.contactSupport);

        if (binding.mobileNumber.getText() != null && binding.mobileNumber.getText().length() == 10) {
            mobileField.setText(binding.mobileNumber.getText().toString());
        }

        contactSupport.setText(getString(R.string.forgot_password_support, getString(R.string.support_phone_display)));
        contactSupport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + getString(R.string.support_phone_dial)));
            startActivity(intent);
        });

        sheetView.findViewById(R.id.closeForgotSheet).setOnClickListener(v -> sheet.dismiss());
        sheetView.findViewById(R.id.btnForgotCancel).setOnClickListener(v -> sheet.dismiss());
        sheetView.findViewById(R.id.btnForgotReset).setOnClickListener(v -> {
            String mobile = mobileField.getText() != null ? mobileField.getText().toString().trim() : "";
            String aadhaar = aadhaarField.getText() != null ? aadhaarField.getText().toString().trim() : "";
            String newPass = newPassField.getText() != null ? newPassField.getText().toString() : "";
            String confirm = confirmField.getText() != null ? confirmField.getText().toString() : "";

            if (mobile.length() != 10) {
                Toast.makeText(this, "Enter valid 10 digit mobile number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (aadhaar.length() != 12) {
                Toast.makeText(this, "Enter valid 12 digit Aadhaar number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPass.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            submitForgotPassword(sheet, mobile, aadhaar, newPass);
        });

        sheet.setOnShowListener(d -> {
            View bottomSheet = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        sheet.show();
        BottomSheetUi.applyFullWidth(sheet);
    }

    private void submitForgotPassword(BottomSheetDialog sheet, String mobile, String aadhaar, String newPassword) {
        SweetAlertDialog pDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Resetting...");
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().forgotPassword(mobile, aadhaar, newPassword).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful() && response.body() != null
                        && "1".equalsIgnoreCase(response.body().getStatus())) {
                    sheet.dismiss();
                    Toast.makeText(Login.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Unable to reset password";
                    Toast.makeText(Login.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(Login.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void signUp() {
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_new_user, null);
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        sheet.setContentView(sheetView);

        TextView contactUser = sheetView.findViewById(R.id.contactUser);
        TextView supportMessage = sheetView.findViewById(R.id.supportMessage);
        if (supportMessage != null) {
            supportMessage.setText(getString(R.string.new_user_support_team, getString(R.string.support_phone_display)));
        }

        sheetView.findViewById(R.id.closeNewUserSheet).setOnClickListener(v -> sheet.dismiss());
        contactUser.setOnClickListener(v -> {
            sheet.dismiss();
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + getString(R.string.support_phone_dial)));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        sheet.show();
        BottomSheetUi.applyFullWidth(sheet);
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

    public void callFlexibleUpdate() {
        // Start a Flexible Update
        mUpdateManager.mode(UpdateManagerConstant.FLEXIBLE).start();
    }

    public void callImmediateUpdate() {
        // Start a Immediate Update
        mUpdateManager.mode(UpdateManagerConstant.IMMEDIATE).start();
    }

}