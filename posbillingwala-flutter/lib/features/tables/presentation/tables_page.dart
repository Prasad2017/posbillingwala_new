import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/app_states.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/tables/domain/tables_providers.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';
import 'package:pos_billingwala_v2/features/tables/presentation/table_ops.dart';

class TablesPage extends ConsumerStatefulWidget {
  const TablesPage({super.key});

  @override
  ConsumerState<TablesPage> createState() => _TablesPageState();
}

class _TablesPageState extends ConsumerState<TablesPage> {
  int? _selectedAreaId;

  @override
  Widget build(BuildContext context) {
    final floor = ref.watch(floorTablesProvider);
    final strings = AppStrings.of(ref);
    final areasAsync = ref.watch(diningAreasProvider);
    final syncState = ref.watch(tablesControllerProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final isSyncing = syncState.isLoading;
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

    final filtered = _selectedAreaId == null
        ? floor
        : floor.where((t) => t.table.areaId == _selectedAreaId).toList();

    return Scaffold(
      appBar: AppBar(
        title: Text(strings.dineInTables),
        actions: [
          IconButton(
            tooltip: strings.syncTables,
            onPressed: isSyncing
                ? null
                : () =>
                    ref.read(tablesControllerProvider.notifier).syncTables(),
            icon: isSyncing
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : const AppSvg(
                    AppAssets.svgCloudDownload,
                    width: 22,
                    height: 22,
                    color: Colors.white,
                  ),
          ),
        ],
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
                  _AreaChip(
                    label: strings.allAreas,
                    selected: _selectedAreaId == null,
                    onTap: () => setState(() => _selectedAreaId = null),
                  ),
                  ...areas.map(
                    (a) => _AreaChip(
                      label: a.areaName.isEmpty ? 'Area ${a.areaId}' : a.areaName,
                      selected: _selectedAreaId == a.areaId,
                      onTap: () => setState(() => _selectedAreaId = a.areaId),
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
                _LegendDot(label: strings.tableAvailable, color: AppColors.success),
                _LegendDot(label: strings.tableRunning, color: AppColors.warning),
                _LegendDot(label: strings.tableHold, color: AppColors.orange),
                _LegendDot(label: strings.tableBill, color: AppColors.purple),
                _LegendDot(label: strings.tableBlocked, color: AppColors.red),
                _LegendDot(label: strings.tableReserved, color: AppColors.teal),
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
                    actionLabel: isSyncing ? null : strings.syncTables,
                    onAction: isSyncing
                        ? null
                        : () => ref
                            .read(tablesControllerProvider.notifier)
                            .syncTables(),
                  )
                : LayoutBuilder(
                    builder: (context, constraints) {
                      final widthClass =
                          AppBreakpoints.ofWidth(constraints.maxWidth);
                      final cols =
                          AppBreakpoints.tableColumnsFor(widthClass);
                      return ResponsiveScrollShell(
                        dashboard: true,
                        child: GridView.builder(
                          padding: EdgeInsets.fromLTRB(
                            AppBreakpoints.pagePaddingFor(widthClass),
                            0,
                            AppBreakpoints.pagePaddingFor(widthClass),
                            24,
                          ),
                          gridDelegate:
                              SliverGridDelegateWithFixedCrossAxisCount(
                            crossAxisCount: cols,
                            mainAxisSpacing: 12,
                            crossAxisSpacing: 12,
                            childAspectRatio:
                                widthClass == AppWidthClass.compact
                                    ? 1.05
                                    : 1.15,
                          ),
                          itemCount: filtered.length,
                          itemBuilder: (context, index) {
                            final item = filtered[index];
                            return _TableCard(
                              floor: item,
                              currency: currency,
                              onTap: () => _onTableTap(context, ref, item),
                              onLongPress: () => _onTableActions(
                                context,
                                ref,
                                item,
                                floor,
                              ),
                            );
                          },
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }

  Future<void> _onTableTap(
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

  Future<void> _onTableActions(
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
      final more = await _showAdvancedTableActions(context, floor);
      if (!context.mounted || more == null) return;
      await _handleTableAction(context, ref, floor, all, more);
      return;
    }

    if (action == 'add_items' || action == 'view_bill' || action == 'open') {
      await _onTableTap(context, ref, floor);
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

    await _handleTableAction(context, ref, floor, all, action);
  }

  Future<String?> _showAdvancedTableActions(
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

  Future<void> _handleTableAction(
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
      await _onTableTap(context, ref, floor);
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
            builder: (context) => _MoveItemsSheet(items: items),
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

class _AreaChip extends StatelessWidget {
  const _AreaChip({
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

class _LegendDot extends StatelessWidget {
  const _LegendDot({required this.label, required this.color});

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

class _TableCard extends StatelessWidget {
  const _TableCard({
    required this.floor,
    required this.currency,
    required this.onTap,
    required this.onLongPress,
  });

  final FloorTableView floor;
  final NumberFormat currency;
  final VoidCallback onTap;
  final VoidCallback onLongPress;

  Color get _bg {
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

  Color get _fg {
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

    return Container(
      decoration: BoxDecoration(
        color: _bg,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: _fg.withValues(alpha: .18)),
        boxShadow: [BoxShadow(color: _fg.withValues(alpha: .06), blurRadius: 16, offset: const Offset(0, 6))],
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
            children: [
              Row(
                children: [
                  AppSvg(
                    AppAssets.svgTable,
                    width: 22,
                    height: 22,
                    color: _fg,
                  ),
                  const Spacer(),
                  AppStatusBadge(
                    label: floor.statusLabel,
                    color: _fg,
                  ),
                ],
              ),
              const Spacer(),
              Text(
                name,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
              ),
              const SizedBox(height: 4),
              Text(
                floor.joinedLabel ??
                    (table.capacity > 0
                        ? 'Seats ${table.capacity}'
                        : 'Table ${table.tableNumber}'),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.bodySmall,
              ),
              if (floor.openSession != null) ...[
                const SizedBox(height: 4),
                Text(
                  '${floor.openSession!.guestCount} guest'
                  '${floor.openSession!.guestCount == 1 ? '' : 's'}'
                  '${(floor.openSession!.waiterName ?? '').trim().isEmpty ? '' : ' · ${floor.openSession!.waiterName}'}',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
              if (floor.currentAmount > 0) ...[
                const SizedBox(height: 8),
                Text(
                  currency.format(floor.currentAmount),
                  style: TextStyle(
                    color: _fg,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ] else if (floor.isJoinedSecondary) ...[
                const SizedBox(height: 8),
                Text(
                  'Joined → T${floor.billingTableNumber}',
                  style: TextStyle(
                    color: _fg,
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

class _MoveItemsSheet extends StatefulWidget {
  const _MoveItemsSheet({required this.items});

  final List<CartItem> items;

  @override
  State<_MoveItemsSheet> createState() => _MoveItemsSheetState();
}

class _MoveItemsSheetState extends State<_MoveItemsSheet> {
  late final Set<int> _selected;

  @override
  void initState() {
    super.initState();
    _selected = {for (var i = 0; i < widget.items.length; i++) i};
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
                    value: _selected.contains(index),
                    title: Text(item.productName),
                    subtitle: Text('Qty ${item.quantity}'),
                    onChanged: (on) => setState(() {
                      if (on == true) {
                        _selected.add(index);
                      } else {
                        _selected.remove(index);
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
                        for (final i in _selected) widget.items[i],
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
