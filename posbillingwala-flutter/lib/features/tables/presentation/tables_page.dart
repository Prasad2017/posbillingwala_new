import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
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
    final areas = areasAsync.maybeWhen(data: (v) => v, orElse: () => const <DiningArea>[]);

    ref.listen(tablesControllerProvider, (prev, next) {
      next.whenOrNull(
        error: (error, _) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('$error')),
          );
        },
      );
    });

    final filtered = selectedAreaId == null
        ? floor
        : floor.where((t) => t.table.areaId == selectedAreaId).toList();

    return Scaffold(
      appBar: AppBar(
        title: Text(strings.dineInTables),
      ),
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
                      label: a.areaName.isEmpty ? 'Area ${a.areaId}' : a.areaName,
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
                LegendDot(label: strings.tableAvailable, color: AppColors.success),
                LegendDot(label: strings.tableRunning, color: AppColors.warning),
                LegendDot(label: strings.tableHold, color: AppColors.orange),
                LegendDot(label: strings.tableBill, color: AppColors.purple),
                LegendDot(label: strings.tableBlocked, color: AppColors.red),
                LegendDot(label: strings.tableReserved, color: AppColors.teal),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(14, 2, 14, 8),
            child: Text(
              strings.longPressTableHint,
              style: const TextStyle(fontSize: 11, color: AppColors.textSecondary),
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
                      final widthClass =
                          AppBreakpoints.ofWidth(constraints.maxWidth);
                      final cols =
                          AppBreakpoints.tableColumnsFor(widthClass);
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
                          onLongPress: () => onTableActions(
                            context,
                            ref,
                            item,
                            floor,
                          ),
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
                        final canvasW = constraints.maxWidth < 720
                            ? 720.0
                            : constraints.maxWidth;
                        final canvasH = constraints.maxHeight < 520
                            ? 520.0
                            : constraints.maxHeight;
                        const cardW = 148.0;
                        double left(double? v) {
                          final n = v ?? 0;
                          final span = canvasW - cardW;
                          if (maxX <= 1.5) return n.clamp(0.0, 1.0) * span;
                          return (n / maxX).clamp(0.0, 1.0) * span;
                        }

                        double top(double? v) {
                          final n = v ?? 0;
                          final span = canvasH - 140;
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
                      final cardWidth =
                          (constraints.maxWidth - (pad * 2) - (gap * (cols - 1))) /
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
                                SizedBox(
                                  width: cardWidth,
                                  child: card(item),
                                ),
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

  Future<void> onTableTap(
    BuildContext context,
    WidgetRef ref,
    FloorTableView floor,
  ) async {
    if (floor.status == FloorTableStatus.blocked ||
        floor.status == FloorTableStatus.reserved) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Table is ${floor.statusLabel.toLowerCase()}')),
      );
      return;
    }

    try {
      await ref.read(tablesControllerProvider.notifier).openTable(floor);
      if (!context.mounted) return;
      context.push('/tables/billing');
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$e')),
      );
    }
  }

  Future<void> onTableActions(
    BuildContext context,
    WidgetRef ref,
    FloorTableView floor,
    List<FloorTableView> all,
  ) async {
    if (floor.status == FloorTableStatus.blocked ||
        floor.status == FloorTableStatus.reserved) {
      return;
    }

    final occupied = floor.status == FloorTableStatus.running ||
        floor.status == FloorTableStatus.hold ||
        floor.status == FloorTableStatus.billRequest;
    final strings = AppStrings.of(ref);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final title = floor.table.displayName.isEmpty
        ? 'Table ${floor.table.tableNumber}'
        : floor.table.displayName;

    final action = await showModalBottomSheet<String>(
      context: context,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: occupied
            ? Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  ListTile(
                    title: Text(title),
                    subtitle: Text(currency.format(floor.currentAmount)),
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
                    title: Text(strings.settleBill),
                    onTap: () => Navigator.pop(context, 'settle'),
                  ),
                  ListTile(
                    title: Text(strings.more),
                    onTap: () => Navigator.pop(context, 'more'),
                  ),
                  ListTile(
                    title: Text(strings.cancel),
                    onTap: () => Navigator.pop(context),
                  ),
                  const SizedBox(height: 8),
                ],
              )
            : Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  ListTile(
                    title: Text(title),
                    subtitle: Text(floor.statusLabel),
                  ),
                  const Divider(height: 1),
                  ListTile(
                    title: Text(strings.openBilling),
                    onTap: () => Navigator.pop(context, 'open'),
                  ),
                  const SizedBox(height: 8),
                ],
              ),
      ),
    );

    if (!context.mounted || action == null) return;

    if (action == 'more') {
      final more = await showAdvancedTableActions(context, floor);
      if (!context.mounted || more == null) return;
      await handleTableAction(context, ref, floor, all, more);
      return;
    }

    if (action == 'add_items' || action == 'view_bill' || action == 'open') {
      await onTableTap(context, ref, floor);
      return;
    }

    if (action == 'settle') {
      try {
        await ref.read(tablesControllerProvider.notifier).openTable(floor);
        if (!context.mounted) return;
        context.push('/tables/payment');
      } catch (e) {
        if (!context.mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
      return;
    }

    await handleTableAction(context, ref, floor, all, action);
  }

  Future<String?> showAdvancedTableActions(
    BuildContext context,
    FloorTableView floor,
  ) {
    return showModalBottomSheet<String>(
      context: context,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const ListTile(title: Text('More actions')),
            const Divider(height: 1),
            ListTile(
              title: const Text('Join with another table'),
              onTap: () => Navigator.pop(context, 'join'),
            ),
            ListTile(
              title: const Text('Transfer to another table'),
              onTap: () => Navigator.pop(context, 'transfer'),
            ),
            ListTile(
              title: const Text('Move items to another table'),
              onTap: () => Navigator.pop(context, 'move'),
            ),
            ListTile(
              title: const Text('Split bill'),
              onTap: () => Navigator.pop(context, 'split_bill'),
            ),
            ListTile(
              title: const Text('Guests / Waiter'),
              onTap: () => Navigator.pop(context, 'meta'),
            ),
            ListTile(
              title: const Text('Duplicate print last bill'),
              onTap: () => Navigator.pop(context, 'print'),
            ),
            if (floor.openSession?.sessionStatus != 'HOLD')
              ListTile(
                title: const Text('Hold table'),
                onTap: () => Navigator.pop(context, 'hold'),
              ),
            if (floor.openSession?.sessionStatus == 'HOLD')
              ListTile(
                title: const Text('Resume table'),
                onTap: () => Navigator.pop(context, 'resume'),
              ),
            ListTile(
              title: const Text('Mark bill requested'),
              onTap: () => Navigator.pop(context, 'bill'),
            ),
            if (floor.openSession != null &&
                (floor.joinedLabel?.contains('+') ?? false) &&
                !floor.isJoinedSecondary)
              ListTile(
                title: const Text('Split joined tables'),
                onTap: () => Navigator.pop(context, 'split'),
              ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  Future<void> handleTableAction(
    BuildContext context,
    WidgetRef ref,
    FloorTableView floor,
    List<FloorTableView> all,
    String action,
  ) async {
    if (action == 'meta' || action == 'print') {
      await handleTableOpsAction(context, ref, floor, all, action);
      return;
    }

    if (action == 'open') {
      await onTableTap(context, ref, floor);
      return;
    }

    if (action == 'split' && floor.openSession != null) {
      await ref
          .read(tablesControllerProvider.notifier)
          .splitJoined(floor.openSession!.sessionId);
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Joined tables split')),
      );
      return;
    }

    if (action == 'split_bill' && floor.openSession != null) {
      await context.push(
        '/tables/split-bill'
        '?table=${Uri.encodeComponent(floor.billingTableNumber)}'
        '&sessionId=${floor.openSession!.sessionId}',
      );
      return;
    }

    if (action == 'hold' && floor.openSession != null) {
      await ref.read(tablesControllerProvider.notifier).setSessionStatus(
            sessionId: floor.openSession!.sessionId,
            status: 'HOLD',
          );
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Table on hold')),
      );
      return;
    }

    if (action == 'resume' && floor.openSession != null) {
      await ref.read(tablesControllerProvider.notifier).setSessionStatus(
            sessionId: floor.openSession!.sessionId,
            status: 'RUNNING',
          );
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Table resumed')),
      );
      return;
    }

    if (action == 'bill' && floor.openSession != null) {
      await ref.read(tablesControllerProvider.notifier).setSessionStatus(
            sessionId: floor.openSession!.sessionId,
            status: 'BILL_REQUEST',
          );
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Bill requested')),
      );
      return;
    }

    if (action == 'transfer' || action == 'move') {
      final candidates = all
          .where(
            (t) =>
                t.table.tableNumber != floor.billingTableNumber &&
                t.status == FloorTableStatus.available,
          )
          .toList();
      if (candidates.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('No available target tables')),
        );
        return;
      }
      final target = await showDialog<FloorTableView>(
        context: context,
        builder: (context) => SimpleDialog(
          title: Text(action == 'transfer' ? 'Transfer to' : 'Move items to'),
          children: candidates
              .map(
                (c) => SimpleDialogOption(
                  onPressed: () => Navigator.pop(context, c),
                  child: Text(
                    c.table.displayName.isEmpty
                        ? 'Table ${c.table.tableNumber}'
                        : c.table.displayName,
                  ),
                ),
              )
              .toList(),
        ),
      );
      if (target == null || !context.mounted) return;
      try {
        if (action == 'transfer') {
          await ref.read(tablesControllerProvider.notifier).transferTable(
                fromTable: floor.billingTableNumber,
                toTable: target.table.tableNumber,
              );
        } else {
          final items = await ref
              .read(appDatabaseProvider)
              .getCartItems(cartScope: floor.billingTableNumber);
          if (items.isEmpty) {
            if (!context.mounted) return;
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('No items to move')),
            );
            return;
          }
          if (!context.mounted) return;
          final selected = await showModalBottomSheet<List<CartItem>>(
            context: context,
            isScrollControlled: true,
            showDragHandle: true,
            builder: (context) => MoveItemsSheet(items: items),
          );
          if (selected == null || selected.isEmpty || !context.mounted) return;
          await ref.read(tablesControllerProvider.notifier).moveItems(
                fromTable: floor.billingTableNumber,
                toTable: target.table.tableNumber,
                items: selected,
              );
        }
        if (!context.mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              action == 'transfer'
                  ? 'Transferred to T${target.table.tableNumber}'
                  : 'Items moved to T${target.table.tableNumber}',
            ),
          ),
        );
      } catch (e) {
        if (!context.mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('$e')),
        );
      }
      return;
    }

    if (action == 'join') {
      final candidates = all
          .where(
            (t) =>
                t.table.tableNumber != floor.billingTableNumber &&
                t.status == FloorTableStatus.available,
          )
          .toList();
      if (candidates.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('No available tables to join')),
        );
        return;
      }

      final secondary = await showDialog<FloorTableView>(
        context: context,
        builder: (context) => SimpleDialog(
          title: const Text('Join with'),
          children: candidates
              .map(
                (c) => SimpleDialogOption(
                  onPressed: () => Navigator.pop(context, c),
                  child: Text(
                    c.table.displayName.isEmpty
                        ? 'Table ${c.table.tableNumber}'
                        : c.table.displayName,
                  ),
                ),
              )
              .toList(),
        ),
      );
      if (secondary == null || !context.mounted) return;

      try {
        await ref.read(tablesControllerProvider.notifier).joinTables(
              primaryTable: floor.billingTableNumber,
              secondaryTable: secondary.table.tableNumber,
            );
        if (!context.mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Joined T${secondary.table.tableNumber} into T${floor.billingTableNumber}',
            ),
          ),
        );
      } catch (e) {
        if (!context.mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('$e')),
        );
      }
    }
  }
}

