package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.LocalSalesAnalytics;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.Extra.ReportUiHelper;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.LocalSalesSnapshot;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentSalesDashboardBinding;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class SalesDashboard extends Fragment {

    private Activity activity;
    private FragmentSalesDashboardBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSalesDashboardBinding.inflate(inflater, container, false);
        activity = getActivity();

        binding.toolbar.toolbarTitle.setText(getString(R.string.ui_sales_dashboard));
        binding.toolbar.backButton.setOnClickListener(v -> ((MainActivity) activity).navigateBack());
        binding.viewAllBills.setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new SalesList(), true));

        View root = binding.getRoot();
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                ((MainActivity) activity).navigateBack();
                return true;
            }
            return false;
        });
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        loadDashboard();
    }

    private void loadDashboard() {
        SweetAlertDialog loader = ListLoader.show(activity);
        executor.execute(() -> {
            LocalSalesSnapshot snapshot = new LocalSalesAnalytics(activity).loadTodayDashboard();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(() -> {
                ListLoader.dismiss(loader);
                if (!isAdded() || binding == null) {
                    return;
                }
                bindSnapshot(snapshot);
            });
        });
    }

    private void bindSnapshot(LocalSalesSnapshot snapshot) {
        String currency = MainActivity.currencyName;
        binding.dateChip.setText(snapshot.getPeriodLabel());
        ReportUiHelper.bindKpi(binding.kpi1, getString(R.string.ui_total_sales),
                ReportUiHelper.money(currency, snapshot.getTotalSales()), snapshot.getTotalSalesTrend());
        ReportUiHelper.bindKpi(binding.kpi2, getString(R.string.ui_net_sales),
                ReportUiHelper.money(currency, snapshot.getNetSales()), snapshot.getNetSalesTrend());
        ReportUiHelper.bindKpi(binding.kpi3, getString(R.string.ui_total_bills),
                String.valueOf(snapshot.getBillCount()), snapshot.getBillCountTrend());
        ReportUiHelper.bindKpi(binding.kpi4, getString(R.string.ui_avg_bill),
                ReportUiHelper.money(currency, snapshot.getAvgBill()), snapshot.getAvgBillTrend());
        ReportUiHelper.setupLine(binding.chartTrend, snapshot.getSalesTrend());
        bindRecentBills(snapshot.getRecentInvoices());
    }

    private void bindRecentBills(List<InvoiceResponse> bills) {
        binding.billsContainer.removeAllViews();
        if (bills == null || bills.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText(R.string.ui_no_sales_found);
            empty.setTextColor(ContextCompat.getColor(activity, R.color.colorTextHint));
            binding.billsContainer.addView(empty);
            return;
        }
        String currency = MainActivity.currencyName;
        for (InvoiceResponse inv : bills) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);

            LinearLayout mid = new LinearLayout(activity);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView title = new TextView(activity);
            title.setText(inv.getInvoiceNumber() != null ? inv.getInvoiceNumber()
                    : ("#" + inv.getInvoiceId()));
            title.setTextColor(ContextCompat.getColor(activity, R.color.colorTextPrimary));
            title.setTextSize(14f);

            TextView sub = new TextView(activity);
            String customer = inv.getCustomerName() != null && !inv.getCustomerName().trim().isEmpty()
                    ? inv.getCustomerName() : getString(R.string.ui_customer_name);
            sub.setText(customer + " · " + ReportCursorHelper.formatInvoiceDate(inv.getInvoiceDate()));
            sub.setTextColor(ContextCompat.getColor(activity, R.color.colorTextSecondary));
            sub.setTextSize(11f);
            mid.addView(title);
            mid.addView(sub);

            LinearLayout right = new LinearLayout(activity);
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(android.view.Gravity.END);

            TextView amt = new TextView(activity);
            amt.setText(ReportUiHelper.money(currency, inv.getTotalAmount()));
            amt.setTextColor(ContextCompat.getColor(activity, R.color.colorTextPrimary));
            amt.setTextSize(13f);

            TextView badge = new TextView(activity);
            String mode = inv.getPaymentMode() != null && !inv.getPaymentMode().trim().isEmpty()
                    ? inv.getPaymentMode() : "Paid";
            badge.setText(mode);
            badge.setBackgroundResource(R.drawable.bg_badge_active);
            badge.setTextColor(ContextCompat.getColor(activity, R.color.statusActive));
            badge.setPadding(16, 6, 16, 6);
            badge.setTextSize(10f);
            right.addView(amt);
            right.addView(badge);

            row.addView(mid);
            row.addView(right);

            LinearLayout wrapper = new LinearLayout(activity);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.addView(row);
            View divider = new View(activity);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(ContextCompat.getColor(activity, R.color.colorBorder));
            wrapper.addView(divider);
            binding.billsContainer.addView(wrapper);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
