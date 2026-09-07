package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.Model.ServiceAppointmentResponse;
import com.pos_billingwala.Model.StaffUserResponse;
import com.pos_billingwala.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Salon / service appointments — local {@code service_appointment} table.
 * Day calendar + optional staff assignment. Cloud sync for appointments + staff roster.
 */
public final class SalonAppointmentModule {

    public static final String STATUS_BOOKED = "booked";
    public static final String STATUS_DONE = "done";
    public static final String STATUS_CANCELLED = "cancelled";

    private SalonAppointmentModule() {
    }

    public static boolean isEnabled(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.APPOINTMENTS);
    }

    public static boolean isServiceFamily(Context context) {
        String type = BusinessTypes.normalize(BusinessSession.getBusinessType(context));
        switch (type) {
            case BusinessTypes.SALON:
            case BusinessTypes.LAUNDRY:
            case BusinessTypes.CAR_WASH:
            case BusinessTypes.REPAIR:
            case BusinessTypes.HEALTHCARE:
                return true;
            default:
                return false;
        }
    }

    public static boolean shouldPromptBook(Context context) {
        return isEnabled(context);
    }

    public static BillingMode preferredBillingMode() {
        return BillingMode.FAST;
    }

    public static void showBookDialog(Activity activity, POSBillingWalaDatabase db,
                                      ProductResponse product, Runnable afterSave) {
        if (activity == null || db == null || product == null) {
            if (afterSave != null) {
                afterSave.run();
            }
            return;
        }
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, pad / 2);
        EditText name = new EditText(activity);
        name.setHint(activity.getString(R.string.appointment_customer_hint));
        name.setSingleLine(true);
        EditText mobile = new EditText(activity);
        mobile.setHint(activity.getString(R.string.appointment_mobile_hint));
        mobile.setInputType(InputType.TYPE_CLASS_PHONE);
        mobile.setSingleLine(true);
        EditText when = new EditText(activity);
        when.setHint(activity.getString(R.string.appointment_when_hint));
        when.setSingleLine(true);
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        when.setText(fmt.format(cal.getTime()));
        EditText notes = new EditText(activity);
        notes.setHint(activity.getString(R.string.appointment_notes_hint));

        List<StaffUserResponse> roster = db.getActiveStaffUsers();
        Spinner staffSpinner = null;
        List<StaffUserResponse> spinnerStaff = new ArrayList<>();
        if (!roster.isEmpty()) {
            staffSpinner = new Spinner(activity);
            List<String> staffLabels = new ArrayList<>();
            staffLabels.add(activity.getString(R.string.appointment_staff_any));
            spinnerStaff.add(null);
            String activeId = SecurityPermissions.getActiveStaffId(activity);
            int selected = 0;
            for (int i = 0; i < roster.size(); i++) {
                StaffUserResponse s = roster.get(i);
                staffLabels.add(s.getStaffName());
                spinnerStaff.add(s);
                if (s.getStaffId() != null && s.getStaffId().equals(activeId)) {
                    selected = i + 1;
                }
            }
            staffSpinner.setAdapter(new ArrayAdapter<>(activity,
                    android.R.layout.simple_spinner_dropdown_item, staffLabels));
            staffSpinner.setSelection(selected);
        }

        box.addView(name);
        box.addView(mobile);
        box.addView(when);
        if (staffSpinner != null) {
            box.addView(staffSpinner);
        }
        box.addView(notes);

        final Spinner staffSpinnerFinal = staffSpinner;
        final List<StaffUserResponse> spinnerStaffFinal = spinnerStaff;
        new AlertDialog.Builder(activity)
                .setTitle(R.string.appointment_book_title)
                .setView(box)
                .setPositiveButton(R.string.appointment_save_and_bill, (d, w) -> {
                    String cName = name.getText() != null ? name.getText().toString().trim() : "";
                    if (cName.isEmpty()) {
                        Toast.makeText(activity, R.string.appointment_customer_required, Toast.LENGTH_SHORT).show();
                        if (afterSave != null) {
                            afterSave.run();
                        }
                        return;
                    }
                    String staffId = "";
                    String staffName = "";
                    if (staffSpinnerFinal != null) {
                        int sel = staffSpinnerFinal.getSelectedItemPosition();
                        if (sel > 0 && sel < spinnerStaffFinal.size()) {
                            StaffUserResponse picked = spinnerStaffFinal.get(sel);
                            if (picked != null) {
                                staffId = picked.getStaffId() != null ? picked.getStaffId() : "";
                                staffName = picked.getStaffName() != null ? picked.getStaffName() : "";
                            }
                        }
                    }
                    db.addServiceAppointment(
                            product.getProductId(),
                            product.getProductName(),
                            cName,
                            mobile.getText() != null ? mobile.getText().toString().trim() : "",
                            when.getText() != null ? when.getText().toString().trim() : "",
                            notes.getText() != null ? notes.getText().toString().trim() : "",
                            staffId,
                            staffName);
                    Toast.makeText(activity, R.string.appointment_saved, Toast.LENGTH_SHORT).show();
                    if (afterSave != null) {
                        afterSave.run();
                    }
                })
                .setNeutralButton(R.string.appointment_bill_only, (d, w) -> {
                    if (afterSave != null) {
                        afterSave.run();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Opens local day calendar (Master Data long-press). */
    public static void showUpcomingDialog(Activity activity, POSBillingWalaDatabase db) {
        showDayCalendar(activity, db, Calendar.getInstance());
    }

    public static void showDayCalendar(Activity activity, POSBillingWalaDatabase db, Calendar day) {
        if (activity == null || db == null) {
            return;
        }
        if (day == null) {
            day = Calendar.getInstance();
        }
        final Calendar dayFinal = (Calendar) day.clone();
        SimpleDateFormat dayFmt = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        SimpleDateFormat titleFmt = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
        String dayKey = dayFmt.format(dayFinal.getTime());
        List<ServiceAppointmentResponse> list = db.getAppointmentsForDay(dayKey);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad / 2, pad, pad / 2);

        LinearLayout nav = new LinearLayout(activity);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button prev = new Button(activity);
        prev.setText("‹");
        Button next = new Button(activity);
        next.setText("›");
        TextView title = new TextView(activity);
        title.setText(titleFmt.format(dayFinal.getTime()));
        title.setTextSize(16f);
        title.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleLp);
        nav.addView(prev);
        nav.addView(title);
        nav.addView(next);
        root.addView(nav);

        TextView body = new TextView(activity);
        body.setPadding(0, pad / 2, 0, pad / 2);
        body.setTextSize(14f);
        if (list.isEmpty()) {
            body.setText(activity.getString(R.string.appointment_none_day));
        } else {
            StringBuilder sb = new StringBuilder();
            for (ServiceAppointmentResponse a : list) {
                sb.append(formatAppointmentLine(a)).append('\n');
            }
            body.setText(sb.toString().trim());
        }
        root.addView(body);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.appointment_calendar_title)
                .setView(root)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.appointment_manage_day, null)
                .setNegativeButton(R.string.appointment_week_view, null)
                .create();
        dialog.setOnShowListener(d -> {
            prev.setOnClickListener(v -> {
                dialog.dismiss();
                dayFinal.add(Calendar.DAY_OF_MONTH, -1);
                showDayCalendar(activity, db, dayFinal);
            });
            next.setOnClickListener(v -> {
                dialog.dismiss();
                dayFinal.add(Calendar.DAY_OF_MONTH, 1);
                showDayCalendar(activity, db, dayFinal);
            });
            Button manage = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (manage != null) {
                manage.setOnClickListener(v -> {
                    if (list.isEmpty()) {
                        Toast.makeText(activity, R.string.appointment_none_day, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                    showDayAppointmentPicker(activity, db, dayFinal, list);
                });
            }
            Button weekBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (weekBtn != null) {
                weekBtn.setOnClickListener(v -> {
                    dialog.dismiss();
                    showWeekGrid(activity, db, dayFinal);
                });
            }
        });
        dialog.show();
    }

    /** Mon–Sun overview for the week containing {@code anyDayInWeek}. */
    public static void showWeekGrid(Activity activity, POSBillingWalaDatabase db, Calendar anyDayInWeek) {
        if (activity == null || db == null) {
            return;
        }
        Calendar weekStart = (Calendar) (anyDayInWeek != null
                ? anyDayInWeek.clone() : Calendar.getInstance());
        int dow = weekStart.get(Calendar.DAY_OF_WEEK);
        int shift = (dow == Calendar.SUNDAY) ? -6 : (Calendar.MONDAY - dow);
        weekStart.add(Calendar.DAY_OF_MONTH, shift);
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);

        SimpleDateFormat dayFmt = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        SimpleDateFormat labelFmt = new SimpleDateFormat("EEE dd MMM", Locale.getDefault());
        SimpleDateFormat rangeFmt = new SimpleDateFormat("dd MMM", Locale.getDefault());

        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Calendar> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Calendar d = (Calendar) weekStart.clone();
            d.add(Calendar.DAY_OF_MONTH, i);
            days.add(d);
            String key = dayFmt.format(d.getTime());
            int count = db.getAppointmentsForDay(key).size();
            labels.add(labelFmt.format(d.getTime()) + " · " + count
                    + (count == 1 ? " apt" : " apts"));
        }
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 6);
        String title = activity.getString(R.string.appointment_week_title,
                rangeFmt.format(weekStart.getTime()), rangeFmt.format(weekEnd.getTime()));

        ArrayList<String> choices = new ArrayList<>(labels);
        choices.add(activity.getString(R.string.appointment_week_prev));
        choices.add(activity.getString(R.string.appointment_week_next));

        BottomSheetUi.showSingleChoice(activity, title, choices.toArray(new String[0]), -1, true,
                index -> {
                    if (index >= 0 && index < 7) {
                        showDayCalendar(activity, db, days.get(index));
                    } else if (index == 7) {
                        Calendar prev = (Calendar) weekStart.clone();
                        prev.add(Calendar.DAY_OF_MONTH, -7);
                        showWeekGrid(activity, db, prev);
                    } else if (index == 8) {
                        Calendar next = (Calendar) weekStart.clone();
                        next.add(Calendar.DAY_OF_MONTH, 7);
                        showWeekGrid(activity, db, next);
                    }
                });
    }

    private static void showDayAppointmentPicker(Activity activity, POSBillingWalaDatabase db,
                                                 Calendar day, List<ServiceAppointmentResponse> list) {
        String[] labels = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            labels[i] = formatAppointmentLine(list.get(i));
        }
        BottomSheetUi.showSingleChoice(activity,
                activity.getString(R.string.appointment_manage_day), labels, -1, true, index -> {
                    if (index >= 0 && index < list.size()) {
                        showAppointmentActions(activity, db, day, list.get(index));
                    }
                });
    }

    private static void showAppointmentActions(Activity activity, POSBillingWalaDatabase db,
                                               Calendar day, ServiceAppointmentResponse appt) {
        if (appt == null) {
            return;
        }
        String status = appt.getAppointmentStatus() != null ? appt.getAppointmentStatus() : STATUS_BOOKED;
        ArrayList<String> actions = new ArrayList<>();
        ArrayList<String> actionIds = new ArrayList<>();
        if (!STATUS_DONE.equalsIgnoreCase(status)) {
            actions.add(activity.getString(R.string.appointment_mark_done));
            actionIds.add(STATUS_DONE);
        }
        if (!STATUS_CANCELLED.equalsIgnoreCase(status)) {
            actions.add(activity.getString(R.string.appointment_mark_cancelled));
            actionIds.add(STATUS_CANCELLED);
        }
        if (!STATUS_BOOKED.equalsIgnoreCase(status)) {
            actions.add(activity.getString(R.string.appointment_mark_booked));
            actionIds.add(STATUS_BOOKED);
        }
        actions.add(activity.getString(R.string.appointment_back_to_day));
        actionIds.add("_back");
        BottomSheetUi.showSingleChoice(activity, formatAppointmentLine(appt),
                actions.toArray(new String[0]), -1, true, index -> {
                    if (index < 0 || index >= actionIds.size()) {
                        return;
                    }
                    String id = actionIds.get(index);
                    if ("_back".equals(id)) {
                        showDayCalendar(activity, db, day);
                        return;
                    }
                    db.updateAppointmentStatus(appt.getAppointmentId(), id);
                    Toast.makeText(activity, R.string.appointment_status_updated, Toast.LENGTH_SHORT).show();
                    showDayCalendar(activity, db, day);
                });
    }

    private static String formatAppointmentLine(ServiceAppointmentResponse a) {
        StringBuilder sb = new StringBuilder();
        String status = a.getAppointmentStatus() != null ? a.getAppointmentStatus() : STATUS_BOOKED;
        sb.append('[').append(status).append("] ");
        String at = a.getAppointmentAt() != null ? a.getAppointmentAt() : "-";
        int sp = at.indexOf(' ');
        sb.append(sp > 0 ? at.substring(sp + 1) : at);
        sb.append(" — ").append(a.getCustomerName() != null ? a.getCustomerName() : "");
        sb.append(" / ").append(a.getProductName() != null ? a.getProductName() : "");
        if (a.getStaffName() != null && !a.getStaffName().trim().isEmpty()) {
            sb.append(" · ").append(a.getStaffName().trim());
        }
        return sb.toString();
    }

    public static String moduleSummary(Context context) {
        POSBillingWalaDatabase db = context != null ? new POSBillingWalaDatabase(context) : null;
        int staffCount = 0;
        int todayCount = 0;
        try {
            if (db != null) {
                staffCount = db.getActiveStaffUsers().size();
                String dayKey = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        .format(Calendar.getInstance().getTime());
                todayCount = db.getAppointmentsForDay(dayKey).size();
            }
        } catch (Exception ignored) {
        }
        return "Appointments: " + (isEnabled(context) ? "on (day + week calendar)" : "off")
                + "\nService family: " + (isServiceFamily(context) ? "yes" : "no")
                + "\nToday: " + todayCount + " · Staff roster: " + staffCount
                + "\nBill via " + preferredBillingMode().getWireValue()
                + " · Sync: upload on cloud sync (status=1)";
    }
}
