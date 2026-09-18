import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_period_controls.dart';

class StaffSalesRow {
  const StaffSalesRow({
    required this.staffId,
    required this.staffName,
    required this.billCount,
    required this.totalSales,
  });

  final String staffId;
  final String staffName;
  final int billCount;
  final double totalSales;
}

final staffWiseSalesProvider = Provider<AsyncValue<List<StaffSalesRow>>>((ref) {
  final invoicesAsync = ref.watch(periodInvoicesProvider);
  return invoicesAsync.when(
    loading: () => const AsyncLoading(),
    error: (e, st) => AsyncError(e, st),
    data: (invoices) {
      final map = <String, StaffSalesRow>{};
      for (final Invoice invoice in invoices) {
        if (invoice.invoiceOrderStatus == 'cancelled' ||
            invoice.invoiceOrderStatus == 'refunded') {
          continue;
        }
        final id = '${invoice.createdByStaffId ?? 0}';
        final name = invoice.createdByStaffName.trim().isNotEmpty
            ? invoice.createdByStaffName.trim()
            : (invoice.createdByStaffId == null || invoice.createdByStaffId == 0
                  ? 'Unassigned'
                  : 'Staff #$id');
        final key = id == '0' || id == 'null' ? '_none' : id;
        final existing = map[key];
        if (existing == null) {
          map[key] = StaffSalesRow(
            staffId: key == '_none' ? '' : key,
            staffName: name,
            billCount: 1,
            totalSales: invoice.totalAmount,
          );
        } else {
          map[key] = StaffSalesRow(
            staffId: existing.staffId,
            staffName: existing.staffName,
            billCount: existing.billCount + 1,
            totalSales: existing.totalSales + invoice.totalAmount,
          );
        }
      }
      final rows = map.values.toList()
        ..sort((a, b) => b.totalSales.compareTo(a.totalSales));
      return AsyncData(rows);
    },
  );
});

class StaffWiseReportPage extends ConsumerWidget {
  const StaffWiseReportPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final rowsAsync = ref.watch(staffWiseSalesProvider);
    final period = ref.watch(reportPeriodProvider);
    final money = NumberFormat.currency(locale: 'en_IN', symbol: '₹');

    return Scaffold(
      appBar: AppBar(
        title: Text('User-wise — ${period.label}'),
        actions: [
          IconButton(
            tooltip: 'Period',
            onPressed: () => showReportPeriodFilterMenu(context, ref),
            icon: const Icon(Icons.filter_list_rounded),
          ),
        ],
      ),
      body: ResponsiveContent(
        child: rowsAsync.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (e, _) => Center(child: Text('$e')),
          data: (rows) {
            if (rows.isEmpty) {
              return const Center(child: Text('No bills in this period'));
            }
            final totalBills = rows.fold<int>(0, (s, r) => s + r.billCount);
            final totalSales = rows.fold<double>(0, (s, r) => s + r.totalSales);
            return ListView(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
              children: [
                AppCard(
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('Total bills'),
                            Text(
                              '$totalBills',
                              style: const TextStyle(
                                fontWeight: FontWeight.w800,
                                fontSize: 20,
                              ),
                            ),
                          ],
                        ),
                      ),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            const Text('Total sales'),
                            Text(
                              money.format(totalSales),
                              style: const TextStyle(
                                fontWeight: FontWeight.w800,
                                fontSize: 20,
                                color: AppColors.primary,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                for (final row in rows)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: AppCard(
                      child: Row(
                        children: [
                          CircleAvatar(
                            backgroundColor: AppColors.primary.withValues(
                              alpha: 0.12,
                            ),
                            child: Text(
                              row.staffName.isNotEmpty
                                  ? row.staffName[0].toUpperCase()
                                  : '?',
                              style: const TextStyle(
                                color: AppColors.primary,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  row.staffName,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                                Text('${row.billCount} bills'),
                              ],
                            ),
                          ),
                          Text(
                            money.format(row.totalSales),
                            style: const TextStyle(
                              fontWeight: FontWeight.w800,
                              color: AppColors.primary,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
              ],
            );
          },
        ),
      ),
    );
  }
}
