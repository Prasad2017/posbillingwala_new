package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.ReportUiHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentSalesOverviewBinding;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SalesOverview extends Fragment {

    Activity activity;
    FragmentSalesOverviewBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSalesOverviewBinding.inflate(inflater, container, false);
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Sales Overview");
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        if (DetectConnection.checkInternetConnection(activity)) {
            load();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void load() {
        SweetAlertDialog p = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        p.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
        p.setTitleText("Loading");
        p.setCancelable(false);
        p.show();
        Api.getClient().getSalesOverviewReport().enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                p.dismiss();
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null
                        && "true".equalsIgnoreCase(response.body().getStatus())) {
                    AllApiResponse b = response.body();
                    binding.dateChip.setText(ReportUiHelper.nz(b.getPeriodLabel()).isEmpty() || "0".equals(b.getPeriodLabel())
                            ? "This Month" : b.getPeriodLabel());
                    ReportUiHelper.bindKpi(binding.kpi1, "Total Sales", ReportUiHelper.money(b.getTotalSales()), b.getTotalSalesTrend());
                    ReportUiHelper.bindKpi(binding.kpi2, "Net Sales", ReportUiHelper.money(b.getNetSalesValue()), b.getNetSalesTrend());
                    ReportUiHelper.bindKpi(binding.kpi3, "Total Invoices", ReportUiHelper.nz(b.getTotalInvoices()), b.getInvoicesTrend());
                    ReportUiHelper.bindKpi(binding.kpi4, "Avg. Bill Value", ReportUiHelper.money(b.getAvgBill()), b.getAvgBillTrend());
                    ReportUiHelper.setupLine(binding.chartSalesTrend, b.getSalesTrend());
                    ReportUiHelper.fillRankList(binding.topCustomersContainer, b.getTopCustomers(), true);
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                p.dismiss();
            }
        });
    }
}
