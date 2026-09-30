import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/app_button.dart';
import 'package:pos_billingwala_v2/core/widgets/dropdown/app_dropdown_form_field.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/product_units.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

const _lowStockBelow = 6.0;

enum _StockReason { purchase, damage, adjustment, other }

class InventoryPage extends ConsumerStatefulWidget {
  const InventoryPage({super.key});

  @override
  ConsumerState<InventoryPage> createState() => _InventoryPageState();
}

class _InventoryPageState extends ConsumerState<InventoryPage> {
  final searchCtrl = TextEditingController();
  final qtyCtrl = TextEditingController();
  final costCtrl = TextEditingController();
  String query = '';
  int? selectedId;
  bool formOpen = false;
  bool adding = true;
  bool busy = false;
  _StockReason reason = _StockReason.purchase;

  @override
  void initState() {
    super.initState();
    Future.microtask(() async {
      try {
        await ref.read(appDatabaseProvider).backfillInventoryProductNames();
      } catch (_) {}
      if (!AppPlatform.requiresNetwork) return;
      try {
        await ref.read(inventoryControllerProvider.notifier).syncAll();
      } catch (_) {}
    });
  }

  @override
  void dispose() {
    searchCtrl.dispose();
    qtyCtrl.dispose();
    costCtrl.dispose();
    super.dispose();
  }

  void openForm({required bool add, int? productId}) {
    setState(() {
      formOpen = true;
      adding = add;
      if (productId != null) selectedId = productId;
      reason = add ? _StockReason.purchase : _StockReason.damage;
      qtyCtrl.clear();
    });
  }

  Future<void> save(List<_StockLine> lines) async {
    final line = _lineFor(lines, selectedId);
    final qty = double.tryParse(qtyCtrl.text.trim()) ?? 0;
    if (line == null || qty <= 0) {
      _toast('Select an item and quantity');
      return;
    }
    final increase = reason == _StockReason.purchase;
    if (!increase && qty > line.stock) {
      _toast('Quantity is more than current stock');
      return;
    }
    setState(() => busy = true);
    try {
      final ctrl = ref.read(inventoryControllerProvider.notifier);
      if (increase) {
        await ctrl.addPurchase(
          productId: line.productId,
          productName: line.name,
          quantity: qty,
          note: 'Purchase',
          unitCost: double.tryParse(costCtrl.text.trim()) ?? 0,
        );
      } else {
        await ctrl.addWaste(
          productId: line.productId,
          productName: line.name,
          quantity: qty,
          reason: _reasonLabel(reason),
        );
      }
      if (!mounted) return;
      final result = ref.read(inventoryControllerProvider);
      if (result.hasError) {
        _toast('${result.error}');
        return;
      }
      qtyCtrl.clear();
      _toast('Stock saved');
    } catch (e) {
      if (!mounted) return;
      _toast('$e');
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  void _toast(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final products = ref
        .watch(allProductsProvider)
        .maybeWhen(data: (rows) => rows, orElse: () => const <Product>[]);
    final balances = ref.watch(stockBalancesProvider);
    final movements = ref
        .watch(inventoryMovementsProvider)
        .maybeWhen(data: (rows) => rows, orElse: () => const <InventoryMovement>[]);
    final lines = _buildLines(products, balances, movements);
    final q = query.trim().toLowerCase();
    final shown = q.isEmpty
        ? lines
        : lines.where((line) {
            return line.name.toLowerCase().contains(q) ||
                line.code.toLowerCase().contains(q);
          }).toList();
    final selected = _lineFor(lines, selectedId) ??
        (lines.isEmpty ? null : lines.first);
    if (selected != null && selectedId != selected.productId) {
      selectedId = selected.productId;
    }
    final wide = MediaQuery.sizeOf(context).width >= 640;
    final reasons = adding
        ? _StockReason.values
        : const [
            _StockReason.damage,
            _StockReason.adjustment,
            _StockReason.other,
          ];
    if (!reasons.contains(reason)) {
      reason = reasons.first;
    }

    final table = Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 10, 12, 8),
          child: TextField(
            controller: searchCtrl,
            onChanged: (value) => setState(() => query = value),
            style: const TextStyle(
              fontFamily: AppFonts.family,
              fontSize: 14,
              color: AppColors.navy,
            ),
            decoration: InputDecoration(
              isDense: true,
              hintText: 'Search Item / Barcode / Item Code',
              hintStyle: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 13,
                color: AppColors.navy.withValues(alpha: .4),
              ),
              prefixIcon: const Icon(Icons.search_rounded, size: 20),
              filled: true,
              fillColor: Colors.white,
              contentPadding: const EdgeInsets.symmetric(vertical: 10),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
            ),
          ),
        ),
        const _Header(),
        Expanded(
          child: shown.isEmpty
              ? Center(
                  child: Text(
                    q.isEmpty
                        ? 'No items yet. Add stock to start.'
                        : 'No matching items.',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      color: AppColors.navy.withValues(alpha: .5),
                    ),
                  ),
                )
              : ListView.builder(
                  itemCount: shown.length,
                  itemBuilder: (context, index) {
                    final line = shown[index];
                    return _StockRow(
                      index: index + 1,
                      line: line,
                      selected: line.productId == selected?.productId,
                      onTap: () => openForm(add: formOpen ? adding : true, productId: line.productId),
                    );
                  },
                ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
          child: Row(
            children: [
              Expanded(
                child: AppButton(
                  label: '+ Add Stock',
                  expanded: true,
                  onPressed: () => openForm(add: true, productId: selected?.productId),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: AppButton(
                  label: 'Stock Adjustment',
                  expanded: true,
                  variant: AppButtonVariant.outlined,
                  onPressed: () => openForm(add: false, productId: selected?.productId),
                ),
              ),
            ],
          ),
        ),
      ],
    );

    final form = !formOpen || selected == null
        ? null
        : _StockForm(
            products: products,
            selected: _productFor(products, selected.productId),
            stockLabel: ProductUnits.formatQty(selected.stock, unit: selected.unit),
            adding: adding,
            reasons: reasons,
            reason: reason,
            qtyCtrl: qtyCtrl,
            costCtrl: costCtrl,
            busy: busy,
            onProduct: (product) => setState(() => selectedId = product?.productId),
            onReason: (value) {
              if (value == null) return;
              setState(() {
                reason = value;
                adding = value == _StockReason.purchase;
              });
            },
            onSave: () => save(lines),
          );

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(title: Text(AppStrings.of(ref).inventory)),
      body: wide && form != null
          ? Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(child: table),
                const VerticalDivider(width: 1),
                SizedBox(
                  width: 320,
                  child: SingleChildScrollView(child: form),
                ),
              ],
            )
          : Column(
              children: [
                Expanded(child: table),
                if (form != null)
                  ConstrainedBox(
                    constraints: BoxConstraints(
                      maxHeight: MediaQuery.sizeOf(context).height * 0.46,
                    ),
                    child: SingleChildScrollView(child: form),
                  ),
              ],
            ),
    );
  }
}

