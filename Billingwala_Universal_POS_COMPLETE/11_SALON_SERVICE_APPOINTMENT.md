# 11 Salon Service Appointment

Salon / beauty / spa (+ laundry/car wash/repair map). Local appointment queue; bill via Fast Billing.

## Facade
`Extra/SalonAppointmentModule.java` — `APPOINTMENTS`, book dialog, **day + week calendar**.

## Wired (POS)
- DB `service_appointment` (local; `appointmentNetworkStatus`)
- Optional **staff** from local `staff_user` roster on book
- CreatePos book sheet; Master Data long-press → calendar
- Cloud sync: `UserSynchronizeData` → `insertServiceAppointment.php`

## Server
| File | Role |
|------|------|
| `API/insertServiceAppointment.php` | Upsert by `userId` + `localAppointmentId` |
| `API/getServiceAppointmentList.php` | Download list for POS fetch chain |
| `API/Owner/getServiceAppointmentList.php` | Owner read-only list by `licenceId` |
| `API/Owner/updateServiceAppointmentStatus.php` | Owner status update (`booked`/`done`/`cancelled`) |
| `API/migrations/p27_service_appointment.sql` | `service_appointments` table |

POS: upload on cloud sync; download via `ServiceAppointmentWorker`. Mark `synced` only when upload `status == "1"`.

## Owner UI
**Settings → Outlet appointments** — day/week calendar for a franchise outlet.
- Tap row → set status (`booked` / `done` / `cancelled`) via `updateServiceAppointmentStatus.php`
- Long-press in week view → jump to that day
- New bookings still created on POS; status changes sync to POS on Fetch Data
