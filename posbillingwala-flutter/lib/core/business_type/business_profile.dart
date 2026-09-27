import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/business_type/app_feature.dart';
import 'package:pos_billingwala_v2/core/business_type/business_type.dart';

/* Dynamic labels that change with business type (Product vs Menu, etc.). */
class BusinessTerminology {
  const BusinessTerminology({
    this.product = 'Product',
    this.products = 'Products',
    this.category = 'Category',
    this.categories = 'Categories',
    this.subcategory = 'Subcategory',
    this.subcategories = 'Subcategories',
    this.customer = 'Customer',
    this.customers = 'Customers',
    this.order = 'Order',
    this.orders = 'Orders',
    this.invoice = 'Invoice',
    this.billing = 'Billing',
    this.inventory = 'Inventory',
    this.staff = 'Staff',
  });

  final String product;
  final String products;
  final String category;
  final String categories;
  final String subcategory;
  final String subcategories;
  final String customer;
  final String customers;
  final String order;
  final String orders;
  final String invoice;
  final String billing;
  final String inventory;
  final String staff;

  static const retail = BusinessTerminology();

  static const restaurant = BusinessTerminology(
    product: 'Menu Item',
    products: 'Menu',
    category: 'Cuisine / Group',
    categories: 'Menu Groups',
    subcategory: 'Course',
    subcategories: 'Courses',
    customer: 'Guest',
    customers: 'Guests',
    order: 'Order',
    orders: 'Orders',
    invoice: 'Bill',
    billing: 'Restaurant Billing',
    inventory: 'Kitchen Stock',
    staff: 'Staff',
  );

  static const hotel = BusinessTerminology(
    product: 'Service / Item',
    products: 'Services & Menu',
    category: 'Category',
    categories: 'Categories',
    subcategory: 'Subcategory',
    subcategories: 'Subcategories',
    customer: 'Guest',
    customers: 'Guests',
    order: 'Booking / Order',
    orders: 'Bookings',
    invoice: 'Folio / Bill',
    billing: 'Hotel Billing',
    inventory: 'Inventory',
    staff: 'Staff',
  );

  static const clothing = BusinessTerminology(
    product: 'Style / SKU',
    products: 'Styles',
    category: 'Category',
    categories: 'Categories',
    subcategory: 'Subcategory',
    subcategories: 'Subcategories',
    customer: 'Customer',
    customers: 'Customers',
    order: 'Sale',
    orders: 'Sales',
    invoice: 'Invoice',
    billing: 'Apparel Billing',
    inventory: 'Stock Matrix',
    staff: 'Staff',
  );

  static const pharmacy = BusinessTerminology(
    product: 'Medicine',
    products: 'Medicines',
    category: 'Therapeutic Class',
    categories: 'Classes',
    subcategory: 'Form',
    subcategories: 'Forms',
    customer: 'Patient / Customer',
    customers: 'Customers',
    order: 'Sale',
    orders: 'Sales',
    invoice: 'Invoice',
    billing: 'Pharmacy Billing',
    inventory: 'Drug Stock',
    staff: 'Staff',
  );
}

/* One actionable tile on Home / dashboard. */
class BusinessHomeAction {
  const BusinessHomeAction({
    required this.id,
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.route,
    required this.feature,
    this.permission,
    this.colors = const [Color(0xFF168BFF), Color(0xFF0756C9)],
  });

  final String id;
  final String title;
  final String subtitle;
  final IconData icon;
  final String route;
  final AppFeature feature;
  final String? permission;
  final List<Color> colors;
}

/* Side-nav / bottom-nav destination driven by business profile. */
class BusinessNavItem {
  const BusinessNavItem({
    required this.id,
    required this.label,
    required this.icon,
    required this.route,
    this.feature,
    this.permission,
    this.aliases = const [],
  });

  final String id;
  final String label;
  final IconData icon;
  final String route;
  final AppFeature? feature;
  final String? permission;
  final List<String> aliases;
}

/* Master-data entry filtered by business profile. */
class BusinessMasterItem {
  const BusinessMasterItem({
    required this.id,
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.route,
    required this.feature,
    this.color = const Color(0xFF0756C9),
  });

  final String id;
  final String title;
  final String subtitle;
  final IconData icon;
  final String route;
  final AppFeature feature;
  final Color color;
}

/* Immutable profile describing UI + modules for one business type. */
class BusinessProfile {
  const BusinessProfile({
    required this.type,
    required this.features,
    required this.terminology,
    required this.homeActions,
    required this.navItems,
    required this.masterItems,
    this.accent = const Color(0xFF0756C9),
  });

  final BusinessType type;
  final Set<AppFeature> features;
  final BusinessTerminology terminology;
  final List<BusinessHomeAction> homeActions;
  final List<BusinessNavItem> navItems;
  final List<BusinessMasterItem> masterItems;
  final Color accent;

  bool has(AppFeature feature) => features.contains(feature);

  bool hasAny(Iterable<AppFeature> list) => list.any(has);

  bool hasAll(Iterable<AppFeature> list) => list.every(has);
}
