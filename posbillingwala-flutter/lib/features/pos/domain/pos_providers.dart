import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/product_units.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';

import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';

final posSelectedCategoryIdProvider =
    NotifierProvider<PosSelectedCategoryId, int?>(PosSelectedCategoryId.new);

class PosSelectedCategoryId extends Notifier<int?> {
  @override
  int? build() => null;

  void select(int? id) {
    state = id;
  }
}

final posSelectedSubcategoryIdProvider =
    NotifierProvider<PosSelectedSubcategoryId, int?>(
      PosSelectedSubcategoryId.new,
    );

class PosSelectedSubcategoryId extends Notifier<int?> {
  @override
  int? build() {
    ref.listen(posSelectedCategoryIdProvider, (prev, next) {
      if (prev != next) {
        state = null;
      }
    });
    return null;
  }

  void select(int? id) => state = id;
}

final posSubcategoriesProvider = StreamProvider<List<ProductSubcategory>>((
  ref,
) {
  final categoryId = ref.watch(posSelectedCategoryIdProvider);
  if (categoryId == null) {
    return Stream.value(const <ProductSubcategory>[]);
  }
  return ref
      .watch(mastersRepositoryProvider)
      .watchSubcategories(categoryId: categoryId);
});

final posProductsProvider = StreamProvider<List<Product>>((ref) {
  final categoryId = ref.watch(posSelectedCategoryIdProvider);
  final subcategoryId = ref.watch(posSelectedSubcategoryIdProvider);
  return ref
      .watch(mastersRepositoryProvider)
      .watchProducts(categoryId: categoryId, subcategoryId: subcategoryId);
});

final posCombosProvider = StreamProvider<List<Combo>>((ref) {
  return ref.watch(appDatabaseProvider).watchActiveCombos();
});

final cartItemsProvider = StreamProvider<List<CartItem>>((ref) {
  final scope = ref.watch(billingSessionProvider).cartScope;
  return ref.watch(appDatabaseProvider).watchCartItems(cartScope: scope);
});

class CartSummary {
  const CartSummary({
    required this.itemKinds,
    required this.totalQuantity,
    required this.subtotal,
    required this.cgstTotal,
    required this.sgstTotal,
    required this.taxTotal,
    required this.grandTotal,
  });

  final int itemKinds;
  final double totalQuantity;
  final double subtotal;
  final double cgstTotal;
  final double sgstTotal;
  final double taxTotal;
  final double grandTotal;

  bool get isEmpty => itemKinds == 0;

  bool get hasCgst => cgstTotal > 0.005;

  bool get hasSgst => sgstTotal > 0.005;

  bool get hasTax => taxTotal > 0.005;
}

final cartSummaryProvider = Provider<CartSummary>((ref) {
  final cart = ref
      .watch(cartItemsProvider)
      .maybeWhen(data: (items) => items, orElse: () => const <CartItem>[]);
  final gstEnabled = ref.watch(shopReceiptProfileProvider).gstEnabled;
  var subtotal = 0.0;
  var cgstTotal = 0.0;
  var sgstTotal = 0.0;
  var taxTotal = 0.0;
  var qty = 0.0;

  for (final item in cart) {
    final lineBase = item.unitPrice * item.quantity;
    subtotal += lineBase;
    qty += item.quantity;
    if (!gstEnabled) continue;

    final cgstRate = item.productCgst;
    final sgstRate = item.productSgst;
    if (cgstRate > 0 || sgstRate > 0) {
      cgstTotal += lineBase * cgstRate / 100;
      sgstTotal += lineBase * sgstRate / 100;
      taxTotal += lineBase * (cgstRate + sgstRate) / 100;
    } else if (item.gstPercent > 0) {
      /* Fallback when only combined GST % is stored. */
      final half = item.gstPercent / 2;
      cgstTotal += lineBase * half / 100;
      sgstTotal += lineBase * half / 100;
      taxTotal += lineBase * item.gstPercent / 100;
    }
  }

  final sub = double.parse(subtotal.toStringAsFixed(2));
  final cgst = gstEnabled ? double.parse(cgstTotal.toStringAsFixed(2)) : 0.0;
  final sgst = gstEnabled ? double.parse(sgstTotal.toStringAsFixed(2)) : 0.0;
  final tax = gstEnabled ? double.parse(taxTotal.toStringAsFixed(2)) : 0.0;

  return CartSummary(
    itemKinds: cart.length,
    totalQuantity: qty,
    subtotal: sub,
    cgstTotal: cgst,
    sgstTotal: sgst,
    taxTotal: tax,
    grandTotal: (sub + tax).ceilToDouble(),
  );
});

class PosCartController extends Notifier<void> {
  @override
  void build() {}

  AppDatabase get db => ref.read(appDatabaseProvider);

  BillingSession get session => ref.read(billingSessionProvider);

  Future<void> addProduct(
    Product product, {
    ProductPortion? portion,
    double? unitPriceOverride,
    double quantity = 1,
  }) async {
    final shop = ref.read(shopReceiptProfileProvider);
    final shopGst = shop.gstEnabled ? shop.shopGstPercent : 0.0;
    await db.addProductToCart(
      product,
      cartScope: session.cartScope,
      diningSessionId: session.diningSessionId,
      portion: portion,
      shopGstPercentFallback: shopGst > 0 ? shopGst : null,
      unitPriceOverride: unitPriceOverride,
      quantity: quantity,
    );
  }

  Future<void> addCombo(Combo combo) async {
    await db.addComboToCart(
      combo,
      cartScope: session.cartScope,
      diningSessionId: session.diningSessionId,
    );
  }

  Future<void> increment(CartItem item) async {
    final step = ProductUnits.stepFor(item.productUnit);
    await db.changeCartQuantity(
      item.productId,
      item.quantity + step,
      cartScope: session.cartScope,
      portionId: item.portionId,
    );
  }

  Future<void> decrement(CartItem item) async {
    final step = ProductUnits.stepFor(item.productUnit);
    await db.changeCartQuantity(
      item.productId,
      item.quantity - step,
      cartScope: session.cartScope,
      portionId: item.portionId,
    );
  }

  Future<void> setLine({
    required CartItem item,
    required double quantity,
    double? unitPrice,
  }) async {
    await db.changeCartQuantity(
      item.productId,
      quantity,
      cartScope: session.cartScope,
      portionId: item.portionId,
      unitPrice: unitPrice,
    );
  }

  Future<void> remove(CartItem item) async {
    await db.removeCartItem(
      item.productId,
      cartScope: session.cartScope,
      portionId: item.portionId,
    );
  }

  Future<void> clear() async {
    await db.clearCart(cartScope: session.cartScope);
  }
}

final posCartControllerProvider = NotifierProvider<PosCartController, void>(
  PosCartController.new,
);
