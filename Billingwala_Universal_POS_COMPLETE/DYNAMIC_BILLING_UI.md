# DYNAMIC_BILLING_UI

## Rule

Reuse **Universal Billing Engine**. Dynamic UI only declares fields/sections.

## Classes

- `BillingFieldConfig` / `BillingSectionConfig` / `BillingUIConfig`  
- `BillingUIResolver`

## Fields (`UiCodes.BF_*`)

Product, Category, Barcode, Weight, Unit, Rate, Table, KOT, Portion, Variant, Size, Color, Customer, Service, Staff, Appointment, Duration, Custom Fields, Delivery Date, Advance, Serial.

## Sections

CATALOG · CART · TABLE · WEIGHT · VARIANT · SERVICE · CUSTOM_ORDER

## Wired today

`CreatePos` barcode camera / scan path requires `BF_BARCODE` visible (template ∩ FeatureEngine).

Weight / variant / salon / bakery prompts remain in existing modules; config exposes whether those fields should be emphasized for the active business.
