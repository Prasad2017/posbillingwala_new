import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';

/// WithTable AddCombo / UpdateCombo full form.
class ComboFormPage extends ConsumerStatefulWidget {
  const ComboFormPage({super.key, this.comboId});

  final int? comboId;

  @override
  ConsumerState<ComboFormPage> createState() => _ComboFormPageState();
}

class _ComboFormPageState extends ConsumerState<ComboFormPage> {
  final _code = TextEditingController();
  final _name = TextEditingController();
  final _price = TextEditingController();
  final _cgst = TextEditingController(text: '0');
  final _sgst = TextEditingController(text: '0');
  final _search = TextEditingController();
  final _selected = <int, int>{};
  var _active = true;
  var _loaded = false;
  var _busy = false;
  var _query = '';

  bool get _isEdit => widget.comboId != null;

  @override
  void dispose() {
    _code.dispose();
    _name.dispose();
    _price.dispose();
    _cgst.dispose();
    _sgst.dispose();
    _search.dispose();
    super.dispose();
  }

  Future<void> _hydrate(Combo combo) async {
    if (_loaded) return;
    _loaded = true;
    _code.text = combo.comboCode ?? '';
    _name.text = combo.comboName;
    _price.text = combo.comboPrice.toStringAsFixed(2);
    _cgst.text = combo.comboCgst.toStringAsFixed(1);
    _sgst.text = combo.comboSgst.toStringAsFixed(1);
    _active = combo.comboActiveStatus == '1';
    final items =
        await ref.read(appDatabaseProvider).getComboItemsForCombo(combo.comboId);
    if (!mounted) return;
    setState(() {
      for (final item in items) {
        final pid = item.productId;
        if (pid == null) continue;
        _selected[pid] = item.comboItemQuantity;
      }
    });
  }

  Future<void> _save() async {
    final name = _name.text.trim();
    final price = double.tryParse(_price.text.trim()) ?? 0;
    if (name.isEmpty || price <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter combo name and price')),
      );
      return;
    }
    setState(() => _busy = true);
    try {
      final items = _selected.entries
          .map((e) => (productId: e.key, quantity: e.value))
          .toList();
      final n = ref.read(mastersSyncControllerProvider.notifier);
      if (_isEdit) {
        await n.updateCombo(
          comboId: widget.comboId!,
          name: name,
          price: price,
          comboCode: _code.text.trim(),
          comboCgst: double.tryParse(_cgst.text.trim()) ?? 0,
          comboSgst: double.tryParse(_sgst.text.trim()) ?? 0,
          activeOnPos: _active,
          items: items,
        );
      } else {
        await n.createCombo(
          name: name,
          price: price,
          comboCode: _code.text.trim(),
          comboCgst: double.tryParse(_cgst.text.trim()) ?? 0,
          comboSgst: double.tryParse(_sgst.text.trim()) ?? 0,
          activeOnPos: _active,
          items: items,
        );
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(_isEdit ? 'Combo updated' : 'Combo saved')),
      );
      context.pop();
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final products = ref.watch(allProductsProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <Product>[],
        );
    if (_isEdit) {
      final combos = ref.watch(combosListProvider).maybeWhen(
            data: (v) => v,
            orElse: () => const <Combo>[],
          );
      Combo? match;
      for (final c in combos) {
        if (c.comboId == widget.comboId) match = c;
      }
      if (match != null) {
        _hydrate(match);
      }
    }

    final filtered = products.where((p) {
      if (_query.trim().isEmpty) return true;
      final q = _query.trim().toLowerCase();
      return p.productName.toLowerCase().contains(q) ||
          (p.productCode?.toLowerCase().contains(q) ?? false);
    }).toList();

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(
        title: Text(_isEdit ? 'Update Combo' : 'Add Combo'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 32),
        children: [
          AppTextField(
            controller: _code,
            label: 'Combo code',
            textCapitalization: TextCapitalization.characters,
          ),
          const SizedBox(height: 12),
          AppTextField(controller: _name, label: 'Combo name'),
          const SizedBox(height: 12),
          AppTextField(
            controller: _price,
            label: 'Combo selling price',
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: AppTextField(
                  controller: _cgst,
                  label: 'CGST',
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: AppTextField(
                  controller: _sgst,
                  label: 'SGST',
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                ),
              ),
            ],
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Combo active on POS'),
            value: _active,
            onChanged: (v) => setState(() => _active = v),
          ),
          const SizedBox(height: 8),
          const Text(
            'Combo items',
            style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16),
          ),
          const SizedBox(height: 8),
          AppTextField(
            controller: _search,
            label: 'Search product',
            onChanged: (v) => setState(() => _query = v),
          ),
          const SizedBox(height: 8),
          if (_selected.isNotEmpty)
            ..._selected.entries.map((e) {
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
                  onPressed: () => setState(() => _selected.remove(e.key)),
                ),
              );
            }),
          ...filtered.take(50).map((p) {
            final qty = _selected[p.productId] ?? 0;
            return CheckboxListTile(
              dense: true,
              value: qty > 0,
              title: Text(p.productName),
              subtitle: qty > 0 ? Text('Qty: $qty') : null,
              onChanged: (checked) => setState(() {
                if (checked == true) {
                  _selected[p.productId] = 1;
                } else {
                  _selected.remove(p.productId);
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
                              _selected.remove(p.productId);
                            } else {
                              _selected[p.productId] = next;
                            }
                          }),
                        ),
                        IconButton(
                          icon: const Icon(Icons.add),
                          onPressed: () => setState(
                            () => _selected[p.productId] = qty + 1,
                          ),
                        ),
                      ],
                    )
                  : null,
            );
          }),
          const SizedBox(height: 16),
          AppButton(
            label: _busy
                ? 'Saving…'
                : (_isEdit ? 'Save Combo' : 'Create Combo'),
            onPressed: _busy ? null : _save,
          ),
        ],
      ),
    );
  }
}
