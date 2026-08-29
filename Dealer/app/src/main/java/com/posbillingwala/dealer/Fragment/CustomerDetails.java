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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Adapter.LicenseAdapter;
import com.posbillingwala.dealer.Extra.DetectConnection;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.Model.CustomerResponse;
import com.posbillingwala.dealer.Model.LicenseResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.databinding.FragmentCustomerDetailsBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class CustomerDetails extends Fragment implements View.OnClickListener {


    public static Activity activity;
    public static String customerId, customerType;
    View view;
    List<CustomerResponse> customerResponseList = new ArrayList<>();
    List<LicenseResponse> licenseResponseList = new ArrayList<>();
    LicenseAdapter licenseAdapter;
    FragmentCustomerDetailsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCustomerDetailsBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        MainActivity.title.setText("Customer Details");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
        }

        MainActivity.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new AllCustomerList(), false);
            }
        });

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                    ((MainActivity) activity).loadFragment(new AllCustomerList(), false);
                    return true;
                }
                return false;
            }
        });

        binding.updateCustomer.setOnClickListener(this);

        binding.openImportExport.setOnClickListener(v -> {
            ProductExport export = new ProductExport();
            Bundle b = new Bundle();
            b.putString("customerId", customerId);
            export.setArguments(b);
            ((MainActivity) activity).loadFragment(export, true);
        });

        binding.openCatalog.setOnClickListener(v -> {
            AddCustomerProductCategory category = new AddCustomerProductCategory();
            Bundle b = new Bundle();
            b.putString("customerId", customerId);
            category.setArguments(b);
            ((MainActivity) activity).loadFragment(category, true);
        });

        return view;

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.updateCustomer) {
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
        }
    }

    private void updateCustomerDetails() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().updateCustomerDetails(customerId, binding.customerName.getText().toString(), binding.customerMobileNumber.getText().toString(),
                binding.customerAddress.getText().toString(), binding.customerShopName.getText().toString(),
                MainActivity.userId,
                binding.fastBilling.isChecked() ? "1" : "0",
                binding.takeAway.isChecked() ? "1" : "0",
                binding.dineIn.isChecked() ? "1" : "0",
                binding.mess.isChecked() ? "1" : "0");
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        getCustomerDetails();
                    } else {
                        Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                Toast.makeText(activity, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void onStart() {
        super.onStart();
        Log.e("onStart", "called");
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(1);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getCustomerDetails();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void getCustomerDetails() {

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        customerResponseList.clear();
        licenseResponseList.clear();

        Call<AllApiResponse> call = Api.getClient().getCustomerDetails(customerId);
        call.enqueue(new Callback<AllApiResponse>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful()) {
                    customerResponseList = response.body().getCustomerResponseList();
                    if (!customerResponseList.isEmpty()) {

                        binding.customerName.setText(customerResponseList.get(0).getName());
                        binding.customerMobileNumber.setText(customerResponseList.get(0).getContactNumber());
                        binding.customerAddress.setText(customerResponseList.get(0).getAddress());
                        binding.customerShopName.setText(customerResponseList.get(0).getShopName());

                        licenseResponseList = customerResponseList.get(0).getLicenseResponseList();
                        bindOwnerModules(licenseResponseList);
                        if (customerResponseList.get(0).getRoleId().equalsIgnoreCase("2")) {
                            customerType = "Dealer";
                        } else {
                            customerType = "Customer";
                        }

                        if (!licenseResponseList.isEmpty()) {
                            licenseAdapter = new LicenseAdapter(activity, licenseResponseList, customerId);
                            binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                            binding.recyclerView.setAdapter(licenseAdapter);
                            binding.recyclerView.setHasFixedSize(true);
                        }
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE);
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

    private void bindOwnerModules(List<LicenseResponse> licenses) {
        LicenseResponse owner = null;
        if (licenses != null) {
            for (LicenseResponse lic : licenses) {
                if (lic != null && "owner".equalsIgnoreCase(lic.getUserType())) {
                    owner = lic;
                    break;
                }
            }
            if (owner == null && !licenses.isEmpty()) {
                owner = licenses.get(0);
            }
        }
        boolean fb = owner != null && isOn(owner.getFastBilling());
        boolean di = owner != null && isOn(owner.getDineIn());
        boolean ta = owner != null && isOn(owner.getTakeAway());
        boolean ms = owner != null && isOn(owner.getMess());
        binding.fastBilling.setChecked(fb);
        binding.dineIn.setChecked(di);
        binding.takeAway.setChecked(ta);
        binding.mess.setChecked(ms);
    }

    private static boolean isOn(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

}