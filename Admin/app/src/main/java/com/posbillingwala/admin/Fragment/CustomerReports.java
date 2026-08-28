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

public class CustomerReports extends Fragment {
    Activity activity;
    FragmentGenericReportBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGenericReportBinding.inflate(inflater, container, false);
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Customer Reports");
        binding.donutTitle.setText("Customer Status");
        binding.chartTitle.setText("Customer Growth");
        binding.chartBar.setVisibility(View.VISIBLE);
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
        Api.getClient().getCustomerReport().enqueue(new Callback<AllApiResponse>() {
            @Override public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                p.dismiss();
                if (!isAdded() || binding == null || response.body() == null) return;
                AllApiResponse b = response.body();
                ReportUiHelper.bindKpi(binding.kpi1, "Total Customers", ReportUiHelper.nz(b.getTotalCustomer()), "");
                ReportUiHelper.bindKpi(binding.kpi2, "Active Customers", ReportUiHelper.nz(b.getActiveCustomer()), "");
                ReportUiHelper.bindKpi(binding.kpi3, "Trial Customers", ReportUiHelper.nz(b.getTrialCustomer()), "");
                ReportUiHelper.bindKpi(binding.kpi4, "Expired Customers", ReportUiHelper.nz(b.getExpiredCustomer()), "");
                List<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry(parse(b.getActiveCustomer()), "Active"));
                entries.add(new PieEntry(parse(b.getTrialCustomer()), "Trial"));
                entries.add(new PieEntry(parse(b.getExpiredCustomer()), "Expired"));
                ReportUiHelper.setupDonut(binding.chartDonut, entries,
                        Arrays.asList(Color.parseColor("#16A34A"), Color.parseColor("#F59E0B"), Color.parseColor("#DC2626")),
                        ReportUiHelper.nz(b.getTotalCustomer()));
                ReportUiHelper.fillLegend(binding.legendContainer,
                        new String[]{"Active", "Trial", "Expired"},
                        new String[]{b.getActiveCustomer(), b.getTrialCustomer(), b.getExpiredCustomer()},
                        new String[]{b.getActivePercent(), b.getTrialPercent(), b.getExpiredPercent()},
                        new int[]{Color.parseColor("#16A34A"), Color.parseColor("#F59E0B"), Color.parseColor("#DC2626")});
                ReportUiHelper.setupBars(binding.chartBar, b.getGrowthBars());
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

    private float parse(String v) {
        try { return Float.parseFloat(ReportUiHelper.nz(v)); } catch (Exception e) { return 0f; }
    }
}
