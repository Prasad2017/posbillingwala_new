import 'dart:convert';
import 'dart:math';

import 'package:drift/drift.dart';
import 'package:drift_flutter/drift_flutter.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/database/branch_scope.dart';
import 'package:pos_billingwala_v2/core/database/tables.dart';
import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';
import 'package:pos_billingwala_v2/features/company/data/company_dtos.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_mode.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';

part 'app_database.g.dart';

@DriftDatabase(
  tables: [
    FoodTypes,
    ProductCategories,
    ProductSubcategories,
    Products,
    ProductPortions,
    Combos,
    ComboItems,
    CartItems,
    CartComboItems,
    Invoices,
    InvoiceItems,
    InvoiceComboItems,
    InvoiceProductDeleteQueue,
    PosTables,
    DiningAreas,
    TableTypes,
    PortionMasters,
    DiningSessions,
    OrderRounds,
    Kots,
    KotItems,
    MessMembers,
    MessTokens,
    MessMealTokenQueue,
    InventoryMovements,
    ShopExpenses,
    MessMemberPayments,
    MessInvoices,
    Companies,
    CompanyPrinterSettings,
  ],
)
class AppDatabase extends _$AppDatabase {
  AppDatabase([QueryExecutor? executor]) : super(executor ?? appDatabaseOpen());

  /// In-memory Android [BranchSession] equivalents.
  String scopeOrg = '';
  String scopeBranch = '';
  String scopeDevice = '';
  String scopeUserId = '';

  String get activeBranchId => scopeBranch;

  @override
  int get schemaVersion => 18;

  @override
  MigrationStrategy get migration => MigrationStrategy(
        onCreate: (m) async {
          await m.createAll();
        },
        onUpgrade: (m, from, to) async {
          if (from < 2) {
            await m.createTable(cartItems);
          }
          if (from < 3) {
            await m.createTable(invoices);
            await m.createTable(invoiceItems);
          }
          if (from < 4) {
            await m.addColumn(invoices, invoices.customerName);
            await m.addColumn(invoices, invoices.customerMobile);
          }
          if (from < 5) {
            await m.addColumn(invoices, invoices.diningSessionId);
            await m.createTable(posTables);
            await m.createTable(diningSessions);
            // Cart PK changed to (productId, cartScope) — recreate safely.
            await m.deleteTable('cart_items');
            await m.createTable(cartItems);
          }
          if (from < 6) {
            await m.addColumn(cartItems, cartItems.printedQuantity);
            await m.addColumn(
              diningSessions,
              diningSessions.joinedTableNumbers,
            );
            await m.createTable(orderRounds);
            await m.createTable(kots);
            await m.createTable(kotItems);
            await m.createTable(messMembers);
            await m.createTable(messTokens);
          }
          if (from < 7) {
            await m.addColumn(invoices, invoices.invoiceSyncStatus);
            await m.addColumn(
              invoiceItems,
              invoiceItems.invoiceItemNetworkStatus,
            );
            await m.addColumn(
              invoiceItems,
              invoiceItems.invoiceItemSyncStatus,
            );
          }
          if (from < 8) {
            await m.createTable(inventoryMovements);
            await m.createTable(shopExpenses);
          }
          if (from < 9) {
            await m.addColumn(
              productCategories,
              productCategories.categorySyncStatus,
            );
            await m.addColumn(products, products.productSyncStatus);
            await m.addColumn(
              productPortions,
              productPortions.portionSyncStatus,
            );
            await m.addColumn(
              diningSessions,
              diningSessions.sessionNetworkStatus,
            );
            await m.addColumn(
              diningSessions,
              diningSessions.sessionSyncStatus,
            );
            await m.addColumn(
              diningSessions,
              diningSessions.sessionVersion,
            );
            await m.addColumn(messMembers, messMembers.memberSyncStatus);
            await m.addColumn(messTokens, messTokens.tokenSyncStatus);
            await m.addColumn(cartItems, cartItems.lineType);
            await m.addColumn(cartItems, cartItems.comboId);
            await m.addColumn(cartItems, cartItems.comboNetworkStatus);
            await m.createTable(combos);
            await m.createTable(comboItems);
          }
          if (from < 10) {
            // Cart PK adds portionId — recreate empty cart safely.
            await m.deleteTable('cart_items');
            await m.createTable(cartItems);
            await m.createTable(messMemberPayments);
          }
          if (from < 11) {
            await m.createTable(productSubcategories);
          }
          if (from < 12) {
            await m.createTable(diningAreas);
            await m.createTable(tableTypes);
            await m.createTable(portionMasters);
          }
          if (from < 13) {
            await m.createTable(messInvoices);
          }
          if (from < 14) {
            await m.addColumn(invoices, invoices.organizationId);
            await m.addColumn(invoices, invoices.branchId);
            await m.addColumn(invoices, invoices.deviceId);
            await m.addColumn(invoiceItems, invoiceItems.organizationId);
            await m.addColumn(invoiceItems, invoiceItems.branchId);
            await m.addColumn(invoiceItems, invoiceItems.deviceId);
            await m.addColumn(posTables, posTables.organizationId);
            await m.addColumn(posTables, posTables.branchId);
            await m.addColumn(posTables, posTables.deviceId);
            await m.addColumn(diningAreas, diningAreas.organizationId);
            await m.addColumn(diningAreas, diningAreas.branchId);
            await m.addColumn(diningAreas, diningAreas.deviceId);
            await m.addColumn(tableTypes, tableTypes.organizationId);
            await m.addColumn(tableTypes, tableTypes.branchId);
            await m.addColumn(tableTypes, tableTypes.deviceId);
            await m.addColumn(diningSessions, diningSessions.organizationId);
            await m.addColumn(diningSessions, diningSessions.branchId);
            await m.addColumn(diningSessions, diningSessions.deviceId);
            await m.addColumn(orderRounds, orderRounds.organizationId);
            await m.addColumn(orderRounds, orderRounds.branchId);
            await m.addColumn(orderRounds, orderRounds.deviceId);
            await m.addColumn(kots, kots.organizationId);
            await m.addColumn(kots, kots.branchId);
            await m.addColumn(kots, kots.deviceId);
            await m.addColumn(kotItems, kotItems.organizationId);
            await m.addColumn(kotItems, kotItems.branchId);
            await m.addColumn(kotItems, kotItems.deviceId);
            await m.addColumn(
              inventoryMovements,
              inventoryMovements.organizationId,
            );
            await m.addColumn(inventoryMovements, inventoryMovements.branchId);
            await m.addColumn(inventoryMovements, inventoryMovements.deviceId);
            await m.addColumn(shopExpenses, shopExpenses.organizationId);
            await m.addColumn(shopExpenses, shopExpenses.branchId);
            await m.addColumn(shopExpenses, shopExpenses.deviceId);
          }
          if (from < 15) {
            await m.addColumn(invoices, invoices.customerEmail);
            await m.addColumn(invoices, invoices.customerAddress);
          }
          if (from < 16) {
            // Align Drift schema with Android POSBillingWalaDatabase (v30).
            await m.addColumn(products, products.userId);
            await m.addColumn(combos, combos.comboStatus);
            await m.addColumn(comboItems, comboItems.comboItemStatus);

            await m.addColumn(cartItems, cartItems.productOldPrice);
            await m.addColumn(cartItems, cartItems.productNewPrice);
            await m.addColumn(cartItems, cartItems.productCgst);
            await m.addColumn(cartItems, cartItems.productSgst);
            await m.addColumn(cartItems, cartItems.kotPrinted);
            await m.addColumn(cartItems, cartItems.portionName);
            await m.addColumn(cartItems, cartItems.snapshotProductName);
            await m.addColumn(cartItems, cartItems.snapshotLinePrice);
            await m.addColumn(cartItems, cartItems.snapshotComboComponents);
            await m.addColumn(cartItems, cartItems.cartDiscount);
            await m.addColumn(cartItems, cartItems.cartDiscountType);
            await m.addColumn(cartItems, cartItems.cartPackingCharge);
            await m.addColumn(cartItems, cartItems.cartPackingChargeType);
            await m.addColumn(cartItems, cartItems.noOfTable);
            await m.addColumn(cartItems, cartItems.cartOrderStatus);
            await m.addColumn(cartItems, cartItems.cartStatus);
            await m.addColumn(cartItems, cartItems.userId);
            await m.addColumn(cartItems, cartItems.orderRoundId);

            await m.addColumn(invoices, invoices.billPrintStatus);
            await m.addColumn(invoiceItems, invoiceItems.portionId);
            await m.addColumn(invoiceItems, invoiceItems.portionName);
            await m.addColumn(invoiceItems, invoiceItems.snapshotProductName);
            await m.addColumn(invoiceItems, invoiceItems.snapshotLinePrice);
            await m.addColumn(
              invoiceItems,
              invoiceItems.snapshotComboComponents,
            );
            await m.addColumn(invoiceItems, invoiceItems.comboId);

            await m.addColumn(posTables, posTables.tableTypeId);
            await m.addColumn(posTables, posTables.positionX);
            await m.addColumn(posTables, posTables.positionY);
            await m.addColumn(posTables, posTables.posTableStatus);
            await m.addColumn(tableTypes, tableTypes.defaultCapacity);

            await m.addColumn(diningSessions, diningSessions.waiterName);
            await m.addColumn(
              diningSessions,
              diningSessions.unpaidInvoiceNumber,
            );

            await m.addColumn(kotItems, kotItems.cartId);

            await m.addColumn(messTokens, messTokens.tokenStatus);
            await m.addColumn(messTokens, messTokens.verifyNetworkStatus);
            await m.addColumn(messTokens, messTokens.verifyStatus);

            await m.createTable(cartComboItems);
            await m.createTable(invoiceComboItems);
            await m.createTable(invoiceProductDeleteQueue);
            await m.createTable(messMealTokenQueue);
            await m.createTable(companies);
            await m.createTable(companyPrinterSettings);
          }
          if (from < 17) {
            await m.addColumn(invoices, invoices.userId);
          }
          if (from < 18) {
            await migrateCartIdPrimaryKey();
          }
        },
      );

  /// Android `cartId INTEGER PRIMARY KEY AUTOINCREMENT`.
  Future<void> migrateCartIdPrimaryKey() async {
    await customStatement('''
CREATE TABLE IF NOT EXISTS cart_items_v18 (
  cart_id INTEGER PRIMARY KEY AUTOINCREMENT,
  product_id INTEGER NOT NULL,
  cart_scope TEXT NOT NULL DEFAULT '',
  portion_id INTEGER NOT NULL DEFAULT 0,
  product_name TEXT NOT NULL DEFAULT '',
  category_id INTEGER NULL,
  category_name TEXT NULL,
  product_code TEXT NULL,
  unit_price REAL NOT NULL DEFAULT 0.0,
  product_old_price REAL NULL,
  product_new_price REAL NULL,
  gst_percent REAL NOT NULL DEFAULT 0.0,
  product_cgst REAL NOT NULL DEFAULT 0.0,
  product_sgst REAL NOT NULL DEFAULT 0.0,
  quantity INTEGER NOT NULL DEFAULT 1,
  printed_quantity INTEGER NOT NULL DEFAULT 0,
  kot_printed TEXT NOT NULL DEFAULT '0',
  product_unit TEXT NULL,
  portion_name TEXT NULL,
  snapshot_product_name TEXT NULL,
  snapshot_line_price REAL NULL,
  snapshot_combo_components TEXT NULL,
  cart_discount REAL NOT NULL DEFAULT 0.0,
  cart_discount_type TEXT NOT NULL DEFAULT 'Amount',
  cart_packing_charge REAL NOT NULL DEFAULT 0.0,
  cart_packing_charge_type TEXT NOT NULL DEFAULT 'Percentage',
  no_of_table TEXT NOT NULL DEFAULT '',
  cart_order_status TEXT NULL,
  cart_status TEXT NOT NULL DEFAULT '1',
  user_id TEXT NULL,
  dining_session_id INTEGER NULL,
  order_round_id INTEGER NULL,
  line_type TEXT NOT NULL DEFAULT 'product',
  combo_id INTEGER NULL,
  combo_network_status TEXT NULL,
  updated_at INTEGER NOT NULL
);
''');
    await customStatement('''
INSERT INTO cart_items_v18 (
  product_id, cart_scope, portion_id, product_name, category_id, category_name,
  product_code, unit_price, product_old_price, product_new_price, gst_percent,
  product_cgst, product_sgst, quantity, printed_quantity, kot_printed,
  product_unit, portion_name, snapshot_product_name, snapshot_line_price,
  snapshot_combo_components, cart_discount, cart_discount_type,
  cart_packing_charge, cart_packing_charge_type, no_of_table, cart_order_status,
  cart_status, user_id, dining_session_id, order_round_id, line_type, combo_id,
  combo_network_status, updated_at
)
SELECT
  product_id, cart_scope, portion_id, product_name, category_id, category_name,
  product_code, unit_price, product_old_price, product_new_price, gst_percent,
  product_cgst, product_sgst, quantity, printed_quantity, kot_printed,
  product_unit, portion_name, snapshot_product_name, snapshot_line_price,
  snapshot_combo_components, cart_discount, cart_discount_type,
  cart_packing_charge, cart_packing_charge_type, no_of_table, cart_order_status,
  cart_status, user_id, dining_session_id, order_round_id, line_type, combo_id,
  combo_network_status, updated_at
FROM cart_items;
''');
    await customStatement('DROP TABLE cart_items;');
    await customStatement('ALTER TABLE cart_items_v18 RENAME TO cart_items;');
    try {
      await customStatement(
        'ALTER TABLE cart_combo_items ADD COLUMN cart_id INTEGER NOT NULL DEFAULT 0;',
      );
    } catch (_) {}
    await customStatement('''
UPDATE cart_combo_items
SET cart_id = IFNULL((
  SELECT cart_id FROM cart_items
  WHERE cart_items.product_id = cart_combo_items.product_id
    AND cart_items.cart_scope = cart_combo_items.cart_scope
    AND cart_items.portion_id = cart_combo_items.parent_portion_id
  LIMIT 1
), 0)
WHERE cart_id = 0;
''');
  }

  static QueryExecutor appDatabaseOpen() {
    return driftDatabase(
      name: 'pos_billingwala_v2',
      web: DriftWebOptions(
        sqlite3Wasm: Uri.parse('sqlite3.wasm'),
        driftWorker: Uri.parse('drift_worker.js'),
      ),
    );
  }

  void setBranchScope({
    required String organizationId,
    required String branchId,
    required String deviceId,
    String userId = '',
  }) {
    scopeOrg = organizationId.trim();
    scopeBranch = branchId.trim();
    scopeDevice = deviceId.trim();
    scopeUserId = userId.trim();
  }

  /// Android [LicenceScopeGuard.applyScope] — purge or claim, then bind prefs.
  Future<void> applyLicenceScope({
    required UserSession session,
    required String deviceId,
  }) async {
    final branchId = BranchScope.effectiveBranchId(session);
    final orgId = BranchScope.effectiveOrganizationId(session);
    setBranchScope(
      organizationId: orgId,
      branchId: branchId,
      deviceId: deviceId,
      userId: session.userId,
    );
    if (branchId.isEmpty) return;

    final bound = await BranchScope.readBound();
    final licenceKey = session.licenceKey.trim();
    final licenceChanged = bound.boundLicence.isNotEmpty &&
        licenceKey.isNotEmpty &&
        bound.boundLicence != licenceKey;
    final branchChanged =
        bound.boundBranch.isNotEmpty && bound.boundBranch != branchId;
    final foreign = await hasInvoicesForOtherBranch(branchId);

    if (licenceChanged || branchChanged || foreign) {
      await purgeLocalDataNotMatchingBranch(branchId);
    } else {
      await claimUnscopedRowsForBranch(branchId);
    }

    await BranchScope.persistBound(
      licenceKey: licenceKey,
      branchId: branchId,
    );
    await BranchScope.mirrorSessionPrefs(session);
  }

  Expression<bool> branchMatches(GeneratedColumn<String> col) {
    if (scopeBranch.isEmpty) return const Constant(true);
    // Include unscoped rows so cloud imports / pre-scope data still appear.
    return col.equals(scopeBranch) | col.equals('');
  }

  /// Android parity: `IFNULL(invoiceOrderStatus,'completed') != 'refunded'`
  /// (+ skip cancelled). Empty / completed / other statuses still count.
  Expression<bool> isBillableInvoice($InvoicesTable t) {
    final status = t.invoiceOrderStatus.lower();
    return status.isNotValue('refunded') & status.isNotValue('cancelled');
  }

  bool valueNonEmpty(Value<String> value) =>
      value.present && value.value.trim().isNotEmpty;

  InvoicesCompanion stampInvoice(InvoicesCompanion c) => c.copyWith(
        organizationId: valueNonEmpty(c.organizationId)
            ? c.organizationId
            : Value(scopeOrg),
        branchId:
            valueNonEmpty(c.branchId) ? c.branchId : Value(scopeBranch),
        deviceId:
            valueNonEmpty(c.deviceId) ? c.deviceId : Value(scopeDevice),
        userId: (c.userId.present &&
                (c.userId.value ?? '').trim().isNotEmpty)
            ? c.userId
            : Value(scopeUserId.isEmpty ? null : scopeUserId),
      );

  InvoiceItemsCompanion stampInvoiceItem(InvoiceItemsCompanion c) =>
      c.copyWith(
        organizationId: valueNonEmpty(c.organizationId)
            ? c.organizationId
            : Value(scopeOrg),
        branchId:
            valueNonEmpty(c.branchId) ? c.branchId : Value(scopeBranch),
        deviceId:
            valueNonEmpty(c.deviceId) ? c.deviceId : Value(scopeDevice),
      );

  PosTablesCompanion stampPosTable(PosTablesCompanion c) => c.copyWith(
        organizationId: valueNonEmpty(c.organizationId)
            ? c.organizationId
            : Value(scopeOrg),
        branchId:
            valueNonEmpty(c.branchId) ? c.branchId : Value(scopeBranch),
        deviceId:
            valueNonEmpty(c.deviceId) ? c.deviceId : Value(scopeDevice),
      );

  DiningAreasCompanion stampDiningArea(DiningAreasCompanion c) => c.copyWith(
        organizationId: valueNonEmpty(c.organizationId)
            ? c.organizationId
            : Value(scopeOrg),
        branchId:
            valueNonEmpty(c.branchId) ? c.branchId : Value(scopeBranch),
        deviceId:
            valueNonEmpty(c.deviceId) ? c.deviceId : Value(scopeDevice),
      );

  TableTypesCompanion stampTableType(TableTypesCompanion c) => c.copyWith(
        organizationId: valueNonEmpty(c.organizationId)
            ? c.organizationId
            : Value(scopeOrg),
        branchId:
            valueNonEmpty(c.branchId) ? c.branchId : Value(scopeBranch),
        deviceId:
            valueNonEmpty(c.deviceId) ? c.deviceId : Value(scopeDevice),
      );

  DiningSessionsCompanion stampDiningSession(DiningSessionsCompanion c) =>
      c.copyWith(
        organizationId: valueNonEmpty(c.organizationId)
            ? c.organizationId
            : Value(scopeOrg),
        branchId:
            valueNonEmpty(c.branchId) ? c.branchId : Value(scopeBranch),
        deviceId:
            valueNonEmpty(c.deviceId) ? c.deviceId : Value(scopeDevice),
      );

  OrderRoundsCompanion stampOrderRound(OrderRoundsCompanion c) => c.copyWith(
        organizationId: valueNonEmpty(c.organizationId)
            ? c.organizationId
            : Value(scopeOrg),
        branchId:
            valueNonEmpty(c.branchId) ? c.branchId : Value(scopeBranch),
        deviceId:
            valueNonEmpty(c.deviceId) ? c.deviceId : Value(scopeDevice),
      );

