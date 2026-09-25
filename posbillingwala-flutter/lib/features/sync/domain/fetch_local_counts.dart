import 'package:drift/drift.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

/* One row on the post-fetch "saved to local DB" screen. */
class FetchCountRow {
  const FetchCountRow({
    required this.label,
    required this.count,
    this.group = 'Catalog',
  });

  final String label;
  final int count;
  final String group;
}

/* Snapshot of how many rows are stored locally after a cloud fetch. */
class FetchLocalCounts {
  const FetchLocalCounts({required this.rows});

  final List<FetchCountRow> rows;

  int get totalSaved => rows.fold(0, (sum, row) => sum + row.count);

  List<String> get groups {
    final seen = <String>{};
    final out = <String>[];
    for (final row in rows) {
      if (seen.add(row.group)) out.add(row.group);
    }
    return out;
  }

  List<FetchCountRow> rowsFor(String group) =>
      rows.where((r) => r.group == group).toList();

  static Future<FetchLocalCounts> load(AppDatabase db) async {
    Future<int> tableCount(TableInfo table) async {
      final exp = countAll();
      final query = db.selectOnly(table)..addColumns([exp]);
      final row = await query.getSingle();
      return row.read(exp) ?? 0;
    }

    final foodTypes = await tableCount(db.foodTypes);
    final categories = await db.countActiveCategories();
    final subcategories = await db.countActiveSubcategories();
    final products = await db.countActiveProducts();
    final portions =
        await (db.select(db.productPortions)
              ..where((t) => t.portionDeletedStatus.equals('0')))
            .get()
            .then((r) => r.length);
    final portionMasters =
        await (db.select(db.portionMasters)
              ..where((t) => t.portionMasterDeletedStatus.equals('0')))
            .get()
            .then((r) => r.length);
    final combos = await db.countActiveCombos();
    final comboItems =
        await (db.select(db.comboItems)
              ..where((t) => t.comboItemDeletedStatus.equals('0')))
            .get()
            .then((r) => r.length);
    final diningAreas = await (db.select(
      db.diningAreas,
    )..where((t) => t.areaActive.equals('1'))).get().then((r) => r.length);
    final tableTypes = await (db.select(
      db.tableTypes,
    )..where((t) => t.tableTypeActive.equals('1'))).get().then((r) => r.length);
    final tables = await db.countActivePosTables();
    final invoices = await db.countTotalInvoices();
    final invoiceItems = await tableCount(db.invoiceItems);
    final invoiceComboItems = await tableCount(db.invoiceComboItems);
    final inventory = await tableCount(db.inventoryMovements);
    final expenses = await tableCount(db.shopExpenses);
    final messMembers = await (db.select(db.messMembers)..where(
      (t) => t.memberStatus.equals('1') | t.memberStatus.equals('active'),
    )).get().then((r) => r.length);
    final messPayments = await tableCount(db.messMemberPayments);
    final messInvoices = await tableCount(db.messInvoices);
    final messTokens = await tableCount(db.messTokens);
    final diningSessions = await tableCount(db.diningSessions);
    final companies = await tableCount(db.companies);
    final printerSettings = await tableCount(db.companyPrinterSettings);
    final screenCounts = await CloudScreenCache.allCounts();

    return FetchLocalCounts(
      rows: [
        FetchCountRow(label: 'Food types', count: foodTypes),
        FetchCountRow(label: 'Categories', count: categories),
        FetchCountRow(label: 'Subcategories', count: subcategories),
        FetchCountRow(label: 'Products', count: products),
        FetchCountRow(label: 'Portions', count: portions),
        FetchCountRow(label: 'Portion masters', count: portionMasters),
        FetchCountRow(label: 'Combos', count: combos),
        FetchCountRow(label: 'Combo items', count: comboItems),
        FetchCountRow(
          label: 'Dining areas',
          count: diningAreas,
          group: 'Tables',
        ),
        FetchCountRow(label: 'Table types', count: tableTypes, group: 'Tables'),
        FetchCountRow(label: 'POS tables', count: tables, group: 'Tables'),
        FetchCountRow(label: 'Invoices', count: invoices, group: 'Sales'),
        FetchCountRow(
          label: 'Invoice items',
          count: invoiceItems,
          group: 'Sales',
        ),
        FetchCountRow(
          label: 'Invoice combo items',
          count: invoiceComboItems,
          group: 'Sales',
        ),
        FetchCountRow(
          label: 'Inventory movements',
          count: inventory,
          group: 'Ops',
        ),
        FetchCountRow(label: 'Expenses', count: expenses, group: 'Ops'),
        FetchCountRow(label: 'Mess members', count: messMembers, group: 'Mess'),
        FetchCountRow(
          label: 'Mess payments',
          count: messPayments,
          group: 'Mess',
        ),
        FetchCountRow(
          label: 'Mess invoices',
          count: messInvoices,
          group: 'Mess',
        ),
        FetchCountRow(label: 'Mess tokens', count: messTokens, group: 'Mess'),
        FetchCountRow(
          label: 'Dining sessions',
          count: diningSessions,
          group: 'Ops',
        ),
        FetchCountRow(
          label: 'Company profiles',
          count: companies,
          group: 'Settings',
        ),
        FetchCountRow(
          label: 'Printer settings',
          count: printerSettings,
          group: 'Settings',
        ),
        FetchCountRow(
          label: 'Staff users',
          count: screenCounts[CloudScreenCache.staff] ?? 0,
          group: 'Screens',
        ),
        FetchCountRow(
          label: 'Salary rows',
          count: screenCounts[CloudScreenCache.salary] ?? 0,
          group: 'Screens',
        ),
        FetchCountRow(
          label: 'Role defaults',
          count: screenCounts[CloudScreenCache.roleDefaults] ?? 0,
          group: 'Screens',
        ),
        FetchCountRow(
          label: 'Meal sessions',
          count: screenCounts[CloudScreenCache.mealSessions] ?? 0,
          group: 'Screens',
        ),
        FetchCountRow(
          label: 'Meal tokens today',
          count: screenCounts[CloudScreenCache.mealTokensToday] ?? 0,
          group: 'Screens',
        ),
        FetchCountRow(
          label: 'Pending meal tokens',
          count: screenCounts[CloudScreenCache.pendingMealTokens] ?? 0,
          group: 'Screens',
        ),
        FetchCountRow(
          label: 'Mess shop payer',
          count: screenCounts[CloudScreenCache.messShopPayerMode] ?? 0,
          group: 'Screens',
        ),
        FetchCountRow(
          label: 'Mess common QR',
          count: screenCounts[CloudScreenCache.messCommonQr] ?? 0,
          group: 'Screens',
        ),
        FetchCountRow(
          label: 'Store printers',
          count: screenCounts[CloudScreenCache.storePrinters] ?? 0,
          group: 'Screens',
        ),
        FetchCountRow(
          label: 'Printer routes',
          count: screenCounts[CloudScreenCache.printerRoutes] ?? 0,
          group: 'Screens',
        ),
        FetchCountRow(
          label: 'Print jobs',
          count: screenCounts[CloudScreenCache.printJobs] ?? 0,
          group: 'Screens',
        ),
        FetchCountRow(
          label: 'Support tickets',
          count: screenCounts[CloudScreenCache.supportTickets] ?? 0,
          group: 'Screens',
        ),
        FetchCountRow(
          label: 'POS devices',
          count: screenCounts[CloudScreenCache.posDevices] ?? 0,
          group: 'Screens',
        ),
        FetchCountRow(
          label: 'Home overview',
          count:
              (screenCounts[CloudScreenCache.homeOverviewToday] ?? 0) +
              (screenCounts[CloudScreenCache.homeOverviewMonth] ?? 0),
          group: 'Screens',
        ),
      ],
    );
  }
}
