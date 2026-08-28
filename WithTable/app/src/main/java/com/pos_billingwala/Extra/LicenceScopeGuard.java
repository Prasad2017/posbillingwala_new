package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.LoginResponse;

/**
 * Keeps local SQLite data tied to the active licence key / branch.
 * When the licence key changes, data from the previous key must not appear.
 */
public final class LicenceScopeGuard {

    public static final String KEY_BOUND_LICENCE = "boundLicenceKey";
    public static final String KEY_BOUND_BRANCH = "boundBranchId";

    private LicenceScopeGuard() {
    }

    /** Call after a successful login or licence refresh response. */
    public static void onLoginSuccess(Context context, LoginResponse response) {
        if (context == null || response == null) {
            return;
        }
        applyScope(context, trim(response.getLicenceKey()), resolveBranchId(response));
    }

    /** Ensures an existing session only sees data for the saved licence (app restart / upgrade). */
    public static void ensureSessionScope(Context context) {
        if (context == null) {
            return;
        }
        BranchSession.loadFromPreferences(context);
        applyScope(
                context,
                trim(Common.getSavedUserData(context, "LicenceKey")),
                trim(BranchSession.effectiveBranchId()));
    }

    private static void applyScope(Context context, String newKey, String newBranch) {
        if (newBranch == null || newBranch.isEmpty()) {
            return;
        }

        String oldKey = trim(Common.getSavedUserData(context, KEY_BOUND_LICENCE));
        String oldBranch = trim(Common.getSavedUserData(context, KEY_BOUND_BRANCH));
        POSBillingWalaDatabase database = new POSBillingWalaDatabase(context);

        boolean licenceChanged = !oldKey.isEmpty() && !newKey.isEmpty() && !oldKey.equals(newKey);
        boolean branchChanged = !oldBranch.isEmpty() && !oldBranch.equals(newBranch);
        boolean foreignBranchData = database.hasInvoicesForOtherBranch(newBranch);

        if (licenceChanged || branchChanged || foreignBranchData) {
            database.purgeLocalDataNotMatchingBranch(newBranch);
        } else {
            database.claimUnscopedRowsForBranch(newBranch);
        }

        if (!newKey.isEmpty()) {
            Common.saveUserData(context, KEY_BOUND_LICENCE, newKey);
        }
        Common.saveUserData(context, KEY_BOUND_BRANCH, newBranch);
        BranchSession.loadFromPreferences(context);
    }

    private static String resolveBranchId(LoginResponse response) {
        String branchId = trim(response.getBranchId());
        if (!branchId.isEmpty()) {
            return branchId;
        }
        return trim(response.getLicenceId());
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
