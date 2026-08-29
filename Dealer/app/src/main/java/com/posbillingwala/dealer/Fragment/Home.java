package com.posbillingwala.dealer.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Extra.Common;
import com.posbillingwala.dealer.Extra.DetectConnection;
import com.posbillingwala.dealer.Extra.LicenseStatusHelper;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.Model.CustomerResponse;
import com.posbillingwala.dealer.Model.DealerSalesResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.databinding.FragmentHomeBinding;
import com.posbillingwala.dealer.databinding.IncludeDashboardKpiCardBinding;
import com.posbillingwala.dealer.databinding.IncludeDashboardPerfSmallBinding;
import com.posbillingwala.dealer.databinding.ItemDashboardAttentionBinding;
import com.posbillingwala.dealer.databinding.ItemDashboardRecentCustomerBinding;
import com.posbillingwala.dealer.databinding.ItemDealerLegendRowBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, UseCompatLoadingForDrawables, StaticFieldLeak")
public class Home extends Fragment implements View.OnClickListener {

    private static final int MAX_CHART_CUSTOMERS = 8;
    private static final int MAX_RECENT_CUSTOMERS = 5;

    public static Activity activity;
    View view;
    FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        view = binding.getRoot();
        activity = getActivity();
        MainActivity.title.setText("Dashboard");

        binding.greetingText.setText(greeting() + ", " + dealerName() + " 👋");
        binding.dateText.setText(todayLabel());

        setupSectionHeaders();
        setupKpiCards();
        setupPerformanceCards();
        stylePieChart(binding.chartDealerSales);
        styleSparkline(binding.perfTotalSales.salesSparkline);

        binding.customerRegistration.setOnClickListener(this);
        binding.quickAddDealer.setOnClickListener(this);
        binding.quickLicenses.setOnClickListener(this);
        binding.onBoardCustomerList.setOnClickListener(this);

        binding.headerRecentCustomers.sectionAction.setOnClickListener(v -> openCustomers());
        binding.headerDealerSales.sectionAction.setOnClickListener(v -> openCustomers());

