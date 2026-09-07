package com.posbillingwala.owner.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Adapter.OutletAppointmentAdapter;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.LicenseResponse;
import com.posbillingwala.owner.Model.ServiceAppointmentResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentOutletAppointmentCalendarBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Owner day/week appointment calendar for a franchise outlet.
 * Tap an appointment to change status (booked / done / cancelled); POS picks up on Fetch Data.
 */
public class OutletAppointmentCalendar extends Fragment {

    private Activity activity;
    private FragmentOutletAppointmentCalendarBinding binding;
    private final List<LicenseResponse> outlets = new ArrayList<>();
    private final List<ServiceAppointmentResponse> allAppointments = new ArrayList<>();
    private final OutletAppointmentAdapter adapter = new OutletAppointmentAdapter();
    private LicenseResponse selectedOutlet;
    private final Calendar selectedDay = Calendar.getInstance();
    private boolean weekMode;
    private boolean suppressOutletLoad;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOutletAppointmentCalendarBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        activity = getActivity();

        binding.toolbar.toolbarTitle.setText(getString(R.string.outlet_appt_title));
        binding.toolbar.backButton.setOnClickListener(v -> goBack());
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                goBack();
                return true;
            }
            return false;
        });

        binding.appointmentRecycler.setLayoutManager(new LinearLayoutManager(activity));
        binding.appointmentRecycler.setAdapter(adapter);
        adapter.setOnAppointmentClickListener(this::showStatusActions);
        adapter.setOnDayClickListener(dayKey -> {
            try {
                SimpleDateFormat dayFmt = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                selectedDay.setTime(dayFmt.parse(dayKey));
                weekMode = false;
                updateModeButtons();
                refreshVisibleList();
            } catch (Exception ignored) {
            }
        });

        binding.prevDayBtn.setOnClickListener(v -> {
            selectedDay.add(Calendar.DAY_OF_MONTH, weekMode ? -7 : -1);
            refreshVisibleList();
        });
        binding.nextDayBtn.setOnClickListener(v -> {
            selectedDay.add(Calendar.DAY_OF_MONTH, weekMode ? 7 : 1);
            refreshVisibleList();
        });
        binding.dayModeBtn.setOnClickListener(v -> {
            weekMode = false;
            updateModeButtons();
            refreshVisibleList();
        });
        binding.weekModeBtn.setOnClickListener(v -> {
            weekMode = true;
            updateModeButtons();
            refreshVisibleList();
        });
        binding.refreshBtn.setOnClickListener(v -> loadAppointments(true));

        binding.outletSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                if (position >= 0 && position < outlets.size()) {
                    selectedOutlet = outlets.get(position);
                    if (!suppressOutletLoad) {
                        loadAppointments(true);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        updateModeButtons();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DetectConnection.checkInternetConnection(activity)) {
            loadOutlets();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void goBack() {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        ((MainActivity) activity).loadFragment(new OutletOpsHub(), true);
    }

    private void updateModeButtons() {
        int primary = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        int secondary = ContextCompat.getColor(requireContext(), R.color.colorTextSecondary);
        binding.dayModeBtn.setTextColor(weekMode ? secondary : primary);
        binding.weekModeBtn.setTextColor(weekMode ? primary : secondary);
        adapter.setWeekMode(weekMode);
    }

    private void loadOutlets() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.outlet_appt_loading_outlets));
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().getStoreWise(MainActivity.userId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call,
                                   @NonNull Response<AllApiResponse> response) {
                pDialog.dismiss();
                outlets.clear();
                if (response.isSuccessful() && response.body() != null
                        && response.body().getLicenseResponseList() != null) {
                    outlets.addAll(response.body().getLicenseResponseList());
                }
                List<String> labels = new ArrayList<>();
                for (LicenseResponse lic : outlets) {
                    String name = lic.getShopName1() != null && !lic.getShopName1().isEmpty()
                            ? lic.getShopName1()
                            : (lic.getUserName() != null ? lic.getUserName() : "Outlet");
                    String branch = lic.getBranchLabel() != null && !lic.getBranchLabel().isEmpty()
                            ? " · " + lic.getBranchLabel() : "";
                    labels.add(name + branch + " (#" + lic.getLicensesId() + ")");
                }
                suppressOutletLoad = true;
                binding.outletSpinner.setAdapter(new ArrayAdapter<>(activity,
                        android.R.layout.simple_spinner_dropdown_item, labels));
                suppressOutletLoad = false;
                if (!outlets.isEmpty()) {
                    selectedOutlet = outlets.get(0);
                    loadAppointments(true);
                } else {
                    allAppointments.clear();
                    refreshVisibleList();
                    binding.emptyLabel.setText(getString(R.string.outlet_appt_no_outlets));
                    binding.emptyLabel.setVisibility(View.VISIBLE);
                    binding.appointmentRecycler.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                binding.emptyLabel.setText(getString(R.string.outlet_appt_load_failed));
                binding.emptyLabel.setVisibility(View.VISIBLE);
                binding.appointmentRecycler.setVisibility(View.GONE);
            }
        });
    }

    private String licenceId() {
        return selectedOutlet != null ? selectedOutlet.getLicensesId() : null;
    }

    private void loadAppointments(boolean showProgress) {
        String id = licenceId();
        if (id == null || id.isEmpty()) {
            return;
        }
        SweetAlertDialog pDialog = null;
        if (showProgress) {
            pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
            pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
            pDialog.setTitleText(getString(R.string.outlet_appt_loading));
            pDialog.setCancelable(false);
            pDialog.show();
        }
        final SweetAlertDialog dialog = pDialog;
        Api.getClient().getServiceAppointmentList(MainActivity.userId, id)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call,
                                           @NonNull Response<AllApiResponse> response) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        allAppointments.clear();
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getServiceAppointmentResponseList() != null) {
                            allAppointments.addAll(response.body().getServiceAppointmentResponseList());
                        }
                        refreshVisibleList();
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        allAppointments.clear();
                        refreshVisibleList();
                        binding.emptyLabel.setText(getString(R.string.outlet_appt_load_failed));
                        binding.emptyLabel.setVisibility(View.VISIBLE);
                        binding.appointmentRecycler.setVisibility(View.GONE);
                    }
                });
    }

    private void refreshVisibleList() {
        SimpleDateFormat dayFmt = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        SimpleDateFormat titleFmt = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat rangeFmt = new SimpleDateFormat("dd MMM", Locale.getDefault());

        List<ServiceAppointmentResponse> visible = new ArrayList<>();
        if (weekMode) {
            Calendar weekStart = (Calendar) selectedDay.clone();
            int dow = weekStart.get(Calendar.DAY_OF_WEEK);
            int shift = (dow == Calendar.SUNDAY) ? -6 : (Calendar.MONDAY - dow);
            weekStart.add(Calendar.DAY_OF_MONTH, shift);
            Calendar weekEnd = (Calendar) weekStart.clone();
            weekEnd.add(Calendar.DAY_OF_MONTH, 6);
            binding.dayTitle.setText(getString(R.string.outlet_appt_week_title,
                    rangeFmt.format(weekStart.getTime()), rangeFmt.format(weekEnd.getTime())));

            for (int i = 0; i < 7; i++) {
                Calendar d = (Calendar) weekStart.clone();
                d.add(Calendar.DAY_OF_MONTH, i);
                String key = dayFmt.format(d.getTime());
                for (ServiceAppointmentResponse a : allAppointments) {
                    if (a.getAppointmentAt().startsWith(key)) {
                        visible.add(a);
                    }
                }
            }
        } else {
            String dayKey = dayFmt.format(selectedDay.getTime());
            binding.dayTitle.setText(titleFmt.format(selectedDay.getTime()));
            for (ServiceAppointmentResponse a : allAppointments) {
                if (a.getAppointmentAt().startsWith(dayKey)) {
                    visible.add(a);
                }
            }
        }

        adapter.setWeekMode(weekMode);
        adapter.setItems(visible);
        boolean empty = visible.isEmpty();
        binding.emptyLabel.setText(weekMode
                ? getString(R.string.outlet_appt_none_week)
                : getString(R.string.outlet_appt_none));
        binding.emptyLabel.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.appointmentRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showStatusActions(ServiceAppointmentResponse appt) {
        if (appt == null || activity == null) {
            return;
        }
        String status = appt.getAppointmentStatus();
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        if (!"done".equalsIgnoreCase(status)) {
            labels.add(getString(R.string.outlet_appt_mark_done));
            ids.add("done");
        }
        if (!"cancelled".equalsIgnoreCase(status)) {
            labels.add(getString(R.string.outlet_appt_mark_cancelled));
            ids.add("cancelled");
        }
        if (!"booked".equalsIgnoreCase(status)) {
            labels.add(getString(R.string.outlet_appt_mark_booked));
            ids.add("booked");
        }
        if (labels.isEmpty()) {
            return;
        }
        String title = appt.getCustomerName().isEmpty()
                ? getString(R.string.outlet_appt_manage)
                : appt.getCustomerName() + " · " + status;
        new android.app.AlertDialog.Builder(activity)
                .setTitle(title)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    if (which >= 0 && which < ids.size()) {
                        runStatusUpdate(appt, ids.get(which));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void runStatusUpdate(ServiceAppointmentResponse appt, String newStatus) {
        String id = licenceId();
        if (id == null || id.isEmpty() || appt == null) {
            return;
        }
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.outlet_appt_updating));
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().updateServiceAppointmentStatus(
                        MainActivity.userId,
                        id,
                        appt.getAppointmentId(),
                        appt.getLocalAppointmentId(),
                        newStatus)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call,
                                           @NonNull Response<AllApiResponse> response) {
                        pDialog.dismiss();
                        AllApiResponse body = response.body();
                        boolean ok = body != null && "1".equals(body.status);
                        String message = body != null && body.message != null
                                ? body.message
                                : getString(R.string.outlet_appt_update_failed);
                        new SweetAlertDialog(activity,
                                ok ? SweetAlertDialog.SUCCESS_TYPE : SweetAlertDialog.ERROR_TYPE)
                                .setTitleText(getString(R.string.outlet_appt_title))
                                .setContentText(ok
                                        ? getString(R.string.outlet_appt_updated)
                                        : message)
                                .setConfirmText(getString(android.R.string.ok))
                                .show();
                        if (ok) {
                            appt.appointmentStatus = newStatus;
                            refreshVisibleList();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        pDialog.dismiss();
                        new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText(getString(R.string.outlet_appt_title))
                                .setContentText(getString(R.string.outlet_appt_update_failed))
                                .setConfirmText(getString(android.R.string.ok))
                                .show();
                    }
                });
    }
}
