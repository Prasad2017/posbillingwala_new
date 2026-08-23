# Production License API Requirements

**Status:** Implemented in this repo (P6 production licensing).  
**Audience:** Server deploy + POS Android integration.  
**Principle:** License data is **server-authoritative**. The APK stores only a **signed payload** and a **public key** — never the private signing key.

---

## Hierarchy

```
Organization (users.id — shop owner)
    ↓
Branch (licenses.id — owner = Main Store, franchise = branch label)
    ↓
Device (licenses.android_device_id — one active licence per device)
    ↓
License (licenses.licenseKey — same key kept on upgrade/renew)
```

| Concept | DB mapping |
|---------|------------|
| Organization | `users.id` where `role_id=3` (customer) |
| Branch | `licenses.id` + `userType` (`owner` / `franchise`) |
| Device | `licenses.android_device_id` + `android_device_name` |
| License | `licenses.licenseKey`, `expiryDate`, `licenseType`, `licenseStatus` |

---

## Registration before trial

Trial licences (`licenseType` = `Demo` or `Trial`) can be created by:

1. **Dealer/Admin** — `insertCustomer.php` (manual registration)
2. **POS self-service** — `registerTrial.php` (7-day Demo licence on signup)

| Rule | Enforcement |
|------|-------------|
| Customer must exist | `users.is_active = 1` |
| One trial per mobile | `licence_contact_has_trial()` on `registerTrial.php` |
| Trial starts on **first device bind**, not reinstall | `licence_on_device_bind()` sets `trialStartedAt`, `expiryDate = today + 7 days` |
| Trial cannot restart | `trialConsumed=1` after expiry or 50 bills; login rejected |

---

## Plans (validity tiers)

| Plan | Days | Label |
|------|------|-------|
| Trial | 7 | Demo / Trial (server-forced) |
| 6 months | 183 | `6 Months` |
| 1 year | 365 | `1 Year` |
| 3 years | 1095 | `3 Years` |
| 5 years | 1825 | `5 Years` |
| Lifetime | 10958 | `Lifetime` |

Same-key upgrade: `licence_same_key_upgrade()` / `updateCustomerLicenceDetails.php` — never rotates `licenseKey` or clears device bind.

**No payment gateway** — renewals remain manual (Dealer/Admin records `paymentStatus` e.g. `cash`).

---

## Signed license payload

### Signing (server only)

- **Private key:** `API/license_signing_private.pem` or env `LICENSE_SIGNING_PRIVATE_KEY_PATH`
- **Algorithm:** RSA-SHA256 over canonical JSON (sorted keys, no whitespace)
- **Public key in APK:** `WithTable/app/src/main/assets/license_signing_public.pem`

### Payload fields (inside base64 `licensePayload`)

| Field | Type | Description |
|-------|------|-------------|
| `payloadVersion` | int | Currently `1` |
| `organizationId` | string | `users.id` |
| `branchId` | string | `licenses.id` |
| `branchLabel` | string | e.g. `Main Store`, `Franchise: …` |
| `licenseId` | string | Same as `branchId` |
| `deviceId` | string | Bound `android_device_id` |
| `licenseKey` | string | Opaque licence key |
| `licenseType` | string | Demo / Regular / … |
| `isTrial` | 0\|1 | Trial flag |
| `trialMaxBills` | int | Server constant `50` |
| `trialBillCount` | int | `COUNT(invoice)` for licence |
| `trialConsumed` | 0\|1 | Anti-restart flag |
| `expiryDate` | string | `Y-m-d` |
| `issuedAt` | int | Unix seconds (server clock) |
| `offlineGraceUntil` | int | Unix seconds (`issuedAt + 14 days`) |
| `fastBilling`, `takeAway`, `dineIn`, `mess` | int | Feature flags |

### Response fields (all auth refresh endpoints)

| Field | Type | Description |
|-------|------|-------------|
| `licensePayload` | string | Base64 canonical JSON |
| `licenseSignature` | string | Base64 RSA-SHA256 signature |
| `issuedAt` | string | Echo of payload |
| `offlineGraceUntil` | string | Echo of payload |
| `organizationId` | string | Organization id |
| `branchId` | string | Branch / licence id |
| `branchLabel` | string | Display label |

---

## API endpoints

### 1. `POST Login.php` (existing — extended)

**Purpose:** Licence key + device pre-check before bind.

| Param | Required |
|-------|----------|
| `app_licence_key` | yes |
| `android_device_id` | yes |

**Status codes:** `1` same device, `2` unbound, `3` other device, `0` fail (incl. trial consumed).

**Trial rule:** Rejects when `trialConsumed=1` on Demo/Trial.

---

### 2. `POST updateAndroidKey.php` (existing — extended)

**Purpose:** Bind device; **starts trial clock** on first bind.

| Param | Required |
|-------|----------|
| `app_licence_key` | yes |
| `androidId` | yes |
| `android_device_name` | yes |

