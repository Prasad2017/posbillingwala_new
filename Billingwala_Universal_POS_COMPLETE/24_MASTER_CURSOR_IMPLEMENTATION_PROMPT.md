# 24 Master Cursor Implementation Prompt

Upgrade the existing Billingwala project safely. Use configuration-driven architecture, reusable engines, feature flags, role permissions and offline-safe behavior. Analyze existing code first and do not break current billing.

## Order (completed foundation)
00 → 01 → 03 → 02 → 04 → 05 → 07 → 06 → 15 → 08–14 (live modules) → 16–23 → 24 (this)

## Always
1. Read the phase `.md` + existing `Extra/*` facade
2. Additive only — no DROP, no wire-string renames
3. Compile WithTable after changes
4. Restaurant/mess default behaviour must stay identical when template = restaurant_default

## Engines map (`WithTable/.../Extra/`)
| Phase | Class |
|-------|--------|
| 01–03 | BusinessTemplate*, FeatureEngine, FeatureFlags |
| 04 | BillingMode, UniversalBillingEngine |
| 05–07 | RestaurantFoodModule, BarRestaurantModule, MessModule |
| 08–12 | WeightFresh, RetailGrocery, FashionJewellery, SalonAppointment, CakeBakery |
| 13–16 | ProductServiceEngine, ImportExportEngine, InventoryStockEngine, UniversalPrinterEngine |
| 17–22 | AdminDealerPlatform, CrashApiDeviceLogging, DynamicSettingsReports, DatabaseApiArchitecture, OfflineSyncArchitecture, SecurityPermissions |

## Code complete vs deploy

**Pack code is 100%** for Priority 1–2 verticals + Admin/Dealer/Owner template + Universal sync.

**Live deploy: done** — `p27`–`p32` / `server_upgrade_all.sql` + POS/Owner/Admin/Dealer PHP + `business_template_ops.php`.

**Remaining:** smoke 1–26 in `23_TESTING_AND_MIGRATION.md`.

Optional deferred: proprietary scale vendor SDK only.

## Next product work (priority)
1. ~~Barcode search in CreatePos when `BARCODE` on~~ **done**
2. ~~Weight keypad when `WEIGHT_SCALE` on~~ **done** (scale SDK later)
3. ~~BOT split print when BOT on~~ **done** (optional dedicated BOT Bluetooth)
4. ~~Staff RBAC (22)~~ **done** (device Owner/Manager/Cashier/Waiter + reportPin)
5. ~~POS catalog Excel (14)~~ **done** (local CSV on POS; cloud Excel stays Owner/Dealer/Admin)
6. ~~Fashion variants / Salon / Bakery~~ **done** (full calendar/photos later)
7. ~~Dedicated BOT Bluetooth address~~ **done** (fallback = KOT MAC)
8. ~~Multi-user staff table~~ **done** (local `staff_user` + appointment assign)
9. ~~Camera barcode~~ **done** (CreatePos QR icon when BARCODE on)
10. ~~Salon day calendar~~ **done** (Master Data appointments row + long-press Products)
11. ~~Bakery reference photo~~ **done** (local file + line label)
12. ~~Week-grid calendar~~ **done**
13. ~~Deposit ledger~~ **done** (`custom_order_deposit`)
14. ~~Bluetooth scale read~~ **done** (SPP ASCII + keypad fallback)
15. ~~Appointment cloud sync upload~~ **done** (client → `insertServiceAppointment.php`; mark synced only on status=1)
16. ~~Wholesale price tiers~~ **done** (`product_price_tier` + CreatePos picker)
17. ~~Bakery deposit cloud sync~~ **done** (p28 + PHP)
18. ~~Print custom-order photo on bill~~ **done** (2″/3″ bitmap under lines)
19. ~~Wholesale price tier cloud sync~~ **done** (p29 + PHP; DB v36)
20. ~~Fashion variant cloud sync~~ **done** (p30 + PHP; DB v37)
21. ~~Business template cloud sync~~ **done** (p31 + PHP; prefs network status)
22. ~~Staff roster cloud sync~~ **done** (p32 + PHP; DB v38)
23. ~~Catalog readiness refresh~~ **done** (Priority 1–2 → Live in `BusinessTypeCatalog`)
24. ~~Owner outlet template~~ **done** (Owner get/set PHP + Settings UI)
25. ~~Owner appointment calendar~~ **done** (day/week + status update; `getServiceAppointmentList` / `updateServiceAppointmentStatus`)
26. ~~Deploy / smoke checklist polish~~ **done** (`23_TESTING_AND_MIGRATION.md`)
27. ~~Owner deposit ledger~~ **done** (`getCustomOrderDepositList` / `updateCustomOrderDepositStatus`)
28. ~~Owner staff roster~~ **done** (`getStaffUserList` / `updateStaffUserStatus`; no PIN in Owner API)
29. ~~Staff PIN hashing~~ **done** (`StaffPinHasher` sha256; legacy plaintext still verifies)
30. ~~Legacy PIN migration + Owner Outlet tools hub~~ **done**
31. ~~Weight scale Settings + wider SPP parse~~ **done** (vendor SDK still optional)
32. ~~BOT prefix UI in printer settings~~ **done**

