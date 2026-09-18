import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';

/* WithTable AddCombo / UpdateCombo full form. */
class ComboFormPage extends ConsumerStatefulWidget {
  const ComboFormPage({super.key, this.comboId});

  final int? comboId;

  @override
  ConsumerState<ComboFormPage> createState() => ComboFormPageState();
}

class ComboFormPageState extends ConsumerState<ComboFormPage> {
  final comboFormPageCode = TextEditingController();
  final comboFormPageName = TextEditingController();
  final comboFormPagePrice = TextEditingController();
  final cgst = TextEditingController();
  final sgst = TextEditingController();
  final search = TextEditingController();
  final selected = <int, int>{};
  var comboFormPageActive = true;
  var loaded = false;
  var busy = false;
  var query = '';

  bool get isEdit => widget.comboId != null;

  @override
  void dispose() {
    comboFormPageCode.dispose();
    comboFormPageName.dispose();
    comboFormPagePrice.dispose();
    cgst.dispose();
    sgst.dispose();
    search.dispose();
    super.dispose();
  }

  Future<void> hydrate(Combo combo) async {
    if (loaded) return;
    loaded = true;
    comboFormPageCode.text = combo.comboCode ?? '';
    comboFormPageName.text = combo.comboName;
    comboFormPagePrice.text = amountInputText(combo.comboPrice);
    cgst.text = amountInputText(combo.comboCgst);
    sgst.text = amountInputText(combo.comboSgst);
    comboFormPageActive = combo.comboActiveStatus == '1';
    final items = await ref
        .read(appDatabaseProvider)
        .getComboItemsForCombo(combo.comboId);
    if (!mounted) return;
    setState(() {
      for (final item in items) {
        final pid = item.productId;
        if (pid == null) continue;
        selected[pid] = item.comboItemQuantity;
      }
    });
  }

  Future<void> save() async {
    final name = comboFormPageName.text.trim();
    final price = double.tryParse(comboFormPagePrice.text.trim()) ?? 0;
    if (name.isEmpty || price <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter combo name and price')),
      );
      return;
    }
    setState(() => busy = true);
    try {
      final items = selected.entries
          .map((e) => (productId: e.key, quantity: e.value))
          .toList();
      final n = ref.read(mastersSyncControllerProvider.notifier);
      if (isEdit) {
        await n.updateCombo(
          comboId: widget.comboId!,
          name: name,
          price: price,
          comboCode: comboFormPageCode.text.trim(),
          comboCgst: double.tryParse(cgst.text.trim()) ?? 0,
          comboSgst: double.tryParse(sgst.text.trim()) ?? 0,
          activeOnPos: comboFormPageActive,
          items: items,
        );
      } else {
        await n.createCombo(
          name: name,
          price: price,
          comboCode: comboFormPageCode.text.trim(),
          comboCgst: double.tryParse(cgst.text.trim()) ?? 0,
          comboSgst: double.tryParse(sgst.text.trim()) ?? 0,
          activeOnPos: comboFormPageActive,
          items: items,
        );
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(isEdit ? 'Combo saved' : 'Combo saved')),
      );
      context.pop();
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final products = ref
        .watch(allProductsProvider)
        .maybeWhen(data: (v) => v, orElse: () => const <Product>[]);
    if (isEdit) {
      final combos = ref
          .watch(combosListProvider)
          .maybeWhen(data: (v) => v, orElse: () => const <Combo>[]);
      Combo? match;
      for (final c in combos) {
        if (c.comboId == widget.comboId) match = c;
      }
      if (match != null) {
        hydrate(match);
      }
    }

    final filtered = products.where((p) {
      if (query.trim().isEmpty) return true;
      final q = query.trim().toLowerCase();
      return p.productName.toLowerCase().contains(q) ||
          (p.productCode?.toLowerCase().contains(q) ?? false);
    }).toList();

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(title: Text(isEdit ? 'Update Combo' : 'Add Combo')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 32),
        children: [
          AppTextField(
            controller: comboFormPageCode,
            label: 'Combo code',
            textCapitalization: TextCapitalization.characters,
          ),
          const SizedBox(height: 12),
          AppTextField(
            required: true,
            controller: comboFormPageName,
            label: 'Combo name',
          ),
          const SizedBox(height: 12),
          AppTextField(
            required: true,
            controller: comboFormPagePrice,
            label: 'Combo selling price',
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: AppTextField(
                  controller: cgst,
                  label: 'CGST',
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: AppTextField(
                  controller: sgst,
                  label: 'SGST',
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                ),
              ),
            ],
          ),
          AppSwitchTile(
            title: 'Combo active on POS',
            value: comboFormPageActive,
            showDivider: false,
            onChanged: (v) => setState(() => comboFormPageActive = v),
          ),
          const SizedBox(height: 8),
          const Text(
            'Combo items',
            style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16),
          ),
          const SizedBox(height: 8),
          AppTextField(
            controller: search,
            label: 'Search product',
            onChanged: (v) => setState(() => query = v),
          ),
          const SizedBox(height: 8),
          if (selected.isNotEmpty)
            ...selected.entries.map((e) {
              Product? product;
              for (final p in products) {
                if (p.productId == e.key) product = p;
              }
              return ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(product?.productName ?? 'Product ${e.key}'),
                subtitle: Text('Qty: ${e.value}'),
                trailing: IconButton(
                  icon: const Icon(Icons.delete_outline),
                  onPressed: () => setState(() => selected.remove(e.key)),
                ),
              );
            }),
          ...filtered.take(50).map((p) {
            final qty = selected[p.productId] ?? 0;
            return CheckboxListTile(
              dense: true,
              value: qty > 0,
              title: Text(p.productName),
              subtitle: qty > 0 ? Text('Qty: $qty') : null,
              onChanged: (checked) => setState(() {
                if (checked == true) {
                  selected[p.productId] = 1;
                } else {
                  selected.remove(p.productId);
                }
              }),
              secondary: qty > 0
                  ? Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        IconButton(
                          icon: const Icon(Icons.remove),
                          onPressed: () => setState(() {
                            final next = qty - 1;
                            if (next <= 0) {
                              selected.remove(p.productId);
                            } else {
                              selected[p.productId] = next;
                            }
                          }),
                        ),
                        IconButton(
                          icon: const Icon(Icons.add),
                          onPressed: () =>
                              setState(() => selected[p.productId] = qty + 1),
                        ),
                      ],
                    )
                  : null,
            );
          }),
          const SizedBox(height: 16),
          AppButton(
            label: busy ? 'Saving…' : (isEdit ? 'Save Combo' : 'Create Combo'),
            onPressed: busy ? null : save,
          ),
        ],
      ),
    );
  }
}
