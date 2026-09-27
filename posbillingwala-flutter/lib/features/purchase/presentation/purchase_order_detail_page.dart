import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/utils/money_format.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/purchase_document.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/purchase_providers.dart';

class PurchaseOrderDetailPage extends ConsumerStatefulWidget {
  const PurchaseOrderDetailPage({super.key, required this.orderId});

  final String orderId;

  @override
  ConsumerState<PurchaseOrderDetailPage> createState() =>
      PurchaseOrderDetailPageState();
}

class PurchaseOrderDetailPageState
    extends ConsumerState<PurchaseOrderDetailPage> {
  PurchaseDocument? doc;
  var loading = true;
  final receiveCtrls = <int, TextEditingController>{};
  final grnRef = TextEditingController();
  final grnNotes = TextEditingController();
  var receiving = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(load);
  }

  @override
  void dispose() {
    for (final c in receiveCtrls.values) {
      c.dispose();
    }
    grnRef.dispose();
    grnNotes.dispose();
    super.dispose();
  }

  Future<void> load() async {
    setState(() => loading = true);
    await ref.read(purchaseDocumentsProvider.future);
    final docs = ref.read(purchaseDocumentsProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <PurchaseDocument>[],
        );
    PurchaseDocument? match;
    for (final d in docs) {
      if (d.id == widget.orderId) {
        match = d;
        break;
      }
    }
    if (!mounted) return;
    for (final c in receiveCtrls.values) {
      c.dispose();
    }
    receiveCtrls.clear();
    if (match != null) {
      for (final line in match.lines) {
        final pending = line.pendingQty;
        receiveCtrls[line.productId] = TextEditingController(
          text: pending > 0 ? pending.toString() : '0',
        );
      }
    }
    setState(() {
      doc = match;
      loading = false;
    });
  }

  Future<void> receive() async {
    final current = doc;
    if (current == null) return;
    final lines = <PurchaseLine>[];
    for (final line in current.lines) {
      final ctrl = receiveCtrls[line.productId];
      final qty = double.tryParse(ctrl?.text.trim() ?? '') ?? 0;
      if (qty <= 0) continue;
      if (qty > line.pendingQty + 0.0001) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Cannot receive more than pending for ${line.productName}',
            ),
          ),
        );
        return;
      }
      lines.add(
        PurchaseLine(
          productId: line.productId,
          productName: line.productName,
          quantity: qty,
          unitCost: line.unitCost,
          unit: line.unit,
        ),
      );
    }
    setState(() => receiving = true);
    try {
      final grn = await ref.read(purchaseControllerProvider.notifier).receiveGrn(
            purchaseOrderId: current.id,
            receiveLines: lines,
            notes: grnNotes.text,
            referenceNo: grnRef.text,
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${grn.docNo} received — stock updated')),
      );
      await load();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$e'.replaceFirst('Bad state: ', ''))),
      );
    } finally {
      if (mounted) setState(() => receiving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }
    final current = doc;
    if (current == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Purchase Order')),
        body: const Center(child: Text('Order not found')),
      );
    }
    final money = MoneyFormat.inrSpaced;
    final date = current.createdAt;

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        title: Text(current.docNo),
        actions: [
          if (current.status != PurchaseDocStatus.received &&
              current.status != PurchaseDocStatus.cancelled)
            IconButton(
              tooltip: 'Edit',
              onPressed: () async {
                await context.push('/purchase/orders/${current.id}/edit');
                await load();
              },
              icon: const Icon(Icons.edit_rounded),
            ),
        ],
      ),
      body: ResponsivePageBody(
        dashboard: true,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.primarySoft,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: AppColors.border),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    current.vendorName,
                    style: const TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w800,
                      fontSize: 17,
                      color: AppColors.navy,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '${current.status.label}'
                    '${date == null ? '' : ' · ${DateFormat('dd MMM yyyy').format(date)}'}'
                    '${current.referenceNo.isEmpty ? '' : ' · Ref ${current.referenceNo}'}',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      color: AppColors.navy.withValues(alpha: .6),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Total ${money.format(current.subTotal)}',
                    style: const TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w800,
                      fontSize: 18,
                      color: AppColors.primary,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'ITEMS',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 11,
                letterSpacing: 1,
                fontWeight: FontWeight.w600,
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: 8),
            for (final line in current.lines)
              Container(
                margin: const EdgeInsets.only(bottom: 8),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.glassSolid,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.border),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            line.productName,
                            style: const TextStyle(
                              fontFamily: AppFonts.family,
                              fontWeight: FontWeight.w700,
                              color: AppColors.navy,
                            ),
                          ),
                          Text(
                            'Ordered ${line.quantity}'
                            ' · Received ${line.receivedQty}'
                            ' · Pending ${line.pendingQty}'
                            ' @ ${money.format(line.unitCost)}',
                            style: TextStyle(
                              fontFamily: AppFonts.family,
                              fontSize: 12,
                              color: AppColors.navy.withValues(alpha: .55),
                            ),
                          ),
                        ],
                      ),
                    ),
                    Text(
                      money.format(line.lineTotal),
                      style: const TextStyle(
                        fontFamily: AppFonts.family,
                        fontWeight: FontWeight.w800,
                        color: AppColors.navy,
                      ),
                    ),
                  ],
                ),
              ),
            if (current.canReceive) ...[
              const SizedBox(height: 18),
              const Text(
                'RECEIVE STOCK (GRN)',
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 11,
                  letterSpacing: 1,
                  fontWeight: FontWeight.w600,
                  color: AppColors.textSecondary,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'Enter qty to receive. Stock is added to inventory immediately.',
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 12.5,
                  color: AppColors.navy.withValues(alpha: .55),
                ),
              ),
              const SizedBox(height: 10),
              AppTextField(
                controller: grnRef,
                label: 'Supplier invoice / challan no',
              ),
              const SizedBox(height: 10),
              AppTextField(
                controller: grnNotes,
                label: 'GRN notes',
                maxLines: 2,
              ),
              const SizedBox(height: 10),
              for (final line in current.lines)
                if (line.pendingQty > 0)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: TextField(
                      controller: receiveCtrls[line.productId],
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                      inputFormatters: [
                        FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                      ],
                      decoration: InputDecoration(
                        labelText: 'Receive · ${line.productName}',
                        helperText: 'Pending ${line.pendingQty}',
                      ),
                    ),
                  ),
              const SizedBox(height: 8),
              FilledButton.icon(
                onPressed: receiving ? null : receive,
                icon: const Icon(Icons.move_to_inbox_rounded),
                label: Text(receiving ? 'Receiving…' : 'Receive & update stock'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(52),
                  backgroundColor: AppColors.green,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
