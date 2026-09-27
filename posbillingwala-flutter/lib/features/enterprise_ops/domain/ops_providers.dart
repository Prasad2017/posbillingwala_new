import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/enterprise/data/enterprise_api.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/data/ops_local_store.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/data/warehouse_stock_store.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';

final opsListProvider =
    FutureProvider.family<List<Map<String, dynamic>>, String>((ref, entity) async {
  final session = ref.watch(authControllerProvider).session;
  if (session == null) return const [];

  final key = _keyFor(entity);
  var local = await OpsLocalStore.load(session, key);

  if (await isDeviceOnline()) {
    try {
      final api = ref.read(enterpriseApiProvider);
      final remote = await api.fetchOpsList(session.licenceUserId, entity);
      if (remote.isNotEmpty) {
        await OpsLocalStore.saveAll(session, key, remote);
        local = remote;
      }
    } catch (_) {
      /* keep offline */
    }
  }
  return local;
});

final auditLogProvider =
    FutureProvider<List<Map<String, dynamic>>>((ref) async {
  final session = ref.watch(authControllerProvider).session;
  if (session == null) return const [];

  var local = await OpsLocalStore.load(session, OpsLocalStore.audit);
  if (await isDeviceOnline()) {
    try {
      final remote =
          await ref.read(enterpriseApiProvider).fetchAuditLog(session.licenceUserId);
      if (remote.isNotEmpty) {
        await OpsLocalStore.saveAll(session, OpsLocalStore.audit, remote);
        local = remote;
      }
    } catch (_) {}
  }
  return local;
});

final maxDiscountPctProvider = FutureProvider<double>((ref) async {
  final session = ref.watch(authControllerProvider).session;
  if (session == null) return 100;
  return OpsLocalStore.loadMaxDiscountPct(session);
});

String keyForOpsEntity(String entity) => _keyFor(entity);

String _keyFor(String entity) => switch (entity) {
      'warehouse' => OpsLocalStore.warehouse,
      'brand' => OpsLocalStore.brand,
      'offer' => OpsLocalStore.offer,
      'approval' => OpsLocalStore.approval,
      'transfer' => OpsLocalStore.transfer,
      'lot' => OpsLocalStore.lot,
      'return' => OpsLocalStore.returns,
      'serial' => OpsLocalStore.serial,
      _ => 'ops_$entity',
    };

Future<Map<String, dynamic>> upsertOpsRecord(
  dynamic ref, {
  required String entity,
  required Map<String, dynamic> row,
}) async {
  final session = ref.read(authControllerProvider).session;
  if (session == null) {
    throw StateError('Not signed in');
  }
  final saved = await OpsLocalStore.upsert(session, _keyFor(entity), row);
  await enterprisePushSafeRef(ref, (api, userId) {
    return api.saveOpsRecord(
      userId: userId,
      entity: entity,
      record: saved,
    );
  });
  ref.invalidate(opsListProvider(entity));
  return saved;
}

Future<void> setOpsStatus(
  dynamic ref, {
  required String entity,
  required String id,
  required String status,
}) async {
  final session = ref.read(authControllerProvider).session;
  if (session == null) return;
  final prevList = await OpsLocalStore.load(session, _keyFor(entity));
  final prev = prevList.firstWhere((e) => e['id'] == id, orElse: () => {});
  final prevStatus = (prev['status'] ?? '').toString().toUpperCase();

  await OpsLocalStore.setStatus(session, _keyFor(entity), id, status);
  final list = await OpsLocalStore.load(session, _keyFor(entity));
  final row = list.firstWhere((e) => e['id'] == id, orElse: () => {});
  if (row.isNotEmpty) {
    await enterprisePushSafeRef(ref, (api, userId) {
      return api.saveOpsRecord(userId: userId, entity: entity, record: row);
    });
  }
  ref.invalidate(opsListProvider(entity));

  final next = status.toUpperCase();
  if (entity == 'transfer' &&
      next == 'RECEIVED' &&
      prevStatus != 'RECEIVED' &&
      row.isNotEmpty) {
    await _postStockIn(ref, row, notePrefix: 'Transfer');
  }
  if (entity == 'return' &&
      next == 'COMPLETED' &&
      prevStatus != 'COMPLETED' &&
      row.isNotEmpty) {
    await _postStockIn(ref, row, notePrefix: 'Return');
  }
}

Future<void> _postStockIn(
  dynamic ref,
  Map<String, dynamic> row, {
  required String notePrefix,
}) async {
  final qty = double.tryParse('${row['qty'] ?? ''}') ?? 0;
  if (qty <= 0) return;
  final productName =
      (row['productName'] ?? row['title'] ?? row['name'] ?? '').toString().trim();
  if (productName.isEmpty) return;
  final productId = int.tryParse('${row['productId'] ?? ''}') ??
      (productName.hashCode.abs() % 100000);

  final session = ref.read(authControllerProvider).session;
  final fromWh = (row['fromWarehouse'] ?? '').toString().trim();
  final toWh = (row['toWarehouse'] ?? row['warehouse'] ?? '').toString().trim();

  if (notePrefix == 'Transfer' && session != null && fromWh.isNotEmpty) {
    /* True WH A → B: move warehouse ledger, then branch stock-in at destination. */
    await WarehouseStockStore.transfer(
      session,
      fromWarehouse: fromWh,
      toWarehouse: toWh.isEmpty ? WarehouseStockStore.defaultWarehouse : toWh,
      productId: productId,
      qty: qty,
    );
  } else if (session != null) {
    await WarehouseStockStore.adjust(
      session,
      warehouse: toWh.isEmpty ? WarehouseStockStore.defaultWarehouse : toWh,
      productId: productId,
      delta: qty,
    );
  }

  await ref.read(inventoryControllerProvider.notifier).addPurchase(
        productId: productId,
        productName: productName,
        quantity: qty,
        note: '$notePrefix ${row['id'] ?? ''}'
            '${fromWh.isEmpty ? '' : ' $fromWh→$toWh'}',
      );
}

Future<void> enterprisePushSafeRef(
  dynamic ref,
  Future<bool> Function(EnterpriseApi api, String userId) action,
) async {
  final session = ref.read(authControllerProvider).session;
  if (session == null) return;
  if (!await isDeviceOnline()) return;
  try {
    await action(ref.read(enterpriseApiProvider), session.licenceUserId);
  } catch (_) {}
}

