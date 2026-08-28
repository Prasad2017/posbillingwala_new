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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.AadhaarMask;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.LicenseStatusHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.CustomerResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentDealerDetailsBinding;
import com.posbillingwala.admin.databinding.IncludeDashboardKpiCardBinding;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n")
public class DealerDetails extends Fragment {

    Activity activity;
    FragmentDealerDetailsBinding binding;
    String dealerId;
    String dealerName;
    String dealerMobile;
    String dealerEmail;
    String dealerAddress;
    String dealerAadhaar;
    boolean dealerActive = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDealerDetailsBinding.inflate(inflater, container, false);
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Dealer Details");

        Bundle args = getArguments();
        if (args != null) {
            dealerId = args.getString("dealerId");
            dealerName = args.getString("dealerName");
            dealerMobile = args.getString("dealerMobile");
            dealerEmail = args.getString("dealerEmail");
            dealerAddress = args.getString("dealerAddress");
            dealerAadhaar = args.getString("dealerAadhaar");
            dealerActive = args.getBoolean("dealerActive", true);
        }

        bindKpi(binding.kpiTotal, R.drawable.ic_nav_customers, R.drawable.bg_kpi_icon_blue,
                R.drawable.bg_kpi_card_blue, "Total\nCustomers", R.color.colorPrimary);
        bindKpi(binding.kpiActive, R.drawable.ic_nav_customers, R.drawable.bg_kpi_icon_green,
                R.drawable.bg_kpi_card_green, "Active\nCustomers", R.color.statusActive);
        bindKpi(binding.kpiTrial, R.drawable.ic_calendar, R.drawable.bg_kpi_icon_orange,
                R.drawable.bg_kpi_card_orange, "Trial\nCustomers", R.color.statusTrial);
        bindKpi(binding.kpiExpired, R.drawable.ic_warning, R.drawable.bg_kpi_icon_red,
                R.drawable.bg_kpi_card_red, "Expired\nCustomers", R.color.statusExpired);

        bindHeaderFromArgs();

        binding.actionEdit.setOnClickListener(v -> {
            DealerProfile profile = new DealerProfile();
            Bundle b = new Bundle();
            b.putString("dealerId", dealerId);
            profile.setArguments(b);
            ((MainActivity) activity).navigateDetail(profile, "Edit Dealer");
        });
        binding.actionCustomers.setOnClickListener(v -> {
            DealerCustomersList list = new DealerCustomersList();
            Bundle b = new Bundle();
            b.putString("dealerId", dealerId);
            b.putString("dealerName", dealerName);
            list.setArguments(b);
            ((MainActivity) activity).navigateDetail(list, "Dealer Customers");
        });
        binding.actionLicenses.setOnClickListener(v -> {
            DealerLicensesList list = new DealerLicensesList();
            Bundle b = new Bundle();
            b.putString("dealerId", dealerId);
            b.putString("dealerName", dealerName);
            list.setArguments(b);
            ((MainActivity) activity).navigateDetail(list, "Dealer Licenses");
        });
        binding.actionReport.setOnClickListener(v -> {
            DealerReport report = new DealerReport();
            Bundle b = new Bundle();
            b.putString("dealerId", dealerId);
            b.putString("dealerName", dealerName);
            report.setArguments(b);
            ((MainActivity) activity).navigateDetail(report, "Dealer Report");
        });

        return binding.getRoot();
    }

    private void bindHeaderFromArgs() {
        binding.dealerName.setText(nzText(dealerName));
        binding.dealerOwnerLine.setText(nzText(dealerMobile));
        if (binding.dealerInitials != null) {
            binding.dealerInitials.setText(initials(dealerName));
        }
        LicenseStatusHelper.applyDealerBadge(binding.dealerStatusBadge, dealerActive);
        binding.infoDealerId.setText("Dealer ID: " + nzText(dealerId));
        binding.infoAddress.setText("Address: " + nzText(dealerAddress));
        binding.infoEmail.setText("Email: " + nzText(dealerEmail));
        binding.infoAadhaar.setText("Aadhaar: " + AadhaarMask.mask(dealerAadhaar));
        binding.infoStatus.setText("Status: " + (dealerActive ? "Active" : "Inactive"));
    }

    private void bindKpi(IncludeDashboardKpiCardBinding card, int iconRes, int iconBg, int cardBg,
                         String label, int tintColor) {
        card.kpiCardInner.setBackgroundResource(cardBg);
        card.kpiIcon.setBackgroundResource(iconBg);
        card.kpiIcon.setImageResource(iconRes);
        card.kpiIcon.setColorFilter(ContextCompat.getColor(requireContext(), tintColor));
        card.statLabel.setText(label);
        card.statValue.setText("0");
        card.statTrend.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        if (DetectConnection.checkInternetConnection(activity)) {
            refreshProfile();
            loadReport();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void refreshProfile() {
        Api.getClient().getProfile(dealerId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    AllApiResponse body = response.body();
                    CustomerResponse c = null;
                    if (body.getCustomerResponseList() != null && !body.getCustomerResponseList().isEmpty()) {
                        c = body.getCustomerResponseList().get(0);
                    }
                    if (c != null) {
                        dealerName = c.getName();
                        dealerMobile = c.getContactNumber();
                        dealerEmail = c.getEmail();
                        dealerAddress = c.getAddress();
                        dealerAadhaar = c.getAadharNumber();
                        if (body.getDealerResponseList() != null && !body.getDealerResponseList().isEmpty()) {
                            dealerActive = body.getDealerResponseList().get(0).isActiveDealer();
                        }
                        bindHeaderFromArgs();
                    }
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
            }
        });
    }

    private void loadReport() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().getDealerReport(dealerId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null
                        && "true".equalsIgnoreCase(response.body().getStatus())) {
                    AllApiResponse body = response.body();
                    binding.kpiTotal.statValue.setText(nz(body.getTotalCustomer()));
                    binding.kpiActive.statValue.setText(nz(body.getActiveCustomer()));
                    binding.kpiTrial.statValue.setText(nz(body.getTrialCustomer()));
                    binding.kpiExpired.statValue.setText(nz(body.getExpiredCustomer()));
                    String sales = body.getMonthSales() != null ? body.getMonthSales() : body.getNetSales();
                    binding.monthSales.setText("₹ " + nz(sales));
                    binding.monthCollection.setText("₹ " + nz(body.getCollection()));
                    binding.actionCustomers.setText("Customers (" + nz(body.getTotalCustomer()) + ")");
                    String lic = body.getTotalLicenses() != null ? body.getTotalLicenses() : body.getActiveLicenses();
                    binding.actionLicenses.setText("Licenses (" + nz(lic) + ")");
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(activity, "Unable to load dealer summary", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String nz(String v) {
        return v == null || v.trim().isEmpty() ? "0" : v.trim();
    }

    private static String nzText(String v) {
        return v == null || v.trim().isEmpty() ? "—" : v.trim();
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "D";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            String p = parts[0];
            return p.length() >= 2 ? p.substring(0, 2).toUpperCase() : p.toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
