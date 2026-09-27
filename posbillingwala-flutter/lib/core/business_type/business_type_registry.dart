import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/business_type/app_feature.dart';
import 'package:pos_billingwala_v2/core/business_type/business_profile.dart';
import 'package:pos_billingwala_v2/core/business_type/business_type.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';

/* Maps each [BusinessType] → [BusinessProfile]. Add new types here only. */
abstract final class BusinessTypeRegistry {
  static BusinessProfile of(BusinessType type) =>
      _profiles[type] ?? _profiles[BusinessType.retailStore]!;

  static BusinessType inferFromLicence({
    required bool fastBilling,
    required bool dineIn,
    required bool takeAway,
    required bool mess,
  }) {
    final food = dineIn || takeAway || mess;
    if (food) return BusinessType.restaurant;
    if (fastBilling) return BusinessType.retailStore;
    /* Legacy licences with no flags → keep existing F&B default. */
    return BusinessType.restaurant;
  }

  static const _coreBilling = {
    AppFeature.productGridBilling,
    AppFeature.quickBilling,
    AppFeature.holdResumeInvoice,
    AppFeature.splitPayment,
    AppFeature.creditSale,
    AppFeature.returns,
    AppFeature.refunds,
    AppFeature.coupons,
    AppFeature.offers,
    AppFeature.customers,
    AppFeature.expenses,
    AppFeature.reports,
    AppFeature.inventory,
    AppFeature.multiBranch,
    AppFeature.multiCounter,
    AppFeature.approvals,
    AppFeature.auditLog,
  };

  static const _retailExtra = {
    AppFeature.barcodeBilling,
    AppFeature.purchaseFlow,
    AppFeature.vendors,
    AppFeature.reorderLevels,
    AppFeature.warehouses,
    AppFeature.stockTransfer,
    AppFeature.multiWarehouse,
    AppFeature.loyalty,
    AppFeature.combos,
  };

  static const _foodExtra = {
    AppFeature.tables,
    AppFeature.floors,
    AppFeature.kot,
    AppFeature.dineIn,
    AppFeature.takeaway,
    AppFeature.parcel,
    AppFeature.userWiseBilling,
    AppFeature.portions,
    AppFeature.modifiers,
    AppFeature.kitchenDepartments,
    AppFeature.waiterAssign,
    AppFeature.combos,
  };

  static final Map<BusinessType, BusinessProfile> _profiles = {
    BusinessType.retailStore: _retail(
      BusinessType.retailStore,
      extra: {AppFeature.weightUnits, AppFeature.brands},
    ),
    BusinessType.clothingStore: _apparel(BusinessType.clothingStore),
    BusinessType.supermarket: _retail(
      BusinessType.supermarket,
      extra: {
        AppFeature.batchExpiry,
        AppFeature.weightUnits,
        AppFeature.brands,
        AppFeature.wallet,
        AppFeature.membership,
      },
    ),
    BusinessType.departmentStore: _retail(
      BusinessType.departmentStore,
      extra: {
        AppFeature.brands,
        AppFeature.variants,
        AppFeature.exchange,
        AppFeature.batchExpiry,
      },
    ),
    BusinessType.groceryStore: _retail(
      BusinessType.groceryStore,
      extra: {
        AppFeature.batchExpiry,
        AppFeature.weightUnits,
        AppFeature.brands,
      },
    ),
    BusinessType.electronicsHardware: _retail(
      BusinessType.electronicsHardware,
      extra: {
        AppFeature.serialNumbers,
        AppFeature.brands,
        AppFeature.exchange,
      },
    ),
    BusinessType.hotel: _hotel(),
    BusinessType.restaurant: _food(BusinessType.restaurant, mess: true),
    BusinessType.cafe: _food(
      BusinessType.cafe,
      mess: false,
    ),
    BusinessType.cakeShopBakery: _food(
      BusinessType.cakeShopBakery,
      mess: false,
      counterOnly: true,
    ),
    BusinessType.bar: _food(BusinessType.bar, mess: false),
    BusinessType.coldDrinksBeverage: _retail(
      BusinessType.coldDrinksBeverage,
      extra: {AppFeature.weightUnits, AppFeature.brands, AppFeature.portions},
      terminology: BusinessTerminology.retail,
    ),
    BusinessType.pharmacy: _retail(
      BusinessType.pharmacy,
      extra: {
        AppFeature.batchExpiry,
        AppFeature.pharmacySchedule,
        AppFeature.brands,
      },
      terminology: BusinessTerminology.pharmacy,
    ),
    BusinessType.footwear: _apparel(
      BusinessType.footwear,
      fabric: false,
    ),
    BusinessType.cosmetics: _retail(
      BusinessType.cosmetics,
      extra: {
        AppFeature.brands,
        AppFeature.variants,
        AppFeature.batchExpiry,
        AppFeature.loyalty,
        AppFeature.membership,
      },
    ),
    BusinessType.homeKitchen: _retail(
      BusinessType.homeKitchen,
      extra: {AppFeature.brands, AppFeature.weightUnits},
    ),
    BusinessType.other: _retail(
      BusinessType.other,
      extra: {AppFeature.brands, AppFeature.weightUnits},
    ),
  };

