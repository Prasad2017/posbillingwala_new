# Mess Common QR — End-to-End Verification Checklist

Deploy `API/migrations/p24_mess_common_qr.sql` (or run `server_upgrade_all.sql` / rely on `mess_common_ensure_schema()`).

## Happy path (§47)
1. POS Mess → QR Management → Generate QR (marks this device as print device).
2. Share / Download / Print poster; display QR at counter.
3. Phone camera scans QR → `mess_q.php?t=…` opens (no app).
4. Enter valid Registration No. → Get Token.
5. Backend creates `mess_meal_token` (PRINT_PENDING), sends FCM `mess.token.created`.
6. POS enqueues locally → single print worker → existing Bluetooth printer.
7. POS ACK → backend PRINTED.
8. Customer page shows token number immediately (does not wait for print).

## Duplicate (§48)
Same Reg + same meal twice → one DB row; second response `alreadyGenerated`; one print job.

## Printer failure (§49)
Disconnect printer → token PRINT_FAILED locally/server; Retry Print reprints same token; no new token.

## POS offline (§50)
Token created while POS offline stays PRINT_PENDING; on Mess open / reconnect, `mess_meal_token_pending` recovers; print + ACK; no duplicate.

## QR inactive (§51)
Deactivate QR → customer sees “This QR code is currently inactive.”

## Wrong member / branch (§52)
Unknown or other-branch registration → “Registration number not found.” (no enumeration details).

## Concurrent (§53)
Parallel POSTs same Reg/date/session → unique constraint + transaction → one token.

## Owner
Home → **Mess QR Management** → select outlet → Generate / Share / Download / Regenerate / Deactivate.
Home → Mess Token Today → Lunch/Dinner Generated / Printed / Pending / Failed.

Note: Owner generate does **not** set the print device. Open POS Mess → QR Management once so the POS device links as printer.