**Response:**

```json
{
  "status": "1",
  "message": "Device bound successfully",
  "expiryDate": "2026-08-30",
  "isTrial": "1"
}
```

**Errors:** `Registration required before trial`, `Trial already used on this licence`.

---

### 3. `POST check_licence_expire.php` (existing — extended)

**Purpose:** Session validation + **issue signed payload** + Bearer token.

| Param | Required |
|-------|----------|
| `userId` | yes (licence id) |
| `android_device_id` | yes |

**On success:** Full session fields + trial metadata + `licensePayload` + `licenseSignature` + `authToken`.

---

### 4. `POST LoginMpin.php` (existing — extended)

Same signed payload append as `check_licence_expire.php` on status `1`.

---

### 5. `POST refreshLicensePayload.php` (new)

**Purpose:** Online-only payload refresh without full UI login flow.

| Param | Required |
|-------|----------|
| `userId` | yes |
| `android_device_id` | yes |

**Auth:** Bearer token (`pos_licence`) via `auth_pos_licence_id_from_request`.

**Response (success):**

```json
{
  "status": "1",
  "message": "License payload refreshed.",
  "licenceId": "123",
  "ownerId": "45",
  "licence_key_expire_date": "2027-01-01",
  "licensePayload": "...",
  "licenseSignature": "...",
  "issuedAt": "1755960000",
  "offlineGraceUntil": "1757179200",
  "authToken": "..."
}
```

---

### 6. `POST insertInvoice.php` (existing — extended)

**Trial gate:** Blocks insert when trial bill count ≥ 50; sets `trialConsumed=1`.

---

### 7. `POST registerTrial.php` (new)

**Purpose:** Self-service POS signup — creates customer + 7-day Demo licence.

| Param | Required |
|-------|----------|
| `name` | yes |
| `contact_number` | yes (10-digit mobile) |
| `address` | yes |
| `shopName` | yes |

**Response (success):**

```json
{
  "status": "1",
  "message": "7-day trial licence created. Login with your licence key. Trial starts when you bind this device.",
  "licenceKey": "aBc12XyZ9q",
  "licenceId": "123",
  "trialDays": "7",
  "trialMaxBills": "50"
}
```

**Errors:** duplicate mobile trial, invalid fields, key generation failure.

---

### 8. Dealer/Admin renew (existing)

`POST Dealer/updateCustomerLicenceDetails.php` / `Admin/updateCustomerLicenceDetails.php`  
Uses `licence_same_key_upgrade()` — **same license key**, extends `expiryDate`, preserves device bind.

---

## Offline validation (Android)

Implemented in `LicenseValidator.java`:

1. Verify RSA signature with embedded public key.
2. Confirm `deviceId` and `licenseKey` match local session.
3. **Date manipulation:** track `licenseLastServerTimeMs` from `issuedAt`; reject if device clock rolls back > 24h.
4. **Trusted time:** `max(deviceNow, lastServerTime)`.
5. Allow billing while `trustedNow ≤ offlineGraceUntil` and `expiryDate` valid — **network not required**.
6. Trial bill cap enforced locally using `max(localBillCount, payload.trialBillCount)`.

Network failure on `LicenceKeyReceiver` does **not** block billing if local payload still valid.

---

## Database migration

Run once:

```bash
mysql -u USER -p DATABASE < API/migrations/p6_production_licensing.sql
```

Or use `API/migrations/server_upgrade_all.sql` (includes Step 7).

**New columns on `licenses`:**

| Column | Purpose |
|--------|---------|
| `trialStartedAt` | First device bind timestamp |
| `trialConsumed` | Trial permanently used |
| `deviceBoundAt` | First bind timestamp |

---

## Server deploy checklist

1. Run migration `p6_production_licensing.sql`.
2. Generate production key pair:
   ```bash
   openssl genrsa -out license_signing_private.pem 2048
   openssl rsa -in license_signing_private.pem -pubout -out license_signing_public.pem
   ```
3. Place **private** key at `API/license_signing_private.pem` (gitignored) or set `LICENSE_SIGNING_PRIVATE_KEY_PATH`.
4. Replace APK asset `WithTable/app/src/main/assets/license_signing_public.pem` with matching **public** key; rebuild POS.
5. Deploy updated PHP: `licence_expiry.php`, `licence_payload.php`, `Login.php`, `LoginMpin.php`, `check_licence_expire.php`, `updateAndroidKey.php`, `refreshLicensePayload.php`, `insertInvoice.php`, `registerTrial.php`.

---

## Security notes

- Private signing key **never** in APK or git.
- One active licence = one `android_device_id`.
- Trial limits (7 days, 50 bills) live on server constants in `licence_expiry.php` — not hard-coded in APK.
- Reinstall / clear data cannot reset trial — state is on `licenses` row + invoice count.

---

*End of document.*
