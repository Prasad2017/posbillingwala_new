import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/features/expense/presentation/expense_page.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

class InventoryPage extends ConsumerStatefulWidget {
  const InventoryPage({super.key, this.initialTab = 0});

  final int initialTab;

  @override
  ConsumerState<InventoryPage> createState() => InventoryPageState();
}

class InventoryPageState extends ConsumerState<InventoryPage>
    with SingleTickerProviderStateMixin {
  late final TabController inventoryPageTabs;

  @override
  void initState() {
    super.initState();
    inventoryPageTabs = TabController(
      length: 2,
      vsync: this,
      initialIndex: widget.initialTab.clamp(0, 1),
    );
    inventoryPageTabs.addListener(() {
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    inventoryPageTabs.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final busy = ref.watch(inventoryControllerProvider).isLoading;
    final onStock = inventoryPageTabs.index == 0;

    ref.listen(inventoryControllerProvider, (prev, next) {
      next.whenOrNull(
        data: (msg) {
          if (msg == null) return;
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text(msg)));
        },
        error: (e, _) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text('$e')));
        },
      );
    });

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(
        title: Text(AppStrings.of(ref).inventory),
        bottom: TabBar(
          controller: inventoryPageTabs,
          indicatorColor: Colors.white,
          indicatorWeight: 3,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.white70,
          labelStyle: const TextStyle(
            fontFamily: AppFonts.family,
            fontWeight: FontWeight.w700,
            fontSize: 13.5,
          ),
          unselectedLabelStyle: const TextStyle(
            fontFamily: AppFonts.family,
            fontWeight: FontWeight.w500,
            fontSize: 13.5,
          ),
          tabs: const [
            Tab(text: 'Stock'),
            Tab(text: 'Expenses'),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Sync stock & expenses',
            onPressed: busy
                ? null
                : () =>
                    ref.read(inventoryControllerProvider.notifier).syncAll(),
            style: IconButton.styleFrom(
              backgroundColor: Colors.white.withValues(alpha: .18),
              foregroundColor: Colors.white,
            ),
            icon: busy
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : const Icon(Icons.cloud_sync_rounded, size: 20),
          ),
          const SizedBox(width: 6),
          Padding(
            padding: const EdgeInsets.only(right: 10),
            child: TextButton(
              onPressed: () {
                if (onStock) {
                  context.push('/inventory/add');
                } else {
                  context.push('/expenses/add');
                }
              },
              style: TextButton.styleFrom(
                backgroundColor: Colors.white,
                foregroundColor: AppColors.navy,
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
              child: Text(
                onStock ? 'Add Inventory' : 'Add Expense',
                style: const TextStyle(
                  fontFamily: AppFonts.family,
                  fontWeight: FontWeight.w700,
                  fontSize: 13,
                ),
              ),
            ),
          ),
        ],
      ),
      body: TabBarView(
        controller: inventoryPageTabs,
        children: const [
          StockTab(),
          ExpensesTab(),
        ],
      ),
    );
  }
}

class StockTab extends ConsumerStatefulWidget {
  const StockTab({super.key});

  @override
  ConsumerState<StockTab> createState() => StockTabState();
}

class StockTabState extends ConsumerState<StockTab> {
  final searchCtrl = TextEditingController();
  String query = '';

  @override
  void dispose() {
    searchCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final balances = ref.watch(stockBalancesProvider);
    final movementsAsync = ref.watch(inventoryMovementsProvider);
    final qtyFormat = NumberFormat('#0.##');
    final lowCount = balances.where((b) => b.lowStock).length;

    return movementsAsync.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(child: Text('$e')),
      data: (rows) {
        final q = query.trim().toLowerCase();
        final filtered = q.isEmpty
            ? rows
            : rows.where((row) {
                final name = row.productName.isEmpty
                    ? 'product ${row.productId}'
                    : row.productName;
                return name.toLowerCase().contains(q);
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
            StockSummaryBar(
              productCount: balances.length,
              lowCount: lowCount,
              movementCount: rows.length,
            ),
            const SizedBox(height: 14),
            TextField(
              controller: searchCtrl,
              onChanged: (v) => setState(() => query = v),
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 14,
                color: AppColors.navy,
              ),
              decoration: InputDecoration(
                hintText: 'Search product',
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
            const SizedBox(height: 14),
            MasterSectionLabel(
              'Inventory List',
              trailing: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                decoration: BoxDecoration(
                  color: AppColors.primaryLight,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  '${filtered.length} Items',
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
            MasterCard(
              padding: EdgeInsets.zero,
              child: filtered.isEmpty
                  ? const MasterEmptyState(
                      title: 'No inventory yet',
                      subtitle: 'Tap Add Inventory to stock a product.',
                    )
                  : Column(
                      children: [
                        const InventoryTableHeader(),
                        const Divider(height: 1, thickness: 1),
                        for (var i = 0; i < filtered.length; i++) ...[
                          if (i > 0)
                            Divider(
                              height: 1,
                              thickness: 1,
                              color: AppColors.border.withValues(alpha: .7),
                            ),
                          InventoryTableRow(
                            index: i + 1,
                            row: filtered[i],
                            qtyFormat: qtyFormat,
                          ),
                        ],
                      ],
                    ),
            ),
          ],
          ),
        );
      },
    );
  }
}

class StockSummaryBar extends StatelessWidget {
  const StockSummaryBar({super.key, 
    required this.productCount,
    required this.lowCount,
    required this.movementCount,
  });

