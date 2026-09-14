import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';

const _productUnits = [
  'Pcs',
  'Kg',
  'Plate',
  'Glass',
  'Bowl',
  'Packet',
  'Litre',
  'Dozen',
  'GRAM',
];

class ProductFormPage extends ConsumerStatefulWidget {
  const ProductFormPage({super.key, this.productId});

  final int? productId;

  @override
  ConsumerState<ProductFormPage> createState() => _ProductFormPageState();
}

class _ProductFormPageState extends ConsumerState<ProductFormPage> {
  final _codeCtrl = TextEditingController();
  final _nameCtrl = TextEditingController();
  final _priceCtrl = TextEditingController();
  final _cgstCtrl = TextEditingController(text: '0');
  final _sgstCtrl = TextEditingController(text: '0');
  final _portionPriceCtrl = TextEditingController();

  ProductCategory? _category;
  ProductSubcategory? _subcategory;
  PortionMaster? _portionMaster;
  String _unit = _productUnits.first;
  bool _openPrice = false;
  bool _busy = false;
  bool _loaded = false;
  final _inlinePortions = <({PortionMaster master, double price})>[];

  bool get _isEdit => widget.productId != null;

  @override
  void dispose() {
    _codeCtrl.dispose();
    _nameCtrl.dispose();
    _priceCtrl.dispose();
    _cgstCtrl.dispose();
    _sgstCtrl.dispose();
    _portionPriceCtrl.dispose();
    super.dispose();
  }

  void _hydrate(Product product, List<ProductCategory> categories,
      List<ProductSubcategory> subs) {
    if (_loaded) return;
    _loaded = true;
    _codeCtrl.text = product.productCode ?? '';
    _nameCtrl.text = product.productName;
    _priceCtrl.text = product.productPrice.toStringAsFixed(
      product.productPrice % 1 == 0 ? 0 : 2,
    );
    _cgstCtrl.text = product.productCgst.toStringAsFixed(
      product.productCgst % 1 == 0 ? 0 : 1,
    );
    _sgstCtrl.text = product.productSgst.toStringAsFixed(
      product.productSgst % 1 == 0 ? 0 : 1,
    );
    _openPrice = product.openPrice == '1';
    _unit = (product.productUnit ?? '').trim().isEmpty
        ? _productUnits.first
        : product.productUnit!;
    if (!_productUnits.contains(_unit)) {
      _unit = _productUnits.first;
    }
    for (final c in categories) {
      if (c.categoryId == product.categoryId) {
        _category = c;
        break;
      }
    }
    for (final s in subs) {
      if (s.subcategoryId == product.subcategoryId) {
        _subcategory = s;
        break;
      }
    }
  }

