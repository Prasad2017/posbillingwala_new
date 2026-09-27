/* Supported business verticals for the unified POS Billingwala platform. */
enum BusinessType {
  retailStore,
  clothingStore,
  supermarket,
  departmentStore,
  groceryStore,
  electronicsHardware,
  hotel,
  restaurant,
  cafe,
  cakeShopBakery,
  bar,
  coldDrinksBeverage,
  pharmacy,
  footwear,
  cosmetics,
  homeKitchen,
  other,
}

extension BusinessTypeX on BusinessType {
  String get id => switch (this) {
    BusinessType.retailStore => 'retail_store',
    BusinessType.clothingStore => 'clothing_store',
    BusinessType.supermarket => 'supermarket',
    BusinessType.departmentStore => 'department_store',
    BusinessType.groceryStore => 'grocery_store',
    BusinessType.electronicsHardware => 'electronics_hardware',
    BusinessType.hotel => 'hotel',
    BusinessType.restaurant => 'restaurant',
    BusinessType.cafe => 'cafe',
    BusinessType.cakeShopBakery => 'cake_shop_bakery',
    BusinessType.bar => 'bar',
    BusinessType.coldDrinksBeverage => 'cold_drinks_beverage',
    BusinessType.pharmacy => 'pharmacy',
    BusinessType.footwear => 'footwear',
    BusinessType.cosmetics => 'cosmetics',
    BusinessType.homeKitchen => 'home_kitchen',
    BusinessType.other => 'other',
  };

  String get displayName => switch (this) {
    BusinessType.retailStore => 'Retail Store',
    BusinessType.clothingStore => 'Clothing Store',
    BusinessType.supermarket => 'Supermarket / Large Retail',
    BusinessType.departmentStore => 'Department Store',
    BusinessType.groceryStore => 'Grocery Store',
    BusinessType.electronicsHardware => 'Electronics / Hardware',
    BusinessType.hotel => 'Hotel',
    BusinessType.restaurant => 'Restaurant',
    BusinessType.cafe => 'Cafe',
    BusinessType.cakeShopBakery => 'Cake Shop / Bakery',
    BusinessType.bar => 'Bar',
    BusinessType.coldDrinksBeverage => 'Cold Drinks / Beverage',
    BusinessType.pharmacy => 'Pharmacy',
    BusinessType.footwear => 'Footwear',
    BusinessType.cosmetics => 'Cosmetics',
    BusinessType.homeKitchen => 'Home & Kitchen',
    BusinessType.other => 'Other / Custom',
  };

  String get shortName => switch (this) {
    BusinessType.retailStore => 'Retail',
    BusinessType.clothingStore => 'Clothing',
    BusinessType.supermarket => 'Supermarket',
    BusinessType.departmentStore => 'Department',
    BusinessType.groceryStore => 'Grocery',
    BusinessType.electronicsHardware => 'Electronics',
    BusinessType.hotel => 'Hotel',
    BusinessType.restaurant => 'Restaurant',
    BusinessType.cafe => 'Cafe',
    BusinessType.cakeShopBakery => 'Bakery',
    BusinessType.bar => 'Bar',
    BusinessType.coldDrinksBeverage => 'Beverage',
    BusinessType.pharmacy => 'Pharmacy',
    BusinessType.footwear => 'Footwear',
    BusinessType.cosmetics => 'Cosmetics',
    BusinessType.homeKitchen => 'Home',
    BusinessType.other => 'Custom',
  };

  String get description => switch (this) {
    BusinessType.retailStore =>
      'Barcode billing, inventory, purchase, offers and stock control.',
    BusinessType.clothingStore =>
      'Size, color, variants, brand, fabric, exchange and stock matrix.',
    BusinessType.supermarket =>
      'High-volume barcode billing, batch, expiry, warehouse and reorder.',
    BusinessType.departmentStore =>
      'Multi-category retail with brands, variants and transfers.',
    BusinessType.groceryStore =>
      'Fast billing with units, weight, batch and expiry tracking.',
    BusinessType.electronicsHardware =>
      'Serial numbers, warranty-ready SKUs and barcode inventory.',
    BusinessType.hotel =>
      'Rooms, booking, check-in/out, guest ledger and restaurant/KOT.',
    BusinessType.restaurant =>
      'Tables, KOT, dine-in, takeaway, parcel, portions and waiters.',
    BusinessType.cafe =>
      'Quick service, takeaway, portions and compact menu billing.',
    BusinessType.cakeShopBakery =>
      'Counter billing, custom orders, portions and takeaway.',
    BusinessType.bar =>
      'Bar billing, KOT routing, tables and beverage portions.',
    BusinessType.coldDrinksBeverage =>
      'Fast counter billing with packs, chill stock and barcode.',
    BusinessType.pharmacy =>
      'Batch, expiry, schedule-aware catalog and prescription notes.',
    BusinessType.footwear =>
      'Size matrix, color variants, brand and exchange flows.',
    BusinessType.cosmetics =>
      'Brand, shade/variant, barcode and loyalty-friendly billing.',
    BusinessType.homeKitchen =>
      'Retail catalog with brands, units and warehouse stock.',
    BusinessType.other =>
      'Start from a flexible retail base and enable modules as needed.',
  };

  bool get isFoodService => switch (this) {
    BusinessType.hotel ||
    BusinessType.restaurant ||
    BusinessType.cafe ||
    BusinessType.cakeShopBakery ||
    BusinessType.bar =>
      true,
    _ => false,
  };

  bool get isApparelLike => switch (this) {
    BusinessType.clothingStore || BusinessType.footwear => true,
    _ => false,
  };

  bool get isRetailLike => !isFoodService;

  static BusinessType? tryParse(String? raw) {
    final value = raw?.trim().toLowerCase() ?? '';
    if (value.isEmpty) return null;
    for (final type in BusinessType.values) {
      if (type.id == value ||
          type.name.toLowerCase() == value ||
          type.displayName.toLowerCase() == value) {
        return type;
      }
    }
    return switch (value) {
      'retail' || 'shop' || 'store' => BusinessType.retailStore,
      'clothing' || 'apparel' || 'fashion' => BusinessType.clothingStore,
      'dmart' || 'hypermarket' || 'super_market' => BusinessType.supermarket,
      'grocery' || 'kirana' => BusinessType.groceryStore,
      'electronics' || 'hardware' || 'mobile' =>
        BusinessType.electronicsHardware,
      'resto' || 'f&b' || 'fnb' => BusinessType.restaurant,
      'bakery' || 'cake' || 'cake_shop' => BusinessType.cakeShopBakery,
      'beverage' || 'cold_drinks' || 'drinks' =>
        BusinessType.coldDrinksBeverage,
      'medical' || 'medical_store' => BusinessType.pharmacy,
      'shoes' || 'shoe' => BusinessType.footwear,
      'beauty' => BusinessType.cosmetics,
      'home' || 'kitchen' || 'home_and_kitchen' => BusinessType.homeKitchen,
      'custom' => BusinessType.other,
      _ => null,
    };
  }
}
