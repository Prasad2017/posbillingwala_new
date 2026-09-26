import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/tables/domain/tables_providers.dart';
import 'package:pos_billingwala_v2/features/tables/presentation/table_ops.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class TablesPage extends ConsumerStatefulWidget {
  const TablesPage({super.key});

  @override
  ConsumerState<TablesPage> createState() => TablesPageState();
}

class TablesPageState extends ConsumerState<TablesPage> {
  int? selectedAreaId;

  @override
  Widget build(BuildContext context) {
    final floor = ref.watch(floorTablesProvider);
    final strings = AppStrings.of(ref);
    final areasAsync = ref.watch(diningAreasProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final areas = areasAsync.maybeWhen(
      data: (v) => v,
      orElse: () => const <DiningArea>[],
    );

    ref.listen(tablesControllerProvider, (prev, next) {
      next.whenOrNull(
        error: (error, _) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text('$error')));
        },
      );
    });

    final filtered = selectedAreaId == null
        ? floor
        : floor.where((t) => t.table.areaId == selectedAreaId).toList();

    return Scaffold(
      appBar: AppBar(title: Text(strings.dineInTables)),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (areas.isNotEmpty)
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 6),
              child: Row(
                children: [
                  AreaChip(
                    label: strings.allAreas,
                    selected: selectedAreaId == null,
                    onTap: () => setState(() => selectedAreaId = null),
                  ),
                  ...areas.map(
                    (a) => AreaChip(
                      label: a.areaName.isEmpty
                          ? 'Area ${a.areaId}'
                          : a.areaName,
                      selected: selectedAreaId == a.areaId,
                      onTap: () => setState(() => selectedAreaId = a.areaId),
                    ),
                  ),
                ],
              ),
            ),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.fromLTRB(12, 2, 12, 6),
            child: Row(
              children: [
                LegendDot(
                  label: strings.tableAvailable,
                  color: AppColors.tableAvailable,
                ),
                LegendDot(
                  label: strings.tableRunning,
                  color: AppColors.tableRunning,
                ),
                LegendDot(
                  label: strings.tableBill,
                  color: AppColors.tableBillRequested,
                ),
                LegendDot(
                  label: strings.tableHold,
                  color: AppColors.tablePayment,
                ),
                LegendDot(
                  label: strings.tableReserved,
                  color: AppColors.tableReserved,
                ),
                LegendDot(
                  label: strings.tableBlocked,
                  color: AppColors.tableBlocked,
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(14, 2, 14, 8),
            child: Text(
              strings.longPressTableHint,
              style: const TextStyle(
                fontSize: 11,
                color: AppColors.textSecondary,
              ),
            ),
          ),
          Expanded(
            child: filtered.isEmpty
                ? AppEmptyState(
                    title: strings.noTablesYet,
                    message: strings.noTablesHint,
                    iconAsset: AppAssets.svgTable,
                  )
                : LayoutBuilder(
                    builder: (context, constraints) {
                      final widthClass = AppBreakpoints.ofWidth(
                        constraints.maxWidth,
                      );
                      final useXy = filtered.any(
                        (t) =>
                            (t.table.positionX ?? 0) != 0 ||
                            (t.table.positionY ?? 0) != 0,
                      );
                      Widget card(FloorTableView item) {
                        return TableCard(
                          floor: item,
                          currency: currency,
                          onTap: () => onTableTap(context, ref, item),
                          onLongPress: () =>
                              showPosTableOverflow(context, ref, item),
                        );
                      }

                      if (useXy) {
                        var maxX = 1.0;
                        var maxY = 1.0;
                        for (final t in filtered) {
                          final x = t.table.positionX ?? 0;
                          final y = t.table.positionY ?? 0;
                          if (x > maxX) maxX = x;
                          if (y > maxY) maxY = y;
                        }
                        /* Compact: fit viewport; wider: keep roomy floor for positions. */
                        final minCanvasW = AppBreakpoints.isMobileClass(widthClass)
                            ? constraints.maxWidth
                            : 720.0;
                        final minCanvasH = context.isShortHeight
                            ? constraints.maxHeight
                            : 520.0;
                        final canvasW = constraints.maxWidth < minCanvasW
                            ? minCanvasW
                            : constraints.maxWidth;
                        final canvasH = constraints.maxHeight < minCanvasH
                            ? minCanvasH
                            : constraints.maxHeight;
                        final cardW = AppBreakpoints.isMobileClass(widthClass)
                            ? 120.0
                            : 148.0;
                        double left(double? v) {
                          final n = v ?? 0;
                          final span = (canvasW - cardW).clamp(1.0, canvasW);
                          if (maxX <= 1.5) return n.clamp(0.0, 1.0) * span;
                          return (n / maxX).clamp(0.0, 1.0) * span;
                        }

                        double top(double? v) {
                          final n = v ?? 0;
                          final span = (canvasH - (cardW * 0.95)).clamp(
                            1.0,
                            canvasH,
                          );
                          if (maxY <= 1.5) return n.clamp(0.0, 1.0) * span;
                          return (n / maxY).clamp(0.0, 1.0) * span;
                        }

                        return InteractiveViewer(
                          constrained: false,
                          minScale: 0.7,
                          maxScale: 2.2,
                          child: SizedBox(
                            width: canvasW,
                            height: canvasH,
                            child: Stack(
                              children: [
                                for (final item in filtered)
                                  Positioned(
                                    left: left(item.table.positionX),
                                    top: top(item.table.positionY),
                                    width: cardW,
                                    child: card(item),
                                  ),
                              ],
                            ),
                          ),
                        );
                      }

                      final pad = AppBreakpoints.pagePaddingFor(widthClass);
                      final gap = 12.0;
                      final cols = AppBreakpoints.tableColumnsForWidth(
                        constraints.maxWidth - (pad * 2),
                      );
                      final cardWidth =
                          (constraints.maxWidth -
                              (pad * 2) -
                              (gap * (cols - 1))) /
                          cols;

                      return ResponsiveScrollShell(
                        dashboard: true,
                        child: SingleChildScrollView(
                          padding: EdgeInsets.fromLTRB(pad, 0, pad, 24),
                          child: Wrap(
                            spacing: gap,
                            runSpacing: gap,
                            children: [
                              for (final item in filtered)
                                SizedBox(width: cardWidth, child: card(item)),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }

  /* Android TableAdapter.onTableTapped — available opens billing; occupied shows sheet. */
  Future<void> onTableTap(
    BuildContext context,
    WidgetRef ref,
    FloorTableView floor,
  ) async {
    if (floor.status == FloorTableStatus.blocked) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Table is blocked')),
      );
      return;
    }
    if (floor.status == FloorTableStatus.reserved) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Table is reserved')),
      );
      return;
    }

    if (floor.status == FloorTableStatus.available) {
      try {
        await ref.read(tablesControllerProvider.notifier).openTable(floor);
        if (!context.mounted) return;
        context.push('/tables/billing');
      } catch (e) {
        if (!context.mounted) return;
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('$e')));
      }
      return;
    }

    await showOccupiedSheet(context, ref, floor);
  }

  /* Android TableAdapter.showOccupiedSheet */
  Future<void> showOccupiedSheet(
    BuildContext context,
    WidgetRef ref,
    FloorTableView floor,
  ) async {
    final strings = AppStrings.of(ref);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final title = floor.table.displayName.isEmpty
        ? 'Table ${floor.table.tableNumber}'
        : floor.table.displayName;

    final amountLines = StringBuffer(
      'Existing Bill:\n${currency.format(floor.currentAmount)}',
    );
    if (floor.remainingAmount > 0.05 &&
        floor.remainingAmount + 0.05 < floor.currentAmount) {
      amountLines.writeln(
        'Remaining: ${currency.format(floor.remainingAmount)}',
      );
    }
    if (floor.printRetryAvailable) {
      amountLines.writeln('Print: Failed — retry available');
    }

    final paymentPending =
        floor.status == FloorTableStatus.paymentPending ||
        floor.status == FloorTableStatus.partiallyPaid;
    final settleLabel = floor.printRetryAvailable
        ? 'Retry Print'
        : strings.settleBill;

    final action = await showModalBottomSheet<String>(
      context: context,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              title: Text('$title IS OCCUPIED'),
              subtitle: Text(amountLines.toString().trim()),
            ),
            const Divider(height: 1),
            ListTile(
              title: Text(strings.addItemsExisting),
              onTap: () => Navigator.pop(context, 'add_items'),
            ),
            ListTile(
              title: Text(strings.viewBill),
              onTap: () => Navigator.pop(context, 'view_bill'),
            ),
            ListTile(
              title: Text(settleLabel),
              onTap: () => Navigator.pop(context, 'settle'),
            ),
            ListTile(
              title: Text(strings.cancel),
              onTap: () => Navigator.pop(context),
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );

    if (!context.mounted || action == null) return;

    if (action == 'add_items') {
      try {
        await ref.read(tablesControllerProvider.notifier).openTable(floor);
        if (!context.mounted) return;
        context.push('/tables/billing');
      } catch (e) {
        if (!context.mounted) return;
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('$e')));
      }
      return;
    }

    if (action == 'view_bill') {
      try {
        await ref.read(tablesControllerProvider.notifier).openTable(floor);
        if (!context.mounted) return;
        context.push('/tables/payment');
      } catch (e) {
        if (!context.mounted) return;
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('$e')));
      }
      return;
    }

    if (action == 'settle') {
      if (floor.printRetryAvailable) {
        await retryFailedBillPrint(context, ref, floor);
        return;
      }
      if (paymentPending && floor.unpaidInvoice != null) {
        await settleUnpaidTableInvoice(context, ref, floor);
        return;
      }
      try {
        await ref.read(tablesControllerProvider.notifier).openTable(floor);
        if (!context.mounted) return;
        context.push('/tables/payment');
      } catch (e) {
        if (!context.mounted) return;
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }
}

