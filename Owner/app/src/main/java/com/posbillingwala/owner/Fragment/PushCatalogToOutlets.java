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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Adapter.PushOutletAdapter;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.CatalogPushBranchResult;
import com.posbillingwala.owner.Model.CatalogPushResponse;
import com.posbillingwala.owner.Model.LicenseResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentPushCatalogToOutletsBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PushCatalogToOutlets extends Fragment {

    private Activity activity;
    private FragmentPushCatalogToOutletsBinding binding;
    private final List<LicenseResponse> outlets = new ArrayList<>();
    private final List<String> sourceModes = new ArrayList<>();
    private final List<String> sourceBranchIds = new ArrayList<>();
    private final PushOutletAdapter outletAdapter = new PushOutletAdapter();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPushCatalogToOutletsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        activity = getActivity();

        binding.toolbar.toolbarTitle.setText(getString(R.string.push_catalog_title));
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

        binding.outletRecycler.setLayoutManager(new LinearLayoutManager(activity));
        binding.outletRecycler.setAdapter(outletAdapter);
        binding.selectAllBtn.setOnClickListener(v -> outletAdapter.setAllSelected(true));
        binding.pushBtn.setOnClickListener(v -> confirmPush());
        binding.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                refreshTargetEnabled();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
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
        ((MainActivity) activity).loadFragment(new UserSetting(), true);
    }

    private void loadOutlets() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.push_catalog_loading));
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().getStoreWise(MainActivity.userId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                outlets.clear();
                if (response.isSuccessful() && response.body() != null
                        && response.body().getLicenseResponseList() != null) {
                    outlets.addAll(response.body().getLicenseResponseList());
                }
                bindSourceSpinner();
                boolean empty = outlets.isEmpty();
                binding.noOutlets.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.outletRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
                refreshTargetEnabled();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                binding.noOutlets.setVisibility(View.VISIBLE);
            }
        });
    }

    private void bindSourceSpinner() {
        sourceModes.clear();
        sourceBranchIds.clear();
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.push_catalog_source_hq));
        sourceModes.add("hq");
        sourceBranchIds.add("");
        for (LicenseResponse license : outlets) {
            String label = license.getBranchLabel();
            if (label == null || label.trim().isEmpty()) {
                label = license.getShopName1();
            }
            if (label == null || label.trim().isEmpty()) {
                label = "Outlet " + license.getLicensesId();
            }
            labels.add(label);
            sourceModes.add("branch");
            sourceBranchIds.add(license.getLicensesId());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, labels);
        binding.sourceSpinner.setAdapter(adapter);
    }

    private void refreshTargetEnabled() {
        int pos = binding.sourceSpinner.getSelectedItemPosition();
        String disableId = "";
        if (pos >= 0 && pos < sourceModes.size() && "branch".equals(sourceModes.get(pos))) {
            disableId = sourceBranchIds.get(pos);
        }
        outletAdapter.setOutlets(outlets, disableId);
    }

    private void confirmPush() {
        List<String> ids = outletAdapter.selectedIds();
        if (ids.isEmpty()) {
            new SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText(getString(R.string.push_catalog_title))
                    .setContentText(getString(R.string.push_catalog_pick_outlet))
                    .setConfirmText(getString(android.R.string.ok))
                    .show();
            return;
        }
        new SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(getString(R.string.push_catalog_confirm_title))
                .setContentText(getString(R.string.push_catalog_confirm_body, ids.size()))
                .setConfirmText(getString(R.string.push_catalog_action))
                .setCancelText(getString(android.R.string.cancel))
                .showCancelButton(true)
                .setConfirmClickListener(dialog -> {
                    dialog.dismissWithAnimation();
                    runPush(ids);
                })
                .show();
    }

    private void runPush(List<String> ids) {
        int pos = binding.sourceSpinner.getSelectedItemPosition();
        String mode = pos >= 0 && pos < sourceModes.size() ? sourceModes.get(pos) : "hq";
        String sourceBranchId = pos >= 0 && pos < sourceBranchIds.size() ? sourceBranchIds.get(pos) : "";

        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.push_catalog_pushing));
        pDialog.setCancelable(false);
        pDialog.show();

        String csv = android.text.TextUtils.join(",", ids);
        Api.getClient().pushCatalogToBranches(MainActivity.userId, mode, sourceBranchId, csv)
                .enqueue(new Callback<CatalogPushResponse>() {
                    @Override
                    public void onResponse(Call<CatalogPushResponse> call, Response<CatalogPushResponse> response) {
                        pDialog.dismiss();
                        CatalogPushResponse body = response.body();
                        String message = body != null && body.message != null
                                ? body.message
                                : getString(R.string.push_catalog_failed);
                        if (body != null && body.branchResults != null && !body.branchResults.isEmpty()) {
                            StringBuilder extra = new StringBuilder(message);
                            for (CatalogPushBranchResult row : body.branchResults) {
                                extra.append("\n").append(row.branchLabel).append(": ").append(row.message);
                                extra.append(" (+").append(row.productsCopied).append(" new, ")
                                        .append(row.productsUpdated).append(" updated)");
                            }
                            message = extra.toString();
                        }
                        int type = body != null && body.isSuccess()
                                ? SweetAlertDialog.SUCCESS_TYPE
                                : SweetAlertDialog.ERROR_TYPE;
                        new SweetAlertDialog(activity, type)
                                .setTitleText(getString(R.string.push_catalog_title))
                                .setContentText(message)
                                .setConfirmText(getString(android.R.string.ok))
                                .show();
                    }

                    @Override
                    public void onFailure(Call<CatalogPushResponse> call, Throwable t) {
                        pDialog.dismiss();
                        new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText(getString(R.string.push_catalog_title))
                                .setContentText(getString(R.string.push_catalog_failed))
                                .setConfirmText(getString(android.R.string.ok))
                                .show();
                    }
                });
    }
}
