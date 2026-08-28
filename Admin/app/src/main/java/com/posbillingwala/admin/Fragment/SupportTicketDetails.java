package com.posbillingwala.admin.Fragment;

import android.app.Activity;
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
import com.posbillingwala.admin.Model.SupportTicketItem;
import com.posbillingwala.admin.Retrofit.Api;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupportTicketDetails extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Activity a = getActivity();
        ((MainActivity) a).setScreenTitle("Ticket Details");
        String ticketId = getArguments() != null ? getArguments().getString("ticketId") : "";
        ScrollView scroll = new ScrollView(a);
        LinearLayout root = SettingsProfile.form(a);
        TextView body = new TextView(a);
        body.setText("Loading...");
        root.addView(body);
        android.widget.EditText reply = SettingsProfile.field(a, root, "Reply", "");
        android.widget.Button send = SettingsProfile.primary(a, root, "Send Reply");
        android.widget.Button resolve = SettingsProfile.primary(a, root, "Mark Resolved");
        resolve.setOnClickListener(v -> Api.getClient().updateSupportTicketStatus(ticketId, "Resolved")
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                        Toast.makeText(a, "Ticket resolved", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<AllApiResponse> call, Throwable t) {
                    }
                }));
        Api.getClient().getSupportTicketDetails(ticketId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || response.body() == null) return;
                AllApiResponse b = response.body();
                StringBuilder sb = new StringBuilder();
                sb.append(b.getTicketNo()).append("  ·  ").append(b.getStatus()).append("\n")
                        .append(b.getSubject()).append("\n\n").append(ReportUiHelper.nz(b.getDescription())).append("\n\n");
                if (b.getTicketMessages() != null) {
                    for (SupportTicketItem.SupportMessageItem m : b.getTicketMessages()) {
                        sb.append(m.getSender()).append(": ").append(m.getMessage()).append("\n");
                    }
                }
                body.setText(sb.toString());
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
            }
        });
        send.setOnClickListener(v -> Api.getClient().replySupportTicket(ticketId, reply.getText().toString(), "Admin")
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                        Toast.makeText(a, "Reply sent", Toast.LENGTH_SHORT).show();
                        reply.setText("");
                    }

                    @Override
                    public void onFailure(Call<AllApiResponse> call, Throwable t) {
                    }
                }));
        scroll.addView(root);
        return scroll;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).lockUnlockDrawer(1);
    }
}
