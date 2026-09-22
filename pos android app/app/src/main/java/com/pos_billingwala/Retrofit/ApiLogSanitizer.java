package com.pos_billingwala.Retrofit;

import com.pos_billingwala.Extra.LogSanitizer;

/** @deprecated Prefer {@link com.pos_billingwala.Extra.LogSanitizer}; kept for interceptor call sites. */
@Deprecated
final class ApiLogSanitizer {

    private ApiLogSanitizer() {
    }

    static String sanitize(String raw) {
        return LogSanitizer.sanitize(raw);
    }
}
