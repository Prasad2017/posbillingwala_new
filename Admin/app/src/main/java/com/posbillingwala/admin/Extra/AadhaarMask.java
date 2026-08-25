package com.posbillingwala.admin.Extra;

/**
 * Mask Aadhaar for list / non-privileged UI. Never show full number in lists.
 */
public final class AadhaarMask {

    private AadhaarMask() {
    }

    public static String mask(String aadhaar) {
        if (aadhaar == null) {
            return "";
        }
        String digits = aadhaar.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "XXXX XXXX XXXX";
        }
        String last4 = digits.substring(digits.length() - 4);
        return "XXXX XXXX " + last4;
    }
}
