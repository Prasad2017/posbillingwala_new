package com.pos_billingwala.Extra;

import android.content.Context;

/**
 * Persists selected business type / template in SharedPreferences {@link Common#SHARED_PREF}.
 * Empty prefs → restaurant default (no behaviour change for existing installs).
 */
public final class BusinessSession {

    public static final String KEY_BUSINESS_TYPE = "businessType";
    public static final String KEY_TEMPLATE_ID = "businessTemplateId";
    public static final String KEY_TEMPLATE_NETWORK_STATUS = "businessTemplateNetworkStatus";

    private BusinessSession() {
    }

    /**
     * Ensures prefs have a default restaurant template. Additive only — never overwrites
     * a previously saved type/template.
     */
    public static void ensureDefaults(Context context) {
        if (context == null) {
            return;
        }
        String type = Common.getSavedUserData(context, KEY_BUSINESS_TYPE);
        if (type == null || type.trim().isEmpty()) {
            Common.saveUserData(context, KEY_BUSINESS_TYPE, BusinessTypes.DEFAULT);
        }
        String templateId = Common.getSavedUserData(context, KEY_TEMPLATE_ID);
        if (templateId == null || templateId.trim().isEmpty()) {
            Common.saveUserData(context, KEY_TEMPLATE_ID,
                    BusinessTemplateRegistry.TEMPLATE_RESTAURANT_DEFAULT);
        }
    }

    public static String getBusinessType(Context context) {
        if (context == null) {
            return BusinessTypes.DEFAULT;
        }
        return BusinessTypes.normalize(Common.getSavedUserData(context, KEY_BUSINESS_TYPE));
    }

    public static String getTemplateId(Context context) {
        if (context == null) {
            return BusinessTemplateRegistry.TEMPLATE_RESTAURANT_DEFAULT;
        }
        String id = Common.getSavedUserData(context, KEY_TEMPLATE_ID);
        if (id == null || id.trim().isEmpty()) {
            return BusinessTemplateRegistry.TEMPLATE_RESTAURANT_DEFAULT;
        }
        return id.trim();
    }

    public static void saveSelection(Context context, String businessType, String templateId) {
        if (context == null) {
            return;
        }
        Common.saveUserData(context, KEY_BUSINESS_TYPE, BusinessTypes.normalize(businessType));
        if (templateId != null && !templateId.trim().isEmpty()) {
            Common.saveUserData(context, KEY_TEMPLATE_ID, templateId.trim());
        } else {
            BusinessTemplate template = BusinessTemplateRegistry.forBusinessType(businessType);
            Common.saveUserData(context, KEY_TEMPLATE_ID, template.getId());
        }
        markTemplatePendingSync(context);
    }

    public static void markTemplatePendingSync(Context context) {
        if (context == null) {
            return;
        }
        Common.saveUserData(context, KEY_TEMPLATE_NETWORK_STATUS, "pending");
    }

    public static void markTemplateSynced(Context context) {
        if (context == null) {
            return;
        }
        Common.saveUserData(context, KEY_TEMPLATE_NETWORK_STATUS, "synced");
    }

    public static boolean isTemplatePendingSync(Context context) {
        if (context == null) {
            return false;
        }
        String status = Common.getSavedUserData(context, KEY_TEMPLATE_NETWORK_STATUS);
        // Missing key → treat as pending so first cloud sync publishes current selection.
        return status == null || status.trim().isEmpty()
                || !"synced".equalsIgnoreCase(status.trim());
    }

    /**
     * True when the user changed template locally and it has not uploaded yet.
     * Fetch Data must not overwrite that pending local selection.
     */
    public static boolean hasUnsyncedLocalTemplateChange(Context context) {
        if (context == null) {
            return false;
        }
        String status = Common.getSavedUserData(context, KEY_TEMPLATE_NETWORK_STATUS);
        return status != null && "pending".equalsIgnoreCase(status.trim());
    }

    public static int countPendingTemplateSync(Context context) {
        return isTemplatePendingSync(context) ? 1 : 0;
    }
}
