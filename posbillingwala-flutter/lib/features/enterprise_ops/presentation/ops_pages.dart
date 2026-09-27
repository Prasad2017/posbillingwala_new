import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/data/ops_local_store.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/data/warehouse_stock_store.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/domain/ops_providers.dart';

/* Shared CRUD list for ops entities (warehouse/brand/offer/…). */
class OpsEntityListPage extends ConsumerWidget {
  const OpsEntityListPage({
    super.key,
    required this.entity,
    required this.title,
    required this.subtitle,
    required this.icon,
    this.color = AppColors.primary,
    this.fields = const ['name'],
    this.statusActions = const [],
    this.embedded = false,
  });

  final String entity;
  final String title;
  final String subtitle;
  final IconData icon;
  final Color color;
  final List<String> fields;
  final List<String> statusActions;
  final bool embedded;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(opsListProvider(entity));

    final body = async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (rows) {
          if (rows.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(icon, size: 48, color: color.withValues(alpha: .55)),
                    const SizedBox(height: 12),
                    Text(
                      subtitle,
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        color: AppColors.navy.withValues(alpha: .55),
                        height: 1.4,
                      ),
                    ),
                  ],
                ),
              ),
            );
          }
          return ResponsiveScrollShell(
            dashboard: true,
            child: ListView.separated(
              padding: EdgeInsets.fromLTRB(
                AppBreakpoints.pagePaddingFor(context.widthClass),
                16,
                AppBreakpoints.pagePaddingFor(context.widthClass),
                100,
              ),
              itemCount: rows.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (context, index) {
                final row = rows[index];
                final name = (row['title'] ?? row['name'] ?? row['id'] ?? '')
                    .toString();
                final status = (row['status'] ?? '').toString();
                final detail = fields
                    .where((f) => f != 'name' && f != 'title')
                    .map((f) => '${_label(f)}: ${row[f] ?? '—'}')
                    .join(' · ');
                return Material(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(14),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(14),
                    onTap: () => _openEditor(context, ref, existing: row),
                    onLongPress: statusActions.isEmpty
                        ? null
                        : () => _statusSheet(context, ref, row),
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Row(
                        children: [
                          Container(
                            width: 44,
                            height: 44,
                            decoration: BoxDecoration(
                              color: color.withValues(alpha: .12),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Icon(icon, color: color),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  name,
                                  style: const TextStyle(
                                    fontFamily: AppFonts.family,
                                    fontWeight: FontWeight.w700,
                                    color: AppColors.navy,
                                  ),
                                ),
                                if (detail.isNotEmpty) ...[
                                  const SizedBox(height: 4),
                                  Text(
                                    detail,
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      fontFamily: AppFonts.family,
                                      fontSize: 12,
                                      color: AppColors.navy.withValues(alpha: .55),
                                    ),
                                  ),
                                ],
                              ],
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 4,
                            ),
                            decoration: BoxDecoration(
                              color: AppColors.navy.withValues(alpha: .06),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              status,
                              style: const TextStyle(
                                fontFamily: AppFonts.family,
                                fontSize: 11,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
          );
        },
      );

    if (embedded) {
      return Stack(
        children: [
          body,
          Positioned(
            right: 16,
            bottom: 16,
            child: FloatingActionButton.extended(
              onPressed: () => _openEditor(context, ref),
              icon: const Icon(Icons.add),
              label: const Text('Add'),
            ),
          ),
        ],
      );
    }

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        title: Text(title),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: () => ref.invalidate(opsListProvider(entity)),
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _openEditor(context, ref),
        icon: const Icon(Icons.add),
        label: const Text('Add'),
      ),
      body: body,
    );
  }

  Future<void> _statusSheet(
    BuildContext context,
    WidgetRef ref,
    Map<String, dynamic> row,
  ) async {
    final id = row['id']?.toString() ?? '';
    final picked = await showModalBottomSheet<String>(
      context: context,
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (final s in statusActions)
              ListTile(
                title: Text(s),
                onTap: () => Navigator.pop(ctx, s),
              ),
          ],
        ),
      ),
    );
    if (picked == null || id.isEmpty) return;
    await setOpsStatus(ref, entity: entity, id: id, status: picked);
  }

  Future<void> _openEditor(
    BuildContext context,
    WidgetRef ref, {
    Map<String, dynamic>? existing,
  }) async {
    final controllers = <String, TextEditingController>{
      for (final f in fields)
        f: TextEditingController(
          text: (existing?[f] ?? existing?['title'] ?? '').toString(),
        ),
    };
    // Prefer name field for title when present.
    if (fields.contains('name') && existing != null) {
      controllers['name']!.text =
          (existing['name'] ?? existing['title'] ?? '').toString();
    }

    final saved = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) {
        return Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: MediaQuery.viewInsetsOf(ctx).bottom + 16,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                existing == null ? 'Add $title' : 'Edit $title',
                style: const TextStyle(
                  fontFamily: AppFonts.family,
                  fontWeight: FontWeight.w700,
                  fontSize: 18,
                ),
              ),
              const SizedBox(height: 12),
              for (final f in fields) ...[
                TextField(
                  controller: controllers[f],
                  decoration: InputDecoration(
                    labelText: _label(f),
                    border: const OutlineInputBorder(),
                  ),
                  keyboardType: _isNumeric(f)
                      ? const TextInputType.numberWithOptions(decimal: true)
                      : TextInputType.text,
                ),
                const SizedBox(height: 10),
              ],
              FilledButton(
                onPressed: () async {
                  final map = <String, dynamic>{
                    ...?existing,
                    'id': existing?['id'] ??
                        '${entity}_${DateTime.now().millisecondsSinceEpoch}',
                    'status': existing?['status'] ?? 'ACTIVE',
                  };
                  for (final f in fields) {
                    map[f] = controllers[f]!.text.trim();
                  }
                  final titleVal = (map['name'] ?? map['title'] ?? map['id'])
                      .toString();
                  map['title'] = titleVal;
                  if (map['name'] == null || (map['name'] as String).isEmpty) {
                    map['name'] = titleVal;
                  }
                  await upsertOpsRecord(ref, entity: entity, row: map);
                  if (ctx.mounted) Navigator.pop(ctx, true);
                },
                child: const Text('Save'),
              ),
            ],
          ),
        );
      },
    );

    for (final c in controllers.values) {
      c.dispose();
    }
    if (saved == true) {
      ref.invalidate(opsListProvider(entity));
    }
  }

  static String _label(String key) {
    return key
        .replaceAllMapped(RegExp(r'([A-Z])'), (m) => ' ${m[1]}')
        .replaceAll('_', ' ')
        .trim()
        .split(' ')
        .map((w) => w.isEmpty ? w : '${w[0].toUpperCase()}${w.substring(1)}')
        .join(' ');
  }

  static bool _isNumeric(String key) {
    const nums = {
      'qty',
      'quantity',
      'stockQty',
      'reorderLevel',
      'discountPct',
      'maxUses',
      'mrp',
      'price',
      'amount',
      'refundAmount',
    };
    return nums.contains(key) || key.toLowerCase().contains('qty');
  }
}

