# POS Billingwala — Admin app (Android)

Mobile admin console for platform operators: dealers, customers, licences, catalog, sales/reports, POS monitoring, support, crashes, website contacts, and push notifications.

Closest Android counterpart to the Laravel web admin. Parent overview: [../README.md](../README.md)

## App identity

| Field | Value |
|-------|--------|
| Module | `Admin/` |
| Package | `com.posbillingwala.admin` |
| versionName / versionCode | **1.1** / **2** |
| minSdk / targetSdk / compileSdk | 26 / 37 / 37 |
| API base | `https://posbillingwala.com/androidApp/Admin/` |

Override via `BuildConfig.API_BASE_URL` in `app/build.gradle`.

## Features

- **Dashboard** — high-level platform KPIs
- **Dealers** — list, add, details
- **Customers & licences** — list/add customers; licence lifecycle
- **POS monitoring** — device / branch / licence awareness
- **Catalog** — products and related masters; customer combos; export; import history
- **Sales & reports** — sales dashboard/overview, invoices; customer / license / dealer / branch / device reports
- **Crashes** — crash analytics and logs (server-backed)
- **Support** — tickets / remote assist
- **Website contacts** — inbox for marketing-site enquiries
- **Push** — send push notifications
- **Settings** — profile / notification preferences
- **Auth** — login → Bearer token against Admin API

## Stack

Java 17, ViewBinding, Retrofit 3 + OkHttp 5, WorkManager, Picasso, Material, Lottie, MPAndroidChart, Firebase Analytics + Messaging.

## Build

Open `Admin/` in Android Studio (standalone Gradle project).

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Release APK naming (often under `../releases/`, gitignored):

`Admin-{version}-v{code}-unsigned.apk`

Add `google-services.json` for Firebase. Sign before rollout.

## Smoke test

1. Admin login → token issued  
2. Open dealers + customers lists  
3. View / edit a licence  
4. Open sales dashboard  
5. Check POS monitoring / device report  
6. Open website contacts inbox  
7. Send a test push (staging only)  

## Related

| Path | Notes |
|------|--------|
| `../API/Admin/` | Admin PHP REST endpoints |
| `../admin.posbillingwala.com/` | Full Laravel web admin (preferred for heavy CMS / import) |
| `../Dealer/` | Field dealer app |
| `../docs/DEPLOY_DB.md` | Shared DB migrations |
| `../docs/DEPLOY_WEB.md` | Web admin + website deploy |
