package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.ReportUiHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Retrofit.Api;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WebsiteContactDetails extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Activity a = getActivity();
        ((MainActivity) a).setScreenTitle("Enquiry Details");
        String contactId = getArguments() != null ? getArguments().getString("contactId") : "";
        ScrollView scroll = new ScrollView(a);
        LinearLayout root = SettingsProfile.form(a);
        TextView body = new TextView(a);
        body.setText("Loading...");
        root.addView(body);

        android.widget.Button reply = SettingsProfile.primary(a, root, "Reply by Email");
        android.widget.Button replied = SettingsProfile.primary(a, root, "Mark Replied");
        android.widget.Button closed = SettingsProfile.primary(a, root, "Mark Closed");

        Api.getClient().getWebsiteContactDetails(contactId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || response.body() == null) return;
                AllApiResponse b = response.body();
                String subject = ReportUiHelper.nz(b.getSubject());
                if (subject.isEmpty()) subject = "Website enquiry";
                StringBuilder sb = new StringBuilder();
                sb.append(b.getName()).append("\n")
                        .append(ReportUiHelper.nz(b.getEmail())).append("\n")
                        .append(b.getStatus()).append("  ·  ")
                        .append(ReportUiHelper.nz(b.getCreatedAt())).append("\n\n")
                        .append("Subject: ").append(subject).append("\n\n")
                        .append(ReportUiHelper.nz(b.getMessage()));
                body.setText(sb.toString());

                reply.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:" + Uri.encode(ReportUiHelper.nz(b.getEmail()))));
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Re: " + subject);
                    startActivity(Intent.createChooser(intent, "Send email"));
                });
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
            }
        });

        replied.setOnClickListener(v -> updateStatus(a, contactId, "Replied"));
        closed.setOnClickListener(v -> updateStatus(a, contactId, "Closed"));

        scroll.addView(root);
        return scroll;
    }

    private void updateStatus(Activity a, String contactId, String status) {
        Api.getClient().updateWebsiteContactStatus(contactId, status).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                Toast.makeText(a, "Status updated", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).lockUnlockDrawer(1);
    }
}
