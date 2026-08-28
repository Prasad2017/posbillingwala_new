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

public class DeviceReports extends Fragment {
    Activity activity;
    FragmentGenericReportBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGenericReportBinding.inflate(inflater, container, false);
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Device Reports");
        binding.donutTitle.setText("Device Status");
        binding.chartTitle.setText("Top Customers by Devices");
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
        Api.getClient().getDeviceReport().enqueue(new Callback<AllApiResponse>() {
            @Override public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                p.dismiss();
                if (!isAdded() || binding == null || response.body() == null) return;
                AllApiResponse b = response.body();
                ReportUiHelper.bindKpi(binding.kpi1, "Total Devices", ReportUiHelper.nz(b.getTotalDevices()), "");
                ReportUiHelper.bindKpi(binding.kpi2, "Active Devices", ReportUiHelper.nz(b.getActiveDevices()), "");
                ReportUiHelper.bindKpi(binding.kpi3, "Inactive Devices", ReportUiHelper.nz(b.getInactiveDevices()), "");
                ReportUiHelper.bindKpi(binding.kpi4, "Not Used (30+ Days)", ReportUiHelper.nz(b.getNotUsedDevices()), "");
                List<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry(f(b.getActiveDevices()), "Active"));
                entries.add(new PieEntry(f(b.getInactiveDevices()), "Inactive"));
                entries.add(new PieEntry(f(b.getNotUsedDevices()), "Not Used"));
                ReportUiHelper.setupDonut(binding.chartDonut, entries,
                        Arrays.asList(Color.parseColor("#16A34A"), Color.parseColor("#F59E0B"), Color.parseColor("#DC2626")),
                        ReportUiHelper.nz(b.getTotalDevices()));
                ReportUiHelper.fillLegend(binding.legendContainer,
                        new String[]{"Active", "Inactive", "Not Used"},
                        new String[]{b.getActiveDevices(), b.getInactiveDevices(), b.getNotUsedDevices()},
                        new String[]{b.getActivePercent(), b.getInactivePercent(), b.getNotUsedPercent()},
                        new int[]{Color.parseColor("#16A34A"), Color.parseColor("#F59E0B"), Color.parseColor("#DC2626")});
                ReportUiHelper.fillRankList(binding.listContainer, b.getTopCustomers(), false);
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
