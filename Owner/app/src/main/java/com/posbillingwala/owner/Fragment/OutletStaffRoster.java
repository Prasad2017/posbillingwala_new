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
import com.posbillingwala.owner.Adapter.OutletStaffAdapter;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.LicenseResponse;
import com.posbillingwala.owner.Model.StaffUserResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentOutletStaffRosterBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Owner outlet staff roster (no PINs shown).
 * Tap to activate/deactivate, soft-delete/restore, or change role.
 */
public class OutletStaffRoster extends Fragment {

    private Activity activity;
    private FragmentOutletStaffRosterBinding binding;
    private final List<LicenseResponse> outlets = new ArrayList<>();
    private final List<StaffUserResponse> allStaff = new ArrayList<>();
    private final OutletStaffAdapter adapter = new OutletStaffAdapter();
    private LicenseResponse selectedOutlet;
    private boolean activeOnly = true;
    private boolean suppressOutletLoad;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOutletStaffRosterBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        activity = getActivity();

        binding.toolbar.toolbarTitle.setText(getString(R.string.outlet_staff_title));
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

        binding.staffRecycler.setLayoutManager(new LinearLayoutManager(activity));
        binding.staffRecycler.setAdapter(adapter);
        adapter.setOnStaffClickListener(this::showStaffActions);

        binding.filterActiveBtn.setOnClickListener(v -> {
            activeOnly = true;
            updateFilterButtons();
            refreshVisibleList();
        });
        binding.filterAllBtn.setOnClickListener(v -> {
            activeOnly = false;
            updateFilterButtons();
            refreshVisibleList();
        });
        binding.refreshBtn.setOnClickListener(v -> loadStaff(true));