  static BusinessProfile _retail(
    BusinessType type, {
    Set<AppFeature> extra = const {},
    BusinessTerminology terminology = BusinessTerminology.retail,
  }) {
    final features = {..._coreBilling, ..._retailExtra, ...extra};
    return BusinessProfile(
      type: type,
      features: features,
      terminology: terminology,
      homeActions: _retailHomeActions(features),
      navItems: _retailNav(features),
      masterItems: _retailMasters(features, terminology),
    );
  }

  static BusinessProfile _apparel(
    BusinessType type, {
    bool fabric = true,
  }) {
    final extra = <AppFeature>{
      AppFeature.sizeColorMatrix,
      AppFeature.variants,
      AppFeature.brands,
      AppFeature.exchange,
      if (fabric) AppFeature.fabric,
    };
    final features = {..._coreBilling, ..._retailExtra, ...extra};
    return BusinessProfile(
      type: type,
      features: features,
      terminology: BusinessTerminology.clothing,
      homeActions: [
        ..._retailHomeActions(features),
        BusinessHomeAction(
          id: 'variants',
          title: 'Size & Color',
          subtitle: 'Stock matrix and variant SKUs',
          icon: Icons.grid_view_rounded,
          route: '/masters/variants',
          feature: AppFeature.sizeColorMatrix,
          permission: 'product.view',
          colors: const [Color(0xFF5B6CFF), Color(0xFF3B4ED8)],
        ),
        BusinessHomeAction(
          id: 'exchange',
          title: 'Exchange',
          subtitle: 'Size / color exchange with price difference',
          icon: Icons.swap_horiz_rounded,
          route: '/returns/exchange',
          feature: AppFeature.exchange,
          permission: 'billing.create',
          colors: const [AppColors.orangeLight, AppColors.orange],
        ),
      ],
      navItems: [
        ..._retailNav(features),
        const BusinessNavItem(
          id: 'variants',
          label: 'Variants',
          icon: Icons.grid_view_rounded,
          route: '/masters/variants',
          feature: AppFeature.sizeColorMatrix,
          permission: 'product.view',
        ),
      ],
      masterItems: [
        ..._retailMasters(features, BusinessTerminology.clothing),
        const BusinessMasterItem(
          id: 'brands',
          title: 'Brands',
          subtitle: 'Brand master for apparel SKUs',
          icon: Icons.sell_rounded,
          route: '/masters/brands',
          feature: AppFeature.brands,
          color: Color(0xFF5B6CFF),
        ),
        const BusinessMasterItem(
          id: 'variants',
          title: 'Size / Color Matrix',
          subtitle: 'Sizes, colors and stock matrix',
          icon: Icons.grid_on_rounded,
          route: '/masters/variants',
          feature: AppFeature.sizeColorMatrix,
          color: AppColors.orange,
        ),
      ],
    );
  }

