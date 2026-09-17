import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/product_units.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';

enum StockMovementMode { purchase, waste }

/* Purchase (stock in) or Waste (stock out) entry page. */
class AddInventoryPage extends ConsumerStatefulWidget {
  const AddInventoryPage({super.key, this.mode = StockMovementMode.purchase});

  final StockMovementMode mode;

  @override
  ConsumerState<AddInventoryPage> createState() => AddInventoryPageState();
}

class AddInventoryPageState extends ConsumerState<AddInventoryPage> {
  final qtyCtrl = TextEditingController(text: '1');
  final noteCtrl = TextEditingController();
  final costCtrl = TextEditingController();
  Product? selected;
  var busy = false;

  bool get isWaste => widget.mode == StockMovementMode.waste;

  @override
  void dispose() {
    qtyCtrl.dispose();
    noteCtrl.dispose();
    costCtrl.dispose();
    super.dispose();
  }

  Future<void> save() async {
    final product = selected;
    final qty = double.tryParse(qtyCtrl.text.trim()) ?? 0;
    if (product == null || qty <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Select a product and quantity')),
      );
      return;
    }
    setState(() => busy = true);
    try {
      final ctrl = ref.read(inventoryControllerProvider.notifier);
      if (isWaste) {
        await ctrl.addWaste(
          productId: product.productId,
          productName: product.productName,
          quantity: qty,
          reason: noteCtrl.text.trim(),
        );
      } else {
        await ctrl.addPurchase(
          productId: product.productId,
          productName: product.productName,
          quantity: qty,
          note: noteCtrl.text.trim(),
          unitCost: double.tryParse(costCtrl.text.trim()) ?? 0,
        );
      }
      if (!mounted) return;
      context.pop();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final products = ref.watch(allProductsProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <Product>[],
        );
    selected ??= products.isEmpty ? null : products.first;
    final balances = ref.watch(stockBalancesProvider);
    double? available;
    if (selected != null) {
      for (final b in balances) {
        if (b.productId == selected!.productId) {
          available = b.remaining;
          break;
        }
      }
    }

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(
        title: Text(isWaste ? 'Waste / Spoilage' : 'Purchase / Stock In'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
        children: [
          MasterCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  isWaste
                      ? 'Record damaged, expired, or wasted stock. Qty is deducted from available balance.'
                      : 'Record purchase or stock received. Qty is added to available balance.',
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 13,
                    color: AppColors.navy.withValues(alpha: .55),
                  ),
                ),
                const SizedBox(height: 14),
                if (products.isEmpty)
                  const Text('Sync Masters products first')
                else
                  AppDropdownFormField<Product>(
                    label: 'Product',
                    items: products,
                    itemLabel: (p) =>
                        '${p.productName}${p.productUnit == null || p.productUnit!.isEmpty ? '' : ' (${ProductUnits.normalize(p.productUnit)})'}',
                    value: selected,
                    enableSearch: true,
                    onChanged: (v) => setState(() => selected = v),
                  ),
                if (available != null) ...[
                  const SizedBox(height: 8),
                  Text(
                    'Current stock: ${ProductUnits.formatQty(available, unit: selected?.productUnit)}',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: available < 6 ? AppColors.orange : AppColors.teal,
                    ),
                  ),
                ],
                const SizedBox(height: 12),
                AppTextField(
                  controller: qtyCtrl,
                  label: isWaste ? 'Waste quantity' : 'Purchase quantity',
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                  ],
                ),
                if (!isWaste) ...[
                  const SizedBox(height: 12),
                  AppTextField(
                    controller: costCtrl,
                    label: 'Unit cost (optional)',
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                    inputFormatters: [
                      FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                    ],
                  ),
                ],
                const SizedBox(height: 12),
                AppTextField(
                  controller: noteCtrl,
                  label: isWaste
                      ? 'Reason (damage / expiry / spoilage)'
                      : 'Supplier / bill no (optional)',
                  textCapitalization: TextCapitalization.sentences,
                ),
                const SizedBox(height: 16),
                MasterPrimaryButton(
                  label: isWaste ? 'Save Waste' : 'Save Purchase',
                  isLoading: busy,
                  onPressed: busy ? null : save,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
