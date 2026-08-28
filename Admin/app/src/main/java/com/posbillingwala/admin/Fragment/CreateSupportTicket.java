package com.posbillingwala.admin.Fragment;

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

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Retrofit.Api;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateSupportTicket extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Activity a = getActivity();
        ((MainActivity) a).setScreenTitle("Create Ticket");
        ScrollView scroll = new ScrollView(a);
        LinearLayout root = SettingsProfile.form(a);
        android.widget.EditText app = SettingsProfile.field(a, root, "Select App", "POS App");
        android.widget.EditText cat = SettingsProfile.field(a, root, "Category", "Billing");
        android.widget.EditText subject = SettingsProfile.field(a, root, "Subject", "");
        android.widget.EditText desc = SettingsProfile.field(a, root, "Description", "");
        desc.setMinLines(4);
        android.widget.Button submit = SettingsProfile.primary(a, root, "Submit Ticket");
        submit.setOnClickListener(v -> {
            if (subject.getText().toString().trim().isEmpty()) {
                Toast.makeText(a, "Subject required", Toast.LENGTH_SHORT).show();
                return;
            }
            SweetAlertDialog p = new SweetAlertDialog(a, SweetAlertDialog.PROGRESS_TYPE);
            p.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
            p.setTitleText("Submitting");
            p.setCancelable(false);
            p.show();
            Api.getClient().createSupportTicket(app.getText().toString(), cat.getText().toString(),
                    subject.getText().toString(), desc.getText().toString()).enqueue(new Callback<AllApiResponse>() {
                @Override
                public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                    p.dismiss();
                    Toast.makeText(a, response.body() != null ? response.body().getMessage() : "Done", Toast.LENGTH_SHORT).show();
                    ((MainActivity) a).removeCurrentFragmentAndMoveBack();
                }

                @Override
                public void onFailure(Call<AllApiResponse> call, Throwable t) {
                    p.dismiss();
                }
            });
        });
        scroll.addView(root);
        return scroll;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).lockUnlockDrawer(1);
    }
}