class BrandsPage extends StatelessWidget {
  const BrandsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const OpsEntityListPage(
      entity: 'brand',
      title: 'Brands',
      subtitle: 'Brand master for retail and apparel catalogs.',
      icon: Icons.sell_rounded,
      color: Color(0xFF5B6CFF),
      fields: ['name', 'description'],
    );
  }
}

class WarehousesPage extends ConsumerWidget {
  const WarehousesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        backgroundColor: Colors.transparent,
        appBar: AppBar(
          title: const Text('Warehouses'),
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Warehouses'),
              Tab(text: 'Transfers'),
              Tab(text: 'Stock'),
            ],
          ),
        ),
        body: const TabBarView(
          children: [
            OpsEntityListPage(
              entity: 'warehouse',
              title: 'Warehouses',
              subtitle: 'Central / branch warehouses.',
              icon: Icons.warehouse_rounded,
              color: Color(0xFFE6A100),
              fields: ['name', 'code', 'address', 'type'],
              embedded: true,
            ),
            OpsEntityListPage(
              entity: 'transfer',
              title: 'Stock Transfers',
              subtitle: 'Request stock movement between warehouses.',
              icon: Icons.swap_horiz_rounded,
              color: AppColors.orange,
              fields: [
                'name',
                'fromWarehouse',
                'toWarehouse',
                'productName',
                'productId',
                'qty',
                'notes',
              ],
              statusActions: [
                'DRAFT',
                'REQUESTED',
                'APPROVED',
                'IN_TRANSIT',
                'RECEIVED',
                'CANCELLED',
              ],
              embedded: true,
            ),
            _WarehouseStockTab(),
          ],
        ),
      ),
    );
  }
}

