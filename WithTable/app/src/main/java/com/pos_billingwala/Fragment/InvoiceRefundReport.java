package com.pos_billingwala.Fragment;

import com.pos_billingwala.Extra.PopupUi;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.ReportAdapter;
import com.pos_billingwala.CalenderView.MonthPickerDialog;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.OperationalReportCharts;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.Extra.ReportUiHelper;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.Model.ReportRankItem;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentOperationalReportBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;

@SuppressLint("SetTextI18n")
public class InvoiceRefundReport extends Fragment implements View.OnClickListener {

    private Activity activity;
    private FragmentOperationalReportBinding binding;
    private POSBillingWalaDatabase posBillingWalaDatabase;
    private ReportAdapter adapter;
    private final List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
    private String invoiceDate = "";
    private int mYear, mMonth, mDay;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOperationalReportBinding.inflate(inflater, container, false);
        activity = getActivity();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        binding.toolbar.heading.setText(getString(R.string.refund_wise_report));
        binding.toolbar.shareInvoice.setVisibility(View.GONE);
        binding.listTitle.setText(getString(R.string.refund_wise_report));
        binding.donutTitle.setText(getString(R.string.refund_wise_report));
        binding.barTitle.setText(getString(R.string.ui_amount_breakdown));
        ReportUiHelper.setupDetailTableHeader(binding.tableHeader,
                getString(R.string.ui_invoice_date),
                getString(R.string.ui_invoice_number),
                getString(R.string.ui_total_amount));

        View root = binding.getRoot();
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("InvoiceRefundReport", "back pressed");
                ((MainActivity) activity).navigateBack();
                return true;
            }
            return false;
        });

        binding.toolbar.backToSetting.setOnClickListener(this);
        binding.toolbar.menuIcon.setOnClickListener(this);
        return root;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.menuIcon) {
            showPeriodMenu();
        }
    }

    private void showPeriodMenu() {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.sale_wise_dialog, null);
        PopupWindow popupWindow = PopupUi.create(activity, popupView);

        Calendar calender = Calendar.getInstance();
        mYear = calender.get(Calendar.YEAR);
        mMonth = calender.get(Calendar.MONTH);
        mDay = calender.get(Calendar.DAY_OF_MONTH);

        LinearLayout dayWiseLayout = popupView.findViewById(R.id.dayWiseLayout);
        LinearLayout monthWiseLayout = popupView.findViewById(R.id.monthWiseLayout);
        LinearLayout yearWiseLayout = popupView.findViewById(R.id.yearWiseLayout);

        dayWiseLayout.setOnClickListener(v -> {
            popupWindow.dismiss();
            new DatePickerDialog(activity, (view, year, monthOfYear, dayOfMonth) -> {
                invoiceDate = formatDay(year, monthOfYear, dayOfMonth);
                loadReport(invoiceDate);
            }, mYear, mMonth, mDay).show();
        });
        monthWiseLayout.setOnClickListener(v -> {
            popupWindow.dismiss();
            MonthYearPickerDialogFragment dialogFragment =
                    MonthYearPickerDialogFragment.getInstance(mMonth, mYear, "Select Month");
            dialogFragment.show(getChildFragmentManager(), null);
            dialogFragment.setOnDateSetListener((year, monthOfYear) -> {
                invoiceDate = monthOfYear + 1 > 9
                        ? year + "-" + (monthOfYear + 1)
                        : year + "-0" + (monthOfYear + 1);
                loadReport(invoiceDate);
            });
        });
        yearWiseLayout.setOnClickListener(v -> {
            popupWindow.dismiss();
            new MonthPickerDialog.Builder(activity, (selectedMonth, selectedYear) -> {
                invoiceDate = String.valueOf(selectedYear);
                mYear = selectedYear;
                loadReport(invoiceDate);
            }, mYear, 0).showYearOnly().setYearRange(1990, 2050).build().show();
        });

        popupWindow.showAsDropDown(binding.toolbar.menuIcon, 0, -75);
    }

    private String formatDay(int year, int monthOfYear, int dayOfMonth) {
        String month = monthOfYear + 1 > 9 ? String.valueOf(monthOfYear + 1) : "0" + (monthOfYear + 1);
        String day = dayOfMonth > 9 ? String.valueOf(dayOfMonth) : "0" + dayOfMonth;
        return year + "-" + month + "-" + day;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        loadReport(invoiceDate);
    }

    private void loadReport(String selectedDate) {
        SweetAlertDialog loader = ListLoader.show(activity);
        try {
            invoiceResponseList.clear();
            invoiceResponseList.addAll(posBillingWalaDatabase.getRefundInvoiceList(selectedDate, 0, 500));
            bindList(OperationalReportCharts.formatPeriodLabel(selectedDate == null ? "" : selectedDate));
        } finally {
            ListLoader.dismiss(loader);
        }
    }

    private void bindList(String periodLabel) {
        if (invoiceResponseList.isEmpty()) {
            binding.noDataFound.setVisibility(View.VISIBLE);
            binding.nestedScrollView.setVisibility(View.GONE);
            return;
        }
        adapter = new ReportAdapter(activity, invoiceResponseList, false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        binding.recyclerView.setAdapter(adapter);

        float totalRefunded = 0f;
        for (InvoiceResponse invoice : invoiceResponseList) {
            totalRefunded += ReportCursorHelper.parseAmount(invoice.getTotalAmount());
        }
        binding.totalAmount.setText(MainActivity.currencyName + " "
                + String.format(Locale.US, "%.2f", totalRefunded));

        List<ReportRankItem> breakdown = new ArrayList<>();
        ReportRankItem refunded = new ReportRankItem();
        refunded.setLabel(getString(R.string.bill_refunded));
        refunded.setCount(String.valueOf(invoiceResponseList.size()));
        refunded.setTotal(String.valueOf(totalRefunded));
        breakdown.add(refunded);

        OperationalReportCharts.bindListSummary(binding, activity, invoiceResponseList.size(), totalRefunded,
                breakdown, getString(R.string.refund_wise_report),
                getString(R.string.ui_amount_breakdown), periodLabel);
        binding.noDataFound.setVisibility(View.GONE);
        binding.nestedScrollView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
