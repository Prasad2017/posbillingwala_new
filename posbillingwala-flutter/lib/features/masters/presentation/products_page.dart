import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

final productPortionsMapProvider =
    StreamProvider<Map<int, List<ProductPortion>>>((ref) {
      return ref.watch(appDatabaseProvider).watchActivePortions().map((rows) {
        final map = <int, List<ProductPortion>>{};
        for (final p in rows) {
          map.putIfAbsent(p.productId, () => []).add(p);
        }
        return map;
      });
    });

class ProductsPage extends ConsumerStatefulWidget {
  const ProductsPage({super.key});

  @override
  ConsumerState<ProductsPage> createState() => ProductsPageState();
}

class ProductsPageState extends ConsumerState<ProductsPage> {
  final searchCtrl = TextEditingController();
  String query = '';

  @override
  void dispose() {
    searchCtrl.dispose();
    super.dispose();
  }

  Future<void> printCatalog(List<Product> products) async {
    if (products.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('No products to print')));
      return;
    }
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final buf = StringBuffer()
      ..writeln('PRODUCT LIST')
      ..writeln(DateFormat('dd MMM yyyy HH:mm').format(DateTime.now()))
      ..writeln('-' * 32);
    for (final p in products) {
      buf.writeln(p.productName);
      buf.writeln('  ${currency.format(p.productPrice)}');
    }
    buf.writeln('-' * 32);
    buf.writeln('Total items: ${products.length}');
    final result = await ref
        .read(printServiceProvider)
        .printRawText(buf.toString(), label: 'Product catalog');
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(result.message ?? result.outcome.name)),
    );
  }

  Future<void> delete(Product product) async {
    final ok = await showAppConfirmBottomSheet(
      context: context,
      title: 'Delete product',
      message: 'Remove ${product.productName}?',
      confirmLabel: 'Delete',
      confirmVariant: AppButtonVariant.danger,
      icon: Icons.delete_outline_rounded,
    );
    if (!ok) return;
    await ref
        .read(mastersSyncControllerProvider.notifier)
        .deleteProduct(product.productId);
    ref.invalidate(catalogCountsProvider);
  }

  @override
  Widget build(BuildContext context) {
    final productsAsync = ref.watch(productsProvider);
    final portionsMap = ref
        .watch(productPortionsMapProvider)
        .maybeWhen(
          data: (v) => v,
          orElse: () => const <int, List<ProductPortion>>{},
        );
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(
        title: Text(AppStrings.of(ref).productList),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => context.pop(),
        ),
        actions: [
          IconButton(
            tooltip: 'Print',
            onPressed: () {
              final products = productsAsync.maybeWhen(
                data: (v) => v,
                orElse: () => const <Product>[],
              );
              printCatalog(products);
            },
            style: IconButton.styleFrom(
              backgroundColor: Colors.white.withValues(alpha: .18),
              foregroundColor: Colors.white,
            ),
            icon: const Icon(Icons.print_rounded, size: 20),
          ),
          const SizedBox(width: 6),
          Padding(
            padding: const EdgeInsets.only(right: 10),
            child: TextButton(
              onPressed: () => context.push('/masters/products/form'),
              style: TextButton.styleFrom(
                backgroundColor: Colors.white,
                foregroundColor: AppColors.primary,
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 8,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
              child: const Text(
                'Add Product',
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontWeight: FontWeight.w700,
                  fontSize: 13,
                ),
              ),
            ),
          ),
        ],
      ),
      body: productsAsync.when(
        data: (all) {
          final q = query.trim().toLowerCase();
          final products = q.isEmpty
              ? all
              : all.where((p) {
                  final hay =
                      '${p.productName} ${p.categoryName ?? ''} ${p.productCode ?? ''}'
                          .toLowerCase();
                  return hay.contains(q);
                }).toList();

          return ResponsiveScrollShell(
            dashboard: true,
            child: ListView(
              padding: EdgeInsets.fromLTRB(
                AppBreakpoints.pagePaddingFor(context.widthClass),
                14,
                AppBreakpoints.pagePaddingFor(context.widthClass),
                28,
              ),
              children: [
                TextField(
                  controller: searchCtrl,
                  onChanged: (v) => setState(() => query = v),
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 14,
                    color: AppColors.navy,
                  ),
                  decoration: InputDecoration(
                    hintText: 'search product by name, category',
                    hintStyle: TextStyle(
                      fontFamily: AppFonts.family,
                      color: AppColors.navy.withValues(alpha: .38),
                    ),
                    prefixIcon: Icon(
                      Icons.search_rounded,
                      color: AppColors.navy.withValues(alpha: .45),
                    ),
                    filled: true,
                    fillColor: Colors.white,
                    contentPadding: const EdgeInsets.symmetric(vertical: 12),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: BorderSide(
                        color: AppColors.border.withValues(alpha: .9),
                      ),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: BorderSide(
                        color: AppColors.border.withValues(alpha: .9),
                      ),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(
                        color: AppColors.primary,
                        width: 1.3,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                MasterSectionLabel(
                  'Product List',
                  trailing: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 5,
                    ),
                    decoration: BoxDecoration(
                      color: AppColors.primaryLight,
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      '${products.length} Products',
                      style: const TextStyle(
                        fontFamily: AppFonts.family,
                        color: AppColors.primary,
                        fontWeight: FontWeight.w700,
                        fontSize: 11.5,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 10),
                if (products.isEmpty)
                  const MasterCard(
                    child: MasterEmptyState(
                      title: 'No products found',
                      subtitle: 'Tap Add Product to create a menu item.',
                    ),
                  )
                else ...[
                  Builder(
                    builder: (context) {
                      final cols = AppBreakpoints.cardColumnsFor(
                        context.widthClass,
                      );
                      final rows = (products.length / cols).ceil();
                      return Column(
                        children: [
                          for (var row = 0; row < rows; row++) ...[
                            if (row > 0) const SizedBox(height: 10),
                            Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                for (var c = 0; c < cols; c++) ...[
                                  if (c > 0) const SizedBox(width: 10),
                                  Expanded(
                                    child: Builder(
                                      builder: (context) {
                                        final i = row * cols + c;
                                        if (i >= products.length) {
                                          return const SizedBox.shrink();
                                        }
                                        final product = products[i];
                                        return ProductCard(
                                          product: product,
                                          priceLabel: currency.format(
                                            product.productPrice,
                                          ),
                                          portions:
                                              portionsMap[product.productId] ??
                                              const [],
                                          onEdit: () => context.push(
                                            '/masters/products/form?id=${product.productId}',
                                          ),
                                          onDelete: () => delete(product),
                                          onPortions: () => context.push(
                                            '/masters/products/portions?id=${product.productId}',
                                          ),
                                        );
                                      },
                                    ),
                                  ),
                                ],
                              ],
                            ),
                          ],
                        ],
                      );
                    },
                  ),
                ],
              ],
            ),
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
      ),
    );
  }
}

