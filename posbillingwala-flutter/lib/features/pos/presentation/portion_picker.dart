import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

/// Matches `dialog_select_portion.xml`: product name, portion tabs, qty stepper,
/// Dismiss + Add to cart.
Future<void> addProductWithPortionPicker(
  BuildContext context,
  WidgetRef ref,
  Product product,
) async {
  final portions =
      await ref.read(appDatabaseProvider).getPortionsForProduct(product.productId);
  if (!context.mounted) return;

  final isOpen = product.openPrice == '1';

  if (portions.isEmpty) {
    if (isOpen) {
      await _promptOpenPriceAndAdd(context, ref, product);
    } else {
      await ref.read(posCartControllerProvider.notifier).addProduct(product);
    }
    return;
  }

  final strings = AppStrings.of(ref);
  final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
  final options = <({String label, double price, ProductPortion? portion})>[
    (
      label: strings.regular,
      price: product.productPrice,
      portion: null,
    ),
    ...portions.map(
      (p) => (
        label: p.portionName.trim().isEmpty ? 'Portion' : p.portionName,
        price: p.portionPrice,
        portion: p,
      ),
    ),
  ];

  var selectedIndex = 0;
  var qty = 1;

  final confirmed = await showDialog<bool>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setLocal) {
        return Dialog(
          insetPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(5)),
          elevation: 7,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                child: Text(
                  product.productName,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 17,
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                child: Text(
                  strings.selectPortion,
                  style: const TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 16,
                  ),
                ),
              ),
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.fromLTRB(10, 8, 10, 0),
                child: Row(
                  children: [
                    for (var i = 0; i < options.length; i++)
                      Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: ChoiceChip(
                          label: Text(
                            '${options[i].label}\n${currency.format(options[i].price)}',
                            textAlign: TextAlign.center,
                            style: const TextStyle(fontSize: 12, height: 1.2),
                          ),
                          selected: selectedIndex == i,
                          onSelected: (_) =>
                              setLocal(() => selectedIndex = i),
                        ),
                      ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                child: Text(
                  strings.quantity,
                  style: const TextStyle(fontSize: 14),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
                child: Row(
                  children: [
                    _QtyBox(
                      label: '−',
                      onTap: qty <= 1
                          ? null
                          : () => setLocal(() => qty -= 1),
                    ),
                    Expanded(
                      child: Container(
                        height: 44,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          border: Border.all(color: AppColors.border),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          '$qty',
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                    _QtyBox(
                      label: '+',
                      onTap: () => setLocal(() => qty += 1),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              Row(
                children: [
                  Expanded(
                    child: Material(
                      color: AppColors.red,
                      child: InkWell(
                        onTap: () => Navigator.pop(context, false),
                        child: SizedBox(
                          height: 40,
                          child: Center(
                            child: Text(
                              strings.dismiss,
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 14,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                  Expanded(
                    child: Material(
                      color: AppColors.primary,
                      child: InkWell(
                        onTap: () => Navigator.pop(context, true),
                        child: SizedBox(
                          height: 40,
                          child: Center(
                            child: Text(
                              strings.addToCart,
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 14,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        );
      },
    ),
  );

  if (confirmed != true || !context.mounted) return;
  final chosen = options[selectedIndex];
  if (isOpen) {
    await _promptOpenPriceAndAdd(
      context,
      ref,
      product,
      portion: chosen.portion,
      initialQty: qty,
    );
  } else {
    await ref.read(posCartControllerProvider.notifier).addProduct(
          product,
          portion: chosen.portion,
          quantity: qty,
        );
  }
}

Future<void> _promptOpenPriceAndAdd(
  BuildContext context,
  WidgetRef ref,
  Product product, {
  ProductPortion? portion,
  int initialQty = 1,
}) async {
  final base = portion?.portionPrice ?? product.productPrice;
  final priceCtrl = TextEditingController(
    text: base > 0 ? base.toStringAsFixed(2) : '',
  );
  final qtyCtrl = TextEditingController(text: '$initialQty');

  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      contentPadding: EdgeInsets.zero,
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 20, 16, 0),
            child: Text(
              product.productName,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
            child: AppTextField(
              controller: priceCtrl,
              label: AppStrings.of(ref).productPrice,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              autofocus: true,
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: AppTextField(
              controller: qtyCtrl,
              label: AppStrings.of(ref).quantity,
              keyboardType: TextInputType.number,
            ),
          ),
          SizedBox(
            width: double.infinity,
            height: 44,
            child: Material(
              color: Theme.of(context).colorScheme.primary,
              child: InkWell(
                onTap: () => Navigator.pop(context, true),
                child: const Center(
                  child: Text(
                    'ADD TO CART',
                    style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w600,
                      fontSize: 15,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    ),
  );

  final price = double.tryParse(priceCtrl.text.trim()) ?? 0;
  final qty = int.tryParse(qtyCtrl.text.trim()) ?? 1;
  priceCtrl.dispose();
  qtyCtrl.dispose();
  if (ok != true || !context.mounted) return;
  if (price <= 0) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(AppStrings.of(ref).enterValidPrice)),
    );
    return;
  }
  await ref.read(posCartControllerProvider.notifier).addProduct(
        product,
        portion: portion,
        unitPriceOverride: price,
        quantity: qty < 1 ? 1 : qty,
      );
}

/// Edit cart line quantity and/or unit price (Android qty/price dialog).
Future<void> editCartLineDialog(
  BuildContext context,
  WidgetRef ref,
  CartItem item,
) async {
  final qtyCtrl = TextEditingController(text: '${item.quantity}');
  final priceCtrl = TextEditingController(
    text: item.unitPrice.toStringAsFixed(2),
  );
  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      contentPadding: EdgeInsets.zero,
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 20, 16, 0),
            child: Text(
              item.productName,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
            child: AppTextField(
              controller: priceCtrl,
              label: AppStrings.of(ref).productPrice,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: AppTextField(
              controller: qtyCtrl,
              label: AppStrings.of(ref).quantity,
              keyboardType: TextInputType.number,
            ),
          ),
          SizedBox(
            width: double.infinity,
            height: 44,
            child: Material(
              color: Theme.of(context).colorScheme.primary,
              child: InkWell(
                onTap: () => Navigator.pop(context, true),
                child: const Center(
                  child: Text(
                    'SAVE',
                    style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w600,
                      fontSize: 15,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    ),
  );
  final qty = int.tryParse(qtyCtrl.text.trim()) ?? item.quantity;
  final price = double.tryParse(priceCtrl.text.trim());
  qtyCtrl.dispose();
  priceCtrl.dispose();
  if (ok != true || !context.mounted) return;
  await ref.read(posCartControllerProvider.notifier).setLine(
        item: item,
        quantity: qty < 0 ? 0 : qty,
        unitPrice: price != null && price > 0 ? price : null,
      );
}

class _QtyBox extends StatelessWidget {
  const _QtyBox({required this.label, this.onTap});

  final String label;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.primary,
      child: InkWell(
        onTap: onTap,
        child: SizedBox(
          width: 44,
          height: 44,
          child: Center(
            child: Text(
              label,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 22,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
