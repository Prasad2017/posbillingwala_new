import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/features/tables/domain/tables_providers.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';

/* Split an open dine-in bill into shares (equal / by item / by amount). */
class SplitBillPage extends ConsumerStatefulWidget {
  const SplitBillPage({
    super.key,
    required this.tableNumber,
    required this.sessionId,
  });

  final String tableNumber;
  final int sessionId;

  @override
  ConsumerState<SplitBillPage> createState() => SplitBillPageState();
}

class SplitBillPageState extends ConsumerState<SplitBillPage> {
  int tab = 0; /* 0 equal, 1 items, 2 amount */
  int equalParts = 2;
  final amountCtrl = TextEditingController();
  final splitBillPageSelected = <int>{};
  bool busy = false;

  @override
  void dispose() {
    amountCtrl.dispose();
    super.dispose();
  }

  double cartTotal(List<CartItem> items) {
    var t = 0.0;
    for (final i in items) {
      t += i.unitPrice * i.quantity * (1 + i.gstPercent / 100);
    }
    return double.parse(t.toStringAsFixed(2));
  }

  Future<void> recordShares(List<double> shares) async {
    if (shares.isEmpty) return;
    setState(() => busy = true);
    try {
      final db = ref.read(appDatabaseProvider);
      for (final share in shares) {
        if (share <= 0) continue;
        await db.addSessionPaidAmount(
          sessionId: widget.sessionId,
          amount: share,
        );
      }
      final session = await db.getDiningSessionById(widget.sessionId);
      if (session != null) {
        await ref
            .read(tablesControllerProvider.notifier)
            .uploadDiningSessionIfOnline(session);
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            '${shares.length} share(s) recorded. Use PAY on the table to close when fully paid.',
          ),
        ),
      );
      Navigator.pop(context, true);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$e')),
      );
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  List<double> equalShares(double total, int parts) {
    if (parts < 2) return [total];
    final paise = (total * 100).round();
    final base = paise ~/ parts;
    final rem = paise % parts;
    return List.generate(parts, (i) {
      final p = base + (i < rem ? 1 : 0);
      return p / 100.0;
    });
  }

  @override
  Widget build(BuildContext context) {
    final cartAsync = ref.watch(allCartItemsProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');

    return Scaffold(
      appBar: AppBar(title: Text('Split Bill • T${widget.tableNumber}')),
      body: Column(children: [
        Expanded(child: cartAsync.when(
        data: (all) {
          final items =
              all.where((e) => e.cartScope == widget.tableNumber).toList();
          final total = cartTotal(items);
          return Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                child: AppCard(
            accentColor: AppColors.primary,
            color: AppColors.primaryLight,
            padding: EdgeInsets.zero,
            child: ListTile(
                    leading: const AppModuleIcon(svgPath: AppAssets.svgBill, color: AppColors.primary, size: 48),
                    title: const Text(
                      'Bill total',
                      style: TextStyle(fontWeight: FontWeight.w700),
                    ),
                    trailing: Text(
                      currency.format(total),
                      style: const TextStyle(
                        fontWeight: FontWeight.w800,
                        color: AppColors.primary,
                      ),
                    ),
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(16),
                child: SegmentedButton<int>(
                  segments: const [
                    ButtonSegment(value: 0, label: Text('Equal')),
                    ButtonSegment(value: 1, label: Text('By item')),
                    ButtonSegment(value: 2, label: Text('By amount')),
                  ],
                  selected: {tab},
                  onSelectionChanged: (v) => setState(() => tab = v.first),
                ),
              ),
              Expanded(
                child: switch (tab) {
                  1 => byItem(items, currency),
                  2 => byAmount(total, currency),
                  _ => splitBillPageEqual(total, currency),
                },
              ),
            ],
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
      )),
      ]),
    );
  }

  Widget splitBillPageEqual(double total, NumberFormat currency) {
    final shares = equalShares(total, equalParts);
    return ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
      padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            0,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            24),
      children: [
        Row(
          children: [
            const Text('Number of shares'),
            const Spacer(),
            IconButton(
              onPressed: equalParts <= 2
                  ? null
                  : () => setState(() => equalParts--),
              icon: const Icon(Icons.remove_circle_outline),
            ),
            Text(
              '$equalParts',
              style: const TextStyle(fontWeight: FontWeight.w800),
            ),
            IconButton(
              onPressed: equalParts >= 20
                  ? null
                  : () => setState(() => equalParts++),
              icon: const Icon(Icons.add_circle_outline),
            ),
          ],
        ),
        const SizedBox(height: 8),
        ...shares.asMap().entries.map(
              (e) => ListTile(
                leading: CircleAvatar(child: Text('${e.key + 1}')),
                title: Text('Share ${e.key + 1}'),
                trailing: Text(
                  currency.format(e.value),
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
            ),
        const SizedBox(height: 16),
        AppButton(
            label: 'Record equal shares',
            isLoading: busy || total <= 0,
            onPressed: () => recordShares(shares),
          ),
      ],
    ),
      );
  }

  Widget byItem(List<CartItem> items, NumberFormat currency) {
    if (items.length < 2) {
      return const Center(
        child: Text('Need at least 2 cart lines to split by item'),
      );
    }
    double sumSelected() {
      var t = 0.0;
      for (final i in splitBillPageSelected) {
        if (i < 0 || i >= items.length) continue;
        final it = items[i];
        t += it.unitPrice * it.quantity * (1 + it.gstPercent / 100);
      }
      return double.parse(t.toStringAsFixed(2));
    }

    final total = cartTotal(items);
    final bill1 = sumSelected();
    final bill2 = double.parse((total - bill1).toStringAsFixed(2));

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
      children: [
        const Text('Select items for Bill 1 (rest â†’ Bill 2)'),
        const SizedBox(height: 8),
        ...items.asMap().entries.map((e) {
          final it = e.value;
          final line =
              it.unitPrice * it.quantity * (1 + it.gstPercent / 100);
          return CheckboxListTile(
            value: splitBillPageSelected.contains(e.key),
            title: Text(it.productName),
            subtitle: Text('Qty ${it.quantity}'),
            secondary: Text(currency.format(line)),
            onChanged: (on) => setState(() {
              if (on == true) {
                splitBillPageSelected.add(e.key);
              } else {
                splitBillPageSelected.remove(e.key);
              }
            }),
          );
        }),
        const Divider(),
        ListTile(
          title: const Text('Bill 1'),
          trailing: Text(currency.format(bill1)),
        ),
        ListTile(
          title: const Text('Bill 2'),
          trailing: Text(currency.format(bill2)),
        ),
        AppButton(
            label: 'Record 2 shares',
            isLoading: busy,
            onPressed: busy ||
                  splitBillPageSelected.isEmpty ||
                  splitBillPageSelected.length == items.length ||
                  bill1 <= 0 ||
                  bill2 <= 0
              ? null
              : () => recordShares([bill1, bill2]),
          ),
      ],
    );
  }

  Widget byAmount(double total, NumberFormat currency) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
      children: [
        Text('Enter share amounts separated by commas (must sum to ${currency.format(total)})'),
        const SizedBox(height: 12),
        AppTextField(
                      controller: amountCtrl,
                      label: 'e.g. 200, 150, 50',
                      keyboardType: TextInputType.text,
                      inputFormatters: [
            FilteringTextInputFormatter.allow(RegExp(r'[0-9., ]')),
          ],
                    ),
        const SizedBox(height: 16),
        AppButton(
            label: 'Record amount shares',
            isLoading: busy,
            onPressed: () {
                  final parts = amountCtrl.text
                      .split(',')
                      .map((e) => double.tryParse(e.trim()) ?? 0)
                      .where((e) => e > 0)
                      .toList();
                  if (parts.length < 2) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('Enter at least 2 amounts'),
                      ),
                    );
                    return;
                  }
                  final sum = parts.fold<double>(0, (a, b) => a + b);
                  if ((sum - total).abs() > 0.05) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text(
                          'Sum ${currency.format(sum)} must match ${currency.format(total)}',
                        ),
                      ),
                    );
                    return;
                  }
                  recordShares(parts);
                },
          ),
      ],
    );
  }
}