  static BusinessProfile _food(
    BusinessType type, {
    bool mess = false,
    bool counterOnly = false,
  }) {
    final features = {
      ..._coreBilling,
      ..._foodExtra,
      AppFeature.barcodeBilling,
      AppFeature.purchaseFlow,
      AppFeature.vendors,
      AppFeature.loyalty,
      if (mess) AppFeature.mess,
    };
    if (counterOnly) {
      features.remove(AppFeature.tables);
      features.remove(AppFeature.floors);
      features.remove(AppFeature.dineIn);
    }
    return BusinessProfile(
      type: type,
      features: features,
      terminology: BusinessTerminology.restaurant,
      homeActions: _foodHomeActions(features),
      navItems: _foodNav(features),
      masterItems: _foodMasters(features),
      accent: AppColors.primary,
    );
  }

  static BusinessProfile _hotel() {
    final features = {
      ..._coreBilling,
      ..._foodExtra,
      AppFeature.barcodeBilling,
      AppFeature.purchaseFlow,
      AppFeature.vendors,
      AppFeature.loyalty,
      AppFeature.rooms,
      AppFeature.roomBooking,
      AppFeature.checkInOut,
      AppFeature.guestLedger,
      AppFeature.roomRestaurantBilling,
      AppFeature.warehouses,
      AppFeature.stockTransfer,
    };
    return BusinessProfile(
      type: BusinessType.hotel,
      features: features,
      terminology: BusinessTerminology.hotel,
      homeActions: [
        const BusinessHomeAction(
          id: 'rooms',
          title: 'Rooms',
          subtitle: 'Room status, check-in and check-out',
          icon: Icons.hotel_rounded,
          route: '/hotel/rooms',
          feature: AppFeature.rooms,
          permission: 'billing.create',
          colors: [Color(0xFF5B6CFF), Color(0xFF3B4ED8)],
        ),
        const BusinessHomeAction(
          id: 'booking',
          title: 'Bookings',
          subtitle: 'Reservations and guest management',
          icon: Icons.event_available_rounded,
          route: '/hotel/bookings',
          feature: AppFeature.roomBooking,
          permission: 'billing.create',
          colors: [AppColors.cyan, AppColors.primaryDark],
        ),
        const BusinessHomeAction(
          id: 'night-audit',
          title: 'Night audit',
          subtitle: 'Day close and in-house folios',
          icon: Icons.nightlight_round,
          route: '/hotel/night-audit',
          feature: AppFeature.rooms,
          permission: 'report.view',
          colors: [AppColors.navy, Color(0xFF1A2A4A)],
        ),
        ..._foodHomeActions(features),
      ],
      navItems: [
        const BusinessNavItem(
          id: 'home',
          label: 'Home',
          icon: Icons.home_rounded,
          route: '/',
        ),
        const BusinessNavItem(
          id: 'rooms',
          label: 'Rooms',
          icon: Icons.hotel_rounded,
          route: '/hotel/rooms',
          feature: AppFeature.rooms,
          permission: 'billing.create',
        ),
        const BusinessNavItem(
          id: 'bookings',
          label: 'Bookings',
          icon: Icons.event_available_rounded,
          route: '/hotel/bookings',
          feature: AppFeature.roomBooking,
          permission: 'billing.create',
        ),
        ..._foodNav(features).where((n) => n.id != 'home'),
      ],
      masterItems: [
        const BusinessMasterItem(
          id: 'room-types',
          title: 'Room Types',
          subtitle: 'Room categories and base pricing',
          icon: Icons.meeting_room_rounded,
          route: '/hotel/room-types',
          feature: AppFeature.rooms,
          color: Color(0xFF5B6CFF),
        ),
        const BusinessMasterItem(
          id: 'rooms',
          title: 'Room Master',
          subtitle: 'Rooms, floors and status',
          icon: Icons.hotel_rounded,
          route: '/hotel/room-master',
          feature: AppFeature.rooms,
          color: AppColors.primary,
        ),
        ..._foodMasters(features),
      ],
    );
  }

