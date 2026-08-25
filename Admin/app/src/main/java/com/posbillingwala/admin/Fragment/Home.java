package com.posbillingwala.admin.Fragment;

import android.annotation.SuppressLint;
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
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentHomeBinding;
import com.posbillingwala.admin.databinding.IncludeDashboardStatBinding;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class Home extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    FragmentHomeBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        MainActivity.title.setText("Dashboard");

        bindStat(binding.statActiveCustomer, "Active\nCustomers");
        bindStat(binding.statTrialCustomer, "Trial\nCustomers");
        bindStat(binding.statExpiredCustomer, "Expired\nCustomers");
        bindStat(binding.statActiveLicenses, "Active\nLicenses");
        bindStat(binding.statExpiringLicenses, "Expiring\n(30 days)");
        bindStat(binding.statExpiredLicenses, "Expired\nLicenses");
        bindStat(binding.statBranches, "Total\nBranches");
        bindStat(binding.statDevices, "Total\nDevices");

        binding.customerRegistration.setOnClickListener(this);
        binding.onBoardCustomerList.setOnClickListener(this);

        return view;
    }

    private void bindStat(IncludeDashboardStatBinding statBinding, String label) {
        if (statBinding != null && statBinding.statLabel != null) {
            statBinding.statLabel.setText(label);
        }
    }

    private void setStatValue(IncludeDashboardStatBinding statBinding, String value) {
        if (statBinding != null && statBinding.statValue != null) {
            statBinding.statValue.setText(value != null ? value : "0");
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.customerRegistration) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new CustomerRegistration(), true);
        } else if (id == R.id.onBoardCustomerList) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AllCustomerList(), true);
        }
    }

    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(0);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getCustomerCount();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void getCustomerCount() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getCustomerCount();
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getStatus() != null
                        && response.body().getStatus().equalsIgnoreCase("true")) {
                    AllApiResponse body = response.body();
                    binding.totalCustomer.setText(nz(body.getTotalCustomer()));
                    binding.totalDealer.setText(nz(body.getTotalDealer()));
                    setStatValue(binding.statActiveCustomer, body.getActiveCustomer());
                    setStatValue(binding.statTrialCustomer, body.getTrialCustomer());
                    setStatValue(binding.statExpiredCustomer, body.getExpiredCustomer());
                    setStatValue(binding.statActiveLicenses, body.getActiveLicenses());
                    setStatValue(binding.statExpiringLicenses, body.getExpiringLicenses());
                    setStatValue(binding.statExpiredLicenses, body.getExpiredLicenses());
                    setStatValue(binding.statBranches, body.getTotalBranches());
                    setStatValue(binding.statDevices, body.getTotalDevices());
                } else {
                    binding.totalCustomer.setText("0");
                    binding.totalDealer.setText("0");
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText("Oops...");
                sweetAlertDialog.setContentText("Unable to load dashboard. Please try again.");
                sweetAlertDialog.setCancelClickListener(SweetAlertDialog::dismiss).show();
            }
        });
    }

    private static String nz(String value) {
        return value == null || value.isEmpty() ? "0" : value;
    }
}