class _StockLine {
  const _StockLine({
    required this.productId,
    required this.name,
    required this.code,
    required this.stock,
    required this.sellPrice,
    required this.purchasePrice,
    required this.unit,
  });

  final int productId;
  final String name;
  final String code;
  final double stock;
  final double sellPrice;
  final double purchasePrice;
  final String? unit;

  String get status {
    if (stock <= 0) return 'Out';
    if (stock < _lowStockBelow) return 'Low';
    return 'In Stock';
  }
}

List<_StockLine> _buildLines(
  List<Product> products,
  List<ProductStockBalance> balances,
  List<InventoryMovement> movements,
) {
  final stockById = {for (final row in balances) row.productId: row.remaining};
  final purchaseById = <int, double>{};
  for (final row in movements) {
    if (row.movementType == 'purchase' &&
        row.unitCost > 0 &&
        !purchaseById.containsKey(row.productId)) {
      purchaseById[row.productId] = row.unitCost;
    }
  }
  final lines = <_StockLine>[
    for (final product in products)
      _StockLine(
        productId: product.productId,
        name: product.productName.trim().isEmpty
            ? 'Product ${product.productId}'
            : product.productName.trim(),
        code: (product.productCode ?? '').trim(),
        stock: stockById[product.productId] ?? 0,
        sellPrice: product.productPrice,
        purchasePrice: purchaseById[product.productId] ?? 0,
        unit: product.productUnit,
      ),
  ]..sort((a, b) => a.name.toLowerCase().compareTo(b.name.toLowerCase()));
  return lines;
}

_StockLine? _lineFor(List<_StockLine> lines, int? id) {
  for (final line in lines) {
    if (line.productId == id) return line;
  }
  return null;
}

Product? _productFor(List<Product> products, int id) {
  for (final product in products) {
    if (product.productId == id) return product;
  }
  return products.isEmpty ? null : products.first;
}

String _reasonLabel(_StockReason reason) {
  switch (reason) {
    case _StockReason.purchase:
      return 'Purchase';
    case _StockReason.damage:
      return 'Damage';
    case _StockReason.adjustment:
      return 'Adjustment';
    case _StockReason.other:
      return 'Other';
  }
}

String _money(double value) {
  if (value == value.roundToDouble()) return '₹${value.round()}';
  return '₹${value.toStringAsFixed(2)}';
}

class _Header extends StatelessWidget {
  const _Header();

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.primaryLight,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          const SizedBox(width: 28, child: _Head('No')),
          const Expanded(child: _Head('Item Name')),
          const SizedBox(width: 64, child: _Head('Code', align: TextAlign.center)),
          const SizedBox(width: 48, child: _Head('Stock', align: TextAlign.center)),
          const SizedBox(width: 64, child: _Head('Price', align: TextAlign.end)),
          const SizedBox(width: 68, child: _Head('Status', align: TextAlign.center)),
        ],
      ),
    );
  }
}