  static List<BusinessHomeAction> _retailHomeActions(Set<AppFeature> f) => [
    if (f.contains(AppFeature.quickBilling) ||
        f.contains(AppFeature.barcodeBilling))
      const BusinessHomeAction(
        id: 'billing',
        title: 'Billing',
        subtitle: 'Barcode / quick counter billing',
        icon: Icons.point_of_sale_rounded,
        route: '/pos',
        feature: AppFeature.quickBilling,
        permission: 'billing.create',
        colors: [AppColors.primaryBright, AppColors.primary],
      ),
    if (f.contains(AppFeature.purchaseFlow))
      const BusinessHomeAction(
        id: 'purchase',
        title: 'Purchase',
        subtitle: 'PO, GRN and vendor invoices',
        icon: Icons.local_shipping_rounded,
        route: '/purchase',
        feature: AppFeature.purchaseFlow,
        permission: 'inventory.view',
        colors: [AppColors.green, Color(0xFF15803D)],
      ),
    if (f.contains(AppFeature.inventory))
      const BusinessHomeAction(
        id: 'inventory',
        title: 'Inventory',
        subtitle: 'Stock, transfers and adjustments',
        icon: Icons.warehouse_rounded,
        route: '/inventory',
        feature: AppFeature.inventory,
        permission: 'inventory.view',
        colors: [AppColors.orangeLight, AppColors.orange],
      ),
    if (f.contains(AppFeature.customers))
      const BusinessHomeAction(
        id: 'customers',
        title: 'Customers',
        subtitle: 'CRM, loyalty and credit',
        icon: Icons.people_alt_rounded,
        route: '/crm/customers',
        feature: AppFeature.customers,
        permission: 'product.view',
        colors: [AppColors.cyan, AppColors.primaryDark],
      ),
    if (f.contains(AppFeature.offers) || f.contains(AppFeature.coupons))
      const BusinessHomeAction(
        id: 'offers',
        title: 'Offers',
        subtitle: 'Coupons and discount campaigns',
        icon: Icons.local_offer_rounded,
        route: '/offers',
        feature: AppFeature.offers,
        permission: 'offer.view',
        colors: [Color(0xFFE91E63), Color(0xFFAD1457)],
      ),
    if (f.contains(AppFeature.approvals))
      const BusinessHomeAction(
        id: 'approvals',
        title: 'Approvals',
        subtitle: 'Purchase and discount approvals',
        icon: Icons.fact_check_rounded,
        route: '/approvals',
        feature: AppFeature.approvals,
        permission: 'approval.view',
        colors: [Color(0xFF00897B), Color(0xFF00695C)],
      ),
  ];

  static List<BusinessHomeAction> _foodHomeActions(Set<AppFeature> f) => [
    if (f.contains(AppFeature.quickBilling))
      const BusinessHomeAction(
        id: 'fast',
        title: 'Fast Billing',
        subtitle: 'Quick billing for walk-in customers',
        icon: Icons.receipt_long_rounded,
        route: '/pos',
        feature: AppFeature.quickBilling,
        permission: 'billing.create',
        colors: [AppColors.primaryBright, AppColors.primary],
      ),
    if (f.contains(AppFeature.dineIn) || f.contains(AppFeature.tables))
      const BusinessHomeAction(
        id: 'dine',
        title: 'Dine In',
        subtitle: 'Tables, KOT and settle bills',
        icon: Icons.table_restaurant_rounded,
        route: '/tables',
        feature: AppFeature.dineIn,
        permission: 'table.view',
        colors: [AppColors.green, Color(0xFF15803D)],
      ),
    if (f.contains(AppFeature.takeaway) || f.contains(AppFeature.parcel))
      const BusinessHomeAction(
        id: 'takeaway',
        title: 'Take Away',
        subtitle: 'Parcel and takeaway orders',
        icon: Icons.shopping_bag_rounded,
        route: '/takeaway',
        feature: AppFeature.takeaway,
        permission: 'takeaway.view',
        colors: [AppColors.orangeLight, AppColors.orange],
      ),
    if (f.contains(AppFeature.mess))
      const BusinessHomeAction(
        id: 'mess',
        title: 'Mess',
        subtitle: 'Manage mess billing easily',
        icon: Icons.restaurant_rounded,
        route: '/mess',
        feature: AppFeature.mess,
        permission: 'mess.view',
        colors: [AppColors.cyan, AppColors.primaryDark],
      ),
  ];

