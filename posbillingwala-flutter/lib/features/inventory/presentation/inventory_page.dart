import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/features/expense/presentation/expense_page.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

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
    Future.microtask(() async {
      if (!AppPlatform.requiresNetwork) return;
      try {
        await ref.read(inventoryControllerProvider.notifier).syncAll();
      } catch (_) {}
    });
  }

  @override
  void dispose() {
    inventoryPageTabs.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
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
          const SizedBox(width: 6),
          Padding(
            padding: const EdgeInsets.only(right: 10),
            child: onStock
                ? PopupMenuButton<String>(
                    onSelected: (v) {
                      if (v == 'purchase') {
                        context.push('/inventory/add');
                      } else if (v == 'waste') {
                        context.push('/inventory/waste');
                      }
                    },
                    itemBuilder: (_) => const [
                      PopupMenuItem(
                        value: 'purchase',
                        child: Text('Purchase / Stock In'),
                      ),
                      PopupMenuItem(
                        value: 'waste',
                        child: Text('Waste / Spoilage'),
                      ),
                    ],
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 8,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Text(
                        'Add',
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          fontWeight: FontWeight.w700,
                          fontSize: 13,
                          color: AppColors.navy,
                        ),
                      ),
                    ),
                  )
                : TextButton(
                    onPressed: () => context.push('/expenses/add'),
                    style: TextButton.styleFrom(
                      backgroundColor: Colors.white,
                      foregroundColor: AppColors.navy,
                      padding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 8,
                      ),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10),
                      ),
                    ),
                    child: const Text(
                      'Add Expense',
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
    final dateFormat = DateFormat('dd MMM');
    final lowCount = balances.where((b) => b.lowStock).length;

    return movementsAsync.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(child: Text('$e')),
      data: (rows) {
        final q = query.trim().toLowerCase();
        final filteredBalances = q.isEmpty
            ? balances
            : balances
                .where((b) => b.productName.toLowerCase().contains(q))
                .toList();
        final filteredMoves = q.isEmpty
            ? rows
            : rows.where((row) {
                final name = row.productName.isEmpty
                    ? 'product ${row.productId}'
                    : row.productName;
                return name.toLowerCase().contains(q) ||
                    row.movementType.toLowerCase().contains(q) ||
                    row.inventoryNote.toLowerCase().contains(q);
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
                  hintText: 'Search product / type / note',
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
                'Current Stock',
                trailing: Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(
                    color: AppColors.primaryLight,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    '${filteredBalances.length} Items',
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
                child: filteredBalances.isEmpty
                    ? const MasterEmptyState(
                        title: 'No stock yet',
                        subtitle: 'Use Add → Purchase to stock a product.',
                      )
                    : Column(
                        children: [
                          for (var i = 0; i < filteredBalances.length; i++) ...[
                            if (i > 0)
                              Divider(
                                height: 1,
                                thickness: 1,
                                color: AppColors.border.withValues(alpha: .7),
                              ),
                            BalanceRow(
                              index: i + 1,
                              balance: filteredBalances[i],
                              qtyFormat: qtyFormat,
                            ),
                          ],
                        ],
                      ),
              ),
              const SizedBox(height: 18),
              const MasterSectionLabel('Movements (Purchase / Waste / Sale)'),
              const SizedBox(height: 10),
              MasterCard(
                padding: EdgeInsets.zero,
                child: filteredMoves.isEmpty
                    ? const MasterEmptyState(
                        title: 'No movements',
                        subtitle: 'Purchases, waste and sales appear here.',
                      )
                    : Column(
                        children: [
                          const MovementTableHeader(),
                          const Divider(height: 1, thickness: 1),
                          for (var i = 0; i < filteredMoves.length; i++) ...[
                            if (i > 0)
                              Divider(
                                height: 1,
                                thickness: 1,
                                color: AppColors.border.withValues(alpha: .7),
                              ),
                            MovementTableRow(
                              index: i + 1,
                              row: filteredMoves[i],
                              qtyFormat: qtyFormat,
                              dateFormat: dateFormat,
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

class BalanceRow extends StatelessWidget {
  const BalanceRow({
    super.key,
    required this.index,
    required this.balance,
    required this.qtyFormat,
  });

  final int index;
  final ProductStockBalance balance;
  final NumberFormat qtyFormat;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 12),
      child: Row(
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
              ),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  balance.productName,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 13.5,
                    fontWeight: FontWeight.w700,
                    color: balance.lowStock
                        ? AppColors.orangeDark
                        : AppColors.navy,
                  ),
                ),
                if (balance.lowStock)
                  Text(
                    'Low stock',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      fontSize: 11,
                      color: AppColors.orange,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
              ],
            ),
          ),
          Text(
            qtyFormat.format(balance.remaining),
            style: TextStyle(
              fontFamily: AppFonts.family,
              fontSize: 16,
              fontWeight: FontWeight.w800,
              color: balance.lowStock ? AppColors.orange : AppColors.teal,
            ),
          ),
        ],
      ),
    );
  }
}

