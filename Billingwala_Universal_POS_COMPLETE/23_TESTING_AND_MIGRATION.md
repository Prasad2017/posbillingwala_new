# 23 Testing And Migration

**Status:** live schema + PHP deploy **done**. Run smoke 1–26 on a real licence before calling Universal “verified in production.”

## Smoke (restaurant default — must pass)
1. Login + MPIN → token
2. Catalog sync
3. Fast / Dine-In / Takeaway / Mess bill + print
4. Inventory stock-in + sale deduct
5. Template switch to Retail → Home hides dine/mess; switch back to Restaurant
6. BOT toggle only on Bar template; KOT unchanged when BOT off
7. Mess QR issue/scan still `POSBILL|v1|…`

## Smoke (Universal POS extras — template-gated)
8. **Barcode** (Retail): Enter + camera scan → exact `productCode` add
9. **Weight** (Weight/Fresh): Settings pick/test scale + CreatePos keypad / BT read
10. **Bar BOT**: food+beverage bill → kitchen then bar; dedicated BOT MAC optional
11. **Staff roster**: add staff → switch session with personal/report PIN
12. **Salon**: book appointment → day/week calendar (Master Data) → Manage status; cloud sync
13. **Bakery**: custom order note/due/deposit/photo → Master Data **Deposit ledger** + bill photo; cloud sync
14. **Wholesale**: product tiers → CreatePos picker; cloud sync
15. **Fashion**: size/color variants → CreatePos picker; cloud sync
16. **Business template**: Settings change → cloud sync; Fetch applies when local not pending
17. **Staff roster**: add/edit staff → cloud sync
18. Print failure never deletes saved invoice

## Smoke (Owner app — after Owner PHP deploy)
19. **Outlet tools**: Settings → Outlet tools → template / appointments / deposits / staff
20. **Outlet template**: set template → POS Fetch applies (if local not pending)
21. **Outlet appointments**: day/week → tap status → POS Fetch shows new status
22. **Outlet deposits**: list → tap status → POS Fetch shows new status
23. **Outlet staff**: list → activate/role/soft-delete (no PINs) → POS Fetch applies

## Smoke (Admin / Dealer — after Admin/Dealer PHP deploy)
24. **Licence template**: Customer → licence → Business template → save → POS Fetch applies
25. **New licence template**: Register franchise licence with non-restaurant template → POS Fetch applies
26. **New customer template**: Register customer with template → owner licence gets cloud template row

## Smoke (Web admin — optional)
27. **Laravel licence template**: Add/Edit customer or licence → Business template + sync modules → POS Fetch applies

## Migration rules
- Additive SQLite only (POS) — current `DATABASE_VERSION` **38**
- Additive MySQL only (server) — `p27`–`p32`
- Never rename `invoiceType` / `cartOrderStatus` wire values
- Prefer feature flags over forking billing Activities

## Server deploy checklist

**Status: done on live** (schema `p27`–`p32` / `server_upgrade_all.sql` + POS/Owner/Admin/Dealer PHP + `business_template_ops.php`). Keep the lists below as the upload inventory / rollback reference.

### 1. Schema (live MySQL)

Run either:
- `API/migrations/server_upgrade_all.sql` (includes p27–p32), or
- Individual: `p27_service_appointment.sql` … `p32_staff_user.sql`

| Step | Table |
|------|--------|
| p27 | `service_appointments` |
| p28 | `custom_order_deposits` |
| p29 | `product_price_tiers` |
| p30 | `product_variants` |
| p31 | `company_business_templates` |
| p32 | `staff_users` |

### 2. POS PHP → `androidApp/` (root)

| Upload | Download |
|--------|----------|
| `insertServiceAppointment.php` | `getServiceAppointmentList.php` |
| `insertCustomOrderDeposit.php` | `getCustomOrderDepositList.php` |
| `insertProductPriceTier.php` | `getProductPriceTierList.php` |
| `insertProductVariant.php` | `getProductVariantList.php` |
| `insertBusinessTemplate.php` | `getBusinessTemplate.php` |
| `insertStaffUser.php` | `getStaffUserList.php` |

### 3. Owner PHP → `androidApp/Owner/`

| File | Role |
|------|------|
| `getBusinessTemplate.php` | Read outlet template |
| `setBusinessTemplate.php` | Set outlet template |
| `getServiceAppointmentList.php` | Read outlet appointments |
| `updateServiceAppointmentStatus.php` | Update appointment status |
| `getCustomOrderDepositList.php` | Read outlet deposits |
| `updateCustomOrderDepositStatus.php` | Update deposit status |
| `getStaffUserList.php` | Read outlet staff (no PIN) |
| `updateStaffUserStatus.php` | Activate / role / soft-delete |

### 4. Admin PHP → `androidApp/Admin/`

| File | Role |
|------|------|
| `getBusinessTemplate.php` | Read licence template |
| `setBusinessTemplate.php` | Set licence template (`admin_push`) |
| `insertCustomer.php` | New customer + owner licence + template |
| `insertNewLicence.php` | Franchise licence + template |

Also upload shared helper once (parent of Admin/Dealer/Owner): `business_template_ops.php` → `androidApp/business_template_ops.php` (or same relative path as repo).

### 5. Dealer PHP → `androidApp/Dealer/`

| File | Role |
|------|------|
| `getBusinessTemplate.php` | Read template (dealer-owned licences only) |
| `setBusinessTemplate.php` | Set template (`dealer_push`) |
| `insertCustomer.php` | New customer + owner licence + template |
| `insertNewLicence.php` | Franchise licence + template |

### 6. Sync behaviour
- POS cloud sync upload → mark `synced` only when API `status == "1"`
- Fetch Data downloads via workers (`ServiceAppointmentWorker`, …)
- Template: pending local changes are **not** overwritten on Fetch
- Owner/Admin/Dealer template pushes appear on POS after Fetch

## Deferred (not blocking deploy)
- Proprietary scale vendor SDK only if a scale does not speak SPP ASCII (Settings pick/test + CreatePos read is the supported path)

## Build
```bash
export JAVA_HOME=…/jbr-21.0.11/Contents/Home
cd WithTable && bash ./gradlew :app:compileDebugJavaWithJavac
cd Owner && bash ./gradlew :app:compileDebugJavaWithJavac
cd Admin && bash ./gradlew :app:compileDebugJavaWithJavac
cd Dealer && bash ./gradlew :app:compileDebugJavaWithJavac
```
