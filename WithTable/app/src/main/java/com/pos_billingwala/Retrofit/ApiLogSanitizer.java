package com.pos_billingwala.Retrofit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Redacts secrets from API payloads before Crashlytics upload. */
final class ApiLogSanitizer {

    private static final Pattern JSON_SECRET = Pattern.compile(
            "(\"(?:mpin|password|authToken|token|app_licence_key|licence_key|android_device_id|androidId)\"\\s*:\\s*\")([^\"]*)(\")",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FORM_SECRET = Pattern.compile(
            "((?:mpin|password|authToken|token|app_licence_key|licence_key|android_device_id|androidId)=)([^&\\s]*)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BEARER = Pattern.compile(
            "(Bearer\\s+)(\\S+)",
            Pattern.CASE_INSENSITIVE
    );

    private ApiLogSanitizer() {
    }

    static String sanitize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String sanitized = raw;
        sanitized = redactBearer(sanitized);
        sanitized = redactJson(sanitized);
        sanitized = redactForm(sanitized);
        return sanitized;
    }

    private static String redactBearer(String input) {
        Matcher matcher = BEARER.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(1) + "***"));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String redactJson(String input) {
        Matcher matcher = JSON_SECRET.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(1) + "***" + matcher.group(3)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String redactForm(String input) {
        Matcher matcher = FORM_SECRET.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(1) + "***"));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
