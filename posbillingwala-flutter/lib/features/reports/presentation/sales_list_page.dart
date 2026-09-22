import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_period_controls.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_widgets.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* WithTable `SalesList` — bills plus product lines for the selected period. */
class SalesListPage extends ConsumerWidget {
  const SalesListPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final period = ref.watch(reportPeriodProvider);
    final invoices = ref.watch(filteredPeriodInvoicesProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');

    return Scaffold(
      backgroundColor: reportPageBg,
      appBar: AppBar(
        title: Text(AppStrings.of(ref).salesList),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => context.pop(),
        ),
      ),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: Column(
        children: [
          Padding(
            padding: EdgeInsets.fromLTRB(
              AppBreakpoints.pagePaddingFor(context.widthClass),
              context.isShortHeight ? 6 : 12,
              AppBreakpoints.pagePaddingFor(context.widthClass),
              8,
            ),
            child: Align(
              alignment: Alignment.centerLeft,
              child: ReportPeriodPill(
                label: reportPeriodDisplayLabel(period),
                onTap: () => pickPeriod(context, ref),
              ),
            ),
          ),
          Expanded(
            child: invoices.isEmpty
                ? Center(child: Text(AppStrings.of(ref).noBillsPeriod))
                : ListView.separated(
                    padding: EdgeInsets.fromLTRB(
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      4,
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      28,
                    ),
                    itemCount: invoices.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (context, index) {
                      final invoice = invoices[index];
                      return SalesListCard(
                        invoice: invoice,
                        currency: currency,
                        onOpen: () => context.push(
                          '/reports/invoice/${invoice.invoiceId}',
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
      ),
    );
  }

  Future<void> pickPeriod(BuildContext context, WidgetRef ref) async {
    final selected = await showModalBottomSheet<String>(
      context: context,
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (final e in const [
              ('day', 'Day wise'),
              ('month', 'Month wise'),
              ('year', 'Year wise'),
            ])
              ListTile(
                title: Text(e.$2),
                onTap: () => Navigator.pop(ctx, e.$1),
              ),
          ],
        ),
      ),
    );
    if (selected == null || !context.mounted) return;
    await applyReportPeriodFilterChoice(context, ref, selected);
  }
}

class SalesListCard extends ConsumerWidget {
  const SalesListCard({
    super.key,
    required this.invoice,
    required this.currency,
    required this.onOpen,
  });

  final Invoice invoice;
  final NumberFormat currency;
  final VoidCallback onOpen;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return ReportSurfaceCard(
      padding: const EdgeInsets.fromLTRB(12, 10, 12, 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(
              invoice.invoiceNumber,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w800,
              ),
            ),
            subtitle: Text(
              DateFormat('dd MMM yyyy, hh:mm a').format(invoice.invoiceDate),
            ),
            trailing: Text(
              currency.format(invoice.totalAmount),
              style: const TextStyle(
                fontWeight: FontWeight.w800,
                color: AppColors.primary,
              ),
            ),
            onTap: onOpen,
          ),
          FutureBuilder<List<InvoiceItem>>(
            future: ref
                .read(appDatabaseProvider)
                .getInvoiceItems(invoice.invoiceNumber),
            builder: (context, snap) {
              final items = snap.data ?? const <InvoiceItem>[];
              if (items.isEmpty) return const SizedBox.shrink();
              return Column(
                children: [
                  const Divider(height: 8),
                  for (final item in items)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 3),
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              '${item.productName}${item.portionName == null || item.portionName!.isEmpty ? '' : ' (${item.portionName})'}',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          Text('×${item.productQuantity}'),
                          const SizedBox(width: 10),
                          Text(
                            currency.format(
                              item.productPrice * item.productQuantity,
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              );
            },
          ),
        ],
      ),
    );
  }
}
