package com.pos_billingwala.Extra.dynamicui;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.pos_billingwala.Extra.AppContexts;
import com.pos_billingwala.Extra.BusinessSession;
import com.pos_billingwala.Extra.BusinessTemplate;
import com.pos_billingwala.Extra.BusinessTemplateRegistry;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Extra.CrashApiDeviceLogging;
import com.pos_billingwala.Extra.FeatureEngine;
import com.pos_billingwala.Extra.SecurityPermissions;

import java.util.ArrayList;
import java.util.List;

/**
 * Central Dynamic UI resolution flow:
 * <pre>
 * Load Business → Template → Features → Business Config → Role → Permissions → Resolve UI
 * </pre>
 * Priority (base → restrictive overlay): System Default → Template → Business Config →
 * Features → Role → Permission. Visibility conflicts: AND (most restrictive wins).
 */
public final class UIResolverService {

    private static final String TAG = "UIResolverService";

    private UIResolverService() {
    }

    public static UIConfiguration resolve(Context context) {
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return UiFallback.defaultPos();
        }
        String key = UiConfigCache.fingerprint(ctx);
        UIConfiguration cached = UiConfigCache.getMemory(key);
        if (cached != null) {
            return cached;
        }
        try {
            UIConfiguration fresh = build(ctx);
            UiConfigCache.put(ctx, key, fresh);
            return fresh;
        } catch (Exception e) {
            Log.e(TAG, "UI resolve failed — using safe fallback", e);
            CrashApiDeviceLogging.logNonFatal(e, TAG);
            logFailure(ctx, e);
            UIConfiguration mem = UiConfigCache.getMemoryAny();
            if (mem != null) {
                return mem;
            }
            return UiFallback.defaultPos();
        }
    }

    /** Force rebuild after template / role / feature change. */
    public static UIConfiguration resolveFresh(Context context) {
        invalidate(context);
        return resolve(context);
    }

    public static void invalidate(@Nullable Context context) {
        UiConfigCache.invalidate(context);
    }

    private static UIConfiguration build(Context ctx) {
        BusinessSession.ensureDefaults(ctx);

        BusinessTemplate template = BusinessTemplateRegistry.resolve(ctx);
        String businessType = BusinessSession.getBusinessType(ctx);
        String templateCode = template != null ? template.getId() : "restaurant_default";
        String businessId = Common.getSavedUserData(ctx, "companyId");
        if (businessId == null || businessId.trim().isEmpty()) {
            businessId = Common.getSavedUserData(ctx, "userId");
        }
        if (businessId == null) {
            businessId = "";
        }

        List<String> templateFeatures = new ArrayList<>();
        if (template != null) {
            templateFeatures.addAll(template.getEnabledFeatures());
        }
        List<String> enabledFeatures = new ArrayList<>();
        for (String flag : templateFeatures) {
            if (FeatureEngine.isEnabled(ctx, flag)) {
                enabledFeatures.add(flag);
            }
        }

        // Role captured in fingerprint via SecurityPermissions — applied in VisibilityRule
        SecurityPermissions.getStaffRole(ctx);

        return new UIConfiguration(
                businessId,
                templateCode,
                businessType,
                enabledFeatures,
                DashboardResolver.resolve(ctx),
                NavigationResolver.resolve(ctx, templateFeatures),
                QuickActionResolver.resolve(ctx),
                BillingUIResolver.resolve(ctx, businessType),
                new FormConfiguration(ProductFormResolver.resolve(ctx, businessType)),
                SettingsUiResolver.resolve(ctx),
                ReportsUiResolver.resolve(ctx),
                ScreenVisibilityResolver.resolve(ctx),
                System.currentTimeMillis(),
                false,
                false
        );
    }

    private static void logFailure(Context ctx, Exception e) {
        try {
            String msg = "DynamicUI fail type=" + BusinessSession.getBusinessType(ctx)
                    + " template=" + BusinessSession.getTemplateId(ctx)
                    + " role=" + SecurityPermissions.getStaffRole(ctx).getId()
                    + " err=" + e.getMessage();
            Log.e(TAG, msg);
            CrashApiDeviceLogging.recordUserAction("dynamic_ui_resolve_fail");
        } catch (Exception ignored) {
        }
    }
}
