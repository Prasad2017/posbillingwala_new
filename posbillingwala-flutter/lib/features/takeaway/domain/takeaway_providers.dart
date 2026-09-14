import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/features/tables/domain/tables_providers.dart';

class TakeawayParcel {
  const TakeawayParcel({
    required this.parcelNumber,
    required this.billAmount,
  });

  final String parcelNumber;
  final double billAmount;
}

/// Open takeaway carts grouped by parcel scope (`P1`, `P2`, …).
final openTakeawayParcelsProvider = Provider<List<TakeawayParcel>>((ref) {
  final cart = ref.watch(allCartItemsProvider).maybeWhen(
        data: (rows) => rows,
        orElse: () => const <CartItem>[],
      );

  final totals = <String, double>{};
  for (final item in cart) {
    if (!AppDatabase.isTakeawayParcelScope(item.cartScope)) continue;
    final key = item.cartScope.trim().toUpperCase();
    final line =
        item.unitPrice * item.quantity * (1 + item.gstPercent / 100);
    totals[key] = (totals[key] ?? 0) + line;
  }

  final parcels = totals.entries
      .map(
        (e) => TakeawayParcel(
          parcelNumber: e.key,
          billAmount: double.parse(e.value.toStringAsFixed(2)),
        ),
      )
      .toList();

  parcels.sort((a, b) {
    final aN = int.tryParse(a.parcelNumber.replaceAll(RegExp(r'[^0-9]'), '')) ?? 0;
    final bN = int.tryParse(b.parcelNumber.replaceAll(RegExp(r'[^0-9]'), '')) ?? 0;
    return aN.compareTo(bN);
  });
  return parcels;
});
