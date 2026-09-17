# Server-Driven UI Plan — POS Billingwala (Flutter v2)

**App:** `posbillingwala-flutter` (`pos_billingwala_v2`)  
**Surfaces:** Android, iOS, Web  
**Status:** Plan only — not implemented  
**Last updated:** 2026-09-17  
**Related:** `docs/P29_EXISTING_ARCHITECTURE.md`, `docs/LICENSE_API_REQUIREMENTS.md`, `posbillingwala-flutter/README.md`

---

## Verdict

**Full server-driven UI (server sends a Flutter widget tree) is not a good fit.**  
**A hybrid “layout manifest” is possible and is the recommended path.**

Billingwala is an offline-first POS. Cart, KOT, print, table sessions, and invoice save must keep working with no network. Those screens stay **native Flutter**. The server should drive **composition** of hubs, banners, feature visibility, and copy — not the billing engine.

Think of it as:

| Layer | Who owns it | Example |
|-------|-------------|---------|
| **Capability** | Native Flutter | POS cart, payment, print, Drift, sync |
| **Composition** | Server JSON + local cache | Home tiles, Settings groups, Reports list, web nav, promo banner |
| **Policy** | Already server-authoritative | Licence modules, staff permissions, signed licence payload |

This is the same pattern used by most production POS / banking apps: **server-configured screens**, not a remote widget interpreter.

---

## 1. Why full SDUI is the wrong target

A true SDUI runtime (`{ "type": "Column", "children": [ { "type": "TextField", ... } ] }`) would fight the current architecture.

| Constraint | Why it blocks full SDUI |
|------------|-------------------------|
| Offline-first | Drift + `FullSyncController`. Billing must not wait for layout JSON. |
| Native I/O | Bluetooth / USB / network ESC/POS, camera QR, FCM, Play in-app update |
| Latency | Counter staff need sub-100ms taps on product grid, qty, KOT |
| Complex local state | Cart, dining session, split bill, KOT queue, unsynced invoices |
| PHP API today | Form-urlencoded endpoints (`Login.php`, `getProductList.php`). No layout service. |
| Multi-platform | Phone, tablet breakpoints (`AppBreakpoints`), web `WebAppShell` — one JSON tree will look wrong on all three |
| Safety | Arbitrary remote widgets + actions = injection / phishing risk on a cash system |

**Rule:** if a screen writes money, prints a ticket, or mutates local SQLite, it stays native.

---

## 2. What the app already server-drives

Do not rebuild these. SDUI should **reuse** them.

| Existing mechanism | File / concept | What the server already controls |
|--------------------|----------------|----------------------------------|
| Licence modules | `LicenseModules`, `UserSession.fastBilling / dineIn / takeAway / mess` | Which billing modes exist |
| Sales KPI flags | `totalSaleData`, `todaySaleData` | Hide/show Home sales cards |
| Staff permissions | `PermissionController`, `staffRoutePermissionRedirect` | Route gate (`billing.create`, `report.view`, …) |
| Signed licence | `license_validator.dart` + public PEM | Expiry, modules, caps — offline-trusted |
| Cloud screen cache | `CloudScreenCache` + fetch/prefetch | Staff, printers, mess tokens, home overview JSON |
| FCM deep links | `notification_navigator.dart` | `type` → route (`license_expiring` → company settings) |
| Locale files | `assets/locale/{en,hi,mr}.json` | Copy (bundled, not remote yet) |
| Feature flags | `AppConfig` | Screenshots, logging, in-app update (build-time) |

Home, Settings hub, Reports hub, Masters hub, and `webNavDestinations` are still **hardcoded Dart lists**. That is the SDUI opportunity.

---

## 3. Target model: layout manifest + action registry

```
Admin / Owner (later)
        │
        ▼
PHP  getUiManifest.php   ← signed, versioned JSON
        │
        ▼
Flutter UiManifestStore  ← Drift or SharedPreferences (offline)
        │
        ├─ HomePage          renders billing tiles from manifest
        ├─ SettingsHubPage   renders section groups from manifest
        ├─ ReportsHubPage    renders report rows from manifest
        ├─ MastersHubPage    renders master rows from manifest
        ├─ WebSideNav        renders nav items from manifest
        └─ PromoBanner       renders campaign from manifest
                    │
                    ▼
            ActionRegistry
            (only allow-listed routes / local handlers)
```

