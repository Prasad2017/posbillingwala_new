package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.ActionButtonUi;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Extra.TabletFormUi;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;
import com.pos_billingwala.databinding.FragmentCreateSupportTicketBinding;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateSupportTicket extends Fragment {
    Activity activity;
    FragmentCreateSupportTicketBinding binding;
    String selectedCategory = "Billing";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        binding = FragmentCreateSupportTicketBinding.inflate(inflater, container, false);

        binding.backButton.setOnClickListener(v -> ((MainActivity) activity).navigateBack());
        binding.submitTicketButton.getRoot().setOnClickListener(v -> submit());
        ActionButtonUi.bind(binding.submitTicketButton.getRoot(), R.drawable.ic_send, R.string.support_submit);
        binding.attachmentRow.setOnClickListener(v ->
                Toast.makeText(activity, R.string.support_attachment_sub, Toast.LENGTH_SHORT).show());

        setupCategorySpinner();
        setupCharCounter();

        TabletFormUi.applyCenteredPanel(binding.supportContent);
        return binding.getRoot();
    }

    private void setupCategorySpinner() {
        String[] categories = getResources().getStringArray(R.array.support_categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(adapter);
        binding.categorySpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                selectedCategory = item;
            }
        });
    }

    private void setupCharCounter() {
        updateCharCount(0);
        binding.descriptionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCharCount(s != null ? s.length() : 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updateCharCount(int count) {
        binding.charCount.setText(getString(R.string.support_char_count, count));
    }

    private void submit() {
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
            return;
        }
        String subject = binding.subjectInput.getText() != null
                ? binding.subjectInput.getText().toString().trim() : "";
        String description = binding.descriptionInput.getText() != null
                ? binding.descriptionInput.getText().toString().trim() : "";
        if (subject.isEmpty()) {
            Toast.makeText(activity, R.string.support_subject_required, Toast.LENGTH_SHORT).show();
            return;
        }

        SweetAlertDialog progress = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        progress.getProgressHelper().setBarColor(Color.parseColor("#4862b7"));
        progress.setTitleText(getString(R.string.support_submitting));
        progress.setCancelable(false);
        progress.show();

        Api.getClient(activity).createSupportTicket(
                MainActivity.userId,
                selectedCategory,
                subject,
                description
        ).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                progress.dismiss();
                AllApiResponse body = response.body();
                if (body != null && ("1".equals(body.getStatus()) || "true".equalsIgnoreCase(body.getStatus()))) {
                    Toast.makeText(activity,
                            body.getMessage() != null ? body.getMessage() : getString(R.string.support_submitted),
                            Toast.LENGTH_SHORT).show();
                    ((MainActivity) activity).navigateBack();
                } else {
                    Toast.makeText(activity,
                            body != null && body.getMessage() != null
                                    ? body.getMessage() : getString(R.string.support_load_failed),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                progress.dismiss();
                Toast.makeText(activity, R.string.support_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
    }
}
