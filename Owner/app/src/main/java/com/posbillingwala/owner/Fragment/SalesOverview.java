package com.posbillingwala.owner.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Extra.ReportUiHelper;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.BranchComparisonResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentSalesOverviewBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SalesOverview extends Fragment {

    private Activity activity;
    private FragmentSalesOverviewBinding binding;
    private String selectedBranchId = "all";
    private final List<BranchComparisonResponse> branchOptions = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSalesOverviewBinding.inflate(inflater, container, false);
        activity = getActivity();

        binding.toolbar.toolbarTitle.setText(getString(R.string.sales_overview));
        binding.toolbar.backButton.setOnClickListener(v -> navigateBack());
        binding.dateChip.setOnClickListener(v -> {
            if (MainActivity.branchCount > 1) {
                showBranchPicker();
            }
        });

        View root = binding.getRoot();
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                navigateBack();
                return true;
            }
            return false;
        });
        return root;
    }

    private void navigateBack() {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DetectConnection.checkInternetConnection(activity)) {
            if (MainActivity.branchCount > 1 && branchOptions.isEmpty()) {
                preloadBranches();
            } else {
                loadOverview();
            }
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void preloadBranches() {
        Api.getClient().getBranchComparison(MainActivity.userId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                branchOptions.clear();
                if (response.body() != null && response.body().getBranchComparisonList() != null) {
                    branchOptions.addAll(response.body().getBranchComparisonList());
                }
                loadOverview();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                loadOverview();
            }
        });
    }

    private void showBranchPicker() {
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.all_branches));
        final List<String> ids = new ArrayList<>();
        ids.add("all");
        for (BranchComparisonResponse b : branchOptions) {
            labels.add(b.getBranchLabel() != null ? b.getBranchLabel() : b.getShopName1());
            ids.add(b.getBranchId());
        }
        int selectedIndex = 0;
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i).equals(selectedBranchId)) {
                selectedIndex = i;
                break;
            }
        }
        BottomSheetUi.showSingleChoice(activity, getString(R.string.select_branch),
                labels.toArray(new String[0]), selectedIndex, false, index -> {
                    selectedBranchId = ids.get(index);
                    loadOverview();
                });
    }

    private void loadOverview() {
        SweetAlertDialog loader = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        loader.getProgressHelper().setBarColor(Color.parseColor("#4862b7"));
        loader.setTitleText(getString(R.string.loading));
        loader.setCancelable(false);
        loader.show();

        Api.getClient().getSalesOverviewReport(MainActivity.userId, selectedBranchId)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                        loader.dismiss();
                        if (!isAdded() || binding == null || response.body() == null) return;
                        AllApiResponse body = response.body();
                        if (!"true".equalsIgnoreCase(body.getStatus())) return;

                        binding.dateChip.setText(body.getPeriodLabel());
                        ReportUiHelper.bindKpi(binding.kpi1, getString(R.string.total_sales),
                                ReportUiHelper.money(body.getTotalSales()), body.getTotalSalesTrend());
                        ReportUiHelper.bindKpi(binding.kpi2, getString(R.string.net_sales),
                                ReportUiHelper.money(body.getNetSales()), body.getNetSalesTrend());
                        ReportUiHelper.bindKpi(binding.kpi3, getString(R.string.total_invoices),
                                body.getTotalInvoices(), body.getInvoicesTrend());
                        ReportUiHelper.bindKpi(binding.kpi4, getString(R.string.avg_bill),
                                ReportUiHelper.money(body.getAvgBill()), body.getAvgBillTrend());
                        ReportUiHelper.setupLine(binding.chartSalesTrend, body.getSalesTrend());
                        ReportUiHelper.fillRankList(binding.topCustomersContainer, body.getTopCustomers(), true);
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        loader.dismiss();
                    }
                });
    }
}
