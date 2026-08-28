package com.posbillingwala.admin.Activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.posbillingwala.admin.Extra.AuthTokens;
import com.posbillingwala.admin.Extra.Common;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.ActivityLoginBinding;

import java.io.File;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId")
public class Login extends AppCompatActivity implements View.OnClickListener {

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    String m_androidId, manufacturerModel;
    ActivityLoginBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        Api.bindContext(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        if (AuthTokens.hasValidSession(this)) {
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        // Stale login flag without token — force re-auth
        File file = new File("data/data/" + getPackageName() + "/shared_prefs/user.xml");
        if (file.exists()) {
            AuthTokens.clear(this);
        }

        binding.userEmail.setSelection(binding.userEmail.getText().toString().length());

        m_androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.e("m_androidId", "" + m_androidId);
        manufacturerModel = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
        Log.e("manufacturerModel", "" + manufacturerModel);

        String privacyPolicy = "By providing email & password, I hereby agree and accept the Terms of service and Privacy Policy in use of the POS Billingwala Admin app.";
        SpannableString spannableString = new SpannableString(privacyPolicy);

        // creating clickable span to be implemented as a link
        ClickableSpan clickableSpan1 = new ClickableSpan() {
            public void onClick(View widget) {

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
            public void onClick(View widget) {

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

        // "Terms of service" and "Privacy Policy" link spans
        spannableString.setSpan(clickableSpan1, 61, 77, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(clickableSpan2, 82, 96, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        binding.privacyPolicy.setText(spannableString);
        binding.privacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());

        binding.ivPassShow.setOnClickListener(this);
        binding.loginCheck.setOnClickListener(this);
        binding.newUser.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.iv_passShow) {
            if (binding.password.getTransformationMethod().equals(PasswordTransformationMethod.getInstance())) {
                binding.ivPassShow.setImageResource(R.drawable.ic_hide);
                binding.password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                binding.ivPassShow.setImageResource(R.drawable.ic_look);
                binding.password.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            binding.password.setSelection(binding.password.getText().toString().length());
        } else if (id == R.id.loginCheck) {
            String email = binding.userEmail.getText() != null
                    ? binding.userEmail.getText().toString().trim() : "";
            String password = binding.password.getText() != null
                    ? binding.password.getText().toString() : "";
            if (email.isEmpty()) {
                binding.userEmail.setError("Please enter email");
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.userEmail.setError("Please enter a valid email");
            } else if (password.isEmpty()) {
                binding.password.setError("Please enter password");
            } else {
                loginAdmin(email, password);
            }
        } else if (id == R.id.newUser) {
            signUp();
        }
    }

    private void loginAdmin(String email, String password) {

        SweetAlertDialog pDialog = new SweetAlertDialog(Login.this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().loginAdmin(email, password);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null && response.body().getStatus() != null
                            && response.body().getStatus().equalsIgnoreCase("true")) {

                        pref = getSharedPreferences("user", Context.MODE_PRIVATE);
                        editor = pref.edit();
                        editor.putString("UserLogin", "UserLoginSuccessful");
                        editor.apply();

                        Common.saveUserData(Login.this, "userId", "" + response.body().getUserId());
                        Common.saveUserData(Login.this, "userEmail", email);
                        AuthTokens.saveFromLogin(Login.this, response.body());

                        Intent intent = new Intent(Login.this, MainActivity.class);
                        startActivity(intent);
                        finishAffinity();

                    } else {
                        String msg = response.body() != null ? response.body().getMessage() : "Login failed";
                        Toast.makeText(Login.this, "" + msg, Toast.LENGTH_SHORT).show();
                    }
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

    private void signUp() {

        final Dialog dialog = new Dialog(Login.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.new_user_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(true);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        TextView contactUser = dialog.findViewById(R.id.contactUser);
        TextView supportMessage = dialog.findViewById(R.id.supportMessage);
        if (supportMessage != null) {
            supportMessage.setText(getString(R.string.new_user_support_team, getString(R.string.support_phone_display)));
        }

        contactUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + getString(R.string.support_phone_dial)));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
