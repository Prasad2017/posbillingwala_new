package com.pos_billingwala.Extra;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Staff personal PIN storage helper.
 * New PINs are stored as {@code sha256:<hex>}; legacy plaintext still verifies.
 * Report PIN / shop PIN is unchanged (server-issued plaintext).
 */
public final class StaffPinHasher {

    public static final String PREFIX = "sha256:";

    private StaffPinHasher() {
    }

    /** Hash a raw PIN for storage, or return empty if blank. Already-hashed values pass through. */
    public static String hashForStorage(@Nullable String rawPin) {
        if (rawPin == null) {
            return "";
        }
        String trimmed = rawPin.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (isHashed(trimmed)) {
            return trimmed;
        }
        return PREFIX + sha256Hex(trimmed);
    }

    public static boolean isHashed(@Nullable String stored) {
        return stored != null && stored.startsWith(PREFIX) && stored.length() > PREFIX.length();
    }

    /**
     * Verify entered PIN against stored value (hashed or legacy plaintext).
     * Empty stored PIN means no personal PIN required.
     */
    public static boolean matches(@Nullable String storedPin, @Nullable CharSequence entered) {
        if (storedPin == null || storedPin.trim().isEmpty()) {
            return true;
        }
        if (entered == null) {
            return false;
        }
        String enteredTrim = entered.toString().trim();
        if (enteredTrim.isEmpty()) {
            return false;
        }
        String stored = storedPin.trim();
        if (isHashed(stored)) {
            String expected = stored.substring(PREFIX.length());
            return constantTimeEquals(expected, sha256Hex(enteredTrim));
        }
        // Legacy plaintext (case-insensitive, same as prior behaviour).
        return stored.equalsIgnoreCase(enteredTrim);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // Extremely unlikely; fall back to reversible-looking marker so we never store raw on failure.
            return value;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < x.length; i++) {
            result |= x[i] ^ y[i];
        }
        return result == 0;
    }
}
