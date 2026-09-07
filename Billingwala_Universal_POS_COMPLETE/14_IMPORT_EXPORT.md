# 14 Import Export

## Catalog Excel (cloud)
Owner / Dealer / Admin: `CatalogImportExportHelper` — server-side Excel for multi-outlet catalogs.

## POS local CSV (WithTable)
- Master Data → **Catalog CSV**
- Export / import product rows as CSV (opens in Excel / Sheets)
- **Additive import only** — never deletes existing products
- Match by `productCode`, else name+category; creates missing categories
- Gated by staff `MASTER_DATA` / report PIN

## Classes
| Class | Role |
|-------|------|
| `ImportExportEngine` | Facade + Master Data menu |
| `CatalogCsvHelper` | CSV read/write + share / file pick |

## Columns
`categoryName,productCode,productName,productPrice,productUnit,productCGST,productSGST,openPrice`

## POS reports
`Utils.ReportToSpreadsheet` for report export (unchanged).

## Safe rules
Do not wipe local catalog on failed import; cloud Excel remains Owner/Dealer/Admin path.
