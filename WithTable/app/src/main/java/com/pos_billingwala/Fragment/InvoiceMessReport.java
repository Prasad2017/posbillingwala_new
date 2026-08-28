package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.github.dewinjm.monthyearpicker.MonthYearPickerDialog;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.InvoiceMessReportAdapter;
import com.pos_billingwala.CalenderView.MonthPickerDialog;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.OperationalReportCharts;
import com.pos_billingwala.Extra.ReportUiHelper;
import com.pos_billingwala.Model.MessInvoiceResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentOperationalReportBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;


public class InvoiceMessReport extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public int mYear, mMonth, mDay;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<MessInvoiceResponse> messInvoiceResponseList = new ArrayList<>();
    Calendar calender;
    DatePickerDialog datePickerDialog;
    String invoiceDate = "";
    FragmentOperationalReportBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOperationalReportBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);
        binding.toolbar.heading.setText(getString(R.string.ui_invoice_mess_report));
        binding.toolbar.shareInvoice.setVisibility(View.GONE);
        binding.listTitle.setText(getString(R.string.ui_invoice_mess_report));
        ReportUiHelper.setupDetailTableHeader(binding.tableHeader,
                getString(R.string.ui_invoice_date),
                getString(R.string.ui_member_name),
                getString(R.string.ui_type));
        binding.cardDonut.setVisibility(View.GONE);
        binding.cardBar.setVisibility(View.GONE);
        if (binding.totalAmount.getParent() instanceof View) {
            ((View) binding.totalAmount.getParent()).setVisibility(View.GONE);
        }

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) getActivity()).navigateBack();
                    return true;
                }
                return false;
            }
        });

        binding.toolbar.backToSetting.setOnClickListener(this);
        binding.toolbar.menuIcon.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            ((MainActivity) getActivity()).navigateBack();
        } else if (id == R.id.menuIcon) {
            setPopUpWindow();
        }
    }

    public void setPopUpWindow() {

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.sale_wise_dialog, null);
        PopupWindow mypopupWindow = new PopupWindow(view, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true);

        LinearLayout dayWiseLayout = view.findViewById(R.id.dayWiseLayout);
        LinearLayout monthWiseLayout = view.findViewById(R.id.monthWiseLayout);
        LinearLayout yearWiseLayout = view.findViewById(R.id.yearWiseLayout);

        //Get Current Date
        calender = Calendar.getInstance();
        mYear = calender.get(Calendar.YEAR);
        mMonth = calender.get(Calendar.MONTH);
        mDay = calender.get(Calendar.DAY_OF_MONTH);

        dayWiseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();

                datePickerDialog = new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        if ((monthOfYear + 1) > 9) {
                            if ((dayOfMonth) > 9) {
                                invoiceDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getDateReportList(invoiceDate);
                            } else {
                                invoiceDate = year + "-" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getDateReportList(invoiceDate);
                            }
                        } else {
                            if ((dayOfMonth) > 9) {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getDateReportList(invoiceDate);
                            } else {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getDateReportList(invoiceDate);
                            }
                        }
                    }
                }, mYear, mMonth, mDay);

                datePickerDialog.show();
            }
        });

        monthWiseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();

                MonthYearPickerDialogFragment dialogFragment = MonthYearPickerDialogFragment.getInstance(mMonth, mYear, "Select Month");
                dialogFragment.show(getChildFragmentManager(), null);
                dialogFragment.setOnDateSetListener(new MonthYearPickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(int year, int monthOfYear) {
                        if ((monthOfYear + 1) > 9) {
                            invoiceDate = year + "-" + (monthOfYear + 1);
                            getDateReportList(invoiceDate);
                        } else {
                            invoiceDate = year + "-" + "0" + (monthOfYear + 1);
                            getDateReportList(invoiceDate);
                        }
                    }
                });

            }
        });


        yearWiseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();
                MonthPickerDialog.Builder builder = new MonthPickerDialog.Builder(activity, new MonthPickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(int selectedMonth, int selectedYear) {
                        invoiceDate = "" + selectedYear;
                        mYear = selectedYear;
                        getDateReportList(invoiceDate);
                    }
                }, mYear, 0);

                builder.showYearOnly()
                        .setYearRange(1990, 2050)
                        .build()
                        .show();
            }
        });

        mypopupWindow.showAsDropDown(binding.toolbar.menuIcon, 0, -75);

    }

    public void getDateReportList(String invoiceDate) {
        SweetAlertDialog loader = ListLoader.show(activity);
        try {
            messInvoiceResponseList.clear();
            messInvoiceResponseList = posBillingWalaDatabase.getInvoiceMessInvoiceDateWiseReportList(invoiceDate);
            if (!messInvoiceResponseList.isEmpty()) {

                InvoiceMessReportAdapter adapter = new InvoiceMessReportAdapter(activity, messInvoiceResponseList);
                binding.recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
                binding.recyclerView.setAdapter(adapter);
                binding.dateChip.setText(OperationalReportCharts.formatPeriodLabel(invoiceDate));
                ReportUiHelper.bindKpi(binding.kpi1, getString(R.string.ui_total_bills),
                        String.valueOf(messInvoiceResponseList.size()), "");
                ReportUiHelper.bindKpi(binding.kpi2, getString(R.string.ui_invoice_mess_report),
                        String.valueOf(messInvoiceResponseList.size()), "");
                binding.kpi3.getRoot().setVisibility(View.GONE);
                binding.kpi4.getRoot().setVisibility(View.GONE);
                binding.nestedScrollView.setVisibility(View.VISIBLE);
                binding.noDataFound.setVisibility(View.GONE);
            } else {
                binding.nestedScrollView.setVisibility(View.GONE);
                binding.noDataFound.setVisibility(View.VISIBLE);
            }
        } finally {
            ListLoader.dismiss(loader);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).lockUnlockDrawer(1);
        getInvoiceMessInvoiceReportList();

    }

    public void getInvoiceMessInvoiceReportList() {
        SweetAlertDialog loader = ListLoader.show(activity);
        try {
            messInvoiceResponseList.clear();
            messInvoiceResponseList = posBillingWalaDatabase.getInvoiceMessInvoiceReportList();
            if (!messInvoiceResponseList.isEmpty()) {
                InvoiceMessReportAdapter adapter = new InvoiceMessReportAdapter(activity, messInvoiceResponseList);
                binding.recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
                binding.recyclerView.setAdapter(adapter);
                binding.dateChip.setText(OperationalReportCharts.formatPeriodLabel(""));
                ReportUiHelper.bindKpi(binding.kpi1, getString(R.string.ui_total_bills),
                        String.valueOf(messInvoiceResponseList.size()), "");
                ReportUiHelper.bindKpi(binding.kpi2, getString(R.string.ui_invoice_mess_report),
                        String.valueOf(messInvoiceResponseList.size()), "");
                binding.kpi3.getRoot().setVisibility(View.GONE);
                binding.kpi4.getRoot().setVisibility(View.GONE);
                binding.nestedScrollView.setVisibility(View.VISIBLE);
                binding.noDataFound.setVisibility(View.GONE);
            } else {
                binding.nestedScrollView.setVisibility(View.GONE);
                binding.noDataFound.setVisibility(View.VISIBLE);
            }
        } finally {
            ListLoader.dismiss(loader);
        }
    }
}