import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

/// WithTable `AddInventory` full page (not only a dialog).
class AddInventoryPage extends ConsumerStatefulWidget {
  const AddInventoryPage({super.key});

  @override
  ConsumerState<AddInventoryPage> createState() => AddInventoryPageState();
}

class AddInventoryPageState extends ConsumerState<AddInventoryPage> {
  final qtyCtrl = TextEditingController(text: '1');
  Product? addInventoryPageSelected;
  var busy = false;

  @override
  void dispose() {
    qtyCtrl.dispose();
    super.dispose();
  }

  Future<void> save() async {
    final selected = addInventoryPageSelected;
    final qty = double.tryParse(qtyCtrl.text.trim()) ?? 0;
    if (selected == null || qty <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Select a product and quantity')),
      );
      return;
    }
    setState(() => busy = true);
    try {
      await ref.read(inventoryControllerProvider.notifier).addStock(
            productId: selected.productId,
            productName: selected.productName,
            quantity: qty,
          );
      if (!mounted) return;
      context.pop();
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
    addInventoryPageSelected ??= products.isEmpty ? null : products.first;

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(title: Text(AppStrings.of(ref).addInventory)),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
        children: [
          MasterCard(
            child: Column(
              children: [
                if (products.isEmpty)
                  const Text('Sync Masters products first')
                else
                  AppDropdownFormField<Product>(
                    label: 'Product',
                    items: products,
                    itemLabel: (p) => p.productName,
                    value: addInventoryPageSelected,
                    enableSearch: true,
                    onChanged: (v) => setState(() => addInventoryPageSelected = v),
                  ),
                const SizedBox(height: 12),
                AppTextField(
                  controller: qtyCtrl,
                  label: 'Quantity',
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                  ],
                ),
                const SizedBox(height: 16),
                MasterPrimaryButton(
                  label: 'Add Inventory',
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
