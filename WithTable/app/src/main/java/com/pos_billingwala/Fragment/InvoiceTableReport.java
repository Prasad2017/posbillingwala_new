package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.AsyncTask;
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
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.dewinjm.monthyearpicker.MonthYearPickerDialog;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.InvoiceTableReportAdapter;
import com.pos_billingwala.CalenderView.MonthPickerDialog;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.Extra.ReportUiHelper;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.Extra.OperationalReportCharts;
import com.pos_billingwala.Model.ReportRankItem;
import com.pos_billingwala.databinding.FragmentOperationalReportBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;


@SuppressLint("SetTextI18n")
public class InvoiceTableReport extends Fragment implements View.OnClickListener {

    public static Activity activity;
    static int pageNumber = 0, totalPages, limit = 25;
    public int mYear, mMonth, mDay;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    InvoiceTableReportAdapter adapter;
    List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
    Calendar calender;
    DatePickerDialog datePickerDialog;
    String invoiceDate = "";
    boolean isLoading = false, isDateMonthWise = false;
    FragmentOperationalReportBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOperationalReportBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);
        binding.toolbar.heading.setText(getString(R.string.ui_invoice_table_report));
        binding.toolbar.shareInvoice.setVisibility(View.GONE);
        binding.listTitle.setText(getString(R.string.ui_table_summary));
        binding.donutTitle.setText(getString(R.string.ui_table_number));
        binding.barTitle.setText(getString(R.string.ui_amount_breakdown));

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

        binding.nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(@NonNull NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (ReportCursorHelper.isNestedScrollAtBottom(v, scrollY)) {
                    if (!isLoading && pageNumber < totalPages) {
                        new getDownloadBills().execute();
                    }
                }
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

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
                                getDateTableReport(invoiceDate);
                            } else {
                                invoiceDate = year + "-" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getDateTableReport(invoiceDate);
                            }
                        } else {
                            if ((dayOfMonth) > 9) {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getDateTableReport(invoiceDate);
                            } else {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getDateTableReport(invoiceDate);
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
                            getDateTableReport(invoiceDate);
                        } else {
                            invoiceDate = year + "-" + "0" + (monthOfYear + 1);
                            getDateTableReport(invoiceDate);
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
                        getDateTableReport(invoiceDate);
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

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).lockUnlockDrawer(1);
        getInvoiceTableReportList();
    }

    public void getNewInvoiceRecords() {
        // Unused — paging handled by getDownloadBills (one page per scroll).
    }

    public void getInvoiceTableReportList() {
        if (isLoading) {
            return;
        }
        isLoading = true;
        isDateMonthWise = false;
        invoiceDate = "";
        pageNumber = 0;
        new LoadInitialTableReport("").execute();
    }

    public void getDateTableReport(String selectedDate) {
        if (isLoading) {
            return;
        }
        isLoading = true;
        isDateMonthWise = true;
        invoiceDate = selectedDate;
        pageNumber = 0;
        new LoadInitialTableReport(selectedDate).execute();
    }

    private void bindTablePage(List<InvoiceResponse> page, float totalAmount) {
        invoiceResponseList = new ArrayList<>();
        if (page != null && !page.isEmpty()) {
            invoiceResponseList.addAll(page);
            adapter = new InvoiceTableReportAdapter(activity, invoiceResponseList);
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
            binding.recyclerView.setAdapter(adapter);
            binding.totalAmount.setText(MainActivity.currencyName + " "
                    + String.format(Locale.US, "%.2f", totalAmount));
            String period = OperationalReportCharts.formatPeriodLabel(
                    isDateMonthWise ? invoiceDate : "");
            List<ReportRankItem> breakdown = OperationalReportCharts.groupedBreakdown(
                    posBillingWalaDatabase, "noOfTable",
                    isDateMonthWise ? invoiceDate : "", "invoiceType = 'table_wise'");
            OperationalReportCharts.bindListSummary(binding, activity, totalPages, totalAmount,
                    breakdown, getString(R.string.ui_table_number),
                    getString(R.string.ui_amount_breakdown), period);
            ReportUiHelper.setupTableHeader(binding.tableHeader, getString(R.string.ui_table_number));
            binding.nestedScrollView.setVisibility(View.VISIBLE);
            binding.noDataFound.setVisibility(View.GONE);
            pageNumber = page.size();
        } else {
            binding.nestedScrollView.setVisibility(View.GONE);
            binding.noDataFound.setVisibility(View.VISIBLE);
            pageNumber = 0;
        }
    }

    private class LoadInitialTableReport extends AsyncTask<Void, Void, List<InvoiceResponse>> {
        private final String dateFilter;
        private int count;
        private float totalAmount;
        private SweetAlertDialog loader;

        LoadInitialTableReport(String dateFilter) {
            this.dateFilter = dateFilter == null ? "" : dateFilter;
        }

        @Override
        protected void onPreExecute() {
            loader = ListLoader.show(activity);
        }

        @Override
        protected List<InvoiceResponse> doInBackground(Void... voids) {
            count = posBillingWalaDatabase.getInvoiceTableCount(dateFilter, "table_wise");
            totalAmount = posBillingWalaDatabase.getInvoiceTableTotal(dateFilter, "table_wise");
            if (count <= 0) {
                return new ArrayList<>();
            }
            return posBillingWalaDatabase.getInvoiceTableReportList(dateFilter, "table_wise", 0);
        }

        @Override
        protected void onPostExecute(List<InvoiceResponse> page) {
            try {
                if (!isAdded()) {
                    isLoading = false;
                    return;
                }
                totalPages = count;
                bindTablePage(page, totalAmount);
                isLoading = false;
            } finally {
                ListLoader.dismiss(loader);
            }
        }
    }

    /** Loads exactly one more page on scroll — never chains all pages. */
    private class getDownloadBills extends AsyncTask<Void, Void, List<InvoiceResponse>> {
        @Override
        protected void onPreExecute() {
            if (isLoading || pageNumber >= totalPages || adapter == null) {
                cancel(true);
                return;
            }
            isLoading = true;
        }

        @Override
        protected List<InvoiceResponse> doInBackground(Void... voids) {
            if (isCancelled()) {
                return null;
            }
            String dateFilter = isDateMonthWise ? invoiceDate : "";
            return posBillingWalaDatabase.getInvoiceTableReportList(dateFilter, "table_wise", pageNumber);
        }

        @Override
        protected void onPostExecute(List<InvoiceResponse> page) {
            if (!isAdded()) {
                isLoading = false;
                return;
            }
            if (page != null && !page.isEmpty()) {
                invoiceResponseList.addAll(page);
                adapter.notifyDataSetChanged();
                pageNumber += page.size();
            }
            isLoading = false;
        }

        @Override
        protected void onCancelled() {
            isLoading = false;
        }
    }

}