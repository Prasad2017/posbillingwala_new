# POS Billingwala Version 2 (Flutter)

Cross-platform rewrite of the Android `WithTable` POS for **Android**, **iOS**, and **Web**.

Package name: `pos_billingwala_v2` · version **2.0.1+76**.

Parent overview: [../README.md](../README.md)

## App identity

| Field | Value |
|-------|--------|
| Folder | `posbillingwala-flutter/` |
| Dart package | `pos_billingwala_v2` |
| Android `applicationId` | `com.pos_billingwala` (same Firebase Android app as WithTable) |
| iOS bundle ID | `com.posbillingwala.posBillingwalaV2` |
| versionName / versionCode | **2.0.1** / **76** (`pubspec.yaml`) |
| API base | `https://posbillingwala.com/androidApp/` |
| Media base | `https://posbillingwala.com/storage/app/` |

Override URLs at build time:

```bash
flutter run --dart-define=API_BASE_URL=https://posbillingwala.com/androidApp/ --dart-define=MEDIA_BASE_URL=https://posbillingwala.com/storage/app/
```

## Features

- **Offline-first on mobile** — Drift/SQLite first; catalog and invoices sync when online. **Web is online-only + API-first** (writes go to cloud immediately; Drift is a cache with periodic upload+download refresh).
- **Auth & licence** — splash, login, register/trial, MPIN, licence modules (fast billing / dine-in / takeaway / mess).
- **Order modes** — POS cart and payment, dine-in tables (join, split bill, KOT), takeaway, mess members + meal tokens (QR generate / scan).
- **Catalog** — food types, categories, subcategories, products, portions, combos.
- **Ops** — inventory, expenses, reports (sales, product, tables, expense, mess), bill upload/download.
- **Print** — Bluetooth (incl. Woosim), LAN ESC/POS, receipt preview/share.
- **Settings** — company/store details, printers, business hours, change PIN, language (EN / HI / MR), about, share app.
- **Support & notifications** — tickets, FCM + in-app bell.
- **Ads** — Google Mobile Ads on supported platforms.

## Stack

| Concern | Package |
|---------|---------|
| UI | Flutter Material 3, Poppins, SVG assets |
| State | flutter_riverpod |
| Navigation | go_router |
| HTTP | dio |
| Local DB | drift + sqlite3 |
| Push | firebase_core, firebase_messaging, flutter_local_notifications |
| Print | ESC/POS + `print_bluetooth_thermal` / LAN / Woosim JAR / `share_plus` |
| Scan / camera | mobile_scanner, image_picker |
| Ads | google_mobile_ads |

## Run

Requires [Flutter](https://docs.flutter.dev/get-started/install) (SDK ^3.12) and a device, emulator, or Chrome.

```bash
cd posbillingwala-flutter
flutter pub get
flutter run
```

Target a platform:

```bash
flutter run -d android
flutter run -d ios
flutter run -d chrome
```

Firebase: add `android/app/google-services.json` (and iOS `GoogleService-Info.plist` if you use FCM on iOS). Those files are not committed.

## Release builds

Interactive or flagged release via `build_script.sh` (APK / AAB / web under `release/`):

```bash
./build_script.sh --apk
./build_script.sh --aab
./build_script.sh --web
./build_script.sh --all
```

Or Flutter CLI:

```bash
# Android App Bundle — add android/key.properties (storeFile, storePassword, keyAlias, keyPassword)
flutter build appbundle --release

# Android APK
flutter build apk --release

# iOS IPA (Xcode signing required)
flutter build ipa --release

# Web
flutter build web --release
```

Without `android/key.properties` (or `-PRELEASE_*` Gradle properties), Android release falls back to the debug keystore.

## Project structure

```
lib/
  app/                 # MaterialApp, theme, go_router
  core/                # constants, network, Drift DB, theme, widgets
  features/
    ads/               # AdMob banner
    auth/              # Licence / MPIN / session
    company/           # Store / company API
    home/              # Module hub + sales snapshot
    pos/               # Cart, payment, KOT
    tables/            # Dine-in floor, join, split
    takeaway/
    mess/              # Members, meal sessions, QR tokens
    masters/           # Catalog + combos
    inventory/
    expense/
    reports/
    sync/              # Upload / download bills, catalog bootstrap
    notifications/     # FCM + in-app store
    print/             # ESC/POS, Bluetooth, share
    settings/
    support/           # Tickets
  shared/models/       # Catalog + inventory API DTOs
  l10n/                # EN / HI / MR strings
```

## Checks

```bash
flutter pub get
flutter analyze
flutter test
```

CI (`.github/workflows/flutter_verify.yml`) runs analyze, test, and a debug APK build.

## Related

| Path | Notes |
|------|--------|
| `../WithTable/` | Native Android POS this app replaces |
| `../API/` | PHP endpoints at `androidApp/` |
| `../docs/` | Licence, combo, store-details API notes |

The native app stays in `../WithTable` until production cutover.
