package com.pos_billingwala.Fragment;

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
import com.pos_billingwala.Adapter.ExpenseAdapter;
import com.pos_billingwala.CalenderView.MonthPickerDialog;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Extra.SimpleDividerItemDecoration;
import com.pos_billingwala.Model.ExpenseResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentInvoiceExpenseReportBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;

@SuppressLint("SetTextI18n")
public class InvoiceExpenseReport extends Fragment implements View.OnClickListener {


    public static Activity activity;
    public int mYear, mMonth, mDay;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    List<ExpenseResponse> expenseResponseList = new ArrayList<>();
    ExpenseAdapter adapter;
    Calendar calender;
    DatePickerDialog datePickerDialog;
    String expenseDate = "";
    FragmentInvoiceExpenseReportBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInvoiceExpenseReportBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();


        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).goBackTo(new ReportSetting(), true);
                    return true;
                }
                return false;
            }
        });

        binding.backToSetting.setOnClickListener(this);
        binding.menuIcon.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            ((MainActivity) activity).goBackTo(new ReportSetting(), true);
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
        LinearLayout tableWiseLayout = view.findViewById(R.id.tableWiseLayout);

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
                                expenseDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getDateReportList(expenseDate);
                            } else {
                                expenseDate = year + "-" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getDateReportList(expenseDate);
                            }
                        } else {
                            if ((dayOfMonth) > 9) {
                                expenseDate = year + "-" + "0" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getDateReportList(expenseDate);
                            } else {
                                expenseDate = year + "-" + "0" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getDateReportList(expenseDate);
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
                            expenseDate = year + "-" + (monthOfYear + 1);
                            getDateReportList(expenseDate);
                        } else {
                            expenseDate = year + "-" + "0" + (monthOfYear + 1);
                            getDateReportList(expenseDate);
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
                        expenseDate = "" + selectedYear;
                        mYear = selectedYear;
                        getDateReportList(expenseDate);
                    }
                }, mYear, 0);

                builder.showYearOnly()
                        .setYearRange(1990, 2050)
                        .build()
                        .show();
            }
        });

        mypopupWindow.showAsDropDown(binding.menuIcon, 0, -75);

    }

    public void getDateReportList(String expenseDate) {
        SweetAlertDialog loader = ListLoader.show(activity);
        try {
            expenseResponseList.clear();
            expenseResponseList = posBillingWalaDatabase.getDateWiseExpenseList(expenseDate);
            if (!expenseResponseList.isEmpty()) {

                adapter = new ExpenseAdapter(activity, expenseResponseList);
                binding.recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
                binding.recyclerView.addItemDecoration(new SimpleDividerItemDecoration(activity));
                binding.recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                // adapter.notifyItemInserted(expenseResponseList.size() - 1);

                float totalExpenseAmount = 0f;
                for (ExpenseResponse expenseResponse : expenseResponseList) {
                    totalExpenseAmount += Float.parseFloat(expenseResponse.getExpenseAmount());
                }
                binding.totalAmount.setText(activity.getString(R.string.inr) + " " + String.format(Locale.US, "%.2f", totalExpenseAmount));

                binding.noDataFound.setVisibility(View.GONE);
                binding.nestedScrollView.setVisibility(View.VISIBLE);

            } else {
                binding.noDataFound.setVisibility(View.VISIBLE);
                binding.nestedScrollView.setVisibility(View.GONE);
            }
        } finally {
            ListLoader.dismiss(loader);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getExpenseList();
    }


    public void getExpenseList() {
        SweetAlertDialog loader = ListLoader.show(activity);
        try {
            expenseResponseList.clear();
            expenseResponseList = posBillingWalaDatabase.getExpenseList();
            if (!expenseResponseList.isEmpty()) {
                adapter = new ExpenseAdapter(activity, expenseResponseList);
                binding.recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
                binding.recyclerView.addItemDecoration(new SimpleDividerItemDecoration(activity));
                binding.recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                //  adapter.notifyItemInserted(expenseResponseList.size() - 1);

                float totalExpenseAmount = 0f;
                for (ExpenseResponse expenseResponse : expenseResponseList) {
                    totalExpenseAmount += Float.parseFloat(expenseResponse.getExpenseAmount());
                }
                binding.totalAmount.setText(activity.getString(R.string.inr) + " " + String.format(Locale.US, "%.2f", totalExpenseAmount));

                binding.noDataFound.setVisibility(View.GONE);
                binding.nestedScrollView.setVisibility(View.VISIBLE);

            } else {
                binding.noDataFound.setVisibility(View.VISIBLE);
                binding.nestedScrollView.setVisibility(View.GONE);
            }
        } finally {
            ListLoader.dismiss(loader);
        }
    }

}