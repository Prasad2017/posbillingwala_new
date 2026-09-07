package com.pos_billingwala.Extra.dynamicui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Root resolved UI configuration for one business + role session.
 */
public final class UIConfiguration {
    public final String businessId;
    public final String templateCode;
    public final String businessType;
    public final List<String> enabledFeatures;
    public final DashboardConfiguration dashboardConfig;
    public final NavigationConfiguration navigationConfig;
    public final List<QuickActionConfiguration> quickActions;
    public final BillingUIConfig billingConfig;
    public final FormConfiguration productFormConfig;
    public final List<SectionConfiguration> settingsConfig;
    public final List<ScreenConfiguration> reportsConfig;
    public final List<ScreenConfiguration> screens;
    public final long resolvedAtMs;
    public final boolean fromFallback;
    public final boolean fromCache;

    public UIConfiguration(String businessId, String templateCode, String businessType,
                           List<String> enabledFeatures,
                           DashboardConfiguration dashboardConfig,
                           NavigationConfiguration navigationConfig,
                           List<QuickActionConfiguration> quickActions,
                           BillingUIConfig billingConfig,
                           FormConfiguration productFormConfig,
                           List<SectionConfiguration> settingsConfig,
                           List<ScreenConfiguration> reportsConfig,
                           List<ScreenConfiguration> screens,
                           long resolvedAtMs,
                           boolean fromFallback,
                           boolean fromCache) {
        this.businessId = businessId != null ? businessId : "";
        this.templateCode = templateCode != null ? templateCode : "";
        this.businessType = businessType != null ? businessType : "";
        this.enabledFeatures = enabledFeatures != null
                ? Collections.unmodifiableList(new ArrayList<>(enabledFeatures))
                : Collections.emptyList();
        this.dashboardConfig = dashboardConfig != null
                ? dashboardConfig
                : new DashboardConfiguration(null);
        this.navigationConfig = navigationConfig != null
                ? navigationConfig
                : new NavigationConfiguration(null);
        this.quickActions = quickActions != null
                ? Collections.unmodifiableList(new ArrayList<>(quickActions))
                : Collections.emptyList();
        this.billingConfig = billingConfig != null
                ? billingConfig
                : new BillingUIConfig(null, null);
        this.productFormConfig = productFormConfig != null
                ? productFormConfig
                : new FormConfiguration(null);
        this.settingsConfig = settingsConfig != null
                ? Collections.unmodifiableList(new ArrayList<>(settingsConfig))
                : Collections.emptyList();
        this.reportsConfig = reportsConfig != null
                ? Collections.unmodifiableList(new ArrayList<>(reportsConfig))
                : Collections.emptyList();
        this.screens = screens != null
                ? Collections.unmodifiableList(new ArrayList<>(screens))
                : Collections.emptyList();
        this.resolvedAtMs = resolvedAtMs;
        this.fromFallback = fromFallback;
        this.fromCache = fromCache;
    }

    public boolean isQuickActionVisible(String actionCode) {
        for (QuickActionConfiguration a : quickActions) {
            if (actionCode.equals(a.actionCode)) {
                return a.visible;
            }
        }
        return false;
    }

    public boolean isWidgetVisible(String widgetCode) {
        return dashboardConfig.isWidgetVisible(widgetCode);
    }

    public boolean isReportVisible(String reportCode) {
        for (ScreenConfiguration s : reportsConfig) {
            if (reportCode.equals(s.screenCode)) {
                return s.visible;
            }
        }
        return false;
    }

    public boolean isSettingsSectionVisible(String sectionCode) {
        for (SectionConfiguration s : settingsConfig) {
            if (sectionCode.equals(s.sectionCode)) {
                return s.visible;
            }
        }
        return false;
    }

    public boolean isScreenVisible(String screenCode) {
        for (ScreenConfiguration s : screens) {
            if (screenCode.equals(s.screenCode)) {
                return s.visible;
            }
        }
        return false;
    }

    public boolean isBillingFieldVisible(String fieldCode) {
        return billingConfig.isFieldVisible(fieldCode);
    }

    public boolean isBillingSectionVisible(String sectionCode) {
        return billingConfig.isSectionVisible(sectionCode);
    }

    public boolean isProductFieldVisible(String fieldCode) {
        return productFormConfig.productForm.isFieldVisible(fieldCode);
    }
}
