# FCM setup (POS Billingwala)

## Firebase project (fixed)

From [`WithTable/app/google-services.json`](../WithTable/app/google-services.json):

| Field | Value |
|-------|--------|
| project_id | `pos-billingwala` |
| project_number | `855823167459` |

Do **not** create a second Firebase project. All apps share this project.

| App | Package | google-services.json |
|-----|---------|----------------------|
| POS | `com.pos_billingwala` | `WithTable/app/` |
| Owner | `com.posbillingwala.owner` | `Owner/app/` |
| Dealer | `com.posbillingwala.dealer` | `Dealer/app/` |
| Admin | `com.posbillingwala.admin` | `Admin/app/` — **add Android app in Firebase Console**, then replace the placeholder JSON |

## Server (send)

1. Firebase Console → **pos-billingwala** → Project settings → Service accounts  
2. **Generate new private key**  
3. Upload as `API/firebase-service-account.json` on the server  
4. In `API/db_local.php` set:

```php
$fcmProjectId = 'pos-billingwala';
```

5. Run migration (or rely on `fcm_ensure_schema()`):

```bash
mysql … < API/migrations/p25_fcm_device_tokens.sql
```

## Token storage

| App | Storage |
|-----|---------|
| POS | `licenses.fcm_token` |
| Owner / Dealer / Admin | `fcm_device_tokens` (`app_type` + `account_id` = `users.id`) |

Register endpoints:

- POS: `API/registerFcmToken.php`
- Owner: `API/Owner/registerFcmToken.php`
- Dealer: `API/Dealer/registerFcmToken.php`
- Admin: `API/Admin/registerFcmToken.php`

## Receive (all Android apps)

- Register FCM token after login; clear on logout
- Channel: `pos_push_alerts`
- POS also handles silent `mess.token.created` for Mess auto-print

## Send (Admin Android + Admin web)

Audience: `pos` | `owner` | `dealer` | `admin` | `all`

- Admin app → **Send Push** → `API/Admin/sendPushNotification.php`
- Admin web → **Push notifications** → same `fcm_broadcast_promotional(..., $audience)`

On production, Admin Laravel looks for FCM under:

`/home/rgusomuk/posbillingwala.com/androidApp/`

Optional override in admin `.env`:

```env
ANDROID_APP_PATH=/home/rgusomuk/posbillingwala.com/androidApp
```

That folder must contain `fcm_helper.php`, `db_connection.php`, and `firebase-service-account.json`.

POS licence filter (`active` / `all` / `license_ids`) applies only when audience includes POS.

If send fails with “FCM not configured”, the service-account JSON is missing on the server.

## Web Push VAPID key (browsers only)

Firebase Console → Project settings → Cloud Messaging → **Web Push certificates** shows a public key (starts with `B…`).

Set in `API/db_local.php`:

```php
$fcmWebPushVapidKey = 'B….';
```

Use it only in browser JS: `getToken(messaging, { vapidKey: '…' })`.

**Not used** for Admin Android / Admin web “Send Push” to POS/Owner/Dealer/Admin apps — those use `firebase-service-account.json`.
