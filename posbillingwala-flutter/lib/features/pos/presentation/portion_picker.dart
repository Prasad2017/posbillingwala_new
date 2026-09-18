import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/masters/domain/product_units.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Matches `dialog_select_portion.xml`: product name, portion tabs, qty stepper, */
/* Dismiss + Add to cart. */
Future<void> addProductWithPortionPicker(
  BuildContext context,
  WidgetRef ref,
  Product product,
) async {
  final portions = await ref
      .read(appDatabaseProvider)
      .getPortionsForProduct(product.productId);
  if (!context.mounted) return;

  final isOpen = product.openPrice == '1';

  if (portions.isEmpty) {
    if (isOpen) {
      await promptOpenPriceAndAdd(context, ref, product);
    } else {
      await ref.read(posCartControllerProvider.notifier).addProduct(product);
    }
    return;
  }

  final strings = AppStrings.of(ref);
  final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
  /* When portions exist, only show portion prices — not the base product price. */
  final options = <({String label, double price, ProductPortion portion})>[
    for (final p in portions)
      (
        label: p.portionName.trim().isEmpty ? 'Portion' : p.portionName.trim(),
        price: p.portionPrice,
        portion: p,
      ),
  ];

  var selectedIndex = 0;
  var qty = 1.0;
  final step = ProductUnits.stepFor(product.productUnit);

  final confirmed = await showDialog<bool>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setLocal) {
        return Dialog(
          insetPadding: const EdgeInsets.symmetric(
            horizontal: 16,
            vertical: 24,
          ),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          elevation: 7,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 18, 16, 0),
                child: Text(
                  product.productName,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w800,
                    fontSize: 18,
                    color: AppColors.navy,
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 14, 16, 0),
                child: Text(
                  strings.selectPortion,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w700,
                    fontSize: 15,
                    color: AppColors.navy,
                  ),
                ),
              ),
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
                child: Row(
                  children: [
                    for (var i = 0; i < options.length; i++)
                      Padding(
                        padding: const EdgeInsets.only(right: 10),
                        child: _PortionChoiceChip(
                          label: options[i].label,
                          priceLabel: currency.format(options[i].price),
                          selected: selectedIndex == i,
                          onTap: () => setLocal(() => selectedIndex = i),
                        ),
                      ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                child: Text(
                  '${strings.quantity} (${ProductUnits.normalize(product.productUnit)})',
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: AppColors.navy,
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
                child: Row(
                  children: [
                    QtyBox(
                      label: '−',
                      onTap: qty <= step
                          ? null
                          : () => setLocal(() {
                              qty = double.parse(
                                (qty - step).toStringAsFixed(3),
                              );
                            }),
                    ),
                    Expanded(
                      child: Container(
                        height: 44,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          border: Border.all(color: AppColors.border),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          ProductUnits.formatQty(
                            qty,
                            unit: product.productUnit,
                          ),
                          style: const TextStyle(
                            fontFamily: AppFonts.family,
                            fontSize: 18,
                            fontWeight: FontWeight.w700,
                            color: AppColors.navy,
                          ),
                        ),
                      ),
                    ),
                    QtyBox(
                      label: '+',
                      onTap: () => setLocal(() {
                        qty = double.parse((qty + step).toStringAsFixed(3));
                      }),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              Row(
                children: [
                  Expanded(
                    child: Material(
                      color: AppColors.red,
                      borderRadius: const BorderRadius.only(
                        bottomLeft: Radius.circular(14),
                      ),
                      child: InkWell(
                        onTap: () => Navigator.pop(context, false),
                        borderRadius: const BorderRadius.only(
                          bottomLeft: Radius.circular(14),
                        ),
                        child: SizedBox(
                          height: 46,
                          child: Center(
                            child: Text(
                              strings.dismiss,
                              style: const TextStyle(
                                fontFamily: AppFonts.family,
                                color: Colors.white,
                                fontSize: 15,
                                fontWeight: FontWeight.w700,
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
                      borderRadius: const BorderRadius.only(
                        bottomRight: Radius.circular(14),
                      ),
                      child: InkWell(
                        onTap: () => Navigator.pop(context, true),
                        borderRadius: const BorderRadius.only(
                          bottomRight: Radius.circular(14),
                        ),
                        child: SizedBox(
                          height: 46,
                          child: Center(
                            child: Text(
                              strings.addToCart,
                              style: const TextStyle(
                                fontFamily: AppFonts.family,
                                color: Colors.white,
                                fontSize: 15,
                                fontWeight: FontWeight.w700,
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
    await promptOpenPriceAndAdd(
      context,
      ref,
      product,
      portion: chosen.portion,
      initialQty: qty,
    );
  } else {
    await ref
        .read(posCartControllerProvider.notifier)
        .addProduct(product, portion: chosen.portion, quantity: qty);
  }
}

class _PortionChoiceChip extends StatelessWidget {
  const _PortionChoiceChip({
    required this.label,
    required this.priceLabel,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final String priceLabel;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final bg = selected ? AppColors.primary : Colors.white;
    final border = selected
        ? AppColors.primary
        : AppColors.primary.withValues(alpha: 0.35);
    final titleColor = selected ? Colors.white : AppColors.navy;
    final priceColor = selected
        ? Colors.white.withValues(alpha: 0.95)
        : AppColors.primary;

    return Material(
      color: bg,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Container(
          constraints: const BoxConstraints(minWidth: 108),
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: border, width: 1.5),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                label,
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 15,
                  fontWeight: FontWeight.w800,
                  color: titleColor,
                  height: 1.15,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                priceLabel,
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                  color: priceColor,
                  height: 1.1,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

Future<void> promptOpenPriceAndAdd(
  BuildContext context,
  WidgetRef ref,
  Product product, {
  ProductPortion? portion,
  double initialQty = 1,
}) async {
  final base = portion?.portionPrice ?? product.productPrice;
  final priceCtrl = TextEditingController(
    text: base > 0 ? base.toStringAsFixed(2) : '',
  );
  final qtyCtrl = TextEditingController(
    text: ProductUnits.formatQty(initialQty, unit: product.productUnit),
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
              product.productName,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
            child: AppTextField(
              required: true,
              controller: priceCtrl,
              label: AppStrings.of(ref).productPrice,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              autofocus: true,
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: AppTextField(
              required: true,
              controller: qtyCtrl,
              label: AppStrings.of(ref).quantity,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
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
  final qty = double.tryParse(qtyCtrl.text.trim()) ?? 1;
  priceCtrl.dispose();
  qtyCtrl.dispose();
  if (ok != true || !context.mounted) return;
  if (price <= 0) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).enterValidPrice)));
    return;
  }
  await ref
      .read(posCartControllerProvider.notifier)
      .addProduct(
        product,
        portion: portion,
        unitPriceOverride: price,
        quantity: qty <= 0 ? 1 : qty,
      );
}

/* Edit cart line quantity and/or unit price (Android qty/price dialog). */
Future<void> editCartLineDialog(
  BuildContext context,
  WidgetRef ref,
  CartItem item,
) async {
  final qtyCtrl = TextEditingController(
    text: ProductUnits.formatQty(item.quantity, unit: item.productUnit),
  );
  final priceCtrl = TextEditingController(
    text: amountInputText(item.unitPrice),
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
              required: true,
              controller: priceCtrl,
              label: AppStrings.of(ref).productPrice,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: AppTextField(
              required: true,
              controller: qtyCtrl,
              label: AppStrings.of(ref).quantity,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
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
  final qty = double.tryParse(qtyCtrl.text.trim()) ?? item.quantity;
  final price = double.tryParse(priceCtrl.text.trim());
  qtyCtrl.dispose();
  priceCtrl.dispose();
  if (ok != true || !context.mounted) return;
  await ref
      .read(posCartControllerProvider.notifier)
      .setLine(
        item: item,
        quantity: qty < 0 ? 0 : qty,
        unitPrice: price != null && price > 0 ? price : null,
      );
}

class QtyBox extends StatelessWidget {
  const QtyBox({super.key, required this.label, this.onTap});

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
