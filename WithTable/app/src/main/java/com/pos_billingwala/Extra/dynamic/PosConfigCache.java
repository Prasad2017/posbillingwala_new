package com.pos_billingwala.Extra.dynamic;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import com.pos_billingwala.Extra.AppContexts;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.CrashApiDeviceLogging;
import com.pos_billingwala.Extra.DynamicUiEngine;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Durable offline POS configuration snapshot per shop (Phase 12).
 * Key: Business/Org + Shop + configuration version metadata.
 */
public final class PosConfigCache {

    private static final String TAG = "PosConfigCache";
    private static final String PREF_SNAPSHOT = "posConfigSnapshotJson";
    private static final String PREF_KEY = "posConfigSnapshotKey";
    private static final String PREF_VERSION = "posConfigVersion";

    private PosConfigCache() {
    }

    public static long readOrBumpVersion(Context context, String cacheKey) {
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return 1L;
        }
        SharedPreferences pref = ctx.getSharedPreferences(Common.SHARED_PREF, 0);
        String storedKey = pref.getString(PREF_KEY, "");
        long version = 1L;
        try {
            version = Long.parseLong(pref.getString(PREF_VERSION, "1"));
        } catch (Exception ignored) {
            version = 1L;
        }
        if (cacheKey != null && !cacheKey.equals(storedKey)) {
            version = version + 1L;
            pref.edit()
                    .putString(PREF_KEY, cacheKey)
                    .putString(PREF_VERSION, String.valueOf(version))
                    .apply();
        } else if (storedKey == null || storedKey.isEmpty()) {
            pref.edit()
                    .putString(PREF_KEY, cacheKey != null ? cacheKey : "")
                    .putString(PREF_VERSION, String.valueOf(version))
                    .apply();
        }
        return version;
    }

    public static void persist(Context context, POSConfiguration config) {
        Context ctx = AppContexts.or(context);
        if (ctx == null || config == null) {
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("organizationId", config.organizationId);
            json.put("shopId", config.shopId);
            json.put("businessType", config.businessType);
            json.put("templateId", config.templateId);
            json.put("staffRole", config.staffRole);
            json.put("configVersion", config.configVersion);
            json.put("features", new JSONArray(config.enabledFeatures));
            json.put("modules", new JSONArray(config.enabledModules));
            json.put("resolvedAtMs", System.currentTimeMillis());
            ctx.getSharedPreferences(Common.SHARED_PREF, 0).edit()
                    .putString(PREF_SNAPSHOT, json.toString())
                    .putString(PREF_KEY, POSConfiguration.cacheKey(ctx))
                    .putString(PREF_VERSION, String.valueOf(config.configVersion))
                    .apply();
        } catch (Exception e) {
            Log.w(TAG, "persist failed", e);
            CrashApiDeviceLogging.logNonFatal(e, TAG);
        }
    }

    @Nullable
    public static JSONObject readSnapshot(Context context) {
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return null;
        }
        try {
            String raw = ctx.getSharedPreferences(Common.SHARED_PREF, 0).getString(PREF_SNAPSHOT, null);
            if (raw == null || raw.trim().isEmpty()) {
                return null;
            }
            return new JSONObject(raw);
        } catch (Exception e) {
            Log.w(TAG, "readSnapshot failed", e);
            return null;
        }
    }

    /**
     * Resolve live config when safe; otherwise reuse last snapshot metadata and memory UI.
     */
    public static POSConfiguration resolveSafe(Context context) {
        Context ctx = AppContexts.or(context);
        if (ConfigApplyGuard.canApplyConfiguration(ctx)) {
            POSConfiguration cfg = POSConfiguration.resolve(ctx);
            persist(ctx, cfg);
            return cfg;
        }
        // Busy: do not invalidate / re-apply; return current resolve without bump side effects
        return POSConfiguration.resolve(ctx);
    }

    public static void invalidate(Context context) {
        if (!ConfigApplyGuard.canApplyConfiguration(context)) {
            return;
        }
        DynamicUiEngine.invalidate(context);
        Context ctx = AppContexts.or(context);
        if (ctx != null) {
            ctx.getSharedPreferences(Common.SHARED_PREF, 0).edit()
                    .remove(PREF_SNAPSHOT)
                    .apply();
        }
    }

    public static String summaryLine(Context context) {
        JSONObject snap = readSnapshot(context);
        if (snap == null) {
            return "PosConfigCache: empty";
        }
        return "PosConfigCache: shop=" + snap.optString("shopId")
                + " v" + snap.optLong("configVersion")
                + " type=" + snap.optString("businessType")
                + " busy=" + ConfigApplyGuard.isBusy(context);
    }
}