class MovementTableHeader extends StatelessWidget {
  const MovementTableHeader({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.primaryLight.withValues(alpha: .55),
      padding: const EdgeInsets.fromLTRB(10, 12, 10, 12),
      child: const Row(
        children: [
          Expanded(flex: 4, child: HeaderCell('Product / Type')),
          Expanded(
            flex: 2,
            child: HeaderCell('In', align: TextAlign.center),
          ),
          Expanded(
            flex: 2,
            child: HeaderCell('Out', align: TextAlign.center),
          ),
          Expanded(
            flex: 2,
            child: HeaderCell('Bal', align: TextAlign.center),
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

class MovementTableRow extends StatelessWidget {
  const MovementTableRow({
    super.key,
    required this.index,
    required this.row,
    required this.qtyFormat,
    required this.dateFormat,
  });

  final int index;
  final InventoryMovement row;
  final NumberFormat qtyFormat;
  final DateFormat dateFormat;

  String get typeLabel {
    switch (row.movementType) {
      case 'waste':
        return 'Waste';
      case 'sale':
        return 'Sale';
      case 'opening':
        return 'Opening';
      case 'adjust':
        return 'Adjust';
      default:
        return 'Purchase';
    }
  }

  Color get typeColor {
    switch (row.movementType) {
      case 'waste':
        return AppColors.red;
      case 'sale':
        return AppColors.primary;
      case 'opening':
        return AppColors.teal;
      default:
        return AppColors.orange;
    }
  }

  @override
  Widget build(BuildContext context) {
    final name = row.productName.trim().isEmpty
        ? 'Product ${row.productId}'
        : row.productName;
    final note = row.inventoryNote.trim();

    return Padding(
      padding: const EdgeInsets.fromLTRB(10, 10, 10, 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 4,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: AppColors.navy,
                  ),
                ),
                const SizedBox(height: 2),
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 6,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: typeColor.withValues(alpha: .12),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        typeLabel,
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          fontSize: 10.5,
                          fontWeight: FontWeight.w700,
                          color: typeColor,
                        ),
                      ),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      dateFormat.format(row.inventoryDate),
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontSize: 11,
                        color: AppColors.navy.withValues(alpha: .45),
                      ),
                    ),
                  ],
                ),
                if (note.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(
                      note,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        fontSize: 11,
                        color: AppColors.navy.withValues(alpha: .5),
                      ),
                    ),
                  ),
              ],
            ),
          ),
          Expanded(
            flex: 2,
            child: QtyCell(qtyFormat.format(row.productInventoryQuantity)),
          ),
          Expanded(
            flex: 2,
            child: QtyCell(
              qtyFormat.format(row.saleInventoryQuantity),
              emphasize: row.saleInventoryQuantity > 0,
              emphasizeColor: typeColor,
            ),
          ),
          Expanded(
            flex: 2,
            child: QtyCell(
              qtyFormat.format(row.afterSaleInventoryQuantity),
              emphasize: true,
              emphasizeColor: AppColors.navy,
            ),
          ),
        ],
      ),
    );
  }
}

class QtyCell extends StatelessWidget {
  const QtyCell(
    this.value, {
    super.key,
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