  KotsCompanion stampKot(KotsCompanion c) => c.copyWith(
        organizationId: valueNonEmpty(c.organizationId)
            ? c.organizationId
            : Value(scopeOrg),
        branchId:
            valueNonEmpty(c.branchId) ? c.branchId : Value(scopeBranch),
        deviceId:
            valueNonEmpty(c.deviceId) ? c.deviceId : Value(scopeDevice),
      );

  KotItemsCompanion stampKotItem(KotItemsCompanion c) => c.copyWith(
        organizationId: valueNonEmpty(c.organizationId)
            ? c.organizationId
            : Value(scopeOrg),
        branchId:
            valueNonEmpty(c.branchId) ? c.branchId : Value(scopeBranch),
        deviceId:
            valueNonEmpty(c.deviceId) ? c.deviceId : Value(scopeDevice),
      );

  InventoryMovementsCompanion stampInventory(InventoryMovementsCompanion c) =>
      c.copyWith(
        organizationId: valueNonEmpty(c.organizationId)
            ? c.organizationId
            : Value(scopeOrg),
        branchId:
            valueNonEmpty(c.branchId) ? c.branchId : Value(scopeBranch),
        deviceId:
            valueNonEmpty(c.deviceId) ? c.deviceId : Value(scopeDevice),
      );

  ShopExpensesCompanion stampExpense(ShopExpensesCompanion c) => c.copyWith(
        organizationId: valueNonEmpty(c.organizationId)
            ? c.organizationId
            : Value(scopeOrg),
        branchId:
            valueNonEmpty(c.branchId) ? c.branchId : Value(scopeBranch),
        deviceId:
            valueNonEmpty(c.deviceId) ? c.deviceId : Value(scopeDevice),
      );

  Future<bool> hasInvoicesForOtherBranch(String branchId) async {
    if (branchId.trim().isEmpty) return false;
    final row = await (select(invoices)
          ..where(
            (t) =>
                t.branchId.isNotValue('') & t.branchId.isNotValue(branchId),
          )
          ..limit(1))
        .getSingleOrNull();
    return row != null;
  }

  /// Android purge: drop invoices (+ lines) that are unscoped or other-branch.
  Future<void> purgeLocalDataNotMatchingBranch(String branchId) async {
    if (branchId.trim().isEmpty) return;
    await transaction(() async {
      final foreign = await (select(invoices)
            ..where(
              (t) =>
                  t.branchId.equals('') | t.branchId.isNotValue(branchId),
            ))
          .get();
      for (final inv in foreign) {
        await (delete(invoiceItems)
              ..where((t) => t.invoiceNumber.equals(inv.invoiceNumber)))
            .go();
      }
      await (delete(invoices)
            ..where(
              (t) =>
                  t.branchId.equals('') | t.branchId.isNotValue(branchId),
            ))
          .go();
    });
  }

  /// Stamp empty branchId rows to the active branch (upgrade path).
  Future<void> claimUnscopedRowsForBranch(String branchId) async {
    if (branchId.trim().isEmpty) return;
    final stamp = InvoicesCompanion(branchId: Value(branchId));
    final itemStamp = InvoiceItemsCompanion(branchId: Value(branchId));
    await (update(invoices)..where((t) => t.branchId.equals('')))
        .write(stamp);
    await (update(invoiceItems)..where((t) => t.branchId.equals('')))
        .write(itemStamp);
    // Also claim operational tables so pre-v14 rows remain visible.
    await (update(posTables)..where((t) => t.branchId.equals(''))).write(
      PosTablesCompanion(branchId: Value(branchId)),
    );
    await (update(diningAreas)..where((t) => t.branchId.equals(''))).write(
      DiningAreasCompanion(branchId: Value(branchId)),
    );
    await (update(tableTypes)..where((t) => t.branchId.equals(''))).write(
      TableTypesCompanion(branchId: Value(branchId)),
    );
    await (update(diningSessions)..where((t) => t.branchId.equals('')))
        .write(DiningSessionsCompanion(branchId: Value(branchId)));
    await (update(orderRounds)..where((t) => t.branchId.equals(''))).write(
      OrderRoundsCompanion(branchId: Value(branchId)),
    );
    await (update(kots)..where((t) => t.branchId.equals(''))).write(
      KotsCompanion(branchId: Value(branchId)),
    );
    await (update(kotItems)..where((t) => t.branchId.equals(''))).write(
      KotItemsCompanion(branchId: Value(branchId)),
    );
    await (update(inventoryMovements)..where((t) => t.branchId.equals('')))
        .write(InventoryMovementsCompanion(branchId: Value(branchId)));
    await (update(shopExpenses)..where((t) => t.branchId.equals(''))).write(
      ShopExpensesCompanion(branchId: Value(branchId)),
    );
  }

  Future<void> clearCatalog() async {
    await transaction(() async {
      await delete(comboItems).go();
      await delete(combos).go();
      await delete(productPortions).go();
      await delete(products).go();
      await delete(productSubcategories).go();
      await delete(productCategories).go();
      await delete(foodTypes).go();
    });
  }

  Future<void> replaceFoodTypes(List<FoodTypesCompanion> rows) async {
    await transaction(() async {
      await delete(foodTypes).go();
      await batch((b) => b.insertAll(foodTypes, rows));
    });
  }

  Future<void> replaceCategories(List<ProductCategoriesCompanion> rows) async {
    await transaction(() async {
      await delete(productCategories).go();
      await batch((b) => b.insertAll(productCategories, rows));
    });
  }

  Future<void> replaceSubcategories(
    List<ProductSubcategoriesCompanion> rows,
  ) async {
    await transaction(() async {
      await delete(productSubcategories).go();
      await batch((b) => b.insertAll(productSubcategories, rows));
    });
  }

  Stream<List<ProductSubcategory>> watchActiveSubcategories({int? categoryId}) {
    final query = select(productSubcategories)
      ..orderBy([
        (t) => OrderingTerm.asc(t.subcategorySortOrder),
        (t) => OrderingTerm.asc(t.subcategoryName),
      ]);
    if (categoryId != null) {
      query.where(
        (t) =>
            t.subcategoryDeletedStatus.equals('0') &
            t.categoryId.equals(categoryId),
      );
    } else {
      query.where((t) => t.subcategoryDeletedStatus.equals('0'));
    }
    return query.watch();
  }

  Future<List<ProductSubcategory>> getPendingSubcategories({int limit = 100}) {
    return (select(productSubcategories)
          ..where((t) => t.subcategorySyncStatus.equals('0'))
          ..orderBy([(t) => OrderingTerm.asc(t.subcategoryId)])
          ..limit(limit))
        .get();
  }

  Future<int> nextLocalSubcategoryId() async {
    final row = await (selectOnly(productSubcategories)
          ..addColumns([productSubcategories.subcategoryId.max()]))
        .getSingle();
    return (row.read(productSubcategories.subcategoryId.max()) ?? 0) + 1;
  }

  Future<int> insertLocalSubcategory({
    required String subcategoryName,
    required int categoryId,
    String? categoryNetworkStatus,
    int subcategorySortOrder = 0,
  }) async {
    final id = await nextLocalSubcategoryId();
    final network = appDatabaseNetworkStatus(prefix: 'sub_');
    await into(productSubcategories).insert(
      ProductSubcategoriesCompanion.insert(
        subcategoryId: Value(id),
        categoryId: Value(categoryId),
        subcategoryName: Value(subcategoryName),
        categoryNetworkStatus: Value(categoryNetworkStatus),
        subcategoryNetworkStatus: Value(network),
        subcategorySortOrder: Value(subcategorySortOrder),
        subcategorySyncStatus: const Value('0'),
      ),
      mode: InsertMode.insertOrReplace,
    );
    return id;
  }

  Future<void> softDeleteSubcategory(int subcategoryId) async {
    await (update(productSubcategories)
          ..where((t) => t.subcategoryId.equals(subcategoryId)))
        .write(
      const ProductSubcategoriesCompanion(
        subcategoryDeletedStatus: Value('1'),
        subcategorySyncStatus: Value('0'),
      ),
    );
  }

  Future<void> updateLocalSubcategory({
    required int subcategoryId,
    required String subcategoryName,
    int? categoryId,
  }) async {
    await (update(productSubcategories)
          ..where((t) => t.subcategoryId.equals(subcategoryId)))
        .write(
      ProductSubcategoriesCompanion(
        subcategoryName: Value(subcategoryName),
        categoryId: categoryId == null ? const Value.absent() : Value(categoryId),
        subcategorySyncStatus: const Value('0'),
      ),
    );
  }

  Future<void> markSubcategorySynced(int subcategoryId) async {
    await (update(productSubcategories)
          ..where((t) => t.subcategoryId.equals(subcategoryId)))
        .write(
      const ProductSubcategoriesCompanion(subcategorySyncStatus: Value('1')),
    );
  }

  Future<int> countActiveSubcategories() async {
    final count = countAll();
    final row = await (selectOnly(productSubcategories)
          ..addColumns([count])
          ..where(productSubcategories.subcategoryDeletedStatus.equals('0')))
        .getSingle();
    return row.read(count) ?? 0;
  }

  Future<void> replaceProducts(List<ProductsCompanion> rows) async {
    await transaction(() async {
      await delete(products).go();
      await batch((b) => b.insertAll(products, rows));
    });
  }

  Future<void> replacePortions(List<ProductPortionsCompanion> rows) async {
    await transaction(() async {
      await delete(productPortions).go();
      await batch((b) => b.insertAll(productPortions, rows));
    });
  }

  Future<void> replaceCombos(List<CombosCompanion> rows) async {
    await transaction(() async {
      await delete(combos).go();
      await batch((b) => b.insertAll(combos, rows));
    });
  }

  Future<void> replaceComboItems(List<ComboItemsCompanion> rows) async {
    await transaction(() async {
      await delete(comboItems).go();
      await batch((b) => b.insertAll(comboItems, rows));
    });
  }

  Stream<List<Combo>> watchActiveCombos() {
    return (select(combos)
          ..where(
            (t) =>
                t.comboDeletedStatus.equals('0') &
                t.comboActiveStatus.equals('1'),
          )
          ..orderBy([
            (t) => OrderingTerm.asc(t.comboSortOrder),
            (t) => OrderingTerm.asc(t.comboName),
          ]))
        .watch();
  }

  Future<int> nextLocalCategoryId() async {
    final row = await (selectOnly(productCategories)
          ..addColumns([productCategories.categoryId.max()]))
        .getSingle();
    return (row.read(productCategories.categoryId.max()) ?? 0) + 1;
  }

  Future<int> nextLocalProductId() async {
    final row = await (selectOnly(products)
          ..addColumns([products.productId.max()]))
        .getSingle();
    return (row.read(products.productId.max()) ?? 0) + 1;
  }

  Future<int> nextLocalPortionId() async {
    final row = await (selectOnly(productPortions)
          ..addColumns([productPortions.portionId.max()]))
        .getSingle();
    return (row.read(productPortions.portionId.max()) ?? 0) + 1;
  }

  Future<int> nextLocalComboId() async {
    final row = await (selectOnly(combos)..addColumns([combos.comboId.max()]))
        .getSingle();
    return (row.read(combos.comboId.max()) ?? 0) + 1;
  }

  Future<int> nextLocalComboItemId() async {
    final row = await (selectOnly(comboItems)
          ..addColumns([comboItems.comboItemId.max()]))
        .getSingle();
    return (row.read(comboItems.comboItemId.max()) ?? 0) + 1;
  }

  Future<int> insertLocalCategory({
    required String categoryName,
    int? foodTypeId,
    String? foodTypeCode,
    int categorySortOrder = 0,
  }) async {
    final id = await nextLocalCategoryId();
    final network = appDatabaseNetworkStatus(prefix: 'cat_');
    await into(productCategories).insert(
      ProductCategoriesCompanion.insert(
        categoryId: Value(id),
        categoryName: Value(categoryName),
        foodTypeId: Value(foodTypeId),
        foodTypeCode: Value(foodTypeCode),
        categorySortOrder: Value(categorySortOrder),
        categoryNetworkStatus: Value(network),
        categorySyncStatus: const Value('0'),
      ),
      mode: InsertMode.insertOrReplace,
    );
    return id;
  }

  Future<int> insertLocalProduct({
    required String productName,
    int? categoryId,
    String? categoryName,
    String? productCode,
    double productPrice = 0,
    String openPrice = '0',
    String? productUnit,
    double productCgst = 0,
    double productSgst = 0,
    int? subcategoryId,
  }) async {
    final id = await nextLocalProductId();
    final network = appDatabaseNetworkStatus(prefix: 'prd_');
    final withGst = productCgst + productSgst <= 0
        ? productPrice
        : productPrice + (productPrice * (productCgst + productSgst) / 100);
    await into(products).insert(
      ProductsCompanion.insert(
        productId: Value(id),
        productName: Value(productName),
        categoryId: Value(categoryId),
        categoryName: Value(categoryName),
        subcategoryId: Value(subcategoryId),
        productCode: Value(productCode),
        productPrice: Value(productPrice),
        openPrice: Value(openPrice),
        productUnit: Value(productUnit),
        productCgst: Value(productCgst),
        productSgst: Value(productSgst),
        productWithGstPrice: Value(withGst),
        productNetworkStatus: Value(network),
        productSyncStatus: const Value('0'),
      ),
      mode: InsertMode.insertOrReplace,
    );
    return id;
  }

  Future<void> updateLocalCategory({
    required int categoryId,
    required String categoryName,
    int? foodTypeId,
    String? foodTypeCode,
  }) async {
    await (update(productCategories)
          ..where((t) => t.categoryId.equals(categoryId)))
        .write(
      ProductCategoriesCompanion(
        categoryName: Value(categoryName),
        foodTypeId: foodTypeId == null ? const Value.absent() : Value(foodTypeId),
        foodTypeCode:
            foodTypeCode == null ? const Value.absent() : Value(foodTypeCode),
        categorySyncStatus: const Value('0'),
      ),
    );
  }

  Future<void> softDeleteCategory(int categoryId) async {
    await (update(productCategories)
          ..where((t) => t.categoryId.equals(categoryId)))
        .write(
      const ProductCategoriesCompanion(
        categoryDeletedStatus: Value('1'),
        categorySyncStatus: Value('0'),
      ),
    );
  }

  Future<void> updateLocalProduct({
    required int productId,
    required String productName,
    required double productPrice,
    int? categoryId,
    String? categoryName,
    String? productCode,
    String openPrice = '0',
    String? productUnit,
    double productCgst = 0,
    double productSgst = 0,
    int? subcategoryId,
  }) async {
    final withGst = productCgst + productSgst <= 0
        ? productPrice
        : productPrice + (productPrice * (productCgst + productSgst) / 100);
    await (update(products)..where((t) => t.productId.equals(productId)))
        .write(
      ProductsCompanion(
        productName: Value(productName),
        productPrice: Value(productPrice),
        categoryId: Value(categoryId),
        categoryName: Value(categoryName),
        subcategoryId: Value(subcategoryId),
        productCode: Value(productCode),
        openPrice: Value(openPrice),
        productUnit: Value(productUnit),
        productCgst: Value(productCgst),
        productSgst: Value(productSgst),
        productWithGstPrice: Value(withGst),
        productSyncStatus: const Value('0'),
      ),
    );
  }

  Future<void> softDeleteProduct(int productId) async {
    await (update(products)..where((t) => t.productId.equals(productId)))
        .write(
      const ProductsCompanion(
        productDeletedStatus: Value('1'),
        productSyncStatus: Value('0'),
      ),
    );
  }

  Future<void> softDeleteCombo(int comboId) async {
    await (update(combos)..where((t) => t.comboId.equals(comboId))).write(
      const CombosCompanion(
        comboDeletedStatus: Value('1'),
        comboSyncStatus: Value('0'),
      ),
    );
  }

  Future<void> updateLocalMessMember({
    required int memberId,
    required String memberName,
    String? mobile,
    String? altMobile,
    String? address,
    String? registrationNo,
    String memberType = 'student',
    String? rollNo,
    String? college,
    String? studentYear,
    String? company,
  }) async {
    await (update(messMembers)..where((t) => t.memberId.equals(memberId)))
        .write(
      MessMembersCompanion(
        memberName: Value(memberName),
        memberMobileNumber: Value(mobile),
        memberAltenetMobileNumber: Value(altMobile),
        memberAddress: Value(address),
        registrationNo: Value(registrationNo),
        memberType: Value(memberType),
        rollNo: Value(rollNo),
        college: Value(college),
        studentYear: Value(studentYear),
        company: Value(company),
        memberSyncStatus: const Value('0'),
      ),
    );
  }

  Future<int> insertLocalPortion({
    required int productId,
    required String portionName,
    double portionPrice = 0,
    int portionSortOrder = 0,
    int? portionMasterId,
  }) async {
    final id = await nextLocalPortionId();
    final network = appDatabaseNetworkStatus(prefix: 'por_');
    await into(productPortions).insert(
      ProductPortionsCompanion.insert(
        portionId: Value(id),
        productId: productId,
        portionMasterId: Value(portionMasterId),
        portionName: Value(portionName),
        portionPrice: Value(portionPrice),
        portionSortOrder: Value(portionSortOrder),
        portionNetworkStatus: Value(network),
        portionSyncStatus: const Value('0'),
      ),
      mode: InsertMode.insertOrReplace,
    );
    return id;
  }

  Future<int> insertLocalCombo({
    required String comboName,
    String? comboCode,
    double comboPrice = 0,
    double comboCgst = 0,
    double comboSgst = 0,
    int comboSortOrder = 0,
    bool activeOnPos = true,
  }) async {
    final id = await nextLocalComboId();
    final network = appDatabaseNetworkStatus(prefix: 'cmb_');
    final withGst = comboCgst + comboSgst <= 0
        ? comboPrice
        : comboPrice + (comboPrice * (comboCgst + comboSgst) / 100);
    await into(combos).insert(
      CombosCompanion.insert(
        comboId: Value(id),
        comboName: Value(comboName),
        comboCode: Value(comboCode),
        comboPrice: Value(comboPrice),
        comboCgst: Value(comboCgst),
        comboSgst: Value(comboSgst),
        comboWithGstPrice: Value(withGst),
        comboActiveStatus: Value(activeOnPos ? '1' : '0'),
        comboNetworkStatus: Value(network),
        comboSortOrder: Value(comboSortOrder),
        comboSyncStatus: const Value('0'),
      ),
      mode: InsertMode.insertOrReplace,
    );
    return id;
  }

  Future<void> updateLocalCombo({
    required int comboId,
    required String comboName,
    required double comboPrice,
    String? comboCode,
    double comboCgst = 0,
    double comboSgst = 0,
    bool? activeOnPos,
  }) async {
    final withGst = comboCgst + comboSgst <= 0
        ? comboPrice
        : comboPrice + (comboPrice * (comboCgst + comboSgst) / 100);
    await (update(combos)..where((t) => t.comboId.equals(comboId))).write(
      CombosCompanion(
        comboName: Value(comboName),
        comboCode: Value(comboCode),
        comboPrice: Value(comboPrice),
        comboCgst: Value(comboCgst),
        comboSgst: Value(comboSgst),
        comboWithGstPrice: Value(withGst),
        comboActiveStatus: activeOnPos == null
            ? const Value.absent()
            : Value(activeOnPos ? '1' : '0'),
        comboSyncStatus: const Value('0'),
      ),
    );
  }

