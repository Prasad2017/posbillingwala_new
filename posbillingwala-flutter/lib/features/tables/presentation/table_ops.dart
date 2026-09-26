import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_mode.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/payment_mode_sheet.dart';
import 'package:pos_billingwala_v2/features/tables/domain/tables_providers.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

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
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).noPreviousBill)));
    return;
  }
  context.push('/print/bill/${invoice.invoiceId}?duplicate=1');
}

Future<void> retryFailedBillPrint(
  BuildContext context,
  WidgetRef ref,
  FloorTableView floor,
) async {
  final db = ref.read(appDatabaseProvider);
  final failed =
      floor.unpaidInvoice ??
      await db.latestFailedPrintInvoiceForTable(floor.billingTableNumber);
  if (!context.mounted) return;
  if (failed == null) {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('No failed bill to reprint')),
    );
    return;
  }
  await db.updateInvoiceBillPrintStatus(
    invoiceNumber: failed.invoiceNumber,
    billPrintStatus: 'PENDING',
  );
  if (!context.mounted) return;
  context.push('/print/bill/${failed.invoiceId}?duplicate=1');
}

Future<void> settleUnpaidTableInvoice(
  BuildContext context,
  WidgetRef ref,
  FloorTableView floor,
) async {
  final db = ref.read(appDatabaseProvider);
  final unpaidList = await db.unpaidInvoicesForTable(floor.billingTableNumber);
  final unpaid = floor.unpaidInvoice ??
      (unpaidList.isEmpty ? null : unpaidList.first);
  if (!context.mounted) return;
  if (unpaid == null) {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('No unpaid bill for this table')),
    );
    return;
  }

  final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
  final settled = await showModalBottomSheet<PaymentTender?>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    builder: (ctx) {
      PaymentTender? result;
      return PaymentModeSheet(
        totalAmount: unpaid.totalAmount,
        currency: currency,
        initialMode: PaymentMode.cash,
        initialCash: unpaid.totalAmount,
        initialUpi: 0,
        onContinue: (mode, cash, upi) {
          result = PaymentTender.resolve(
            mode: mode,
            totalAmount: unpaid.totalAmount,
            cashAmount: cash,
            upiAmount: upi,
          );
          Navigator.pop(ctx, result);
        },
      );
    },
  );
  if (settled == null || !context.mounted) return;
  if (!settled.isValidFor(unpaid.totalAmount)) {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Cash + UPI must equal bill total')),
    );
    return;
  }

  await ref
      .read(tablesControllerProvider.notifier)
      .settleUnpaidInvoice(
        floor: floor,
        invoice: unpaid,
        paymentMode: settled.mode.label,
        cashAmount: settled.cashAmount,
        upiAmount: settled.upiAmount,
      );
  if (!context.mounted) return;
  ScaffoldMessenger.of(
    context,
  ).showSnackBar(const SnackBar(content: Text('Payment saved')));
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
  disposeTextControllers([guests, waiter]);
  if (ok != true) return;
  await ref
      .read(tablesControllerProvider.notifier)
      .updateSessionMeta(
        sessionId: session.sessionId,
        guestCount: guestCount,
        waiterName: waiterName,
      );
  if (!context.mounted) return;
  ScaffoldMessenger.of(
    context,
  ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).tableDetailsSaved)));
}

/* Android DineInOpsUi.showTableActionsMenu — long-press more actions. */
Future<void> showPosTableOverflow(
  BuildContext context,
  WidgetRef ref,
  FloorTableView floor,
) async {
  if (floor.status == FloorTableStatus.blocked) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('Table is blocked')));
    return;
  }

  final all = ref.read(floorTablesProvider);
  final strings = AppStrings.of(ref);
  final joined =
      floor.openSession != null &&
      (floor.joinedLabel?.contains('+') ?? false) &&
      !floor.isJoinedSecondary;

  final more = await showAppActionSheet(
    context: context,
    title: '${strings.tableActions} • T${floor.billingTableNumber}',
    actions: [
      AppSheetAction(label: strings.joinTable, value: 'join'),
      AppSheetAction(label: strings.markBillRequested, value: 'bill'),
      if (joined) AppSheetAction(label: strings.splitJoined, value: 'split'),
      AppSheetAction(label: strings.moveItemsTable, value: 'move'),
      AppSheetAction(label: strings.transferTable, value: 'transfer'),
      AppSheetAction(label: strings.splitBill, value: 'split_bill'),
      AppSheetAction(label: strings.duplicatePrintLastBill, value: 'print'),
      AppSheetAction(label: 'Hold / Save', value: 'hold'),
      AppSheetAction(label: strings.guestsWaiter, value: 'meta'),
    ],
  );
  if (!context.mounted || more == null) return;
  await handleTableOpsAction(context, ref, floor, all, more);
}

