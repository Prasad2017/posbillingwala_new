package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.LoginResponse;

/**
 * Persists server license session fields from login/check responses.
 */
public final class LicenseSession {

    private LicenseSession() {
    }

    public static void saveFromLogin(Context context, LoginResponse response) {
        TrialLimits.saveFromLogin(context, response);
        LicenseValidator.saveFromLogin(context, response);
    }

    public static boolean isBillingAllowed(Context context, POSBillingWalaDatabase database) {
        if (LicenseValidator.hasStoredPayload(context)) {
            return LicenseValidator.isValidForBilling(context, database);
        }
        // Legacy fallback until first online refresh issues signed payload
        if (!TrialLimits.isTrial(context)) {
            return true;
        }
        return !TrialLimits.isNewBillBlocked(context, database);
    }

    public static String billingBlockedMessage(Context context, POSBillingWalaDatabase database) {
        if (LicenseValidator.hasStoredPayload(context)) {
            LicenseValidator.ValidationResult result = LicenseValidator.validate(context, database);
            if (result.message != null && !result.message.isEmpty()) {
                return result.message;
            }
        }
        return TrialLimits.blockedMessage(context);
    }
}
