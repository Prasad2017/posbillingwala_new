# UI_TESTING_PLAN

## Business switch (no reinstall)

Settings → Business Template / type → apply → return Home.

Expect Dashboard, Navigation codes, Quick Actions, Billing barcode, Settings scale/inventory, Reports rows to match new template.

## Feature enable / disable

Toggle via template change or company GST/tables/KOT where applicable. Hidden rows must not crash.

## Role change

Owner → Cashier → Waiter: Settings elevated rows soft-hide; billing remains for waiter; reports PIN still required.

## Offline

Airplane mode after one online resolve: Home still shows last-good / fallback modules; billing must open.

## Checklist

- [ ] Business change  
- [ ] Feature enable / disable  
- [ ] Role / permission change  
- [ ] Offline mode + cache  
- [ ] Dashboard / Navigation / Quick Actions  
- [ ] Billing UI / Product form fields  
- [ ] Settings / Reports  
- [ ] Printer / Tables / KOT / BOT  
- [ ] Weight / Variants / Appointments / Custom Orders  

## Business matrix (smoke)

Restaurant · Bar · Bar+Restaurant · Mess · Fish · Meat · Vegetable · Fruit · Retail · Grocery · Clothing · Footwear · Salon · Beauty · Spa · Cake · Bakery · Electronics · Mobile · Hardware · Repair · Rental · Custom  

Use Settings diagnostics (`UniversalPosModules.fullDiagnostics`) to confirm Dynamic UI summary line.
