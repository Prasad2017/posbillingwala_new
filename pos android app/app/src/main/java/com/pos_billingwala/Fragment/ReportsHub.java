package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Extra.LicenseModules;
import com.pos_billingwala.NetworkToOffline.UserSynchronizeData;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentReportsHubBinding;
import com.pos_billingwala.databinding.ItemGroupedMenuRowBinding;

public class ReportsHub extends Fragment {

    private Activity activity;
    private FragmentReportsHubBinding binding;
    private POSBillingWalaDatabase posBillingWalaDatabase;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReportsHubBinding.inflate(inflater, container, false);
        activity = getActivity();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        binding.toolbar.toolbarTitle.setText(getString(R.string.ui_reports_hub));
        binding.toolbar.backButton.setOnClickListener(v -> ((MainActivity) activity).navigateBack());

        setupRow(binding.rowSalesDashboard, R.drawable.ic_report_dashboard, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.ui_sales_dashboard),
                getString(R.string.ui_this_branch_only));
        setupRow(binding.rowSalesOverview, R.drawable.ic_report_overview, R.drawable.bg_quick_action_purple,
                R.color.deepPurple, getString(R.string.ui_sales_overview),
                getString(R.string.ui_this_branch_only));
        setupRow(binding.rowInvoiceReport, R.drawable.ic_report_invoice, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.ui_invoice_report),
                getString(R.string.ui_report_detail));
        setupRow(binding.rowSaleReport, R.drawable.ic_report_sales, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.ui_sale_wise_report),
                getString(R.string.ui_sale_reports));
        setupRow(binding.rowTableReport, R.drawable.ic_table, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.ui_invoice_table_report),
                getString(R.string.ui_sale_reports));
        setupRow(binding.rowTakeAwayReport, R.drawable.ic_takeaway, R.drawable.bg_quick_action_purple,
                R.color.deepPurple, getString(R.string.ui_invoice_take_away_report),
                getString(R.string.ui_sale_reports));
        setupRow(binding.rowPaymentReport, R.drawable.ic_report_payment, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.ui_invoice_payment_mode_report),
                getString(R.string.ui_sale_reports));
        setupRow(binding.rowDiscountReport, R.drawable.ic_report_sales, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.discount_wise_report),
                getString(R.string.ui_sale_reports));
        setupRow(binding.rowRefundReport, R.drawable.ic_report_sales, R.drawable.bg_quick_action_red,
                R.color.statusExpired, getString(R.string.refund_wise_report),
                getString(R.string.ui_sale_reports));
        setupRow(binding.rowProductReport, R.drawable.ic_report_product, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.ui_product_wise_report),
                getString(R.string.ui_sale_reports));
        setupRow(binding.rowComboReport, R.drawable.ic_report_combo, R.drawable.bg_quick_action_blue,
                R.color.colorPrimary, getString(R.string.ui_combo_wise_report),
                getString(R.string.ui_sale_reports));
        setupRow(binding.rowExpenseReport, R.drawable.ic_report_expense, R.drawable.bg_quick_action_purple,
                R.color.deepPurple, getString(R.string.ui_expense_wise_report),
                getString(R.string.ui_sale_reports));
        setupRow(binding.rowMessMemberReport, R.drawable.ic_report_member, R.drawable.bg_quick_action_green,
                R.color.green_600, getString(R.string.ui_invoice_member_report),
                getString(R.string.ui_sale_reports));
        setupRow(binding.rowMessReport, R.drawable.ic_report_mess, R.drawable.bg_quick_action_orange,
                R.color.statusTrial, getString(R.string.ui_invoice_mess_report),
                getString(R.string.ui_sale_reports));
        setupRow(binding.rowDeleteAllInvoices, R.drawable.ic_delete, R.drawable.bg_quick_action_red,
                R.color.statusExpired, getString(R.string.ui_delete_all_invoice_सर्व_बले_हटव),
                getString(R.string.ui_delete_all_invoices_hint));

        binding.rowSalesDashboard.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new SalesDashboard(), true));
        binding.rowSalesOverview.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new SalesOverview(), true));
        binding.rowInvoiceReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new InvoiceReport(), true));
        binding.rowSaleReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new SaleReport(), true));
        binding.rowTableReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new InvoiceTableReport(), true));
        binding.rowTakeAwayReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new InvoiceTakeAwayReport(), true));
        binding.rowPaymentReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new InvoicePaymentModeWiseReport(), true));
        binding.rowDiscountReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new InvoiceDiscountReport(), true));
        binding.rowRefundReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new InvoiceRefundReport(), true));
        binding.rowProductReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new InvoiceProductReport(), true));
        binding.rowComboReport.getRoot().setOnClickListener(v -> {
            InvoiceProductReport comboReport = new InvoiceProductReport();
            Bundle comboArgs = new Bundle();
            comboArgs.putString(InvoiceProductReport.ARG_INVOICE_ITEM_TYPE, "COMBO");
            comboReport.setArguments(comboArgs);
            ((MainActivity) activity).loadFragment(comboReport, true);
        });
        binding.rowExpenseReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new InvoiceExpenseReport(), true));
        binding.rowMessMemberReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new InvoiceMessMemberReportList(), true));
        binding.rowMessReport.getRoot().setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new InvoiceMessReport(), true));
        binding.rowDeleteAllInvoices.getRoot().setOnClickListener(v -> clearAllInvoices());

        applyModuleVisibility();
        showGroupDividers(binding.rowSalesDashboard, binding.rowSalesOverview);
        showGroupDividers(
                binding.rowInvoiceReport,
                binding.rowSaleReport,
                binding.rowTableReport,
                binding.rowTakeAwayReport,
                binding.rowPaymentReport,
                binding.rowDiscountReport,
                binding.rowRefundReport,
                binding.rowProductReport,
                binding.rowComboReport,
                binding.rowExpenseReport,
                binding.rowMessMemberReport,
                binding.rowMessReport);

        View root = binding.getRoot();
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("ReportsHub", "back pressed");
                ((MainActivity) activity).navigateBack();
                return true;
            }
            return false;
        });
        return root;
    }

    private void setupRow(ItemGroupedMenuRowBinding row, int iconRes, int bgRes, int tintColor,
                          String title, String subtitle) {
        row.menuIcon.setBackgroundResource(bgRes);
        row.menuIcon.setImageResource(iconRes);
        row.menuIcon.clearColorFilter();
        row.menuIcon.setColorFilter(ContextCompat.getColor(requireContext(), tintColor));
        row.menuTitle.setText(title);
        row.menuSubtitle.setText(subtitle);
    }

    private void showGroupDividers(ItemGroupedMenuRowBinding... rows) {
        boolean firstVisible = true;
        for (ItemGroupedMenuRowBinding row : rows) {
            if (row.getRoot().getVisibility() != View.VISIBLE) {
                row.rowDivider.setVisibility(View.GONE);
                continue;
            }
            row.rowDivider.setVisibility(firstVisible ? View.GONE : View.VISIBLE);
            firstVisible = false;
        }
    }

    private void applyModuleVisibility() {
        LicenseModules.setVisible(binding.rowTableReport.getRoot(),
                LicenseModules.isEnabled(MainActivity.dineIn));
        LicenseModules.setVisible(binding.rowTakeAwayReport.getRoot(),
                LicenseModules.isEnabled(MainActivity.takeAway));
        LicenseModules.setVisible(binding.rowMessMemberReport.getRoot(),
                LicenseModules.isEnabled(MainActivity.mess));
        LicenseModules.setVisible(binding.rowMessReport.getRoot(),
                LicenseModules.isEnabled(MainActivity.mess));
    }

    private void clearAllInvoices() {
        int unsynced = posBillingWalaDatabase.countUnsyncedInvoices();
        if (unsynced > 0) {
            Toast.makeText(activity,
                    unsynced + " unsynced bill(s). Upload to cloud first — clear blocked to protect data.",
                    Toast.LENGTH_LONG).show();
            if (DetectConnection.checkInternetConnection(activity)) {
                ((MainActivity) activity).openCloudSyncStatus();
                UserSynchronizeData.start(activity, false);
            } else {
                DetectConnection.noInternetConnection(activity);
            }
            return;
        }
        posBillingWalaDatabase.clearInvoice();
        Toast.makeText(activity, getString(R.string.toast_invoice_cleared), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
