package com.pos_billingwala.Extra;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Local persistence for optional custom template JSON overlays.
 * Built-in selection still uses {@link BusinessSession} keys; JSON is only for
 * dealer/server-supplied or future synced templates.
 */
public final class BusinessConfigStore {

    public static final String KEY_TEMPLATE_JSON = "businessTemplateJson";
    public static final String KEY_TEMPLATE_VERSION = "businessTemplateVersion";

    private BusinessConfigStore() {
    }

    public static String getTemplateJson(Context context) {
        if (context == null) {
            return "";
        }
        String json = Common.getSavedUserData(context, KEY_TEMPLATE_JSON);
        return json != null ? json : "";
    }

    public static void saveTemplateJson(Context context, String json) {
        if (context == null) {
            return;
        }
        if (json == null || json.trim().isEmpty()) {
            clearTemplateJson(context);
            return;
        }
        BusinessTemplate parsed = BusinessTemplateJson.parse(json);
        if (parsed == null) {
            return;
        }
        Common.saveUserData(context, KEY_TEMPLATE_JSON, json.trim());
        Common.saveUserData(context, KEY_TEMPLATE_VERSION, String.valueOf(BusinessTemplateJson.VERSION));
        BusinessSession.saveSelection(context, parsed.getBusinessType(), parsed.getId());
    }

    public static void clearTemplateJson(Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences pref = context.getSharedPreferences(Common.SHARED_PREF, 0);
        pref.edit().remove(KEY_TEMPLATE_JSON).remove(KEY_TEMPLATE_VERSION).apply();
    }

    public static boolean hasCustomTemplate(Context context) {
        return BusinessTemplateJson.parse(getTemplateJson(context)) != null;
    }
}
