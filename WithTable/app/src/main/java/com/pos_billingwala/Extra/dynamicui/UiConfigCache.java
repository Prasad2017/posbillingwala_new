package com.pos_billingwala.Extra.dynamicui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import com.pos_billingwala.Extra.AppContexts;
import com.pos_billingwala.Extra.BusinessSession;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.CrashApiDeviceLogging;
import com.pos_billingwala.Extra.SecurityPermissions;

/**
 * Memory + SharedPreferences cache for resolved UI configuration metadata.
 * Full object graph is memory-cached; disk stores fingerprint keys for offline reuse hints.
 */
final class UiConfigCache {

    private static final String TAG = "UiConfigCache";
    private static final String PREF_FINGERPRINT = "dynamicUiFingerprint";
    private static final String PREF_RESOLVED_AT = "dynamicUiResolvedAt";
    private static final String PREF_TEMPLATE = "dynamicUiCachedTemplate";
    private static final String PREF_TYPE = "dynamicUiCachedType";
    private static final String PREF_ROLE = "dynamicUiCachedRole";

    private static volatile UIConfiguration memory;
    private static volatile String memoryKey;

    private UiConfigCache() {
    }

    static synchronized void put(Context context, String key, UIConfiguration config) {
        memory = config;
        memoryKey = key;
        Context ctx = AppContexts.or(context);
        if (ctx == null || config == null) {
            return;
        }
        try {
            SharedPreferences pref = ctx.getSharedPreferences(Common.SHARED_PREF, 0);
            pref.edit()
                    .putString(PREF_FINGERPRINT, key)
                    .putString(PREF_RESOLVED_AT, String.valueOf(config.resolvedAtMs))
                    .putString(PREF_TEMPLATE, config.templateCode)
                    .putString(PREF_TYPE, config.businessType)
                    .putString(PREF_ROLE, SecurityPermissions.getStaffRole(ctx).getId())
                    .apply();
        } catch (Exception e) {
            Log.w(TAG, "disk cache write failed", e);
            CrashApiDeviceLogging.logNonFatal(e, TAG);
        }
    }

    @Nullable
    static synchronized UIConfiguration getMemory(String key) {
        if (key != null && key.equals(memoryKey) && memory != null) {
            return memory;
        }
        return null;
    }

    @Nullable
    static synchronized UIConfiguration getMemoryAny() {
        return memory;
    }

    static synchronized void invalidate() {
        memory = null;
        memoryKey = null;
    }

    static synchronized void invalidate(Context context) {
        invalidate();
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return;
        }
        try {
            ctx.getSharedPreferences(Common.SHARED_PREF, 0).edit()
                    .remove(PREF_FINGERPRINT)
                    .remove(PREF_RESOLVED_AT)
                    .apply();
        } catch (Exception ignored) {
        }
    }

    static String fingerprint(Context context) {
        Context ctx = AppContexts.or(context);
        String type = BusinessSession.getBusinessType(ctx);
        String template = BusinessSession.getTemplateId(ctx);
        String role = SecurityPermissions.getStaffRole(ctx).getId();
        String staff = SecurityPermissions.getActiveStaffId(ctx);
        return type + "|" + template + "|" + role + "|" + staff;
    }

    static boolean diskMatches(Context context, String key) {
        Context ctx = AppContexts.or(context);
        if (ctx == null || key == null) {
            return false;
        }
        String saved = Common.getSavedUserData(ctx, PREF_FINGERPRINT);
        return key.equals(saved);
    }
}
