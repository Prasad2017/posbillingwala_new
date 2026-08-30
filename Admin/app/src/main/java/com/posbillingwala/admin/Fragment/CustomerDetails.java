package com.posbillingwala.admin.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
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
        ((MainActivity) activity).setScreenTitle("Customer Details");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
        }

        MainActivity.back.setOnClickListener(v ->
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack());

        setupTabs();
        binding.updateCustomer.setOnClickListener(this);
        if (binding.fabAddLicense != null) {
            binding.fabAddLicense.setOnClickListener(v -> openAddLicense());
        }
        binding.openCatalog.setOnClickListener(v -> {
            AddCustomerProductCategory category = new AddCustomerProductCategory();
            Bundle b = new Bundle();
            b.putString("customerId", customerId);
            category.setArguments(b);
            ((MainActivity) activity).loadFragment(category, true);
        });
        if (binding.openImportExport != null) {
            binding.openImportExport.setOnClickListener(v -> {
                ProductExport export = new ProductExport();
                Bundle b = new Bundle();
                b.putString("customerId", customerId);
                export.setArguments(b);
                ((MainActivity) activity).loadFragment(export, true);
            });
        }
        if (binding.catalogCategoriesCard != null) {
            binding.catalogCategoriesCard.setOnClickListener(v -> binding.openCatalog.performClick());
        }
        if (binding.catalogSubcategoriesCard != null) {
            binding.catalogSubcategoriesCard.setOnClickListener(v -> {
                AddCustomerSubcategory sub = new AddCustomerSubcategory();
                Bundle b = new Bundle();
                b.putString("customerId", customerId);
                sub.setArguments(b);
                ((MainActivity) activity).loadFragment(sub, true);
            });
        }
        if (binding.catalogProductsCard != null) {
            binding.catalogProductsCard.setOnClickListener(v -> {
                AllCustomerProductList products = new AllCustomerProductList();
                Bundle b = new Bundle();
                b.putString("customerId", customerId);
                products.setArguments(b);
                ((MainActivity) activity).loadFragment(products, true);
            });
        }
        if (binding.catalogPortionsCard != null) {
            binding.catalogPortionsCard.setOnClickListener(v -> {
                AddCustomerPortionMaster portions = new AddCustomerPortionMaster();
                Bundle b = new Bundle();
                b.putString("customerId", customerId);
                portions.setArguments(b);
                ((MainActivity) activity).loadFragment(portions, true);
            });
        }
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

    private void openAddLicense() {
        NewLicenceRegistration license = new NewLicenceRegistration();
        Bundle b = new Bundle();
        b.putString("customerId", customerId);
        if (!customerResponseList.isEmpty()) {
            CustomerResponse c = customerResponseList.get(0);
            b.putString("customerName", c.getName());
            b.putString("customerMobile", c.getContactNumber());
            b.putString("customerAddress", c.getAddress());
            b.putString("shopName", c.getShopName());
        }
        license.setArguments(b);
        ((MainActivity) activity).loadFragment(license, true);
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
                if (binding.headerCustomerId != null) {
                    binding.headerCustomerId.setText("Customer ID: " + safe(customer.getId()));
                }

                List<LicenseResponse> licenses = customer.getLicenseResponseList();
                if (licenses == null) {
                    licenses = new ArrayList<>();
                }
                licenseResponseList = new ArrayList<>(licenses);

                int branchCount = licenseResponseList.size();
                if (customer.getBranchCount() != null && !customer.getBranchCount().isEmpty()) {
                    try {
                        branchCount = Integer.parseInt(customer.getBranchCount());
                    } catch (Exception ignored) {
                    }
                }
                int licenseCount = licenseResponseList.size();
                deviceList.clear();
                for (LicenseResponse lic : licenseResponseList) {
                    String deviceId = lic.getAndroidDeviceId();
                    if (deviceId != null && !deviceId.trim().isEmpty()) {
                        deviceList.add(lic);
                    }
                }
                int deviceCount = deviceList.size();

                binding.statBranchesValue.setText(String.valueOf(branchCount));
                binding.statLicensesValue.setText(String.valueOf(licenseCount));
                binding.statDevicesValue.setText(String.valueOf(deviceCount));

                String overallStatus = overallCustomerStatus(licenseResponseList);
                LicenseStatusHelper.applyBadge(binding.headerStatusBadge, overallStatus);

                bindInfoRow(binding.rowShopName, "Shop Name", safe(customer.getShopName()));
                bindInfoRow(binding.rowOwnerName, "Owner Name", safe(customer.getName()));
                bindInfoRow(binding.rowMobile, "Mobile Number", safe(customer.getContactNumber()));
                bindInfoRow(binding.rowEmail, "Email", safe(customer.getEmail()));
                bindInfoRow(binding.rowAddress, "Address", safe(customer.getAddress()));
                String reportPin = customer.getReportPin();
                if (reportPin == null || reportPin.trim().isEmpty()) {
                    reportPin = "9082";
                }
                bindInfoRow(binding.rowReportPin, "Report PIN", reportPin);
                String dealerLine = customer.getDealerName() != null && !customer.getDealerName().trim().isEmpty()
                        ? "  ·  Dealer: " + customer.getDealerName() : "";
                binding.headerOwnerLine.setText("Owner: " + safe(customer.getName())
                        + "  ·  Mobile: " + safe(customer.getContactNumber()) + dealerLine);

                if (response.body().getCategoryCount() != null) {
                    if (binding.catalogCategoryCount != null) {
                        binding.catalogCategoryCount.setText(nz(response.body().getCategoryCount()));
                    }
                    if (binding.catalogSubcategoryCount != null) {
                        binding.catalogSubcategoryCount.setText(nz(response.body().getSubcategoryCount()));
                    }
                    if (binding.catalogProductCount != null) {
                        binding.catalogProductCount.setText(nz(response.body().getProductCount()));
                    }
                    if (binding.catalogPortionCount != null) {
                        binding.catalogPortionCount.setText(nz(response.body().getPortionCount()));
                    }
                }

                bindLicenseSummary(licenseResponseList);
                loadCatalogSummary();

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
                binding.recyclerModules.setAdapter(new ModuleCardAdapter(activity, licenseResponseList));
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

    private void bindLicenseSummary(List<LicenseResponse> licenses) {
        if (binding.summaryActiveLicenses == null) return;
        int active = 0, expiring = 0, expired = 0, trial = 0;
        String nextExpiry = null;
        for (LicenseResponse lic : licenses) {
            String status = LicenseStatusHelper.displayStatus(lic);
            if (LicenseStatusHelper.STATUS_TRIAL.equals(status)) {
                trial++;
            } else if (LicenseStatusHelper.STATUS_EXPIRING.equals(status)) {
                expiring++;
                active++;
            } else if (LicenseStatusHelper.STATUS_ACTIVE.equals(status)
                    || LicenseStatusHelper.STATUS_LIFETIME.equals(status)
                    || LicenseStatusHelper.STATUS_PENDING.equals(status)) {
                active++;
            } else if (LicenseStatusHelper.STATUS_EXPIRED.equals(status)
                    || LicenseStatusHelper.STATUS_SUSPENDED.equals(status)
                    || LicenseStatusHelper.STATUS_REVOKED.equals(status)) {
                expired++;
            }
            String exp = lic.getExpiryDate();
            if (exp != null && !exp.trim().isEmpty()) {
                if (nextExpiry == null || exp.compareTo(nextExpiry) < 0) {
                    if (!LicenseStatusHelper.STATUS_EXPIRED.equals(status)) {
                        nextExpiry = exp;
                    }
                }
            }
        }
        binding.summaryActiveLicenses.setText(String.valueOf(active));
        binding.summaryExpiringLicenses.setText(String.valueOf(expiring));
        binding.summaryExpiredLicenses.setText(String.valueOf(expired));
        binding.summaryTrialLicenses.setText(String.valueOf(trial));
        binding.summaryNextExpiry.setText(nextExpiry != null ? nextExpiry : "—");

        bindInfoRow(binding.rowActiveLicenses, "Active Licenses", String.valueOf(active));
        bindInfoRow(binding.rowExpiringLicenses, "Expiring Soon", String.valueOf(expiring));
        bindInfoRow(binding.rowExpiredLicenses, "Expired Licenses", String.valueOf(expired));
        bindInfoRow(binding.rowTrialLicenses, "Trial Licenses", String.valueOf(trial));
        bindInfoRow(binding.rowNextExpiry, "Next Expiry", nextExpiry != null ? nextExpiry : "—");
    }

    private void bindInfoRow(com.posbillingwala.admin.databinding.ItemInfoRowBinding row, String label, String value) {
        if (row == null) return;
        row.infoLabel.setText(label);
        row.infoValue.setText(value != null && !value.trim().isEmpty() ? value : "—");
    }

    private void loadCatalogSummary() {
        if (customerId == null || customerId.trim().isEmpty()) return;
        Api.getClient().getCustomerCatalogSummary(customerId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null
                        && "true".equalsIgnoreCase(response.body().getStatus())) {
                    AllApiResponse body = response.body();
                    if (binding.catalogCategoryCount != null) {
                        binding.catalogCategoryCount.setText(nz(body.getCategoryCount()));
                    }
                    if (binding.catalogSubcategoryCount != null) {
                        binding.catalogSubcategoryCount.setText(nz(body.getSubcategoryCount()));
                    }
                    if (binding.catalogProductCount != null) {
                        binding.catalogProductCount.setText(nz(body.getProductCount()));
                    }
                    if (binding.catalogPortionCount != null) {
                        binding.catalogPortionCount.setText(nz(body.getPortionCount()));
                    }
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
            }
        });
    }

    private static String nz(String v) {
        return v == null || v.trim().isEmpty() ? "0" : v.trim();
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
