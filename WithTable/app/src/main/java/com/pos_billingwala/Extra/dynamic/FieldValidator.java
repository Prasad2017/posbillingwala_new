package com.pos_billingwala.Extra.dynamic;

import android.text.TextUtils;

/**
 * Lightweight field validators for Dynamic forms (Phase 04).
 */
public final class FieldValidator {

    private FieldValidator() {
    }

    public static boolean required(String value) {
        return value != null && TextUtils.getTrimmedLength(value) > 0;
    }

    public static boolean positiveNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            return Double.parseDouble(value.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean nonNegativeNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        try {
            return Double.parseDouble(value.trim()) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String requireMessage(String fieldLabel) {
        return (fieldLabel != null ? fieldLabel : "Field") + " is required";
    }
}
