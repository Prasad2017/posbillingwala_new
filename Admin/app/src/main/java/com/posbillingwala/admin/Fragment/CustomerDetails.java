package com.posbillingwala.admin.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.BranchCardAdapter;
import com.posbillingwala.admin.Adapter.DeviceCardAdapter;
import com.posbillingwala.admin.Adapter.LicenseAdapter;
import com.posbillingwala.admin.Adapter.ModuleCardAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.LicenseStatusHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.CustomerResponse;
import com.posbillingwala.admin.Model.LicenseResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentCustomerDetailsBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class CustomerDetails extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static String customerId;
    View view;
    FragmentCustomerDetailsBinding binding;
    List<CustomerResponse> customerResponseList = new ArrayList<>();
    List<LicenseResponse> licenseResponseList = new ArrayList<>();
    List<LicenseResponse> deviceList = new ArrayList<>();
    LicenseAdapter licenseAdapter;

    private static final int TAB_OVERVIEW = 0;
    private static final int TAB_BRANCHES = 1;
    private static final int TAB_LICENSES = 2;
    private static final int TAB_DEVICES = 3;
    private static final int TAB_MODULES = 4;

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

        MainActivity.back.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AllCustomerList(), false);
        });

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                ((MainActivity) activity).loadFragment(new AllCustomerList(), false);
                return true;
            }
            return false;
        });

        setupTabs();
        binding.updateCustomer.setOnClickListener(this);
        binding.openCatalog.setOnClickListener(v -> {
            AddCustomerProductCategory category = new AddCustomerProductCategory();
            Bundle b = new Bundle();
            b.putString("customerId", customerId);
            category.setArguments(b);
            ((MainActivity) activity).loadFragment(category, true);
        });
        binding.openCombos.setOnClickListener(v -> {
            CustomerCombos combos = new CustomerCombos();
            Bundle b = new Bundle();
            b.putString("customerId", customerId);
            combos.setArguments(b);
            ((MainActivity) activity).loadFragment(combos, true);
        });
        binding.openSales.setOnClickListener(v -> {
            CustomerSales sales = new CustomerSales();
            Bundle b = new Bundle();
            b.putString("customerId", customerId);
            sales.setArguments(b);
            ((MainActivity) activity).loadFragment(sales, true);
        });

        showPanel(TAB_OVERVIEW);
        return view;
    }

    private void setupTabs() {
        binding.customerTabs.removeAllTabs();
        binding.customerTabs.addTab(binding.customerTabs.newTab().setText("Overview"));
        binding.customerTabs.addTab(binding.customerTabs.newTab().setText("Branches"));
        binding.customerTabs.addTab(binding.customerTabs.newTab().setText("Licenses"));
        binding.customerTabs.addTab(binding.customerTabs.newTab().setText("Devices"));
        binding.customerTabs.addTab(binding.customerTabs.newTab().setText("Modules"));

        binding.customerTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showPanel(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void showPanel(int index) {
        binding.panelOverview.setVisibility(index == TAB_OVERVIEW ? View.VISIBLE : View.GONE);
        binding.panelBranches.setVisibility(index == TAB_BRANCHES ? View.VISIBLE : View.GONE);
        binding.panelLicenses.setVisibility(index == TAB_LICENSES ? View.VISIBLE : View.GONE);
        binding.panelDevices.setVisibility(index == TAB_DEVICES ? View.VISIBLE : View.GONE);
        binding.panelModules.setVisibility(index == TAB_MODULES ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.updateCustomer) {
            if (binding.customerName.getText().toString().trim().isEmpty()) {
                binding.customerName.setError("Required");
                return;
            }
            if (binding.customerMobileNumber.getText().toString().trim().isEmpty()) {
                binding.customerMobileNumber.setError("Required");
                return;
            }
            if (binding.customerAddress.getText().toString().trim().isEmpty()) {
                binding.customerAddress.setError("Required");
                return;
            }
            if (binding.customerShopName.getText().toString().trim().isEmpty()) {
                binding.customerShopName.setError("Required");
                return;
            }
            if (!DetectConnection.checkInternetConnection(activity)) {
                DetectConnection.noInternetConnection(activity);
                return;
            }
            updateCustomerDetails();
        }
    }

    private void updateCustomerDetails() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Saving");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().updateCustomerDetails(
                customerId,
                binding.customerName.getText().toString(),
                binding.customerMobileNumber.getText().toString(),
                binding.customerAddress.getText().toString(),
                binding.customerShopName.getText().toString());
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful() && response.body() != null
                        && "1".equals(response.body().getStatus())) {
                    Toast.makeText(activity, "" + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    getCustomerDetails();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Update failed";
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(activity, "Unable to update customer. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onStart() {
        super.onStart();
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
        deviceList.clear();

        Call<AllApiResponse> call = Api.getClient().getCustomerDetails(customerId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getCustomerResponseList() == null
                        || response.body().getCustomerResponseList().isEmpty()) {
                    Toast.makeText(activity, "Customer not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                customerResponseList = response.body().getCustomerResponseList();
                CustomerResponse customer = customerResponseList.get(0);

                binding.customerName.setText(safe(customer.getName()));
                binding.customerMobileNumber.setText(safe(customer.getContactNumber()));
                binding.customerAddress.setText(safe(customer.getAddress()));
                binding.customerShopName.setText(safe(customer.getShopName()));

                binding.headerShopName.setText(safe(customer.getShopName()));
                binding.headerOwnerLine.setText("Owner: " + safe(customer.getName())
                        + "  ·  Mobile: " + safe(customer.getContactNumber()));

                List<LicenseResponse> licenses = customer.getLicenseResponseList();
                if (licenses == null) {
                    licenses = new ArrayList<>();
                }
                licenseResponseList = new ArrayList<>(licenses);

                int branchCount = licenseResponseList.size();
                int licenseCount = licenseResponseList.size();
                deviceList.clear();
                for (LicenseResponse lic : licenseResponseList) {
                    String deviceId = lic.getAndroidDeviceId();
                    if (deviceId != null && !deviceId.trim().isEmpty()) {
                        deviceList.add(lic);
                    }
                }
                int deviceCount = deviceList.size();

                binding.statBranches.setText(branchCount + "\nBranches");
                binding.statLicenses.setText(licenseCount + "\nLicenses");
                binding.statDevices.setText(deviceCount + "\nDevices");

                String overallStatus = overallCustomerStatus(licenseResponseList);
                binding.headerStatusBadge.setText(overallStatus);
                binding.headerStatusBadge.setBackgroundTintList(
                        ColorStateList.valueOf(LicenseStatusHelper.badgeColor(overallStatus)));

                // Licenses tab
                if (licenseResponseList.isEmpty()) {
                    binding.emptyLicenses.setVisibility(View.VISIBLE);
                    binding.recyclerView.setAdapter(null);
                } else {
                    binding.emptyLicenses.setVisibility(View.GONE);
                    licenseAdapter = new LicenseAdapter(activity, licenseResponseList, customerId);
                    binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                    binding.recyclerView.setAdapter(licenseAdapter);
                }

                // Branches tab (one card per license/branch)
                if (licenseResponseList.isEmpty()) {
                    binding.emptyBranches.setVisibility(View.VISIBLE);
                    binding.recyclerBranches.setAdapter(null);
                } else {
                    binding.emptyBranches.setVisibility(View.GONE);
                    binding.recyclerBranches.setLayoutManager(new LinearLayoutManager(activity));
                    binding.recyclerBranches.setAdapter(new BranchCardAdapter(licenseResponseList));
                }

                // Devices tab
                if (deviceList.isEmpty()) {
                    binding.emptyDevices.setVisibility(View.VISIBLE);
                    binding.recyclerDevices.setAdapter(null);
                } else {
                    binding.emptyDevices.setVisibility(View.GONE);
                    binding.recyclerDevices.setLayoutManager(new LinearLayoutManager(activity));
                    binding.recyclerDevices.setAdapter(new DeviceCardAdapter(deviceList));
                }

                // Modules tab
                binding.recyclerModules.setLayoutManager(new LinearLayoutManager(activity));
                binding.recyclerModules.setAdapter(new ModuleCardAdapter(licenseResponseList));
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog dialog = new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE);
                dialog.setTitleText("Oops...");
                dialog.setContentText("Unable to load customer details. Please try again.");
                dialog.setCancelClickListener(SweetAlertDialog::dismiss).show();
            }
        });
    }

    private static String overallCustomerStatus(List<LicenseResponse> licenses) {
        if (licenses == null || licenses.isEmpty()) {
            return LicenseStatusHelper.STATUS_PENDING;
        }
        boolean anyActive = false;
        boolean anyTrial = false;
        boolean allExpired = true;
        for (LicenseResponse lic : licenses) {
            String s = LicenseStatusHelper.displayStatus(lic);
            if (LicenseStatusHelper.STATUS_ACTIVE.equals(s)
                    || LicenseStatusHelper.STATUS_LIFETIME.equals(s)
                    || LicenseStatusHelper.STATUS_EXPIRING.equals(s)
                    || LicenseStatusHelper.STATUS_PENDING.equals(s)) {
                anyActive = true;
                allExpired = false;
            }
            if (LicenseStatusHelper.STATUS_TRIAL.equals(s)) {
                anyTrial = true;
                allExpired = false;
            }
            if (!LicenseStatusHelper.STATUS_EXPIRED.equals(s)
                    && !LicenseStatusHelper.STATUS_SUSPENDED.equals(s)
                    && !LicenseStatusHelper.STATUS_REVOKED.equals(s)) {
                allExpired = false;
            }
        }
        if (anyActive) {
            return LicenseStatusHelper.STATUS_ACTIVE;
        }
        if (anyTrial) {
            return LicenseStatusHelper.STATUS_TRIAL;
        }
        if (allExpired) {
            return LicenseStatusHelper.STATUS_EXPIRED;
        }
        return LicenseStatusHelper.STATUS_PENDING;
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
