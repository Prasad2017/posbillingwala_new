package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Model.StaffUserResponse;
import com.pos_billingwala.R;

/**
 * Security / permissions facade.
 * Phase 22: licence modules + Bearer tokens + branch scope + device staff RBAC + local staff roster.
 * <p>
 * Default role {@link StaffRole#OWNER} → same as before (no new prompts for billing).
 * Restricted roles can still override with shop {@code reportPin}.
 * Optional {@code staff_user} roster: switch named staff (personal PIN) without changing Owner shop PIN.
 */
public final class SecurityPermissions {

    public static final String PREF_STAFF_ROLE = "staffRole";
    public static final String PREF_ACTIVE_STAFF_ID = "activeStaffId";
    public static final String PREF_ACTIVE_STAFF_NAME = "activeStaffName";

    public static final String BILL = "bill";
    public static final String CLEAR_CART = "clear_cart";
    public static final String REPORTS = "reports";
    public static final String MASTER_DATA = "master_data";
    public static final String SHOP_SETTINGS = "shop_settings";
    public static final String BUSINESS_TEMPLATE = "business_template";
    public static final String PRINTER_SETTINGS = "printer_settings";
    public static final String INVENTORY = "inventory";
    public static final String EXPENSE = "expense";
    public static final String SYNC_FETCH = "sync_fetch";
    public static final String CHANGE_APP_PIN = "change_app_pin";
    public static final String SET_STAFF_ROLE = "set_staff_role";
    public static final String MESS_MEMBERS = "mess_members";

    public interface AuthCallback {
        void onAuthorized();
    }

    private SecurityPermissions() {
    }

