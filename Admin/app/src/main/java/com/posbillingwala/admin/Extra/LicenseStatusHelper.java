package com.posbillingwala.admin.Extra;

import android.graphics.Color;

import com.posbillingwala.admin.Model.LicenseResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Maps license row fields to CRM status labels and badge colors.
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

    public static String displayStatus(LicenseResponse license) {
        if (license == null) {
            return STATUS_PENDING;
        }
        String raw = safe(license.getLicenseStatus()).toLowerCase(Locale.US);
        String type = safe(license.getLicenseType()).toLowerCase(Locale.US);
        String validity = LicenceValidityTiers.toDayCount(license.getLicenseValidity());

        if (raw.contains("suspend")) {
            return STATUS_SUSPENDED;
        }
        if (raw.contains("revok")) {
            return STATUS_REVOKED;
        }
        if ("10958".equals(validity) || "lifetime".equalsIgnoreCase(safe(license.getLicenseValidity()))) {
            if (!raw.contains("expire") && !raw.contains("suspend")) {
                return STATUS_LIFETIME;
            }
        }
        if (raw.contains("expire") || isPastExpiry(license.getExpiryDate())) {
            return STATUS_EXPIRED;
        }
        String deviceId = safe(license.getAndroidDeviceId());
        if (deviceId.isEmpty()) {
            if ("demo".equals(type) || "trial".equals(type) || "7".equals(validity)) {
                return STATUS_TRIAL;
            }
            return STATUS_PENDING;
        }
        if ("demo".equals(type) || "trial".equals(type) || "7".equals(validity)) {
            if (daysUntilExpiry(license.getExpiryDate()) <= 2 && daysUntilExpiry(license.getExpiryDate()) >= 0) {
                return STATUS_EXPIRING;
            }
            return STATUS_TRIAL;
        }
        long daysLeft = daysUntilExpiry(license.getExpiryDate());
        if (daysLeft >= 0 && daysLeft <= 30) {
            return STATUS_EXPIRING;
        }
        if (raw.contains("pending")) {
            return STATUS_PENDING;
        }
        return STATUS_ACTIVE;
    }

    public static int badgeColor(String displayStatus) {
        if (displayStatus == null) {
            return Color.parseColor("#607D8B");
        }
        switch (displayStatus) {
            case STATUS_ACTIVE:
            case STATUS_LIFETIME:
                return Color.parseColor("#2E7D32");
            case STATUS_TRIAL:
                return Color.parseColor("#1565C0");
            case STATUS_EXPIRING:
                return Color.parseColor("#EF6C00");
            case STATUS_EXPIRED:
            case STATUS_REVOKED:
                return Color.parseColor("#C62828");
            case STATUS_SUSPENDED:
                return Color.parseColor("#6A1B9A");
            case STATUS_PENDING:
            default:
                return Color.parseColor("#546E7A");
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
