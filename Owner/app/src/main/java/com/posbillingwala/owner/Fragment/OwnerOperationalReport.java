package com.posbillingwala.owner.Fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.data.PieEntry;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Adapter.OwnerReportAdapter;
import com.posbillingwala.owner.Extra.BottomSheetUi;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Extra.EmptyListUi;
import com.posbillingwala.owner.Extra.ReportUiHelper;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.BranchComparisonResponse;
import com.posbillingwala.owner.Model.OwnerReportBreakdownItem;
import com.posbillingwala.owner.Model.OwnerReportListItem;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentOwnerOperationalReportBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OwnerOperationalReport extends Fragment {

    public static final String ARG_REPORT_TYPE = "reportType";

    public static final String TYPE_INVOICE = "invoice";
    public static final String TYPE_SALE = "sale";
    public static final String TYPE_TABLE = "table";
    public static final String TYPE_TAKEAWAY = "takeaway";
    public static final String TYPE_PAYMENT = "payment";
    public static final String TYPE_DISCOUNT = "discount";
    public static final String TYPE_REFUND = "refund";
    public static final String TYPE_PRODUCT = "product";
    public static final String TYPE_COMBO = "combo";
    public static final String TYPE_EXPENSE = "expense";
    public static final String TYPE_MESS_MEMBER = "mess_member";
    public static final String TYPE_MESS = "mess";

    private static final int[] CHART_COLORS = new int[]{
            Color.parseColor("#4862b7"),
            Color.parseColor("#10B981"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#8B5CF6"),
            Color.parseColor("#06B6D4"),
            Color.parseColor("#EC4899"),
            Color.parseColor("#64748B")
    };

    private Activity activity;
    private FragmentOwnerOperationalReportBinding binding;
    private OwnerReportAdapter adapter;
    private String reportType = TYPE_INVOICE;
    private String selectedBranchId = "all";
    private String selectedDate = "";
    private final List<BranchComparisonResponse> branchOptions = new ArrayList<>();

    public static OwnerOperationalReport newInstance(String reportType) {
        OwnerOperationalReport fragment = new OwnerOperationalReport();
        Bundle args = new Bundle();
        args.putString(ARG_REPORT_TYPE, reportType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOwnerOperationalReportBinding.inflate(inflater, container, false);
        activity = getActivity();
        if (getArguments() != null && getArguments().getString(ARG_REPORT_TYPE) != null) {
            reportType = getArguments().getString(ARG_REPORT_TYPE);
        }

        binding.toolbar.toolbarTitle.setText(titleForType(reportType));
        binding.toolbar.backButton.setOnClickListener(v -> navigateBack());

        adapter = new OwnerReportAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        binding.recyclerView.setAdapter(adapter);

        boolean multi = MainActivity.isMultiOutlet();
        binding.branchChip.setVisibility(multi ? View.VISIBLE : View.GONE);
        binding.branchChip.setOnClickListener(v -> {
            if (branchOptions.size() > 1) {
                showBranchPicker();
            }
        });
        binding.dateChip.setOnClickListener(v -> showDatePicker());

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

    @Override
    public void onStart() {
        super.onStart();
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
            return;
        }
        if (MainActivity.isMultiOutlet() && branchOptions.isEmpty()) {
            preloadBranches();
        } else {
            loadReport();
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
                updateBranchChip();
                loadReport();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                loadReport();
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
                    updateBranchChip();
                    loadReport();
                });
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (selectedDate != null && selectedDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                String[] p = selectedDate.split("-");
                cal.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
            } catch (Exception ignored) {
            }
        }
        DatePickerDialog dialog = new DatePickerDialog(activity, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            binding.dateChip.setText(selectedDate);
            loadReport();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, getString(R.string.ui_all_time), (d, which) -> {
            selectedDate = "";
            binding.dateChip.setText(R.string.ui_all_time);
            loadReport();
        });
        dialog.show();
    }

    private void updateBranchChip() {
        if (binding == null) return;
        if (!MainActivity.isMultiOutlet() || branchOptions.size() <= 1) {
            binding.branchChip.setText(R.string.all_branches);
            return;
        }
        if ("all".equals(selectedBranchId)) {
            binding.branchChip.setText(R.string.all_branches);
            return;
        }
        for (BranchComparisonResponse b : branchOptions) {
            if (selectedBranchId.equals(b.getBranchId())) {
                String name = b.getBranchLabel() != null && !b.getBranchLabel().isEmpty()
                        ? b.getBranchLabel() : b.getShopName1();
                binding.branchChip.setText(name != null ? name : selectedBranchId);
                return;
            }
        }
        binding.branchChip.setText(selectedBranchId);
    }

    private void loadReport() {
        if (binding == null || activity == null) return;
        SweetAlertDialog loader = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        loader.getProgressHelper().setBarColor(Color.parseColor("#4862b7"));
        loader.setTitleText(getString(R.string.loading));
        loader.setCancelable(false);
        loader.show();

        String branch = selectedBranchId == null || selectedBranchId.isEmpty() ? "all" : selectedBranchId;
        String date = selectedDate == null ? "" : selectedDate;

        Api.getClient().getOwnerOperationalReport(MainActivity.userId, branch, reportType, date)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call,
                                           @NonNull Response<AllApiResponse> response) {
                        loader.dismiss();
                        if (!isAdded() || binding == null) return;
                        bindResponse(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        loader.dismiss();
                        if (!isAdded() || binding == null) return;
                        bindResponse(null);
                    }
                });
    }

    private void bindResponse(@Nullable AllApiResponse body) {
        List<OwnerReportListItem> items = body != null ? body.getReportItems() : null;
        List<OwnerReportBreakdownItem> breakdown = body != null ? body.getBreakdown() : null;
        boolean hasData = (items != null && !items.isEmpty())
                || (breakdown != null && !breakdown.isEmpty());

        EmptyListUi.bind(binding.noDataFound.getRoot(), hasData, R.string.empty_sub_reports);
        binding.nestedScrollView.setVisibility(hasData ? View.VISIBLE : View.GONE);
        if (!hasData) {
            return;
        }

        String period = body.getPeriodLabel() != null ? body.getPeriodLabel() : "";
        binding.periodLabel.setText(period);

        ReportUiHelper.bindKpi(binding.kpi1, getString(R.string.total_bills),
                nz(body.getTotalBills()), "");
        ReportUiHelper.bindKpi(binding.kpi2, getString(R.string.total_sales),
                ReportUiHelper.money(body.getTotalAmount()), "");
        ReportUiHelper.bindKpi(binding.kpi3, getString(R.string.avg_bill),
                ReportUiHelper.money(body.getAvgBill()), "");
        String extraLabel = body.getExtraKpiLabel() != null && !body.getExtraKpiLabel().isEmpty()
                ? body.getExtraKpiLabel() : getString(R.string.ui_details);
        String extraValue = body.getExtraKpiValue() != null ? body.getExtraKpiValue() : "0";
        boolean extraIsMoney = TYPE_DISCOUNT.equals(reportType);
        ReportUiHelper.bindKpi(binding.kpi4, extraLabel,
                extraIsMoney ? ReportUiHelper.money(extraValue) : extraValue, "");

        bindChart(breakdown, body.getTotalAmount());
        boolean showMoney = !TYPE_MESS_MEMBER.equals(reportType) && !TYPE_MESS.equals(reportType);
        adapter.setItems(items, showMoney);
    }

    private void bindChart(@Nullable List<OwnerReportBreakdownItem> breakdown, String totalAmount) {
        if (breakdown == null || breakdown.isEmpty()) {
            binding.chartCard.setVisibility(View.GONE);
            return;
        }
        binding.chartCard.setVisibility(View.VISIBLE);
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<String> percents = new ArrayList<>();
        float sum = 0f;
        for (OwnerReportBreakdownItem b : breakdown) {
            sum += ReportUiHelper.parseAmount(b.getAmount());
        }
        if (sum <= 0f) {
            sum = ReportUiHelper.parseAmount(totalAmount);
        }
        int limit = Math.min(breakdown.size(), 8);
        for (int i = 0; i < limit; i++) {
            OwnerReportBreakdownItem b = breakdown.get(i);
            float amt = ReportUiHelper.parseAmount(b.getAmount());
            if (amt <= 0f && b.getCount() != null) {
                try {
                    amt = Float.parseFloat(b.getCount());
                } catch (Exception ignored) {
                }
            }
            String label = b.getLabel() != null ? b.getLabel() : "—";
            entries.add(new PieEntry(Math.max(amt, 0.01f), label));
            colors.add(CHART_COLORS[i % CHART_COLORS.length]);
            labels.add(label);
            values.add(ReportUiHelper.money(b.getAmount()));
            float pct = sum > 0 ? (ReportUiHelper.parseAmount(b.getAmount()) * 100f / sum) : 0f;
            percents.add(String.format(Locale.US, "%.0f", pct));
        }
        int[] colorArr = new int[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            colorArr[i] = colors.get(i);
        }
        ReportUiHelper.setupDonut(binding.chartDonut, entries, colors,
                ReportUiHelper.money(totalAmount));
        ReportUiHelper.fillLegend(binding.legendContainer,
                labels.toArray(new String[0]),
                values.toArray(new String[0]),
                percents.toArray(new String[0]),
                colorArr);
    }

    private String titleForType(String type) {
        if (type == null) return getString(R.string.ui_invoice_report);
        switch (type) {
            case TYPE_SALE:
                return getString(R.string.ui_sale_wise_report);
            case TYPE_TABLE:
                return getString(R.string.ui_invoice_table_report);
            case TYPE_TAKEAWAY:
                return getString(R.string.ui_invoice_take_away_report);
            case TYPE_PAYMENT:
                return getString(R.string.ui_invoice_payment_mode_report);
            case TYPE_DISCOUNT:
                return getString(R.string.discount_wise_report);
            case TYPE_REFUND:
                return getString(R.string.refund_wise_report);
            case TYPE_PRODUCT:
                return getString(R.string.ui_product_wise_report);
            case TYPE_COMBO:
                return getString(R.string.ui_combo_wise_report);
            case TYPE_EXPENSE:
                return getString(R.string.ui_expense_wise_report);
            case TYPE_MESS_MEMBER:
                return getString(R.string.ui_invoice_member_report);
            case TYPE_MESS:
                return getString(R.string.ui_invoice_mess_report);
            case TYPE_INVOICE:
            default:
                return getString(R.string.ui_invoice_report);
        }
    }

    private static String nz(String value) {
        return value == null || value.trim().isEmpty() ? "0" : value;
    }

    private void navigateBack() {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