  final int productCount;
  final int lowCount;
  final int movementCount;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: SummaryChip(
            label: 'Products',
            value: '$productCount',
            color: AppColors.primary,
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: SummaryChip(
            label: 'Low stock',
            value: '$lowCount',
            color: AppColors.orange,
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: SummaryChip(
            label: 'Entries',
            value: '$movementCount',
            color: AppColors.teal,
          ),
        ),
      ],
    );
  }
}

class SummaryChip extends StatelessWidget {
  const SummaryChip({super.key, 
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.border.withValues(alpha: .8)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: TextStyle(
              fontFamily: AppFonts.family,
              fontSize: 11,
              color: AppColors.navy.withValues(alpha: .5),
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            value,
            style: TextStyle(
              fontFamily: AppFonts.family,
              fontSize: 18,
              fontWeight: FontWeight.w800,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}

class InventoryTableHeader extends StatelessWidget {
  const InventoryTableHeader({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.primaryLight.withValues(alpha: .55),
      padding: const EdgeInsets.fromLTRB(10, 12, 10, 12),
      child: const Row(
        children: [
          SizedBox(
            width: 28,
            child: HeaderCell('Sr', align: TextAlign.center),
          ),
          SizedBox(width: 6),
          Expanded(
            flex: 5,
            child: HeaderCell('Product'),
          ),
          Expanded(
            flex: 2,
            child: HeaderCell('Total\nQty', align: TextAlign.center),
          ),
          Expanded(
            flex: 2,
            child: HeaderCell('After\nSale', align: TextAlign.center),
          ),
          Expanded(
            flex: 2,
            child: HeaderCell('Sale\nQty', align: TextAlign.center),
          ),
        ],
      ),
    );
  }
}

class HeaderCell extends StatelessWidget {
  const HeaderCell(this.label, {super.key, this.align = TextAlign.left});

  final String label;
  final TextAlign align;

  @override
  Widget build(BuildContext context) {
    return Text(
      label,
      textAlign: align,
      style: TextStyle(
        fontFamily: AppFonts.family,
        fontSize: 11,
        height: 1.2,
        fontWeight: FontWeight.w700,
        color: AppColors.navy.withValues(alpha: .62),
      ),
    );
  }
}

class InventoryTableRow extends StatelessWidget {
  const InventoryTableRow({super.key, 
    required this.index,
    required this.row,
    required this.qtyFormat,
  });

  final int index;
  final InventoryMovement row;
  final NumberFormat qtyFormat;

  @override
  Widget build(BuildContext context) {
    final name = row.productName.trim().isEmpty
        ? 'Product ${row.productId}'
        : row.productName;
    final low = row.afterSaleInventoryQuantity < 6;

    return Padding(
      padding: const EdgeInsets.fromLTRB(10, 12, 10, 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          SizedBox(
            width: 28,
            child: Text(
              '$index',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 12,
                color: AppColors.navy.withValues(alpha: .45),
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          const SizedBox(width: 6),
          Expanded(
            flex: 5,
            child: Text(
              name,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 13,
                fontWeight: FontWeight.w700,
                color: low ? AppColors.orangeDark : AppColors.navy,
                height: 1.25,
              ),
            ),
          ),
          Expanded(
            flex: 2,
            child: QtyCell(
              qtyFormat.format(row.productInventoryQuantity),
            ),
          ),
          Expanded(
            flex: 2,
            child: QtyCell(
              qtyFormat.format(row.afterSaleInventoryQuantity),
              emphasize: low,
              emphasizeColor: AppColors.orange,
            ),
          ),
          Expanded(
            flex: 2,
            child: QtyCell(
              qtyFormat.format(row.saleInventoryQuantity),
              emphasize: true,
              emphasizeColor: AppColors.primary,
            ),
          ),
        ],
      ),
    );
  }
}

class QtyCell extends StatelessWidget {
  const QtyCell(
    this.value, {super.key, 
    this.emphasize = false,
    this.emphasizeColor,
  });

  final String value;
  final bool emphasize;
  final Color? emphasizeColor;

  @override
  Widget build(BuildContext context) {
    return Text(
      value,
      textAlign: TextAlign.center,
      style: TextStyle(
        fontFamily: AppFonts.family,
        fontSize: 13,
        fontWeight: emphasize ? FontWeight.w800 : FontWeight.w600,
        color: emphasize
            ? (emphasizeColor ?? AppColors.primary)
            : AppColors.navy.withValues(alpha: .72),
      ),
    );
  }
}

class ExpensesTab extends ConsumerWidget {
  const ExpensesTab({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return const ExpenseListBody();
  }
}
