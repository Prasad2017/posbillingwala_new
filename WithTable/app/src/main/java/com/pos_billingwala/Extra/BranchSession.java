package com.pos_billingwala.Extra;

import android.content.ContentValues;
import android.content.Context;
import android.provider.Settings;

import com.pos_billingwala.Activity.MainActivity;

/**
 * Multi-branch session scope: organization → branch → device.
 * Single-branch customers: branchId defaults to userId (licence id); no config needed.
 */
public final class BranchSession {

    private BranchSession() {
    }

    public static void loadFromPreferences(Context context) {
        MainActivity.organizationId = Common.getSavedUserData(context, "organizationId");
        if (MainActivity.organizationId == null || MainActivity.organizationId.isEmpty()) {
            MainActivity.organizationId = Common.getSavedUserData(context, "ownerId");
        }
        MainActivity.branchId = Common.getSavedUserData(context, "branchId");
        if (MainActivity.branchId == null || MainActivity.branchId.isEmpty()) {
            MainActivity.branchId = Common.getSavedUserData(context, "userId");
        }
        MainActivity.branchLabel = Common.getSavedUserData(context, "branchLabel");
        MainActivity.deviceId = Common.getSavedUserData(context, "deviceId");
        if (MainActivity.deviceId == null || MainActivity.deviceId.isEmpty()) {
            MainActivity.deviceId = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
    }

    public static void applyScope(ContentValues values) {
        putIfPresent(values, "organizationId", MainActivity.organizationId);
        putIfPresent(values, "branchId", effectiveBranchId());
        putIfPresent(values, "deviceId", MainActivity.deviceId);
    }

    public static String effectiveBranchId() {
        if (MainActivity.branchId != null && !MainActivity.branchId.isEmpty()) {
            return MainActivity.branchId;
        }
        return MainActivity.userId;
    }

    public static String effectiveOrganizationId() {
        if (MainActivity.organizationId != null && !MainActivity.organizationId.isEmpty()) {
            return MainActivity.organizationId;
        }
        return MainActivity.ownerId;
    }

    private static void putIfPresent(ContentValues values, String key, String value) {
        if (value != null && !value.isEmpty()) {
            values.put(key, value);
        }
    }
}
