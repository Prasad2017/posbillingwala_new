package com.posbillingwala.owner.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Extra.ReportUiHelper;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.BranchComparisonResponse;
import com.posbillingwala.owner.Model.InvoiceResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentSalesDashboardBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SalesDashboard extends Fragment {

    private Activity activity;
    private FragmentSalesDashboardBinding binding;
    private String selectedBranchId = "all";
    private final List<BranchComparisonResponse> branchOptions = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSalesDashboardBinding.inflate(inflater, container, false);
        activity = getActivity();
        if (getArguments() != null && getArguments().getString("branchId") != null
                && !getArguments().getString("branchId").trim().isEmpty()) {
            selectedBranchId = getArguments().getString("branchId");
        } else if (MainActivity.branchCount <= 1) {
            selectedBranchId = "all";
        }

        binding.toolbar.toolbarTitle.setText(getString(R.string.sales_dashboard));
        binding.toolbar.backButton.setOnClickListener(v -> navigateBack());
        binding.viewAllBills.setOnClickListener(v -> openStoreWise());
        binding.dateChip.setOnClickListener(v -> {
            if (branchOptions.size() > 1) {
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

    private void openStoreWise() {
        InvoiceStoreWise fragment = new InvoiceStoreWise();
        Bundle args = new Bundle();
        args.putString("saleDate", "todaySale");
        fragment.setArguments(args);
        ((MainActivity) activity).loadFragment(fragment, true);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DetectConnection.checkInternetConnection(activity)) {
            if (branchOptions.isEmpty()) {
                preloadBranches();
            } else {
                loadDashboard();
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
                if (!branchOptions.isEmpty()) {
                    MainActivity.setOutletCounts(branchOptions.size());
                }
                loadDashboard();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                loadDashboard();
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
                    loadDashboard();
                });
    }

    private String scopeLabel(String periodLabel) {
        String period = periodLabel != null ? periodLabel : getString(R.string.ui_today);
        if (branchOptions.size() <= 1) {
            return period;
        }
        String branchName = getString(R.string.all_branches);
        if (!"all".equals(selectedBranchId)) {
            for (BranchComparisonResponse b : branchOptions) {
                if (selectedBranchId.equals(b.getBranchId())) {
                    if (b.getBranchLabel() != null && !b.getBranchLabel().isEmpty()) {
                        branchName = b.getBranchLabel();
                    } else if (b.getShopName1() != null) {
                        branchName = b.getShopName1();
                    }
                    break;
                }
            }
        }
        return period + " · " + branchName;
    }

    private void loadDashboard() {
        SweetAlertDialog loader = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        loader.getProgressHelper().setBarColor(Color.parseColor("#4862b7"));
        loader.setTitleText(getString(R.string.loading));
        loader.setCancelable(false);
        loader.show();

        Api.getClient().getSalesDashboard(MainActivity.userId, selectedBranchId)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                        loader.dismiss();
                        if (!isAdded() || binding == null || response.body() == null) return;
                        AllApiResponse body = response.body();
                        if (!"true".equalsIgnoreCase(body.getStatus())) return;

                        binding.dateChip.setText(scopeLabel(body.getPeriodLabel()));
                        ReportUiHelper.bindKpi(binding.kpi1, getString(R.string.total_sales),
                                ReportUiHelper.money(body.getTotalSales()), body.getTotalSalesTrend());
                        ReportUiHelper.bindKpi(binding.kpi2, getString(R.string.net_sales),
                                ReportUiHelper.money(body.getNetSales()), body.getNetSalesTrend());
                        ReportUiHelper.bindKpi(binding.kpi3, getString(R.string.total_bills),
                                body.getTotalInvoices(), body.getInvoicesTrend());
                        ReportUiHelper.bindKpi(binding.kpi4, getString(R.string.avg_bill),
                                ReportUiHelper.money(body.getAvgBill()), body.getAvgBillTrend());
                        ReportUiHelper.setupLine(binding.chartTrend, body.getSalesTrend());
                        bindRecentBills(body.getRecentInvoices());
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        loader.dismiss();
                    }
                });
    }

    private void bindRecentBills(List<InvoiceResponse> bills) {
        binding.billsContainer.removeAllViews();
        if (bills == null || bills.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText(R.string.no_sales_found);
            empty.setTextColor(ContextCompat.getColor(activity, R.color.colorTextHint));
            binding.billsContainer.addView(empty);
            return;
        }
        for (InvoiceResponse inv : bills) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);

            LinearLayout mid = new LinearLayout(activity);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView title = new TextView(activity);
            title.setText(inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : ("#" + inv.getInvoiceId()));
            title.setTextColor(ContextCompat.getColor(activity, R.color.colorTextPrimary));

            TextView sub = new TextView(activity);
            String branch = inv.getCustomerName() != null ? inv.getCustomerName() : "";
            sub.setText(branch + " · " + (inv.getInvoiceDate() != null ? inv.getInvoiceDate() : ""));
            sub.setTextColor(ContextCompat.getColor(activity, R.color.colorTextSecondary));
            sub.setTextSize(11f);
            mid.addView(title);
            mid.addView(sub);

            TextView amt = new TextView(activity);
            amt.setText(ReportUiHelper.money(inv.getTotalAmount()));
            amt.setTextColor(ContextCompat.getColor(activity, R.color.colorTextPrimary));
            row.addView(mid);
            row.addView(amt);
            binding.billsContainer.addView(row);
        }
    }
}
