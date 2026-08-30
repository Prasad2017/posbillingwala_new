package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Extra.SupportUiHelper;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.SupportTicketItem;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupportTicketDetails extends Fragment {
    Activity activity;
    LinearLayout root;
    TextView body;
    TextView statusBanner;
    android.widget.EditText reply;
    Button sendBtn;
    String ticketId;
    boolean closed;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        ticketId = getArguments() != null ? getArguments().getString("ticketId", "") : "";
        root = SupportUiHelper.form(activity);
        SupportUiHelper.addScreenHeader(activity, root, getString(R.string.support_ticket_details),
                () -> ((MainActivity) activity).navigateBack());
        SupportUiHelper.notice(activity, root, getString(R.string.support_online_only_notice));
        statusBanner = new TextView(activity);
        statusBanner.setVisibility(View.GONE);
        root.addView(statusBanner);
        body = new TextView(activity);
        body.setText(R.string.support_loading);
        SupportUiHelper.styleDetailBody(body);
        root.addView(body);
        reply = SupportUiHelper.field(activity, root, getString(R.string.support_reply), "");
        sendBtn = SupportUiHelper.primary(activity, root, getString(R.string.support_send_reply));
        sendBtn.setOnClickListener(v -> sendReply());
        Button refreshBtn = SupportUiHelper.primary(activity, root, getString(R.string.support_refresh));
        refreshBtn.setOnClickListener(v -> loadDetails());
        SupportUiHelper.applySideBySideButtons(activity, root, sendBtn, refreshBtn);
        return SupportUiHelper.wrapScreen(activity, root);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        loadDetails();
    }

    private void loadDetails() {
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
            return;
        }
        body.setText(R.string.support_loading);
        Api.getClient(activity).getSupportTicketDetails(MainActivity.userId, ticketId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || response.body() == null) return;
                AllApiResponse b = response.body();
                if (!"true".equalsIgnoreCase(b.getStatus()) && !"1".equals(b.getStatus())) {
                    Toast.makeText(activity, R.string.support_load_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                String st = b.getTicketStatus() != null ? b.getTicketStatus() : "";
                closed = st.equalsIgnoreCase("closed") || st.equalsIgnoreCase("resolved");
                showStatusBanner(st);
                StringBuilder sb = new StringBuilder();
                sb.append(b.getTicketNo()).append("\n").append(b.getSubject()).append("\n\n");
                if (b.getDescription() != null && !b.getDescription().isEmpty()) {
                    sb.append(getString(R.string.support_you)).append(": ").append(b.getDescription()).append("\n\n");
                }
                if (b.getTicketMessages() != null) {
                    for (SupportTicketItem.SupportMessageItem m : b.getTicketMessages()) {
                        String sender = m.getSender() != null ? m.getSender() : "";
                        boolean isAdmin = !sender.equalsIgnoreCase("You");
                        sb.append(isAdmin ? getString(R.string.support_admin) : getString(R.string.support_you));
                        sb.append(": ").append(m.getMessage());
                        if (m.getCreatedAt() != null && !m.getCreatedAt().isEmpty()) {
                            sb.append("\n  ").append(m.getCreatedAt());
                        }
                        sb.append("\n\n");
                    }
                }
                body.setText(sb.toString().trim());
                reply.setEnabled(!closed);
                sendBtn.setEnabled(!closed);
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(activity, R.string.support_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showStatusBanner(String status) {
        if (status == null || status.isEmpty()) {
            statusBanner.setVisibility(View.GONE);
            return;
        }
        statusBanner.setVisibility(View.VISIBLE);
        statusBanner.setPadding(20, 16, 20, 16);
        statusBanner.setBackgroundResource(R.drawable.button_rounded_border);
        if (closed) {
            statusBanner.setText(getString(R.string.support_ticket_closed_banner, status));
            statusBanner.setTextColor(ContextCompat.getColor(activity, R.color.red));
        } else {
            statusBanner.setText(getString(R.string.support_ticket_open_banner, status));
            statusBanner.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimary));
        }
    }

    private void sendReply() {
        if (closed) {
            Toast.makeText(activity, R.string.support_ticket_closed, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
            return;
        }
        String msg = reply.getText().toString().trim();
        if (msg.isEmpty()) {
            Toast.makeText(activity, R.string.support_reply_required, Toast.LENGTH_SHORT).show();
            return;
        }
        Api.getClient(activity).replySupportTicket(MainActivity.userId, ticketId, msg).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                AllApiResponse body = response.body();
                if (body != null && ("1".equals(body.getStatus()) || "true".equalsIgnoreCase(body.getStatus()))) {
                    Toast.makeText(activity, R.string.support_reply_sent, Toast.LENGTH_SHORT).show();
                    reply.setText("");
                    loadDetails();
                } else {
                    Toast.makeText(activity, body != null && body.getMessage() != null ? body.getMessage() : getString(R.string.support_load_failed), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Toast.makeText(activity, R.string.support_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