        binding.outletSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                if (position >= 0 && position < outlets.size()) {
                    selectedOutlet = outlets.get(position);
                    if (!suppressOutletLoad) {
                        loadStaff(true);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        updateFilterButtons();
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

    private void updateFilterButtons() {
        int primary = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        int secondary = ContextCompat.getColor(requireContext(), R.color.colorTextSecondary);
        binding.filterActiveBtn.setTextColor(activeOnly ? primary : secondary);
        binding.filterAllBtn.setTextColor(activeOnly ? secondary : primary);
    }

    private void loadOutlets() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.outlet_staff_loading_outlets));
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
                    loadStaff(true);
                } else {
                    allStaff.clear();
                    refreshVisibleList();
                    binding.emptyLabel.setText(getString(R.string.outlet_staff_no_outlets));
                    binding.emptyLabel.setVisibility(View.VISIBLE);
                    binding.staffRecycler.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                binding.emptyLabel.setText(getString(R.string.outlet_staff_load_failed));
                binding.emptyLabel.setVisibility(View.VISIBLE);
                binding.staffRecycler.setVisibility(View.GONE);
            }
        });
    }

    private String licenceId() {
        return selectedOutlet != null ? selectedOutlet.getLicensesId() : null;
    }

    private void loadStaff(boolean showProgress) {
        String id = licenceId();
        if (id == null || id.isEmpty()) {
            return;
        }
        SweetAlertDialog pDialog = null;
        if (showProgress) {
            pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
            pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
            pDialog.setTitleText(getString(R.string.outlet_staff_loading));
            pDialog.setCancelable(false);
            pDialog.show();
        }
        final SweetAlertDialog dialog = pDialog;
        Api.getClient().getStaffUserList(MainActivity.userId, id)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call,
                                           @NonNull Response<AllApiResponse> response) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        allStaff.clear();
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getStaffUserResponseList() != null) {
                            allStaff.addAll(response.body().getStaffUserResponseList());
                        }
                        refreshVisibleList();
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        allStaff.clear();
                        refreshVisibleList();
                        binding.emptyLabel.setText(getString(R.string.outlet_staff_load_failed));
                        binding.emptyLabel.setVisibility(View.VISIBLE);
                        binding.staffRecycler.setVisibility(View.GONE);
                    }
                });
    }

    private void refreshVisibleList() {
        List<StaffUserResponse> visible = new ArrayList<>();
        for (StaffUserResponse s : allStaff) {
            if (activeOnly && !s.isActive()) {
                continue;
            }
            visible.add(s);
        }
        adapter.setItems(visible);
        boolean empty = visible.isEmpty();
        binding.emptyLabel.setText(activeOnly
                ? getString(R.string.outlet_staff_none_active)
                : getString(R.string.outlet_staff_none));
        binding.emptyLabel.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.staffRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showStaffActions(StaffUserResponse staff) {
        if (staff == null || activity == null) {
            return;
        }
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Runnable> actions = new ArrayList<>();

        if (staff.isDeleted()) {
            labels.add(getString(R.string.outlet_staff_restore));
            actions.add(() -> runUpdate(staff, "1", "0", null));
        } else {
            if (staff.isActive()) {
                labels.add(getString(R.string.outlet_staff_deactivate));
                actions.add(() -> runUpdate(staff, "0", "0", null));
            } else {
                labels.add(getString(R.string.outlet_staff_activate));
                actions.add(() -> runUpdate(staff, "1", "0", null));
            }
            labels.add(getString(R.string.outlet_staff_soft_delete));
            actions.add(() -> runUpdate(staff, "0", "1", null));
            labels.add(getString(R.string.outlet_staff_set_role));
            actions.add(() -> showRolePicker(staff));
        }

        String title = staff.getStaffName().isEmpty()
                ? getString(R.string.outlet_staff_manage)
                : staff.getStaffName() + " · " + staff.getStaffRole();
        new android.app.AlertDialog.Builder(activity)
                .setTitle(title)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    if (which >= 0 && which < actions.size()) {
                        actions.get(which).run();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showRolePicker(StaffUserResponse staff) {
        String[] roles = new String[]{"owner", "manager", "cashier", "waiter"};
        new android.app.AlertDialog.Builder(activity)
                .setTitle(R.string.outlet_staff_set_role)
                .setItems(roles, (dialog, which) -> {
                    if (which >= 0 && which < roles.length) {
                        runUpdate(staff, null, null, roles[which]);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void runUpdate(StaffUserResponse staff, String active, String deleted, String role) {
        String id = licenceId();
        if (id == null || id.isEmpty() || staff == null) {
            return;
        }
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.outlet_staff_updating));
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().updateStaffUserStatus(
                        MainActivity.userId,
                        id,
                        staff.getStaffId(),
                        staff.getLocalStaffId(),
                        active != null ? active : "",
                        deleted != null ? deleted : "",
                        role != null ? role : "")
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call,
                                           @NonNull Response<AllApiResponse> response) {
                        pDialog.dismiss();
                        AllApiResponse body = response.body();
                        boolean ok = body != null && "1".equals(body.status);
                        String message = body != null && body.message != null
                                ? body.message
                                : getString(R.string.outlet_staff_update_failed);
                        new SweetAlertDialog(activity,
                                ok ? SweetAlertDialog.SUCCESS_TYPE : SweetAlertDialog.ERROR_TYPE)
                                .setTitleText(getString(R.string.outlet_staff_title))
                                .setContentText(ok
                                        ? getString(R.string.outlet_staff_updated)
                                        : message)
                                .setConfirmText(getString(android.R.string.ok))
                                .show();
                        if (ok) {
                            if (active != null) {
                                staff.staffActive = active;
                            }
                            if (deleted != null) {
                                staff.staffDeletedStatus = deleted;
                            }
                            if (role != null) {
                                staff.staffRole = role;
                            }
                            refreshVisibleList();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        pDialog.dismiss();
                        new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText(getString(R.string.outlet_staff_title))
                                .setContentText(getString(R.string.outlet_staff_update_failed))
                                .setConfirmText(getString(android.R.string.ok))
                                .show();
                    }
                });
    }
}