  Future<void> replaceLocalComboItems({
    required int comboId,
    required List<({int productId, int quantity})> items,
  }) async {
    await (delete(comboItems)..where((t) => t.comboId.equals(comboId))).go();
    var sort = 0;
    for (final item in items) {
      if (item.quantity <= 0) continue;
      final id = await nextLocalComboItemId();
      await into(comboItems).insert(
        ComboItemsCompanion.insert(
          comboItemId: Value(id),
          comboId: comboId,
          productId: Value(item.productId),
          comboItemQuantity: Value(item.quantity),
          comboItemSortOrder: Value(sort++),
          comboItemNetworkStatus: Value(appDatabaseNetworkStatus(prefix: 'cbi_')),
          comboItemSyncStatus: const Value('0'),
        ),
        mode: InsertMode.insertOrReplace,
      );
    }
    await (update(combos)..where((t) => t.comboId.equals(comboId))).write(
      const CombosCompanion(comboSyncStatus: Value('0')),
    );
  }

  Future<List<ProductSalesRow>> getProductWiseSales({
    required DateTime start,
    required DateTime end,
    String? invoiceItemType,
    bool leastSold = false,
  }) async {
    final headers = await (select(invoices)
          ..where(
            (t) =>
                t.invoiceDate.isBiggerOrEqualValue(start) &
                t.invoiceDate.isSmallerThanValue(end) &
                isBillableInvoice(t) &
                branchMatches(t.branchId),
          ))
        .get();
    final totals = <String, ({int qty, double amount, String type})>{};
    for (final inv in headers) {
      final lines = await getInvoiceItems(inv.invoiceNumber);
      for (final line in lines) {
        if (invoiceItemType != null &&
            line.invoiceItemType != invoiceItemType) {
          continue;
        }
        final key = line.productName.trim().isEmpty
            ? 'Item'
            : line.productName.trim();
        final prev = totals[key];
        final qty = line.productQuantity + (prev?.qty ?? 0);
        final amount =
            (line.productPrice * line.productQuantity) + (prev?.amount ?? 0);
        totals[key] = (
          qty: qty,
          amount: amount,
          type: line.invoiceItemType,
        );
      }
    }
    final rows = totals.entries
        .map(
          (e) => ProductSalesRow(
            productName: e.key,
            totalQuantity: e.value.qty,
            totalAmount: e.value.amount,
            itemType: e.value.type,
          ),
        )
        .toList()
      ..sort(
        (a, b) => leastSold
            ? a.totalQuantity.compareTo(b.totalQuantity)
            : b.totalQuantity.compareTo(a.totalQuantity),
      );
    return rows;
  }

  Future<List<ProductPortion>> getPortionsForProduct(int productId) {
    return (select(productPortions)
          ..where(
            (t) =>
                t.productId.equals(productId) &
                t.portionDeletedStatus.equals('0'),
          )
          ..orderBy([(t) => OrderingTerm.asc(t.portionSortOrder)]))
        .get();
  }

  Future<List<ProductCategory>> getPendingCategories({int limit = 100}) {
    return (select(productCategories)
          ..where((t) => t.categorySyncStatus.equals('0'))
          ..orderBy([(t) => OrderingTerm.asc(t.categoryId)])
          ..limit(limit))
        .get();
  }

  Future<List<Product>> getPendingProducts({int limit = 100}) {
    return (select(products)
          ..where((t) => t.productSyncStatus.equals('0'))
          ..orderBy([(t) => OrderingTerm.asc(t.productId)])
          ..limit(limit))
        .get();
  }

  Future<List<ProductPortion>> getPendingPortions({int limit = 100}) {
    return (select(productPortions)
          ..where((t) => t.portionSyncStatus.equals('0'))
          ..orderBy([(t) => OrderingTerm.asc(t.portionId)])
          ..limit(limit))
        .get();
  }

  Future<List<Combo>> getPendingCombos({int limit = 100}) {
    return (select(combos)
          ..where((t) => t.comboSyncStatus.equals('0'))
          ..orderBy([(t) => OrderingTerm.asc(t.comboId)])
          ..limit(limit))
        .get();
  }

  Future<List<DiningSession>> getPendingDiningSessions({int limit = 100}) {
    return (select(diningSessions)
          ..where(
            (t) => t.sessionSyncStatus.equals('0') & branchMatches(t.branchId),
          )
          ..orderBy([(t) => OrderingTerm.asc(t.sessionId)])
          ..limit(limit))
        .get();
  }

  Future<List<PosTable>> getPendingPosTables({int limit = 100}) {
    return (select(posTables)
          ..where(
            (t) =>
                t.posTableStatus.equals('0') &
                t.tableActive.equals('1') &
                branchMatches(t.branchId),
          )
          ..orderBy([(t) => OrderingTerm.asc(t.tableId)])
          ..limit(limit))
        .get();
  }

  Future<void> markPosTableSynced(int tableId) async {
    await (update(posTables)..where((t) => t.tableId.equals(tableId))).write(
      const PosTablesCompanion(posTableStatus: Value('1')),
    );
  }

  Future<List<MessMember>> getPendingMessMembers({int limit = 100}) {
    return (select(messMembers)
          ..where((t) => t.memberSyncStatus.equals('0'))
          ..orderBy([(t) => OrderingTerm.asc(t.memberId)])
          ..limit(limit))
        .get();
  }

  Future<List<MessToken>> getPendingMessTokens({int limit = 100}) {
    return (select(messTokens)
          ..where((t) => t.tokenSyncStatus.equals('0'))
          ..orderBy([(t) => OrderingTerm.asc(t.tokenId)])
          ..limit(limit))
        .get();
  }

  Future<void> markCategorySynced(int categoryId) async {
    await (update(productCategories)
          ..where((t) => t.categoryId.equals(categoryId)))
        .write(
      const ProductCategoriesCompanion(categorySyncStatus: Value('1')),
    );
  }

  Future<void> markProductSynced(int productId) async {
    await (update(products)..where((t) => t.productId.equals(productId)))
        .write(const ProductsCompanion(productSyncStatus: Value('1')));
  }

  Future<void> markPortionSynced(int portionId) async {
    await (update(productPortions)
          ..where((t) => t.portionId.equals(portionId)))
        .write(
      const ProductPortionsCompanion(portionSyncStatus: Value('1')),
    );
  }

  Future<void> softDeletePortion(int portionId) async {
    await (update(productPortions)
          ..where((t) => t.portionId.equals(portionId)))
        .write(
      const ProductPortionsCompanion(
        portionDeletedStatus: Value('1'),
        portionSyncStatus: Value('0'),
      ),
    );
  }

  Future<void> markComboSynced(int comboId) async {
    await transaction(() async {
      await (update(combos)..where((t) => t.comboId.equals(comboId)))
          .write(const CombosCompanion(comboSyncStatus: Value('1')));
      await (update(comboItems)..where((t) => t.comboId.equals(comboId)))
          .write(const ComboItemsCompanion(comboItemSyncStatus: Value('1')));
    });
  }

  Future<Product?> getProduct(int productId) {
    return (select(products)..where((t) => t.productId.equals(productId)))
        .getSingleOrNull();
  }

  Future<List<ComboItem>> getComboItemsForCombo(int comboId) {
    return (select(comboItems)
          ..where((t) => t.comboId.equals(comboId))
          ..orderBy([(t) => OrderingTerm.asc(t.comboItemSortOrder)]))
        .get();
  }

  Future<void> markDiningSessionSynced(int sessionId) async {
    await (update(diningSessions)..where((t) => t.sessionId.equals(sessionId)))
        .write(
      const DiningSessionsCompanion(sessionSyncStatus: Value('1')),
    );
  }

  Future<DiningSession?> getDiningSessionById(int sessionId) {
    return (select(diningSessions)..where((t) => t.sessionId.equals(sessionId)))
        .getSingleOrNull();
  }

  Future<void> markMessMemberSynced(int memberId) async {
    await (update(messMembers)..where((t) => t.memberId.equals(memberId)))
        .write(const MessMembersCompanion(memberSyncStatus: Value('1')));
  }

  Future<void> markMessTokenSynced(int tokenId) async {
    await (update(messTokens)..where((t) => t.tokenId.equals(tokenId)))
        .write(const MessTokensCompanion(tokenSyncStatus: Value('1')));
  }

  /// Upsert cloud dining sessions keyed by [sessionNetworkStatus] when present.
  Future<int> upsertDiningSessionsFromCloud(
    List<DiningSessionsCompanion> rows,
  ) async {
    var count = 0;
    await transaction(() async {
      for (final row in rows) {
        final network = row.sessionNetworkStatus.present
            ? row.sessionNetworkStatus.value
            : null;
        DiningSession? existing;
        if (network != null && network.trim().isNotEmpty) {
          existing = await (select(diningSessions)
                ..where((t) => t.sessionNetworkStatus.equals(network))
                ..limit(1))
              .getSingleOrNull();
        }
        if (existing == null && row.sessionId.present) {
          existing = await (select(diningSessions)
                ..where((t) => t.sessionId.equals(row.sessionId.value))
                ..limit(1))
              .getSingleOrNull();
        }

        final companion = DiningSessionsCompanion(
          primaryTableNumber: row.primaryTableNumber,
          joinedTableNumbers: row.joinedTableNumbers,
          sessionStatus: row.sessionStatus,
          guestCount: row.guestCount,
          startedAt: row.startedAt,
          closedAt: row.closedAt,
          customerName: row.customerName,
          customerMobile: row.customerMobile,
          waiterName: row.waiterName,
          unpaidInvoiceNumber: row.unpaidInvoiceNumber,
          paidAmount: row.paidAmount,
          sessionNetworkStatus: row.sessionNetworkStatus,
          sessionSyncStatus: const Value('1'),
          sessionVersion: row.sessionVersion,
        );

        if (existing != null) {
          await (update(diningSessions)
                ..where((t) => t.sessionId.equals(existing!.sessionId)))
              .write(stampDiningSession(companion));
        } else {
          await into(diningSessions).insert(
            stampDiningSession(
              DiningSessionsCompanion.insert(
                primaryTableNumber: row.primaryTableNumber.present
                    ? row.primaryTableNumber.value
                    : '',
                joinedTableNumbers: row.joinedTableNumbers,
                sessionStatus: row.sessionStatus,
                guestCount: row.guestCount,
                startedAt: row.startedAt.present
                    ? row.startedAt.value
                    : DateTime.now(),
                closedAt: row.closedAt,
                customerName: row.customerName,
                customerMobile: row.customerMobile,
                waiterName: row.waiterName,
                unpaidInvoiceNumber: row.unpaidInvoiceNumber,
                paidAmount: row.paidAmount,
                sessionNetworkStatus: row.sessionNetworkStatus,
                sessionSyncStatus: const Value('1'),
                sessionVersion: row.sessionVersion,
              ),
            ),
          );
        }
        count++;
      }
    });
    return count;
  }

  Future<void> replacePosTables(List<PosTablesCompanion> rows) async {
    await transaction(() async {
      await delete(posTables).go();
      await batch(
        (b) => b.insertAll(
          posTables,
          rows.map(stampPosTable).toList(),
        ),
      );
    });
  }

  Future<void> replaceDiningAreas(List<DiningAreasCompanion> rows) async {
    await transaction(() async {
      await delete(diningAreas).go();
      await batch(
        (b) => b.insertAll(
          diningAreas,
          rows.map(stampDiningArea).toList(),
        ),
      );
    });
  }

  Future<void> replaceTableTypes(List<TableTypesCompanion> rows) async {
    await transaction(() async {
      await delete(tableTypes).go();
      await batch(
        (b) => b.insertAll(
          tableTypes,
          rows.map(stampTableType).toList(),
        ),
      );
    });
  }

  Future<void> replacePortionMasters(List<PortionMastersCompanion> rows) async {
    await transaction(() async {
      await delete(portionMasters).go();
      await batch((b) => b.insertAll(portionMasters, rows));
    });
  }

  Stream<List<DiningArea>> watchActiveDiningAreas() {
    return (select(diningAreas)
          ..where(
            (t) => t.areaActive.equals('1') & branchMatches(t.branchId),
          )
          ..orderBy([
            (t) => OrderingTerm.asc(t.areaSortOrder),
            (t) => OrderingTerm.asc(t.areaName),
          ]))
        .watch();
  }

  Stream<List<TableType>> watchActiveTableTypes() {
    return (select(tableTypes)
          ..where(
            (t) =>
                t.tableTypeActive.equals('1') & branchMatches(t.branchId),
          )
          ..orderBy([
            (t) => OrderingTerm.asc(t.tableTypeSortOrder),
            (t) => OrderingTerm.asc(t.tableTypeName),
          ]))
        .watch();
  }

  Stream<List<PortionMaster>> watchActivePortionMasters() {
    return (select(portionMasters)
          ..where((t) => t.portionMasterDeletedStatus.equals('0'))
          ..orderBy([(t) => OrderingTerm.asc(t.portionName)]))
        .watch();
  }

  Stream<List<FoodType>> watchActiveFoodTypes() {
    return (select(foodTypes)
          ..where((t) => t.foodTypeStatus.equals('1'))
          ..orderBy([
            (t) => OrderingTerm.asc(t.foodTypeSortOrder),
            (t) => OrderingTerm.asc(t.foodTypeName),
          ]))
        .watch();
  }

  Future<List<DiningArea>> getPendingDiningAreas({int limit = 100}) {
    return (select(diningAreas)
          ..where(
            (t) => t.areaSyncStatus.equals('0') & branchMatches(t.branchId),
          )
          ..orderBy([(t) => OrderingTerm.asc(t.areaId)])
          ..limit(limit))
        .get();
  }

  Future<List<TableType>> getPendingTableTypes({int limit = 100}) {
    return (select(tableTypes)
          ..where(
            (t) =>
                t.tableTypeSyncStatus.equals('0') &
                branchMatches(t.branchId),
          )
          ..orderBy([(t) => OrderingTerm.asc(t.tableTypeId)])
          ..limit(limit))
        .get();
  }

  Future<List<PortionMaster>> getPendingPortionMasters({int limit = 100}) {
    return (select(portionMasters)
          ..where((t) => t.portionMasterSyncStatus.equals('0'))
          ..orderBy([(t) => OrderingTerm.asc(t.portionMasterId)])
          ..limit(limit))
        .get();
  }

  Future<int> nextLocalDiningAreaId() async {
    final row = await (selectOnly(diningAreas)
          ..addColumns([diningAreas.areaId.max()]))
        .getSingle();
    return (row.read(diningAreas.areaId.max()) ?? 0) + 1;
  }

  Future<int> nextLocalTableTypeId() async {
    final row = await (selectOnly(tableTypes)
          ..addColumns([tableTypes.tableTypeId.max()]))
        .getSingle();
    return (row.read(tableTypes.tableTypeId.max()) ?? 0) + 1;
  }

  Future<int> nextLocalPortionMasterId() async {
    final row = await (selectOnly(portionMasters)
          ..addColumns([portionMasters.portionMasterId.max()]))
        .getSingle();
    return (row.read(portionMasters.portionMasterId.max()) ?? 0) + 1;
  }

  Future<int> insertLocalDiningArea({
    required String areaName,
    int areaSortOrder = 0,
  }) async {
    final id = await nextLocalDiningAreaId();
    final network = appDatabaseNetworkStatus(prefix: 'area_');
    await into(diningAreas).insert(
      stampDiningArea(
        DiningAreasCompanion.insert(
          areaId: Value(id),
          areaName: Value(areaName),
          areaSortOrder: Value(areaSortOrder),
          areaNetworkStatus: Value(network),
          areaSyncStatus: const Value('0'),
        ),
      ),
      mode: InsertMode.insertOrReplace,
    );
    return id;
  }

  Future<int> insertLocalTableType({
    required String tableTypeName,
    int tableTypeSortOrder = 0,
  }) async {
    final id = await nextLocalTableTypeId();
    final network = appDatabaseNetworkStatus(prefix: 'tt_');
    await into(tableTypes).insert(
      stampTableType(
        TableTypesCompanion.insert(
          tableTypeId: Value(id),
          tableTypeName: Value(tableTypeName),
          tableTypeSortOrder: Value(tableTypeSortOrder),
          tableTypeNetworkStatus: Value(network),
          tableTypeSyncStatus: const Value('0'),
        ),
      ),
      mode: InsertMode.insertOrReplace,
    );
    return id;
  }

  Future<int> insertLocalPortionMaster({required String portionName}) async {
    final id = await nextLocalPortionMasterId();
    final network = appDatabaseNetworkStatus(prefix: 'pm_');
    await into(portionMasters).insert(
      PortionMastersCompanion.insert(
        portionMasterId: Value(id),
        portionName: Value(portionName),
        portionMasterNetworkStatus: Value(network),
        portionMasterSyncStatus: const Value('0'),
      ),
      mode: InsertMode.insertOrReplace,
    );
    return id;
  }

  Future<void> markDiningAreaSynced(int areaId) async {
    await (update(diningAreas)..where((t) => t.areaId.equals(areaId))).write(
      const DiningAreasCompanion(areaSyncStatus: Value('1')),
    );
  }

  Future<void> markTableTypeSynced(int tableTypeId) async {
    await (update(tableTypes)..where((t) => t.tableTypeId.equals(tableTypeId)))
        .write(
      const TableTypesCompanion(tableTypeSyncStatus: Value('1')),
    );
  }

  Future<void> markPortionMasterSynced(int portionMasterId) async {
    await (update(portionMasters)
          ..where((t) => t.portionMasterId.equals(portionMasterId)))
        .write(
      const PortionMastersCompanion(portionMasterSyncStatus: Value('1')),
    );
  }

  Future<void> softDeletePortionMaster(int portionMasterId) async {
    await (update(portionMasters)
          ..where((t) => t.portionMasterId.equals(portionMasterId)))
        .write(
      const PortionMastersCompanion(
        portionMasterDeletedStatus: Value('1'),
        portionMasterSyncStatus: Value('0'),
      ),
    );
  }

  Future<void> updateLocalPortionMaster({
    required int portionMasterId,
    required String portionName,
  }) async {
    await (update(portionMasters)
          ..where((t) => t.portionMasterId.equals(portionMasterId)))
        .write(
      PortionMastersCompanion(
        portionName: Value(portionName),
        portionMasterSyncStatus: const Value('0'),
      ),
    );
  }

  Future<void> updateLocalDiningArea({
    required int areaId,
    required String areaName,
  }) async {
    await (update(diningAreas)..where((t) => t.areaId.equals(areaId))).write(
      DiningAreasCompanion(
        areaName: Value(areaName),
        areaSyncStatus: const Value('0'),
      ),
    );
  }

  Future<void> deactivateDiningArea(int areaId) async {
    await (update(diningAreas)..where((t) => t.areaId.equals(areaId))).write(
      const DiningAreasCompanion(
        areaActive: Value('0'),
        areaSyncStatus: Value('0'),
      ),
    );
  }

  Future<void> updateLocalTableType({
    required int tableTypeId,
    required String tableTypeName,
  }) async {
    await (update(tableTypes)..where((t) => t.tableTypeId.equals(tableTypeId)))
        .write(
      TableTypesCompanion(
        tableTypeName: Value(tableTypeName),
        tableTypeSyncStatus: const Value('0'),
      ),
    );
  }

  Future<void> deactivateTableType(int tableTypeId) async {
    await (update(tableTypes)..where((t) => t.tableTypeId.equals(tableTypeId)))
        .write(
      const TableTypesCompanion(
        tableTypeActive: Value('0'),
        tableTypeSyncStatus: Value('0'),
      ),
    );
  }

  Future<void> updateLocalPosTable({
    required int tableId,
    required String tableNumber,
    required String displayName,
    required int capacity,
    int? areaId,
  }) async {
    await (update(posTables)..where((t) => t.tableId.equals(tableId))).write(
      PosTablesCompanion(
        tableNumber: Value(tableNumber),
        displayName: Value(displayName),
        capacity: Value(capacity),
        areaId: areaId == null ? const Value.absent() : Value(areaId),
        posTableStatus: const Value('0'),
      ),
    );
  }

  Future<void> deactivatePosTable(int tableId) async {
    await (update(posTables)..where((t) => t.tableId.equals(tableId))).write(
      const PosTablesCompanion(tableActive: Value('0')),
    );
  }