Server never sends raw Flutter widgets. It sends **named slots** that the app already knows how to paint.

### 3.1 Design principles

1. **Unknown type = skip, never crash.** Ship a fallback bundled manifest.
2. **Allow-listed actions only.** JSON may say `navigate:/pos`, never `eval` or arbitrary URLs except `https` promo.
3. **Intersect with licence + staff permissions.** Manifest can hide extra items; it cannot unlock a module the licence did not buy.
4. **Versioned.** `schemaVersion` + `minAppBuild`. Old apps ignore unknown keys.
5. **Cached like catalog.** Fetch on login, on sync, and on FCM `ui_manifest_updated`.
6. **One manifest per licence + locale + platform.** Phone / tablet / web can share the same items with `platforms: ["android","ios","web"]`.

---

## 4. What to drive vs what stays native

### Drive from server (Phase 1–3)

| Surface | Today | After |
|---------|-------|-------|
| Home billing tiles | Hardcoded Fast / Dine-in / Takeaway / Mess | Ordered list of `module_tile` |
| Home catalog tiles | Hardcoded 4 counts | Optional order / hide |
| Home promo banner | Hardcoded “Serve Better Sell Smarter” | Remote campaign(s) |
| Settings hub groups | Hardcoded Dart | Section + item list |
| Reports hub rows | Hardcoded + licence filter | Same filter, server order / extra rows |
| Masters hub rows | Hardcoded | Order / hide (e.g. hide Tables if no dine-in) |
| Web side nav | `webNavDestinations` const | Same items, server order / extra entries |
| Support contact | Hardcoded in README / support page | Phone, hours, WhatsApp, email |
| In-app announcements | FCM list only | Home / login banner slot |

### Stay native forever

- Splash, login, register, PB-PIN, staff login
- `PosPage`, payment, KOT preview, split bill
- Tables floor, takeaway parcels, mess tokens / QR / scan
- Product / combo / table master **forms**
- Invoice save, Drift schema, `FullSyncController`
- Print pipeline (`PrintService`, routing, queue)
- Licence validation and token refresh

### Maybe later (Phase 4+, only if needed)

- Simple **read-only** info pages (About, “what’s new”, festival hours)
- Support ticket **field list** (subject presets)
- Report filter chips (date presets)

Do **not** generate POS product grids or payment forms from JSON.

---

## 5. Component catalog (closed set)

Keep this list small. Every type maps to an existing widget in `lib/core/widgets` or a home/hub tile.

| `type` | Renders as | Allowed on |
|--------|------------|------------|
| `screen` | Page chrome + body slots | All hubs |
| `section` | `AppSectionHeader` + children | Hubs |
| `module_tile` | `BillingTile` / `AppModuleIcon` | Home |
| `kpi_card` | Sales / catalog count card | Home |
| `catalog_tile` | Category/product/combo count | Home |
| `list_row` | `MasterRowTile` / report row | Masters, Reports, Settings |
| `nav_item` | Web rail / drawer item | Web shell |
| `banner` | `PromoBanner` | Home, login (optional) |
| `action_row` | Settings row (language, logout) | Settings |
| `spacer` / `divider` | Layout only | Any |

**Not in the catalog:** `TextField`, `WebView`, `HTML`, custom paint, nested arbitrary columns.

### 5.1 Example manifest (Home + Settings excerpt)

