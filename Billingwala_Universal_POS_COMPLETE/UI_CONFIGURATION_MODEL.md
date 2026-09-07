# UI_CONFIGURATION_MODEL

## Root: `UIConfiguration`

| Field | Meaning |
|-------|---------|
| `businessId` | Shop / user id |
| `templateCode` | e.g. `restaurant_default` |
| `businessType` | Normalized `BusinessTypes` |
| `enabledFeatures` | Runtime-on flags |
| `dashboardConfig` | Widgets |
| `navigationConfig` | Nav items |
| `quickActions` | Home tiles |
| `billingConfig` | Billing UI |
| `productFormConfig` | Product form |
| `settingsConfig` | Settings sections |
| `reportsConfig` | Report screens |
| `screens` | Screen visibility registry |
| `resolvedAtMs` | Resolve time |
| `fromFallback` / `fromCache` | Provenance |

## Related models

`ScreenConfiguration`, `NavigationItemConfig`, `WidgetConfiguration`, `QuickActionConfiguration`, `BillingFieldConfig`, `BillingSectionConfig`, `ProductFieldConfig`, `SectionConfiguration`, `VisibilityRule`

## Cache keys

Fingerprint: `businessType|templateId|staffRole|activeStaffId`  
Prefs: `dynamicUiFingerprint`, `dynamicUiResolvedAt`, `dynamicUiCachedTemplate`, …