class AreaChip extends StatelessWidget {
  const AreaChip({super.key, 
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
        label: Text(label),
        selected: selected,
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
  const TableCard({super.key, 
    required this.floor,
    required this.currency,
    required this.onTap,
    required this.onLongPress,
  });

  final FloorTableView floor;
  final NumberFormat currency;
  final VoidCallback onTap;
  final VoidCallback onLongPress;

  Color get bg {
    switch (floor.status) {
      case FloorTableStatus.available:
        return AppColors.success.withValues(alpha: 0.12);
      case FloorTableStatus.running:
        return AppColors.warning.withValues(alpha: 0.16);
      case FloorTableStatus.hold:
        return Colors.blueGrey.withValues(alpha: 0.16);
      case FloorTableStatus.billRequest:
        return AppColors.primary.withValues(alpha: 0.16);
      case FloorTableStatus.blocked:
        return AppColors.danger.withValues(alpha: 0.12);
      case FloorTableStatus.reserved:
        return AppColors.primaryLight;
    }
  }

  Color get fg {
    switch (floor.status) {
      case FloorTableStatus.available:
        return AppColors.success;
      case FloorTableStatus.running:
        return AppColors.warning;
      case FloorTableStatus.hold:
        return Colors.blueGrey.shade700;
      case FloorTableStatus.billRequest:
        return AppColors.primary;
      case FloorTableStatus.blocked:
        return AppColors.danger;
      case FloorTableStatus.reserved:
        return AppColors.primary;
    }
  }

  @override
  Widget build(BuildContext context) {
    final table = floor.table;
    final name = table.displayName.trim().isEmpty
        ? 'Table ${table.tableNumber}'
        : table.displayName;
    final typeLabel = floor.tableTypeName.trim();
    final meta = floor.joinedLabel ??
        (table.capacity > 0
            ? 'Seats ${table.capacity}'
            : 'Table ${table.tableNumber}');

    return Container(
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: fg.withValues(alpha: .18)),
        boxShadow: [
          BoxShadow(
            color: fg.withValues(alpha: .06),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(22),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          onLongPress: onLongPress,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  children: [
                    AppSvg(
                      AppAssets.svgTable,
                      width: 22,
                      height: 22,
                      color: fg,
                    ),
                    const Spacer(),
                    AppStatusBadge(
                      label: floor.statusLabel,
                      color: fg,
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  name,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w800,
                        color: AppColors.navy,
                      ),
                ),
                if (typeLabel.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text(
                    typeLabel,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontFamily: Theme.of(context).textTheme.bodySmall?.fontFamily,
                      color: fg,
                      fontWeight: FontWeight.w700,
                      fontSize: 12.5,
                    ),
                  ),
                ],
                const SizedBox(height: 4),
                Text(
                  meta,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: AppColors.textSecondary,
                      ),
                ),
                if (floor.openSession != null) ...[
                  const SizedBox(height: 4),
                  Text(
                    '${floor.openSession!.guestCount} guest'
                    '${floor.openSession!.guestCount == 1 ? '' : 's'}'
                    '${(floor.openSession!.waiterName ?? '').trim().isEmpty ? '' : ' · ${floor.openSession!.waiterName}'}',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: AppColors.textSecondary,
                        ),
                  ),
                ],
                if (floor.currentAmount > 0) ...[
                  const SizedBox(height: 8),
                  Text(
                    currency.format(floor.currentAmount),
                    style: TextStyle(
                      color: fg,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ] else if (floor.isJoinedSecondary) ...[
                  const SizedBox(height: 8),
                  Text(
                    'Joined → T${floor.billingTableNumber}',
                    style: TextStyle(
                      color: fg,
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
