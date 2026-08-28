package com.posbillingwala.admin.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.CustomerAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.LicenseStatusHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.CustomerResponse;
import com.posbillingwala.admin.Model.LicenseResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentAllCustomerListBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class AllCustomerList extends Fragment {

    public static Activity activity;
    View view;
    FragmentAllCustomerListBinding binding;
    CustomerAdapter customerAdapter;
    List<CustomerResponse> customerResponseList = new ArrayList<>();
    List<CustomerResponse> filteredList = new ArrayList<>();
    String statusFilter = "ALL";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAllCustomerListBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        // Keep title from navigateRoot (Customers / Licenses / etc.)
        if (MainActivity.title != null
                && (MainActivity.title.getText() == null
                || MainActivity.title.getText().toString().trim().isEmpty())) {
            ((MainActivity) activity).setScreenTitle("Customers");
        }

        if (binding.searchCustomer != null) {
            binding.searchCustomer.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilters();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (binding.fabAddCustomer != null) {
            binding.fabAddCustomer.setOnClickListener(v ->
                    ((MainActivity) activity).navigateDetail(new CustomerRegistration(), "Add Customer"));
        }

        if (binding.chipAll != null) {
            binding.chipAll.setOnClickListener(v -> setStatusFilter("ALL"));
            binding.chipActive.setOnClickListener(v -> setStatusFilter("ACTIVE"));
            binding.chipTrial.setOnClickListener(v -> setStatusFilter("TRIAL"));
            binding.chipExpired.setOnClickListener(v -> setStatusFilter("EXPIRED"));
            Bundle args = getArguments();
            String initial = args != null ? args.getString("statusFilter", "ALL") : "ALL";
            if (initial == null || initial.isEmpty()) initial = "ALL";
            statusFilter = initial;
            highlightChip(initial);
        }

        return view;
    }

    private void setStatusFilter(String filter) {
        statusFilter = filter;
        highlightChip(filter);
        applyFilters();
    }

    private void highlightChip(String selected) {
        styleChip(binding.chipAll, "ALL".equals(selected));
        styleChip(binding.chipActive, "ACTIVE".equals(selected));
        styleChip(binding.chipTrial, "TRIAL".equals(selected));
        styleChip(binding.chipExpired, "EXPIRED".equals(selected));
    }

    private void styleChip(TextView chip, boolean selected) {
        if (chip == null) return;
        chip.setBackgroundResource(selected ? R.drawable.bg_month_chip : R.drawable.bg_card);
        chip.setTextColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.colorPrimary : R.color.colorTextSecondary));
    }

    private void applyFilters() {
        String q = binding.searchCustomer.getText() != null
                ? binding.searchCustomer.getText().toString().trim().toLowerCase() : "";
        filteredList.clear();
        int all = 0, active = 0, trial = 0, expired = 0;
        for (CustomerResponse c : customerResponseList) {
            if (c == null) continue;
            String status = customerStatus(c);
            all++;
            if (LicenseStatusHelper.STATUS_TRIAL.equals(status)) trial++;
            else if (LicenseStatusHelper.STATUS_EXPIRED.equals(status)
                    || LicenseStatusHelper.STATUS_SUSPENDED.equals(status)
                    || LicenseStatusHelper.STATUS_REVOKED.equals(status)) expired++;
            else active++;

            boolean statusOk = "ALL".equals(statusFilter)
                    || ("ACTIVE".equals(statusFilter) && !LicenseStatusHelper.STATUS_TRIAL.equals(status)
                    && !LicenseStatusHelper.STATUS_EXPIRED.equals(status)
                    && !LicenseStatusHelper.STATUS_SUSPENDED.equals(status)
                    && !LicenseStatusHelper.STATUS_REVOKED.equals(status))
                    || ("TRIAL".equals(statusFilter) && LicenseStatusHelper.STATUS_TRIAL.equals(status))
                    || ("EXPIRED".equals(statusFilter) && (LicenseStatusHelper.STATUS_EXPIRED.equals(status)
                    || LicenseStatusHelper.STATUS_SUSPENDED.equals(status)
                    || LicenseStatusHelper.STATUS_REVOKED.equals(status)));
            if (!statusOk) continue;

            String shop = c.getShopName() != null ? c.getShopName().toLowerCase() : "";
            String name = c.getName() != null ? c.getName().toLowerCase() : "";
            String mobile = c.getContactNumber() != null ? c.getContactNumber().toLowerCase() : "";
            if (q.isEmpty() || shop.contains(q) || name.contains(q) || mobile.contains(q)) {
                filteredList.add(c);
            }
        }
        if (binding.chipAll != null) {
            binding.chipAll.setText("All (" + all + ")");
            binding.chipActive.setText("Active (" + active + ")");
            binding.chipTrial.setText("Trial (" + trial + ")");
            binding.chipExpired.setText("Expired (" + expired + ")");
        }
        customerAdapter = new CustomerAdapter(activity, filteredList);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        binding.recyclerView.setAdapter(customerAdapter);
        binding.emptyCustomers.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String customerStatus(CustomerResponse customer) {
        if (customer.getLicenseResponseList() == null || customer.getLicenseResponseList().isEmpty()) {
            return LicenseStatusHelper.STATUS_PENDING;
        }
        LicenseResponse primary = customer.getLicenseResponseList().get(0);
        return LicenseStatusHelper.displayStatus(primary);
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(0);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getCustomerList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void getCustomerList() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        customerResponseList.clear();

        Call<AllApiResponse> call = Api.getClient().getCustomerList();
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getCustomerResponseList() != null) {
                    customerResponseList = new ArrayList<>(response.body().getCustomerResponseList());
                    applyFilters();
                } else {
                    binding.emptyCustomers.setVisibility(View.VISIBLE);
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE);
                sweetAlertDialog.setTitleText("Oops...");
                sweetAlertDialog.setContentText("Unable to load customers. Please try again.");
                sweetAlertDialog.setCancelClickListener(SweetAlertDialog::dismiss).show();
            }
        });
    }
}