class AreaChip extends StatelessWidget {
  const AreaChip({
    super.key,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: ChoiceChip(
        label: Text(
          label,
          style: TextStyle(
            color: selected ? Colors.white : AppColors.navy,
            fontWeight: FontWeight.w700,
          ),
        ),
        selected: selected,
        selectedColor: AppColors.navy,
        backgroundColor: AppColors.primaryLight,
        checkmarkColor: Colors.white,
        side: BorderSide.none,
        onSelected: (_) => onTap(),
      ),
    );
  }
}

class LegendDot extends StatelessWidget {
  const LegendDot({super.key, required this.label, required this.color});

  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 12),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 8,
            height: 8,
            decoration: BoxDecoration(color: color, shape: BoxShape.circle),
          ),
          const SizedBox(width: 6),
          Text(label, style: const TextStyle(fontSize: 11)),
        ],
      ),
    );
  }
}

class TableCard extends StatelessWidget {
  const TableCard({
    super.key,
    required this.floor,
    required this.currency,
    required this.onTap,
    required this.onLongPress,
  });

  final FloorTableView floor;
  final NumberFormat currency;
  final VoidCallback onTap;
  final VoidCallback onLongPress;

  Color get statusColor {
    switch (floor.status) {
      case FloorTableStatus.available:
        return AppColors.tableAvailable;
      case FloorTableStatus.running:
        return AppColors.tableRunning;
      case FloorTableStatus.billRequested:
        return AppColors.tableBillRequested;
      case FloorTableStatus.paymentPending:
      case FloorTableStatus.partiallyPaid:
        return AppColors.tablePayment;
      case FloorTableStatus.blocked:
        return AppColors.tableBlocked;
      case FloorTableStatus.reserved:
        return AppColors.tableReserved;
    }
  }

