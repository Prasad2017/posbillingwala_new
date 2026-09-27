import 'package:pos_billingwala_v2/core/database/branch_scope.dart';
import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

/* Per-warehouse qty ledger (branch-scoped). Keys: warehouseName -> productId -> qty. */
abstract final class WarehouseStockStore {
  WarehouseStockStore._();

  static const _base = 'warehouse_stock_ledger';
  static const defaultWarehouse = 'Main';

  static String _key(UserSession s) =>
      '${_base}_${BranchScope.effectiveOrganizationId(s)}_${BranchScope.effectiveBranchId(s)}';

  static Future<Map<String, Map<int, double>>> load(UserSession session) async {
    final raw = await CloudScreenCache.loadJson(_key(session));
    if (raw is! Map) return {};
    final out = <String, Map<int, double>>{};
    raw.forEach((wh, products) {
      if (products is! Map) return;
      final m = <int, double>{};
      products.forEach((pid, qty) {
        final id = int.tryParse('$pid');
        final q = qty is num ? qty.toDouble() : double.tryParse('$qty');
        if (id != null && q != null) m[id] = q;
      });
      out['$wh'] = m;
    });
    return out;
  }

  static Future<void> save(
    UserSession session,
    Map<String, Map<int, double>> ledger,
  ) async {
    final encoded = <String, Map<String, double>>{};
    ledger.forEach((wh, products) {
      encoded[wh] = {
        for (final e in products.entries) '${e.key}': e.value,
      };
    });
    await CloudScreenCache.saveJson(_key(session), encoded);
  }

  static Future<double> qty(
    UserSession session, {
    required String warehouse,
    required int productId,
  }) async {
    final ledger = await load(session);
    return ledger[warehouse]?[productId] ?? 0;
  }

  static Future<void> adjust(
    UserSession session, {
    required String warehouse,
    required int productId,
    required double delta,
  }) async {
    final wh = warehouse.trim().isEmpty ? defaultWarehouse : warehouse.trim();
    final ledger = await load(session);
    final products = Map<int, double>.from(ledger[wh] ?? {});
    final next = (products[productId] ?? 0) + delta;
    products[productId] = next < 0 ? 0 : next;
    ledger[wh] = products;
    await save(session, ledger);
  }

  static Future<void> transfer(
    UserSession session, {
    required String fromWarehouse,
    required String toWarehouse,
    required int productId,
    required double qty,
  }) async {
    if (qty <= 0) return;
    await adjust(
      session,
      warehouse: fromWarehouse,
      productId: productId,
      delta: -qty,
    );
    await adjust(
      session,
      warehouse: toWarehouse,
      productId: productId,
      delta: qty,
    );
  }

  static Future<List<({String warehouse, int productId, double qty})>>
      flatBalances(UserSession session) async {
    final ledger = await load(session);
    final rows = <({String warehouse, int productId, double qty})>[];
    ledger.forEach((wh, products) {
      products.forEach((pid, qty) {
        if (qty == 0) return;
        rows.add((warehouse: wh, productId: pid, qty: qty));
      });
    });
    rows.sort((a, b) => a.warehouse.compareTo(b.warehouse));
    return rows;
  }
}
