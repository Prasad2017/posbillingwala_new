# UI_MIGRATION_PLAN

## Strategy

```text
Existing Screen → Feature/Dynamic guard → Connect UIConfiguration → Make dynamic → Regression test
```

Do **not** replace all screens in one commit.

## Completed this phase

1. Analysis (`DYNAMIC_UI_ANALYSIS.md`)  
2. Engine + cache + resolvers  
3. Home quick actions + widgets (+ combo gate)  
4. ReportsHub / ReportSetting FeatureEngine alignment  
5. UserSetting inventory + scale  
6. CreatePos barcode field  
7. Invalidate on template / role change  
8. Diagnostics via `UniversalPosModules`

## Next gradual steps (optional)

1. Bind remaining Home catalog widgets from config only  
2. `AddProduct` / `UpdateProduct` optional field rows via `isProductFieldVisible`  
3. Settings group dividers refresh after dynamic hide  
4. Soft-hide waiter Settings elevated rows via section codes  
5. Wire ACTIVE_TABLES / APPOINTMENTS widgets when KPI queries exist  

## Rollback

Remove Dynamic UI calls and restore prior `FeatureEngine`/`LicenseModules` lines; engines remain additive.
