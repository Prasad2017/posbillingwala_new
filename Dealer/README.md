# POS Billingwala — Dealer app

Dealer field Android app: onboard customers, register/renew licences, bootstrap customer catalogs, and track licence KPIs.

Parent overview: [../README.md](../README.md)

## App identity

| Field | Value |
|-------|--------|
| Module | `Dealer/` |
| Package | `com.posbillingwala.dealer` |
| versionName / versionCode | **1.0.11** / **13** |
| minSdk / targetSdk / compileSdk | 26 / 37 / 37 |
| API base | `https://posbillingwala.com/androidApp/Dealer/` |

Override via `BuildConfig.API_BASE_URL` in `app/build.gradle`.

## Features

- **Home KPIs** — customers (total / active / trial / expired), licences (active / expiring / trial / expired), recent customers, sales snapshot
- **Customers** — registration, list, details
- **Licences** — new registration / renew; validity tiers (6m / 1y / 3y / 5y / lifetime); same-key renew where supported by API
- **Catalog setup** — products, categories, subcategories, portion masters, portions for customer shops
- **Export** — product export + catalog import history
- **Profile** — dealer profile, change password
- **Auth** — login → Bearer token against Dealer API
- **FCM** — Firebase Analytics + Messaging

## Navigation

Bottom nav: **Home | Customers | Product export | Profile**

## Stack

Java 17, ViewBinding, MultiDex, Retrofit 3 + OkHttp 5, Picasso, Material, Lottie, MPAndroidChart, Play In-App Update, AdMob, Firebase Analytics + Messaging.

## Build

Open `Dealer/` in Android Studio (standalone Gradle project).

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Release APK naming (often under `../releases/`, gitignored):

`Dealer-{version}-v{code}-unsigned.apk`

Add `google-services.json` for Firebase. Sign before store rollout.

## Smoke test

1. Dealer login → token issued  
2. Home KPIs load (customers + licences)  
3. Register a customer  
4. Register / renew a licence (check expiry on server)  
5. Add catalog items for that customer  
6. Product export works  

## Related

| Path | Notes |
|------|--------|
| `../API/Dealer/` | Dealer PHP REST endpoints |
| `../docs/LICENSE_API_REQUIREMENTS.md` | Trial / renew / expiry rules |
| `../Admin/` | Mobile admin (dealers + licences) |
| `../admin.posbillingwala.com/` | Web admin dealer & licence management |
| `../website/` | Public dealer finder (CMS) |