  Future<int> insertLocalPosTable({
    required String tableNumber,
    required String displayName,
    int capacity = 4,
    int? areaId,
    int? sortOrder,
  }) async {
    final maxRow = await (selectOnly(posTables)
          ..addColumns([posTables.tableId.max()]))
        .getSingle();
    final id = (maxRow.read(posTables.tableId.max()) ?? 0) + 1;
    await into(posTables).insert(
      stampPosTable(
        PosTablesCompanion.insert(
          tableId: Value(id),
          tableNumber: tableNumber,
          displayName: Value(displayName),
          capacity: Value(capacity),
          areaId: Value(areaId),
          sortOrder: Value(sortOrder ?? id),
          posTableNetworkStatus: Value(
            'tbl_${DateTime.now().millisecondsSinceEpoch}',
          ),
          posTableStatus: const Value('0'),
        ),
      ),
      mode: InsertMode.insertOrReplace,
    );
    return id;
  }

  Stream<List<ProductPortion>> watchActivePortions() {
    return (select(productPortions)
          ..where((t) => t.portionDeletedStatus.equals('0'))
          ..orderBy([
            (t) => OrderingTerm.asc(t.portionSortOrder),
            (t) => OrderingTerm.asc(t.portionName),
          ]))
        .watch();
  }

  /// Updates header fields on a settled/pending bill and marks it for re-upload.
  Future<void> updateInvoiceHeader({
    required int invoiceId,
    String? customerName,
    String? customerMobile,
    String? customerEmail,
    String? customerAddress,
    String? paymentMode,
    double? cashAmount,
    double? upiAmount,
    double? discount,
    String? discountType,
    double? packingCharge,
    String? packingChargeType,
  }) async {
    final invoice = await getInvoiceById(invoiceId);
    if (invoice == null) {
      throw StateError('Invoice not found');
    }

    final nextDiscount = discount ?? invoice.discount;
    final nextDiscountType = discountType ?? invoice.discountType;
    final nextPacking = packingCharge ?? invoice.packingCharge;
    final nextPackingType = packingChargeType ?? invoice.packingChargeType;
    final discountValue = nextDiscountType.toLowerCase().startsWith('p')
        ? (invoice.subTotal * nextDiscount / 100)
        : nextDiscount;
    final packingValue = nextPackingType.toLowerCase().startsWith('p')
        ? (invoice.subTotal * nextPacking / 100)
        : nextPacking;
    final nextTotal = double.parse(
      (invoice.subTotal + invoice.totalGstAmount + packingValue - discountValue)
          .clamp(0, double.infinity)
          .toStringAsFixed(2),
    );

    await (update(invoices)..where((t) => t.invoiceId.equals(invoiceId))).write(
      InvoicesCompanion(
        customerName: Value(customerName ?? invoice.customerName),
        customerMobile: Value(customerMobile ?? invoice.customerMobile),
        customerEmail: Value(customerEmail ?? invoice.customerEmail),
        customerAddress: Value(customerAddress ?? invoice.customerAddress),
        paymentMode: Value(paymentMode ?? invoice.paymentMode),
        cashAmount: Value(cashAmount ?? invoice.cashAmount),
        upiAmount: Value(upiAmount ?? invoice.upiAmount),
        discount: Value(nextDiscount),
        discountType: Value(nextDiscountType),
        packingCharge: Value(nextPacking),
        packingChargeType: Value(nextPackingType),
        totalAmount: Value(nextTotal),
        invoiceSyncStatus: const Value('0'),
      ),
    );
  }

  Stream<List<ProductCategory>> watchActiveCategories() {
    // Match WithTable: show non-deleted only (status is sync flag on Android).
    return (select(productCategories)
          ..where((t) => t.categoryDeletedStatus.equals('0'))
          ..orderBy([
            (t) => OrderingTerm.asc(t.categorySortOrder),
            (t) => OrderingTerm.asc(t.categoryName),
          ]))
        .watch();
  }

  Stream<List<Product>> watchActiveProducts({
    int? categoryId,
    int? subcategoryId,
  }) {
    final query = select(products)
      ..where((t) => t.productDeletedStatus.equals('0'))
      ..orderBy([(t) => OrderingTerm.asc(t.productName)]);

    if (categoryId != null) {
      query.where((t) => t.categoryId.equals(categoryId));
    }
    if (subcategoryId != null) {
      query.where((t) => t.subcategoryId.equals(subcategoryId));
    }

    return query.watch();
  }

  Future<int> countActiveCombos() async {
    final rows = await (select(combos)
          ..where(
            (t) =>
                t.comboDeletedStatus.equals('0') &
                t.comboActiveStatus.equals('1'),
          ))
        .get();
    return rows.length;
  }

  Future<int> countActiveProducts() async {
    final countExp = products.productId.count();
    final query = selectOnly(products)
      ..addColumns([countExp])
      ..where(products.productDeletedStatus.equals('0'));
    final row = await query.getSingle();
    return row.read(countExp) ?? 0;
  }

  Future<int> countActiveCategories() async {
    final countExp = productCategories.categoryId.count();
    final query = selectOnly(productCategories)
      ..addColumns([countExp])
      ..where(productCategories.categoryDeletedStatus.equals('0'));
    final row = await query.getSingle();
    return row.read(countExp) ?? 0;
  }

  Stream<List<PosTable>> watchActivePosTables() {
    return (select(posTables)
          ..where(
            (t) => t.tableActive.equals('1') & branchMatches(t.branchId),
          )
          ..orderBy([
            (t) => OrderingTerm.asc(t.sortOrder),
            (t) => OrderingTerm.asc(t.tableNumber),
          ]))
        .watch();
  }

  Future<int> countActivePosTables() async {
    final countExp = posTables.tableId.count();
    final query = selectOnly(posTables)
      ..addColumns([countExp])
      ..where(
        posTables.tableActive.equals('1') &
            branchMatches(posTables.branchId),
      );
    final row = await query.getSingle();
    return row.read(countExp) ?? 0;
  }

  Future<void> seedDefaultTablesIfEmpty() async {
    final count = await countActivePosTables();
    if (count > 0) return;

    final rows = List.generate(8, (index) {
      final n = index + 1;
      return PosTablesCompanion.insert(
        tableId: Value(n),
        tableNumber: '$n',
        displayName: Value('Table $n'),
        capacity: const Value(4),
        sortOrder: Value(n),
      );
    });
    await replacePosTables(rows);
  }

  Stream<List<DiningSession>> watchOpenDiningSessions() {
    return (select(diningSessions)
          ..where((t) => isOpenSession(t) & branchMatches(t.branchId))
          ..orderBy([(t) => OrderingTerm.desc(t.startedAt)]))
        .watch();
  }

  List<String> parseJoinedTables(String? csv) {
    if (csv == null || csv.trim().isEmpty) return const [];
    return csv
        .split(',')
        .map((e) => e.trim())
        .where((e) => e.isNotEmpty)
        .toList();
  }

  String encodeJoinedTables(Iterable<String> tables) {
    final unique = <String>{};
    for (final t in tables) {
      final trimmed = t.trim();
      if (trimmed.isNotEmpty) unique.add(trimmed);
    }
    return unique.join(',');
  }

  Expression<bool> isOpenSession(DiningSessions t) =>
      t.sessionStatus.equals('RUNNING') |
      t.sessionStatus.equals('HOLD') |
      t.sessionStatus.equals('BILL_REQUEST') |
      t.sessionStatus.equals('PARTIALLY_PAID');

  Future<DiningSession?> getOpenSessionForTable(String tableNumber) async {
    final primary = await (select(diningSessions)
          ..where(
            (t) =>
                t.primaryTableNumber.equals(tableNumber) &
                isOpenSession(t) &
                branchMatches(t.branchId),
          )
          ..orderBy([(t) => OrderingTerm.desc(t.startedAt)])
          ..limit(1))
        .getSingleOrNull();
    if (primary != null) return primary;

    final open = await (select(diningSessions)
          ..where((t) => isOpenSession(t) & branchMatches(t.branchId)))
        .get();
    for (final session in open) {
      if (parseJoinedTables(session.joinedTableNumbers)
          .contains(tableNumber)) {
        return session;
      }
    }
    return null;
  }

  Future<DiningSession> openOrGetDiningSession(
    String tableNumber, {
    int guestCount = 1,
    String? waiterName,
  }) async {
    final existing = await getOpenSessionForTable(tableNumber);
    if (existing != null) {
      if (waiterName != null &&
          waiterName.trim().isNotEmpty &&
          (existing.waiterName == null || existing.waiterName!.trim().isEmpty)) {
        await updateDiningSessionMeta(
          existing.sessionId,
          waiterName: waiterName.trim(),
        );
        return (select(diningSessions)
              ..where((t) => t.sessionId.equals(existing.sessionId)))
            .getSingle();
      }
      return existing;
    }

    final id = await into(diningSessions).insert(
      stampDiningSession(
        DiningSessionsCompanion.insert(
          primaryTableNumber: tableNumber,
          guestCount: Value(guestCount),
          startedAt: DateTime.now(),
          waiterName: Value(waiterName?.trim().isEmpty == true
              ? null
              : waiterName?.trim()),
          sessionNetworkStatus: Value(appDatabaseNetworkStatus(prefix: 'ds_')),
          sessionSyncStatus: const Value('0'),
        ),
      ),
    );
    return (select(diningSessions)..where((t) => t.sessionId.equals(id)))
        .getSingle();
  }

  Future<void> updateDiningSessionMeta(
    int sessionId, {
    String? waiterName,
    int? guestCount,
    String? unpaidInvoiceNumber,
    bool clearUnpaidInvoiceNumber = false,
  }) async {
    await (update(diningSessions)..where((t) => t.sessionId.equals(sessionId)))
        .write(
      DiningSessionsCompanion(
        waiterName: waiterName == null
            ? const Value.absent()
            : Value(waiterName.trim().isEmpty ? null : waiterName.trim()),
        guestCount: guestCount == null
            ? const Value.absent()
            : Value(guestCount < 1 ? 1 : guestCount),
        unpaidInvoiceNumber: clearUnpaidInvoiceNumber
            ? const Value(null)
            : unpaidInvoiceNumber == null
                ? const Value.absent()
                : Value(
                    unpaidInvoiceNumber.trim().isEmpty
                        ? null
                        : unpaidInvoiceNumber.trim(),
                  ),
        sessionSyncStatus: const Value('0'),
      ),
    );
  }

  Future<void> markDiningSessionPending(int sessionId) async {
    await (update(diningSessions)..where((t) => t.sessionId.equals(sessionId)))
        .write(
      const DiningSessionsCompanion(sessionSyncStatus: Value('0')),
    );
  }

  Future<void> settleDiningSession(int sessionId) async {
    await (update(diningSessions)..where((t) => t.sessionId.equals(sessionId)))
        .write(
      DiningSessionsCompanion(
        sessionStatus: const Value('SETTLED'),
        closedAt: Value(DateTime.now()),
        sessionSyncStatus: const Value('0'),
      ),
    );
  }

  Future<void> addSessionPaidAmount({
    required int sessionId,
    required double amount,
  }) async {
    final session = await (select(diningSessions)
          ..where((t) => t.sessionId.equals(sessionId)))
        .getSingleOrNull();
    if (session == null) return;
    final paid = session.paidAmount + amount;
    await (update(diningSessions)..where((t) => t.sessionId.equals(sessionId)))
        .write(
      DiningSessionsCompanion(
        paidAmount: Value(double.parse(paid.toStringAsFixed(2))),
        sessionStatus: const Value('PARTIALLY_PAID'),
        sessionSyncStatus: const Value('0'),
        sessionVersion: Value(session.sessionVersion + 1),
      ),
    );
  }

  /// Merges [secondaryTable] into [primaryTable]'s running session.
  Future<DiningSession> joinTables({
    required String primaryTable,
    required String secondaryTable,
  }) async {
    if (primaryTable == secondaryTable) {
      throw StateError('Cannot join a table with itself');
    }

    return transaction(() async {
      final primary = await openOrGetDiningSession(primaryTable);
      final secondary = await getOpenSessionForTable(secondaryTable);

      // Move / merge secondary cart into primary scope.
      final secondaryItems =
          await getCartItems(cartScope: secondaryTable);
      for (final item in secondaryItems) {
        final existing = await (select(cartItems)
              ..where(
                (t) =>
                    t.productId.equals(item.productId) &
                    t.cartScope.equals(primaryTable) &
                    t.portionId.equals(item.portionId),
              ))
            .getSingleOrNull();
        if (existing == null) {
          await into(cartItems).insert(
            CartItemsCompanion.insert(
              productId: item.productId,
              cartScope: Value(primaryTable),
              portionId: Value(item.portionId),
              productName: Value(item.productName),
              categoryId: Value(item.categoryId),
              categoryName: Value(item.categoryName),
              productCode: Value(item.productCode),
              unitPrice: Value(item.unitPrice),
              gstPercent: Value(item.gstPercent),
              quantity: Value(item.quantity),
              printedQuantity: Value(item.printedQuantity),
              productUnit: Value(item.productUnit),
              diningSessionId: Value(primary.sessionId),
              lineType: Value(item.lineType),
              comboId: Value(item.comboId),
              comboNetworkStatus: Value(item.comboNetworkStatus),
            ),
          );
        } else {
          await (update(cartItems)
                ..where(
                  (t) =>
                      t.productId.equals(item.productId) &
                      t.cartScope.equals(primaryTable) &
                      t.portionId.equals(item.portionId),
                ))
              .write(
            CartItemsCompanion(
              quantity: Value(existing.quantity + item.quantity),
              printedQuantity: Value(
                existing.printedQuantity + item.printedQuantity,
              ),
              diningSessionId: Value(primary.sessionId),
              updatedAt: Value(DateTime.now()),
            ),
          );
        }
      }
      await clearCart(cartScope: secondaryTable);

      // Move KOT table labels to primary.
      await (update(kots)..where((t) => t.tableNumber.equals(secondaryTable)))
          .write(KotsCompanion(tableNumber: Value(primaryTable)));

      final joined = {
        ...parseJoinedTables(primary.joinedTableNumbers),
        if (secondary != null)
          ...parseJoinedTables(secondary.joinedTableNumbers),
        secondaryTable,
      }..remove(primary.primaryTableNumber);

      await (update(diningSessions)
            ..where((t) => t.sessionId.equals(primary.sessionId)))
          .write(
        DiningSessionsCompanion(
          joinedTableNumbers: Value(encodeJoinedTables(joined)),
        ),
      );

      if (secondary != null &&
          secondary.sessionId != primary.sessionId) {
        await (update(diningSessions)
              ..where((t) => t.sessionId.equals(secondary.sessionId)))
            .write(
          DiningSessionsCompanion(
            sessionStatus: const Value('CLOSED'),
            closedAt: Value(DateTime.now()),
            joinedTableNumbers: const Value(''),
          ),
        );
      }

      return (select(diningSessions)
            ..where((t) => t.sessionId.equals(primary.sessionId)))
          .getSingle();
    });
  }

  /// Clears join CSV only — cart stays on primary (Android parity).
  Future<void> splitJoinedTables(int sessionId) async {
    await (update(diningSessions)..where((t) => t.sessionId.equals(sessionId)))
        .write(const DiningSessionsCompanion(joinedTableNumbers: Value('')));
  }

  Future<List<CartItem>> getUnprintedCartItems({
    required String cartScope,
  }) async {
    final items = await getCartItems(cartScope: cartScope);
    return items
        .where((e) => e.quantity > e.printedQuantity)
        .toList(growable: false);
  }

  Future<int> nextRoundNumber(int sessionId) async {
    final maxExp = orderRounds.roundNumber.max();
    final query = selectOnly(orderRounds)
      ..addColumns([maxExp])
      ..where(orderRounds.sessionId.equals(sessionId));
    final row = await query.getSingle();
    return (row.read(maxExp) ?? 0) + 1;
  }

  /// Creates a delta KOT for unprinted cart qty on a dine-in table.
  Future<KotTicket> createKotFromUnprintedCart({
    required int sessionId,
    required String tableNumber,
    String cartScope = '',
    String kitchenName = 'Main Kitchen',
  }) async {
    final scope = cartScope.isEmpty ? tableNumber : cartScope;
    final unprinted = await getUnprintedCartItems(cartScope: scope);
    if (unprinted.isEmpty) {
      throw StateError('No new items to send to kitchen');
    }

    return transaction(() async {
      final roundNumber = await nextRoundNumber(sessionId);
      final kotNumber = 'KOT-${roundNumber.toString().padLeft(3, '0')}';
      final now = DateTime.now();

      final roundId = await into(orderRounds).insert(
        stampOrderRound(
          OrderRoundsCompanion.insert(
            sessionId: sessionId,
            roundNumber: roundNumber,
            createdAt: Value(now),
          ),
        ),
      );

      final kotId = await into(kots).insert(
        stampKot(
          KotsCompanion.insert(
            sessionId: sessionId,
            orderRoundId: roundId,
            kotNumber: kotNumber,
            tableNumber: tableNumber,
            printStatus: const Value('PENDING'),
            kitchenName: Value(kitchenName),
            createdAt: Value(now),
          ),
        ),
      );

      await (update(orderRounds)
            ..where((t) => t.orderRoundId.equals(roundId)))
          .write(OrderRoundsCompanion(kotId: Value(kotId)));

      final lines = <KotItem>[];
      for (final item in unprinted) {
        final delta = item.quantity - item.printedQuantity;
        final kotItemId = await into(kotItems).insert(
          stampKotItem(
            KotItemsCompanion.insert(
              kotId: kotId,
              cartId: Value(item.cartId),
              productId: Value(item.productId),
              productName: Value(item.productName),
              productQuantity: Value(delta),
              portionName: Value(item.portionName),
              productUnit: Value(item.productUnit),
            ),
          ),
        );
        lines.add(
          await (select(kotItems)
                ..where((t) => t.kotItemId.equals(kotItemId)))
              .getSingle(),
        );

        await (update(cartItems)
              ..where(
                (t) =>
                    t.productId.equals(item.productId) &
                    t.cartScope.equals(scope) &
                    t.portionId.equals(item.portionId),
              ))
            .write(
          CartItemsCompanion(
            printedQuantity: Value(item.quantity),
            orderRoundId: Value(roundId),
            kotPrinted: const Value('1'),
            updatedAt: Value(now),
          ),
        );
      }

      final kot = await (select(kots)..where((t) => t.kotId.equals(kotId)))
          .getSingle();
      return KotTicket(kot: kot, items: lines, roundNumber: roundNumber);
    });
  }

  Future<void> markKotPrinted(int kotId, {bool failed = false}) async {
    await (update(kots)..where((t) => t.kotId.equals(kotId))).write(
      KotsCompanion(
        printStatus: Value(failed ? 'FAILED' : 'PRINTED'),
      ),
    );
  }

  Future<KotTicket?> getKotTicket(int kotId) async {
    final kot =
        await (select(kots)..where((t) => t.kotId.equals(kotId)))
            .getSingleOrNull();
    if (kot == null) return null;
    final items = await (select(kotItems)
          ..where((t) => t.kotId.equals(kotId)))
        .get();
    final round = await (select(orderRounds)
          ..where((t) => t.orderRoundId.equals(kot.orderRoundId)))
        .getSingleOrNull();
    return KotTicket(
      kot: kot,
      items: items,
      roundNumber: round?.roundNumber ?? 0,
    );
  }

