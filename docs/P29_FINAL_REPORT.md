# P29 final report — Multi-user / printers / permissions

Status: **implemented across Admin / Dealer / Owner / Flutter POS**. Hardware e2e (live printers) is still not claimed 100%.

## 1. Architecture changes

- One licence remains one store. User Management is **opt-in** (`userManagementEnabled`, default `0`).
- POS staff is a new identity (`pos_staff`), not `users`. USER ≠ DEVICE ≠ PRINTER.
- Effective permission = role default, then user ALLOW/DENY override. Backend is authoritative.
- Printing: keep `company_printer_setting` for layout; add `store_printers` + routes + print jobs + print host.
- Flutter Android / iOS / Web share the same POS app. WithTable Java stays UM-off compatible.

## 2. Database changes

Migration: `API/migrations/p29_multi_user_printers.sql` (additive).

- `licenses`: `userManagementEnabled` (default 0), `maxUsers`, `maxDevices`, `maxPrinters`, `permissionVersion`
- `pos_staff`, `pos_role_permission`, `pos_staff_permission_override`
- `pos_devices`, `pos_sessions`
- `store_printers`, `printer_routes`, `print_hosts`, `print_jobs` (unique `licenseId + idempotencyKey`)
- `pos_audit_log` (PIN/hash stripped from meta)

Also auto-ensured at runtime by `API/pos_schema.php`.

## 3. API changes

New staff/device/printer/job endpoints (one PHP file each), plus:

- `staffLogin.php` — bcrypt PIN, never returns hash
- `X-Pos-Staff-Id` required when UM is on
- Login/device bind: extra devices in `pos_devices` up to `maxDevices`
- Mutating POS APIs gated with `pos_require_permission` (bills, products, tables, expenses, inventory, mess, printers, reports)
- UM off: permission helper allows full licence access (existing shops unchanged)

## 4. Admin changes

Laravel licence fields: User Management toggle, max users/devices/printers.  
Customer edit / edit-license pages list staff, devices, printers, and routing (`partials/store-ops.blade.php`).  
`API/Admin/updateLicenseUserManagement.php`, `API/Admin/getStoreOpsSummary.php` (full lists).  
Admin Android: **Users / Devices / Printers** on each licence card.

## 5. Dealer changes

`API/Dealer/getStoreOpsSummary.php` returns staff/device/printer/route lists for licences owned by that dealer.  
Dealer Android: **Users / Devices / Printers** on each licence card.

## 6. Owner changes

`API/Owner/getStoreOpsSummary.php` returns the same lists, scoped to the owner's store.  
Owner Android: **Users / Devices / Printers** on each outlet licence card (`StoreOpsFragment`).

## 7. Android changes (Flutter POS)

Staff PIN login, users/roles/overrides, devices, multi-printer + routing + print queue, print host polling, permission-gated home/settings/routes, KOT/bill via `PrintJobDispatcher`. Soft logout with UM on returns to staff PIN (licence stays).

## 8. iOS changes

Same Flutter app. Bluetooth/USB follow iOS platform support. Not separately device-tested.

## 9. Web changes

Same Flutter web target: staff login, permissions, print host polling, remote print jobs, print queue. ESC/POS local hardware is limited vs Android.

## 10. User / role / permission changes

Fixed 8 roles only (Owner, Manager, Waiter, Kitchen, Helper, Bar Attender, Security, Accountant). Form: name, mobile, role, PIN + confirm, optional address/image. Reset PIN hashed. Reset to role defaults clears overrides.

## 11. Printer changes

N store printers (Bluetooth / USB / Wi-Fi). Category routing. Fallback to existing bill/KOT `PrintService` when `store_printers` is empty.

## 12. Print host changes

`registerPrintHost` + `claimPrintJobs`. Flutter `PrintHostService` starts after home hydrate. Waiter device can create a job without a local printer.

## 13. Print job changes

Idempotent create, list, get, acknowledge, retry. Success is not claimed until host ack (remote path). Duplicate key per licence.

## 14. Offline / sync changes

Existing invoice/catalog sync reused. Print jobs queue on the server; local print still uses existing offline ESC/POS path when printers are local. No separate rewrite of WorkManager/Drift sync.

## 15. Security changes

- PIN bcrypt; never returned or audited
- `log_sanitizer.php` redacts `pin` / `appLoginPin` / `pinHash` / `confirmPin`
- Backend permission checks; frontend hides modules only
- Store isolation via existing branch/licence scope
- Last owner cannot be deactivated

## 16. Migration steps

1. Backup MySQL.
2. Import `API/migrations/server_upgrade_all.sql` if not already on current schema.
3. Import `API/migrations/p29_multi_user_printers.sql`.
4. Deploy PHP `API/` and Laravel admin.
5. Ship Flutter POS. Leave UM **off** until the shop is on this POS (WithTable blocks login if UM is on).
6. Enable UM per licence in Admin; seed owner from licence MPIN on first staff list/login.

## 17. Tests passed

`C:\xampp\php\php.exe API/tests/pos_permissions_test.php` — permission/PIN/role/sanitizer/migration assertions (extended).

`flutter analyze` on touched POS modules — no errors after import/color fixes (deprecation infos on dropdown `value` may remain).

Existing `API/tests/multi_branch_scope_test.php` / `catalog_push_test.php` were not re-run in this pass.

## 18. Tests failed / not run

- No live multi-device / multi-printer / remote-host hardware run
- No iOS device run
- No Flutter web print-host run against a kitchen printer
- WithTable Java has no staff UI (login blocked when UM is on)
- No full offline print-job e2e
- No CI suite for new endpoints against a real DB

## 19. Known limitations

- WithTable Java POS has **no** staff/printer-host UI; if UM is on it **blocks PB-PIN login** and tells the shop to use Flutter POS.
- Owner/Dealer/Admin Android show store ops lists (users, devices, printers, routing). Staff create/edit still happens in Flutter POS.
- USB/Bluetooth behaviour is platform-specific; single BT plugin connection still switches MAC.
- Cash drawer / auto-cutter not newly wired.
- Some read-only catalog APIs stay licence-auth only so kitchen/waiter local sync is not blocked.
- `licenses.mpin` remains plaintext for UM-off apps; staff PIN is hashed separately.

## 20. Files changed (primary)

See git status. Core new/updated areas:

- `API/migrations/p29_multi_user_printers.sql`, `API/pos_ops_panel.php`
- `API/pos_*.php`, staff/device/printer/job endpoints, Login/licence payload, mutating POS APIs
- `admin.posbillingwala.com` licence fields, store-ops partial, CustomerController
- Owner / Dealer / Admin Android `StoreOpsFragment` + licence card button
- WithTable `UserManagementGuard` (block UM-on login)
- `posbillingwala-flutter` auth, staff, print, router, home, settings, POS payment/KOT
- `docs/P29_*.md`