  @override
  Widget build(BuildContext context) {
    final table = floor.table;
    final name = table.displayName.trim().isEmpty
        ? 'Table ${table.tableNumber}'
        : table.displayName;
    final typeLabel = floor.tableTypeName.trim();
    final elapsed = floor.elapsedLabel;
    final metaParts = <String>[
      if (floor.joinedLabel != null) floor.joinedLabel!,
      if (floor.joinedLabel == null && table.capacity > 0)
        'Seats ${table.capacity}',
      if (elapsed.isNotEmpty) elapsed,
      if (floor.remainingAmount > 0.05 &&
          floor.status == FloorTableStatus.partiallyPaid)
        'Due ${currency.format(floor.remainingAmount)}',
    ];
    final seatsLabel = metaParts.isEmpty ? null : metaParts.join(' · ');
    final color = statusColor;
    const onFill = Colors.white;

    return Container(
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: color.withValues(alpha: 0.35),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(16),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          onLongPress: onLongPress,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(14, 14, 14, 14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  name,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 18,
                    color: onFill,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  floor.statusLabel,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: onFill.withValues(alpha: 0.92),
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                  ),
                ),
                if (typeLabel.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text(
                    typeLabel,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: onFill.withValues(alpha: 0.9),
                      fontWeight: FontWeight.w600,
                      fontSize: 12,
                    ),
                  ),
                ],
                if (floor.currentAmount > 0) ...[
                  const SizedBox(height: 6),
                  Text(
                    currency.format(floor.currentAmount),
                    style: const TextStyle(
                      color: onFill,
                      fontWeight: FontWeight.w800,
                      fontSize: 15,
                    ),
                  ),
                ],
                if (seatsLabel != null && seatsLabel.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text(
                    seatsLabel,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: onFill.withValues(alpha: 0.85),
                      fontWeight: FontWeight.w500,
                      fontSize: 12,
                    ),
                  ),
                ] else if (floor.isJoinedSecondary) ...[
                  const SizedBox(height: 4),
                  Text(
                    'Joined → T${floor.billingTableNumber}',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: onFill.withValues(alpha: 0.9),
                      fontWeight: FontWeight.w700,
                      fontSize: 12,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class MoveItemsSheet extends StatefulWidget {
  const MoveItemsSheet({super.key, required this.items});

  final List<CartItem> items;

  @override
  State<MoveItemsSheet> createState() => MoveItemsSheetState();
}

class MoveItemsSheetState extends State<MoveItemsSheet> {
  late final Set<int> tablesPageSelected;

  @override
  void initState() {
    super.initState();
    tablesPageSelected = {for (var i = 0; i < widget.items.length; i++) i};
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Select items to move',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            Flexible(
              child: ListView.builder(
                shrinkWrap: true,
                itemCount: widget.items.length,
                itemBuilder: (context, index) {
                  final item = widget.items[index];
                  return CheckboxListTile(
                    value: tablesPageSelected.contains(index),
                    title: Text(item.productName),
                    subtitle: Text('Qty ${item.quantity}'),
                    onChanged: (on) => setState(() {
                      if (on == true) {
                        tablesPageSelected.add(index);
                      } else {
                        tablesPageSelected.remove(index);
                      }
                    }),
                  );
                },
              ),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: AppButton(
                    label: 'Cancel',
                    variant: AppButtonVariant.outlined,
                    onPressed: () => Navigator.pop(context),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: AppButton(
                    label: 'Continue',
                    onPressed: () {
                      final picked = [
                        for (final i in tablesPageSelected) widget.items[i],
                      ];
                      Navigator.pop(context, picked);
                    },
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