  Stream<List<Kot>> watchKotsForSession(int sessionId) {
    return (select(kots)
          ..where((t) => t.sessionId.equals(sessionId))
          ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]))
        .watch();
  }

  Future<void> replaceMessMembers(List<MessMembersCompanion> rows) async {
    await transaction(() async {
      await delete(messMembers).go();
      await batch((b) => b.insertAll(messMembers, rows));
    });
  }

  Stream<List<MessMember>> watchMessMembers() {
    return (select(messMembers)
          ..where((t) => t.memberStatus.equals('1'))
          ..orderBy([(t) => OrderingTerm.asc(t.memberName)]))
        .watch();
  }

  Future<MessMember?> getMessMember(int memberId) {
    return (select(messMembers)..where((t) => t.memberId.equals(memberId)))
        .getSingleOrNull();
  }

  Future<int> upsertLocalMessMember({
    required String memberName,
    String? mobile,
    String? altMobile,
    String? address,
    String? registrationNo,
    String memberType = 'student',
    String? rollNo,
    String? college,
    String? studentYear,
    String? company,
  }) async {
    final network = 'local_${DateTime.now().millisecondsSinceEpoch}';
    // Negative local ids avoid colliding with server ids until sync assigns one.
    final minId = await (selectOnly(messMembers)
          ..addColumns([messMembers.memberId.min()]))
        .getSingle();
    final nextLocalId = ((minId.read(messMembers.memberId.min()) ?? 0) < 0
            ? (minId.read(messMembers.memberId.min())! - 1)
            : -1);

    await into(messMembers).insert(
      MessMembersCompanion.insert(
        memberId: Value(nextLocalId),
        memberName: Value(memberName),
        memberMobileNumber: Value(mobile),
        memberAltenetMobileNumber: Value(altMobile),
        memberAddress: Value(address),
        registrationNo: Value(registrationNo),
        memberType: Value(memberType),
        rollNo: Value(rollNo),
        college: Value(college),
        studentYear: Value(studentYear),
        company: Value(company),
        memberNetworkStatus: Value(network),
        memberSyncStatus: const Value('0'),
      ),
      mode: InsertMode.insertOrReplace,
    );
    return nextLocalId;
  }

  Future<MessToken> issueMessToken({
    required String tokenCode,
    String? memberId,
    String? memberName,
    String? memberMobile,
    String memberType = 'member',
    String messType = 'Lunch',
    double tokenAmount = 0,
  }) async {
    final id = await into(messTokens).insert(
      MessTokensCompanion.insert(
        tokenCode: tokenCode,
        memberId: Value(memberId),
        memberName: Value(memberName),
        memberMobile: Value(memberMobile),
        memberType: Value(memberType),
        messType: Value(messType),
        tokenAmount: Value(tokenAmount),
        tokenDate: DateTime.now(),
        tokenState: const Value('active'),
        tokenNetworkStatus: Value(
          'tok_${DateTime.now().millisecondsSinceEpoch}',
        ),
      ),
    );
    return (select(messTokens)..where((t) => t.tokenId.equals(id))).getSingle();
  }

  Future<MessToken?> getMessTokenByCode(String tokenCode) {
    return (select(messTokens)..where((t) => t.tokenCode.equals(tokenCode)))
        .getSingleOrNull();
  }

  Future<MessToken?> verifyMessToken(String tokenCode) async {
    final token = await getMessTokenByCode(tokenCode);
    if (token == null) return null;
    if (token.tokenState == 'verified') return token;

    await (update(messTokens)..where((t) => t.tokenId.equals(token.tokenId)))
        .write(
      MessTokensCompanion(
        tokenState: const Value('verified'),
        verifiedDate: Value(DateTime.now()),
        // Keep insert sync status; only verify queue goes pending.
        verifyNetworkStatus: Value(appDatabaseNetworkStatus(prefix: 'ver_')),
        verifyStatus: const Value('0'),
      ),
    );
    return getMessTokenByCode(tokenCode);
  }

  /// Tokens verified offline that still need `verifyMessToken.php`.
  Future<List<MessToken>> getPendingMessTokenVerifies({int limit = 100}) {
    return (select(messTokens)
          ..where(
            (t) =>
                t.tokenState.equals('verified') & t.verifyStatus.equals('0'),
          )
          ..orderBy([(t) => OrderingTerm.asc(t.tokenId)])
          ..limit(limit))
        .get();
  }

  Future<void> markMessTokenVerifySynced(int tokenId) async {
    await (update(messTokens)..where((t) => t.tokenId.equals(tokenId))).write(
      const MessTokensCompanion(verifyStatus: Value('1')),
    );
  }

  /// Deletes an invoice line and recomputes header totals.
  /// Works for synced bills too — marks header pending re-upload.
  /// Enqueues cloud line-delete when [invoiceItemNetworkStatus] is present.
  Future<void> deleteInvoiceItemAndRecompute(int invoiceItemId) async {
    await transaction(() async {
      final item = await (select(invoiceItems)
            ..where((t) => t.invoiceItemId.equals(invoiceItemId)))
          .getSingleOrNull();
      if (item == null) return;

      final invoice = await getInvoiceByNumber(item.invoiceNumber);
      if (invoice == null) return;
      if (invoice.invoiceOrderStatus == 'cancelled' ||
          invoice.invoiceOrderStatus == 'refunded') {
        throw StateError('Cannot edit voided / refunded bills');
      }

      final remainingBefore = await getInvoiceItems(item.invoiceNumber);
      if (remainingBefore.length <= 1) {
        throw StateError('Keep at least one item on the bill');
      }

      final network = item.invoiceItemNetworkStatus?.trim();
      if (network != null && network.isNotEmpty) {
        await enqueueInvoiceProductDelete(
          invoiceNumber: item.invoiceNumber,
          invoiceProductNetworkStatus: network,
        );
        await (delete(invoiceComboItems)
              ..where(
                (t) =>
                    t.invoiceNumber.equals(item.invoiceNumber) &
                    t.invoiceProductNetworkStatus.equals(network),
              ))
            .go();
      }

      await (delete(invoiceItems)
            ..where((t) => t.invoiceItemId.equals(invoiceItemId)))
          .go();

      await recomputeInvoiceTotals(invoice.invoiceId, item.invoiceNumber);
    });
  }

  Future<void> enqueueInvoiceProductDelete({
    required String invoiceNumber,
    required String invoiceProductNetworkStatus,
  }) async {
    final existing = await (select(invoiceProductDeleteQueue)
          ..where(
            (t) => t.invoiceProductNetworkStatus
                .equals(invoiceProductNetworkStatus),
          )
          ..limit(1))
        .getSingleOrNull();
    if (existing != null) return;
    await into(invoiceProductDeleteQueue).insert(
      InvoiceProductDeleteQueueCompanion.insert(
        invoiceNumber: Value(invoiceNumber),
        invoiceProductNetworkStatus: Value(invoiceProductNetworkStatus),
      ),
    );
  }

  Future<List<InvoiceProductDeleteQueueData>>
      getPendingInvoiceProductDeletes({int limit = 100}) {
    return (select(invoiceProductDeleteQueue)
          ..orderBy([(t) => OrderingTerm.asc(t.deleteId)])
          ..limit(limit))
        .get();
  }

  Future<void> removeInvoiceProductDelete(int deleteId) async {
    await (delete(invoiceProductDeleteQueue)
          ..where((t) => t.deleteId.equals(deleteId)))
        .go();
  }

  Future<void> removeInvoiceProductDeleteByNetworkStatus(
    String invoiceProductNetworkStatus,
  ) async {
    await (delete(invoiceProductDeleteQueue)
          ..where(
            (t) => t.invoiceProductNetworkStatus
                .equals(invoiceProductNetworkStatus),
          ))
        .go();
  }

  Future<Invoice?> latestInvoiceForTable(String tableNumber) {
    return (select(invoices)
          ..where(
            (t) =>
                t.noOfTable.equals(tableNumber) &
                t.invoiceOrderStatus.isNotValue('cancelled') &
                t.invoiceOrderStatus.isNotValue('refunded'),
          )
          ..orderBy([(t) => OrderingTerm.desc(t.invoiceId)])
          ..limit(1))
        .getSingleOrNull();
  }

  Future<Invoice?> latestInvoiceOfType(String invoiceType) {
    return (select(invoices)
          ..where(
            (t) =>
                t.invoiceType.equals(invoiceType) &
                t.invoiceOrderStatus.isNotValue('cancelled') &
                t.invoiceOrderStatus.isNotValue('refunded'),
          )
          ..orderBy([(t) => OrderingTerm.desc(t.invoiceId)])
          ..limit(1))
        .getSingleOrNull();
  }

  Future<void> addInvoiceItemLine({
    required int invoiceId,
    required String productName,
    required double productPrice,
    required int quantity,
    int? productId,
    String? productCode,
    String? categoryName,
    double cgst = 0,
    double sgst = 0,
    String invoiceItemType = 'product',
    int? portionId,
    String? portionName,
  }) async {
    await transaction(() async {
      final invoice = await getInvoiceById(invoiceId);
      if (invoice == null) throw StateError('Invoice not found');
      if (invoice.invoiceOrderStatus == 'cancelled' ||
          invoice.invoiceOrderStatus == 'refunded') {
        throw StateError('Cannot edit voided / refunded bills');
      }
      await into(invoiceItems).insert(
        stampInvoiceItem(
          InvoiceItemsCompanion.insert(
            invoiceNumber: invoice.invoiceNumber,
            productId: Value(productId),
            productName: Value(productName),
            productCode: Value(productCode),
            productPrice: Value(productPrice),
            productQuantity: Value(quantity),
            productCgst: Value(cgst),
            productSgst: Value(sgst),
            categoryName: Value(categoryName),
            invoiceItemType: Value(invoiceItemType),
            portionId: Value(portionId),
            portionName: Value(portionName),
            snapshotProductName: Value(productName),
            snapshotLinePrice: Value(productPrice),
            productStatus: const Value('completed'),
            invoiceItemNetworkStatus: Value(appDatabaseNetworkStatus()),
            invoiceItemSyncStatus: const Value('0'),
          ),
        ),
      );
      await recomputeInvoiceTotals(invoiceId, invoice.invoiceNumber);
    });
  }

  Future<void> updateInvoiceItemQuantity({
    required int invoiceItemId,
    required int quantity,
    double? productPrice,
  }) async {
    if (quantity <= 0) {
      throw StateError('Quantity must be at least 1');
    }
    await transaction(() async {
      final item = await (select(invoiceItems)
            ..where((t) => t.invoiceItemId.equals(invoiceItemId)))
          .getSingleOrNull();
      if (item == null) return;
      final invoice = await getInvoiceByNumber(item.invoiceNumber);
      if (invoice == null) return;
      if (invoice.invoiceOrderStatus == 'cancelled' ||
          invoice.invoiceOrderStatus == 'refunded') {
        throw StateError('Cannot edit voided / refunded bills');
      }
      await (update(invoiceItems)
            ..where((t) => t.invoiceItemId.equals(invoiceItemId)))
          .write(
        InvoiceItemsCompanion(
          productQuantity: Value(quantity),
          productPrice: productPrice == null
              ? const Value.absent()
              : Value(productPrice),
          invoiceItemSyncStatus: const Value('0'),
        ),
      );
      await recomputeInvoiceTotals(invoice.invoiceId, item.invoiceNumber);
    });
  }

  Future<void> recomputeInvoiceTotals(
    int invoiceId,
    String invoiceNumber,
  ) async {
    final invoice = await getInvoiceById(invoiceId);
    if (invoice == null) return;
    final remaining = await getInvoiceItems(invoiceNumber);
    var subtotal = 0.0;
    var taxTotal = 0.0;
    var qtyTotal = 0;
    for (final row in remaining) {
      final lineBase = row.productPrice * row.productQuantity;
      subtotal += lineBase;
      taxTotal += lineBase * (row.productCgst + row.productSgst) / 100;
      qtyTotal += row.productQuantity;
    }
    subtotal = double.parse(subtotal.toStringAsFixed(2));
    taxTotal = double.parse(taxTotal.toStringAsFixed(2));
    final discountValue = invoice.discountType.toLowerCase().startsWith('p')
        ? subtotal * invoice.discount / 100
        : invoice.discount;
    final packingValue = invoice.packingChargeType.toLowerCase().startsWith('p')
        ? subtotal * invoice.packingCharge / 100
        : invoice.packingCharge;
    final total = double.parse(
      (subtotal + taxTotal + packingValue - discountValue)
          .clamp(0, double.infinity)
          .toStringAsFixed(2),
    );

    await (update(invoices)..where((t) => t.invoiceId.equals(invoiceId)))
        .write(
      InvoicesCompanion(
        subTotal: Value(subtotal),
        totalGstAmount: Value(taxTotal),
        totalAmount: Value(total),
        itemCount: Value(qtyTotal),
        invoiceSyncStatus: const Value('0'),
        cashAmount: Value(
          invoice.paymentMode == 'UPI' ? 0 : total,
        ),
        upiAmount: Value(
          invoice.paymentMode == 'Cash'
              ? 0
              : invoice.paymentMode == 'UPI'
                  ? total
                  : invoice.upiAmount,
        ),
      ),
    );
  }

  Future<void> voidInvoiceLocally(int invoiceId) async {
    await (update(invoices)..where((t) => t.invoiceId.equals(invoiceId)))
        .write(
      const InvoicesCompanion(
        invoiceOrderStatus: Value('cancelled'),
        invoiceSyncStatus: Value('0'),
      ),
    );
  }

  Future<void> refundInvoiceLocally(int invoiceId) async {
    await (update(invoices)..where((t) => t.invoiceId.equals(invoiceId)))
        .write(
      const InvoicesCompanion(
        invoiceOrderStatus: Value('refunded'),
        invoiceSyncStatus: Value('0'),
      ),
    );
  }

  Stream<List<MessToken>> watchTodayMessTokens() {
    final now = DateTime.now();
    final start = DateTime(now.year, now.month, now.day);
    final end = start.add(const Duration(days: 1));
    return (select(messTokens)
          ..where(
            (t) =>
                t.tokenDate.isBiggerOrEqualValue(start) &
                t.tokenDate.isSmallerThanValue(end),
          )
          ..orderBy([(t) => OrderingTerm.desc(t.tokenDate)]))
        .watch();
  }

  Stream<List<CartItem>> watchAllCartItems() {
    return (select(cartItems)
          ..orderBy([
            (t) => OrderingTerm.desc(t.updatedAt),
            (t) => OrderingTerm.asc(t.productName),
          ]))
        .watch();
  }

  Stream<List<CartItem>> watchCartItems({String cartScope = ''}) {
    return (select(cartItems)
          ..where((t) => t.cartScope.equals(cartScope))
          ..orderBy([
            (t) => OrderingTerm.desc(t.updatedAt),
            (t) => OrderingTerm.asc(t.productName),
          ]))
        .watch();
  }

  Future<List<CartItem>> getCartItems({String cartScope = ''}) {
    return (select(cartItems)
          ..where((t) => t.cartScope.equals(cartScope))
          ..orderBy([
            (t) => OrderingTerm.desc(t.updatedAt),
            (t) => OrderingTerm.asc(t.productName),
          ]))
        .get();
  }

  Future<Map<String, double>> cartTotalsByScope() async {
    final items = await select(cartItems).get();
    final map = <String, double>{};
    for (final item in items) {
      if (item.cartScope.isEmpty) continue;
      final line =
          item.unitPrice * item.quantity * (1 + item.gstPercent / 100);
      map[item.cartScope] = (map[item.cartScope] ?? 0) + line;
    }
    return {
      for (final e in map.entries)
        e.key: double.parse(e.value.toStringAsFixed(2)),
    };
  }

  /// Whether [scope] is a takeaway parcel id (`P1`, `P2`, …).
  static bool isTakeawayParcelScope(String scope) {
    final trimmed = scope.trim();
    if (trimmed.isEmpty) return false;
    return RegExp(r'^P\d+$', caseSensitive: false).hasMatch(trimmed);
  }

  /// Next parcel counter id for takeaway (`P1`, `P2`, …). Not a dine-in table.
  Future<String> nextTakeAwayParcelNumber() async {
    final items = await select(cartItems).get();
    var max = 0;
    for (final item in items) {
      if (!isTakeawayParcelScope(item.cartScope)) continue;
      final digits = item.cartScope.replaceAll(RegExp(r'[^0-9]'), '');
      if (digits.isEmpty) continue;
      final n = int.tryParse(digits);
      if (n != null && n > max) max = n;
    }
    return 'P${max + 1}';
  }

  Future<void> addProductToCart(
    Product product, {
    String cartScope = '',
    int? diningSessionId,
    ProductPortion? portion,
    double? shopGstPercentFallback,
    double? unitPriceOverride,
    int quantity = 1,
  }) async {
    final productGst = product.productCgst + product.productSgst;
    final gstPercent = productGst > 0
        ? productGst
        : (shopGstPercentFallback ?? 0);
    final portionId = portion?.portionId ?? 0;
    final qty = quantity < 1 ? 1 : quantity;
    final existing = await (select(cartItems)
          ..where(
            (t) =>
                t.productId.equals(product.productId) &
                t.cartScope.equals(cartScope) &
                t.portionId.equals(portionId),
          ))
        .getSingleOrNull();

    final unitPrice =
        unitPriceOverride ?? portion?.portionPrice ?? product.productPrice;
    final portionName = portion?.portionName.trim().isNotEmpty == true
        ? portion!.portionName.trim()
        : null;
    final displayName = portionName == null
        ? product.productName
        : '${product.productName} ($portionName)';

    if (existing == null) {
      await into(cartItems).insert(
        CartItemsCompanion.insert(
          productId: product.productId,
          cartScope: Value(cartScope),
          portionId: Value(portionId),
          productName: Value(displayName),
          categoryId: Value(product.categoryId),
          categoryName: Value(product.categoryName),
          productCode: Value(product.productCode),
          unitPrice: Value(unitPrice),
          productOldPrice: Value(product.productPrice),
          productNewPrice: Value(unitPrice),
          gstPercent: Value(gstPercent),
          productCgst: Value(product.productCgst),
          productSgst: Value(product.productSgst),
          quantity: Value(qty),
          productUnit: Value(product.productUnit),
          portionName: Value(portionName),
          snapshotProductName: Value(product.productName),
          snapshotLinePrice: Value(unitPrice),
          diningSessionId: Value(diningSessionId),
          lineType: const Value('product'),
        ),
      );
      return;
    }

    await (update(cartItems)
          ..where((t) => t.cartId.equals(existing.cartId)))
        .write(
      CartItemsCompanion(
        quantity: Value(existing.quantity + qty),
        unitPrice: Value(unitPrice),
        productNewPrice: Value(unitPrice),
        portionName: Value(portionName),
        snapshotProductName: Value(product.productName),
        snapshotLinePrice: Value(unitPrice),
        diningSessionId: Value(diningSessionId ?? existing.diningSessionId),
        lineType: const Value('product'),
        updatedAt: Value(DateTime.now()),
      ),
    );
  }

  /// Adds a combo as a cart line using negative [productId] (`-combo.comboId`).
  Future<void> addComboToCart(
    Combo combo, {
    String cartScope = '',
    int? diningSessionId,
  }) async {
    final cartProductId = -combo.comboId;
    final existing = await (select(cartItems)
          ..where(
            (t) =>
                t.productId.equals(cartProductId) &
                t.cartScope.equals(cartScope) &
                t.portionId.equals(0),
          ))
        .getSingleOrNull();

    final unitPrice = combo.comboWithGstPrice > 0
        ? combo.comboWithGstPrice
        : combo.comboPrice;
    final gstPercent = combo.comboCgst + combo.comboSgst;
    final components = await getComboItemsForCombo(combo.comboId);
    final snapshotJson = await buildComboSnapshotJson(components);

    if (existing == null) {
      final cartId = await into(cartItems).insert(
        CartItemsCompanion.insert(
          productId: cartProductId,
          cartScope: Value(cartScope),
          portionId: const Value(0),
          productName: Value(combo.comboName),
          productCode: Value(combo.comboCode),
          unitPrice: Value(unitPrice),
          productOldPrice: Value(combo.comboPrice),
          productNewPrice: Value(unitPrice),
          gstPercent: Value(gstPercent),
          productCgst: Value(combo.comboCgst),
          productSgst: Value(combo.comboSgst),
          quantity: const Value(1),
          productUnit: const Value('combo'),
          snapshotProductName: Value(combo.comboName),
          snapshotLinePrice: Value(unitPrice),
          snapshotComboComponents: Value(snapshotJson),
          diningSessionId: Value(diningSessionId),
          lineType: const Value('combo'),
          comboId: Value(combo.comboId),
          comboNetworkStatus: Value(combo.comboNetworkStatus),
        ),
      );
      await replaceCartComboItems(
        cartId: cartId,
        productId: cartProductId,
        cartScope: cartScope,
        parentPortionId: 0,
        comboId: combo.comboId,
        components: components,
      );
    } else {
      await (update(cartItems)
            ..where((t) => t.cartId.equals(existing.cartId)))
          .write(
        CartItemsCompanion(
          quantity: Value(existing.quantity + 1),
          snapshotComboComponents: Value(snapshotJson),
          diningSessionId: Value(diningSessionId ?? existing.diningSessionId),
          lineType: const Value('combo'),
          comboId: Value(combo.comboId),
          comboNetworkStatus: Value(combo.comboNetworkStatus),
          updatedAt: Value(DateTime.now()),
        ),
      );
      await replaceCartComboItems(
        cartId: existing.cartId,
        productId: cartProductId,
        cartScope: cartScope,
        parentPortionId: 0,
        comboId: combo.comboId,
        components: components,
      );
    }
  }

  Future<String> buildComboSnapshotJson(List<ComboItem> components) async {
    final rows = <Map<String, dynamic>>[];
    for (final c in components) {
      if (c.comboItemDeletedStatus == '1') continue;
      final product = c.productId == null
          ? null
          : await getProduct(c.productId!);
      String? portionName;
      if (c.portionId != null) {
        final portion = await (select(productPortions)
              ..where((t) => t.portionId.equals(c.portionId!))
              ..limit(1))
            .getSingleOrNull();
        portionName = portion?.portionName;
      }
      rows.add({
        'productId': c.productId,
        'productName': product?.productName ?? '',
        'portionId': c.portionId,
        'portionName': portionName ?? '',
        'quantity': c.comboItemQuantity,
      });
    }
    return jsonEncode(rows);
  }

  Future<void> replaceCartComboItems({
    required int cartId,
    required int productId,
    required String cartScope,
    required int parentPortionId,
    required int comboId,
    required List<ComboItem> components,
  }) async {
    await (delete(cartComboItems)..where((t) => t.cartId.equals(cartId))).go();
    await deleteCartComboItemsForLine(
      productId: productId,
      cartScope: cartScope,
      parentPortionId: parentPortionId,
    );
    var sort = 0;
    for (final c in components) {
      if (c.comboItemDeletedStatus == '1') continue;
      final product = c.productId == null
          ? null
          : await getProduct(c.productId!);
      String? portionName;
      if (c.portionId != null) {
        final portion = await (select(productPortions)
              ..where((t) => t.portionId.equals(c.portionId!))
              ..limit(1))
            .getSingleOrNull();
        portionName = portion?.portionName;
      }
      await into(cartComboItems).insert(
        CartComboItemsCompanion.insert(
          cartId: Value(cartId),
          productId: productId,
          cartScope: Value(cartScope),
          parentPortionId: Value(parentPortionId),
          comboId: Value(comboId),
          componentProductId: Value(c.productId),
          productNameSnapshot: Value(product?.productName),
          portionId: Value(c.portionId),
          portionNameSnapshot: Value(portionName),
          quantity: Value(c.comboItemQuantity),
          sortOrder: Value(sort++),
        ),
      );
    }
  }

  Future<void> deleteCartComboItemsForLine({
    required int productId,
    required String cartScope,
    required int parentPortionId,
  }) async {
    await (delete(cartComboItems)
          ..where(
            (t) =>
                t.productId.equals(productId) &
                t.cartScope.equals(cartScope) &
                t.parentPortionId.equals(parentPortionId),
          ))
        .go();
  }

  Future<List<CartComboItem>> getCartComboItemsForLine({
    required int productId,
    required String cartScope,
    int parentPortionId = 0,
    int? cartId,
  }) {
    if (cartId != null && cartId > 0) {
      return (select(cartComboItems)
            ..where((t) => t.cartId.equals(cartId))
            ..orderBy([(t) => OrderingTerm.asc(t.sortOrder)]))
          .get();
    }
    return (select(cartComboItems)
          ..where(
            (t) =>
                t.productId.equals(productId) &
                t.cartScope.equals(cartScope) &
                t.parentPortionId.equals(parentPortionId),
          )
          ..orderBy([(t) => OrderingTerm.asc(t.sortOrder)]))
        .get();
  }

  Future<void> changeCartQuantity(
    int productId,
    int quantity, {
    String cartScope = '',
    int portionId = 0,
    double? unitPrice,
  }) async {
    if (quantity <= 0) {
      await removeCartItem(
        productId,
        cartScope: cartScope,
        portionId: portionId,
      );
      return;
    }

    final existing = await (select(cartItems)
          ..where(
            (t) =>
                t.productId.equals(productId) &
                t.cartScope.equals(cartScope) &
                t.portionId.equals(portionId),
          ))
        .getSingleOrNull();
    if (existing == null) return;
    final printed = existing.printedQuantity > quantity
        ? quantity
        : existing.printedQuantity;

    await (update(cartItems)..where((t) => t.cartId.equals(existing.cartId)))
        .write(
      CartItemsCompanion(
        quantity: Value(quantity),
        printedQuantity: Value(printed),
        unitPrice: unitPrice == null ? const Value.absent() : Value(unitPrice),
        updatedAt: Value(DateTime.now()),
      ),
    );
  }

  Future<void> removeCartItem(
    int productId, {
    String cartScope = '',
    int portionId = 0,
  }) async {
    final existing = await (select(cartItems)
          ..where(
            (t) =>
                t.productId.equals(productId) &
                t.cartScope.equals(cartScope) &
                t.portionId.equals(portionId),
          ))
        .getSingleOrNull();
    if (existing != null) {
      await (delete(cartComboItems)
            ..where((t) => t.cartId.equals(existing.cartId)))
          .go();
    }
    await deleteCartComboItemsForLine(
      productId: productId,
      cartScope: cartScope,
      parentPortionId: portionId,
    );
    await (delete(cartItems)
          ..where(
            (t) =>
                t.productId.equals(productId) &
                t.cartScope.equals(cartScope) &
                t.portionId.equals(portionId),
          ))
        .go();
  }

  Future<void> clearCart({String cartScope = ''}) async {
    await (delete(cartComboItems)..where((t) => t.cartScope.equals(cartScope)))
        .go();
    await (delete(cartItems)..where((t) => t.cartScope.equals(cartScope))).go();
  }

  /// Moves an open dining session (and its cart) from [fromTable] to [toTable].
  Future<DiningSession> transferTable({
    required String fromTable,
    required String toTable,
  }) async {
    if (fromTable == toTable) {
      throw StateError('Cannot transfer to the same table');
    }
    return transaction(() async {
      final session = await getOpenSessionForTable(fromTable);
      if (session == null) {
        throw StateError('No open session on table $fromTable');
      }
      final targetBusy = await getOpenSessionForTable(toTable);
      if (targetBusy != null) {
        throw StateError('Target table already has an open bill');
      }

      final items = await getCartItems(cartScope: fromTable);
      for (final item in items) {
        await (update(cartItems)..where((t) => t.cartId.equals(item.cartId)))
            .write(
          CartItemsCompanion(
            cartScope: Value(toTable),
            noOfTable: Value(toTable),
            diningSessionId: Value(session.sessionId),
            updatedAt: Value(DateTime.now()),
          ),
        );
        await (update(cartComboItems)
              ..where((t) => t.cartId.equals(item.cartId)))
            .write(CartComboItemsCompanion(cartScope: Value(toTable)));
      }

      await (update(kots)..where((t) => t.tableNumber.equals(fromTable)))
          .write(KotsCompanion(tableNumber: Value(toTable)));

      await (update(diningSessions)
            ..where((t) => t.sessionId.equals(session.sessionId)))
          .write(
        DiningSessionsCompanion(
          primaryTableNumber: Value(toTable),
          sessionSyncStatus: const Value('0'),
          sessionVersion: Value(session.sessionVersion + 1),
        ),
      );
      return (await (select(diningSessions)
            ..where((t) => t.sessionId.equals(session.sessionId)))
          .getSingle());
    });
  }

  Future<void> setDiningSessionStatus({
    required int sessionId,
    required String status,
  }) async {
    await (update(diningSessions)..where((t) => t.sessionId.equals(sessionId)))
        .write(
      DiningSessionsCompanion(
        sessionStatus: Value(status),
        sessionSyncStatus: const Value('0'),
      ),
    );
  }

  /// Moves selected cart lines from [fromTable] to [toTable] (opens target session).
  Future<void> moveCartItemsToTable({
    required String fromTable,
    required String toTable,
    required List<CartItem> items,
  }) async {
    if (items.isEmpty) return;
    if (fromTable == toTable) {
      throw StateError('Cannot move to the same table');
    }
    await transaction(() async {
      final target = await openOrGetDiningSession(toTable);
      for (final item in items) {
        final existing = await (select(cartItems)
              ..where(
                (t) =>
                    t.productId.equals(item.productId) &
                    t.cartScope.equals(toTable) &
                    t.portionId.equals(item.portionId),
              ))
            .getSingleOrNull();
        if (existing == null) {
          await into(cartItems).insert(
            CartItemsCompanion.insert(
              productId: item.productId,
              cartScope: Value(toTable),
              portionId: Value(item.portionId),
              productName: Value(item.productName),
              categoryId: Value(item.categoryId),
              categoryName: Value(item.categoryName),
              productCode: Value(item.productCode),
              unitPrice: Value(item.unitPrice),
              gstPercent: Value(item.gstPercent),
              quantity: Value(item.quantity),
              printedQuantity: Value(item.printedQuantity),
              productUnit: Value(item.productUnit),
              diningSessionId: Value(target.sessionId),
              lineType: Value(item.lineType),
              comboId: Value(item.comboId),
              comboNetworkStatus: Value(item.comboNetworkStatus),
            ),
          );
        } else {
          await (update(cartItems)
                ..where(
                  (t) =>
                      t.productId.equals(item.productId) &
                      t.cartScope.equals(toTable) &
                      t.portionId.equals(item.portionId),
                ))
              .write(
            CartItemsCompanion(
              quantity: Value(existing.quantity + item.quantity),
              printedQuantity: Value(
                existing.printedQuantity + item.printedQuantity,
              ),
              diningSessionId: Value(target.sessionId),
              updatedAt: Value(DateTime.now()),
            ),
          );
        }
        await removeCartItem(
          item.productId,
          cartScope: fromTable,
          portionId: item.portionId,
        );
      }
    });
  }

  Future<int> upsertLocalMessPayment({
    required String memberId,
    required String memberName,
    required double messAmount,
    required double paidAmount,
    required String messTotalDays,
    required String paymentDate,
    required String paymentNetworkStatus,
  }) {
    return into(messMemberPayments).insert(
      MessMemberPaymentsCompanion.insert(
        memberId: memberId,
        memberName: Value(memberName),
        paymentMessAmount: Value(messAmount),
        paymentPaidAmount: Value(paidAmount),
        messTotalDays: Value(messTotalDays),
        paymentDate: paymentDate,
        paymentNetworkStatus: paymentNetworkStatus,
        paymentSyncStatus: const Value('0'),
      ),
    );
  }

  Future<void> updateLocalMessPayment({
    required int localPaymentId,
    required double messAmount,
    required double paidAmount,
    required String messTotalDays,
  }) {
    return (update(messMemberPayments)
          ..where((t) => t.localPaymentId.equals(localPaymentId)))
        .write(
      MessMemberPaymentsCompanion(
        paymentMessAmount: Value(messAmount),
        paymentPaidAmount: Value(paidAmount),
        messTotalDays: Value(messTotalDays),
        paymentSyncStatus: const Value('0'),
      ),
    );
  }

  Future<List<MessMemberPayment>> getPendingMessPayments({int limit = 50}) {
    return (select(messMemberPayments)
          ..where((t) => t.paymentSyncStatus.equals('0'))
          ..orderBy([(t) => OrderingTerm.asc(t.localPaymentId)])
          ..limit(limit))
        .get();
  }

  Future<List<MessMemberPayment>> getLocalMessPayments({String? memberId}) {
    final q = select(messMemberPayments)
      ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]);
    if (memberId != null) {
      q.where((t) => t.memberId.equals(memberId));
    }
    return q.get();
  }

  Stream<List<MessMemberPayment>> watchMessMemberPayments({String? memberId}) {
    final q = select(messMemberPayments)
      ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]);
    if (memberId != null) {
      q.where((t) => t.memberId.equals(memberId));
    }
    return q.watch();
  }

  Future<void> markMessPaymentSynced(int localPaymentId) async {
    await (update(messMemberPayments)
          ..where((t) => t.localPaymentId.equals(localPaymentId)))
        .write(
      const MessMemberPaymentsCompanion(paymentSyncStatus: Value('1')),
    );
  }

  Future<void> replaceMessInvoices(List<MessInvoicesCompanion> rows) async {
    await transaction(() async {
      await delete(messInvoices).go();
      await batch((b) => b.insertAll(messInvoices, rows));
    });
  }

  Future<void> replaceMessMemberPayments(
    List<MessMemberPaymentsCompanion> rows,
  ) async {
    await transaction(() async {
      await delete(messMemberPayments).go();
      if (rows.isNotEmpty) {
        await batch((b) => b.insertAll(messMemberPayments, rows));
      }
    });
  }

  Future<void> replaceMessTokens(List<MessTokensCompanion> rows) async {
    await transaction(() async {
      await delete(messTokens).go();
      if (rows.isNotEmpty) {
        await batch((b) => b.insertAll(messTokens, rows));
      }
    });
  }

  Future<int> countMessCouponsForMember(String memberName) async {
    final count = countAll();
    final row = await (selectOnly(messInvoices)
          ..addColumns([count])
          ..where(messInvoices.memberName.equals(memberName)))
        .getSingle();
    return row.read(count) ?? 0;
  }

  Future<List<MessInvoice>> getPendingMessInvoices({int limit = 100}) {
    return (select(messInvoices)
          ..where((t) => t.messInvoiceStatus.equals('0'))
          ..orderBy([(t) => OrderingTerm.asc(t.invoiceId)])
          ..limit(limit))
        .get();
  }

  Future<int> issueMessCoupon({
    required String memberId,
    required String memberName,
    required String messType,
  }) async {
    return into(messInvoices).insert(
      MessInvoicesCompanion.insert(
        memberId: Value(memberId),
        memberName: Value(memberName),
        messType: Value(messType),
        messInvoiceDate: DateTime.now(),
        messInvoiceNetworkStatus: appDatabaseNetworkStatus(prefix: 'mi_'),
        messInvoiceStatus: const Value('0'),
      ),
    );
  }

  Future<void> markMessInvoiceSynced(int invoiceId) async {
    await (update(messInvoices)..where((t) => t.invoiceId.equals(invoiceId)))
        .write(
      const MessInvoicesCompanion(messInvoiceStatus: Value('1')),
    );
  }

  Future<MessInvoice?> getMessInvoiceById(int invoiceId) {
    return (select(messInvoices)..where((t) => t.invoiceId.equals(invoiceId)))
        .getSingleOrNull();
  }

  Stream<List<MessInvoice>> watchMessInvoices({String? memberName}) {
    final q = select(messInvoices)
      ..orderBy([(t) => OrderingTerm.desc(t.messInvoiceDate)]);
    if (memberName != null && memberName.trim().isNotEmpty) {
      q.where((t) => t.memberName.equals(memberName));
    }
    return q.watch();
  }

  Future<int> countTotalInvoices() async {
    final count = countAll();
    final row = await (selectOnly(invoices)
          ..addColumns([count])
          ..where(branchMatches(invoices.branchId)))
        .getSingle();
    return row.read(count) ?? 0;
  }

  Future<bool> hasMessPaymentForMonth({
    required String memberId,
    required String paymentDate,
  }) async {
    final row = await (select(messMemberPayments)
          ..where(
            (t) =>
                t.memberId.equals(memberId) & t.paymentDate.equals(paymentDate),
          )
          ..limit(1))
        .getSingleOrNull();
    return row != null;
  }

  Stream<List<Invoice>> watchRecentInvoices({int limit = 50}) {
    return (select(invoices)
          ..where((t) => branchMatches(t.branchId))
          ..orderBy([(t) => OrderingTerm.desc(t.createdAt)])
          ..limit(limit))
        .watch();
  }

  Stream<List<Invoice>> watchTodayInvoices() {
    final now = DateTime.now();
    final start = DateTime(now.year, now.month, now.day);
    final end = start.add(const Duration(days: 1));
    return watchInvoicesInRange(start, end);
  }

  /// All non-refunded / non-cancelled bills (Android home "Total Sales").
  Stream<List<Invoice>> watchAllBillableInvoices() {
    return (select(invoices)
          ..where(
            (t) => isBillableInvoice(t) & branchMatches(t.branchId),
          )
          ..orderBy([(t) => OrderingTerm.desc(t.invoiceDate)]))
        .watch();
  }

  Stream<List<Invoice>> watchInvoicesInRange(DateTime start, DateTime end) {
    return (select(invoices)
          ..where(
            (t) =>
                t.invoiceDate.isBiggerOrEqualValue(start) &
                t.invoiceDate.isSmallerThanValue(end) &
                isBillableInvoice(t) &
                branchMatches(t.branchId),
          )
          ..orderBy([(t) => OrderingTerm.desc(t.invoiceDate)]))
        .watch();
  }

  Future<Invoice?> getInvoiceById(int invoiceId) {
    return (select(invoices)..where((t) => t.invoiceId.equals(invoiceId)))
        .getSingleOrNull();
  }

  Future<Invoice?> getInvoiceByNumber(String invoiceNumber) {
    return (select(invoices)
          ..where((t) => t.invoiceNumber.equals(invoiceNumber)))
        .getSingleOrNull();
  }

  Future<Invoice?> getInvoiceByNetworkStatus(String networkStatus) {
    return (select(invoices)
          ..where((t) => t.invoiceNetworkStatus.equals(networkStatus))
          ..limit(1))
        .getSingleOrNull();
  }

  Future<List<InvoiceItem>> getInvoiceItems(String invoiceNumber) {
    return (select(invoiceItems)
          ..where((t) => t.invoiceNumber.equals(invoiceNumber)))
        .get();
  }

  /// Upserts cloud invoices as already-synced so they are not re-uploaded.
  Future<({int inserted, int updated, int skipped, int comboItems})>
      upsertCloudInvoices({
    required List<InvoicesCompanion> headers,
    required Map<String, List<InvoiceItemsCompanion>> itemsByNumber,
    List<InvoiceComboItemsCompanion> comboItems = const [],
  }) async {
    var inserted = 0;
    var updated = 0;
    var skipped = 0;
    var comboInserted = 0;

    await transaction(() async {
      final touchedNumbers = <String>{};

      for (final header in headers) {
        final network = header.invoiceNetworkStatus.present
            ? header.invoiceNetworkStatus.value
            : '';
        final number =
            header.invoiceNumber.present ? header.invoiceNumber.value : '';
        if (network.isEmpty || number.isEmpty) {
          skipped++;
          continue;
        }

        // Never overwrite a local pending bill with the same network key mid-edit.
        final existingByNetwork = await getInvoiceByNetworkStatus(network);
        if (existingByNetwork != null &&
            existingByNetwork.invoiceSyncStatus == '0') {
          skipped++;
          continue;
        }

        final existing = existingByNetwork ?? await getInvoiceByNumber(number);
        if (existing == null) {
          await (delete(invoiceItems)
                ..where((t) => t.invoiceNumber.equals(number)))
              .go();
          await (delete(invoiceComboItems)
                ..where((t) => t.invoiceNumber.equals(number)))
              .go();
          await into(invoices).insert(stampInvoice(header));
          inserted++;
        } else {
          await (update(invoices)
                ..where((t) => t.invoiceId.equals(existing.invoiceId)))
              .write(
            stampInvoice(
              header.copyWith(
                // Keep local auto-id; force synced.
                invoiceSyncStatus: const Value('1'),
              ),
            ),
          );
          await (delete(invoiceItems)
                ..where((t) => t.invoiceNumber.equals(existing.invoiceNumber)))
              .go();
          await (delete(invoiceComboItems)
                ..where((t) => t.invoiceNumber.equals(existing.invoiceNumber)))
              .go();
          // If cloud renamed number (rare), also clear old number lines already done.
          if (existing.invoiceNumber != number) {
            await (delete(invoiceItems)
                  ..where((t) => t.invoiceNumber.equals(number)))
                .go();
            await (delete(invoiceComboItems)
                  ..where((t) => t.invoiceNumber.equals(number)))
                .go();
          }
          updated++;
        }

        touchedNumbers.add(number);
        final lines = itemsByNumber[number] ?? const <InvoiceItemsCompanion>[];
        if (lines.isNotEmpty) {
          await batch(
            (b) => b.insertAll(
              invoiceItems,
              lines.map(stampInvoiceItem).toList(),
            ),
          );
        }
      }

      if (comboItems.isNotEmpty && touchedNumbers.isNotEmpty) {
        final toInsert = <InvoiceComboItemsCompanion>[];
        for (final row in comboItems) {
          final number =
              row.invoiceNumber.present ? row.invoiceNumber.value : null;
          if (number == null || number.isEmpty) continue;
          if (!touchedNumbers.contains(number)) continue;
          toInsert.add(row);
        }
        if (toInsert.isNotEmpty) {
          await batch((b) => b.insertAll(invoiceComboItems, toInsert));
          comboInserted = toInsert.length;
        }
      }
    });

    return (
      inserted: inserted,
      updated: updated,
      skipped: skipped,
      comboItems: comboInserted,
    );
  }

  Future<String> nextInvoiceNumber({String prefix = 'PB'}) async {
    final now = DateTime.now();
    final dayKey = DateFormat('dd-MM').format(now);
    final start = DateTime(now.year, now.month, now.day);
    final end = start.add(const Duration(days: 1));

    final countExp = invoices.invoiceId.count();
    final query = selectOnly(invoices)
      ..addColumns([countExp])
      ..where(
        invoices.invoiceDate.isBiggerOrEqualValue(start) &
            invoices.invoiceDate.isSmallerThanValue(end) &
            branchMatches(invoices.branchId),
      );
    final row = await query.getSingle();
    final todayCount = row.read(countExp) ?? 0;
    final seq = (todayCount + 1).toString().padLeft(5, '0');
    return '$prefix/$dayKey/$seq';
  }

  String appDatabaseNetworkStatus({String prefix = ''}) {
    const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
    final rand = Random();
    final body = List.generate(10, (_) => chars[rand.nextInt(chars.length)])
        .join();
    return prefix.isEmpty ? body : '$prefix$body';
  }

  Future<List<Invoice>> getPendingSyncInvoices({int limit = 50}) {
    return (select(invoices)
          ..where(
            (t) =>
                t.invoiceSyncStatus.equals('0') & branchMatches(t.branchId),
          )
          ..orderBy([(t) => OrderingTerm.asc(t.createdAt)])
          ..limit(limit))
        .get();
  }

  Stream<List<Invoice>> watchPendingSyncInvoices() {
    return (select(invoices)
          ..where(
            (t) =>
                t.invoiceSyncStatus.equals('0') & branchMatches(t.branchId),
          )
          ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]))
        .watch();
  }

  Future<int> countPendingSyncInvoices() async {
    final countExp = invoices.invoiceId.count();
    final query = selectOnly(invoices)
      ..addColumns([countExp])
      ..where(
        invoices.invoiceSyncStatus.equals('0') &
            branchMatches(invoices.branchId),
      );
    final row = await query.getSingle();
    return row.read(countExp) ?? 0;
  }

  Future<SyncPendingSnapshot> getSyncPendingSnapshot() async {
    Future<int> len(Future<List<dynamic>> f) async => (await f).length;
    return SyncPendingSnapshot(
      invoices: await countPendingSyncInvoices(),
      categories: await len(getPendingCategories()),
      products: await len(getPendingProducts()),
      portions: await len(getPendingPortions()),
      combos: await len(getPendingCombos()),
      subcategories: await len(getPendingSubcategories()),
      messMembers: await len(getPendingMessMembers()),
      messTokens: await len(getPendingMessTokens()),
      messPayments: await len(getPendingMessPayments()),
      messInvoices: await len(getPendingMessInvoices()),
      diningSessions: await len(getPendingDiningSessions()),
      inventory: await len(getPendingInventory()),
      expenses: await len(getPendingExpenses()),
    );
  }

  /// Destructive reset used by Android "Fetch Data" (wipe local then re-download).
  /// Keeps auth session; clears operational / catalog tables.
  Future<void> resetOperationalDataForFetch() async {
    await transaction(() async {
      await delete(kotItems).go();
      await delete(kots).go();
      await delete(orderRounds).go();
      await delete(diningSessions).go();
      await delete(cartComboItems).go();
      await delete(cartItems).go();
      await delete(invoiceComboItems).go();
      await delete(invoiceItems).go();
      await delete(invoices).go();
      await delete(invoiceProductDeleteQueue).go();
      await delete(comboItems).go();
      await delete(combos).go();
      await delete(productPortions).go();
      await delete(products).go();
      await delete(productSubcategories).go();
      await delete(productCategories).go();
      await delete(foodTypes).go();
      await delete(posTables).go();
      await delete(diningAreas).go();
      await delete(tableTypes).go();
      await delete(portionMasters).go();
      await delete(messMealTokenQueue).go();
      await delete(messTokens).go();
      await delete(messMemberPayments).go();
      await delete(messInvoices).go();
      await delete(messMembers).go();
      await delete(inventoryMovements).go();
      await delete(shopExpenses).go();
      await delete(companyPrinterSettings).go();
      await delete(companies).go();
    });
  }

  Future<List<DailySalesPoint>> getDailySalesLastDays(int days) async {
    final now = DateTime.now();
    final startDay = DateTime(now.year, now.month, now.day)
        .subtract(Duration(days: days - 1));
    final end = startDay.add(Duration(days: days));
    final rows = await (select(invoices)
          ..where(
            (t) =>
                t.invoiceDate.isBiggerOrEqualValue(startDay) &
                t.invoiceDate.isSmallerThanValue(end) &
                isBillableInvoice(t) &
                branchMatches(t.branchId),
          ))
        .get();
    final byDay = <String, double>{};
    for (var i = 0; i < days; i++) {
      final d = startDay.add(Duration(days: i));
      final key =
          '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
      byDay[key] = 0;
    }
    for (final inv in rows) {
      final d = inv.invoiceDate;
      final key =
          '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
      byDay[key] = (byDay[key] ?? 0) + inv.totalAmount;
    }
    return [
      for (var i = 0; i < days; i++)
        DailySalesPoint(
          date: startDay.add(Duration(days: i)),
          total: double.parse(
            (byDay[
                        '${startDay.add(Duration(days: i)).year}-${startDay.add(Duration(days: i)).month.toString().padLeft(2, '0')}-${startDay.add(Duration(days: i)).day.toString().padLeft(2, '0')}'] ??
                    0)
                .toStringAsFixed(2),
          ),
        ),
    ];
  }

  Future<void> updateCategorySortOrder(int categoryId, int sortOrder) async {
    await (update(productCategories)
          ..where((t) => t.categoryId.equals(categoryId)))
        .write(
      ProductCategoriesCompanion(
        categorySortOrder: Value(sortOrder),
        categorySyncStatus: const Value('0'),
      ),
    );
  }

  Future<void> updateSubcategorySortOrder(
    int subcategoryId,
    int sortOrder,
  ) async {
    await (update(productSubcategories)
          ..where((t) => t.subcategoryId.equals(subcategoryId)))
        .write(
      ProductSubcategoriesCompanion(
        subcategorySortOrder: Value(sortOrder),
        subcategorySyncStatus: const Value('0'),
      ),
    );
  }

  /// Alias used by reports hub clear-all flow.
  Future<int> countPendingInvoiceSync() => countPendingSyncInvoices();

  /// Deletes all invoice items then invoices. Blocked while any bill is
  /// pending cloud sync (`invoiceSyncStatus == '0'`).
  Future<void> clearAllInvoices() async {
    final pending = await countPendingInvoiceSync();
    if (pending > 0) {
      throw StateError(
        'Cannot clear invoices while $pending bill(s) are pending sync',
      );
    }
    await transaction(() async {
      final headers = await (select(invoices)
            ..where((t) => branchMatches(t.branchId)))
          .get();
      for (final inv in headers) {
        await (delete(invoiceItems)
              ..where((t) => t.invoiceNumber.equals(inv.invoiceNumber)))
            .go();
      }
      await (delete(invoices)..where((t) => branchMatches(t.branchId))).go();
    });
  }

  Future<List<InvoiceItem>> getPendingUnsyncedInvoiceItems({int limit = 500}) {
    return (select(invoiceItems)
          ..where((t) => t.invoiceItemSyncStatus.equals('0'))
          ..orderBy([(t) => OrderingTerm.asc(t.invoiceItemId)])
          ..limit(limit))
        .get();
  }

  Future<List<InvoiceComboItem>> getAllPendingInvoiceComboItems({
    int limit = 500,
  }) {
    return (select(invoiceComboItems)
          ..where((t) => t.invoiceComboItemStatus.equals('0'))
          ..orderBy([(t) => OrderingTerm.asc(t.invoiceComboItemId)])
          ..limit(limit))
        .get();
  }

  Future<void> markInvoiceItemSynced(int invoiceItemId) async {
    await (update(invoiceItems)
          ..where((t) => t.invoiceItemId.equals(invoiceItemId)))
        .write(
      const InvoiceItemsCompanion(invoiceItemSyncStatus: Value('1')),
    );
  }

  Future<void> markInvoiceSynced(int invoiceId) async {
    await transaction(() async {
      final invoice = await getInvoiceById(invoiceId);
      if (invoice == null) return;
      await (update(invoices)..where((t) => t.invoiceId.equals(invoiceId)))
          .write(const InvoicesCompanion(invoiceSyncStatus: Value('1')));
      await (update(invoiceItems)
            ..where((t) => t.invoiceNumber.equals(invoice.invoiceNumber)))
          .write(
        const InvoiceItemsCompanion(invoiceItemSyncStatus: Value('1')),
      );
      await (update(invoiceComboItems)
            ..where((t) => t.invoiceNumber.equals(invoice.invoiceNumber)))
          .write(
        const InvoiceComboItemsCompanion(invoiceComboItemStatus: Value('1')),
      );
    });
  }

  Future<List<InvoiceComboItem>> getPendingInvoiceComboItems(
    String invoiceNumber, {
    int limit = 200,
  }) {
    return (select(invoiceComboItems)
          ..where(
            (t) =>
                t.invoiceNumber.equals(invoiceNumber) &
                t.invoiceComboItemStatus.equals('0'),
          )
          ..orderBy([(t) => OrderingTerm.asc(t.sortOrder)])
          ..limit(limit))
        .get();
  }

  Future<void> markInvoiceComboItemSynced(int invoiceComboItemId) async {
    await (update(invoiceComboItems)
          ..where((t) => t.invoiceComboItemId.equals(invoiceComboItemId)))
        .write(
      const InvoiceComboItemsCompanion(invoiceComboItemStatus: Value('1')),
    );
  }

  /// Saves scoped cart as a completed invoice and clears that cart scope.
  Future<SavedInvoiceResult> saveInvoiceFromCart({
    required PaymentTender tender,
    String invoiceType = 'fast_billing',
    String invoicePrefix = 'PB',
    String? customerName,
    String? customerMobile,
    String? customerEmail,
    String? customerAddress,
    String cartScope = '',
    String? tableNumber,
    int? diningSessionId,
    double discount = 0,
    String discountType = 'Amount',
    double packingCharge = 0,
    String packingChargeType = 'Amount',
    bool updateInventory = true,
  }) async {
    // WithTable BluetoothPrint always deducts when stock exists; the printer
    // productQuantityUpdate switch is stored but not applied at save.
    final items = await getCartItems(cartScope: cartScope);
    if (items.isEmpty) {
      throw StateError('Cart is empty');
    }

    var subtotal = 0.0;
    var taxTotal = 0.0;
    var qtyTotal = 0;
    for (final item in items) {
      final lineBase = item.unitPrice * item.quantity;
      subtotal += lineBase;
      taxTotal += lineBase * item.gstPercent / 100;
      qtyTotal += item.quantity;
    }
    subtotal = double.parse(subtotal.toStringAsFixed(2));
    taxTotal = double.parse(taxTotal.toStringAsFixed(2));
    final discountValue = discountType.toLowerCase().startsWith('p')
        ? double.parse((subtotal * discount / 100).toStringAsFixed(2))
        : double.parse(discount.toStringAsFixed(2));
    final packingValue = packingChargeType.toLowerCase().startsWith('p')
        ? double.parse((subtotal * packingCharge / 100).toStringAsFixed(2))
        : double.parse(packingCharge.toStringAsFixed(2));
    final totalAmount = double.parse(
      (subtotal + taxTotal + packingValue - discountValue)
          .clamp(0, double.infinity)
          .toStringAsFixed(2),
    );

    if (!tender.isValidFor(totalAmount)) {
      throw StateError('Cash + UPI amounts must equal bill total');
    }

    return transaction(() async {
      final invoiceNumber = await nextInvoiceNumber(prefix: invoicePrefix);
      final now = DateTime.now();

      final invoiceId = await into(invoices).insert(
        stampInvoice(
          InvoicesCompanion.insert(
            invoiceNumber: invoiceNumber,
            invoiceDate: now,
            invoiceType: Value(invoiceType),
            subTotal: Value(subtotal),
            totalGstAmount: Value(taxTotal),
            discount: Value(discount),
            discountType: Value(discountType),
            packingCharge: Value(packingCharge),
            packingChargeType: Value(packingChargeType),
            totalAmount: Value(totalAmount),
            paymentMode: Value(tender.mode.label),
            cashAmount: Value(tender.cashAmount),
            upiAmount: Value(tender.upiAmount),
            invoiceNetworkStatus: appDatabaseNetworkStatus(),
            invoiceSyncStatus: const Value('0'),
            noOfTable: Value(tableNumber ?? ''),
            customerName: Value(customerName),
            customerMobile: Value(customerMobile),
            customerEmail: Value(customerEmail),
            customerAddress: Value(customerAddress),
            diningSessionId: Value(diningSessionId),
            billPrintStatus: const Value(''),
            itemCount: Value(qtyTotal),
          ),
        ),
      );

      for (final item in items) {
        final lineNetwork = appDatabaseNetworkStatus();
        final cgst = item.productCgst > 0 || item.productSgst > 0
            ? item.productCgst
            : item.gstPercent / 2;
        final sgst = item.productCgst > 0 || item.productSgst > 0
            ? item.productSgst
            : item.gstPercent / 2;
        await into(invoiceItems).insert(
          stampInvoiceItem(
            InvoiceItemsCompanion.insert(
              invoiceNumber: invoiceNumber,
              productId: Value(item.productId),
              productName: Value(item.productName),
              productCode: Value(item.productCode),
              productPrice: Value(item.unitPrice),
              productQuantity: Value(item.quantity),
              productCgst: Value(cgst),
              productSgst: Value(sgst),
              productUnit: Value(item.productUnit),
              categoryName: Value(item.categoryName),
              portionId: Value(item.portionId == 0 ? null : item.portionId),
              portionName: Value(item.portionName),
              snapshotProductName: Value(
                item.snapshotProductName ?? item.productName,
              ),
              snapshotLinePrice: Value(
                item.snapshotLinePrice ?? item.unitPrice,
              ),
              snapshotComboComponents: Value(item.snapshotComboComponents),
              comboId: Value(item.comboId),
              invoiceItemType: Value(
                item.lineType.trim().isEmpty ? 'product' : item.lineType,
              ),
              productStatus: const Value('completed'),
              invoiceItemNetworkStatus: Value(lineNetwork),
              invoiceItemSyncStatus: const Value('0'),
            ),
          ),
        );

        if (item.lineType == 'combo' || item.comboId != null) {
          final comboLines = await getCartComboItemsForLine(
            productId: item.productId,
            cartScope: cartScope,
            parentPortionId: item.portionId,
            cartId: item.cartId,
          );
          var sort = 0;
          for (final c in comboLines) {
            await into(invoiceComboItems).insert(
              InvoiceComboItemsCompanion.insert(
                invoiceNumber: Value(invoiceNumber),
                invoiceProductNetworkStatus: Value(lineNetwork),
                comboId: Value(c.comboId ?? item.comboId),
                comboNetworkStatus: Value(item.comboNetworkStatus),
                productId: Value(c.componentProductId),
                productNameSnapshot: Value(c.productNameSnapshot),
                portionId: Value(c.portionId),
                portionNameSnapshot: Value(c.portionNameSnapshot),
                quantity: Value(c.quantity * item.quantity),
                sortOrder: Value(sort++),
                invoiceComboItemNetworkStatus: Value(
                  appDatabaseNetworkStatus(prefix: 'ici_'),
                ),
                invoiceComboItemStatus: const Value('0'),
              ),
            );
          }
        }
      }

      // WithTable `BluetoothPrint.saveInvoice`: deduct only if that product
      // already has an inventory row. Combos deduct component products.
      // productQuantityUpdate is accepted for API parity; Android does not
      // gate save on that switch.
      final deductLikeAndroid = updateInventory || true;
      if (deductLikeAndroid) {
      for (final item in items) {
        final comboLine = item.lineType == 'combo' || item.comboId != null;
        if (comboLine) {
          final comboLines = await getCartComboItemsForLine(
            productId: item.productId,
            cartScope: cartScope,
            parentPortionId: item.portionId,
            cartId: item.cartId,
          );
          final comboQty = item.quantity < 1 ? 1 : item.quantity;
          for (final c in comboLines) {
            final pid = c.componentProductId;
            if (pid == null || pid <= 0) continue;
            final componentQty = c.quantity < 1 ? 1 : c.quantity;
            await deductInventoryForSaleIfTracked(
              productId: pid,
              productName: c.productNameSnapshot ?? item.productName,
              quantity: (comboQty * componentQty).toDouble(),
              at: now,
            );
          }
        } else if (item.productId > 0) {
          await deductInventoryForSaleIfTracked(
            productId: item.productId,
            productName: item.productName,
            quantity: item.quantity.toDouble(),
            at: now,
          );
        }
      }
      }

      await clearCart(cartScope: cartScope);
      if (diningSessionId != null) {
        await updateDiningSessionMeta(
          diningSessionId,
          clearUnpaidInvoiceNumber: true,
        );
        await settleDiningSession(diningSessionId);
      }

      return SavedInvoiceResult(
        invoiceId: invoiceId,
        invoiceNumber: invoiceNumber,
        totalAmount: totalAmount,
        paymentMode: tender.mode.label,
      );
    });
  }

  Future<InventoryMovement?> getLatestInventory(int productId) {
    return (select(inventoryMovements)
          ..where(
            (t) =>
                t.productId.equals(productId) & branchMatches(t.branchId),
          )
          ..orderBy([(t) => OrderingTerm.desc(t.inventoryId)])
          ..limit(1))
        .getSingleOrNull();
  }

  Future<double> getCurrentStock(int productId) async {
    final latest = await getLatestInventory(productId);
    return latest?.afterSaleInventoryQuantity ?? 0;
  }

  Stream<List<InventoryMovement>> watchInventoryMovements({int limit = 200}) {
    return (select(inventoryMovements)
          ..where((t) => branchMatches(t.branchId))
          ..orderBy([(t) => OrderingTerm.desc(t.inventoryId)])
          ..limit(limit))
        .watch();
  }

  Future<List<InventoryMovement>> getPendingInventory({int limit = 100}) {
    return (select(inventoryMovements)
          ..where(
            (t) =>
                t.inventorySyncStatus.equals('0') &
                branchMatches(t.branchId),
          )
          ..orderBy([(t) => OrderingTerm.asc(t.inventoryId)])
          ..limit(limit))
        .get();
  }

  Future<void> markInventorySynced(int inventoryId) async {
    await (update(inventoryMovements)
          ..where((t) => t.inventoryId.equals(inventoryId)))
        .write(const InventoryMovementsCompanion(
      inventorySyncStatus: Value('1'),
    ));
  }

  /// Stock-in: remaining = previous remaining + qty (fixes Android restock=0 bug).
  Future<InventoryMovement> addStockIn({
    required int productId,
    required String productName,
    required double quantity,
    DateTime? at,
  }) async {
    if (quantity <= 0) throw StateError('Quantity must be positive');
    final when = at ?? DateTime.now();
    final previous = await getCurrentStock(productId);
    final remaining = previous + quantity;
    final id = await into(inventoryMovements).insert(
      stampInventory(
        InventoryMovementsCompanion.insert(
          productId: productId,
          productName: Value(productName),
          productInventoryQuantity: Value(quantity),
          afterSaleInventoryQuantity: Value(remaining),
          saleInventoryQuantity: const Value(0),
          inventoryDate: when,
          inventoryNetworkStatus: appDatabaseNetworkStatus(),
          inventorySyncStatus: const Value('0'),
        ),
      ),
    );
    return (select(inventoryMovements)
          ..where((t) => t.inventoryId.equals(id)))
        .getSingle();
  }

  /// WithTable: skip products that were never stocked (`getInventoryDetails` empty).
  Future<InventoryMovement?> deductInventoryForSaleIfTracked({
    required int productId,
    required String productName,
    required double quantity,
    DateTime? at,
  }) async {
    final latest = await getLatestInventory(productId);
    if (latest == null) return null;
    return deductInventoryForSale(
      productId: productId,
      productName: productName,
      quantity: quantity,
      at: at,
    );
  }

  /// Sale deduct when a stock row exists. Remaining may go negative.
  Future<InventoryMovement?> deductInventoryForSale({
    required int productId,
    required String productName,
    required double quantity,
    DateTime? at,
  }) async {
    if (quantity <= 0) return null;
    final when = at ?? DateTime.now();
    final previous = await getCurrentStock(productId);
    final remaining = previous - quantity;
    final id = await into(inventoryMovements).insert(
      stampInventory(
        InventoryMovementsCompanion.insert(
          productId: productId,
          productName: Value(productName),
          productInventoryQuantity: const Value(0),
          afterSaleInventoryQuantity: Value(remaining),
          saleInventoryQuantity: Value(quantity),
          inventoryDate: when,
          inventoryNetworkStatus: appDatabaseNetworkStatus(),
          inventorySyncStatus: const Value('0'),
        ),
      ),
    );
    return (select(inventoryMovements)
          ..where((t) => t.inventoryId.equals(id)))
        .getSingle();
  }

  Future<InventoryMovement?> getInventoryByNetworkStatus(String network) {
    return (select(inventoryMovements)
          ..where((t) => t.inventoryNetworkStatus.equals(network))
          ..limit(1))
        .getSingleOrNull();
  }

  Future<int> upsertCloudInventory(List<InventoryMovementsCompanion> rows) async {
    var count = 0;
    await transaction(() async {
      for (final row in rows) {
        final network = row.inventoryNetworkStatus.present
            ? row.inventoryNetworkStatus.value
            : '';
        if (network.isEmpty) continue;
        final existing = await getInventoryByNetworkStatus(network);
        if (existing != null) continue;
        await into(inventoryMovements).insert(
          stampInventory(
            row.copyWith(inventorySyncStatus: const Value('1')),
          ),
        );
        count++;
      }
    });
    return count;
  }

  Stream<List<ShopExpense>> watchExpenses({int limit = 200}) {
    return (select(shopExpenses)
          ..where((t) => branchMatches(t.branchId))
          ..orderBy([(t) => OrderingTerm.desc(t.expensesDate)])
          ..limit(limit))
        .watch();
  }

  Stream<List<ShopExpense>> watchExpensesInRange(DateTime start, DateTime end) {
    return (select(shopExpenses)
          ..where(
            (t) =>
                t.expensesDate.isBiggerOrEqualValue(start) &
                t.expensesDate.isSmallerThanValue(end) &
                branchMatches(t.branchId),
          )
          ..orderBy([(t) => OrderingTerm.desc(t.expensesDate)]))
        .watch();
  }

  Future<List<ShopExpense>> getPendingExpenses({int limit = 100}) {
    return (select(shopExpenses)
          ..where(
            (t) =>
                t.expensesSyncStatus.equals('0') & branchMatches(t.branchId),
          )
          ..orderBy([(t) => OrderingTerm.asc(t.expensesId)])
          ..limit(limit))
        .get();
  }

  Future<void> markExpenseSynced(int expensesId) async {
    await (update(shopExpenses)..where((t) => t.expensesId.equals(expensesId)))
        .write(const ShopExpensesCompanion(expensesSyncStatus: Value('1')));
  }

  Future<ShopExpense> addExpense({
    required String name,
    required double amount,
    DateTime? at,
  }) async {
    final trimmed = name.trim();
    if (trimmed.isEmpty) throw StateError('Expense name is required');
    if (amount <= 0) throw StateError('Amount must be positive');
    final id = await into(shopExpenses).insert(
      stampExpense(
        ShopExpensesCompanion.insert(
          expensesName: Value(trimmed),
          expensesAmount: Value(amount),
          expensesDate: at ?? DateTime.now(),
          expensesNetworkStatus: appDatabaseNetworkStatus(),
          expensesSyncStatus: const Value('0'),
        ),
      ),
    );
    return (select(shopExpenses)..where((t) => t.expensesId.equals(id)))
        .getSingle();
  }

  Future<ShopExpense?> getExpenseByNetworkStatus(String network) {
    return (select(shopExpenses)
          ..where((t) => t.expensesNetworkStatus.equals(network))
          ..limit(1))
        .getSingleOrNull();
  }

  Future<int> upsertCloudExpenses(List<ShopExpensesCompanion> rows) async {
    var count = 0;
    await transaction(() async {
      for (final row in rows) {
        final network = row.expensesNetworkStatus.present
            ? row.expensesNetworkStatus.value
            : '';
        if (network.isEmpty) continue;
        final existing = await getExpenseByNetworkStatus(network);
        if (existing != null) continue;
        await into(shopExpenses).insert(
          stampExpense(
            row.copyWith(expensesSyncStatus: const Value('1')),
          ),
        );
        count++;
      }
    });
    return count;
  }

  // ── Company / printer (Android company + company_printer_setting) ──

  Future<Company?> getLocalCompany() {
    return (select(companies)
          ..orderBy([(t) => OrderingTerm.desc(t.companyId)])
          ..limit(1))
        .getSingleOrNull();
  }

  Future<void> upsertLocalCompany(CompanyDto dto) async {
    await transaction(() async {
      await delete(companies).go();
      await into(companies).insert(
        CompaniesCompanion.insert(
          companyId: dto.companyId == null
              ? const Value.absent()
              : Value(dto.companyId!),
          companyName: Value(dto.companyName),
          cashierName: Value(dto.cashierName),
          companyMobile: Value(dto.companyMobile),
          companyAddress: Value(dto.companyAddress),
          shopName1: Value(dto.shopName1),
          shopName2: Value(dto.shopName2),
          addressLine1: Value(dto.addressLine1),
          addressLine2: Value(dto.addressLine2),
          addressLine3: Value(dto.addressLine3),
          phoneNo1: Value(dto.phoneNo1),
          phoneNo2: Value(dto.phoneNo2),
          currencyName: Value(dto.currencyName),
          countryName: Value(dto.countryName),
          stateName: Value(dto.stateName),
          tableStatus: Value(dto.tableStatus),
          noOfTable: Value(dto.noOfTable),
          gstStatus: Value(dto.gstStatus),
          gstNumber: Value(dto.gstNumber),
          shopCgst: Value(dto.shopCgst),
          shopSgst: Value(dto.shopSgst),
          panNumber: Value(dto.panNumber),
          companyFssis: Value(dto.companyFssis),
          companyLogo: Value(dto.companyLogo),
          paymentLogo: Value(dto.paymentLogo),
          openingMinutes: Value(dto.openingMinutes),
          closingMinutes: Value(dto.closingMinutes),
          companyStatus: Value(dto.companyStatus),
        ),
      );
    });
  }

  Future<void> upsertLocalCompanyFromProfile(ShopReceiptProfile profile) async {
    await upsertLocalCompany(
      CompanyDto(
        companyName: profile.companyName,
        cashierName: profile.cashierName,
        companyMobile: profile.companyMobile,
        companyAddress: profile.companyAddress,
        shopName1: profile.shopName1,
        shopName2: profile.shopName2,
        addressLine1: profile.addressLine1,
        addressLine2: profile.addressLine2,
        addressLine3: profile.addressLine3,
        phoneNo1: profile.phoneNo1,
        phoneNo2: profile.phoneNo2,
        gstNumber: profile.gstNumber,
        panNumber: profile.panNumber,
        companyFssis: profile.companyFssis,
        paymentLogo: profile.paymentLogo,
        shopCgst: profile.shopCgst,
        shopSgst: profile.shopSgst,
        gstStatus: profile.gstEnabled ? '1' : '0',
      ),
    );
  }

  PrinterSettings mergePrinterSettings(
    PrinterSettings prefs,
    CompanyPrinterSetting? row,
  ) {
    if (row == null) return prefs;
    bool flagOn(String? value) => value == '1' || value == 'on';
    bool previewOn(String? value) => value != '0' && value != 'off';
    String text(String? value) => (value ?? '').trim();
    return prefs.copyWith(
      billBluetoothAddress: text(row.bluetoothAddress).isNotEmpty
          ? text(row.bluetoothAddress)
          : prefs.billBluetoothAddress,
      kotBluetoothAddress: text(row.bluetoothKotAddress).isNotEmpty
          ? text(row.bluetoothKotAddress)
          : prefs.kotBluetoothAddress,
      feedLines: int.tryParse(text(row.printerFeedLines)) ?? prefs.feedLines,
      kotFeedLines:
          int.tryParse(text(row.kotPrinterFeedLines)) ?? prefs.kotFeedLines,
      invoiceTitle: text(row.invoiceTitle).isNotEmpty
          ? text(row.invoiceTitle)
          : prefs.invoiceTitle,
      invoiceTerms: text(row.invoiceTermsCondition).isNotEmpty
          ? text(row.invoiceTermsCondition)
          : prefs.invoiceTerms,
      invoicePrefix: text(row.invoicePrefix).isNotEmpty
          ? text(row.invoicePrefix)
          : prefs.invoicePrefix,
      kotPrefix: text(row.kotPrefix).isNotEmpty
          ? text(row.kotPrefix)
          : prefs.kotPrefix,
      customerUse: flagOn(row.customerUse),
      paymentUse: flagOn(row.paymentUse),
      duplicateBillUse: flagOn(row.duplicateBillUse),
      logoUse: flagOn(row.logoUse),
      kotEnable: row.kotEnable != '0' && row.kotEnable != 'off',
      productQuantityUpdate: flagOn(row.productQuantityUpdate),
      kotAutoPrint: flagOn(row.kotAutoPrint),
      kotPreview: previewOn(row.kotPreview),
      kotCopies: int.tryParse(text(row.kotCopies)) ?? prefs.kotCopies,
    );
  }

  Future<CompanyPrinterSetting?> getLocalCompanyPrinterSettings() {
    return (select(companyPrinterSettings)
          ..orderBy([(t) => OrderingTerm.desc(t.settingId)])
          ..limit(1))
        .getSingleOrNull();
  }

  Future<void> upsertLocalCompanyPrinterSettings(
    CompanyPrinterSettingDto dto,
  ) async {
    await transaction(() async {
      await delete(companyPrinterSettings).go();
      await into(companyPrinterSettings).insert(
        CompanyPrinterSettingsCompanion.insert(
          settingId: dto.settingId == null
              ? const Value.absent()
              : Value(dto.settingId!),
          printerName: Value(dto.printerName),
          invoicePrefix: Value(dto.invoicePrefix),
          invoiceTitle: Value(dto.invoiceTitle),
          invoiceTermsCondition: Value(dto.invoiceTermsCondition),
          logoUse: Value(dto.logoUse),
          paymentUse: Value(dto.paymentUse),
          customerUse: Value(dto.customerUse),
          productQuantityUpdate: Value(dto.productQuantityUpdate),
          duplicateBillUse: Value(dto.duplicateBillUse),
          bluetoothAddress: Value(dto.bluetoothAddress),
          bluetoothKotAddress: Value(dto.bluetoothKotAddress),
          kotPrinterName: Value(dto.kotPrinterName),
          printerFeedLines: Value(dto.printerFeedLines),
          kotPrinterFeedLines: Value(dto.kotPrinterFeedLines),
          settingStatus: Value(dto.settingStatus),
          kotEnable: Value(dto.kotEnable == '1' || dto.kotEnable == 'on'
              ? 'on'
              : 'off'),
          kotPrefix: Value(dto.kotPrefix),
          kotCopies: Value(dto.kotCopies),
          kotAutoPrint: Value(
            dto.kotAutoPrint == '1' || dto.kotAutoPrint == 'on' ? 'on' : 'off',
          ),
          kotPreview: Value(
            dto.kotPreview == '0' || dto.kotPreview == 'off' ? 'off' : 'on',
          ),
        ),
      );
    });
  }

  Future<void> upsertLocalCompanyPrinterFromSettings(
    PrinterSettings settings,
  ) async {
    await upsertLocalCompanyPrinterSettings(
      CompanyPrinterSettingDto(
        printerName: settings.billBluetoothAddress,
        kotPrinterName: settings.kotBluetoothAddress,
        invoicePrefix: settings.invoicePrefix,
        invoiceTitle: settings.invoiceTitle,
        invoiceTermsCondition: settings.invoiceTerms,
        logoUse: settings.logoUse ? '1' : '0',
        paymentUse: settings.paymentUse ? '1' : '0',
        customerUse: settings.customerUse ? '1' : '0',
        productQuantityUpdate: settings.productQuantityUpdate ? '1' : '0',
        duplicateBillUse: settings.duplicateBillUse ? '1' : '0',
        bluetoothAddress: settings.billBluetoothAddress,
        bluetoothKotAddress: settings.kotBluetoothAddress,
        printerFeedLines: '${settings.feedLines}',
        kotPrinterFeedLines: '${settings.kotFeedLines}',
        kotEnable: settings.kotEnable ? 'on' : 'off',
        kotPrefix: settings.kotPrefix,
        kotCopies: '${settings.kotCopies}',
        kotAutoPrint: settings.kotAutoPrint ? 'on' : 'off',
        kotPreview: settings.kotPreview ? 'on' : 'off',
      ),
    );
  }

  // ── Mess meal token print queue (Android mess_meal_token_queue) ──

  Future<void> enqueueMessMealToken({
    required String serverPublicId,
    String? tokenNumber,
    String? registrationNo,
    String? mealSession,
    String? tokenDate,
    String? memberName,
    String? createdAt,
    String printStatus = 'RECEIVED',
  }) async {
    final id = serverPublicId.trim();
    if (id.isEmpty) return;
    final existing = await (select(messMealTokenQueue)
          ..where((t) => t.serverPublicId.equals(id))
          ..limit(1))
        .getSingleOrNull();
    final now = DateTime.now().toIso8601String();
    if (existing != null) {
      await (update(messMealTokenQueue)..where((t) => t.id.equals(existing.id)))
          .write(
        MessMealTokenQueueCompanion(
          tokenNumber: Value(tokenNumber ?? existing.tokenNumber),
          registrationNo: Value(registrationNo ?? existing.registrationNo),
          mealSession: Value(mealSession ?? existing.mealSession),
          tokenDate: Value(tokenDate ?? existing.tokenDate),
          memberName: Value(memberName ?? existing.memberName),
          printStatus: Value(printStatus),
          localUpdatedAt: Value(now),
        ),
      );
      return;
    }
    await into(messMealTokenQueue).insert(
      MessMealTokenQueueCompanion.insert(
        serverPublicId: id,
        tokenNumber: Value(tokenNumber),
        registrationNo: Value(registrationNo),
        mealSession: Value(mealSession),
        tokenDate: Value(tokenDate),
        memberName: Value(memberName),
        createdAt: Value(createdAt ?? now),
        printStatus: Value(printStatus),
        localUpdatedAt: Value(now),
      ),
    );
  }

  Future<List<MessMealTokenQueueData>> getPendingMessMealTokens({
    int limit = 100,
  }) {
    return (select(messMealTokenQueue)
          ..where((t) => t.printStatus.isNotValue('PRINTED'))
          ..orderBy([(t) => OrderingTerm.desc(t.id)])
          ..limit(limit))
        .get();
  }

  Future<void> markMessMealTokenPrinted(int id) async {
    await (update(messMealTokenQueue)..where((t) => t.id.equals(id))).write(
      MessMealTokenQueueCompanion(
        printStatus: const Value('PRINTED'),
        localUpdatedAt: Value(DateTime.now().toIso8601String()),
      ),
    );
  }
}

