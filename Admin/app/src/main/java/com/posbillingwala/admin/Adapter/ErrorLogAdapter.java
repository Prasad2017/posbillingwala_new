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
import java.util.Locale;

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
        String severity = nz(e.getSeverity()).toUpperCase(Locale.US);
        holder.binding.severityBadge.setText(severity.isEmpty() ? "ERROR" : severity);
        applySeverityStyle(holder, severity);

        String typeLabel = typeLabel(e);
        holder.binding.typeBadge.setText(typeLabel);
        applyTypeStyle(holder, typeLabel);

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
        if (!nz(e.getOriginalExceptionClass()).isEmpty() && !"-".equals(nz(e.getOriginalExceptionClass()))) {
            meta.append(" · ").append(simpleClass(e.getOriginalExceptionClass()));
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

    private static String typeLabel(ErrorLogSummary e) {
        String type = nz(e.getErrorType()).toUpperCase(Locale.US);
        String clazz = nz(e.getOriginalExceptionClass()).toLowerCase(Locale.US);
        String cat = nz(e.getErrorCategory()).toLowerCase(Locale.US);
        if (type.equals("ANR")) {
            return "ANR";
        }
        if (type.equals("NATIVE_CRASH")) {
            return "NATIVE";
        }
        if (type.equals("LOW_MEMORY") || clazz.contains("outofmemory") || cat.equals("oom")) {
            return "OOM";
        }
        if (type.equals("DEVICE")) {
            if (cat.contains("storage")) {
                return "STORAGE";
            }
            if (cat.contains("thermal")) {
                return "THERMAL";
            }
            if (cat.contains("battery")) {
                return "BATTERY";
            }
            return "DEVICE";
        }
        if (type.equals("CRASH") || clazz.contains("nullpointer") || cat.equals("npe")) {
            if (clazz.contains("nullpointer") || cat.equals("npe")) {
                return "NPE";
            }
            return "CRASH";
        }
        if (type.isEmpty() || type.equals("-")) {
            return "APP";
        }
        return type;
    }

    private static String simpleClass(String fqcn) {
        if (fqcn == null) {
            return "";
        }
        int dot = fqcn.lastIndexOf('.');
        return dot >= 0 ? fqcn.substring(dot + 1) : fqcn;
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

    private void applyTypeStyle(Holder holder, String type) {
        int bg = R.drawable.bg_badge_trial;
        int fg = R.color.statusTrial;
        if ("NPE".equals(type) || "CRASH".equals(type) || "NATIVE".equals(type)
                || "ANR".equals(type) || "OOM".equals(type)
                || "DEVICE".equals(type) || "STORAGE".equals(type)
                || "THERMAL".equals(type) || "BATTERY".equals(type)) {
            bg = R.drawable.bg_badge_expired;
            fg = R.color.statusExpired;
        } else if ("API".equals(type) || "NETWORK".equals(type)) {
            bg = R.drawable.bg_badge_suspended;
            fg = R.color.statusSuspended;
        } else if ("PRINTER".equals(type)) {
            bg = R.drawable.bg_badge_trial;
            fg = R.color.statusTrial;
        } else if ("DATABASE".equals(type)) {
            bg = R.drawable.bg_badge_active;
            fg = R.color.statusActive;
        }
        holder.binding.typeBadge.setBackgroundResource(bg);
        holder.binding.typeBadge.setTextColor(
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
