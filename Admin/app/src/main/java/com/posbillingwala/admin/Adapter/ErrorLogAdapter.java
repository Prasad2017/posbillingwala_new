package com.posbillingwala.admin.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Model.ErrorLogSummary;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.databinding.ItemErrorLogCardBinding;

import java.util.List;

public class ErrorLogAdapter extends RecyclerView.Adapter<ErrorLogAdapter.Holder> {

    public interface OnClick {
        void onClick(ErrorLogSummary item);
    }

    private final List<ErrorLogSummary> items;
    private final OnClick onClick;

    public ErrorLogAdapter(List<ErrorLogSummary> items, OnClick onClick) {
        this.items = items;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemErrorLogCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ErrorLogSummary e = items.get(position);
        String severity = nz(e.getSeverity()).toUpperCase();
        holder.binding.severityBadge.setText(severity.isEmpty() ? "ERROR" : severity);
        applySeverityStyle(holder, severity);

        String title = emptyToNull(e.getSummary());
        if (title == null) {
            title = emptyToNull(e.getOriginalExceptionClass());
        }
        if (title == null) {
            title = nz(e.getErrorType()) + " error";
        }
        holder.binding.title.setText(title);

        StringBuilder meta = new StringBuilder();
        meta.append(nz(e.getAppType()));
        if (!nz(e.getAppVersion()).isEmpty()) {
            meta.append(" v").append(e.getAppVersion());
        }
        meta.append(" · ").append(nz(e.getScreenName()));
        meta.append("\n");
        if (!nz(e.getUserAction()).isEmpty()) {
            meta.append(e.getUserAction()).append("\n");
        }
        if (!nz(e.getApiMethodPath()).isEmpty()) {
            meta.append(e.getApiMethodPath()).append("\n");
        }
        String shop = nz(e.getShopName());
        String branch = nz(e.getBranchLabel());
        if (!shop.equals("-") || !branch.equals("-")) {
            meta.append(shop.equals("-") ? "" : shop);
            if (!branch.equals("-")) {
                if (!shop.equals("-")) {
                    meta.append(" · ");
                }
                meta.append(branch);
            }
        }
        holder.binding.meta.setText(meta.toString().trim());

        String occ = nz(e.getOccurrenceCount());
        String last = nz(e.getLastSeenAt());
        holder.binding.footer.setText(occ + " occurrence" + ("1".equals(occ) ? "" : "s")
                + " · Last seen " + last);

        holder.binding.getRoot().setOnClickListener(v -> {
            if (onClick != null) {
                onClick.onClick(e);
            }
        });
    }

    private void applySeverityStyle(Holder holder, String severity) {
        int bg;
        int fg;
        if ("CRITICAL".equals(severity)) {
            bg = R.drawable.bg_badge_expired;
            fg = R.color.statusExpired;
        } else if ("WARNING".equals(severity)) {
            bg = R.drawable.bg_badge_trial;
            fg = R.color.statusTrial;
        } else if ("INFO".equals(severity)) {
            bg = R.drawable.bg_badge_active;
            fg = R.color.statusActive;
        } else {
            bg = R.drawable.bg_badge_suspended;
            fg = R.color.statusSuspended;
            if ("ERROR".equals(severity)) {
                bg = R.drawable.bg_badge_expired;
                fg = R.color.statusExpired;
            }
        }
        holder.binding.severityBadge.setBackgroundResource(bg);
        holder.binding.severityBadge.setTextColor(
                ContextCompat.getColor(holder.binding.getRoot().getContext(), fg));
    }

    private static String nz(String v) {
        return v == null || v.isEmpty() ? "-" : v;
    }

    private static String emptyToNull(String v) {
        return v == null || v.isEmpty() ? null : v;
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemErrorLogCardBinding binding;

        Holder(ItemErrorLogCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
