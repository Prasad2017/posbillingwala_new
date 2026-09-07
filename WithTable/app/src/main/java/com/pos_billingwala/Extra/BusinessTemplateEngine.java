package com.pos_billingwala.Extra;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Business Template Engine facade (Phases 01–02).
 * Resolves active template, applies built-ins / catalog types, summarizes capabilities.
 * Does not rewrite billing — Home visibility uses {@link FeatureEngine}.
 */
public final class BusinessTemplateEngine {

    private BusinessTemplateEngine() {
    }

    public static BusinessTemplate resolve(Context context) {
        BusinessSession.ensureDefaults(context);
        return BusinessTemplateRegistry.resolve(context);
    }

    /** Distinct built-in templates (may be fewer than catalog types that share a template). */
    public static List<BusinessTemplate> listBuiltIns() {
        List<BusinessTemplate> list = new ArrayList<>();
        list.add(BusinessTemplateRegistry.restaurantDefault());
        list.add(BusinessTemplateRegistry.barRestaurant());
        list.add(BusinessTemplateRegistry.messFocused());
        list.add(BusinessTemplateRegistry.retailDefault());
        list.add(BusinessTemplateRegistry.groceryDefault());
        list.add(BusinessTemplateRegistry.weightFreshDefault());
        list.add(BusinessTemplateRegistry.wholesaleDefault());
        list.add(BusinessTemplateRegistry.fashionDefault());
        list.add(BusinessTemplateRegistry.jewelleryDefault());
        list.add(BusinessTemplateRegistry.salonDefault());
        list.add(BusinessTemplateRegistry.bakeryDefault());
        return list;
    }

    /** Priority 1–2 catalog rows for the Settings picker. */
    public static List<BusinessTypeInfo> listSelectableTypes() {
        return BusinessTypeCatalog.selectable();
    }

    /**
     * Switch to a built-in template and clear any custom JSON overlay.
     */
    public static boolean applyBuiltIn(Context context, String templateId) {
        if (context == null || templateId == null || templateId.trim().isEmpty()) {
            return false;
        }
        if (!com.pos_billingwala.Extra.dynamic.ConfigApplyGuard.canApplyConfiguration(context)) {
            return false;
        }
        BusinessTemplate template = BusinessTemplateRegistry.findById(templateId.trim());
        if (template == null) {
            return false;
        }
        BusinessConfigStore.clearTemplateJson(context);
        BusinessSession.saveSelection(context, template.getBusinessType(), template.getId());
        invalidateDynamicSafe(context);
        return true;
    }

    /**
     * Apply a catalog business type (uses that type's default template id).
     */
    public static boolean applyBusinessType(Context context, String businessType) {
        if (context == null) {
            return false;
        }
        if (!com.pos_billingwala.Extra.dynamic.ConfigApplyGuard.canApplyConfiguration(context)) {
            return false;
        }
        BusinessTypeInfo info = BusinessTypeCatalog.get(businessType);
        BusinessConfigStore.clearTemplateJson(context);
        BusinessSession.saveSelection(context, info.getTypeId(), info.getDefaultTemplateId());
        invalidateDynamicSafe(context);
        return BusinessTemplateRegistry.findById(info.getDefaultTemplateId()) != null
                || BusinessTemplateRegistry.forBusinessType(info.getTypeId()) != null;
    }

    /**
     * Apply a custom JSON template (must parse). Persists JSON + session keys.
     */
    public static boolean applyJson(Context context, String json) {
        BusinessTemplate parsed = BusinessTemplateJson.parse(json);
        if (parsed == null || context == null) {
            return false;
        }
        if (!com.pos_billingwala.Extra.dynamic.ConfigApplyGuard.canApplyConfiguration(context)) {
            return false;
        }
        BusinessConfigStore.saveTemplateJson(context, BusinessTemplateJson.toJson(parsed));
        invalidateDynamicSafe(context);
        return true;
    }

    private static void invalidateDynamicSafe(Context context) {
        com.pos_billingwala.Extra.dynamic.PosConfigCache.invalidate(context);
        DynamicUiEngine.invalidate(context);
        com.pos_billingwala.Extra.dynamic.PosConfigCache.persist(context,
                com.pos_billingwala.Extra.dynamic.POSConfiguration.resolve(context));
    }

    /**
     * Apply cloud template selection (Fetch Data). Custom JSON wins when present and valid;
     * otherwise built-in id / business type. Marks synced when applied.
     * No-op when payload empty (keeps local restaurant default).
     */
    public static boolean applyFromCloud(Context context, String businessType,
                                         String templateId, String templateJson) {
        if (context == null) {
            return false;
        }
        String json = templateJson != null ? templateJson.trim() : "";
        if (!json.isEmpty()) {
            if (applyJson(context, json)) {
                BusinessSession.markTemplateSynced(context);
                return true;
            }
        }
        String id = templateId != null ? templateId.trim() : "";
        if (!id.isEmpty() && applyBuiltIn(context, id)) {
            BusinessSession.markTemplateSynced(context);
            return true;
        }
        String type = businessType != null ? businessType.trim() : "";
        if (!type.isEmpty() && applyBusinessType(context, type)) {
            BusinessSession.markTemplateSynced(context);
            return true;
        }
        return false;
    }

    public static int countPendingSync(Context context) {
        return BusinessSession.countPendingTemplateSync(context);
    }

    public static String summaryLine(Context context) {
        BusinessTypeInfo typeInfo = BusinessTypeCatalog.forCurrent(context);
        BusinessTemplate template = resolve(context);
        return typeInfo.getDisplayName()
                + " · "
                + typeInfo.readinessLabel()
                + "\n"
                + template.getId();
    }

    public static String featureSummary(Context context) {
        BusinessTypeInfo typeInfo = BusinessTypeCatalog.forCurrent(context);
        BusinessTemplate template = resolve(context);
        StringBuilder sb = new StringBuilder();
        sb.append(typeInfo.getEngineNotes());
        sb.append("\n");
        for (String flag : template.getEnabledFeatures()) {
            boolean on = FeatureEngine.isEnabled(context, flag);
            sb.append('\n');
            sb.append(on ? "• " : "○ ").append(flag);
            if (!on) {
                String reason = FeatureEngine.disabledReason(context, flag);
                if (reason != null && !reason.isEmpty()) {
                    sb.append(" (").append(reason).append(')');
                }
            }
        }
        return sb.toString();
    }
}
