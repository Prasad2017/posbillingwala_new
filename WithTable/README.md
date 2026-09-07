# POS Billingwala — POS app (`WithTable`)

Offline-first Android POS for restaurants and shops: dine-in tables, takeaway, mess + QR tokens, combos, catalog, reports, and Bluetooth receipt printing.

Parent overview: [../README.md](../README.md)

## App identity

| Field | Value |
|-------|--------|
| Module | `WithTable/` |
| Package | `com.pos_billingwala` |
| versionName / versionCode | **2.0.59** / **75** |
| minSdk / targetSdk / compileSdk | 24 / 37 / 37 |
| API base | `https://posbillingwala.com/androidApp/` |
| Media base | `https://posbillingwala.com/storage/app/` |

Override URLs via `BuildConfig.API_BASE_URL` / `MEDIA_BASE_URL` in `app/build.gradle`.

## Features

- **Offline-first billing** — SQLite first; WorkManager + connectivity receivers sync when online
- **Order modes** — dine-in tables, takeaway, mess membership, walk-in mess QR tokens (generate / print / scan / verify)
- **Catalog** — Food type → Category → optional Subcategory → Product → Portions (Portion Master)
- **Combos** — combo master with components + sell price; invoice component snapshots
- **Store details** — structured shop name / address / phone for receipt headers
- **Print** — Bluetooth (Woosim/SPP); print failure never wipes a saved bill
- **Licensing** — server-authoritative expiry; trial (7-day / 50-bill); renew same key
- **Auth** — login + MPIN → Bearer token (`api_tokens`)
- **i18n** — English / Hindi / Marathi
- **Reports** — sales, invoice, product, payment, discount, refund, expense, mess
- **Ops** — inventory, expenses, printer settings, cloud sync status, support tickets
- **Universal templates** — Settings → Business Template; appointments / deposits in Master Data; staff roster; barcode / weight / variants / wholesale tiers when template enables them
- **Observability** — Firebase Crashlytics, Performance, Analytics, Messaging

Pack + smoke: [../Billingwala_Universal_POS_COMPLETE/23_TESTING_AND_MIGRATION.md](../Billingwala_Universal_POS_COMPLETE/23_TESTING_AND_MIGRATION.md)

## Stack

Java 17, Activities/Fragments, ViewBinding, SQLite, Retrofit 3 + OkHttp 5, WorkManager, Picasso, Material, ZXing, MPAndroidChart, Lottie, Play In-App Update, AdMob, Firebase, Woosim Bluetooth JARs.

## Build

Open `WithTable/` in Android Studio (standalone Gradle project).

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Release APK naming (often under `../releases/`, gitignored):

`POS-Billingwala-{version}-v{code}-unsigned.apk`

Add `google-services.json` for Firebase builds. Sign with your upload keystore before store rollout.

## Smoke test

1. Login (licence + MPIN) → Bearer token issued  
2. Sync catalog (food types, categories, subcategories, portion masters, portions)  
3. Add product with portion + price → syncs  
4. Create combo → bill + print  
5. Store Details → print header uses structured fields  
6. Mess walk-in token → QR print → scan/verify  
7. Switch language EN / HI / MR  
8. Expired / trial-blocked licence rejected server-side  
9. Universal extras (template-gated): barcode/camera, weight+scale, BOT printer, staff roster, salon calendar sync, bakery deposit/photo, wholesale tiers — see `Billingwala_Universal_POS_COMPLETE/23_TESTING_AND_MIGRATION.md` (includes Owner template/appointments + full server deploy checklist)

## Related

| Path | Notes |
|------|--------|
| `../API/` | POS PHP endpoints at androidApp root |
| `../Billingwala_Universal_POS_COMPLETE/23_TESTING_AND_MIGRATION.md` | Smoke + live deploy checklist (p27–p32, Owner PHP) |
| `../API/insertServiceAppointment.php` | Salon appointment cloud upload |
| `../API/insertCustomOrderDeposit.php` | Bakery deposit cloud upload |
| `../API/migrations/p27_service_appointment.sql` | Appointments table |
| `../API/migrations/p28_custom_order_deposit.sql` | Custom order deposits table |
| `../API/migrations/p29_product_price_tier.sql` | Wholesale price tiers table |
| `../API/insertProductPriceTier.php` | Wholesale tier cloud upload |
| `../API/migrations/p30_product_variant.sql` | Fashion product variants table |
| `../API/insertProductVariant.php` | Fashion variant cloud upload |
| `../API/migrations/p31_company_business_template.sql` | Business template per licence |
| `../API/insertBusinessTemplate.php` | Business template cloud upload (POS) |
| `../API/Owner/getBusinessTemplate.php` | Owner read outlet template |
| `../API/Owner/setBusinessTemplate.php` | Owner set outlet template |
| `../API/Owner/getServiceAppointmentList.php` | Owner read outlet appointments |
| `../API/Owner/updateServiceAppointmentStatus.php` | Owner update appointment status |
| `../API/Owner/getCustomOrderDepositList.php` | Owner read outlet deposits |
| `../API/Owner/updateCustomOrderDepositStatus.php` | Owner update deposit status |
| `../API/Owner/getStaffUserList.php` | Owner read outlet staff (no PIN) |
| `../API/Owner/updateStaffUserStatus.php` | Owner update staff active/role/delete |
| `../API/migrations/p32_staff_user.sql` | Staff roster table |
| `../API/insertStaffUser.php` | Staff roster cloud upload |
| `../API/Admin/setBusinessTemplate.php` | Admin set licence template |
| `../API/Admin/insertNewLicence.php` | Admin register licence (+ optional template) |
| `../API/Dealer/setBusinessTemplate.php` | Dealer set licence template |
| `../API/Dealer/insertNewLicence.php` | Dealer register licence (+ optional template) |
| `../API/business_template_ops.php` | Shared template ensure/upsert helper |
| `../docs/COMBO_API_REQUIREMENTS.md` | Combo API contract |
| `../docs/STORE_DETAILS_API_CHANGES.md` | Structured store fields |
| `../docs/LICENSE_API_REQUIREMENTS.md` | Licence / trial behaviour |
| `../docs/DEPLOY_DB.md` | Migrations (`server_upgrade_all.sql`) |
