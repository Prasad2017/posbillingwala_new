package com.pos_billingwala.Extra.dynamicui;

import androidx.annotation.Nullable;

/**
 * Declarative screen visibility entry for Dynamic UI.
 */
public final class ScreenConfiguration {
    public final String screenCode;
    public final String title;
    @Nullable
    public final String icon;
    @Nullable
    public final String featureRequired;
    @Nullable
    public final String permissionRequired;
    @Nullable
    public final String businessRule;
    public final boolean visible;
    public final int order;

    public ScreenConfiguration(String screenCode, String title, @Nullable String icon,
                               @Nullable String featureRequired, @Nullable String permissionRequired,
                               @Nullable String businessRule, boolean visible, int order) {
        this.screenCode = screenCode;
        this.title = title;
        this.icon = icon;
        this.featureRequired = featureRequired;
        this.permissionRequired = permissionRequired;
        this.businessRule = businessRule;
        this.visible = visible;
        this.order = order;
    }

    public ScreenConfiguration withVisible(boolean v) {
        return new ScreenConfiguration(screenCode, title, icon, featureRequired, permissionRequired,
                businessRule, v, order);
    }
}
