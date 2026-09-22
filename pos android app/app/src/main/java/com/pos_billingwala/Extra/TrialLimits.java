package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.LoginResponse;
import com.pos_billingwala.R;

/**
 * P4-2: Trial bill limits come from the server login/check response only.
 * Never hard-code trial day or bill caps in the APK.
 */
public final class TrialLimits {

    private TrialLimits() {
    }

    public static void saveFromLogin(Context context, LoginResponse response) {
        if (response == null) {
            return;
        }
        Common.saveUserData(context, "licenseType", nullToEmpty(response.getLicenseType()));
        Common.saveUserData(context, "isTrial", nullToEmpty(response.getIsTrial()));
        Common.saveUserData(context, "trialDays", nullToEmpty(response.getTrialDays()));
        Common.saveUserData(context, "trialMaxBills", nullToEmpty(response.getTrialMaxBills()));
        Common.saveUserData(context, "trialBillCount", nullToEmpty(response.getTrialBillCount()));
        Common.saveUserData(context, "trialBillsRemaining", nullToEmpty(response.getTrialBillsRemaining()));
    }

    public static boolean isTrial(Context context) {
        return "1".equals(Common.getSavedUserData(context, "isTrial"));
    }

    /**
     * Soft local gate using server-provided max. If server never sent a max, do not block
     * (server insertInvoice still enforces on sync).
     */
    public static boolean isNewBillBlocked(Context context, POSBillingWalaDatabase database) {
        if (!isTrial(context)) {
            return false;
        }
        String maxRaw = Common.getSavedUserData(context, "trialMaxBills");
        if (maxRaw == null || maxRaw.trim().isEmpty()) {
            return false;
        }
        int maxBills;
        try {
            maxBills = Integer.parseInt(maxRaw.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        if (maxBills <= 0) {
            return false;
        }
        int localCount = database != null ? database.getTotalInvoiceCount() : 0;
        int serverCount = 0;
        try {
            String serverRaw = Common.getSavedUserData(context, "trialBillCount");
            if (serverRaw != null && !serverRaw.trim().isEmpty()) {
                serverCount = Integer.parseInt(serverRaw.trim());
            }
        } catch (NumberFormatException ignored) {
        }
        // Use the higher of local vs last known server count to avoid under-counting offline.
        return Math.max(localCount, serverCount) >= maxBills;
    }

    public static String blockedMessage(Context context) {
        String max = Common.getSavedUserData(context, "trialMaxBills");
        if (max == null || max.trim().isEmpty()) {
            return context.getString(R.string.licence_msg_trial_limit_generic);
        }
        try {
            int maxBills = Integer.parseInt(max.trim());
            return context.getString(R.string.licence_msg_trial_limit, maxBills);
        } catch (NumberFormatException e) {
            return context.getString(R.string.licence_msg_trial_limit_generic);
        }
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