```json
{
  "schemaVersion": 1,
  "manifestId": "pos-home-settings-v3",
  "minAppBuild": 76,
  "locale": "en",
  "licenceId": "12345",
  "updatedAt": "2026-09-17T12:00:00Z",
  "screens": {
    "home": {
      "slots": {
        "kpis": [
          { "id": "today_sales", "type": "kpi_card", "bind": "todaySales", "visibleIf": ["flag:todaySaleData"] },
          { "id": "month_sales", "type": "kpi_card", "bind": "monthSales", "visibleIf": ["flag:totalSaleData"] }
        ],
        "catalog": [
          { "id": "categories", "type": "catalog_tile", "bind": "categoriesCount", "action": "navigate:/masters/categories", "permission": "product.view" },
          { "id": "products", "type": "catalog_tile", "bind": "productsCount", "action": "navigate:/masters/products", "permission": "product.view" }
        ],
        "billing": [
          {
            "id": "fast_billing",
            "type": "module_tile",
            "titleKey": "fast_billing",
            "subtitleKey": "fast_billing_hint",
            "icon": "receipt_long",
            "colors": ["primaryBright", "primary"],
            "action": "navigate:/pos",
            "permission": "billing.create",
            "module": "fastBilling"
          },
          {
            "id": "dine_in",
            "type": "module_tile",
            "titleKey": "dine_in",
            "action": "navigate:/tables",
            "permission": "table.view",
            "module": "dineIn"
          }
        ],
        "banners": [
          {
            "id": "promo_q3",
            "type": "banner",
            "title": "Serve Better\nSell Smarter!",
            "subtitle": "Happy Customers\nStronger Business",
            "platforms": ["android", "ios"],
            "action": "open_url:https://posbillingwala.com"
          }
        ]
      }
    },
    "settings": {
      "sections": [
        {
          "id": "store",
          "titleKey": "store",
          "items": [
            { "id": "company", "type": "list_row", "titleKey": "shop_details", "action": "navigate:/settings/company", "permission": "settings.view" },
            { "id": "printers", "type": "list_row", "titleKey": "printers", "action": "navigate:/settings/printers", "permission": "printer.view" }
          ]
        }
      ]
    },
    "web_nav": {
      "items": [
        { "id": "home", "type": "nav_item", "titleKey": "home", "icon": "home", "action": "navigate:/" },
        { "id": "pos", "type": "nav_item", "titleKey": "billing", "icon": "point_of_sale", "action": "navigate:/pos", "permission": "billing.create", "module": "fastBilling" }
      ]
    }
  }
}
```

`titleKey` resolves through `AppStrings` / locale JSON. Raw `title` is allowed only for campaigns.

---

## 6. Action registry (allow list)

JSON `action` is a string. The client maps it; anything unknown is a no-op.

| Prefix | Example | Handler |
|--------|---------|---------|
| `navigate:` | `navigate:/pos` | `context.push` / `go` — path must exist in `router.dart` |
| `navigate_name:` | `navigate_name:payment` | GoRouter named route |
| `local:` | `local:logout`, `local:pick_language`, `local:check_update` | Named Dart callbacks on that page |
| `open_url:` | `open_url:https://…` | `url_launcher`, https only |
| `none` | — | Decorative |

**Reject:** `javascript:`, `intent:`, file URLs, unknown paths, routes the staff permission would redirect away from.

Intersect every item with:

1. `module` → `LicenseModules.*`
2. `permission` → `PermissionController.allows`
3. `visibleIf` flags (`todaySaleData`, `userManagementEnabled`, `AppPlatform.useDesktopShell`)
4. `platforms` and `minAppBuild`

Server cannot grant Fast Billing if the signed licence has `fastBilling=false`.

---

## 7. Data binding (not remote widgets)

Hub tiles that show numbers stay bound to **local providers**, not to values inside the manifest.

| `bind` | Source |
|--------|--------|
| `todaySales` / `monthSales` | `home_sales_providers.dart` + `CloudScreenCache.homeOverview*` |
| `categoriesCount` / `productsCount` / `combosCount` | Drift catalog counts |
| `unreadNotifications` | `unreadNotificationCountProvider` |
| `printerChip` | `BluetoothPrinterHub` (never from server) |

The manifest says *which* KPI to show and in which order. The value still comes from the device.

---

## 8. API contract

Additive PHP endpoint. Do not change existing login/sync payloads except an optional `uiManifestVersion` hint.

### `GET/POST getUiManifest.php`

**Request (form-urlencoded, same as other POS APIs)**

| Field | Required | Notes |
|-------|----------|-------|
| `userId` | yes | Licence id (`UserSession.licenceUserId`) |
| `locale` | yes | `en` / `hi` / `mr` |
| `platform` | yes | `android` / `ios` / `web` |
| `appBuild` | yes | `76` |
| `currentManifestId` | no | Client cache id — server may return `304`/`not_modified` |