  static List<BusinessNavItem> _retailNav(Set<AppFeature> f) => [
    const BusinessNavItem(
      id: 'home',
      label: 'Home',
      icon: Icons.home_rounded,
      route: '/',
    ),
    if (f.contains(AppFeature.quickBilling) ||
        f.contains(AppFeature.barcodeBilling))
      const BusinessNavItem(
        id: 'billing',
        label: 'Billing',
        icon: Icons.point_of_sale_rounded,
        route: '/pos',
        feature: AppFeature.quickBilling,
        permission: 'billing.create',
      ),
    if (f.contains(AppFeature.purchaseFlow))
      const BusinessNavItem(
        id: 'purchase',
        label: 'Purchase',
        icon: Icons.local_shipping_rounded,
        route: '/purchase',
        feature: AppFeature.purchaseFlow,
        permission: 'inventory.view',
      ),
    const BusinessNavItem(
      id: 'masters',
      label: 'Masters',
      icon: Icons.inventory_2_rounded,
      route: '/masters',
      permission: 'product.view',
    ),
    if (f.contains(AppFeature.inventory))
      const BusinessNavItem(
        id: 'inventory',
        label: 'Inventory',
        icon: Icons.warehouse_rounded,
        route: '/inventory',
        feature: AppFeature.inventory,
        permission: 'inventory.view',
        aliases: ['/expenses'],
      ),
    if (f.contains(AppFeature.customers))
      const BusinessNavItem(
        id: 'customers',
        label: 'Customers',
        icon: Icons.people_alt_rounded,
        route: '/crm/customers',
        feature: AppFeature.customers,
      ),
    if (f.contains(AppFeature.reports))
      const BusinessNavItem(
        id: 'reports',
        label: 'Reports',
        icon: Icons.bar_chart_rounded,
        route: '/reports',
        feature: AppFeature.reports,
        permission: 'report.view',
      ),
    const BusinessNavItem(
      id: 'settings',
      label: 'Settings',
      icon: Icons.settings_rounded,
      route: '/settings',
    ),
  ];

  static List<BusinessNavItem> _foodNav(Set<AppFeature> f) => [
    const BusinessNavItem(
      id: 'home',
      label: 'Home',
      icon: Icons.home_rounded,
      route: '/',
    ),
    if (f.contains(AppFeature.quickBilling))
      const BusinessNavItem(
        id: 'billing',
        label: 'Billing',
        icon: Icons.point_of_sale_rounded,
        route: '/pos',
        feature: AppFeature.quickBilling,
        permission: 'billing.create',
      ),
    if (f.contains(AppFeature.tables) || f.contains(AppFeature.dineIn))
      const BusinessNavItem(
        id: 'tables',
        label: 'Tables',
        icon: Icons.table_restaurant_rounded,
        route: '/tables',
        feature: AppFeature.tables,
        permission: 'table.view',
      ),
    if (f.contains(AppFeature.takeaway) || f.contains(AppFeature.parcel))
      const BusinessNavItem(
        id: 'takeaway',
        label: 'Takeaway',
        icon: Icons.takeout_dining_rounded,
        route: '/takeaway',
        feature: AppFeature.takeaway,
        permission: 'takeaway.view',
      ),
    if (f.contains(AppFeature.mess))
      const BusinessNavItem(
        id: 'mess',
        label: 'Mess',
        icon: Icons.restaurant_rounded,
        route: '/mess',
        feature: AppFeature.mess,
        permission: 'mess.view',
      ),
    const BusinessNavItem(
      id: 'masters',
      label: 'Masters',
      icon: Icons.inventory_2_rounded,
      route: '/masters',
      permission: 'product.view',
    ),
    if (f.contains(AppFeature.inventory))
      const BusinessNavItem(
        id: 'inventory',
        label: 'Inventory',
        icon: Icons.warehouse_rounded,
        route: '/inventory',
        feature: AppFeature.inventory,
        permission: 'inventory.view',
        aliases: ['/expenses'],
      ),
    if (f.contains(AppFeature.reports))
      const BusinessNavItem(
        id: 'reports',
        label: 'Reports',
        icon: Icons.bar_chart_rounded,
        route: '/reports',
        feature: AppFeature.reports,
        permission: 'report.view',
      ),
    const BusinessNavItem(
      id: 'settings',
      label: 'Settings',
      icon: Icons.settings_rounded,
      route: '/settings',
    ),
  ];

