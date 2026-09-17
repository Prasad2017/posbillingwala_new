# P29 — Gap analysis

Target: `POS_Master_Cursor_Prompt.md`

| Area | Already exists | Needs modification | New |
|------|----------------|--------------------|-----|
| One licence = one store | Yes (`licenses` = branch) | — | — |
| Single-user licence login | Yes | Keep as default when UM off | — |
| `userManagementEnabled` | No | `licenses` columns | Flag + limits |
| Fixed 8 POS roles | No (only Admin/Dealer/Customer) | — | `pos_role_permission` seed |
| POS staff users + hashed PIN | No | Do **not** reuse `users` | `pos_staff` |
| User permission overrides | No | — | `pos_staff_permission_override` |
| Effective permissions + backend enforce | Frontend licence modules only | Guard mutating APIs | `pos_require_permission` |
| Multi-device | One `android_device_id` | Login/bind when UM on | `pos_devices` |
| Sessions | `api_tokens` | Optional staff id on POS | `pos_sessions` |
| Printer master N printers | Dual bill/KOT endpoints | Keep `company_printer_setting` for layout | `store_printers` |
| Category routing | No | — | `printer_routes` |
| Print host / remote print | Mess token ack only | Reuse ESC/POS write path | `print_hosts`, `print_jobs` |
| Audit log | error/crash/mess audit | — | `pos_audit_log` (never PIN) |
| Admin / Dealer / Owner UM | Licence CRUD | License fields + counts | Staff/device/printer views |
| Flutter PIN staff login | Licence MPIN only | Auth status + router | Staff screens |
| WithTable Java staff UI | — | Leave UM-off compatible | Not rewritten in this pass |
| Cash drawer / auto-cutter | Helpers unused | Do not break raster path | Optional later |

## Potential conflicts

- `users` is platform org/admin/dealer — POS staff must be a **new** table.
- `licenses.mpin` remains for UM-off apps; staff PIN is hashed separately and never returned.
- Single Bluetooth plugin connection: extra BT printers switch MAC; USB/network stay independent.
- Enabling UM on a shop still running only WithTable Java will require staff login that that APK does not have — UM is opt-in.

## Backward compatibility

Existing licences default `userManagementEnabled = 0`. Login, billing, KOT, printing, offline sync unchanged until Admin/Dealer enables UM.
