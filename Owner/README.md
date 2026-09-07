# POS Billingwala — Owner app

Shop-owner Android app for multi-outlet oversight: sales dashboards, branch comparison, catalog CRUD, and mess QR visibility (no full POS billing).

Parent overview: [../README.md](../README.md)

## App identity

| Field | Value |
|-------|--------|
| Module | `Owner/` |
| Package | `com.posbillingwala.owner` |
| versionName / versionCode | **1.0.7** / **8** |
| minSdk / targetSdk / compileSdk | 26 / 37 / 37 |
| API base | `https://posbillingwala.com/androidApp/Owner/` |

Override via `BuildConfig.API_BASE_URL` in `app/build.gradle`.

## Features

- **Home KPIs** — total / today sales, branch comparison, store-wise invoices
- **Multi-branch** — organization / outlet scope; store-wise sales comparison
- **Catalog CRUD** — products, categories, subcategories, portion masters, portions
- **Catalog push** — push catalog to outlets; product export; import history
- **Sales & reports** — sales dashboard/overview, order invoices, operational reports, reports hub
- **Mess** — today’s mess tokens + QR management (monitor, not full POS billing)
- **Auth** — login → Bearer token against Owner API
- **FCM** — Firebase Analytics + Messaging

## Stack

Java 17, ViewBinding, MultiDex, Retrofit 3 + OkHttp 5, WorkManager, Picasso, Material, Lottie, MPAndroidChart, ZXing, Play In-App Update, AdMob, Firebase Analytics + Messaging.

## Build

Open `Owner/` in Android Studio (standalone Gradle project).

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Release APK naming (often under `../releases/`, gitignored):

`Owner-{version}-v{code}-unsigned.apk`

Add `google-services.json` for Firebase. Sign before store rollout.

## Smoke test

1. Owner login → token issued  
2. Home shows sales + branch comparison  
3. Open store-wise invoices for an outlet  
4. Add/update product or portion → syncs via Owner API  
5. Push catalog to outlets (if multi-branch)  
6. View mess token / QR screens for an outlet  

## Related

| Path | Notes |
|------|--------|
| `../API/Owner/` | Owner PHP REST endpoints |
| `../WithTable/` | POS app used by each outlet |
| `../docs/DEPLOY_DB.md` | Shared DB / multi-branch migrations |
| `../admin.posbillingwala.com/` | Web admin (customers, licences, sales) |
