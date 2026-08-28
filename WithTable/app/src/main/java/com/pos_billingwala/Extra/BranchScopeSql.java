package com.pos_billingwala.Extra;

import com.pos_billingwala.Activity.MainActivity;

/**
 * SQLite scope for single-branch POS device (organization + branch).
 */
public final class BranchScopeSql {

    private BranchScopeSql() {
    }

    public static String effectiveBranchId() {
        return BranchSession.effectiveBranchId();
    }

    public static String branchLabel() {
        if (MainActivity.branchLabel != null && !MainActivity.branchLabel.trim().isEmpty()) {
            return MainActivity.branchLabel.trim();
        }
        if (MainActivity.shopName != null && !MainActivity.shopName.trim().isEmpty()) {
            return MainActivity.shopName.trim();
        }
        return "This Branch";
    }

    /** AND clause + single branchId bind arg (strict — other licences/branches excluded). */
    public static ScopeClause invoiceBranchScope() {
        String branchId = effectiveBranchId();
        if (branchId == null || branchId.isEmpty()) {
            return ScopeClause.none();
        }
        return new ScopeClause(" AND branchId = ?", new String[]{branchId});
    }

    public static final class ScopeClause {
        public final String sql;
        public final String[] args;

        ScopeClause(String sql, String[] args) {
            this.sql = sql;
            this.args = args;
        }

        static ScopeClause none() {
            return new ScopeClause("", new String[0]);
        }
    }
}
