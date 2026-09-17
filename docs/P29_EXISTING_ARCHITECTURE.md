# P29 — Existing Architecture (audit)

Inspected 15 Sep 2026. Do not treat this as the target design.

## Apps

| Surface | Path | Stack |
|---------|------|--------|
| POS (production Java) | `WithTable/` | Android, SQLite, Retrofit |
| POS v2 | `posbillingwala-flutter/` | Flutter Android / iOS / Web, Drift |
| Owner | `Owner/` | Android |
| Dealer | `Dealer/` | Android |
| Admin Android | `Admin/` | Android |
| Web admin | `admin.posbillingwala.com/` | Laravel 9 |
| API | `API/` | PHP + mysqli |
| Marketing | `posbillingwala-react/` | React (no POS auth) |

Shared MySQL. No Prisma/ORM. No WebSockets.

## Authentication

POS identity is **licence key + one bound device + 4-digit licence MPIN** (`licenses.mpin`, stored plaintext today). Platform users (`users.role_id` 1/2/3) are Admin / Dealer / Customer only. There is **no POS staff table**.

Tokens: `api_tokens` actor_type `pos_licence` | `owner` | `dealer` | `admin`.

## License / store

`users.id` = organization. `licenses.id` = branch. One licence = one store. `licenses.android_device_id` = **single device**. No `userManagementEnabled`, `maxUsers`, `maxDevices`, `maxPrinters`.

## Printing

`company_printer_setting` is **one row per licence** (bill MAC + KOT MAC, paper size, logo/QR flags). Flutter adds USB + one network host. No printer inventory, no category routing, no print host, no bill/KOT print job queue (mess tokens have a queue). Web shares text; no ESC/POS bridge.

## Offline / sync

Android WorkManager + status flags. Flutter `FullSyncController`. Unique network-status keys. No general print-job sync.