class _Head extends StatelessWidget {
  const _Head(this.label, {this.align = TextAlign.start});

  final String label;
  final TextAlign align;

  @override
  Widget build(BuildContext context) {
    return Text(
      label,
      textAlign: align,
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
      style: const TextStyle(
        fontFamily: AppFonts.family,
        fontSize: 11,
        fontWeight: FontWeight.w700,
        color: AppColors.primary,
      ),
    );
  }
}

class _StockRow extends StatelessWidget {
  const _StockRow({
    required this.index,
    required this.line,
    required this.selected,
    required this.onTap,
  });

  final int index;
  final _StockLine line;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final statusColor = line.stock <= 0
        ? AppColors.danger
        : line.stock < _lowStockBelow
            ? AppColors.tableBillRequested
            : AppColors.tableAvailable;
    return InkWell(
      onTap: onTap,
      child: Container(
        color: selected ? AppColors.primaryLight : Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
        child: Row(
          children: [
            SizedBox(
              width: 28,
              child: Text(
                '$index',
                textAlign: TextAlign.center,
                style: const TextStyle(fontFamily: AppFonts.family, fontSize: 12),
              ),
            ),
            Expanded(
              child: Text(
                line.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color: AppColors.navy,
                ),
              ),
            ),
            SizedBox(
              width: 64,
              child: Text(
                line.code.isEmpty ? '—' : line.code,
                textAlign: TextAlign.center,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontFamily: AppFonts.family, fontSize: 12),
              ),
            ),
            SizedBox(
              width: 48,
              child: Text(
                ProductUnits.formatQty(line.stock, unit: line.unit),
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
            SizedBox(
              width: 64,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    _money(line.sellPrice),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontFamily: AppFonts.family, fontSize: 12),
                  ),
                  if (line.purchasePrice > 0)
                    Text(
                      'Buy ${_money(line.purchasePrice)}',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontSize: 10,
                        color: AppColors.navy.withValues(alpha: .45),
                      ),
                    ),
                ],
              ),
            ),
            SizedBox(
              width: 68,
              child: Text(
                line.status,
                textAlign: TextAlign.center,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  color: statusColor,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StockForm extends StatelessWidget {
  const _StockForm({
    required this.products,
    required this.selected,
    required this.stockLabel,
    required this.adding,
    required this.reasons,
    required this.reason,
    required this.qtyCtrl,
    required this.costCtrl,
    required this.busy,
    required this.onProduct,
    required this.onReason,
    required this.onSave,
  });

  final List<Product> products;
  final Product? selected;
  final String stockLabel;
  final bool adding;
  final List<_StockReason> reasons;
  final _StockReason reason;
  final TextEditingController qtyCtrl;
  final TextEditingController costCtrl;
  final bool busy;
  final ValueChanged<Product?> onProduct;
  final ValueChanged<_StockReason?> onReason;
  final VoidCallback onSave;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (products.isEmpty)
              const Text('Add products in Masters first')
            else
              AppDropdownFormField<Product>(
                label: 'Select Item',
                items: products,
                enableSearch: true,
                itemComparer: (a, b) => a.productId == b.productId,
                itemLabel: (p) {
                  final code = (p.productCode ?? '').trim();
                  return code.isEmpty ? p.productName : '${p.productName} ($code)';
                },
                value: selected,
                onChanged: onProduct,
              ),
            const SizedBox(height: 6),
            Text(
              'Current Stock : $stockLabel',
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w700,
                color: AppColors.navy,
              ),
            ),
            const SizedBox(height: 6),
            TextField(
              controller: qtyCtrl,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.]'))],
              decoration: const InputDecoration(
                isDense: true,
                labelText: 'Add / Remove Qty',
                border: OutlineInputBorder(),
              ),
            ),
            if (adding) ...[
              const SizedBox(height: 6),
              TextField(
                controller: costCtrl,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.]'))],
                decoration: const InputDecoration(
                  isDense: true,
                  labelText: 'Purchase price (optional)',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
            const SizedBox(height: 6),
            DropdownButtonFormField<_StockReason>(
              key: ValueKey('${adding}_${reason.name}'),
              initialValue: reason,
              decoration: const InputDecoration(
                isDense: true,
                labelText: 'Reason',
                border: OutlineInputBorder(),
              ),
              items: [
                for (final item in reasons)
                  DropdownMenuItem(value: item, child: Text(_reasonLabel(item))),
              ],
              onChanged: onReason,
            ),
            const SizedBox(height: 8),
            AppButton(
              label: 'Save Stock',
              isLoading: busy,
              onPressed: busy ? null : onSave,
            ),
          ],
        ),
      ),
    );
  }
}
