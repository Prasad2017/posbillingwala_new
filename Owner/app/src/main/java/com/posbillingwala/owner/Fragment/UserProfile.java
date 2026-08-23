package com.posbillingwala.owner.Fragment;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Adapter.LicenseAdapter;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Extra.SimpleDividerItemDecoration;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.CustomerResponse;
import com.posbillingwala.owner.Model.LicenseResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentUserProfileBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfile extends Fragment {

    public static Activity activity;
    public FragmentUserProfileBinding binding;
    public List<CustomerResponse> customerResponseList = new ArrayList<>();
    public List<LicenseResponse> licenseResponseList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentUserProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        activity = getActivity();

        // Set up key listener for back navigation
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("tag", "onKey Back listener is working!!!");
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new UserSetting(), true);
                return true;
            }
            return false;
        });

        // Set up click listeners
        binding.backToSetting.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new UserSetting(), true);
        });

        binding.updateCustomer.setOnClickListener(v -> {
            if (!binding.customerName.getText().toString().isEmpty()) {
                if (!binding.customerMobileNumber.getText().toString().isEmpty()) {
                    if (!binding.customerAddress.getText().toString().isEmpty()) {
                        if (!binding.customerShopName.getText().toString().isEmpty()) {
                            updateCustomerDetails();
                        } else {
                            binding.customerShopName.setError("Please fill this");
                        }
                    } else {
                        binding.customerAddress.setError("Please fill this");
                    }
                } else {
                    binding.customerMobileNumber.setError("Please fill this");
                }
            } else {
                binding.customerName.setError("Please fill this");
            }
        });

        return view;
    }

    public void updateCustomerDetails() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().updateCustomerDetails(
                MainActivity.userId,
                binding.customerName.getText().toString(),
                binding.customerMobileNumber.getText().toString(),
                binding.customerAddress.getText().toString(),
                binding.customerShopName.getText().toString()
        );
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful()) {
                    if ("1".equalsIgnoreCase(response.body().getStatus())) {
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        getCustomerDetails();
                    } else {
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                Toast.makeText(activity, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DetectConnection.checkInternetConnection(activity)) {
            getCustomerDetails();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    public void getCustomerDetails() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        customerResponseList.clear();
        licenseResponseList.clear();

        Call<AllApiResponse> call = Api.getClient().getProfile(MainActivity.userId);
        call.enqueue(new Callback<AllApiResponse>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful()) {
                    customerResponseList = response.body().getCustomerResponseList();
                    if (!customerResponseList.isEmpty()) {
                        CustomerResponse customerResponse = customerResponseList.get(0);

                        binding.customerName.setText(customerResponse.getName());
                        binding.customerMobileNumber.setText(customerResponse.getContactNumber());
                        binding.customerAddress.setText(customerResponse.getAddress());
                        binding.customerShopName.setText(customerResponse.getShopName());

                        licenseResponseList = customerResponse.getLicenseResponseList();

                        if (!licenseResponseList.isEmpty()) {
                            LicenseAdapter licenseAdapter = new LicenseAdapter(activity, licenseResponseList);
                            binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                            binding.recyclerView.addItemDecoration(new SimpleDividerItemDecoration(activity));
                            binding.recyclerView.setAdapter(licenseAdapter);
                            binding.recyclerView.setHasFixedSize(true);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Oops...")
                        .setContentText("Something went wrong!")
                        .setCancelClickListener(SweetAlertDialog::dismiss)
                        .show();
            }
        });
    }
}
