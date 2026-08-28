package com.posbillingwala.owner.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.databinding.FragmentReportsHubBinding;
import com.posbillingwala.owner.databinding.ItemReportMenuRowBinding;

public class ReportsHub extends Fragment {

    private Activity activity;
    private FragmentReportsHubBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReportsHubBinding.inflate(inflater, container, false);
        activity = getActivity();

        binding.toolbar.toolbarTitle.setText(getString(R.string.reports_hub));
        binding.toolbar.backButton.setOnClickListener(v -> navigateBack());

        setupRow(binding.rowSalesDashboard, R.drawable.ic_report_dashboard, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.sales_dashboard),
                MainActivity.branchCount > 1 ? getString(R.string.owner_multi_branch) : getString(R.string.sales_trend));
        setupRow(binding.rowSalesOverview, R.drawable.ic_report_overview, R.drawable.bg_quick_action_purple,
                R.color.deepPurple, getString(R.string.sales_overview),
                MainActivity.branchCount > 1 ? getString(R.string.owner_multi_branch) : getString(R.string.top_branches));
        setupRow(binding.rowInvoiceReport, R.drawable.ic_business, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.branch_comparison), getString(R.string.all_branches));
        setupRow(binding.rowSaleReport, R.drawable.ic_store, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.store_wise_sales), getString(R.string.total_today_sales));
        setupRow(binding.rowTableReport, R.drawable.ic_report_invoice, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.order_invoices), getString(R.string.invoice_list));

        hideUnusedRows();

        binding.rowSalesDashboard.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new SalesDashboard(), true));
        binding.rowSalesOverview.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new SalesOverview(), true));
        binding.rowInvoiceReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new BranchComparison(), true));
        binding.rowSaleReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new InvoiceStoreWise(), true));
        binding.rowTableReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new OrderInvoice(), true));

        if (MainActivity.branchCount <= 1) {
            binding.rowInvoiceReport.getRoot().setVisibility(View.GONE);
        }

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

    private void hideUnusedRows() {
        binding.rowTakeAwayReport.getRoot().setVisibility(View.GONE);
        binding.rowPaymentReport.getRoot().setVisibility(View.GONE);
        binding.rowProductReport.getRoot().setVisibility(View.GONE);
        binding.rowComboReport.getRoot().setVisibility(View.GONE);
        binding.rowExpenseReport.getRoot().setVisibility(View.GONE);
        binding.rowMessMemberReport.getRoot().setVisibility(View.GONE);
        binding.rowMessReport.getRoot().setVisibility(View.GONE);
        binding.rowDeleteAllInvoices.getRoot().setVisibility(View.GONE);
    }

    private void setupRow(ItemReportMenuRowBinding row, int iconRes, int bgRes, int tintColor,
                          String title, String subtitle) {
        row.menuIcon.setBackgroundResource(bgRes);
        row.menuIcon.setImageResource(iconRes);
        row.menuIcon.clearColorFilter();
        row.menuIcon.setColorFilter(ContextCompat.getColor(requireContext(), tintColor));
        row.menuTitle.setText(title);
        row.menuSubtitle.setText(subtitle);
    }

    private void navigateBack() {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
    }
}
