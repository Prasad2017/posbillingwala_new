package com.pos_billingwala.Extra.dynamicui;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Package-private config POJOs for Dynamic UI Engine. */
final class NavigationItemConfig {
    final String code;
    final String title;
    @Nullable
    final String icon;
    @Nullable
    final String featureRequired;
    @Nullable
    final String permissionRequired;
    final String target;
    final int order;
    final boolean visible;

    NavigationItemConfig(String code, String title, @Nullable String icon,
                         @Nullable String featureRequired, @Nullable String permissionRequired,
                         String target, int order, boolean visible) {
        this.code = code;
        this.title = title;
        this.icon = icon;
        this.featureRequired = featureRequired;
        this.permissionRequired = permissionRequired;
        this.target = target;
        this.order = order;
        this.visible = visible;
    }

    NavigationItemConfig withVisible(boolean v) {
        return new NavigationItemConfig(code, title, icon, featureRequired, permissionRequired,
                target, order, v);
    }
}

final class NavigationConfiguration {
    final List<NavigationItemConfig> items;

    NavigationConfiguration(List<NavigationItemConfig> items) {
        this.items = items != null
                ? Collections.unmodifiableList(new ArrayList<>(items))
                : Collections.emptyList();
    }

    List<NavigationItemConfig> visibleItems() {
        List<NavigationItemConfig> out = new ArrayList<>();
        for (NavigationItemConfig i : items) {
            if (i.visible) {
                out.add(i);
            }
        }
        return out;
    }
}

final class WidgetConfiguration {
    final String widgetCode;
    final String title;
    @Nullable
    final String icon;
    @Nullable
    final String featureRequired;
    @Nullable
    final String permissionRequired;
    final boolean visible;
    final int position;
    final int priority;
    final String refreshBehaviour;

    WidgetConfiguration(String widgetCode, String title, @Nullable String icon,
                        @Nullable String featureRequired, @Nullable String permissionRequired,
                        boolean visible, int position, int priority, String refreshBehaviour) {
        this.widgetCode = widgetCode;
        this.title = title;
        this.icon = icon;
        this.featureRequired = featureRequired;
        this.permissionRequired = permissionRequired;
        this.visible = visible;
        this.position = position;
        this.priority = priority;
        this.refreshBehaviour = refreshBehaviour != null ? refreshBehaviour : "on_resume";
    }

    WidgetConfiguration withVisible(boolean v) {
        return new WidgetConfiguration(widgetCode, title, icon, featureRequired, permissionRequired,
                v, position, priority, refreshBehaviour);
    }
}

final class DashboardConfiguration {
    final List<WidgetConfiguration> widgets;

    DashboardConfiguration(List<WidgetConfiguration> widgets) {
        this.widgets = widgets != null
                ? Collections.unmodifiableList(new ArrayList<>(widgets))
                : Collections.emptyList();
    }

    boolean isWidgetVisible(String code) {
        for (WidgetConfiguration w : widgets) {
            if (code.equals(w.widgetCode)) {
                return w.visible;
            }
        }
        return false;
    }
}

final class QuickActionConfiguration {
    final String actionCode;
    final String title;
    @Nullable
    final String icon;
    @Nullable
    final String featureRequired;
    @Nullable
    final String permissionRequired;
    final String target;
    final int order;
    final boolean visible;

    QuickActionConfiguration(String actionCode, String title, @Nullable String icon,
                             @Nullable String featureRequired, @Nullable String permissionRequired,
                             String target, int order, boolean visible) {
        this.actionCode = actionCode;
        this.title = title;
        this.icon = icon;
        this.featureRequired = featureRequired;
        this.permissionRequired = permissionRequired;
        this.target = target;
        this.order = order;
        this.visible = visible;
    }

    QuickActionConfiguration withVisible(boolean v) {
        return new QuickActionConfiguration(actionCode, title, icon, featureRequired,
                permissionRequired, target, order, v);
    }
}

final class BillingFieldConfig {
    final String fieldCode;
    final String title;
    final boolean visible;
    final int order;
    @Nullable
    final String featureRequired;