/// Aggregated product / combo sales for reports.
class ProductSalesRow {
  const ProductSalesRow({
    required this.productName,
    required this.totalQuantity,
    required this.totalAmount,
    this.itemType = 'product',
  });

  final String productName;
  final int totalQuantity;
  final double totalAmount;
  final String itemType;
}

class DailySalesPoint {
  const DailySalesPoint({required this.date, required this.total});

  final DateTime date;
  final double total;
}

class SyncPendingSnapshot {
  const SyncPendingSnapshot({
    this.invoices = 0,
    this.categories = 0,
    this.products = 0,
    this.portions = 0,
    this.combos = 0,
    this.subcategories = 0,
    this.messMembers = 0,
    this.messTokens = 0,
    this.messPayments = 0,
    this.messInvoices = 0,
    this.diningSessions = 0,
    this.inventory = 0,
    this.expenses = 0,
  });

  final int invoices;
  final int categories;
  final int products;
  final int portions;
  final int combos;
  final int subcategories;
  final int messMembers;
  final int messTokens;
  final int messPayments;
  final int messInvoices;
  final int diningSessions;
  final int inventory;
  final int expenses;

  int get total =>
      invoices +
      categories +
      products +
      portions +
      combos +
      subcategories +
      messMembers +
      messTokens +
      messPayments +
      messInvoices +
      diningSessions +
      inventory +
      expenses;
}

/// Result of creating a kitchen order ticket.
class KotTicket {
  const KotTicket({
    required this.kot,
    required this.items,
    required this.roundNumber,
  });

  final Kot kot;
  final List<KotItem> items;
  final int roundNumber;
}

/// Current stock balance derived from latest inventory movement.
class ProductStockBalance {
  const ProductStockBalance({
    required this.productId,
    required this.productName,
    required this.remaining,
    this.lowStock = false,
  });

  final int productId;
  final String productName;
  final double remaining;
  final bool lowStock;
}
