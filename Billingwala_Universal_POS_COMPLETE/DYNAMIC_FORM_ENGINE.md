# DYNAMIC_FORM_ENGINE

## Rule

Reuse **Product Service Engine** / existing `AddProduct` / `UpdateProduct`. Do not duplicate forms per business.

## Classes

- `ProductFieldConfig` / `ProductFormConfig` / `FormConfiguration`  
- `ProductFormResolver`  
- `DynamicUiComponents.shouldShowProductField`

## Common fields

Name, Category, Subcategory, SKU, Barcode, Price, Tax, Description, Image

## Dynamic fields (by type / feature)

| Vertical | Extra fields |
|----------|----------------|
| Restaurant / Mess | Veg/Non-veg, Kitchen route, Prep time, Portion |
| Bar | ML, Bottle conversion, Bar route |
| Weight fresh | Unit, Weight, Rate/kg |
| Fashion / Jewellery | Size, Color, Variants, Brand |
| Salon-like | Duration, Staff |
| Electronics / Repair | Serial, Warranty |
| Bakery | Weight, Flavour, Custom order support |

Call sites should wrap optional XML rows with `DynamicUiEngine.isProductFieldVisible` when migrating field-by-field.
