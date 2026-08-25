package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentAddDealerBinding;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddDealer extends Fragment {

    Activity activity;
    FragmentAddDealerBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddDealerBinding.inflate(inflater, container, false);
        activity = getActivity();
        MainActivity.title.setText("Add Dealer");

        MainActivity.back.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AllDealerList(), false);
        });

        binding.submitDealer.setOnClickListener(v -> {
            String name = text(binding.dealerName);
            String mobile = text(binding.dealerMobile);
            if (name.isEmpty() || mobile.isEmpty()) {
                Toast.makeText(activity, "Name and mobile are required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!DetectConnection.checkInternetConnection(activity)) {
                DetectConnection.noInternetConnection(activity);
                return;
            }
            submit();
        });
        return binding.getRoot();
    }

    private void submit() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Creating");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().insertDealer(
                text(binding.dealerName),
                text(binding.dealerMobile),
                text(binding.dealerAddress),
                text(binding.dealerEmail),
                text(binding.dealerAadhaar),
                text(binding.dealerPassword));
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful() && response.body() != null && "1".equals(response.body().getStatus())) {
                    Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_LONG).show();
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new AllDealerList(), false);
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Create failed";
                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(activity, "Unable to create dealer. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String text(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(1);
    }
}
