package com.pos_billingwala.Extra;

/**
 * Non-fatal marker for failed API calls reported to Crashlytics.
 */
public final class ApiFailureException extends Exception {

    public ApiFailureException(String message) {
        super(message);
    }
}
