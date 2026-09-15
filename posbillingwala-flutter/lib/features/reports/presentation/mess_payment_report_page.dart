import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_widgets.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

/* WithTable member payment report (all local mess payments). */
class MessPaymentReportPage extends ConsumerStatefulWidget {
  const MessPaymentReportPage({super.key});

  @override
  ConsumerState<MessPaymentReportPage> createState() =>
      MessPaymentReportPageState();
}

class MessPaymentReportPageState extends ConsumerState<MessPaymentReportPage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      if (AppPlatform.requiresNetwork) {
        ref.read(messControllerProvider.notifier).syncMembers();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder(
      future: ref.read(appDatabaseProvider).getLocalMessPayments(),
      builder: (context, snap) {
        final rows = snap.data ?? const [];
        final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
        return Scaffold(
          backgroundColor: reportPageBg,
          appBar: AppBar(
            title: Text(AppStrings.of(ref).memberPaymentReport),
            leading: IconButton(
              icon: const Icon(Icons.arrow_back_rounded),
              onPressed: () => context.pop(),
            ),
          ),
          body: snap.connectionState != ConnectionState.done
              ? const Center(child: CircularProgressIndicator())
              : rows.isEmpty
                  ? Center(child: Text(AppStrings.of(ref).noPaymentsYet))
                  : ListView.separated(
                      padding: EdgeInsets.fromLTRB(
                        AppBreakpoints.pagePaddingFor(context.widthClass),
                        12,
                        AppBreakpoints.pagePaddingFor(context.widthClass),
                        28,
                      ),
                      itemCount: rows.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 8),
                      itemBuilder: (context, i) {
                        final p = rows[i];
                        return ReportSurfaceCard(
                          padding: const EdgeInsets.all(12),
                          child: ListTile(
                            contentPadding: EdgeInsets.zero,
                            title: Text(p.memberName),
                            subtitle: Text(
                              '${p.paymentDate} · ${p.messTotalDays} days',
                            ),
                            trailing: Text(
                              currency.format(p.paymentPaidAmount),
                              style:
                                  const TextStyle(fontWeight: FontWeight.w800),
                            ),
                          ),
                        );
                      },
                    ),
        );
      },
    );
  }
}