**Response**

```json
{
  "status": "1",
  "notModified": false,
  "manifest": { "...": "see §5.1" },
  "signature": "base64-rsa-sha256"
}
```

Sign the canonical JSON of `manifest` with the **same licence signing key** used today (`docs/LICENSE_API_REQUIREMENTS.md`). Client verifies with `assets/license_signing_public.pem`. Unsigned manifests are ignored when a signed cache already exists.

### Cache + fetch points

| When | Behaviour |
|------|-----------|
| First authenticated Home | Use bundled fallback immediately; refresh in background |
| Login / MPIN success | Fetch if `uiManifestVersion` changed |
| Full sync / Fetch from cloud | Include manifest as a `CloudScreenCache` key (`ui_manifest`) |
| FCM type `ui_manifest_updated` | Fetch + `ref.invalidate(uiManifestProvider)` |
| Offline | Last good signed manifest; else bundled default |

Add `ui_manifest` to `CloudScreenCache.allKeys` so Fetch Result counts it.

### Fallback

Ship `assets/ui/default_manifest.json` that matches **today’s** hardcoded Home / Settings / Reports / Masters / web nav. If parse or signature fails, that file is the UI. Shops never see an empty Home.

---

## 9. Flutter implementation sketch

New code only. Do not rewrite `PosPage`.

```
lib/features/ui_manifest/
  data/
    ui_manifest_api.dart          # getUiManifest.php
    ui_manifest_store.dart        # cache + signature check
  domain/
    ui_manifest.dart              # parsed models
    ui_manifest_providers.dart    # Riverpod
    ui_action_registry.dart       # allow-listed actions
    ui_visibility.dart            # licence ∩ permission ∩ platform
  presentation/
    sdui_module_tile.dart         # thin wrapper over BillingTile
    sdui_list_row.dart
    sdui_banner.dart
    sdui_section.dart
```

Wire-up (additive):

| File | Change |
|------|--------|
| `ApiEndpoints` | `getUiManifest = 'getUiManifest.php'` |
| `CloudScreenCache` | `uiManifest = 'ui_manifest'` |
| `full_sync_controller.dart` / `cloud_screen_prefetch.dart` | Fetch + save |
| `home_page.dart` | Build `billingTiles` from manifest (fallback to current list) |
| `settings_hub_page.dart` | Sections from manifest |
| `reports_hub_page.dart` | Rows from manifest |
| `masters_hub_page.dart` | Rows from manifest |
| `web_app_shell.dart` | `webNavDestinations` from manifest |
| `router.dart` | No dynamic routes in v1. Unknown `navigate:` paths are ignored |

Feature flag in `AppConfig`:

```dart
static const bool enableUiManifest = true;
```

When `false`, hubs keep today’s Dart lists. Use this to ship the parser before any PHP is live.

---

## 10. Phased plan

One phase at a time. Each phase must boot offline with the bundled fallback.

### Phase 0 — Inventory (no API)

- Extract Home billing tiles, Settings groups, Reports rows, Masters rows, `webNavDestinations` into Dart **default manifest objects** (same JSON shape as the API).
- Add `UiActionRegistry` with current routes only.
- Tests: parse bundled JSON; unknown type skipped; locked module still locked.

**Exit:** Home looks identical. No server yet.

### Phase 1 — Home composition

- `getUiManifest.php` returns `screens.home` only (billing + catalog + banners).
- Cache in `CloudScreenCache`.
- Promo banner becomes the first real remote content (campaign without an app release).

**Exit:** Admin can hide Mess tile or reorder billing tiles per licence without a Play release. POS/payment unchanged.

### Phase 2 — Hubs + web nav

- Settings / Reports / Masters / `web_nav` in the same manifest.
- Support contact block remote.

**Exit:** A new report row can be added only if the **route already exists** in the app. Manifest cannot invent `/reports/magic`.

### Phase 3 — Ops

