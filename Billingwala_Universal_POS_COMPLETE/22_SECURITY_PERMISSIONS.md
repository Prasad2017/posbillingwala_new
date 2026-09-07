# 22 Security Permissions

## Live
- Licence key + MPIN → Bearer `api_tokens`
- Module flags (Fast/Dine/Takeaway/Mess)
- FeatureEngine: template ∩ licence ∩ company
- BranchSession + LicenceScopeGuard
- **Device staff RBAC** (Owner / Manager / Cashier / Waiter)
- **Multi-user roster** (`staff_user`) — named staff + optional personal PIN + **cloud sync** (`p32`)

## Classes
| Class | Role |
|-------|------|
| `StaffRole` | Device-session role enum (default **Owner**) |
| `StaffUserResponse` | Roster row (local + sync) |
| `SecurityPermissions` | `canWithoutPin`, `runAuthorized`, `activateStaff`, report PIN prompt |

## Permission matrix (device role)

| Permission | Owner/Manager | Cashier | Waiter |
|------------|---------------|---------|--------|
| Bill | yes | yes | yes |
| Clear cart | yes | yes | PIN |
| Inventory / Expense | yes | yes | PIN |
| Master / Shop / Template / Printer / Sync | yes | PIN | PIN |
| Reports / Change App PIN / Set role / Mess members | **always PIN** | always PIN | always PIN |

Any restricted action can be overridden with shop **reportPin** (manager override).

## Wired
- Settings → Device staff / roster (PIN to open)
  - Manage staff roster (add / edit / remove)
  - Switch to named staff (personal PIN or reportPin)
  - Quick device role (no roster) — prior single-role picker
- Settings rows gated via `runAuthorized`
- CreatePos clear cart
- Mess member list
- Logout resets role to Owner and clears active staff
- Cloud: `insertStaffUser.php` / `getStaffUserList.php` / `StaffUserWorker`
- **Owner**: Settings → Outlet staff — list (no PINs) + activate / role / soft-delete via `updateStaffUserStatus.php`

## Safe rules
1. Default Owner = prior behaviour for billing / master data (reports still PIN as before)
2. Empty roster = same as before (quick role only)
3. Additive SQLite `staff_user` + prefs `activeStaffId` / `activeStaffName`
4. Platform Admin/Dealer `role_id` remains separate
5. Staff PIN: new values stored as `sha256:<hex>`; legacy plaintext still verifies and is re-hashed on roster read (then synced)
6. Owner API never returns `staffPin`
7. Owner Settings → **Outlet tools** hub → template / appointments / deposits / staff