    public static StaffRole getStaffRole(Context context) {
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return StaffRole.OWNER;
        }
        return StaffRole.fromId(Common.getSavedUserData(ctx, PREF_STAFF_ROLE));
    }

    public static void setStaffRole(Context context, StaffRole role) {
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return;
        }
        Common.saveUserData(ctx, PREF_STAFF_ROLE, role != null ? role.getId() : StaffRole.OWNER.getId());
        // Quick device role is not tied to a roster row.
        Common.saveUserData(ctx, PREF_ACTIVE_STAFF_ID, "");
        Common.saveUserData(ctx, PREF_ACTIVE_STAFF_NAME, "");
        DynamicUiEngine.invalidate(ctx);
    }

    public static String getActiveStaffId(Context context) {
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return "";
        }
        String id = Common.getSavedUserData(ctx, PREF_ACTIVE_STAFF_ID);
        return id != null ? id : "";
    }

    public static String getActiveStaffName(Context context) {
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return "";
        }
        String name = Common.getSavedUserData(ctx, PREF_ACTIVE_STAFF_NAME);
        return name != null ? name : "";
    }

    public static void activateStaff(Context context, StaffUserResponse staff) {
        Context ctx = AppContexts.or(context);
        if (ctx == null || staff == null) {
            return;
        }
        StaffRole role = StaffRole.fromId(staff.getStaffRole());
        Common.saveUserData(ctx, PREF_STAFF_ROLE, role.getId());
        Common.saveUserData(ctx, PREF_ACTIVE_STAFF_ID,
                staff.getStaffId() != null ? staff.getStaffId() : "");
        Common.saveUserData(ctx, PREF_ACTIVE_STAFF_NAME,
                staff.getStaffName() != null ? staff.getStaffName() : "");
        DynamicUiEngine.invalidate(ctx);
    }

    public static boolean matchesStaffPin(StaffUserResponse staff, @Nullable CharSequence entered) {
        if (staff == null) {
            return false;
        }
        return StaffPinHasher.matches(staff.getStaffPin(), entered);
    }

    public static void clearStaffRoleOnLogout(Context context) {
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return;
        }
        Common.saveUserData(ctx, PREF_STAFF_ROLE, StaffRole.OWNER.getId());
        Common.saveUserData(ctx, PREF_ACTIVE_STAFF_ID, "");
        Common.saveUserData(ctx, PREF_ACTIVE_STAFF_NAME, "");
    }

    public static boolean hasLicenceModule(Context context, String featureFlag) {
        return FeatureEngine.isEnabled(context, featureFlag);
    }

    /**
     * Whether the current device role grants the permission without a PIN.
     * {@link #REPORTS}, {@link #CHANGE_APP_PIN}, {@link #SET_STAFF_ROLE}, {@link #MESS_MEMBERS}
     * always require PIN (existing shop habit).
     */
    public static boolean canWithoutPin(Context context, String permission) {
        if (permission == null) {
            return false;
        }
        if (alwaysRequiresPin(permission)) {
            return false;
        }
        return roleGrants(getStaffRole(context), permission);
    }

    public static boolean roleGrants(StaffRole role, String permission) {
        if (role == null) {
            role = StaffRole.OWNER;
        }
        if (permission == null) {
            return false;
        }
        if (role.isElevated()) {
            return true;
        }
        switch (permission) {
            case BILL:
                return true;
            case CLEAR_CART:
                return role == StaffRole.CASHIER;
            case EXPENSE:
                return role == StaffRole.CASHIER;
            case INVENTORY:
                return role == StaffRole.CASHIER;
            case REPORTS:
            case MASTER_DATA:
            case SHOP_SETTINGS:
            case BUSINESS_TEMPLATE:
            case PRINTER_SETTINGS:
            case SYNC_FETCH:
            case CHANGE_APP_PIN:
            case SET_STAFF_ROLE:
            case MESS_MEMBERS:
                return false;
            default:
                return false;
        }
    }

    public static boolean alwaysRequiresPin(String permission) {
        return REPORTS.equals(permission)
                || CHANGE_APP_PIN.equals(permission)
                || SET_STAFF_ROLE.equals(permission)
                || MESS_MEMBERS.equals(permission);
    }

    public static String expectedReportPin(Context context) {
        if (MainActivity.reportPin != null && !MainActivity.reportPin.trim().isEmpty()) {
            return MainActivity.reportPin.trim();
        }
        Context ctx = AppContexts.or(context);
        if (ctx != null) {
            String saved = Common.getSavedUserData(ctx, "reportPin");
            if (saved != null && !saved.trim().isEmpty()) {
                return saved.trim();
            }
        }
        return "9082";
    }

    public static boolean matchesReportPin(Context context, @Nullable CharSequence entered) {
        if (entered == null) {
            return false;
        }
        return expectedReportPin(context).equalsIgnoreCase(entered.toString().trim());
    }

    /**
     * Run {@code onOk} immediately if role allows; otherwise prompt for report PIN (manager override).
     */
    public static void runAuthorized(Activity activity, String permission,
                                     @Nullable CharSequence title, AuthCallback onOk) {
        if (activity == null || onOk == null) {
            return;
        }
        if (canWithoutPin(activity, permission)) {
            onOk.onAuthorized();
            return;
        }
        promptReportPin(activity, title, onOk);
    }

    public static void promptReportPin(Activity activity, @Nullable CharSequence title, AuthCallback onOk) {
        if (activity == null || onOk == null) {
            return;
        }
        View content = LayoutInflater.from(activity).inflate(R.layout.report_password_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextView continueToReport = content.findViewById(R.id.continueToReport);
        TextView dismissReport = content.findViewById(R.id.dismissReport);
        TextInputEditText reportPin = content.findViewById(R.id.reportPin);
        TextView details = content.findViewById(R.id.details);
        if (details != null && title != null && title.length() > 0) {
            details.setText(title);
        }

        dismissReport.setOnClickListener(v -> sheet.dismiss());
        continueToReport.setOnClickListener(v -> {
            CharSequence entered = reportPin.getText();
            if (matchesReportPin(activity, entered)) {
                sheet.dismiss();
                onOk.onAuthorized();
            } else {
                reportPin.requestFocus();
                reportPin.setError(activity.getString(R.string.toast_enter_correct_pin));
            }
        });
    }

    public static String sessionLabel(Context context) {
        StaffRole role = getStaffRole(context);
        String name = getActiveStaffName(context);
        if (name != null && !name.trim().isEmpty()) {
            return name.trim() + " (" + role.getLabel() + ")";
        }
        return role.getLabel();
    }

    public static String authModel() {
        return "Licence key + MPIN → Bearer api_tokens (AuthTokens)";
    }

    public static String scopeModel() {
        return "BranchSession + LicenceScopeGuard (org/branch/device)";
    }

    public static String staffRbacStatus(Context context) {
        return "Session " + sessionLabel(context)
                + " · reportPin override · staff_user roster (cloud sync)"
                + " · staff PIN sha256 (legacy plaintext still verifies)";
    }

    public static String moduleSummary(Context context) {
        return authModel()
                + "\n" + scopeModel()
                + "\nFeature resolve: template ∩ licence ∩ company"
                + "\nStaff RBAC: " + staffRbacStatus(context)
                + "\nMess: " + (MessModule.isEnabled(context) ? "on" : "off");
    }
}
