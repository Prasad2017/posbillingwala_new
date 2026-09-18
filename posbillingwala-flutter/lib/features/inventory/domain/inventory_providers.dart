import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/inventory/data/inventory_expense_api.dart';

final inventoryMovementsProvider = StreamProvider<List<InventoryMovement>>((
  ref,
) {
  return ref.watch(appDatabaseProvider).watchInventoryMovements();
});

final stockBalancesProvider = Provider<List<ProductStockBalance>>((ref) {
  final movements = ref
      .watch(inventoryMovementsProvider)
      .maybeWhen(
        data: (rows) => rows,
        orElse: () => const <InventoryMovement>[],
      );
  final latestByProduct = <int, InventoryMovement>{};
  /* movements are newest-first; first seen wins. */
  for (final row in movements) {
    latestByProduct.putIfAbsent(row.productId, () => row);
  }
  final balances =
      latestByProduct.values
          .map(
            (row) => ProductStockBalance(
              productId: row.productId,
              productName: row.productName.isEmpty
                  ? 'Product ${row.productId}'
                  : row.productName,
              remaining: row.afterSaleInventoryQuantity,
              lowStock: row.afterSaleInventoryQuantity < 6,
            ),
          )
          .toList()
        ..sort((a, b) => a.productName.compareTo(b.productName));
  return balances;
});

final expensesProvider = StreamProvider<List<ShopExpense>>((ref) {
  return ref.watch(appDatabaseProvider).watchExpenses();
});

final expensesTotalProvider = Provider<double>((ref) {
  final rows = ref
      .watch(expensesProvider)
      .maybeWhen(data: (rows) => rows, orElse: () => const <ShopExpense>[]);
  var total = 0.0;
  for (final row in rows) {
    total += row.expensesAmount;
  }
  return double.parse(total.toStringAsFixed(2));
});

class InventoryController extends Notifier<AsyncValue<String?>> {
  @override
  AsyncValue<String?> build() => const AsyncData(null);

  Future<void> addStock({
    required int productId,
    required String productName,
    required double quantity,
  }) => addPurchase(
    productId: productId,
    productName: productName,
    quantity: quantity,
  );

  Future<void> addPurchase({
    required int productId,
    required String productName,
    required double quantity,
    String note = '',
    double unitCost = 0,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      if (AppPlatform.requiresNetwork && !await ensureOnline()) {
        throw StateError(kOnlineRequiredMessage);
      }
      await ref
          .read(appDatabaseProvider)
          .addStockIn(
            productId: productId,
            productName: productName,
            quantity: quantity,
            movementType: 'purchase',
            note: note,
            unitCost: unitCost,
          );
      await uploadPendingIfOnline();
      return 'Purchase saved';
    });
  }

  Future<void> addWaste({
    required int productId,
    required String productName,
    required double quantity,
    String reason = '',
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      if (AppPlatform.requiresNetwork && !await ensureOnline()) {
        throw StateError(kOnlineRequiredMessage);
      }
      await ref
          .read(appDatabaseProvider)
          .addWasteOut(
            productId: productId,
            productName: productName,
            quantity: quantity,
            reason: reason,
          );
      await uploadPendingIfOnline();
      return 'Waste saved';
    });
  }

  Future<void> addExpense({
    required String name,
    required double amount,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      if (AppPlatform.requiresNetwork && !await ensureOnline()) {
        throw StateError(kOnlineRequiredMessage);
      }
      await ref
          .read(appDatabaseProvider)
          .addExpense(name: name, amount: amount);
      await uploadPendingIfOnline();
      return 'Expense saved';
    });
  }

  /* Dual-write when online: push pending inventory/expense rows to API. */
  Future<void> uploadPendingIfOnline() async {
    if (!await isDeviceOnline()) {
      if (AppPlatform.requiresNetwork) {
        throw StateError(kOnlineRequiredMessage);
      }
      return;
    }
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      if (AppPlatform.requiresNetwork) {
        throw StateError('Please login to save on Web POS.');
      }
      return;
    }
    final db = ref.read(appDatabaseProvider);
    final api = InventoryExpenseApi(ref.read(apiClientProvider));
    for (final row in await db.getPendingInventory()) {
      final ok = await api.uploadInventory(userId: userId, row: row);
      if (ok) await db.markInventorySynced(row.inventoryId);
    }
    for (final row in await db.getPendingExpenses()) {
      final ok = await api.uploadExpense(userId: userId, row: row);
      if (ok) await db.markExpenseSynced(row.expensesId);
    }
    if (AppPlatform.requiresNetwork) {
      final leftInv = (await db.getPendingInventory()).length;
      final leftExp = (await db.getPendingExpenses()).length;
      if (leftInv > 0 || leftExp > 0) {
        throw StateError(kWebApiSaveFailedMessage);
      }
    }
  }

  Future<String> syncAll() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      throw StateError('Please login first');
    }
    state = const AsyncLoading();
    try {
      final db = ref.read(appDatabaseProvider);
      final api = InventoryExpenseApi(ref.read(apiClientProvider));

      var uploaded = 0;
      for (final row in await db.getPendingInventory()) {
        final ok = await api.uploadInventory(userId: userId, row: row);
        if (ok) {
          await db.markInventorySynced(row.inventoryId);
          uploaded++;
        }
      }
      for (final row in await db.getPendingExpenses()) {
        final ok = await api.uploadExpense(userId: userId, row: row);
        if (ok) {
          await db.markExpenseSynced(row.expensesId);
          uploaded++;
        }
      }

      final cloudInventory = await api.fetchInventory(userId);
      final products = await ref
          .read(appDatabaseProvider)
          .watchActiveProducts()
          .first;
      final nameById = {for (final p in products) p.productId: p.productName};
      final invCompanions = cloudInventory
          .where((e) => e.productId > 0)
          .map((e) => e.toCompanion(productNameOverride: nameById[e.productId]))
          .toList();
      final invDownloaded = await db.upsertCloudInventory(invCompanions);

      final cloudExpenses = await api.fetchExpenses(userId);
      final expCompanions = cloudExpenses
          .where((e) => e.expensesName.trim().isNotEmpty)
          .map((e) => e.toCompanion())
          .toList();
      final expDownloaded = await db.upsertCloudExpenses(expCompanions);

      final message =
          'Uploaded $uploaded · Downloaded inventory $invDownloaded, expenses $expDownloaded';
      state = AsyncData(message);
      return message;
    } catch (e, st) {
      state = AsyncError(e, st);
      rethrow;
    }
  }
}

final inventoryControllerProvider =
    NotifierProvider<InventoryController, AsyncValue<String?>>(
      InventoryController.new,
    );
