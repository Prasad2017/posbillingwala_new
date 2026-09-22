package com.pos_billingwala.Extra;

import android.app.Activity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.pos_billingwala.Model.LoginResponse;

public class UserManagementGuard {

    public static boolean isEnabled(LoginResponse body) {
        if (body == null) {
            return false;
        }
        String flag = body.getUserManagementEnabled();
        return "1".equals(flag) || "true".equalsIgnoreCase(flag);
    }

    /**
     * WithTable has no staff PIN UI. If UM is on, stop login and tell the shop to use Flutter POS.
     * @return true if login should abort
     */
    public static boolean blockIfEnabled(Activity activity, LoginResponse body) {
        if (!isEnabled(body)) {
            return false;
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle("User Management")
                .setMessage("This licence uses staff PIN login. Open the new POS Billingwala app on this device. This older POS stays for shops with User Management off.")
                .setPositiveButton(android.R.string.ok, null)
                .show();
        return true;
    }
}
