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

public class BranchReports extends Fragment {
    Activity activity;
    FragmentGenericReportBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGenericReportBinding.inflate(inflater, container, false);
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Branch Reports");
        binding.donutTitle.setText("Branch Status");
        binding.chartTitle.setText("Top Customers by Branches");
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
        Api.getClient().getBranchReport().enqueue(new Callback<AllApiResponse>() {
            @Override public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                p.dismiss();
                if (!isAdded() || binding == null || response.body() == null) return;
                AllApiResponse b = response.body();
                ReportUiHelper.bindKpi(binding.kpi1, "Total Branches", ReportUiHelper.nz(b.getTotalBranches()), "");
                ReportUiHelper.bindKpi(binding.kpi2, "Active Branches", ReportUiHelper.nz(b.getActiveBranches()), "");
                ReportUiHelper.bindKpi(binding.kpi3, "Inactive Branches", ReportUiHelper.nz(b.getInactiveBranches()), "");
                ReportUiHelper.bindKpi(binding.kpi4, "New Branches", ReportUiHelper.nz(b.getNewBranches()), "This Month");
                List<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry(f(b.getActiveBranches()), "Active"));
                entries.add(new PieEntry(f(b.getInactiveBranches()), "Inactive"));
                entries.add(new PieEntry(f(b.getNewBranches()), "New"));
                ReportUiHelper.setupDonut(binding.chartDonut, entries,
                        Arrays.asList(Color.parseColor("#16A34A"), Color.parseColor("#F59E0B"), Color.parseColor("#2563EB")),
                        ReportUiHelper.nz(b.getTotalBranches()));
                ReportUiHelper.fillLegend(binding.legendContainer,
                        new String[]{"Active", "Inactive", "New"},
                        new String[]{b.getActiveBranches(), b.getInactiveBranches(), b.getNewBranches()},
                        new String[]{b.getActivePercent(), b.getInactivePercent(), b.getNewPercent()},
                        new int[]{Color.parseColor("#16A34A"), Color.parseColor("#F59E0B"), Color.parseColor("#2563EB")});
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
