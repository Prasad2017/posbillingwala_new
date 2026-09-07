package com.pos_billingwala.Extra;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;

import com.pos_billingwala.Extra.dynamicui.DynamicNavigationService;
import com.pos_billingwala.Extra.dynamicui.DynamicUiComponents;
import com.pos_billingwala.Extra.dynamicui.DynamicUiGuard;
import com.pos_billingwala.Extra.dynamicui.UIConfiguration;
import com.pos_billingwala.Extra.dynamicui.UIResolverService;
import com.pos_billingwala.Extra.dynamicui.UiCodes;

/**
 * Dynamic UI Engine facade.
 * One app / one UI engine — template + features + role + permissions decide what to show.
 * Reuses FeatureEngine, BusinessTemplateEngine, SecurityPermissions, UniversalBillingEngine.
 */
public final class DynamicUiEngine {

    private DynamicUiEngine() {
    }

    public static UIConfiguration resolve(Context context) {
        return UIResolverService.resolve(context);
    }

    public static UIConfiguration resolveFresh(Context context) {
        return UIResolverService.resolveFresh(context);
    }

    public static void invalidate(@Nullable Context context) {
        UIResolverService.invalidate(context);
    }

    public static boolean isQuickActionVisible(Context context, String actionCode) {
        return resolve(context).isQuickActionVisible(actionCode);
    }

    public static boolean isWidgetVisible(Context context, String widgetCode) {
        return resolve(context).isWidgetVisible(widgetCode);
    }

    public static boolean isReportVisible(Context context, String reportCode) {
        return resolve(context).isReportVisible(reportCode);
    }

    public static boolean isSettingsSectionVisible(Context context, String sectionCode) {
        return resolve(context).isSettingsSectionVisible(sectionCode);
    }

    public static boolean isScreenVisible(Context context, String screenCode) {
        return resolve(context).isScreenVisible(screenCode);
    }

    public static boolean isBillingFieldVisible(Context context, String fieldCode) {
        return DynamicUiComponents.shouldShowBillingField(context, fieldCode);
    }

    public static boolean isProductFieldVisible(Context context, String fieldCode) {
        return DynamicUiComponents.shouldShowProductField(context, fieldCode);
    }

    public static void applyQuickAction(Context context, @Nullable View view, String actionCode) {
        DynamicUiGuard.setVisibleByQuickAction(context, view, actionCode);
    }

    public static void applyWidget(Context context, @Nullable View view, String widgetCode) {
        DynamicUiGuard.setVisibleByWidget(context, view, widgetCode);
    }

    public static void applyReport(Context context, @Nullable View view, String reportCode) {
        DynamicUiGuard.setVisibleByReport(context, view, reportCode);
    }

    public static void applySettingsSection(Context context, @Nullable View view, String sectionCode) {
        DynamicUiGuard.setVisibleBySettings(context, view, sectionCode);
    }

    public static void applyScreen(Context context, @Nullable View view, String screenCode) {
        DynamicUiGuard.setVisibleByScreen(context, view, screenCode);
    }

    public static java.util.List<String> visibleNavigationCodes(Context context) {
        return DynamicNavigationService.visibleCodes(context);
    }

    public static String moduleSummary(Context context) {
        UIConfiguration cfg = resolve(context);
        StringBuilder sb = new StringBuilder();
        sb.append("Dynamic UI Engine\n");
        sb.append("Type: ").append(cfg.businessType)
                .append(" · Template: ").append(cfg.templateCode).append('\n');
        sb.append("Fallback: ").append(cfg.fromFallback)
                .append(" · Cache: ").append(cfg.fromCache).append('\n');
        sb.append("Quick actions: ")
                .append(cfg.isQuickActionVisible(UiCodes.QA_FAST_BILLING) ? "fast " : "")
                .append(cfg.isQuickActionVisible(UiCodes.QA_DINE_IN) ? "dine " : "")
                .append(cfg.isQuickActionVisible(UiCodes.QA_TAKE_AWAY) ? "takeaway " : "")
                .append(cfg.isQuickActionVisible(UiCodes.QA_MESS) ? "mess " : "")
                .append(cfg.isQuickActionVisible(UiCodes.QA_WEIGHT_BILLING) ? "weight " : "")
                .append(cfg.isQuickActionVisible(UiCodes.QA_APPOINTMENTS) ? "appt " : "")
                .append(cfg.isQuickActionVisible(UiCodes.QA_CUSTOM_ORDER) ? "custom " : "")
                .append('\n');
        sb.append("Widgets: today=")
                .append(cfg.isWidgetVisible(UiCodes.W_TODAY_SALES))
                .append(" total=")
                .append(cfg.isWidgetVisible(UiCodes.W_TOTAL_SALES))
                .append(" combos=")
                .append(cfg.isWidgetVisible(UiCodes.W_COMBOS))
                .append('\n');
        sb.append("Nav: ").append(DynamicNavigationService.visibleCodes(context)).append('\n');
        sb.append("Reports FeatureEngine-gated · Billing/Product fields via resolvers");
        return sb.toString();
    }
}