Future<FloorTableView?> pickTargetTable({
  required BuildContext context,
  required List<FloorTableView> candidates,
  required String title,
}) {
  if (candidates.isEmpty) return Future.value(null);
  return showAppActionSheet(
    context: context,
    title: title,
    actions: [
      for (final c in candidates)
        AppSheetAction(
          label: c.table.displayName.isEmpty
              ? 'Table ${c.table.tableNumber} — ${c.statusLabel}'
              : '${c.table.displayName} — ${c.statusLabel}',
          value: c.table.tableNumber,
        ),
    ],
  ).then((value) {
    if (value == null) return null;
    for (final c in candidates) {
      if (c.table.tableNumber == value) return c;
    }
    return null;
  });
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
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('Joined tables split')));
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

  if (action == 'hold') {
    await ref.read(tablesControllerProvider.notifier).softHoldTable(floor);
    if (!context.mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('Table held')));
    return;
  }

  if (action == 'bill') {
    await ref.read(tablesControllerProvider.notifier).markBillRequested(floor);
    if (!context.mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('Bill requested')));
    return;
  }

  if (action == 'transfer') {
    final candidates = all
        .where(
          (t) =>
              t.table.tableNumber != floor.billingTableNumber &&
              t.status == FloorTableStatus.available,
        )
        .toList();
    if (candidates.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No available table to transfer')),
      );
      return;
    }
    final target = await pickTargetTable(
      context: context,
      candidates: candidates,
      title: 'Transfer to',
    );
    if (target == null || !context.mounted) return;
    try {
      await ref
          .read(tablesControllerProvider.notifier)
          .transferTable(
            fromTable: floor.billingTableNumber,
            toTable: target.table.tableNumber,
          );
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Transferred to T${target.table.tableNumber}'),
        ),
      );
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
    return;
  }

  if (action == 'move') {
    /* Android: any non-blocked target. */
    final candidates = all
        .where(
          (t) =>
              t.table.tableNumber != floor.billingTableNumber &&
              t.status != FloorTableStatus.blocked,
        )
        .toList();
    if (candidates.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('No target table')));
      return;
    }
    final items = await ref
        .read(appDatabaseProvider)
        .getCartItems(cartScope: floor.billingTableNumber);
    if (items.isEmpty) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('No items to move')));
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
    final target = await pickTargetTable(
      context: context,
      candidates: candidates,
      title: 'Move items to',
    );
    if (target == null || !context.mounted) return;
    try {
      await ref
          .read(tablesControllerProvider.notifier)
          .moveItems(
            fromTable: floor.billingTableNumber,
            toTable: target.table.tableNumber,
            items: selected,
          );
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Items moved to T${target.table.tableNumber}'),
        ),
      );
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
    return;
  }

  if (action == 'join') {
    /* Android: any non-blocked table (not only available). */
    final candidates = all
        .where(
          (t) =>
              t.table.tableNumber != floor.billingTableNumber &&
              t.status != FloorTableStatus.blocked,
        )
        .toList();
    if (candidates.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No tables available to join')),
      );
      return;
    }
    final secondary = await pickTargetTable(
      context: context,
      candidates: candidates,
      title: 'Join with',
    );
    if (secondary == null || !context.mounted) return;
    await ref
        .read(tablesControllerProvider.notifier)
        .joinTables(
          primaryTable: floor.billingTableNumber,
          secondaryTable: secondary.table.tableNumber,
        );
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'Joined T${floor.billingTableNumber} + T${secondary.table.tableNumber}',
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
