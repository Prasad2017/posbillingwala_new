package com.pos_billingwala.Extra;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacts secrets from crash/error payloads before queue/upload.
 * Masks credentials only — never replaces technical exception text with generics.
 */
public final class LogSanitizer {

    public static final int DEFAULT_MAX_BYTES = 20 * 1024;

    private static final Pattern JSON_SECRET = Pattern.compile(
            "(\"(?:password|mpin|otp|token|authorization|authToken|refresh_token|refreshToken|"
                    + "access_token|accessToken|aadhaar|aadhar|card_number|cardNumber|cvv|secret|"
                    + "api_key|apiKey|app_licence_key|licence_key|license_key|android_device_id|androidId)"
                    + "\"\\s*:\\s*\")([^\"]*)(\")",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FORM_SECRET = Pattern.compile(
            "((?:password|mpin|otp|token|authorization|authToken|refresh_token|refreshToken|"
                    + "access_token|accessToken|aadhaar|aadhar|card_number|cvv|secret|api_key|"
                    + "app_licence_key|licence_key|license_key|android_device_id|androidId)=)([^&\\s]*)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BEARER = Pattern.compile(
            "(Bearer\\s+)(\\S+)",
            Pattern.CASE_INSENSITIVE
    );

    private LogSanitizer() {
    }

    public static String sanitize(String raw) {
        return sanitize(raw, DEFAULT_MAX_BYTES);
    }

    public static String sanitize(String raw, int maxBytes) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String sanitized = raw;
        sanitized = redactBearer(sanitized);
        sanitized = redactJson(sanitized);
        sanitized = redactForm(sanitized);
        if (maxBytes > 0 && sanitized.length() > maxBytes) {
            return sanitized.substring(0, maxBytes) + "…[truncated]";
        }
        return sanitized;
    }

    /** True if message is a forbidden generic placeholder (must not overwrite originals). */
    public static boolean isGenericMessage(String msg) {
        if (msg == null) {
            return false;
        }
        String n = msg.trim().toLowerCase();
        return n.equals("something went wrong")
                || n.equals("unknown error")
                || n.equals("operation failed")
                || n.equals("an error occurred")
                || n.equals("error occurred")
                || n.equals("failed")
                || n.equals("unknown");
    }

    private static String redactBearer(String input) {
        Matcher matcher = BEARER.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(1) + "********"));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String redactJson(String input) {
        Matcher matcher = JSON_SECRET.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(1) + "******" + matcher.group(3)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String redactForm(String input) {
        Matcher matcher = FORM_SECRET.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(1) + "******"));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
