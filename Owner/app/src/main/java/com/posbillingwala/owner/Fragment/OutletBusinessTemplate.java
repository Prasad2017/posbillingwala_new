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

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Extra.BusinessTemplateChoices;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.BusinessTemplateResponse;
import com.posbillingwala.owner.Model.LicenseResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentOutletBusinessTemplateBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Owner: view / set business template for a franchise outlet (licence).
 * POS devices pick up the change on Fetch Data.
 */
public class OutletBusinessTemplate extends Fragment {

    private static final class TemplateChoice {
        final String businessType;
        final String templateId;
        final String label;

        TemplateChoice(String businessType, String templateId, String label) {
            this.businessType = businessType;
            this.templateId = templateId;
            this.label = label;
        }
    }

    private Activity activity;
    private FragmentOutletBusinessTemplateBinding binding;
    private final List<LicenseResponse> outlets = new ArrayList<>();
    private final List<TemplateChoice> templates = new ArrayList<>();
    private LicenseResponse selectedOutlet;
    private boolean suppressTemplateLoad;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOutletBusinessTemplateBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        activity = getActivity();

        binding.toolbar.toolbarTitle.setText(getString(R.string.outlet_template_title));
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

        buildTemplateChoices();
        List<String> labels = new ArrayList<>();
        for (TemplateChoice t : templates) {
            labels.add(t.label);
        }
        binding.templateSpinner.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, labels));

        binding.outletSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                if (position >= 0 && position < outlets.size()) {
                    selectedOutlet = outlets.get(position);
                    if (!suppressTemplateLoad) {
                        loadCurrentTemplate();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        binding.saveBtn.setOnClickListener(v -> confirmSave());
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

    private void buildTemplateChoices() {
        templates.clear();
        for (BusinessTemplateChoices.Item item : BusinessTemplateChoices.all()) {
            templates.add(new TemplateChoice(item.businessType, item.templateId, item.label));
        }
    }

    private void loadOutlets() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.outlet_template_loading));
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
                boolean empty = outlets.isEmpty();
                binding.noOutlets.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.outletSpinner.setVisibility(empty ? View.GONE : View.VISIBLE);
                binding.saveBtn.setEnabled(!empty);
                suppressTemplateLoad = true;
                binding.outletSpinner.setAdapter(new ArrayAdapter<>(activity,
                        android.R.layout.simple_spinner_dropdown_item, labels));
                suppressTemplateLoad = false;
                if (!empty) {
                    selectedOutlet = outlets.get(0);
                    loadCurrentTemplate();
                } else {
                    binding.currentTemplateLabel.setText(getString(R.string.outlet_template_current_placeholder));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                binding.noOutlets.setVisibility(View.VISIBLE);
            }
        });
    }

    private String licenceId() {
        return selectedOutlet != null ? selectedOutlet.getLicensesId() : null;
    }

    private void loadCurrentTemplate() {
        String id = licenceId();
        if (id == null || id.isEmpty()) {
            return;
        }
        Api.getClient().getBusinessTemplate(MainActivity.userId, id)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call,
                                           @NonNull Response<AllApiResponse> response) {
                        String type = "restaurant";
                        String templateId = "restaurant_default";
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getBusinessTemplateResponseList() != null
                                && !response.body().getBusinessTemplateResponseList().isEmpty()) {
                            BusinessTemplateResponse row =
                                    response.body().getBusinessTemplateResponseList().get(0);
                            type = row.getBusinessType();
                            templateId = row.getBusinessTemplateId();
                        }
                        selectTemplate(type, templateId);
                        binding.currentTemplateLabel.setText(
                                getString(R.string.outlet_template_current, type, templateId));
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        binding.currentTemplateLabel.setText(
                                getString(R.string.outlet_template_load_failed));
                    }
                });
    }

    private void selectTemplate(String type, String templateId) {
        for (int i = 0; i < templates.size(); i++) {
            TemplateChoice t = templates.get(i);
            if (t.templateId.equals(templateId) || t.businessType.equals(type)) {
                binding.templateSpinner.setSelection(i);
                return;
            }
        }
        binding.templateSpinner.setSelection(0);
    }

    private void confirmSave() {
        String id = licenceId();
        if (id == null || id.isEmpty()) {
            new SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText(getString(R.string.outlet_template_title))
                    .setContentText(getString(R.string.outlet_template_pick_outlet))
                    .setConfirmText(getString(android.R.string.ok))
                    .show();
            return;
        }
        int pos = binding.templateSpinner.getSelectedItemPosition();
        if (pos < 0 || pos >= templates.size()) {
            return;
        }
        TemplateChoice choice = templates.get(pos);
        new SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(getString(R.string.outlet_template_confirm_title))
                .setContentText(getString(R.string.outlet_template_confirm_body, choice.label))
                .setConfirmText(getString(R.string.outlet_template_save))
                .setCancelText(getString(android.R.string.cancel))
                .showCancelButton(true)
                .setConfirmClickListener(dialog -> {
                    dialog.dismissWithAnimation();
                    runSave(id, choice);
                })
                .show();
    }

    private void runSave(String licenceId, TemplateChoice choice) {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.outlet_template_saving));
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().setBusinessTemplate(MainActivity.userId, licenceId,
                        choice.businessType, choice.templateId, "",
                        binding.syncModules != null && binding.syncModules.isChecked() ? "1" : "0")
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call,
                                           @NonNull Response<AllApiResponse> response) {
                        pDialog.dismiss();
                        AllApiResponse body = response.body();
                        boolean ok = body != null && "1".equals(body.status);
                        String message = body != null && body.message != null
                                ? body.message
                                : getString(R.string.outlet_template_failed);
                        new SweetAlertDialog(activity,
                                ok ? SweetAlertDialog.SUCCESS_TYPE : SweetAlertDialog.ERROR_TYPE)
                                .setTitleText(getString(R.string.outlet_template_title))
                                .setContentText(message)
                                .setConfirmText(getString(android.R.string.ok))
                                .show();
                        if (ok) {
                            binding.currentTemplateLabel.setText(getString(
                                    R.string.outlet_template_current,
                                    choice.businessType, choice.templateId));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        pDialog.dismiss();
                        new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText(getString(R.string.outlet_template_title))
                                .setContentText(getString(R.string.outlet_template_failed))
                                .setConfirmText(getString(android.R.string.ok))
                                .show();
                    }
                });
    }
}
