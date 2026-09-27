import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/domain/ops_providers.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';

final unprintedCartCountProvider = Provider<double>((ref) {
  final cart = ref
      .watch(cartItemsProvider)
      .maybeWhen(data: (items) => items, orElse: () => const <CartItem>[]);
  var count = 0.0;
  for (final item in cart) {
    final delta = item.quantity - item.printedQuantity;
    if (delta > 0) count += delta;
  }
  return count;
});

String resolveKitchenName(
  Product? product,
  List<Map<String, dynamic>> kitchens,
) {
  if (product == null || kitchens.isEmpty) return 'Main Kitchen';
  final cat = (product.categoryName ?? '').trim().toLowerCase();
  final code = (product.productCode ?? '').trim().toLowerCase();
  for (final k in kitchens) {
    final type = (k['type'] ?? '').toString().toLowerCase();
    final isKitchen = type.contains('kitchen') ||
        type.contains('dept') ||
        type.isEmpty ||
        type == 'kitchen';
    if (!isKitchen && type.isNotEmpty) continue;
    final name = (k['name'] ?? k['title'] ?? '').toString().trim();
    final kCode = (k['code'] ?? '').toString().trim().toLowerCase();
    if (name.isEmpty) continue;
    if (cat.isNotEmpty && cat == name.toLowerCase()) return name;
    if (kCode.isNotEmpty &&
        (code == kCode || code.startsWith('$kCode-') || cat.contains(kCode))) {
      return name;
    }
  }
  return 'Main Kitchen';
}

class KotController extends Notifier<AsyncValue<KotTicket?>> {
  @override
  AsyncValue<KotTicket?> build() => const AsyncData(null);

  Future<KotTicket> createKot() async {
    final session = ref.read(billingSessionProvider);
    final diningSessionId = session.diningSessionId;
    final tableNumber = session.tableNumber;
    if (diningSessionId == null || tableNumber == null) {
      throw StateError('KOT is only available for dine-in tables');
    }

    state = const AsyncLoading();
    try {
      final db = ref.read(appDatabaseProvider);
      final scope = session.cartScope;
      final unprinted = await db.getUnprintedCartItems(
        cartScope: scope.isEmpty ? tableNumber : scope,
      );
      if (unprinted.isEmpty) {
        throw StateError('No new items to send to kitchen');
      }

      List<Map<String, dynamic>> kitchens = const [];
      try {
        kitchens = await ref.read(opsListProvider('warehouse').future);
      } catch (_) {}

      final groups = <String, Set<int>>{};
      for (final item in unprinted) {
        final product = await db.getProduct(item.productId);
        final kitchen = resolveKitchenName(product, kitchens);
        groups.putIfAbsent(kitchen, () => {}).add(item.productId);
      }

      KotTicket? last;
      for (final entry in groups.entries) {
        last = await db.createKotFromUnprintedCart(
          sessionId: diningSessionId,
          tableNumber: tableNumber,
          cartScope: session.cartScope,
          kitchenName: entry.key,
          onlyProductIds: entry.value,
        );
      }
      state = AsyncData(last);
      return last!;
    } catch (e, st) {
      state = AsyncError(e, st);
      rethrow;
    }
  }

  Future<void> markPrinted(int kotId) async {
    await ref.read(appDatabaseProvider).markKotPrinted(kotId);
  }
}

final kotControllerProvider =
    NotifierProvider<KotController, AsyncValue<KotTicket?>>(KotController.new);
