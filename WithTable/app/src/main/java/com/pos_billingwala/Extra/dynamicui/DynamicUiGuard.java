package com.pos_billingwala.Extra.dynamicui;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;

import com.pos_billingwala.Extra.LicenseModules;

/**
 * Apply Dynamic UI visibility to existing Views without rewriting screens.
 */
public final class DynamicUiGuard {

    private DynamicUiGuard() {
    }

    public static void setVisible(@Nullable View view, boolean visible) {
        LicenseModules.setVisible(view, visible);
    }

    public static void setVisibleByQuickAction(Context context, @Nullable View view, String actionCode) {
        UIConfiguration cfg = UIResolverService.resolve(context);
        setVisible(view, cfg.isQuickActionVisible(actionCode));
    }

    public static void setVisibleByWidget(Context context, @Nullable View view, String widgetCode) {
        UIConfiguration cfg = UIResolverService.resolve(context);
        setVisible(view, cfg.isWidgetVisible(widgetCode));
    }

    public static void setVisibleByReport(Context context, @Nullable View view, String reportCode) {
        UIConfiguration cfg = UIResolverService.resolve(context);
        setVisible(view, cfg.isReportVisible(reportCode));
    }

    public static void setVisibleBySettings(Context context, @Nullable View view, String sectionCode) {
        UIConfiguration cfg = UIResolverService.resolve(context);
        setVisible(view, cfg.isSettingsSectionVisible(sectionCode));
    }

    public static void setVisibleByScreen(Context context, @Nullable View view, String screenCode) {
        UIConfiguration cfg = UIResolverService.resolve(context);
        setVisible(view, cfg.isScreenVisible(screenCode));
    }
}
