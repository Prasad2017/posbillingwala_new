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
      ProductPortionsPageState();
}

class ProductPortionsPageState extends ConsumerState<ProductPortionsPage> {
  final priceCtrl = TextEditingController();
  final sortCtrl = TextEditingController(text: '1');
  PortionMaster? productPortionsPageSelected;
  bool busy = false;
  List<ProductPortion> productPortionsPagePortions = const [];
  bool loaded = false;

  @override
  void dispose() {
    priceCtrl.dispose();
    sortCtrl.dispose();
    super.dispose();
  }

  Future<void> reload() async {
    final rows = await ref
        .read(appDatabaseProvider)
        .getPortionsForProduct(widget.productId);
    if (!mounted) return;
    setState(() {
      productPortionsPagePortions = rows;
      loaded = true;
    });
  }

  Future<void> productPortionsPageAdd() async {
    final masters = ref.read(portionMastersProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <PortionMaster>[],
        );
    final selected = productPortionsPageSelected ?? (masters.isNotEmpty ? masters.first : null);
    if (selected == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Add portion masters first')),
      );
      return;
    }
    final price = double.tryParse(priceCtrl.text.trim());
    if (price == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter portion price')),
      );
      return;
    }
    setState(() => busy = true);
    try {
      await ref.read(appDatabaseProvider).insertLocalPortion(
            productId: widget.productId,
            portionName: selected.portionName,
            portionPrice: price,
            portionSortOrder: int.tryParse(sortCtrl.text.trim()) ?? 1,
            portionMasterId: selected.portionMasterId,
          );
      priceCtrl.clear();
      await reload();
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> delete(ProductPortion portion) async {
    final ok = await showAppConfirmBottomSheet(
      context: context,
      title: 'Delete portion',
      message: 'Remove ${portion.portionName}?',
      confirmLabel: 'Delete',
      confirmVariant: AppButtonVariant.danger,
    );
    if (!ok) return;
    await ref.read(appDatabaseProvider).softDeletePortion(portion.portionId);
    await reload();
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
    final selected = productPortionsPageSelected != null &&
            masters.any((m) => m.portionMasterId == productPortionsPageSelected!.portionMasterId)
        ? masters.firstWhere(
            (m) => m.portionMasterId == productPortionsPageSelected!.portionMasterId,
          )
        : (masters.isNotEmpty ? masters.first : null);

    if (!loaded) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted && !loaded) reload();
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
                  productPortionsPagePortions.isEmpty
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
                  onChanged: (v) => setState(() => productPortionsPageSelected = v),
                ),
                const SizedBox(height: 12),
                MasterOutlinedField(
                  controller: priceCtrl,
                  hint: 'Portion Price',
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                ),
                const SizedBox(height: 12),
                MasterOutlinedField(
                  controller: sortCtrl,
                  hint: 'Sort Order',
                  keyboardType: TextInputType.number,
                ),
                const SizedBox(height: 12),
                MasterPrimaryButton(
                  label: 'Add Portion',
                  isLoading: busy,
                  onPressed: busy ? null : productPortionsPageAdd,
                ),
              ],
            ),
          ),
          const SizedBox(height: 18),
          const MasterSectionLabel('Portion List'),
          const SizedBox(height: 10),
          MasterCard(
            padding: EdgeInsets.zero,
            child: !loaded
                ? const Padding(
                    padding: EdgeInsets.all(24),
                    child: Center(child: CircularProgressIndicator()),
                  )
                : productPortionsPagePortions.isEmpty
                    ? const MasterEmptyState(
                        title: 'No data found',
                        subtitle: 'Add portion sizes like Half / Full.',
                      )
                    : Column(
                        children: [
                          for (var i = 0; i < productPortionsPagePortions.length; i++)
                            MasterListRow(
                              index: i + 1,
                              title: productPortionsPagePortions[i].portionName,
                              subtitle: currency
                                  .format(productPortionsPagePortions[i].portionPrice),
                              onEdit: () {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(
                                    content: Text(
                                      'Delete and re-add to change a portion',
                                    ),
                                  ),
                                );
                              },
                              onDelete: () => delete(productPortionsPagePortions[i]),
                              showDivider: i < productPortionsPagePortions.length - 1,
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