- FCM `ui_manifest_updated`.
- `minAppBuild` + “update app to see this item”.
- Admin UI in `admin.posbillingwala.com` to edit banners and tile order (licence or org scope).
- Locale overlays: optional `copy` map merged on top of `assets/locale`.

### Phase 4 — Only if product asks

- Read-only CMS pages (`type: markdown` or `rich_text` with a sanitised subset).
- A/B: `experimentId` on a banner.
- Per-staff Home (waiter vs cashier) — still intersected with `PermissionController`.

**Stop before** generating POS grids, payment fields, or print layouts from JSON.

---

## 11. Admin / Owner editing (Phase 3)

Keep v1 editing boring and safe.

| Editable | Not editable |
|----------|--------------|
| Tile order | New arbitrary routes |
| Hide tile (still licence-gated) | Payment modes |
| Banner title, image URL, schedule, link | Print templates (those stay printer settings) |
| Support phone / hours | Staff permission keys (those stay role API) |

Suggested table (additive, no DROP):

```sql
CREATE TABLE ui_manifests (
  id INT PRIMARY KEY AUTO_INCREMENT,
  licence_id INT NULL,          -- NULL = org default
  organization_id INT NULL,
  platform VARCHAR(16) NOT NULL DEFAULT 'all',
  locale VARCHAR(8) NOT NULL DEFAULT 'en',
  schema_version INT NOT NULL DEFAULT 1,
  min_app_build INT NOT NULL DEFAULT 0,
  manifest_json MEDIUMTEXT NOT NULL,
  updated_at DATETIME NOT NULL,
  updated_by INT NULL
);
```

Resolve: licence row → org default → bundled client fallback.

---

## 12. Security

| Risk | Mitigation |
|------|------------|
| Manifest grants a paid module | Client intersects `LicenseModules` + signed licence payload |
| Open redirect / malware URL | `open_url` https only; optional host allow list (`posbillingwala.com`, Play Store) |
| Tamper on the wire | RSA-SHA256 signature, same as licence |
| Stale cache after expiry | Licence check still runs; expired licence never reaches Home |
| Huge JSON | Cap size (e.g. 128 KB); reject extra nesting |
| Staff sees Settings they should not | `permission` on every item + existing router redirect |

Never put secrets, printer MACs, or MPIN in the manifest.

---

## 13. Offline and performance

- Manifest is small (~5–20 KB). Store next to other cloud screens.
- Home first paint uses cache synchronously (or bundled asset).
- Do not block `catalogBootstrapListener` or printer auto-connect on manifest fetch.
- Web: refresh with existing `WebAppShell.refreshCloud`.
- Tablet / web breakpoints stay in Flutter (`AppBreakpoints`). Manifest does not send pixel widths.

---

## 14. Testing

| Case | Expect |
|------|--------|
| No network, empty cache | Bundled default Home (4 billing tiles as today) |
| Licence without Mess | Mess tile absent even if manifest includes it |
| Staff without `report.view` | Reports nav/row hidden; `/reports` still redirects Home |
| Unknown `type` | Item skipped, rest render |
| Unknown `action` | Tap no-ops |
| Signature fail | Keep previous good cache; do not apply |
| `minAppBuild` > current | Item hidden or shows “Update app” |
| Banner `platforms: ["android"]` | Hidden on web |

Widget tests against `default_manifest.json` so a schema change cannot empty Home.

---

## 15. Success metrics

SDUI is worth it only if these move:

- Promo / upsell on Home without a store release
- Per-plan Home (trial vs paid vs mess-only) without forking Dart
- Tile order experiments (Fast Billing first vs Tables first)
- Support phone / festival banner same-day

It is **not** success if:

- POS tap-to-cart gets slower
- Offline billing breaks when manifest fetch fails
- Every new screen still needs an app release **and** a JSON edit (double work)

---

## 16. Recommendation

**Yes, implement — as layout manifests for hubs only.**

1. Phase 0 in Flutter (extract defaults, action registry, flag `enableUiManifest`).
2. Phase 1 API for Home tiles + promo banner.
3. Stop there until a real Admin editor exists; do not invent a widget VM.

POS, payment, print, tables, mess, and sync stay native. That is the correct split for this codebase.
