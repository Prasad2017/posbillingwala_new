package com.pos_billingwala.Activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;
import com.pos_billingwala.databinding.ActivityRegisterBinding;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Register extends AppCompatActivity implements View.OnClickListener {

    public static final String EXTRA_LICENCE_KEY = "licenceKey";
    public static final String EXTRA_AUTO_LOGIN = "autoLogin";

    ActivityRegisterBinding binding;

    public void setScreenSizeSmall() {
        Configuration configuration = getResources().getConfiguration();
        configuration.fontScale = (float) 1;
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        configuration.densityDpi = (int) getResources().getDisplayMetrics().xdpi;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setScreenSizeSmall();

        binding.backToLogin.setOnClickListener(this);
        binding.submitSignup.setOnClickListener(this);

        binding.supportContact.setText(getString(R.string.trial_support_contact, getString(R.string.support_phone_display)));
        binding.supportContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + getString(R.string.support_phone_dial)));
            startActivity(intent);
        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToLogin) {
            finish();
        } else if (id == R.id.submitSignup) {
            submitRegistration();
        }
    }

    private void submitRegistration() {
        String name = binding.signupName.getText().toString().trim();
        String contact = binding.signupContact.getText().toString().trim();
        String shopName = binding.signupShopName.getText().toString().trim();
        String address = binding.signupAddress.getText().toString().trim();

        if (name.isEmpty() || contact.isEmpty() || shopName.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, R.string.trial_error_empty_fields, Toast.LENGTH_LONG).show();
            return;
        }

        String mobileDigits = contact.replaceAll("\\D+", "");
        if (mobileDigits.length() < 10) {
            Toast.makeText(this, R.string.trial_error_invalid_mobile, Toast.LENGTH_LONG).show();
            return;
        }

        SweetAlertDialog pDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.trial_loading));
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient(this).registerTrial(name, contact, address, shopName);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (!response.isSuccessful() || response.body() == null) {
                    Snackbar.make(binding.registerLayout, R.string.trial_error_server, Snackbar.LENGTH_LONG).show();
                    return;
                }

                AllApiResponse body = response.body();
                if ("1".equalsIgnoreCase(body.getStatus())) {
                    showSuccessAndReturn(body.getLicenceKey());
                } else {
                    String message = body.getMessage();
                    if (message == null || message.trim().isEmpty()) {
                        message = getString(R.string.trial_error_default);
                    }
                    Snackbar.make(binding.registerLayout, message, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(Register.this, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText(getString(R.string.trial_error_dialog_title));
                sweetAlertDialog.setContentText(getString(R.string.trial_error_network));
                sweetAlertDialog.setConfirmText(getString(R.string.retry));
                sweetAlertDialog.setCancelClickListener(SweetAlertDialog::dismiss);
                sweetAlertDialog.show();
            }
        });
    }

    private void showSuccessAndReturn(String licenceKey) {
        SweetAlertDialog successDialog = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
        successDialog.setTitleText(getString(R.string.trial_success_title));
        successDialog.setContentText(getString(R.string.trial_success_message, licenceKey));
        successDialog.setConfirmText(getString(R.string.trial_success_button));
        successDialog.setCancelable(false);
        successDialog.setConfirmClickListener(sDialog -> {
            sDialog.dismiss();
            Intent intent = new Intent();
            intent.putExtra(EXTRA_LICENCE_KEY, licenceKey);
            intent.putExtra(EXTRA_AUTO_LOGIN, true);
            setResult(RESULT_OK, intent);
            finish();
        });
        successDialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
