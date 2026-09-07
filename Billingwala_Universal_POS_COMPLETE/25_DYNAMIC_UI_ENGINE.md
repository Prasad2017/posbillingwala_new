# 25 Dynamic UI Engine

## Entry
Facade: `Extra/DynamicUiEngine.java`  
Package: `Extra/dynamicui/*`  
Docs: `DYNAMIC_UI_*.md`, `UI_*.md`

## Resolve flow
Business → Template → Features → Config → Role → Permissions → `UIResolverService` → render via existing Views

## Wired
Home (quick actions + widgets) · ReportsHub/ReportSetting · UserSetting inventory/scale · CreatePos barcode · cache invalidate on template/role change · `UniversalPosModules` diagnostics

## Rule
One app · one UI engine · no per-business duplicate screens · soft UI hide + hard `SecurityPermissions` / backend
