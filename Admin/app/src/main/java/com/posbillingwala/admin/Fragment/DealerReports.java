package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.ReportUiHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.DealerResponse;
import com.posbillingwala.admin.Model.DealerSalesResponse;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentGenericReportBinding;
import com.posbillingwala.admin.databinding.ItemReportRankRowBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DealerReports extends Fragment {
    Activity activity;
    FragmentGenericReportBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGenericReportBinding.inflate(inflater, container, false);
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Dealer Reports");
        binding.donutTitle.setText("Dealer Status");
        binding.chartTitle.setText("Top Dealers by Sales");
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
        Api.getClient().getDealerList().enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || binding == null) {
                    p.dismiss();
                    return;
                }
                List<DealerResponse> dealers = response.body() != null ? response.body().getDealerResponseList() : null;
                int total = dealers != null ? dealers.size() : 0;
                int active = 0;
                int customers = 0;
                if (dealers != null) {
                    for (DealerResponse d : dealers) {
                        if (d.isActiveDealer()) active++;
                        try {
                            customers += Integer.parseInt(ReportUiHelper.nz(d.getTotalCustomer()));
                        } catch (Exception ignored) {
                        }
                    }
                }
                int inactive = Math.max(0, total - active);
                ReportUiHelper.bindKpi(binding.kpi1, "Total Dealers", String.valueOf(total), "");
                ReportUiHelper.bindKpi(binding.kpi2, "Active Dealers", String.valueOf(active), "");
                ReportUiHelper.bindKpi(binding.kpi3, "Total Customers", String.valueOf(customers), "");
                List<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry(active, "Active"));
                entries.add(new PieEntry(inactive, "Inactive"));
                ReportUiHelper.setupDonut(binding.chartDonut, entries,
                        Arrays.asList(Color.parseColor("#16A34A"), Color.parseColor("#6B7280")),
                        String.valueOf(total));
                ReportUiHelper.fillLegend(binding.legendContainer,
                        new String[]{"Active", "Inactive"},
                        new String[]{String.valueOf(active), String.valueOf(inactive)},
                        new String[]{
                                total > 0 ? String.valueOf(Math.round(active * 100f / total)) : "0",
                                total > 0 ? String.valueOf(Math.round(inactive * 100f / total)) : "0"
                        },
                        new int[]{Color.parseColor("#16A34A"), Color.parseColor("#6B7280")});
                loadSales(p);
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                p.dismiss();
            }
        });
    }

    private void loadSales(SweetAlertDialog p) {
        Api.getClient().getDealerSalesOverview(8).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                p.dismiss();
                if (!isAdded() || binding == null || response.body() == null) return;
                AllApiResponse b = response.body();
                ReportUiHelper.bindKpi(binding.kpi4, "Total Sales", ReportUiHelper.money(b.getTotalSales()), "");
                List<DealerSalesResponse> sales = b.getDealerSalesResponseList();
                binding.listContainer.removeAllViews();
                List<BarEntry> entries = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                if (sales != null) {
                    for (int i = 0; i < sales.size(); i++) {
                        DealerSalesResponse s = sales.get(i);
                        String name = s.getDealerName() != null ? s.getDealerName() : "Dealer";
                        ItemReportRankRowBinding row = ItemReportRankRowBinding.inflate(
                                LayoutInflater.from(activity), binding.listContainer, false);
                        row.rankInitials.setText(ReportUiHelper.initials(name));
                        row.rankTitle.setText(name);
                        row.rankSubtitle.setText("Sales");
                        row.rankValue.setText(ReportUiHelper.money(s.getTotalSales()));
                        binding.listContainer.addView(row.getRoot());
                        float y = 0f;
                        try {
                            y = Float.parseFloat(ReportUiHelper.nz(s.getTotalSales()));
                        } catch (Exception ignored) {
                        }
                        entries.add(new BarEntry(i, y));
                        labels.add(name.length() > 6 ? name.substring(0, 6) : name);
                    }
                }
                if (!entries.isEmpty()) {
                    BarDataSet ds = new BarDataSet(entries, "");
                    ds.setColor(Color.parseColor("#F59E0B"));
                    ds.setDrawValues(false);
                    binding.chartBar.getDescription().setEnabled(false);
                    binding.chartBar.getLegend().setEnabled(false);
                    binding.chartBar.getAxisRight().setEnabled(false);
                    binding.chartBar.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                    binding.chartBar.setData(new BarData(ds));
                    binding.chartBar.invalidate();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                p.dismiss();
            }
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
}
