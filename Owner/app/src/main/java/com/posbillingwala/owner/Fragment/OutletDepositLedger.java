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
import com.posbillingwala.owner.Adapter.OutletDepositAdapter;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.CustomOrderDepositResponse;
import com.posbillingwala.owner.Model.LicenseResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentOutletDepositLedgerBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Owner custom-order deposit ledger for a franchise outlet.
 * Tap a row to mark applied / refunded / open again; POS picks up on Fetch Data.
 */
public class OutletDepositLedger extends Fragment {

    private Activity activity;
    private FragmentOutletDepositLedgerBinding binding;
    private final List<LicenseResponse> outlets = new ArrayList<>();
    private final List<CustomOrderDepositResponse> allDeposits = new ArrayList<>();
    private final OutletDepositAdapter adapter = new OutletDepositAdapter();
    private LicenseResponse selectedOutlet;
    private boolean openOnly;
    private boolean suppressOutletLoad;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOutletDepositLedgerBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        activity = getActivity();

        binding.toolbar.toolbarTitle.setText(getString(R.string.outlet_deposit_title));
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

        binding.depositRecycler.setLayoutManager(new LinearLayoutManager(activity));
        binding.depositRecycler.setAdapter(adapter);
        adapter.setOnDepositClickListener(this::showStatusActions);

        binding.filterAllBtn.setOnClickListener(v -> {
            openOnly = false;
            updateFilterButtons();
            refreshVisibleList();
        });
        binding.filterOpenBtn.setOnClickListener(v -> {
            openOnly = true;
            updateFilterButtons();
            refreshVisibleList();
        });
        binding.refreshBtn.setOnClickListener(v -> loadDeposits(true));

        binding.outletSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                if (position >= 0 && position < outlets.size()) {
                    selectedOutlet = outlets.get(position);
                    if (!suppressOutletLoad) {
                        loadDeposits(true);
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
        binding.filterAllBtn.setTextColor(openOnly ? secondary : primary);
        binding.filterOpenBtn.setTextColor(openOnly ? primary : secondary);
    }

    private void loadOutlets() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.outlet_deposit_loading_outlets));
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
                    loadDeposits(true);
                } else {
                    allDeposits.clear();
                    refreshVisibleList();
                    binding.emptyLabel.setText(getString(R.string.outlet_deposit_no_outlets));
                    binding.emptyLabel.setVisibility(View.VISIBLE);
                    binding.depositRecycler.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                binding.emptyLabel.setText(getString(R.string.outlet_deposit_load_failed));
                binding.emptyLabel.setVisibility(View.VISIBLE);
                binding.depositRecycler.setVisibility(View.GONE);
            }
        });
    }

    private String licenceId() {
        return selectedOutlet != null ? selectedOutlet.getLicensesId() : null;
    }

    private void loadDeposits(boolean showProgress) {
        String id = licenceId();
        if (id == null || id.isEmpty()) {
            return;
        }
        SweetAlertDialog pDialog = null;
        if (showProgress) {
            pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
            pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
            pDialog.setTitleText(getString(R.string.outlet_deposit_loading));
            pDialog.setCancelable(false);
            pDialog.show();
        }
        final SweetAlertDialog dialog = pDialog;
        Api.getClient().getCustomOrderDepositList(MainActivity.userId, id)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call,
                                           @NonNull Response<AllApiResponse> response) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        allDeposits.clear();
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getCustomOrderDepositResponseList() != null) {
                            allDeposits.addAll(response.body().getCustomOrderDepositResponseList());
                        }
                        refreshVisibleList();
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        allDeposits.clear();
                        refreshVisibleList();
                        binding.emptyLabel.setText(getString(R.string.outlet_deposit_load_failed));
                        binding.emptyLabel.setVisibility(View.VISIBLE);
                        binding.depositRecycler.setVisibility(View.GONE);
                    }
                });
    }

    private void refreshVisibleList() {
        List<CustomOrderDepositResponse> visible = new ArrayList<>();
        for (CustomOrderDepositResponse d : allDeposits) {
            if (openOnly && !"open".equalsIgnoreCase(d.getDepositStatus())) {
                continue;
            }
            visible.add(d);
        }
        adapter.setItems(visible);
        boolean empty = visible.isEmpty();
        binding.emptyLabel.setText(openOnly
                ? getString(R.string.outlet_deposit_none_open)
                : getString(R.string.outlet_deposit_none));
        binding.emptyLabel.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.depositRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showStatusActions(CustomOrderDepositResponse deposit) {
        if (deposit == null || activity == null) {
            return;
        }
        String status = deposit.getDepositStatus();
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        if (!"applied".equalsIgnoreCase(status)) {
            labels.add(getString(R.string.outlet_deposit_mark_applied));
            ids.add("applied");
        }
        if (!"refunded".equalsIgnoreCase(status)) {
            labels.add(getString(R.string.outlet_deposit_mark_refunded));
            ids.add("refunded");
        }
        if (!"open".equalsIgnoreCase(status)) {
            labels.add(getString(R.string.outlet_deposit_mark_open));
            ids.add("open");
        }
        if (labels.isEmpty()) {
            return;
        }
        String title = deposit.getProductName().isEmpty()
                ? getString(R.string.outlet_deposit_manage)
                : deposit.getProductName() + " · " + status;
        new android.app.AlertDialog.Builder(activity)
                .setTitle(title)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    if (which >= 0 && which < ids.size()) {
                        runStatusUpdate(deposit, ids.get(which));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void runStatusUpdate(CustomOrderDepositResponse deposit, String newStatus) {
        String id = licenceId();
        if (id == null || id.isEmpty() || deposit == null) {
            return;
        }
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.outlet_deposit_updating));
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().updateCustomOrderDepositStatus(
                        MainActivity.userId,
                        id,
                        deposit.getDepositId(),
                        deposit.getLocalDepositId(),
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
                                : getString(R.string.outlet_deposit_update_failed);
                        new SweetAlertDialog(activity,
                                ok ? SweetAlertDialog.SUCCESS_TYPE : SweetAlertDialog.ERROR_TYPE)
                                .setTitleText(getString(R.string.outlet_deposit_title))
                                .setContentText(ok
                                        ? getString(R.string.outlet_deposit_updated)
                                        : message)
                                .setConfirmText(getString(android.R.string.ok))
                                .show();
                        if (ok) {
                            deposit.depositStatus = newStatus;
                            refreshVisibleList();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        pDialog.dismiss();
                        new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText(getString(R.string.outlet_deposit_title))
                                .setContentText(getString(R.string.outlet_deposit_update_failed))
                                .setConfirmText(getString(android.R.string.ok))
                                .show();
                    }
                });
    }
}
