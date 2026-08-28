package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Extra.SupportUiHelper;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateSupportTicket extends Fragment {
    Activity activity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = SupportUiHelper.form(activity);
        SupportUiHelper.addScreenHeader(activity, root, getString(R.string.support_create_ticket),
                () -> ((MainActivity) activity).navigateBack());
        SupportUiHelper.notice(activity, root, getString(R.string.support_online_only_notice));
        android.widget.EditText category = SupportUiHelper.field(activity, root, getString(R.string.support_category), "Billing");
        android.widget.EditText subject = SupportUiHelper.field(activity, root, getString(R.string.support_subject), "");
        android.widget.EditText desc = SupportUiHelper.field(activity, root, getString(R.string.support_description), "");
        desc.setMinLines(4);
        SupportUiHelper.primary(activity, root, getString(R.string.support_submit)).setOnClickListener(v -> submit(category, subject, desc));
        scroll.addView(root);
        return scroll;
    }

    private void submit(android.widget.EditText category, android.widget.EditText subject, android.widget.EditText desc) {
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
            return;
        }
        if (subject.getText().toString().trim().isEmpty()) {
            Toast.makeText(activity, R.string.support_subject_required, Toast.LENGTH_SHORT).show();
            return;
        }
        SweetAlertDialog p = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        p.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
        p.setTitleText(getString(R.string.support_submitting));
        p.setCancelable(false);
        p.show();
        Api.getClient(activity).createSupportTicket(
                MainActivity.userId,
                category.getText().toString().trim(),
                subject.getText().toString().trim(),
                desc.getText().toString().trim()
        ).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                p.dismiss();
                AllApiResponse body = response.body();
                if (body != null && ("1".equals(body.getStatus()) || "true".equalsIgnoreCase(body.getStatus()))) {
                    Toast.makeText(activity, body.getMessage() != null ? body.getMessage() : getString(R.string.support_submitted), Toast.LENGTH_SHORT).show();
                    ((MainActivity) activity).navigateBack();
                } else {
                    Toast.makeText(activity, body != null && body.getMessage() != null ? body.getMessage() : getString(R.string.support_load_failed), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                p.dismiss();
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
