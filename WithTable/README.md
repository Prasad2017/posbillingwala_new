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
- **Observability** — Firebase Crashlytics, Performance, Analytics, Messaging

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

## Related

| Path | Notes |
|------|--------|
| `../API/` | POS PHP endpoints at androidApp root |
| `../docs/COMBO_API_REQUIREMENTS.md` | Combo API contract |
| `../docs/STORE_DETAILS_API_CHANGES.md` | Structured store fields |
| `../docs/LICENSE_API_REQUIREMENTS.md` | Licence / trial behaviour |
| `../docs/DEPLOY_DB.md` | Migrations (`server_upgrade_all.sql`) |
