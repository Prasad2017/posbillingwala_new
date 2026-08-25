package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentDealerReportBinding;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DealerReport extends Fragment {

    Activity activity;
    FragmentDealerReportBinding binding;
    String dealerId;
    String dealerName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDealerReportBinding.inflate(inflater, container, false);
        activity = getActivity();
        MainActivity.title.setText("Dealer Report");

        Bundle bundle = getArguments();
        if (bundle != null) {
            dealerId = bundle.getString("dealerId");
            dealerName = bundle.getString("dealerName");
        }
        binding.dealerTitle.setText(dealerName != null ? dealerName : "Dealer Report");

        MainActivity.back.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AllDealerList(), false);
        });

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(1);
        if (DetectConnection.checkInternetConnection(activity)) {
            loadReport();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void loadReport() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getDealerReport(dealerId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful() && response.body() != null
                        && "true".equalsIgnoreCase(response.body().getStatus())) {
                    AllApiResponse b = response.body();
                    binding.reportBody.setText(
                            "Customers: " + nz(b.getTotalCustomer())
                                    + "\nActive Customers: " + nz(b.getActiveCustomer())
                                    + "\nTrial Customers: " + nz(b.getTrialCustomer())
                                    + "\nActive Licenses: " + nz(b.getActiveLicenses())
                                    + "\nExpired Licenses: " + nz(b.getExpiredLicenses())
                                    + "\nBranches: " + nz(b.getTotalBranches())
                                    + "\nDevices: " + nz(b.getTotalDevices()));
                } else {
                    binding.reportBody.setText("Unable to load dealer report.");
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                binding.reportBody.setText("Unable to load dealer report.");
            }
        });
    }

    private static String nz(String v) {
        return v == null || v.isEmpty() ? "0" : v;
    }
}