## Server follow-up
~~Deploy appointment PHP~~ **done**
- Upload: `API/insertServiceAppointment.php`
- Download: `API/getServiceAppointmentList.php`
- Schema: `API/migrations/p27_service_appointment.sql`

~~Deploy bakery deposit PHP~~ **done**
- Upload: `API/insertCustomOrderDeposit.php`
- Download: `API/getCustomOrderDepositList.php`
- Schema: `API/migrations/p28_custom_order_deposit.sql`

~~Deploy wholesale tier PHP~~ **done**
- Upload: `API/insertProductPriceTier.php`
- Download: `API/getProductPriceTierList.php`
- Schema: `API/migrations/p29_product_price_tier.sql`

~~Deploy fashion variant PHP~~ **done**
- Upload: `API/insertProductVariant.php`
- Download: `API/getProductVariantList.php`
- Schema: `API/migrations/p30_product_variant.sql`

~~Deploy business template PHP~~ **done**
- Upload: `API/insertBusinessTemplate.php`
- Download: `API/getBusinessTemplate.php`
- Schema: `API/migrations/p31_company_business_template.sql`

~~Deploy staff roster PHP~~ **done**
- Upload: `API/insertStaffUser.php`
- Download: `API/getStaffUserList.php`
- Schema: `API/migrations/p32_staff_user.sql` (+ `server_upgrade_all.sql`)

~~Deploy Owner template PHP~~ **done** (upload to `androidApp/Owner/`)
- Read: `API/Owner/getBusinessTemplate.php`
- Write: `API/Owner/setBusinessTemplate.php`

~~Deploy Owner appointment APIs~~ **done** (upload to `androidApp/Owner/`)
- Read: `API/Owner/getServiceAppointmentList.php`
- Status: `API/Owner/updateServiceAppointmentStatus.php`

~~Deploy Owner deposit APIs~~ **done** (upload to `androidApp/Owner/`)
- Read: `API/Owner/getCustomOrderDepositList.php`
- Status: `API/Owner/updateCustomOrderDepositStatus.php`

~~Deploy Owner staff APIs~~ **done** (upload to `androidApp/Owner/`)
- Read: `API/Owner/getStaffUserList.php` (no PIN)
- Status: `API/Owner/updateStaffUserStatus.php`

~~Deploy Admin/Dealer template PHP~~ **done**
- Shared: `API/business_template_ops.php`
- Admin: `API/Admin/getBusinessTemplate.php`, `setBusinessTemplate.php`
- Dealer: `API/Dealer/getBusinessTemplate.php`, `setBusinessTemplate.php`
- UI: Customer → licence → **Business template** (Admin + Dealer)

~~Licence registration template~~ **done**
- Admin/Dealer `insertNewLicence.php` + `insertCustomer.php` accept `businessType` + `businessTemplateId`
- New customer / new licence screens: template spinner → `admin_register` / `dealer_register`
- Template pick auto-maps Fast/Dine/Takeaway/Mess (UI + server fallback if all `0`)
- Existing licence **set template**: optional `syncModules` (default on) updates licence module flags
- Owner get/set + POS `insertBusinessTemplate.php` / `getBusinessTemplate.php` use `business_template_ops.php`

**Live deploy:** schema + PHP **done**. Remaining: smoke 1–26 in `23_TESTING_AND_MIGRATION.md`.

~~Priority-3 catalog types~~ **done** — selectable in POS/Admin/Dealer/Owner; map to nearest live template.

## Deferred (optional)
- Proprietary scale vendor SDK — only if a device does not speak SPP ASCII (SPP + Settings pick/test is the supported path)

~~Laravel web admin business-template UI~~ **done** — `BusinessTemplateSupport` on Add/Edit customer & licence (`partials/license-fields`).

