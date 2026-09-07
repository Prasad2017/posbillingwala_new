package com.posbillingwala.dealer.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Extra.DetectConnection;
import com.posbillingwala.dealer.Extra.BusinessTemplateChoices;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.Model.BusinessTemplateResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.databinding.FragmentLicenceBusinessTemplateBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dealer: set business template for a licence owned by this dealer's customer.
 */
public class LicenceBusinessTemplate extends Fragment {

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
    private FragmentLicenceBusinessTemplateBinding binding;
    private final List<TemplateChoice> templates = new ArrayList<>();
    private String licenceId;
    private String licenceDisplay;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLicenceBusinessTemplateBinding.inflate(inflater, container, false);
        activity = getActivity();
        MainActivity.title.setText(getString(R.string.licence_template_title));
        MainActivity.title.setVisibility(View.VISIBLE);

        Bundle args = getArguments();
        if (args != null) {
            licenceId = args.getString("licenceId", "");
            licenceDisplay = args.getString("licenceDisplay", "Licence #" + licenceId);
        }

        MainActivity.back.setOnClickListener(v ->
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack());

        binding.licenceLabel.setText(licenceDisplay != null ? licenceDisplay : "");
        buildTemplateChoices();
        List<String> labels = new ArrayList<>();
        for (TemplateChoice t : templates) {
            labels.add(t.label);
        }
        binding.templateSpinner.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, labels));
        binding.saveBtn.setOnClickListener(v -> confirmSave());
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DetectConnection.checkInternetConnection(activity)) {
            loadCurrentTemplate();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void buildTemplateChoices() {
        templates.clear();
        for (BusinessTemplateChoices.Item item : BusinessTemplateChoices.all()) {
            templates.add(new TemplateChoice(item.businessType, item.templateId, item.label));
        }
    }

    private void loadCurrentTemplate() {
        if (licenceId == null || licenceId.isEmpty()) {
            return;
        }
        Api.getClient().getBusinessTemplate(MainActivity.userId, licenceId)
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
                                getString(R.string.licence_template_current, type, templateId));
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        binding.currentTemplateLabel.setText(
                                getString(R.string.licence_template_load_failed));
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
        if (licenceId == null || licenceId.isEmpty()) {
            return;
        }
        int pos = binding.templateSpinner.getSelectedItemPosition();
        if (pos < 0 || pos >= templates.size()) {
            return;
        }
        TemplateChoice choice = templates.get(pos);
        new SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(getString(R.string.licence_template_confirm_title))
                .setContentText(getString(R.string.licence_template_confirm_body, choice.label))
                .setConfirmText(getString(R.string.licence_template_save))
                .setCancelText(getString(android.R.string.cancel))
                .showCancelButton(true)
                .setConfirmClickListener(dialog -> {
                    dialog.dismissWithAnimation();
                    runSave(choice);
                })
                .show();
    }

    private void runSave(TemplateChoice choice) {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText(getString(R.string.licence_template_saving));
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
                        boolean ok = body != null && "1".equals(body.getStatus());
                        String message = body != null && body.getMessage() != null
                                ? body.getMessage()
                                : getString(R.string.licence_template_failed);
                        new SweetAlertDialog(activity,
                                ok ? SweetAlertDialog.SUCCESS_TYPE : SweetAlertDialog.ERROR_TYPE)
                                .setTitleText(getString(R.string.licence_template_title))
                                .setContentText(message)
                                .setConfirmText(getString(android.R.string.ok))
                                .show();
                        if (ok) {
                            binding.currentTemplateLabel.setText(getString(
                                    R.string.licence_template_current,
                                    choice.businessType, choice.templateId));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        pDialog.dismiss();
                        new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText(getString(R.string.licence_template_title))
                                .setContentText(getString(R.string.licence_template_failed))
                                .setConfirmText(getString(android.R.string.ok))
                                .show();
                    }
                });
    }
}