        return view;
    }

    private void setupSectionHeaders() {
        binding.headerBusinessOverview.sectionTitle.setText("Business Overview");
        binding.headerBusinessOverview.sectionTitle.setTextSize(20);
        binding.headerBusinessOverview.sectionAction.setVisibility(View.GONE);
        binding.headerLicenseOverview.sectionTitle.setText("License Overview");
        binding.headerLicenseOverview.sectionTitle.setTextSize(20);
        binding.headerLicenseOverview.sectionAction.setVisibility(View.GONE);
        binding.headerDealerSales.sectionTitle.setText("Customer Sales (This Month)");
        binding.headerDealerSales.sectionAction.setText("View All");
        binding.headerAttention.sectionTitle.setText("Attention Required");
        binding.headerAttention.sectionAction.setVisibility(View.GONE);
        binding.headerRecentCustomers.sectionTitle.setText("Recent Customers");
        binding.headerRecentCustomers.sectionAction.setText("View All");
    }

    private void setupKpiCards() {
        bindKpi(binding.kpiTotalCustomer, R.drawable.ic_nav_customers, R.drawable.bg_kpi_icon_blue,
                R.drawable.bg_kpi_card_blue, "Total\nCustomers", R.color.colorPrimary);
        bindKpi(binding.kpiActiveCustomer, R.drawable.ic_nav_customers, R.drawable.bg_kpi_icon_green,
                R.drawable.bg_kpi_card_green, "Active\nCustomers", R.color.statusActive);
        bindKpi(binding.kpiTrialCustomer, R.drawable.ic_calendar, R.drawable.bg_kpi_icon_orange,
                R.drawable.bg_kpi_card_orange, "Trial\nCustomers", R.color.statusTrial);
        bindKpi(binding.kpiExpiredCustomer, R.drawable.ic_warning, R.drawable.bg_kpi_icon_red,
                R.drawable.bg_kpi_card_red, "Expired\nCustomers", R.color.statusExpired);
        bindKpi(binding.kpiActiveLicenses, R.drawable.ic_report_licenses, R.drawable.bg_kpi_icon_green,
                R.drawable.bg_kpi_card_green, "Active\nLicenses", R.color.statusActive);
        bindKpi(binding.kpiExpiringLicenses, R.drawable.ic_warning, R.drawable.bg_kpi_icon_orange,
                R.drawable.bg_kpi_card_orange, "Expiring\nSoon", R.color.statusTrial);
        bindKpi(binding.kpiTrialLicenses, R.drawable.ic_calendar, R.drawable.bg_kpi_icon_blue,
                R.drawable.bg_kpi_card_blue, "Trial\nLicenses", R.color.colorPrimary);
        bindKpi(binding.kpiExpiredLicenses, R.drawable.ic_trending_down, R.drawable.bg_kpi_icon_red,
                R.drawable.bg_kpi_card_red, "Expired\nLicenses", R.color.statusExpired);

        binding.kpiTotalCustomer.getRoot().setOnClickListener(v -> openCustomers());
        binding.kpiActiveCustomer.getRoot().setOnClickListener(v -> openCustomers());
        binding.kpiTrialCustomer.getRoot().setOnClickListener(v -> openCustomers());
        binding.kpiExpiredCustomer.getRoot().setOnClickListener(v -> openCustomers());
        binding.kpiActiveLicenses.getRoot().setOnClickListener(v -> openCustomers());
        binding.kpiExpiringLicenses.getRoot().setOnClickListener(v -> openCustomers());
        binding.kpiTrialLicenses.getRoot().setOnClickListener(v -> openCustomers());
        binding.kpiExpiredLicenses.getRoot().setOnClickListener(v -> openCustomers());
    }

    private void openCustomers() {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        ((MainActivity) activity).loadFragment(new AllCustomerList(), false);
    }

    private void bindKpi(IncludeDashboardKpiCardBinding card, int iconRes, int iconBg, int cardBg,
                         String label, int tintColor) {
        card.kpiCardInner.setBackgroundResource(cardBg);
        card.kpiIcon.setBackgroundResource(iconBg);
        card.kpiIcon.setImageResource(iconRes);
        card.kpiIcon.setColorFilter(ContextCompat.getColor(requireContext(), tintColor));
        card.statLabel.setText(label);
        card.statValue.setText("0");
        card.statTrend.setVisibility(View.GONE);
    }

    private void setupPerformanceCards() {
        bindPerfSmall(binding.perfTodaySales, R.drawable.ic_nav_sales, R.drawable.bg_kpi_icon_blue,
                "Today's Sales", R.color.colorPrimary);
        bindPerfSmall(binding.perfCustomersAdded, R.drawable.ic_nav_customers, R.drawable.bg_kpi_icon_purple,
                "Customers Added", R.color.deepPurple);
        bindPerfSmall(binding.perfActiveBranches, R.drawable.ic_business, R.drawable.bg_kpi_icon_green,
                "Active Branches", R.color.statusActive);
        binding.perfTodaySales.getRoot().setOnClickListener(v -> openCustomers());
        binding.perfCustomersAdded.getRoot().setOnClickListener(v -> openCustomers());
        binding.perfActiveBranches.getRoot().setOnClickListener(v -> openCustomers());
        binding.perfTotalSales.getRoot().setOnClickListener(v -> openCustomers());
    }

    private void bindPerfSmall(IncludeDashboardPerfSmallBinding stat, int iconRes, int iconBg, String label, int tintColor) {
        stat.perfIcon.setBackgroundResource(iconBg);
        stat.perfIcon.setImageResource(iconRes);
        stat.perfIcon.setColorFilter(ContextCompat.getColor(requireContext(), tintColor));
        stat.statLabel.setText(label);
        stat.statValue.setText("0");
        stat.statTrend.setVisibility(View.GONE);
    }

    private String dealerName() {
        String name = Common.getSavedUserData(activity, "userName");
        if (name == null || name.trim().isEmpty()) {
            return "Dealer";
        }
        return name.trim();
    }

    private String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good Morning";
        if (hour < 17) return "Good Afternoon";
        return "Good Evening";
    }

    private String todayLabel() {
        return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(new Date());
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.customerRegistration) {
            ((MainActivity) activity).loadFragment(new CustomerRegistration(), true);
        } else if (id == R.id.quickAddDealer) {
            openCustomers();
        } else if (id == R.id.quickLicenses) {
            openCustomers();
        } else if (id == R.id.onBoardCustomerList) {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new ProductExport(), false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(0);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getCustomerCount();
            loadRecentCustomers();
            loadCustomerSalesChart();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void getCustomerCount() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
        pDialog.setTitleText("Loading dashboard");
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().getCustomerCount(MainActivity.userId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || binding == null) {
                    pDialog.dismiss();
                    return;
                }
                if (response.isSuccessful() && response.body() != null
                        && response.body().getStatus() != null
                        && response.body().getStatus().equalsIgnoreCase("true")) {
                    AllApiResponse body = response.body();
                    setKpiValue(binding.kpiTotalCustomer, body.getTotalCustomer());
                    setKpiTrend(binding.kpiTotalCustomer, body.getTotalCustomerTrendLabel(), body.getTotalCustomerTrend());
                    setKpiValue(binding.kpiActiveCustomer, body.getActiveCustomer());
                    setKpiTrend(binding.kpiActiveCustomer, body.getActiveCustomerTrendLabel(), body.getActiveCustomerTrend());
                    setKpiValue(binding.kpiTrialCustomer, body.getTrialCustomer());
                    setKpiTrend(binding.kpiTrialCustomer, body.getTrialCustomerTrendLabel(), body.getTrialCustomerTrend());
                    setKpiValue(binding.kpiExpiredCustomer, body.getExpiredCustomer());
                    setKpiTrend(binding.kpiExpiredCustomer, body.getExpiredCustomerTrendLabel(), body.getExpiredCustomerTrend());
                    setKpiValue(binding.kpiActiveLicenses, body.getActiveLicenses());
                    setKpiTrend(binding.kpiActiveLicenses, body.getActiveLicensesTrendLabel(), body.getActiveLicensesTrend());
                    setKpiValue(binding.kpiExpiringLicenses, body.getExpiringLicenses());
                    setKpiTrend(binding.kpiExpiringLicenses, body.getExpiringLicensesTrendLabel(), body.getExpiringLicensesTrend());
                    setKpiValue(binding.kpiTrialLicenses, body.getTrialLicenses());
                    setKpiTrend(binding.kpiTrialLicenses, body.getTrialLicensesTrendLabel(), body.getTrialLicensesTrend());
                    setKpiValue(binding.kpiExpiredLicenses, body.getExpiredLicenses());
                    setKpiTrend(binding.kpiExpiredLicenses, body.getExpiredLicensesTrendLabel(), body.getExpiredLicensesTrend());
                    binding.perfTotalSales.statValue.setText("₹ " + formatAmount(body.getNetSales()));
                    applyTrend(binding.perfTotalSales.statTrend, body.getNetSalesTrendLabel(), body.getNetSalesTrend());
                    setPerfSmallValue(binding.perfTodaySales, "₹ " + formatAmount(body.getTodaySales()));
                    applyTrend(binding.perfTodaySales.statTrend, body.getTodaySalesTrendLabel(), body.getTodaySalesTrend());
                    setPerfSmallValue(binding.perfCustomersAdded, nz(body.getCustomersAddedThisMonth()));
                    applyTrend(binding.perfCustomersAdded.statTrend, body.getCustomersAddedTrendLabel(), body.getCustomersAddedTrend());
                    setPerfSmallValue(binding.perfActiveBranches, nz(body.getTotalBranches()));
                    applyTrend(binding.perfActiveBranches.statTrend, body.getActiveBranchesTrendLabel(), body.getActiveBranchesTrend());
                    updateSparkline(body.getSalesSparkline(), parseFloat(body.getNetSales()));
                    buildAttentionItems(body);
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
            }
        });
    }

    private void loadRecentCustomers() {
        Api.getClient().getCustomerList(MainActivity.userId, "customer").enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || binding == null) return;
                List<CustomerResponse> customers = response.isSuccessful() && response.body() != null
                        ? response.body().getCustomerResponseList() : null;
                renderRecentCustomers(customers);
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                if (isAdded() && binding != null) {
                    renderRecentCustomers(null);
                }
            }
        });
    }

    private void renderRecentCustomers(List<CustomerResponse> customers) {
        binding.recentCustomersContainer.removeAllViews();
        if (customers == null || customers.isEmpty()) {
            binding.recentCustomersEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.recentCustomersEmpty.setVisibility(View.GONE);
        int count = Math.min(customers.size(), MAX_RECENT_CUSTOMERS);
        LayoutInflater inflater = LayoutInflater.from(activity);
        for (int i = 0; i < count; i++) {
            CustomerResponse customer = customers.get(i);
            ItemDashboardRecentCustomerBinding row =
                    ItemDashboardRecentCustomerBinding.inflate(inflater, binding.recentCustomersContainer, false);
            row.recentShopName.setText(customer.getShopName() != null ? customer.getShopName() : customer.getName());
            row.recentLocation.setText(customer.getAddress() != null ? customer.getAddress() : "—");
            String status = LicenseStatusHelper.STATUS_PENDING;
            if (customer.getLicenseResponseList() != null && !customer.getLicenseResponseList().isEmpty()) {
                status = LicenseStatusHelper.displayStatus(customer.getLicenseResponseList().get(0));
            }
            LicenseStatusHelper.applyBadge(row.recentStatusBadge, status);
            row.getRoot().setOnClickListener(v -> {
                CustomerDetails details = new CustomerDetails();
                Bundle bundle = new Bundle();
                bundle.putString("customerId", customer.getId());
                details.setArguments(bundle);
                ((MainActivity) activity).loadFragment(details, true);
            });
            binding.recentCustomersContainer.addView(row.getRoot());
        }
    }

    private void buildAttentionItems(AllApiResponse body) {
        binding.attentionContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(activity);
        addAttentionItem(inflater, R.drawable.ic_calendar, R.drawable.bg_alert_row_orange,
                parseInt(body.getExpiringLicenses7Days()) + " licenses expire within 7 days",
                R.color.statusTrial);
        addAttentionItem(inflater, R.drawable.ic_warning, R.drawable.bg_alert_row_red,
                parseInt(body.getExpiredLicenses()) + " customers have expired licenses",
                R.color.statusExpired);
        addAttentionItem(inflater, R.drawable.ic_nav_customers, R.drawable.bg_alert_row_blue,
                parseInt(body.getTrialLicensesExpiringTomorrow()) + " trial licenses expire tomorrow",
                R.color.colorPrimary);
    }

    private void addAttentionItem(LayoutInflater inflater, int iconRes, int rowBg, String text, int tintColor) {
        ItemDashboardAttentionBinding row =
                ItemDashboardAttentionBinding.inflate(inflater, binding.attentionContainer, false);
        row.alertRowRoot.setBackgroundResource(rowBg);
        row.alertIcon.setImageResource(iconRes);
        row.alertIcon.setColorFilter(ContextCompat.getColor(requireContext(), tintColor));
        row.alertText.setText(text);
        row.getRoot().setOnClickListener(v -> openCustomers());
        binding.attentionContainer.addView(row.getRoot());
    }

    private void loadCustomerSalesChart() {
        Api.getClient().getCustomerSalesOverview(MainActivity.userId, MAX_CHART_CUSTOMERS)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                        if (!isAdded() || binding == null) return;
                        List<DealerSalesResponse> rows = response.isSuccessful() && response.body() != null
                                ? response.body().getDealerSalesResponseList() : null;
                        renderSalesPieChart(rows);
                    }

                    @Override
                    public void onFailure(Call<AllApiResponse> call, Throwable t) {
                        if (isAdded() && binding != null) {
                            showEmptyChart(true);
                        }
                    }
                });
    }

    private void renderSalesPieChart(List<DealerSalesResponse> data) {
        if (data == null || data.isEmpty()) {
            showEmptyChart(true);
            return;
        }
        float total = 0f;
        for (DealerSalesResponse d : data) {
            float sales = parseFloat(d.getTotalSales());
            total += sales > 0 ? sales : 0f;
        }
        if (total <= 0f) {
            showEmptyChart(true);
            return;
        }
        showEmptyChart(false);
        List<PieEntry> entries = new ArrayList<>();
        for (DealerSalesResponse d : data) {
            float sales = parseFloat(d.getTotalSales());
            if (sales > 0) {
                entries.add(new PieEntry(sales, d.shortLabel()));
            }
        }
        if (entries.isEmpty()) {
            showEmptyChart(true);
            return;
        }
        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(chartColors());
        set.setSliceSpace(2f);
        PieData pieData = new PieData(set);
        pieData.setValueFormatter(new PercentFormatter(binding.chartDealerSales));
        pieData.setDrawValues(false);
        PieChart chart = binding.chartDealerSales;
        chart.setData(pieData);
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(55f);
        chart.setTransparentCircleRadius(58f);
        chart.setCenterText("Total Sales\n₹ " + formatIndianAmount(total));
        chart.setCenterTextSize(10f);
        chart.getLegend().setEnabled(false);
        chart.setDrawEntryLabels(false);
        chart.invalidate();
        chart.animateY(800);
        renderLegend(data, total, chartColors());
    }

    private void renderLegend(List<DealerSalesResponse> data, float total, int[] colors) {
        binding.dealerLegendContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(activity);
        int colorIndex = 0;
        for (DealerSalesResponse d : data) {
            float sales = parseFloat(d.getTotalSales());
            if (sales <= 0) continue;
            ItemDealerLegendRowBinding row = ItemDealerLegendRowBinding.inflate(
                    inflater, binding.dealerLegendContainer, false);
            int color = colors[colorIndex % colors.length];
            colorIndex++;
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(color);
            row.legendDot.setBackground(dot);
            row.legendName.setText(d.getDealerName());
            row.legendAmount.setText("₹ " + formatIndianAmount(sales));
            row.legendPercent.setText(Math.round((sales / total) * 100f) + "%");
            binding.dealerLegendContainer.addView(row.getRoot());
        }
    }

    private int[] chartColors() {
        return new int[]{
                ContextCompat.getColor(requireContext(), R.color.colorPrimary),
                ContextCompat.getColor(requireContext(), R.color.statusActive),
                ContextCompat.getColor(requireContext(), R.color.statusTrial),
                ContextCompat.getColor(requireContext(), R.color.deepPurple),
                ContextCompat.getColor(requireContext(), R.color.colorTextHint)
        };
    }

    private void showEmptyChart(boolean empty) {
        binding.chartDealerSales.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.chartDealerEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.dealerLegendContainer.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void styleSparkline(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setViewPortOffsets(0f, 0f, 0f, 0f);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setEnabled(false);
    }

    private void stylePieChart(PieChart chart) {
        chart.setNoDataText("Loading customer sales…");
        chart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary));
    }

    private void updateSparkline(List<String> sparkline, float fallbackBase) {
        if (binding == null || binding.perfTotalSales.salesSparkline == null) return;
        List<Entry> points = new ArrayList<>();
        if (sparkline != null && !sparkline.isEmpty()) {
            for (int i = 0; i < sparkline.size(); i++) {
                points.add(new Entry(i, parseFloat(sparkline.get(i))));
            }
        } else {
            float step = Math.max(fallbackBase / 6f, 1f);
            for (int i = 0; i < 7; i++) {
                points.add(new Entry(i, step * (0.6f + (i * 0.08f))));
            }
        }
        LineDataSet set = new LineDataSet(points, "");
        set.setColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryLight));
        binding.perfTotalSales.salesSparkline.setData(new LineData(set));
        binding.perfTotalSales.salesSparkline.invalidate();
    }

    private void setKpiValue(IncludeDashboardKpiCardBinding card, String value) {
        card.statValue.setText(nz(value));
    }

    private void setKpiTrend(IncludeDashboardKpiCardBinding card, String label, String trendPct) {
        applyTrend(card.statTrend, label, trendPct);
    }

    private void setPerfSmallValue(IncludeDashboardPerfSmallBinding stat, String value) {
        stat.statValue.setText(value != null ? value : "0");
    }

    private void applyTrend(TextView trendView, String label, String trendPct) {
        if (trendView == null) return;
        if (label == null || label.trim().isEmpty()) {
            trendView.setVisibility(View.GONE);
            return;
        }
        trendView.setText(label);
        trendView.setVisibility(View.VISIBLE);
        float pct = parseFloat(trendPct);
        int color = pct >= 0 ? R.color.statusActive : R.color.statusExpired;
        trendView.setTextColor(ContextCompat.getColor(requireContext(), color));
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(nz(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private float parseFloat(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return 0f;
            return Float.parseFloat(value.trim().replaceAll("[^0-9.-]", ""));
        } catch (Exception e) {
            return 0f;
        }
    }

    private String nz(String value) {
        return value == null || value.trim().isEmpty() ? "0" : value.trim();
    }

    private String formatIndianAmount(float value) {
        return String.format(Locale.US, "%,.0f", value);
    }

    private String formatAmount(String value) {
        if (value == null || value.trim().isEmpty()) return "0";
        return value.trim();
    }
}
