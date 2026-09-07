# DYNAMIC_DASHBOARD_ARCHITECTURE

## Widgets (`UiCodes.W_*`)

| Code | Feature gate | Home wiring |
|------|--------------|-------------|
| TOTAL_SALES | `total_sale_data` | KPI card |
| TODAY_SALES | `today_sale_data` | KPI card |
| TOTAL_PRODUCTS | — | Catalog count (always) |
| SUBCATEGORIES | — | Catalog count |
| COMBOS | `combos` | Combo card (now gated) |
| LOW_STOCK / TOTAL_STOCK | `inventory` | Config ready |
| ACTIVE_TABLES / ACTIVE_ORDERS | `tables` / `dine_in` | Config ready |
| APPOINTMENTS / TODAY_APPOINTMENTS / TOTAL_SERVICES | `appointments` | Config ready |
| PENDING_CUSTOM_ORDERS | `custom_orders` | Config ready |
| TOTAL_BILLS / PENDING_REPAIRS / RENTAL_ITEMS | reserved | Hidden until data wired |

## Resolver

`DashboardResolver` → `WidgetConfiguration` (code, title, feature, permission, position, priority, refreshBehaviour).

## Refresh

Default `on_resume` — Home already refreshes counts on resume / swipe. Avoid full UI rebuild; only `setVisible` + text updates.
