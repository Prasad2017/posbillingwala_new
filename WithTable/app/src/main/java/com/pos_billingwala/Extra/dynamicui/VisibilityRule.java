package com.pos_billingwala.Extra.dynamicui;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.pos_billingwala.Extra.FeatureEngine;
import com.pos_billingwala.Extra.SecurityPermissions;
import com.pos_billingwala.Extra.StaffRole;

/**
 * Visibility rule for Dynamic UI.
 * Visibility = defaultVisible AND feature (if set) AND soft permission (if set).
 * Backend {@link SecurityPermissions#runAuthorized} remains the hard gate for actions.
 */
public final class VisibilityRule {

    @Nullable
    public final String featureRequired;
    @Nullable
    public final String permissionRequired;
    @Nullable
    public final String businessRule;
    public final boolean defaultVisible;

    public VisibilityRule(@Nullable String featureRequired,
                          @Nullable String permissionRequired,
                          @Nullable String businessRule,
                          boolean defaultVisible) {
        this.featureRequired = featureRequired;
        this.permissionRequired = permissionRequired;
        this.businessRule = businessRule;
        this.defaultVisible = defaultVisible;
    }

    public static VisibilityRule feature(String feature) {
        return new VisibilityRule(feature, null, null, true);
    }

    public static VisibilityRule featureAndPermission(String feature, String permission) {
        return new VisibilityRule(feature, permission, null, true);
    }

    public static VisibilityRule permission(String permission) {
        return new VisibilityRule(null, permission, null, true);
    }

    public static VisibilityRule always() {
        return new VisibilityRule(null, null, null, true);
    }

    public static VisibilityRule never() {
        return new VisibilityRule(null, null, null, false);
    }

    public boolean evaluate(Context context) {
        if (!defaultVisible) {
            return false;
        }
        if (!TextUtils.isEmpty(featureRequired) && !FeatureEngine.isEnabled(context, featureRequired)) {
            return false;
        }
        if (!TextUtils.isEmpty(permissionRequired)) {
            return PermissionRule.softAllows(context, permissionRequired);
        }
        return true;
    }
}

/**
 * Soft action gate: feature must be on before navigation/action runs.
 */
final class ActionRule {
    @Nullable
    final String featureRequired;
    @Nullable
    final String permissionRequired;

    ActionRule(@Nullable String featureRequired, @Nullable String permissionRequired) {
        this.featureRequired = featureRequired;
        this.permissionRequired = permissionRequired;
    }

    boolean mayRun(Context context) {
        if (!TextUtils.isEmpty(featureRequired) && !FeatureEngine.isEnabled(context, featureRequired)) {
            return false;
        }
        return true;
    }
}

/**
 * Soft UI permission — hide rows waiters should not see; PIN entries still shown.
 */
final class PermissionRule {
    private PermissionRule() {
    }

    static boolean softAllows(Context context, @Nullable String permission) {
        if (TextUtils.isEmpty(permission)) {
            return true;
        }
        StaffRole role = SecurityPermissions.getStaffRole(context);
        if (role.isElevated()) {
            return true;
        }
        if (SecurityPermissions.BILL.equals(permission)) {
            return true;
        }
        if (SecurityPermissions.alwaysRequiresPin(permission)) {
            // Show entry; PIN collected on open (existing habit for reports / mess members)
            return role != StaffRole.WAITER
                    || SecurityPermissions.REPORTS.equals(permission);
        }
        if (role == StaffRole.WAITER) {
            return false;
        }
        if (role == StaffRole.CASHIER) {
            return SecurityPermissions.roleGrants(role, permission)
                    || SecurityPermissions.EXPENSE.equals(permission)
                    || SecurityPermissions.INVENTORY.equals(permission)
                    || SecurityPermissions.CLEAR_CART.equals(permission);
        }
        return SecurityPermissions.roleGrants(role, permission);
    }
}