class _WarehouseStockTab extends ConsumerWidget {
  const _WarehouseStockTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(authControllerProvider).session;
    if (session == null) {
      return const Center(child: Text('Sign in required'));
    }
    return FutureBuilder(
      future: WarehouseStockStore.flatBalances(session),
      builder: (context, snap) {
        if (!snap.hasData) {
          return const Center(child: CircularProgressIndicator());
        }
        final rows = snap.data!;
        if (rows.isEmpty) {
          return const Center(
            child: Text(
              'No warehouse stock yet.\nReceive transfers or GRN into a warehouse.',
              textAlign: TextAlign.center,
            ),
          );
        }
        return ListView.separated(
          padding: const EdgeInsets.all(16),
          itemCount: rows.length,
          separatorBuilder: (_, _) => const SizedBox(height: 8),
          itemBuilder: (context, i) {
            final r = rows[i];
            return ListTile(
              tileColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              title: Text(
                r.warehouse,
                style: const TextStyle(fontWeight: FontWeight.w700),
              ),
              subtitle: Text('Product #${r.productId}'),
              trailing: Text(
                r.qty.toStringAsFixed(r.qty == r.qty.roundToDouble() ? 0 : 2),
                style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16),
              ),
            );
          },
        );
      },
    );
  }
}

class OffersPage extends StatelessWidget {
  const OffersPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const OpsEntityListPage(
      entity: 'offer',
      title: 'Offers & Coupons',
      subtitle: 'Percentage / flat offers with coupon codes.',
      icon: Icons.local_offer_rounded,
      color: Color(0xFFE91E63),
      fields: [
        'name',
        'code',
        'type',
        'discountPct',
        'amount',
        'buyQty',
        'getQty',
        'maxUses',
        'validTill',
      ],
      statusActions: ['ACTIVE', 'INACTIVE', 'EXPIRED'],
    );
  }
}

class ApprovalsPage extends StatelessWidget {
  const ApprovalsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const OpsEntityListPage(
      entity: 'approval',
      title: 'Approvals',
      subtitle: 'Purchase, discount and transfer approval queue.',
      icon: Icons.fact_check_rounded,
      color: Color(0xFF00897B),
      fields: ['name', 'requestType', 'requestedBy', 'amount', 'notes'],
      statusActions: ['PENDING', 'APPROVED', 'REJECTED'],
    );
  }
}