    BillingFieldConfig(String fieldCode, String title, boolean visible, int order,
                       @Nullable String featureRequired) {
        this.fieldCode = fieldCode;
        this.title = title;
        this.visible = visible;
        this.order = order;
        this.featureRequired = featureRequired;
    }
}

final class BillingSectionConfig {
    final String sectionCode;
    final String title;
    final boolean visible;
    final int order;
    final List<BillingFieldConfig> fields;

    BillingSectionConfig(String sectionCode, String title, boolean visible, int order,
                         List<BillingFieldConfig> fields) {
        this.sectionCode = sectionCode;
        this.title = title;
        this.visible = visible;
        this.order = order;
        this.fields = fields != null
                ? Collections.unmodifiableList(new ArrayList<>(fields))
                : Collections.emptyList();
    }
}

final class BillingUIConfig {
    final List<BillingSectionConfig> sections;
    final List<BillingFieldConfig> fields;

    BillingUIConfig(List<BillingSectionConfig> sections, List<BillingFieldConfig> fields) {
        this.sections = sections != null
                ? Collections.unmodifiableList(new ArrayList<>(sections))
                : Collections.emptyList();
        this.fields = fields != null
                ? Collections.unmodifiableList(new ArrayList<>(fields))
                : Collections.emptyList();
    }

    boolean isFieldVisible(String fieldCode) {
        for (BillingFieldConfig f : fields) {
            if (fieldCode.equals(f.fieldCode)) {
                return f.visible;
            }
        }
        for (BillingSectionConfig s : sections) {
            for (BillingFieldConfig f : s.fields) {
                if (fieldCode.equals(f.fieldCode)) {
                    return f.visible && s.visible;
                }
            }
        }
        return false;
    }

    boolean isSectionVisible(String sectionCode) {
        for (BillingSectionConfig s : sections) {
            if (sectionCode.equals(s.sectionCode)) {
                return s.visible;
            }
        }
        return false;
    }
}

final class ProductFieldConfig {
    final String fieldCode;
    final String title;
    final boolean visible;
    final boolean required;
    final int order;
    @Nullable
    final String featureRequired;

    ProductFieldConfig(String fieldCode, String title, boolean visible, boolean required,
                       int order, @Nullable String featureRequired) {
        this.fieldCode = fieldCode;
        this.title = title;
        this.visible = visible;
        this.required = required;
        this.order = order;
        this.featureRequired = featureRequired;
    }
}

final class ProductFormConfig {
    final List<ProductFieldConfig> fields;

    ProductFormConfig(List<ProductFieldConfig> fields) {
        this.fields = fields != null
                ? Collections.unmodifiableList(new ArrayList<>(fields))
                : Collections.emptyList();
    }

    boolean isFieldVisible(String fieldCode) {
        for (ProductFieldConfig f : fields) {
            if (fieldCode.equals(f.fieldCode)) {
                return f.visible;
            }
        }
        return false;
    }

    List<ProductFieldConfig> visibleFields() {
        List<ProductFieldConfig> out = new ArrayList<>();
        for (ProductFieldConfig f : fields) {
            if (f.visible) {
                out.add(f);
            }
        }
        return out;
    }
}

final class FormConfiguration {
    final ProductFormConfig productForm;

    FormConfiguration(ProductFormConfig productForm) {
        this.productForm = productForm != null ? productForm : new ProductFormConfig(null);
    }
}

final class SectionConfiguration {
    final String sectionCode;
    final String title;
    final boolean visible;
    final int order;
    @Nullable
    final String featureRequired;
    @Nullable
    final String permissionRequired;

    SectionConfiguration(String sectionCode, String title, boolean visible, int order,
                         @Nullable String featureRequired, @Nullable String permissionRequired) {
        this.sectionCode = sectionCode;
        this.title = title;
        this.visible = visible;
        this.order = order;
        this.featureRequired = featureRequired;
        this.permissionRequired = permissionRequired;
    }

    SectionConfiguration withVisible(boolean v) {
        return new SectionConfiguration(sectionCode, title, v, order, featureRequired, permissionRequired);
    }
}