  static List<BusinessMasterItem> _retailMasters(
    Set<AppFeature> f,
    BusinessTerminology t,
  ) => [
    BusinessMasterItem(
      id: 'categories',
      title: t.categories,
      subtitle: 'Organize your catalog',
      icon: Icons.category_rounded,
      route: '/masters/categories',
      feature: AppFeature.inventory,
      color: AppColors.primary,
    ),
    BusinessMasterItem(
      id: 'subcategories',
      title: t.subcategories,
      subtitle: 'Refine category groups',
      icon: Icons.folder_rounded,
      route: '/masters/subcategories',
      feature: AppFeature.inventory,
      color: AppColors.purple,
    ),
    BusinessMasterItem(
      id: 'products',
      title: t.products,
      subtitle: 'SKU, barcode, price and tax',
      icon: Icons.inventory_2_rounded,
      route: '/masters/products',
      feature: AppFeature.inventory,
      color: AppColors.green,
    ),
    if (f.contains(AppFeature.combos))
      const BusinessMasterItem(
        id: 'combos',
        title: 'Combos / Bundles',
        subtitle: 'Multi-item packages and offers',
        icon: Icons.filter_none_rounded,
        route: '/masters/combos',
        feature: AppFeature.combos,
        color: Color(0xFF5B6CFF),
      ),
    if (f.contains(AppFeature.vendors))
      const BusinessMasterItem(
        id: 'vendors',
        title: 'Vendors',
        subtitle: 'Suppliers, GSTIN and credit terms',
        icon: Icons.storefront_rounded,
        route: '/purchase/vendors',
        feature: AppFeature.vendors,
        color: AppColors.orange,
      ),
    if (f.contains(AppFeature.warehouses))
      const BusinessMasterItem(
        id: 'warehouses',
        title: 'Warehouses',
        subtitle: 'Central and branch warehouses',
        icon: Icons.warehouse_rounded,
        route: '/inventory/warehouses',
        feature: AppFeature.warehouses,
        color: Color(0xFFE6A100),
      ),
    if (f.contains(AppFeature.batchExpiry) || f.contains(AppFeature.serialNumbers))
      const BusinessMasterItem(
        id: 'lots',
        title: 'Batch / Expiry / Serial',
        subtitle: 'Lots, expiry dates and serial tracking',
        icon: Icons.science_rounded,
        route: '/inventory/lots',
        feature: AppFeature.batchExpiry,
        color: AppColors.green,
      ),
    if (f.contains(AppFeature.brands))
      const BusinessMasterItem(
        id: 'brands',
        title: 'Brands',
        subtitle: 'Brand master',
        icon: Icons.sell_rounded,
        route: '/masters/brands',
        feature: AppFeature.brands,
        color: Color(0xFF5B6CFF),
      ),
    if (f.contains(AppFeature.offers) || f.contains(AppFeature.coupons))
      const BusinessMasterItem(
        id: 'offers',
        title: 'Offers & Coupons',
        subtitle: 'Campaigns and coupon codes',
        icon: Icons.local_offer_rounded,
        route: '/offers',
        feature: AppFeature.offers,
        color: Color(0xFFE91E63),
      ),
    if (f.contains(AppFeature.auditLog))
      const BusinessMasterItem(
        id: 'audit',
        title: 'Audit Log',
        subtitle: 'Who changed what and when',
        icon: Icons.history_rounded,
        route: '/audit-log',
        feature: AppFeature.auditLog,
        color: AppColors.navy,
      ),
    if (f.contains(AppFeature.pharmacySchedule))
      const BusinessMasterItem(
        id: 'pharmacy',
        title: 'Pharmacy schedule',
        subtitle: 'Schedule H / H1 / X and Rx lots',
        icon: Icons.medical_services_rounded,
        route: '/pharmacy/schedule',
        feature: AppFeature.pharmacySchedule,
        color: AppColors.green,
      ),
    if (f.contains(AppFeature.modifiers))
      const BusinessMasterItem(
        id: 'modifiers',
        title: 'Modifiers',
        subtitle: 'Add-ons and extras',
        icon: Icons.extension_rounded,
        route: '/masters/modifiers',
        feature: AppFeature.modifiers,
        color: AppColors.orange,
      ),
    if (f.contains(AppFeature.kitchenDepartments))
      const BusinessMasterItem(
        id: 'kitchen-depts',
        title: 'Kitchen departments',
        subtitle: 'KOT routing departments',
        icon: Icons.soup_kitchen_rounded,
        route: '/masters/kitchen-depts',
        feature: AppFeature.kitchenDepartments,
        color: Color(0xFFE6A100),
      ),
  ];