class StockLotsPage extends StatelessWidget {
  const StockLotsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        backgroundColor: Colors.transparent,
        appBar: AppBar(
          title: const Text('Batch / Expiry / Serial'),
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Batches'),
              Tab(text: 'Serials'),
            ],
          ),
        ),
        body: const TabBarView(
          children: [
            OpsEntityListPage(
              entity: 'lot',
              title: 'Stock Lots',
              subtitle: 'Batch number, expiry and reorder levels.',
              icon: Icons.science_rounded,
              color: AppColors.green,
              fields: [
                'name',
                'productName',
                'batchNo',
                'expiryDate',
                'qty',
                'reorderLevel',
                'warehouse',
              ],
              statusActions: ['ACTIVE', 'EXPIRED', 'QUARANTINE'],
              embedded: true,
            ),
            OpsEntityListPage(
              entity: 'serial',
              title: 'Serial Numbers',
              subtitle: 'IMEI / serial tracking for high-value items.',
              icon: Icons.qr_code_2_rounded,
              fields: ['name', 'productName', 'serialNo', 'warehouse'],
              statusActions: ['IN_STOCK', 'SOLD', 'RETURNED'],
              embedded: true,
            ),
          ],
        ),
      ),
    );
  }
}

class ExchangePage extends StatelessWidget {
  const ExchangePage({super.key});

  @override
  Widget build(BuildContext context) {
    return const OpsEntityListPage(
      entity: 'return',
      title: 'Exchange / Return',
      subtitle: 'Invoice / barcode based returns and exchanges.',
      icon: Icons.swap_horiz_rounded,
      color: AppColors.orange,
      fields: [
        'name',
        'invoiceNo',
        'productName',
        'qty',
        'reason',
        'refundAmount',
        'exchangeProduct',
      ],
      statusActions: ['DRAFT', 'COMPLETED', 'CANCELLED'],
    );
  }
}

class AuditLogPage extends ConsumerWidget {
  const AuditLogPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(auditLogProvider);

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        title: const Text('Audit Log'),
        actions: [
          IconButton(
            onPressed: () => ref.invalidate(auditLogProvider),
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (rows) {
          if (rows.isEmpty) {
            return const Center(child: Text('No audit events yet.'));
          }
          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: rows.length,
            separatorBuilder: (_, _) => const SizedBox(height: 8),
            itemBuilder: (context, i) {
              final r = rows[i];
              return ListTile(
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                tileColor: Colors.white,
                leading: const Icon(Icons.history_rounded, color: AppColors.navy),
                title: Text(
                  (r['action'] ?? '').toString(),
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                subtitle: Text(
                  [
                    if ((r['entityType'] ?? '').toString().isNotEmpty)
                      '${r['entityType']} ${r['entityId'] ?? ''}'.trim(),
                    r['createdAt'] ?? '',
                  ].where((e) => e.toString().isNotEmpty).join(' · '),
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 12,
                    color: AppColors.navy.withValues(alpha: .55),
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }
}

class MaxDiscountSettingsPage extends ConsumerStatefulWidget {
  const MaxDiscountSettingsPage({super.key});

  @override
  ConsumerState<MaxDiscountSettingsPage> createState() =>
      _MaxDiscountSettingsPageState();
}

class _MaxDiscountSettingsPageState
    extends ConsumerState<MaxDiscountSettingsPage> {
  final _ctrl = TextEditingController();
  bool _busy = false;

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final async = ref.watch(maxDiscountPctProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Max Discount %')),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (pct) {
          if (_ctrl.text.isEmpty) {
            _ctrl.text = pct.toStringAsFixed(0);
          }
          return Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Staff without billing.max_discount cannot exceed this % on invoices.',
                  style: TextStyle(fontFamily: AppFonts.family, height: 1.4),
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: _ctrl,
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                  decoration: const InputDecoration(
                    labelText: 'Max discount %',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: _busy
                      ? null
                      : () async {
                          final session =
                              ref.read(authControllerProvider).session;
                          if (session == null) return;
                          setState(() => _busy = true);
                          final v = double.tryParse(_ctrl.text.trim()) ?? 100;
                          await OpsLocalStore.saveMaxDiscountPct(session, v);
                          ref.invalidate(maxDiscountPctProvider);
                          setState(() => _busy = false);
                          if (context.mounted) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('Saved')),
                            );
                          }
                        },
                  child: Text(_busy ? 'Saving…' : 'Save'),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
