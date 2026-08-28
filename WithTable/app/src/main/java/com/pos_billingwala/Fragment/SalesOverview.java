package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.LocalSalesAnalytics;
import com.pos_billingwala.Extra.ReportUiHelper;
import com.pos_billingwala.Model.LocalSalesSnapshot;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentSalesOverviewBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class SalesOverview extends Fragment {

    private Activity activity;
    private FragmentSalesOverviewBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSalesOverviewBinding.inflate(inflater, container, false);
        activity = getActivity();

        binding.toolbar.toolbarTitle.setText(getString(R.string.ui_sales_overview));
        binding.toolbar.backButton.setOnClickListener(v -> ((MainActivity) activity).navigateBack());

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
        loadOverview();
    }

    private void loadOverview() {
        SweetAlertDialog loader = ListLoader.show(activity);
        executor.execute(() -> {
            LocalSalesSnapshot snapshot = new LocalSalesAnalytics(activity).loadMonthlyOverview();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(() -> {
                ListLoader.dismiss(loader);
                if (!isAdded() || binding == null) {
                    return;
                }
                String currency = MainActivity.currencyName;
                binding.dateChip.setText(snapshot.getPeriodLabel());
                ReportUiHelper.bindKpi(binding.kpi1, getString(R.string.ui_total_sales),
                        ReportUiHelper.money(currency, snapshot.getTotalSales()), snapshot.getTotalSalesTrend());
                ReportUiHelper.bindKpi(binding.kpi2, getString(R.string.ui_net_sales),
                        ReportUiHelper.money(currency, snapshot.getNetSales()), snapshot.getNetSalesTrend());
                ReportUiHelper.bindKpi(binding.kpi3, getString(R.string.ui_total_invoices),
                        String.valueOf(snapshot.getBillCount()), snapshot.getBillCountTrend());
                ReportUiHelper.bindKpi(binding.kpi4, getString(R.string.ui_avg_bill),
                        ReportUiHelper.money(currency, snapshot.getAvgBill()), snapshot.getAvgBillTrend());
                ReportUiHelper.setupLine(binding.chartSalesTrend, snapshot.getSalesTrend());
                ReportUiHelper.fillRankList(binding.topCustomersContainer, snapshot.getTopCustomers(), true);
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
