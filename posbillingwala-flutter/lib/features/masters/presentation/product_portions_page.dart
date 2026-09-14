import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';

class ProductPortionsPage extends ConsumerStatefulWidget {
  const ProductPortionsPage({super.key, required this.productId});

  final int productId;

  @override
  ConsumerState<ProductPortionsPage> createState() =>
      _ProductPortionsPageState();
}

class _ProductPortionsPageState extends ConsumerState<ProductPortionsPage> {
  final _priceCtrl = TextEditingController();
  final _sortCtrl = TextEditingController(text: '1');
  PortionMaster? _selected;
  bool _busy = false;
  List<ProductPortion> _portions = const [];
  bool _loaded = false;

  @override
  void dispose() {
    _priceCtrl.dispose();
    _sortCtrl.dispose();
    super.dispose();
  }

  Future<void> _reload() async {
    final rows = await ref
        .read(appDatabaseProvider)
        .getPortionsForProduct(widget.productId);
    if (!mounted) return;
    setState(() {
      _portions = rows;
      _loaded = true;
    });
  }

  Future<void> _add() async {
    final masters = ref.read(portionMastersProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <PortionMaster>[],
        );
    final selected = _selected ?? (masters.isNotEmpty ? masters.first : null);
    if (selected == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Add portion masters first')),
      );
      return;
    }
    final price = double.tryParse(_priceCtrl.text.trim());
    if (price == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter portion price')),
      );
      return;
    }
    setState(() => _busy = true);
    try {
      await ref.read(appDatabaseProvider).insertLocalPortion(
            productId: widget.productId,
            portionName: selected.portionName,
            portionPrice: price,
            portionSortOrder: int.tryParse(_sortCtrl.text.trim()) ?? 1,
            portionMasterId: selected.portionMasterId,
          );
      _priceCtrl.clear();
      await _reload();
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _delete(ProductPortion portion) async {
    final ok = await showAppConfirmBottomSheet(
      context: context,
      title: 'Delete portion',
      message: 'Remove ${portion.portionName}?',
      confirmLabel: 'Delete',
      confirmVariant: AppButtonVariant.danger,
    );
    if (!ok) return;
    await ref.read(appDatabaseProvider).softDeletePortion(portion.portionId);
    await _reload();
  }

  @override
  Widget build(BuildContext context) {
    final products = ref.watch(productsProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <Product>[],
        );
    Product? product;
    for (final p in products) {
      if (p.productId == widget.productId) {
        product = p;
        break;
      }
    }
    final masters = ref.watch(portionMastersProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <PortionMaster>[],
        );
    final selected = _selected != null &&
            masters.any((m) => m.portionMasterId == _selected!.portionMasterId)
        ? masters.firstWhere(
            (m) => m.portionMasterId == _selected!.portionMasterId,
          )
        : (masters.isNotEmpty ? masters.first : null);

    if (!_loaded) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted && !_loaded) _reload();
      });
    }

    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final title = product == null
        ? 'Portions'
        : '${product.productName}${product.categoryName == null || product.categoryName!.isEmpty ? '' : ' (${product.categoryName})'}';

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(
        title: const Text('Portions'),
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
          MasterCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w800,
                    fontSize: 16,
                    color: AppColors.navy,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  _portions.isEmpty
                      ? 'No portions — product price is used for billing. Optionally add portions.'
                      : 'Configured portions override base price when selected.',
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 12.5,
                    height: 1.35,
                    color: AppColors.navy.withValues(alpha: .5),
                  ),
                ),
                if (product != null) ...[
                  const SizedBox(height: 8),
                  Text(
                    'Base price: ${currency.format(product.productPrice)}',
                    style: const TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w600,
                      fontSize: 13,
                      color: AppColors.navy,
                    ),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(height: 18),
          MasterSectionLabel(
            'Portion Detail',
            trailing: MasterLinkButton(
              label: 'Manage Portion Master',
              onTap: () => context.push('/masters/portion-masters'),
            ),
          ),
          const SizedBox(height: 10),
          MasterCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Select Portion',
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: AppColors.navy.withValues(alpha: .55),
                  ),
                ),
                const SizedBox(height: 6),
                MasterDropdown<PortionMaster>(
                  value: selected,
                  items: masters,
                  hint: 'Select Portion',
                  itemLabel: (m) => m.portionName,
                  onChanged: (v) => setState(() => _selected = v),
                ),
                const SizedBox(height: 12),
                MasterOutlinedField(
                  controller: _priceCtrl,
                  hint: 'Portion Price',
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                ),
                const SizedBox(height: 12),
                MasterOutlinedField(
                  controller: _sortCtrl,
                  hint: 'Sort Order',
                  keyboardType: TextInputType.number,
                ),
                const SizedBox(height: 12),
                MasterPrimaryButton(
                  label: 'Add Portion',
                  isLoading: _busy,
                  onPressed: _busy ? null : _add,
                ),
              ],
            ),
          ),
          const SizedBox(height: 18),
          const MasterSectionLabel('Portion List'),
          const SizedBox(height: 10),
          MasterCard(
            padding: EdgeInsets.zero,
            child: !_loaded
                ? const Padding(
                    padding: EdgeInsets.all(24),
                    child: Center(child: CircularProgressIndicator()),
                  )
                : _portions.isEmpty
                    ? const MasterEmptyState(
                        title: 'No data found',
                        subtitle: 'Add portion sizes like Half / Full.',
                      )
                    : Column(
                        children: [
                          for (var i = 0; i < _portions.length; i++)
                            MasterListRow(
                              index: i + 1,
                              title: _portions[i].portionName,
                              subtitle: currency
                                  .format(_portions[i].portionPrice),
                              onEdit: () {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(
                                    content: Text(
                                      'Delete and re-add to change a portion',
                                    ),
                                  ),
                                );
                              },
                              onDelete: () => _delete(_portions[i]),
                              showDivider: i < _portions.length - 1,
                            ),
                        ],
                      ),
          ),
        ],
      ),
      ),
    );
  }
}
