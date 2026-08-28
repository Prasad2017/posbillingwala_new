package com.posbillingwala.admin.Extra;

import android.graphics.Color;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.posbillingwala.admin.Model.LicenseResponse;
import com.posbillingwala.admin.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Maps license row fields to CRM status labels and badge colors.
 * Dealers have no licenses — use Active/Inactive separately.
 */
public final class LicenseStatusHelper {

    public static final String STATUS_TRIAL = "TRIAL";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_EXPIRING = "EXPIRING SOON";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_REVOKED = "REVOKED";
    public static final String STATUS_LIFETIME = "LIFETIME";
    public static final String STATUS_PENDING = "PENDING ACTIVATION";

    private LicenseStatusHelper() {
    }

    /**
     * License status from type / expiry / DB status.
     * A present license key is enough for Active/Trial — device bind is not required.
     */
    public static String displayStatus(LicenseResponse license) {
        if (license == null) {
            return STATUS_PENDING;
        }

        String raw = safe(license.getLicenseStatus()).toLowerCase(Locale.US);
        String type = safe(license.getLicenseType()).toLowerCase(Locale.US);
        String validity = LicenceValidityTiers.toDayCount(license.getLicenseValidity());
        String key = safe(license.getLicenseKey());
        boolean hasKey = !key.isEmpty();

        if (raw.contains("suspend")) {
            return STATUS_SUSPENDED;
        }
        if (raw.contains("revok")) {
            return STATUS_REVOKED;
        }

        if ("10958".equals(validity) || "lifetime".equalsIgnoreCase(safe(license.getLicenseValidity()))) {
            if (!raw.contains("expire")) {
                return STATUS_LIFETIME;
            }
        }

        if (raw.contains("expire") || isPastExpiry(license.getExpiryDate())) {
            return STATUS_EXPIRED;
        }

        boolean isTrial = "demo".equals(type) || "trial".equals(type) || "7".equals(validity);
        long daysLeft = daysUntilExpiry(license.getExpiryDate());

        if (isTrial) {
            if (daysLeft >= 0 && daysLeft <= 2) {
                return STATUS_EXPIRING;
            }
            return STATUS_TRIAL;
        }

        if (daysLeft >= 0 && daysLeft <= 30) {
            return STATUS_EXPIRING;
        }

        if (raw.contains("pending") && !hasKey) {
            return STATUS_PENDING;
        }

        // License key present (or any non-expired paid license) → Active
        if (hasKey || !raw.isEmpty() || !safe(license.getExpiryDate()).isEmpty()) {
            return STATUS_ACTIVE;
        }

        return STATUS_PENDING;
    }

    /** Short chip/list label: Active, Trial, Expired, Expiring, … */
    public static String shortLabel(String displayStatus) {
        if (displayStatus == null) {
            return "Pending";
        }
        switch (displayStatus) {
            case STATUS_ACTIVE:
                return "Active";
            case STATUS_LIFETIME:
                return "Lifetime";
            case STATUS_TRIAL:
                return "Trial";
            case STATUS_EXPIRING:
                return "Expiring";
            case STATUS_EXPIRED:
                return "Expired";
            case STATUS_SUSPENDED:
                return "Suspended";
            case STATUS_REVOKED:
                return "Revoked";
            case STATUS_PENDING:
            default:
                return "Pending";
        }
    }

    public static int badgeColor(String displayStatus) {
        if (displayStatus == null) {
            return Color.parseColor("#6B7280");
        }
        switch (displayStatus) {
            case STATUS_ACTIVE:
            case STATUS_LIFETIME:
                return Color.parseColor("#16A34A"); // green
            case STATUS_TRIAL:
                return Color.parseColor("#F59E0B"); // amber
            case STATUS_EXPIRING:
                return Color.parseColor("#F59E0B"); // amber
            case STATUS_EXPIRED:
            case STATUS_REVOKED:
                return Color.parseColor("#DC2626"); // red
            case STATUS_SUSPENDED:
            case STATUS_PENDING:
            default:
                return Color.parseColor("#6B7280"); // grey
        }
    }

    public static int badgeBackgroundRes(String displayStatus) {
        if (displayStatus == null) {
            return R.drawable.bg_badge_suspended;
        }
        switch (displayStatus) {
            case STATUS_ACTIVE:
            case STATUS_LIFETIME:
                return R.drawable.bg_badge_active;
            case STATUS_TRIAL:
            case STATUS_EXPIRING:
                return R.drawable.bg_badge_trial;
            case STATUS_EXPIRED:
            case STATUS_REVOKED:
                return R.drawable.bg_badge_expired;
            case STATUS_SUSPENDED:
            case STATUS_PENDING:
            default:
                return R.drawable.bg_badge_suspended;
        }
    }

    public static int badgeTextColorRes(String displayStatus) {
        if (displayStatus == null) {
            return R.color.statusSuspended;
        }
        switch (displayStatus) {
            case STATUS_ACTIVE:
            case STATUS_LIFETIME:
                return R.color.statusActive;
            case STATUS_TRIAL:
            case STATUS_EXPIRING:
                return R.color.statusTrial;
            case STATUS_EXPIRED:
            case STATUS_REVOKED:
                return R.color.statusExpired;
            case STATUS_SUSPENDED:
            case STATUS_PENDING:
            default:
                return R.color.statusSuspended;
        }
    }

    public static void applyBadge(TextView badge, String displayStatus) {
        if (badge == null) return;
        badge.setText(shortLabel(displayStatus));
        badge.setBackgroundResource(badgeBackgroundRes(displayStatus));
        badge.setBackgroundTintList(null);
        badge.setTextColor(ContextCompat.getColor(badge.getContext(), badgeTextColorRes(displayStatus)));
    }

    /** Dealer accounts have no licenses — Active / Inactive only. */
    public static void applyDealerBadge(TextView badge, boolean active) {
        if (badge == null) return;
        if (active) {
            badge.setText("Active");
            badge.setBackgroundResource(R.drawable.bg_badge_active);
            badge.setBackgroundTintList(null);
            badge.setTextColor(ContextCompat.getColor(badge.getContext(), R.color.statusActive));
        } else {
            badge.setText("Inactive");
            badge.setBackgroundResource(R.drawable.bg_badge_suspended);
            badge.setBackgroundTintList(null);
            badge.setTextColor(ContextCompat.getColor(badge.getContext(), R.color.statusSuspended));
        }
    }

    public static boolean isSuspended(LicenseResponse license) {
        return STATUS_SUSPENDED.equals(displayStatus(license));
    }

    public static boolean canUpgradeOrRenew(LicenseResponse license) {
        String status = displayStatus(license);
        return !STATUS_REVOKED.equals(status) && !STATUS_SUSPENDED.equals(status);
    }

    private static boolean isPastExpiry(String expiryDate) {
        long days = daysUntilExpiry(expiryDate);
        return days < 0 && expiryDate != null && !expiryDate.trim().isEmpty();
    }

    private static long daysUntilExpiry(String expiryDate) {
        Date expiry = parseDate(expiryDate);
        if (expiry == null) {
            return Long.MAX_VALUE;
        }
        long diff = expiry.getTime() - System.currentTimeMillis();
        return TimeUnit.MILLISECONDS.toDays(diff);
    }

    private static Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String raw = value.trim();
        if (raw.length() >= 10) {
            raw = raw.substring(0, 10);
        }
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
