package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.ReportUiHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.InvoiceSaleResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentSalesDashboardBinding;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SalesDashboard extends Fragment {
    Activity activity;
    FragmentSalesDashboardBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSalesDashboardBinding.inflate(inflater, container, false);
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Sales Dashboard");
        binding.viewAllBills.setOnClickListener(v ->
                ((MainActivity) activity).navigateDetail(new SalesList(), "Sales List"));
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(0);
        if (DetectConnection.checkInternetConnection(activity)) load();
        else DetectConnection.noInternetConnection(activity);
    }

    private void load() {
        SweetAlertDialog p = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        p.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
        p.setTitleText("Loading");
        p.setCancelable(false);
        p.show();
        Api.getClient().getSalesDashboard().enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                p.dismiss();
                if (!isAdded() || binding == null || response.body() == null) return;
                AllApiResponse b = response.body();
                binding.dateChip.setText(b.getPeriodLabel() != null ? b.getPeriodLabel() : "Today");
                ReportUiHelper.bindKpi(binding.kpi1, "Total Sales", ReportUiHelper.money(b.getTotalSales()), b.getTotalSalesTrend());
                ReportUiHelper.bindKpi(binding.kpi2, "Net Sales", ReportUiHelper.money(b.getNetSalesValue()), b.getNetSalesTrend());
                ReportUiHelper.bindKpi(binding.kpi3, "Total Bills", ReportUiHelper.nz(b.getTotalInvoices()), b.getInvoicesTrend());
                ReportUiHelper.bindKpi(binding.kpi4, "Avg. Bill Value", ReportUiHelper.money(b.getAvgBill()), b.getAvgBillTrend());
                ReportUiHelper.setupLine(binding.chartTrend, b.getSalesTrend());
                bindBills(b.getRecentInvoices());
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                p.dismiss();
            }
        });
    }

    private void bindBills(List<InvoiceSaleResponse> bills) {
        binding.billsContainer.removeAllViews();
        if (bills == null || bills.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText("No recent bills");
            empty.setTextColor(ContextCompat.getColor(activity, R.color.colorTextHint));
            binding.billsContainer.addView(empty);
            return;
        }
        for (InvoiceSaleResponse inv : bills) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);
            LinearLayout mid = new LinearLayout(activity);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView title = new TextView(activity);
            title.setText(inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : ("#" + inv.getInvoiceId()));
            title.setTextColor(ContextCompat.getColor(activity, R.color.colorTextPrimary));
            title.setTextSize(14f);
            TextView sub = new TextView(activity);
            sub.setText((inv.getCustomerName() != null ? inv.getCustomerName() : "Customer")
                    + "  ·  " + (inv.getInvoiceDate() != null ? inv.getInvoiceDate() : ""));
            sub.setTextColor(ContextCompat.getColor(activity, R.color.colorTextSecondary));
            sub.setTextSize(11f);
            mid.addView(title);
            mid.addView(sub);
            TextView amt = new TextView(activity);
            amt.setText(ReportUiHelper.money(inv.getTotalAmount()));
            amt.setTextColor(ContextCompat.getColor(activity, R.color.colorTextPrimary));
            TextView badge = new TextView(activity);
            badge.setText(inv.getPaymentStatus());
            badge.setBackgroundResource(R.drawable.bg_badge_active);
            badge.setTextColor(ContextCompat.getColor(activity, R.color.statusActive));
            badge.setPadding(16, 6, 16, 6);
            badge.setTextSize(10f);
            LinearLayout right = new LinearLayout(activity);
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(android.view.Gravity.END);
            right.addView(amt);
            right.addView(badge);
            row.addView(mid);
            row.addView(right);
            row.setOnClickListener(v -> {
                SalesDetails d = new SalesDetails();
                Bundle b = new Bundle();
                b.putString("invoiceId", inv.getInvoiceId());
                d.setArguments(b);
                ((MainActivity) activity).navigateDetail(d, "Sales Details");
            });
            binding.billsContainer.addView(row);
        }
    }
}
