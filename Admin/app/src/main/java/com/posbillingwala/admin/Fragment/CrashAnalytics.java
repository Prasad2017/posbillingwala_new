package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.data.PieEntry;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.ReportUiHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.ReportRankItem;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentGenericReportBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class CrashAnalytics extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Activity a = getActivity();
        ((MainActivity) a).setScreenTitle("Crash Analytics");
        FragmentGenericReportBinding binding = FragmentGenericReportBinding.inflate(inflater, container, false);
        binding.kpi1.getRoot().setVisibility(View.GONE);
        binding.kpi2.getRoot().setVisibility(View.GONE);
        binding.kpi3.getRoot().setVisibility(View.GONE);
        binding.kpi4.getRoot().setVisibility(View.GONE);
        binding.donutTitle.setText("Crashes by App");
        binding.chartTitle.setText("Crashes Over Time");
        binding.chartLine.setVisibility(View.VISIBLE);
        Api.getClient().getCrashAnalytics().enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || response.body() == null) return;
                AllApiResponse b = response.body();
                binding.dateChip.setVisibility(View.VISIBLE);
                binding.dateChip.setText(ReportUiHelper.nz(b.getPeriodLabel()));
                List<PieEntry> entries = new ArrayList<>();
                List<Integer> colors = Arrays.asList(Color.parseColor("#16A34A"), Color.parseColor("#F59E0B"), Color.parseColor("#7C3AED"));
                List<ReportRankItem> byApp = b.getByApp();
                if (byApp != null) {
                    String[] labels = new String[byApp.size()];
                    String[] counts = new String[byApp.size()];
                    String[] percents = new String[byApp.size()];
                    int[] cols = new int[byApp.size()];
                    for (int i = 0; i < byApp.size(); i++) {
                        ReportRankItem it = byApp.get(i);
                        entries.add(new PieEntry(f(it.getCount()), it.getLabel()));
                        labels[i] = it.getLabel();
                        counts[i] = it.getCount();
                        percents[i] = it.getPercent();
                        cols[i] = colors.get(i % colors.size());
                    }
                    ReportUiHelper.setupDonut(binding.chartDonut, entries, colors, ReportUiHelper.nz(b.getTotalCrashes()));
                    ReportUiHelper.fillLegend(binding.legendContainer, labels, counts, percents, cols);
                }
                ReportUiHelper.setupLine(binding.chartLine, b.getOverTime());
                ReportUiHelper.fillRankList(binding.listContainer, b.getTopErrors(), false);
            }
            @Override public void onFailure(Call<AllApiResponse> call, Throwable t) {}
        });
        return binding.getRoot();
    }
    private float f(String v) { try { return Float.parseFloat(ReportUiHelper.nz(v)); } catch (Exception e) { return 0f; } }
    @Override public void onStart() { super.onStart(); ((MainActivity) getActivity()).lockUnlockDrawer(1); }
}