# DYNAMIC_UI_ARCHITECTURE

## Goal

One Billingwala POS app. UI changes by configuration — not by duplicating apps or screens per business.

```text
BUSINESS → TEMPLATE → FEATURES → BUSINESS CONFIG → ROLE → PERMISSIONS
        → DYNAMIC UI CONFIGURATION
        → Dashboard / Navigation / Quick Actions / Screens /
          Billing UI / Product Forms / Settings / Reports
```

## Facade

`WithTable/.../Extra/DynamicUiEngine.java`

| API | Role |
|-----|------|
| `resolve` / `resolveFresh` | Full `UIConfiguration` |
| `invalidate` | Clear memory + fingerprint cache |
| `isQuickActionVisible` / `applyQuickAction` | Home billing tiles |
| `isWidgetVisible` / `applyWidget` | Home KPIs / catalog cards |
| `isReportVisible` / `applyReport` | ReportsHub / ReportSetting |
| `isSettingsSectionVisible` / `applySettingsSection` | Settings rows |
| `isBillingFieldVisible` / `isProductFieldVisible` | CreatePos / forms |
| `visibleNavigationCodes` | Resolved nav codes |
| `moduleSummary` | Diagnostics |

## Package

`com.pos_billingwala.Extra.dynamicui`

| Class | Responsibility |
|-------|----------------|
| `UIResolverService` | Central resolve flow + fallback |
| `UiConfigCache` | Memory + SharedPreferences fingerprint |
| `UiFallback` | Safe default restaurant POS UI |
| `NavigationResolver` / `DynamicNavigationService` / `NavigationGuard` | Nav |
| `DashboardResolver` | Widgets |
| `QuickActionResolver` / `QuickActionGuard` | Quick actions |
| `BillingUIResolver` | Billing fields/sections |
| `ProductFormResolver` | Product form fields |
| `SettingsUiResolver` / `ReportsUiResolver` / `ScreenVisibilityResolver` | Menus |
| `VisibilityRule` / `PermissionRule` / `ActionRule` | Gates |
| `DynamicUiGuard` / `DynamicUiComponents` | Apply to Views |
| `UiCodes` | Canonical codes |
| `UIConfiguration` / `ScreenConfiguration` / models | Config graph |

## Priority (conflict handling)

1. System default (`UiFallback` / base lists)  
2. Business template (feature set)  
3. Business configuration (custom JSON via template engine)  
4. Enabled features (`FeatureEngine`)  
5. User role  
6. User permission (soft UI; hard gate remains `SecurityPermissions.runAuthorized`)

**Visibility:** AND across layers (most restrictive wins).  
**Labels/order:** higher overlay wins when explicitly set.

## Reused engines

Business Template · Feature · Universal Billing · Product · Inventory · Settings/Reports · Permissions · Printer

## Wired screens (gradual)

| Screen | Dynamic UI use |
|--------|----------------|
| `Home` | Quick actions + widgets (incl. combo card) |
| `ReportsHub` / `ReportSetting` | Report FeatureEngine gates |
| `UserSetting` | Inventory + scale sections |
| `CreatePos` | Barcode field visibility |
| Template / role change | `invalidate` cache |

## Offline

Cached fingerprint + last memory config. On resolve failure → memory → `UiFallback.defaultPos()`. Never blank / crash / block billing.