class ProductCard extends StatelessWidget {
  const ProductCard({
    super.key,
    required this.product,
    required this.priceLabel,
    required this.portions,
    required this.onEdit,
    required this.onDelete,
    required this.onPortions,
  });

  final Product product;
  final String priceLabel;
  final List<ProductPortion> portions;
  final VoidCallback onEdit;
  final VoidCallback onDelete;
  final VoidCallback onPortions;

  @override
  Widget build(BuildContext context) {
    final trimmed = product.productName.trim();
    final initial = trimmed.isEmpty
        ? '?'
        : trimmed.substring(0, 1).toUpperCase();
    final meta = [
      if ((product.productCode ?? '').trim().isNotEmpty)
        '#${product.productCode}',
      if ((product.categoryName ?? '').trim().isNotEmpty) product.categoryName!,
      if ((product.productUnit ?? '').trim().isNotEmpty) product.productUnit!,
    ].join(' · ');

    return MasterCard(
      padding: const EdgeInsets.fromLTRB(12, 12, 10, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 42,
                height: 42,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: AppColors.primaryLight,
                  borderRadius: BorderRadius.circular(11),
                ),
                child: Text(
                  initial,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    color: AppColors.primary,
                    fontWeight: FontWeight.w800,
                    fontSize: 18,
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            product.productName,
                            style: const TextStyle(
                              fontFamily: AppFonts.family,
                              color: AppColors.navy,
                              fontWeight: FontWeight.w700,
                              fontSize: 15,
                            ),
                          ),
                        ),
                        Text(
                          priceLabel,
                          style: const TextStyle(
                            fontFamily: AppFonts.family,
                            color: AppColors.primary,
                            fontWeight: FontWeight.w800,
                            fontSize: 14.5,
                          ),
                        ),
                      ],
                    ),
                    if (meta.isNotEmpty) ...[
                      const SizedBox(height: 3),
                      Text(
                        meta,
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          color: AppColors.navy.withValues(alpha: .45),
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(width: 6),
              MasterIconAction(
                icon: Icons.edit_rounded,
                color: AppColors.primary,
                filled: true,
                onTap: onEdit,
              ),
              const SizedBox(width: 4),
              MasterIconAction(
                icon: Icons.delete_outline_rounded,
                color: AppColors.red,
                filled: true,
                onTap: onDelete,
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'PORTIONS',
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontSize: 10.5,
                        letterSpacing: 0.6,
                        fontWeight: FontWeight.w600,
                        color: AppColors.navy.withValues(alpha: .45),
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      portions.isEmpty
                          ? 'Not configured'
                          : portions
                                .map((p) => p.portionName)
                                .where((n) => n.trim().isNotEmpty)
                                .join(', '),
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontSize: 12.5,
                        color: AppColors.navy.withValues(alpha: .55),
                      ),
                    ),
                  ],
                ),
              ),
              TextButton.icon(
                onPressed: onPortions,
                icon: const Icon(Icons.table_rows_rounded, size: 16),
                label: const Text('Portions'),
                style: TextButton.styleFrom(
                  foregroundColor: AppColors.primary,
                  textStyle: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w700,
                    fontSize: 12.5,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              TaxChip(
                label:
                    'CGST ${product.productCgst.toStringAsFixed(product.productCgst % 1 == 0 ? 0 : 1)}%',
              ),
              const SizedBox(width: 8),
              TaxChip(
                label:
                    'SGST ${product.productSgst.toStringAsFixed(product.productSgst % 1 == 0 ? 0 : 1)}%',
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class TaxChip extends StatelessWidget {
  const TaxChip({super.key, required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.border),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontFamily: AppFonts.family,
          fontSize: 11.5,
          color: AppColors.navy.withValues(alpha: .65),
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
