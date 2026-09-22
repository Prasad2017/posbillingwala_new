package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.ActionButtonUi;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Extra.TabletFormUi;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.SupportTicketItem;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;
import com.pos_billingwala.databinding.FragmentSupportTicketDetailsBinding;
import com.pos_billingwala.databinding.ItemSupportMessageBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupportTicketDetails extends Fragment {
    Activity activity;
    FragmentSupportTicketDetailsBinding binding;
    String ticketId;
    boolean closed;
    boolean oldestFirst = true;
    final List<ChatLine> conversation = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        ticketId = getArguments() != null ? getArguments().getString("ticketId", "") : "";
        binding = FragmentSupportTicketDetailsBinding.inflate(inflater, container, false);

        binding.backButton.setOnClickListener(v -> ((MainActivity) activity).navigateBack());
        binding.overflowButton.setOnClickListener(this::showOverflowMenu);
        binding.sortButton.setOnClickListener(v -> {
            oldestFirst = !oldestFirst;
            binding.sortLabel.setText(oldestFirst
                    ? R.string.support_sort_oldest
                    : R.string.support_sort_newest);
            renderConversation();
        });
        ActionButtonUi.bind(binding.sendReplyButton.getRoot(), R.drawable.ic_send, R.string.support_send_reply);
        binding.sendReplyButton.getRoot().setOnClickListener(v -> sendReply());
        binding.refreshTicketsButton.setOnClickListener(v -> loadDetails());
        binding.openNewTicketButton.setOnClickListener(v ->
                ((MainActivity) activity).loadFragment(new CreateSupportTicket(), true));
        binding.attachButton.setOnClickListener(v ->
                Toast.makeText(activity, R.string.support_attachment_sub, Toast.LENGTH_SHORT).show());
        binding.emojiButton.setOnClickListener(v -> {
            CharSequence current = binding.replyInput.getText();
            binding.replyInput.append(current != null && current.length() > 0 ? " 😊" : "😊");
        });

        TabletFormUi.applyCenteredPanel(binding.supportContent);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        loadDetails();
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu menu = new PopupMenu(activity, anchor);
        menu.getMenu().add(0, 1, 0, getString(R.string.support_refresh_tickets));
        menu.getMenu().add(0, 2, 1, getString(R.string.support_open_new_ticket));
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                loadDetails();
                return true;
            }
            if (item.getItemId() == 2) {
                ((MainActivity) activity).loadFragment(new CreateSupportTicket(), true);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void loadDetails() {
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
            return;
        }
        binding.loadingState.setVisibility(View.VISIBLE);
        Api.getClient(activity).getSupportTicketDetails(MainActivity.userId, ticketId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || binding == null) {
                    return;
                }
                binding.loadingState.setVisibility(View.GONE);
                if (response.body() == null) {
                    return;
                }
                AllApiResponse body = response.body();
                if (!"true".equalsIgnoreCase(body.getStatus()) && !"1".equals(body.getStatus())) {
                    Toast.makeText(activity, R.string.support_load_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                String status = body.getTicketStatus() != null ? body.getTicketStatus() : "";
                closed = status.equalsIgnoreCase("closed") || status.equalsIgnoreCase("resolved");
                bindTicketHeader(body, status);
                showStatusBanner(status);
                applyReplyUi(closed);
                buildConversation(body);
                renderConversation();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                if (!isAdded() || binding == null) {
                    return;
                }
                binding.loadingState.setVisibility(View.GONE);
                Toast.makeText(activity, R.string.support_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindTicketHeader(AllApiResponse body, String status) {
        binding.ticketNo.setText(nz(body.getTicketNo()));
        String subtitle = body.getSubject();
        if (subtitle == null || subtitle.isEmpty()) {
            subtitle = body.getAppName();
        }
        binding.ticketSubject.setText(nz(subtitle));
        binding.ticketStatus.setText(formatStatus(status));
        binding.ticketStatus.setBackgroundResource(closed
                ? R.drawable.bg_support_status_closed
                : R.drawable.bg_support_status_open);
        binding.ticketStatus.setTextColor(ContextCompat.getColor(activity, closed
                ? R.color.statusExpired
                : R.color.statusActive));
    }

    private void applyReplyUi(boolean isClosed) {
        binding.closedActions.setVisibility(isClosed ? View.VISIBLE : View.GONE);
        binding.openActions.setVisibility(isClosed ? View.GONE : View.VISIBLE);
    }

    private void showStatusBanner(String status) {
        if (status == null || status.isEmpty()) {
            binding.statusBanner.setVisibility(View.GONE);
            return;
        }
        binding.statusBanner.setVisibility(View.VISIBLE);
        ImageView icon = binding.statusBannerIcon;
        TextView text = binding.statusBannerText;
        if (closed) {
            binding.statusBanner.setBackgroundResource(R.drawable.bg_support_status_closed_banner);
            icon.setImageResource(R.drawable.ic_lock);
            icon.setColorFilter(ContextCompat.getColor(activity, R.color.statusExpired));
            text.setText(getString(R.string.support_ticket_closed_banner, formatStatus(status)));
            text.setTextColor(ContextCompat.getColor(activity, R.color.statusExpired));
        } else {
            binding.statusBanner.setBackgroundResource(R.drawable.bg_support_status_open_banner);
            icon.setImageResource(R.drawable.ic_lock_open);
            icon.setColorFilter(ContextCompat.getColor(activity, R.color.statusActive));
            text.setText(getString(R.string.support_ticket_open_banner, formatStatus(status)));
            text.setTextColor(ContextCompat.getColor(activity, R.color.statusActive));
        }
    }

    private void buildConversation(AllApiResponse body) {
        conversation.clear();
        if (body.getDescription() != null && !body.getDescription().isEmpty()) {
            conversation.add(new ChatLine(false, body.getDescription(), body.getCreatedAt()));
        }
        if (body.getTicketMessages() != null) {
            for (SupportTicketItem.SupportMessageItem message : body.getTicketMessages()) {
                String sender = message.getSender() != null ? message.getSender() : "";
                boolean isSupport = !sender.equalsIgnoreCase("You");
                conversation.add(new ChatLine(isSupport, message.getMessage(), message.getCreatedAt()));
            }
        }
    }

    private void renderConversation() {
        binding.conversationList.removeAllViews();
        if (conversation.isEmpty()) {
            binding.emptyConversation.setVisibility(View.VISIBLE);
            return;
        }
        binding.emptyConversation.setVisibility(View.GONE);
        List<ChatLine> ordered = new ArrayList<>(conversation);
        if (!oldestFirst) {
            Collections.reverse(ordered);
        }
        LayoutInflater inflater = LayoutInflater.from(activity);
        for (int i = 0; i < ordered.size(); i++) {
            ChatLine line = ordered.get(i);
            ItemSupportMessageBinding item = ItemSupportMessageBinding.inflate(
                    inflater, binding.conversationList, false);
            boolean isSupport = line.support;
            item.messageAvatar.setText(isSupport ? "S" : "Y");
            item.messageAvatar.setBackgroundResource(isSupport
                    ? R.drawable.bg_support_icon_circle_green
                    : R.drawable.bg_support_icon_circle_blue);
            item.messageAvatar.setTextColor(ContextCompat.getColor(activity, isSupport
                    ? R.color.statusActive
                    : R.color.colorPrimary));
            item.messageSender.setText(isSupport ? R.string.support_admin : R.string.support_you);
            item.messageBody.setText(nz(line.message));
            item.messageTime.setText(nz(line.createdAt));
            item.timelineLine.setVisibility(i == ordered.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            binding.conversationList.addView(item.getRoot());
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
        String msg = binding.replyInput.getText() != null
                ? binding.replyInput.getText().toString().trim()
                : "";
        if (msg.isEmpty()) {
            Toast.makeText(activity, R.string.support_reply_required, Toast.LENGTH_SHORT).show();
            return;
        }
        Api.getClient(activity).replySupportTicket(MainActivity.userId, ticketId, msg)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                        if (!isAdded()) {
                            return;
                        }
                        AllApiResponse body = response.body();
                        if (body != null && ("1".equals(body.getStatus()) || "true".equalsIgnoreCase(body.getStatus()))) {
                            Toast.makeText(activity, R.string.support_reply_sent, Toast.LENGTH_SHORT).show();
                            binding.replyInput.setText("");
                            loadDetails();
                        } else {
                            Toast.makeText(activity, body != null && body.getMessage() != null
                                    ? body.getMessage()
                                    : getString(R.string.support_load_failed), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AllApiResponse> call, Throwable t) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(activity, R.string.support_load_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String formatStatus(String status) {
        if (status == null || status.isEmpty()) {
            return "—";
        }
        return status.substring(0, 1).toUpperCase(Locale.US) + status.substring(1).toLowerCase(Locale.US);
    }

    private String nz(String value) {
        return value == null || value.isEmpty() ? "—" : value;
    }

    private static final class ChatLine {
        final boolean support;
        final String message;
        final String createdAt;

        ChatLine(boolean support, String message, String createdAt) {
            this.support = support;
            this.message = message;
            this.createdAt = createdAt;
        }
    }
}
