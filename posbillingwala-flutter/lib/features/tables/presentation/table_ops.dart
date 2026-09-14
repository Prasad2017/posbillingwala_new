import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/tables/domain/tables_providers.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

FloorTableView? floorForTable(WidgetRef ref, String? tableNumber) {
  if (tableNumber == null || tableNumber.isEmpty) return null;
  for (final floor in ref.read(floorTablesProvider)) {
    if (floor.billingTableNumber == tableNumber ||
        floor.table.tableNumber == tableNumber) {
      return floor;
    }
  }
  return null;
}

Future<void> printLatestInvoiceDuplicate(
  BuildContext context,
  WidgetRef ref, {
  String? tableNumber,
  String? invoiceType,
}) async {
  final db = ref.read(appDatabaseProvider);
  Invoice? invoice;
  if (tableNumber != null && tableNumber.isNotEmpty) {
    invoice = await db.latestInvoiceForTable(tableNumber);
  } else if (invoiceType != null) {
    invoice = await db.latestInvoiceOfType(invoiceType);
  }
  if (!context.mounted) return;
  if (invoice == null) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(AppStrings.of(ref).noPreviousBill)),
    );
    return;
  }
  context.push('/print/bill/${invoice.invoiceId}?duplicate=1');
}

Future<void> editDiningGuestsWaiter(
  BuildContext context,
  WidgetRef ref,
  FloorTableView floor,
) async {
  final session = floor.openSession;
  if (session == null) return;
  final guests = TextEditingController(text: '${session.guestCount}');
  final waiter = TextEditingController(text: session.waiterName ?? '');
  final strings = AppStrings.of(ref);
  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      title: Text(strings.guestsWaiter),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          AppTextField(
            controller: guests,
            label: strings.guestCount,
            keyboardType: TextInputType.number,
          ),
          const SizedBox(height: 12),
          AppTextField(
            controller: waiter,
            label: strings.waiterName,
            textCapitalization: TextCapitalization.words,
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context, false),
          child: Text(strings.cancel),
        ),
        AppButton(
          label: strings.save,
          expanded: false,
          onPressed: () => Navigator.pop(context, true),
        ),
      ],
    ),
  );
  final guestCount = int.tryParse(guests.text.trim()) ?? session.guestCount;
  final waiterName = waiter.text.trim();
  guests.dispose();
  waiter.dispose();
  if (ok != true) return;
  await ref.read(tablesControllerProvider.notifier).updateSessionMeta(
        sessionId: session.sessionId,
        guestCount: guestCount,
        waiterName: waiterName,
      );
  if (!context.mounted) return;
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(content: Text(AppStrings.of(ref).tableDetailsSaved)),
  );
}

Future<void> showPosTableOverflow(
  BuildContext context,
  WidgetRef ref,
  FloorTableView floor,
) async {
  final all = ref.read(floorTablesProvider);
  final strings = AppStrings.of(ref);
  final more = await showModalBottomSheet<String>(
    context: context,
    showDragHandle: true,
    builder: (context) => SafeArea(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ListTile(title: Text(strings.tableActions)),
          const Divider(height: 1),
          ListTile(
            title: Text(strings.guestsWaiter),
            onTap: () => Navigator.pop(context, 'meta'),
          ),
          ListTile(
            title: Text(strings.joinTable),
            onTap: () => Navigator.pop(context, 'join'),
          ),
          ListTile(
            title: Text(strings.transferTable),
            onTap: () => Navigator.pop(context, 'transfer'),
          ),
          ListTile(
            title: Text(strings.moveItemsTable),
            onTap: () => Navigator.pop(context, 'move'),
          ),
          ListTile(
            title: Text(strings.splitBill),
            onTap: () => Navigator.pop(context, 'split_bill'),
          ),
          if (floor.openSession?.sessionStatus != 'HOLD')
            ListTile(
              title: Text(strings.holdTable),
              onTap: () => Navigator.pop(context, 'hold'),
            ),
          if (floor.openSession?.sessionStatus == 'HOLD')
            ListTile(
              title: Text(strings.resumeTable),
              onTap: () => Navigator.pop(context, 'resume'),
            ),
          ListTile(
            title: Text(strings.markBillRequested),
            onTap: () => Navigator.pop(context, 'bill'),
          ),
          ListTile(
            title: Text(strings.duplicatePrintLastBill),
            onTap: () => Navigator.pop(context, 'print'),
          ),
          if (floor.openSession != null &&
              (floor.joinedLabel?.contains('+') ?? false) &&
              !floor.isJoinedSecondary)
            ListTile(
              title: Text(strings.splitJoined),
              onTap: () => Navigator.pop(context, 'split'),
            ),
          const SizedBox(height: 8),
        ],
      ),
    ),
  );
  if (!context.mounted || more == null) return;
  await handleTableOpsAction(context, ref, floor, all, more);
}

Future<void> handleTableOpsAction(
  BuildContext context,
  WidgetRef ref,
  FloorTableView floor,
  List<FloorTableView> all,
  String action,
) async {
  if (action == 'meta') {
    await editDiningGuestsWaiter(context, ref, floor);
    return;
  }

  if (action == 'print') {
    await printLatestInvoiceDuplicate(
      context,
      ref,
      tableNumber: floor.billingTableNumber,
    );
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
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
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
    await ref.read(tablesControllerProvider.notifier).joinTables(
          primaryTable: floor.billingTableNumber,
          secondaryTable: secondary.table.tableNumber,
        );
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Joined with T${secondary.table.tableNumber}')),
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
  late final Set<int> tableOpsSelected;

  @override
  void initState() {
    super.initState();
    tableOpsSelected = {for (var i = 0; i < widget.items.length; i++) i};
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
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
                    value: tableOpsSelected.contains(index),
                    title: Text(item.productName),
                    subtitle: Text('Qty ${item.quantity}'),
                    onChanged: (on) => setState(() {
                      if (on == true) {
                        tableOpsSelected.add(index);
                      } else {
                        tableOpsSelected.remove(index);
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
                      Navigator.pop(context, [
                        for (final i in tableOpsSelected) widget.items[i],
                      ]);
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
