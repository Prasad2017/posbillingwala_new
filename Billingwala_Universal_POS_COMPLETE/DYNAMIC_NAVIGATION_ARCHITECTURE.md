# DYNAMIC_NAVIGATION_ARCHITECTURE

## Principle

One navigation resolver for all businesses. Items appear/disappear by feature + soft permission.

## Classes

- `NavigationItemConfig` — code, title, feature, permission, target, order, visible  
- `NavigationConfiguration` — item list  
- `NavigationResolver` — builds base list, applies `NavigationGuard`  
- `NavigationGuard` — template feature membership ∩ `VisibilityRule`  
- `DynamicNavigationService` — `visibleCodes` / `isVisible`

## Example resolved sets

| Business | Typical visible codes |
|----------|----------------------|
| Restaurant | HOME, BILLING, TABLES, ORDERS, TAKE_AWAY, MESS*, PRODUCTS, REPORTS, SETTINGS |
| Fish / weight | HOME, BILLING, WEIGHT_BILLING, PRODUCTS, STOCK, REPORTS, SETTINGS |
| Salon | HOME, BILLING, APPOINTMENTS, SERVICES, PRODUCTS, REPORTS, SETTINGS |
| Clothing | HOME, BILLING, BARCODE, PRODUCTS, STOCK, REPORTS, SETTINGS |
| Cake | HOME, BILLING, CUSTOM_ORDERS, PRODUCTS, REPORTS, SETTINGS |

\* Mess only when template + licence enable `mess`.

## Runtime host

Still `MainActivity.loadFragment` — Dynamic UI does not invent a second drawer/bottom-nav graph. Codes map to existing fragment/activity targets.
