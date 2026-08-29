package com.posbillingwala.dealer.Extra;

import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.posbillingwala.dealer.Model.LicenseResponse;
import com.posbillingwala.dealer.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class LicenseStatusHelper {

    public static final String STATUS_TRIAL = "TRIAL";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_PENDING = "PENDING ACTIVATION";

    private LicenseStatusHelper() {}

    public static String displayStatus(LicenseResponse license) {
        if (license == null) {
            return STATUS_PENDING;
        }
        String raw = safe(license.getLicenseStatus()).toLowerCase(Locale.US);
        String type = safe(license.getLicenseType()).toLowerCase(Locale.US);
        String validity = safe(license.getLicenseValidity());
        if (raw.contains("expire") || isPastExpiry(license.getExpiryDate())) {
            return STATUS_EXPIRED;
        }
        if ("demo".equals(type) || "trial".equals(type) || "7".equals(validity)) {
            return STATUS_TRIAL;
        }
        if (!safe(license.getLicenseKey()).isEmpty() || !safe(license.getExpiryDate()).isEmpty()) {
            return STATUS_ACTIVE;
        }
        return STATUS_PENDING;
    }

    public static String shortLabel(String displayStatus) {
        if (STATUS_ACTIVE.equals(displayStatus)) return "Active";
        if (STATUS_TRIAL.equals(displayStatus)) return "Trial";
        if (STATUS_EXPIRED.equals(displayStatus)) return "Expired";
        return "Pending";
    }

    public static void applyBadge(TextView badge, String displayStatus) {
        if (badge == null) return;
        badge.setText(shortLabel(displayStatus));
        int bg = R.drawable.bg_badge_suspended;
        int color = R.color.statusSuspended;
        if (STATUS_ACTIVE.equals(displayStatus)) {
            bg = R.drawable.bg_badge_active;
            color = R.color.statusActive;
        } else if (STATUS_TRIAL.equals(displayStatus)) {
            bg = R.drawable.bg_badge_trial;
            color = R.color.statusTrial;
        } else if (STATUS_EXPIRED.equals(displayStatus)) {
            bg = R.drawable.bg_badge_expired;
            color = R.color.statusExpired;
        }
        badge.setBackgroundResource(bg);
        badge.setBackgroundTintList(null);
        badge.setTextColor(ContextCompat.getColor(badge.getContext(), color));
    }

    private static boolean isPastExpiry(String expiryDate) {
        Date expiry = parseDate(expiryDate);
        if (expiry == null) return false;
        return TimeUnit.MILLISECONDS.toDays(expiry.getTime() - System.currentTimeMillis()) < 0;
    }

    private static Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String raw = value.trim();
        if (raw.length() >= 10) raw = raw.substring(0, 10);
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw);
        } catch (ParseException e) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
