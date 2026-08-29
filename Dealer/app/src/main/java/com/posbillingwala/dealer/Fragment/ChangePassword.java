package com.posbillingwala.dealer.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Extra.DetectConnection;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.databinding.FragmentChangePasswordBinding;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, StaticFieldLeak, NonConstantResourceId")
public class ChangePassword extends Fragment {

    public static Activity activity;
    FragmentChangePasswordBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);
        activity = getActivity();
        MainActivity.title.setText("Change Password");

        MainActivity.back.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new DealerProfile(), false);
        });

        binding.getRoot().setFocusableInTouchMode(true);
        binding.getRoot().requestFocus();
        binding.getRoot().setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                MainActivity.back.performClick();
                return true;
            }
            return false;
        });

        binding.changePasswordBtn.setOnClickListener(v -> submitChangePassword());
        return binding.getRoot();
    }

    private void submitChangePassword() {
        String current = binding.currentPassword.getText() != null
                ? binding.currentPassword.getText().toString() : "";
        String newPass = binding.newPassword.getText() != null
                ? binding.newPassword.getText().toString() : "";
        String confirm = binding.confirmPassword.getText() != null
                ? binding.confirmPassword.getText().toString() : "";

        if (current.isEmpty()) {
            binding.currentPassword.setError("Required");
            return;
        }
        if (newPass.length() < 6) {
            Toast.makeText(activity, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPass.equals(confirm)) {
            Toast.makeText(activity, "New passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (current.equals(newPass)) {
            Toast.makeText(activity, "New password must be different", Toast.LENGTH_SHORT).show();
            return;
        }

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Updating...");
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().changePassword(MainActivity.userId, current, newPass)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                        pDialog.dismiss();
                        if (response.isSuccessful() && response.body() != null
                                && "1".equalsIgnoreCase(response.body().getStatus())) {
                            Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_LONG).show();
                            binding.currentPassword.setText("");
                            binding.newPassword.setText("");
                            binding.confirmPassword.setText("");
                            MainActivity.back.performClick();
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : "Unable to change password";
                            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        pDialog.dismiss();
                        Log.e("ChangePassword", t.getMessage());
                        Toast.makeText(activity, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(0);
        MainActivity.drawerLayout.closeDrawers();
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
        }
    }
}