  Future<void> _addInlinePortion() async {
    final master = _portionMaster;
    if (master == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Select a portion')),
      );
      return;
    }
    final price = double.tryParse(_portionPriceCtrl.text.trim());
    if (price == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter selling price for portion')),
      );
      return;
    }
    setState(() {
      _inlinePortions.removeWhere(
        (e) => e.master.portionMasterId == master.portionMasterId,
      );
      _inlinePortions.add((master: master, price: price));
      _portionPriceCtrl.clear();
    });
  }

  Future<void> _save() async {
    final name = _nameCtrl.text.trim();
    if (name.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Product name is required')),
      );
      return;
    }
    if (_category == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Select a category')),
      );
      return;
    }
    final price = _openPrice
        ? 0.0
        : (double.tryParse(_priceCtrl.text.trim()) ?? -1);
    if (!_openPrice && price < 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter a valid product price')),
      );
      return;
    }

    setState(() => _busy = true);
    try {
      final sync = ref.read(mastersSyncControllerProvider.notifier);
      if (_isEdit) {
        await sync.updateProduct(
          productId: widget.productId!,
          name: name,
          price: price,
          categoryId: _category!.categoryId,
          categoryName: _category!.categoryName,
          productCode: _codeCtrl.text.trim().isEmpty
              ? null
              : _codeCtrl.text.trim(),
          openPrice: _openPrice ? '1' : '0',
          productUnit: _unit,
          productCgst: double.tryParse(_cgstCtrl.text.trim()) ?? 0,
          productSgst: double.tryParse(_sgstCtrl.text.trim()) ?? 0,
          subcategoryId: _subcategory?.subcategoryId,
        );
      } else {
        final id = await sync.createProduct(
          name: name,
          price: price,
          categoryId: _category!.categoryId,
          categoryName: _category!.categoryName,
          productCode: _codeCtrl.text.trim().isEmpty
              ? null
              : _codeCtrl.text.trim(),
          openPrice: _openPrice ? '1' : '0',
          productUnit: _unit,
          productCgst: double.tryParse(_cgstCtrl.text.trim()) ?? 0,
          productSgst: double.tryParse(_sgstCtrl.text.trim()) ?? 0,
          subcategoryId: _subcategory?.subcategoryId,
        );
        final db = ref.read(appDatabaseProvider);
        var sort = 1;
        for (final portion in _inlinePortions) {
          await db.insertLocalPortion(
            productId: id,
            portionName: portion.master.portionName,
            portionPrice: portion.price,
            portionSortOrder: sort++,
            portionMasterId: portion.master.portionMasterId,
          );
        }
      }
      ref.invalidate(catalogCountsProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(_isEdit ? 'Product updated' : 'Product saved'),
        ),
      );
      context.pop();
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final categories = ref.watch(categoriesProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <ProductCategory>[],
        );
    final allSubs = ref.watch(subcategoriesProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <ProductSubcategory>[],
        );
    final portionMasters = ref.watch(portionMastersProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <PortionMaster>[],
        );

    if (_isEdit) {
      final products = ref.watch(productsProvider).maybeWhen(
            data: (v) => v,
            orElse: () => const <Product>[],
          );
      Product? existing;
      for (final p in products) {
        if (p.productId == widget.productId) {
          existing = p;
          break;
        }
      }
      if (existing != null) {
        _hydrate(existing, categories, allSubs);
      }
    } else if (_category == null && categories.isNotEmpty) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted || _category != null) return;
        setState(() {
          _category = categories.first;
          _portionMaster =
              portionMasters.isNotEmpty ? portionMasters.first : null;
        });
      });
    }

    final category = _category != null &&
            categories.any((c) => c.categoryId == _category!.categoryId)
        ? categories.firstWhere((c) => c.categoryId == _category!.categoryId)
        : (categories.isNotEmpty ? categories.first : null);
    final subs = category == null
        ? const <ProductSubcategory>[]
        : allSubs.where((s) => s.categoryId == category.categoryId).toList();
    final subcategory = _subcategory != null &&
            subs.any((s) => s.subcategoryId == _subcategory!.subcategoryId)
        ? subs.firstWhere((s) => s.subcategoryId == _subcategory!.subcategoryId)
        : null;
    final portionMaster = _portionMaster != null &&
            portionMasters.any(
              (m) => m.portionMasterId == _portionMaster!.portionMasterId,
            )
        ? portionMasters.firstWhere(
            (m) => m.portionMasterId == _portionMaster!.portionMasterId,
          )
        : (portionMasters.isNotEmpty ? portionMasters.first : null);

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(
        title: Text(_isEdit ? 'Edit Product' : 'Add Product'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => context.pop(),
        ),
      ),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
        padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            16,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            28),
        children: [
          const MasterSectionLabel('Product Category*'),
          const SizedBox(height: 10),
          MasterCard(
            child: Column(
              children: [
                MasterDropdown<ProductCategory>(
                  value: category,
                  items: categories,
                  hint: 'Select category',
                  itemLabel: (c) => c.categoryName,
                  onChanged: (v) => setState(() {
                    _category = v;
                    _subcategory = null;
                  }),
                ),
                const SizedBox(height: 12),
                MasterDropdown<ProductSubcategory>(
                  value: subcategory,
                  items: subs,
                  hint: 'None',
                  itemLabel: (s) => s.subcategoryName,
                  onChanged: (v) => setState(() => _subcategory = v),
                ),
              ],
            ),
          ),
          if (!_isEdit) ...[
            const SizedBox(height: 18),
            MasterSectionLabel(
              'Portions (Optional)',
              trailing: MasterLinkButton(
                label: 'Manage Portion Master',
                onTap: () => context.push('/masters/portion-masters'),
              ),
            ),
            const SizedBox(height: 6),
            Text(
              'Product price is used when no portions are added.',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 12,
                color: AppColors.navy.withValues(alpha: .45),
              ),
            ),
            const SizedBox(height: 10),
            MasterCard(
              child: Column(
                children: [
                  MasterDropdown<PortionMaster>(
                    value: portionMaster,
                    items: portionMasters,
                    hint: 'Select portion',
                    itemLabel: (m) => m.portionName,
                    onChanged: (v) => setState(() => _portionMaster = v),
                  ),
                  const SizedBox(height: 12),
                  MasterOutlinedField(
                    controller: _portionPriceCtrl,
                    hint: 'Selling price for this product + portion',
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                  ),
                  const SizedBox(height: 12),
                  MasterPrimaryButton(
                    label: 'Add Portion to Product',
                    onPressed: _addInlinePortion,
                  ),
                  if (_inlinePortions.isNotEmpty) ...[
                    const SizedBox(height: 12),
                    for (final p in _inlinePortions)
                      ListTile(
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                        title: Text(p.master.portionName),
                        subtitle: Text('₹ ${p.price.toStringAsFixed(2)}'),
                        trailing: IconButton(
                          icon: const Icon(
                            Icons.close_rounded,
                            color: AppColors.red,
                          ),
                          onPressed: () => setState(
                            () => _inlinePortions.remove(p),
                          ),
                        ),
                      ),
                  ],
                ],
              ),
            ),
          ] else ...[
            const SizedBox(height: 14),
            MasterPrimaryButton(
              label: 'Manage Portions',
              onPressed: () => context.push(
                '/masters/products/portions?id=${widget.productId}',
              ),
            ),
          ],
          const SizedBox(height: 18),
          const MasterSectionLabel('Product Details'),
          const SizedBox(height: 10),
          MasterCard(
            child: Column(
              children: [
                MasterOutlinedField(
                  controller: _codeCtrl,
                  hint: 'Product Code',
                  textCapitalization: TextCapitalization.characters,
                ),
                const SizedBox(height: 12),
                MasterOutlinedField(
                  controller: _nameCtrl,
                  hint: 'Product Name',
                ),
                const SizedBox(height: 8),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text(
                    'Open Price',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w600,
                      fontSize: 14.5,
                      color: AppColors.navy,
                    ),
                  ),
                  subtitle: Text(
                    'Price is entered while billing (tap amount on cart)',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontSize: 12,
                      color: AppColors.navy.withValues(alpha: .45),
                    ),
                  ),
                  value: _openPrice,
                  activeThumbColor: AppColors.primary,
                  onChanged: (v) => setState(() => _openPrice = v),
                ),
                if (!_openPrice) ...[
                  const SizedBox(height: 4),
                  MasterOutlinedField(
                    controller: _priceCtrl,
                    hint: 'Product Price (without GST)',
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                  ),
                ],
                const SizedBox(height: 12),
                MasterDropdown<String>(
                  value: _unit,
                  items: _productUnits,
                  itemLabel: (u) => u,
                  onChanged: (v) {
                    if (v != null) setState(() => _unit = v);
                  },
                ),
              ],
            ),
          ),
          const SizedBox(height: 18),
          const MasterSectionLabel('Tax / GST'),
          const SizedBox(height: 10),
          MasterCard(
            child: Row(
              children: [
                Expanded(
                  child: MasterOutlinedField(
                    controller: _cgstCtrl,
                    hint: 'Product CGST',
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: MasterOutlinedField(
                    controller: _sgstCtrl,
                    hint: 'Product SGST',
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 22),
          MasterPrimaryButton(
            label: _isEdit ? 'Save Product' : 'Add Product',
            isLoading: _busy,
            onPressed: _busy ? null : _save,
          ),
        ],
      ),
      ),
    );
  }
}
