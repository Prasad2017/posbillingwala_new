package com.pos_billingwala.Fragment;

import com.pos_billingwala.Extra.PopupUi;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import com.github.dewinjm.monthyearpicker.MonthYearPickerDialog;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.CalenderView.MonthPickerDialog;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.LicenseModules;
import com.pos_billingwala.Extra.OperationalReportCharts;
import com.pos_billingwala.Extra.ReportCursorHelper;
import com.pos_billingwala.Extra.ReportUiHelper;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentOperationalReportBinding;

import java.util.Calendar;
import java.util.Locale;

@SuppressLint({"Range", "SetTextI18n", "StaticFieldLeak"})
public class SaleReport extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public int mYear, mMonth, mDay;

    POSBillingWalaDatabase posBillingWalaDatabase;
    float subAmount = 0f, discount = 0f, totalAmount = 0f, totalGSTAmount = 0f,
            tableAmount = 0f, takeAwayAmount = 0f, fastBilling = 0f;
    Calendar calender;
    DatePickerDialog datePickerDialog;
    String invoiceDate = "";
    FragmentOperationalReportBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOperationalReportBinding.inflate(inflater, container, false);
        activity = getActivity();
        ReportUiHelper.applyOperationalReportLayout(activity, binding);
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        binding.toolbar.heading.setText(getString(R.string.ui_sale_reports));
        binding.toolbar.shareInvoice.setVisibility(View.GONE);
        binding.listTitle.setText(getString(R.string.ui_sale_wise_report));

        View root = binding.getRoot();
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("SaleReport", "back pressed");
                ((MainActivity) activity).navigateBack();
                return true;
            }
            return false;
        });

        binding.toolbar.backToSetting.setOnClickListener(this);
        binding.toolbar.menuIcon.setOnClickListener(this);
        applyModuleVisibility();
        return root;
    }

    private void applyModuleVisibility() {
        if (!LicenseModules.isEnabled(MainActivity.fastBilling) && fastBilling <= 0f) {
            // amounts hidden via chart binding when zero
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.menuIcon) {
            setPopUpWindow();
        }
    }

    public void setPopUpWindow() {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.sale_wise_dialog, null);
        PopupWindow mypopupWindow = PopupUi.create(activity, popupView);

        LinearLayout dayWiseLayout = popupView.findViewById(R.id.dayWiseLayout);
        LinearLayout monthWiseLayout = popupView.findViewById(R.id.monthWiseLayout);
        LinearLayout yearWiseLayout = popupView.findViewById(R.id.yearWiseLayout);

        calender = Calendar.getInstance();
        mYear = calender.get(Calendar.YEAR);
        mMonth = calender.get(Calendar.MONTH);
        mDay = calender.get(Calendar.DAY_OF_MONTH);

        dayWiseLayout.setOnClickListener(v -> {
            mypopupWindow.dismiss();
            datePickerDialog = new DatePickerDialog(activity, (view, year, monthOfYear, dayOfMonth) -> {
                invoiceDate = formatDay(year, monthOfYear, dayOfMonth);
                getDateSaleReports(invoiceDate);
            }, mYear, mMonth, mDay);
            datePickerDialog.show();
        });

        monthWiseLayout.setOnClickListener(v -> {
            mypopupWindow.dismiss();
            MonthYearPickerDialogFragment dialogFragment =
                    MonthYearPickerDialogFragment.getInstance(mMonth, mYear, "Select Month");
            dialogFragment.show(getChildFragmentManager(), null);
            dialogFragment.setOnDateSetListener((year, monthOfYear) -> {
                invoiceDate = monthOfYear > 9 ? year + "-" + (monthOfYear + 1)
                        : year + "-0" + (monthOfYear + 1);
                getDateSaleReports(invoiceDate);
            });
        });

        yearWiseLayout.setOnClickListener(v -> {
            mypopupWindow.dismiss();
            MonthPickerDialog.Builder builder = new MonthPickerDialog.Builder(activity,
                    (selectedMonth, selectedYear) -> {
                        invoiceDate = String.valueOf(selectedYear);
                        mYear = selectedYear;
                        getDateSaleReports(invoiceDate);
                    }, mYear, 0);
            builder.showYearOnly().setYearRange(1990, 2050).build().show();
        });

        PopupUi.showAsToolbarMenu(mypopupWindow, binding.toolbar.menuIcon);
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
        invoiceDate = "";
        getSaleReports();
    }

    public void getSaleReports() {
        discount = 0f;
        getSaleDiscountReport(null);
        SQLiteDatabase database = posBillingWalaDatabase.getReadableDatabase();
        try {
            Cursor cursor = database.rawQuery(
                    "SELECT SUM(totalAmount) as totalAmount, SUM(subTotal) as subTotal, "
                            + "SUM(totalGSTAmount) as totalGSTAmount FROM "
                            + POSBillingWalaDatabase.INVOICE_TABLE
                            + " WHERE " + POSBillingWalaDatabase.notRefundedClause(), null);
            if (cursor.moveToNext()) {
                subAmount = ReportCursorHelper.readFloat(cursor, "subTotal");
                totalAmount = ReportCursorHelper.readFloat(cursor, "totalAmount");
                totalGSTAmount = ReportCursorHelper.readFloat(cursor, "totalGSTAmount");
            }
            cursor.close();
            tableAmount = queryTypeTotal(database, "table_wise", null);
            takeAwayAmount = queryTypeTotal(database, "take_away", null);
            fastBilling = queryTypeTotal(database, "fast_billing", null);
        } finally {
            database.close();
        }
        bindUi(OperationalReportCharts.formatPeriodLabel(""));
    }

    @SuppressLint("Range")
    public void getSaleDiscountReport(String dateFilter) {
        float runningDiscount = 0f;
        SQLiteDatabase database = posBillingWalaDatabase.getReadableDatabase();
        try {
            String sql = "SELECT * FROM " + POSBillingWalaDatabase.INVOICE_TABLE
                    + " WHERE " + POSBillingWalaDatabase.notRefundedClause();
            if (dateFilter != null && !dateFilter.isEmpty()) {
                sql += " AND invoiceDate LIKE '%" + dateFilter + "%'";
            }
            Cursor cursor = database.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                float disc = ReportCursorHelper.readFloat(cursor, "discount");
                float subAmt = ReportCursorHelper.readFloat(cursor, "subTotal");
                String discountType = cursor.getString(cursor.getColumnIndex("discountType"));
                if (discountType != null && discountType.equalsIgnoreCase("Amount")) {
                    // flat amount
                } else if (disc != 0f) {
                    disc = subAmt / (100f / disc);
                } else {
                    disc = 0f;
                }
                runningDiscount += disc;
            }
            cursor.close();
            discount = runningDiscount;
        } finally {
            database.close();
        }
    }

    private float queryTypeTotal(SQLiteDatabase database, String invoiceType, String invoiceDateFilter) {
        String sql = "SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE
                + " WHERE invoiceType = '" + invoiceType + "'" + POSBillingWalaDatabase.andNotRefunded();
        if (invoiceDateFilter != null && !invoiceDateFilter.isEmpty()) {
            sql += " AND invoiceDate LIKE '%" + invoiceDateFilter + "%'";
        }
        Cursor cursor = database.rawQuery(sql, null);
        float amount = 0f;
        if (cursor.moveToNext()) {
            amount = ReportCursorHelper.readFloat(cursor, "totalAmount");
        }
        cursor.close();
        return amount;
    }

    @SuppressLint("Range")
    public void getDateSaleReports(String selectedDate) {
        getSaleDiscountReport(selectedDate);
        SQLiteDatabase database = posBillingWalaDatabase.getReadableDatabase();
        try {
            Cursor cursor = database.rawQuery(
                    "SELECT SUM(totalAmount) as totalAmount, SUM(subTotal) as subTotal, "
                            + "SUM(totalGSTAmount) as totalGSTAmount FROM "
                            + POSBillingWalaDatabase.INVOICE_TABLE
                            + " WHERE invoiceDate LIKE '%" + selectedDate + "%'"
                            + POSBillingWalaDatabase.andNotRefunded(), null);
            if (cursor.moveToNext()) {
                subAmount = ReportCursorHelper.readFloat(cursor, "subTotal");
                totalGSTAmount = ReportCursorHelper.readFloat(cursor, "totalGSTAmount");
                totalAmount = ReportCursorHelper.readFloat(cursor, "totalAmount");
            }
            cursor.close();
            tableAmount = queryTypeTotal(database, "table_wise", selectedDate);
            takeAwayAmount = queryTypeTotal(database, "take_away", selectedDate);
            fastBilling = queryTypeTotal(database, "fast_billing", selectedDate);
        } finally {
            database.close();
        }
        bindUi(OperationalReportCharts.formatPeriodLabel(selectedDate));
    }

    private void bindUi(String periodLabel) {
        if (!isAdded() || binding == null) {
            return;
        }
        float shownFast = LicenseModules.isEnabled(MainActivity.fastBilling) ? fastBilling : 0f;
        float shownTable = LicenseModules.isEnabled(MainActivity.dineIn) ? tableAmount : 0f;
        float shownTakeAway = LicenseModules.isEnabled(MainActivity.takeAway) ? takeAwayAmount : 0f;
        float cashTotal = posBillingWalaDatabase.getInvoiceTenderCashTotal(invoiceDate);
        float upiTotal = posBillingWalaDatabase.getInvoiceTenderUpiTotal(invoiceDate);
        OperationalReportCharts.bindSaleSummary(binding, requireContext(),
                subAmount, totalGSTAmount, discount, totalAmount,
                shownFast, shownTable, shownTakeAway, periodLabel, cashTotal, upiTotal);
        binding.nestedScrollView.setVisibility(View.VISIBLE);
        binding.noDataFound.setVisibility(View.GONE);
    }
}
