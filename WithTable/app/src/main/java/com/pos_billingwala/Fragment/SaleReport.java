package com.pos_billingwala.Fragment;

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
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentSaleReportBinding;

import java.util.Calendar;
import java.util.Locale;


@SuppressLint({"Range", "SetTextI18n, StaticFieldLeak"})
public class SaleReport extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public int mYear, mMonth, mDay;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    float subAmount = 0f, discount = 0f, totalAmount = 0f, totalGSTAmount = 0f, tableAmount = 0f, takeAwayAmount = 0f, fastBilling = 0f;
    Calendar calender;
    DatePickerDialog datePickerDialog;
    String invoiceDate = "";
    FragmentSaleReportBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSaleReportBinding.inflate(inflater, container, false);
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
                    ((MainActivity) activity).navigateBack();
                    return true;
                }
                return false;
            }
        });

        binding.backToSetting.setOnClickListener(this);
        binding.menuIcon.setOnClickListener(this);

        applyModuleVisibility();

        return view;
    }

    private void applyModuleVisibility() {
        LicenseModules.setVisible(binding.totalFastBillingAmountCardView,
                LicenseModules.isEnabled(MainActivity.fastBilling));
        LicenseModules.setVisible(binding.totalTableAmountCardView,
                LicenseModules.isEnabled(MainActivity.dineIn));
        LicenseModules.setVisible(binding.totalTakeAwayAmountCardView,
                LicenseModules.isEnabled(MainActivity.takeAway));
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
                        if ((monthOfYear) > 9) {
                            if ((dayOfMonth) > 9) {
                                invoiceDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getDateSaleReports(invoiceDate);
                            } else {
                                invoiceDate = year + "-" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getDateSaleReports(invoiceDate);
                            }
                        } else {
                            if ((dayOfMonth) > 9) {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + dayOfMonth;
                                getDateSaleReports(invoiceDate);
                            } else {
                                invoiceDate = year + "-" + "0" + (monthOfYear + 1) + "-" + "0" + dayOfMonth;
                                getDateSaleReports(invoiceDate);
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
                        if ((monthOfYear) > 9) {
                            invoiceDate = year + "-" + (monthOfYear + 1);
                            getDateSaleReports(invoiceDate);
                        } else {
                            invoiceDate = year + "-" + "0" + (monthOfYear + 1);
                            getDateSaleReports(invoiceDate);
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
                        getDateSaleReports(invoiceDate);
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

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getSaleReports();
    }


    public void getSaleReports() {

        getSaleDiscountReport();

        SQLiteDatabase database = posBillingWalaDatabase.getReadableDatabase();
        Cursor cursor;
        cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount, SUM(subTotal) as subTotal, SUM(totalGSTAmount) as totalGSTAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE, null);
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndex("subTotal")) != null) {
                subAmount = Float.parseFloat(cursor.getString(cursor.getColumnIndex("subTotal")));
            } else {
                subAmount = 0f;
            }

            if (cursor.getString(cursor.getColumnIndex("totalAmount")) != null) {
                totalAmount = Float.parseFloat(cursor.getString(cursor.getColumnIndex("totalAmount")));
            } else {
                totalAmount = 0f;
            }

            if (cursor.getString(cursor.getColumnIndex("totalGSTAmount")) != null) {
                totalGSTAmount = Float.parseFloat(cursor.getString(cursor.getColumnIndex("totalGSTAmount")));
            } else {
                totalGSTAmount = 0f;
            }

            binding.totalSubAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", subAmount));
            binding.totalGST.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalGSTAmount));
            binding.totalAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalAmount));

        }

        cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE + " WHERE invoiceType = 'table_wise'", null);
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndex("totalAmount")) != null) {
                tableAmount = Float.parseFloat(cursor.getString(cursor.getColumnIndex("totalAmount")));
            } else {
                tableAmount = 0f;
            }

            binding.totalTableAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", tableAmount));

        }

        cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE + " WHERE invoiceType = 'take_away'", null);
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndex("totalAmount")) != null) {
                takeAwayAmount = Float.parseFloat(cursor.getString(cursor.getColumnIndex("totalAmount")));
            } else {
                takeAwayAmount = 0f;
            }

            binding.takeAwayAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", takeAwayAmount));

        }

        cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE + " WHERE invoiceType = 'fast_billing'", null);
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndex("totalAmount")) != null) {
                fastBilling = Float.parseFloat(cursor.getString(cursor.getColumnIndex("totalAmount")));
            } else {
                fastBilling = 0f;
            }

            binding.fastBillingAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", fastBilling));

        }

        database.close();

    }

    @SuppressLint("Range")
    public void getSaleDiscountReport() {

        SQLiteDatabase database = posBillingWalaDatabase.getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM " + POSBillingWalaDatabase.INVOICE_TABLE, null);
        while (cursor.moveToNext()) {
            float disc = Float.parseFloat(cursor.getString(cursor.getColumnIndex("discount")));
            float subAmt = Float.parseFloat(cursor.getString(cursor.getColumnIndex("subTotal")));

            if (cursor.getString(cursor.getColumnIndex("discountType")) != null) {
                if (cursor.getString(cursor.getColumnIndex("discountType")).equalsIgnoreCase("Amount")) {
                    disc = disc;
                } else {
                    disc = subAmt / (100 / disc);
                }
            } else {
                disc = subAmt / (100 / disc);
            }

            discount += disc;

        }

        binding.totalDiscount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", discount));

        database.close();

    }

    @SuppressLint("Range")
    public void getDateSaleReports(String invoiceDate) {

        getDateWiseDiscount(invoiceDate);

        SQLiteDatabase database = posBillingWalaDatabase.getReadableDatabase();
        Cursor cursor;
        cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount, SUM(subTotal) as subTotal, SUM(totalGSTAmount) as totalGSTAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE + " WHERE invoiceDate LIKE '%" + invoiceDate + "%'", null);
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndex("subTotal")) != null) {
                subAmount = Float.parseFloat(cursor.getString(cursor.getColumnIndex("subTotal")));
            } else {
                subAmount = 0f;
            }

            if (cursor.getString(cursor.getColumnIndex("totalGSTAmount")) != null) {
                totalGSTAmount = Float.parseFloat(cursor.getString(cursor.getColumnIndex("totalGSTAmount")));
            } else {
                totalGSTAmount = 0f;
            }

            if (cursor.getString(cursor.getColumnIndex("totalAmount")) != null) {
                totalAmount = Float.parseFloat(cursor.getString(cursor.getColumnIndex("totalAmount")));
            } else {
                totalAmount = 0f;
            }

            binding.totalSubAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", subAmount));
            binding.totalGST.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalGSTAmount));
            binding.totalAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", totalAmount));

        }

        cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE + " WHERE invoiceType = 'table_wise' AND invoiceDate LIKE '%" + invoiceDate + "%'", null);
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndex("totalAmount")) != null) {
                tableAmount = Float.parseFloat(cursor.getString(cursor.getColumnIndex("totalAmount")));
            } else {
                tableAmount = 0f;
            }

            binding.totalTableAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", tableAmount));

        }

        cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE + " WHERE invoiceType = 'take_away' AND invoiceDate LIKE '%" + invoiceDate + "%'", null);
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndex("totalAmount")) != null) {
                takeAwayAmount = Float.parseFloat(cursor.getString(cursor.getColumnIndex("totalAmount")));
            } else {
                takeAwayAmount = 0f;
            }

            binding.takeAwayAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", takeAwayAmount));

        }

        cursor = database.rawQuery("SELECT SUM(totalAmount) as totalAmount FROM " + POSBillingWalaDatabase.INVOICE_TABLE + " WHERE invoiceType = 'fast_billing' AND invoiceDate LIKE '%" + invoiceDate + "%'", null);
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndex("totalAmount")) != null) {
                fastBilling = Float.parseFloat(cursor.getString(cursor.getColumnIndex("totalAmount")));
            } else {
                fastBilling = 0f;
            }

            binding.fastBillingAmount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", fastBilling));

        }

        database.close();
    }

    @SuppressLint("Range")
    public void getDateWiseDiscount(String invoiceDate) {


        float discount = 0f;
        SQLiteDatabase database = posBillingWalaDatabase.getReadableDatabase();
        Cursor cursor;
        cursor = database.rawQuery("SELECT * FROM " + POSBillingWalaDatabase.INVOICE_TABLE + " WHERE invoiceDate LIKE '%" + invoiceDate + "%'", null);
        while (cursor.moveToNext()) {

            float disc = Float.parseFloat(cursor.getString(cursor.getColumnIndex("discount")));
            float subAmt = Float.parseFloat(cursor.getString(cursor.getColumnIndex("subTotal")));

            if (cursor.getString(cursor.getColumnIndex("discountType")) != null) {
                if (cursor.getString(cursor.getColumnIndex("discountType")).equalsIgnoreCase("Amount")) {
                    disc = disc;
                } else {
                    disc = subAmt / (100 / disc);
                }
            } else {
                disc = subAmt / (100 / disc);
            }

            discount += disc;

        }

        binding.totalDiscount.setText(MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", discount));

        database.close();

    }

}