package com.pos_billingwala.Extra;

import android.annotation.SuppressLint;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.PrinterSettingResponse;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Print Fast Bill: when ON, the cashier-selected calendar day drives invoice
 * date, print date, and date-wise bill numbering. When OFF, always use now.
 */
public final class BillingDateHelper {

    private static Calendar selectedDay;

    private BillingDateHelper() {
    }

    public static void resetToToday() {
        selectedDay = Calendar.getInstance();
        clearTime(selectedDay);
    }

    public static void setSelectedDay(int year, int month, int dayOfMonth) {
        if (selectedDay == null) {
            selectedDay = Calendar.getInstance();
        }
        selectedDay.set(Calendar.YEAR, year);
        selectedDay.set(Calendar.MONTH, month);
        selectedDay.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        clearTime(selectedDay);
        Calendar today = Calendar.getInstance();
        clearTime(today);
        if (selectedDay.after(today)) {
            selectedDay = (Calendar) today.clone();
        }
    }

    public static Calendar getSelectedDay() {
        if (selectedDay == null) {
            resetToToday();
        }
        return (Calendar) selectedDay.clone();
    }

    public static boolean isPrintFastBillOn(POSBillingWalaDatabase db) {
        if (db == null) {
            return false;
        }
        List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
        if (list == null || list.isEmpty()) {
            return false;
        }
        String flag = list.get(0).getPrintFastBill();
        return flag != null && flag.equalsIgnoreCase("on");
    }

    /** Display label: 24-Sep-2026 */
    public static String formatDisplayDate() {
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
        return fmt.format(getSelectedDay().getTime());
    }

    /**
     * Invoice timestamp used for save / print / numbering.
     * Print Fast Bill ON → selected day + current clock time.
     * OFF → device now.
     */
    public static Date resolveInvoiceDate(boolean printFastBillOn) {
        Calendar now = Calendar.getInstance();
        if (!printFastBillOn) {
            return now.getTime();
        }
        Calendar day = getSelectedDay();
        day.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
        day.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
        day.set(Calendar.SECOND, now.get(Calendar.SECOND));
        day.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND));
        return day.getTime();
    }

    @SuppressLint("SimpleDateFormat")
    public static String formatInvoiceDateTime(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date);
    }

    public static String formatDayKey(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }

    public static String formatNumberDayKey(Date date) {
        return new SimpleDateFormat("dd-MM", Locale.getDefault()).format(date);
    }

    private static void clearTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }
}