  static List<BusinessMasterItem> _foodMasters(Set<AppFeature> f) => [
    const BusinessMasterItem(
      id: 'categories',
      title: 'Categories',
      subtitle: 'Food groups such as Veg, Non Veg',
      icon: Icons.category_rounded,
      route: '/masters/categories',
      feature: AppFeature.inventory,
      color: AppColors.primary,
    ),
    const BusinessMasterItem(
      id: 'subcategories',
      title: 'Subcategories',
      subtitle: 'Starter, Main Course, Beverage',
      icon: Icons.folder_rounded,
      route: '/masters/subcategories',
      feature: AppFeature.inventory,
      color: AppColors.purple,
    ),
    if (f.contains(AppFeature.portions))
      const BusinessMasterItem(
        id: 'portions',
        title: 'Portions',
        subtitle: 'Half, Full, Small, Large and custom',
        icon: Icons.layers_rounded,
        route: '/masters/portion-masters',
        feature: AppFeature.portions,
        color: AppColors.orange,
      ),
    const BusinessMasterItem(
      id: 'products',
      title: 'Products',
      subtitle: 'Menu items and prices',
      icon: Icons.inventory_2_rounded,
      route: '/masters/products',
      feature: AppFeature.inventory,
      color: AppColors.green,
    ),
    if (f.contains(AppFeature.combos))
      const BusinessMasterItem(
        id: 'combos',
        title: 'Combos',
        subtitle: 'Combo meals and offers',
        icon: Icons.filter_none_rounded,
        route: '/masters/combos',
        feature: AppFeature.combos,
        color: Color(0xFF5B6CFF),
      ),
    if (f.contains(AppFeature.tables))
      const BusinessMasterItem(
        id: 'tables',
        title: 'Table Master',
        subtitle: 'Areas, types and tables with seats',
        icon: Icons.table_restaurant_rounded,
        route: '/masters/tables',
        feature: AppFeature.tables,
        color: Color(0xFFE6A100),
      ),
    if (f.contains(AppFeature.modifiers))
      const BusinessMasterItem(
        id: 'modifiers',
        title: 'Modifiers',
        subtitle: 'Add-ons and extras',
        icon: Icons.extension_rounded,
        route: '/masters/modifiers',
        feature: AppFeature.modifiers,
        color: AppColors.orange,
      ),
    if (f.contains(AppFeature.kitchenDepartments))
      const BusinessMasterItem(
        id: 'kitchen-depts',
        title: 'Kitchen departments',
        subtitle: 'KOT routing departments',
        icon: Icons.soup_kitchen_rounded,
        route: '/masters/kitchen-depts',
        feature: AppFeature.kitchenDepartments,
        color: Color(0xFFE6A100),
      ),
  ];
}
