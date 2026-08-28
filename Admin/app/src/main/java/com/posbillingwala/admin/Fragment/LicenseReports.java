package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.data.PieEntry;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.ReportUiHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentGenericReportBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LicenseReports extends Fragment {
    Activity activity;
    FragmentGenericReportBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGenericReportBinding.inflate(inflater, container, false);
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("License Reports");
        binding.donutTitle.setText("License Status");
        binding.chartTitle.setText("License Expiry (Next 30 Days)");
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        if (DetectConnection.checkInternetConnection(activity)) load();
        else DetectConnection.noInternetConnection(activity);
    }

    private void load() {
        SweetAlertDialog p = busy();
        Api.getClient().getLicenseReport().enqueue(new Callback<AllApiResponse>() {
            @Override public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                p.dismiss();
                if (!isAdded() || binding == null || response.body() == null) return;
                AllApiResponse b = response.body();
                ReportUiHelper.bindKpi(binding.kpi1, "Active Licenses", ReportUiHelper.nz(b.getActiveLicenses()), "");
                ReportUiHelper.bindKpi(binding.kpi2, "Trial Licenses", ReportUiHelper.nz(b.getTrialLicenses()), "");
                ReportUiHelper.bindKpi(binding.kpi3, "Expiring Licenses", ReportUiHelper.nz(b.getExpiringLicenses()), "");
                ReportUiHelper.bindKpi(binding.kpi4, "Expired Licenses", ReportUiHelper.nz(b.getExpiredLicenses()), "");
                List<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry(f(b.getActiveLicenses()), "Active"));
                entries.add(new PieEntry(f(b.getTrialLicenses()), "Trial"));
                entries.add(new PieEntry(f(b.getExpiringLicenses()), "Expiring"));
                entries.add(new PieEntry(f(b.getExpiredLicenses()), "Expired"));
                ReportUiHelper.setupDonut(binding.chartDonut, entries,
                        Arrays.asList(Color.parseColor("#16A34A"), Color.parseColor("#F59E0B"),
                                Color.parseColor("#EAB308"), Color.parseColor("#DC2626")),
                        ReportUiHelper.nz(b.getTotalLicenses()));
                ReportUiHelper.fillLegend(binding.legendContainer,
                        new String[]{"Active", "Trial", "Expiring", "Expired"},
                        new String[]{b.getActiveLicenses(), b.getTrialLicenses(), b.getExpiringLicenses(), b.getExpiredLicenses()},
                        new String[]{b.getActivePercent(), b.getTrialPercent(), b.getExpiringPercent(), b.getExpiredPercent()},
                        new int[]{Color.parseColor("#16A34A"), Color.parseColor("#F59E0B"),
                                Color.parseColor("#EAB308"), Color.parseColor("#DC2626")});
                ReportUiHelper.fillRankList(binding.listContainer, b.getExpiryWindows(), false);
            }
            @Override public void onFailure(Call<AllApiResponse> call, Throwable t) { p.dismiss(); }
        });
    }

    private SweetAlertDialog busy() {
        SweetAlertDialog p = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        p.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
        p.setTitleText("Loading");
        p.setCancelable(false);
        p.show();
        return p;
    }
    private float f(String v) { try { return Float.parseFloat(ReportUiHelper.nz(v)); } catch (Exception e) { return 0f; } }
}
