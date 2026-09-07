package com.pos_billingwala.Extra.dynamic;

import android.content.Context;

import androidx.annotation.Nullable;

import com.pos_billingwala.Extra.AppContexts;
import com.pos_billingwala.Extra.BranchSession;
import com.pos_billingwala.Extra.BusinessSession;
import com.pos_billingwala.Extra.BusinessTemplate;
import com.pos_billingwala.Extra.BusinessTemplateRegistry;
import com.pos_billingwala.Extra.DynamicUiEngine;
import com.pos_billingwala.Extra.SecurityPermissions;
import com.pos_billingwala.Extra.dynamicui.UIConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central POS configuration aggregate for Dynamic pack Phase 02.
 * Shop → Template → Modules → Features → Permissions → UI.
 */
public final class POSConfiguration {

    public final String organizationId;
    public final String shopId;
    public final String businessType;
    public final String templateId;
    public final String staffRole;
    public final long configVersion;
    public final List<String> enabledFeatures;
    public final List<String> enabledModules;
    public final UIConfiguration ui;
    public final boolean fromFallback;

    private POSConfiguration(String organizationId, String shopId, String businessType,
                             String templateId, String staffRole, long configVersion,
                             List<String> enabledFeatures, List<String> enabledModules,
                             UIConfiguration ui, boolean fromFallback) {
        this.organizationId = organizationId != null ? organizationId : "";
        this.shopId = shopId != null ? shopId : "";
        this.businessType = businessType != null ? businessType : "";
        this.templateId = templateId != null ? templateId : "";
        this.staffRole = staffRole != null ? staffRole : "";
        this.configVersion = configVersion;
        this.enabledFeatures = enabledFeatures != null
                ? Collections.unmodifiableList(new ArrayList<>(enabledFeatures))
                : Collections.emptyList();
        this.enabledModules = enabledModules != null
                ? Collections.unmodifiableList(new ArrayList<>(enabledModules))
                : Collections.emptyList();
        this.ui = ui;
        this.fromFallback = fromFallback;
    }

    public static POSConfiguration resolve(Context context) {
        Context ctx = AppContexts.or(context);
        UIConfiguration ui = DynamicUiEngine.resolve(ctx);
        BusinessTemplate template = BusinessTemplateRegistry.resolve(ctx);
        long version = PosConfigCache.readOrBumpVersion(ctx, cacheKey(ctx));
        return new POSConfiguration(
                BranchSession.effectiveOrganizationId(),
                BranchSession.effectiveBranchId(),
                BusinessSession.getBusinessType(ctx),
                template != null ? template.getId() : BusinessSession.getTemplateId(ctx),
                SecurityPermissions.getStaffRole(ctx).getId(),
                version,
                ui != null ? ui.enabledFeatures : Collections.emptyList(),
                ModuleRegistry.enabledModuleCodes(ctx),
                ui,
                ui != null && ui.fromFallback);
    }

    public static String cacheKey(Context context) {
        Context ctx = AppContexts.or(context);
        return BranchSession.effectiveOrganizationId()
                + "|" + BranchSession.effectiveBranchId()
                + "|" + BusinessSession.getBusinessType(ctx)
                + "|" + BusinessSession.getTemplateId(ctx)
                + "|" + SecurityPermissions.getStaffRole(ctx).getId();
    }

    public String summaryLine() {
        return "POS cfg v" + configVersion
                + " · " + businessType
                + " · shop=" + shopId
                + " · modules=" + enabledModules.size()
                + " · features=" + enabledFeatures.size()
                + (fromFallback ? " · FALLBACK" : "");
    }

    @Nullable
    public UIConfiguration uiOrNull() {
        return ui;
    }
}
