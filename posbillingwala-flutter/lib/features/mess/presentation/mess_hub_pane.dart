import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_slip_builder.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_token_qr_page.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_service.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/report_pin_gate.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Android MessInvoiceAdapter.resolveAllowedMessDays — One Time=1, else 2. */
int messAllowedPrintsPerDay(String? messTotalDays) {
  final t = (messTotalDays ?? '').trim().toLowerCase();
  if (t == 'one time' || t == '1') return 1;
  return 2;
}

class MessHubMemberStats {
  const MessHubMemberStats({
    required this.member,
    required this.messAmount,
    required this.paidAmount,
    required this.messTotalDays,
    required this.todayPrints,
    required this.monthTokens,
    required this.hasCurrentMonthPayment,
  });

  final MessMember member;
  final double messAmount;
  final double paidAmount;
  final String messTotalDays;
  final int todayPrints;
  final int monthTokens;
  final bool hasCurrentMonthPayment;

  double get pending => (messAmount - paidAmount).clamp(0, double.infinity);

  bool get hasPending => pending > 0.009;

  int get allowedToday => messAllowedPrintsPerDay(messTotalDays);

  bool get alreadyPrintedToday => todayPrints >= allowedToday;

  bool canPrint({required bool institutePay}) {
    if (alreadyPrintedToday) return false;
    if (institutePay) return true;
    return messAmount > 0.009 && paidAmount > 0.009;
  }
}

/* Load payment + today/month token stats for hub cards (Android InvoiceMess). */
final messHubStatsProvider =
    FutureProvider.autoDispose<List<MessHubMemberStats>>((ref) async {
      final members = await ref.watch(messMembersProvider.future);
      final db = ref.read(appDatabaseProvider);
      final month = DateFormat('yyyy-MM').format(DateTime.now());
      final today = DateTime.now();
      final out = <MessHubMemberStats>[];

      for (final m in members) {
        final id = '${m.memberId}';
        final payment = await db.getMessPaymentForMonth(
          memberId: id,
          yyyyMm: month,
        );
        final messAmt = payment?.paymentMessAmount ?? 0;
        final paidAmt = payment?.paymentPaidAmount ?? 0;
        final days = payment?.messTotalDays ?? 'Two Time';
        final todayCoupons = await db.countMessCouponsForMemberOnDay(
          m.memberName,
          today,
          memberId: id,
        );
        final todayTokens = await db.countMessTokensForMemberOnDay(
          memberId: id,
          day: today,
        );
        /* Android: month = coupons + QR mess_token slips. */
        final monthCoupons = await db.countMessCouponsForMemberMonth(
          memberId: id,
          memberName: m.memberName,
          yyyyMm: month,
        );
        final monthQrTokens = await db.countMessTokensForMemberMonth(
          memberId: id,
          yyyyMm: month,
        );
        var monthTokens = monthCoupons > monthQrTokens
            ? monthCoupons
            : monthQrTokens;
        if (monthCoupons > 0 &&
            monthQrTokens > 0 &&
            monthCoupons != monthQrTokens) {
          monthTokens = monthCoupons + monthQrTokens;
        }
        out.add(
          MessHubMemberStats(
            member: m,
            messAmount: messAmt,
            paidAmount: paidAmt,
            messTotalDays: days,
            todayPrints: todayCoupons + todayTokens,
            monthTokens: monthTokens,
            hasCurrentMonthPayment: messAmt > 0.009 || paidAmt > 0.009,
          ),
        );
      }
      return out;
    });

/* Android InvoiceMess body: search + AutoFitGrid of member invoice cards. */
class MessHubMembersPane extends ConsumerStatefulWidget {
  const MessHubMembersPane({super.key});

  @override
  ConsumerState<MessHubMembersPane> createState() => MessHubMembersPaneState();
}

class MessHubMembersPaneState extends ConsumerState<MessHubMembersPane> {
  final searchCtrl = TextEditingController();

  @override
  void dispose() {
    searchCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final institutePay =
        ref.watch(messInstitutePayProvider).asData?.value ?? false;
    final statsAsync = ref.watch(messHubStatsProvider);
    final query = searchCtrl.text.trim().toLowerCase();

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 4, 12, 0),
          child: AppTextField(
            controller: searchCtrl,
            label: 'Search Mess Member',
            hint: 'Name, mobile, registration…',
            onChanged: (_) => setState(() {}),
          ),
        ),
        const SizedBox(height: 8),
        Expanded(
          child: statsAsync.when(
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (e, _) => Center(child: Text('$e')),
            data: (all) {
              /* User Pay: only members with current-month package; Institute: all. */
              var rows = institutePay
                  ? all
                  : all.where((s) => s.hasCurrentMonthPayment).toList();
              if (query.isNotEmpty) {
                rows = rows.where((s) {
                  final m = s.member;
                  final hay = [
                    m.memberName,
                    m.memberMobileNumber ?? '',
                    m.memberAltenetMobileNumber ?? '',
                    m.registrationNo ?? '',
                  ].join(' ').toLowerCase();
                  return hay.contains(query);
                }).toList();
              }

              if (rows.isEmpty) {
                return Center(
                  child: Text(
                    all.isEmpty
                        ? 'No mess members yet'
                        : 'No mess invoices found for the selected period.',
                    textAlign: TextAlign.center,
                    style: const TextStyle(color: AppColors.textSecondary),
                  ),
                );
              }

              return LayoutBuilder(
                builder: (context, constraints) {
                  final cols = AppBreakpoints.columnsForWidth(
                    constraints.maxWidth,
                    minItemWidth: 200,
                    minColumns: 1,
                    maxColumns: 4,
                    spacing: 8,
                  );
                  if (cols == 1) {
                    /* Phone: height follows content (no fixed grid cell). */
                    return ListView.separated(
                      padding: const EdgeInsets.fromLTRB(12, 0, 12, 16),
                      itemCount: rows.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 8),
                      itemBuilder: (context, index) => MessHubMemberCard(
                        stats: rows[index],
                        institutePay: institutePay,
                        onPrinted: () => ref.invalidate(messHubStatsProvider),
                      ),
                    );
                  }
                  return GridView.builder(
                    padding: const EdgeInsets.fromLTRB(12, 0, 12, 16),
                    itemCount: rows.length,
                    gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: cols,
                      mainAxisSpacing: 8,
                      crossAxisSpacing: 8,
                      mainAxisExtent: 148,
                    ),
                    itemBuilder: (context, index) => MessHubMemberCard(
                      stats: rows[index],
                      institutePay: institutePay,
                      onPrinted: () => ref.invalidate(messHubStatsProvider),
                    ),
                  );
                },
              );
            },
          ),
        ),
      ],
    );
  }
}

/* Android member_invoice_list — Print Coupon + Print QR Token. */
class MessHubMemberCard extends ConsumerWidget {
  const MessHubMemberCard({
    super.key,
    required this.stats,
    required this.institutePay,
    required this.onPrinted,
  });

  final MessHubMemberStats stats;
  final bool institutePay;
  final VoidCallback onPrinted;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final member = stats.member;
    final canPrint = stats.canPrint(institutePay: institutePay);
    final pendingColor = const Color(0xFFF59E0B);

    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(12),
      elevation: 0,
      child: Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              width: 1.6,
              color: stats.hasPending
                  ? pendingColor
                  : AppColors.primary.withValues(alpha: 0.45),
            ),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      member.memberName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 14,
                        color: AppColors.navy,
                      ),
                    ),
                  ),
                  if (stats.hasPending)
                    Icon(
                      Icons.currency_rupee_rounded,
                      size: 18,
                      color: pendingColor,
                    ),
                ],
              ),
              if (member.memberMobileNumber?.trim().isNotEmpty == true) ...[
                const SizedBox(height: 2),
                Text(
                  member.memberMobileNumber!,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 11,
                    color: AppColors.textSecondary,
                  ),
                ),
              ],
              if (stats.hasPending) ...[
                const SizedBox(height: 6),
                Text(
                  'Pending: ₹ ${stats.pending.toStringAsFixed(2)}',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: pendingColor,
                  ),
                ),
              ],
              const SizedBox(height: 2),
              Text(
                'Today: ${stats.todayPrints}/${stats.allowedToday} · '
                'Month tokens: ${stats.monthTokens}',
                style: const TextStyle(
                  fontSize: 11,
                  color: AppColors.textSecondary,
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: _PrintActionChip(
                      label: canPrint ? 'Print Coupon' : 'Printed today',
                      enabled: canPrint,
                      onTap: () => printCoupon(context, ref),
                    ),
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: _PrintActionChip(
                      label: canPrint ? 'Print QR Token' : 'Printed today',
                      enabled: canPrint,
                      onTap: () => printQr(context, ref),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
    );
  }

  Future<void> printCoupon(BuildContext context, WidgetRef ref) async {
    if (!await guardPrint(context, ref, qr: false)) return;
    if (!context.mounted) return;

    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(child: CircularProgressIndicator()),
    );

    var saved = false;
    try {
      if (AppPlatform.requiresNetwork && !await ensureOnline()) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text(kOnlineRequiredMessage)),
          );
        }
        return;
      }

      final db = ref.read(appDatabaseProvider);
      final used = await db.countMessCouponsForMemberOnDay(
        stats.member.memberName,
        DateTime.now(),
      );
      if (used >= stats.allowedToday) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Already coupon created')),
          );
        }
        return;
      }

      final messType = MessSlipBuilder.resolveMessType(
        existingPrintsToday: used,
      );
      final profile = ref.read(shopReceiptProfileProvider);
      final layout = MessSlipBuilder.couponLayout(
        profile: profile,
        memberName: stats.member.memberName,
        messType: messType,
        couponNo: used + 1,
      );
      final printResult = await ref.read(printServiceProvider).printMessSlip(
        layout.toPlainText(
          width: ref.read(printerSettingsProvider).charsPerLine,
        ),
        layout: layout,
        label: 'Mess coupon',
      );
      final printed =
          printResult.outcome != PrintOutcome.failed &&
          printResult.outcome != PrintOutcome.previewOnly;
      if (!printed) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(printResult.message ?? 'Print failed — not saved'),
            ),
          );
        }
        return;
      }

      final id = await db.issueMessCoupon(
        memberId: '${stats.member.memberId}',
        memberName: stats.member.memberName,
        messType: messType.isEmpty ? 'Lunch' : messType,
      );

      final userId = ref.read(authControllerProvider).session?.userId;
      var uploaded = false;
      if (userId != null && userId.isNotEmpty) {
        final row = await db.getMessInvoiceById(id);
        if (row != null) {
          final ok = await MessApi(ref.read(apiClientProvider)).insertMessInvoice(
            userId: userId,
            memberName: row.memberName,
            messType: row.messType,
            messInvoiceDate: DateFormat(
              'yyyy-MM-dd HH:mm:ss',
            ).format(row.messInvoiceDate),
            messInvoiceNetworkStatus: row.messInvoiceNetworkStatus,
            messInvoiceStatus: '0',
          );
          if (ok) {
            await db.markMessInvoiceSynced(id);
            uploaded = true;
          }
        }
      }
      if (AppPlatform.requiresNetwork && !uploaded) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text(kWebApiSaveFailedMessage)),
          );
        }
        return;
      }

      saved = true;
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Coupon printed')),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    } finally {
      if (context.mounted) Navigator.of(context, rootNavigator: true).pop();
    }
    if (saved) onPrinted();
  }

  Future<void> printQr(BuildContext context, WidgetRef ref) async {
    if (!await guardPrint(context, ref, qr: true)) return;
    if (!context.mounted) return;
    if (!messTokenHasRequiredIdentity(
      stats.member.memberName,
      stats.member.memberMobileNumber,
    )) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(AppStrings.of(ref).tokenPrintNameMobileRequired),
        ),
      );
      return;
    }

    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(child: CircularProgressIndicator()),
    );

    var saved = false;
    try {
      final messType = MessTokenQrHelper.resolveMessType(
        existingPrintsToday: stats.todayPrints,
      );
      final prep = ref
          .read(messControllerProvider.notifier)
          .prepareMemberToken(stats.member, messType: messType);
      final profile = ref.read(shopReceiptProfileProvider);
      final mobile = messTokenDigits(stats.member.memberMobileNumber);
      final layout = MessSlipBuilder.qrTokenLayout(
        profile: profile,
        memberName: stats.member.memberName,
        memberMobile: mobile,
        messType: prep.messType,
        tokenCode: prep.tokenCode,
        qrPayload: prep.payload,
      );
      final result = await ref.read(printServiceProvider).printMessSlip(
        layout.toPlainText(
          width: ref.read(printerSettingsProvider).charsPerLine,
        ),
        layout: layout,
        qrPayload: prep.payload,
        channel: PrinterChannelKind.bill,
        label: 'Mess QR token',
      );
      final ok =
          result.outcome != PrintOutcome.failed &&
          result.outcome != PrintOutcome.previewOnly;
      if (!ok) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(result.message ?? 'Print failed')),
          );
        }
        return;
      }

      await ref.read(messControllerProvider.notifier).commitMemberToken(
        member: stats.member,
        tokenCode: prep.tokenCode,
        messType: prep.messType,
      );
      saved = true;
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(result.message ?? 'QR token printed')),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    } finally {
      if (context.mounted) Navigator.of(context, rootNavigator: true).pop();
    }
    if (saved) onPrinted();
  }

  Future<bool> guardPrint(
    BuildContext context,
    WidgetRef ref, {
    required bool qr,
  }) async {
    if (stats.alreadyPrintedToday) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Already coupon created')),
      );
      return false;
    }
    if (!institutePay &&
        !(stats.messAmount > 0.009 && stats.paidAmount > 0.009)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Member has not paid this month')),
      );
      return false;
    }
    final mobile = stats.member.memberMobileNumber?.trim() ?? '';
    if (mobile.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppStrings.of(ref).mobileRequired)),
      );
      return false;
    }
    return showMessMobilePinGate(
      context,
      memberMobile: mobile,
      title: qr ? 'QR Token Password' : 'Bill Print Password',
    );
  }
}

class _PrintActionChip extends StatelessWidget {
  const _PrintActionChip({
    required this.label,
    required this.enabled,
    required this.onTap,
  });

  final String label;
  final bool enabled;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = enabled ? AppColors.primary : AppColors.textSecondary;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: enabled ? onTap : null,
        borderRadius: BorderRadius.circular(8),
        child: Opacity(
          opacity: enabled ? 1 : 0.45,
          child: Container(
            height: 36,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: color.withValues(alpha: 0.45)),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.print_rounded, size: 14, color: color),
                const SizedBox(width: 4),
                Flexible(
                  child: Text(
                    label,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: color,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/* Android messAlertBanner — pending / unpaid-with-tokens. */
class MessPaymentAlertBanner extends ConsumerWidget {
  const MessPaymentAlertBanner({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final institutePay =
        ref.watch(messInstitutePayProvider).asData?.value ?? false;
    if (institutePay) return const SizedBox.shrink();

    final statsAsync = ref.watch(messHubStatsProvider);
    return statsAsync.maybeWhen(
      data: (all) {
        var pendingCount = 0;
        var unpaidWithTokens = 0;
        for (final s in all) {
          if (s.hasPending) pendingCount++;
          final unpaid = s.messAmount <= 0.009 || s.paidAmount <= 0.009;
          if (unpaid && (s.monthTokens > 0 || s.todayPrints > 0)) {
            unpaidWithTokens++;
          }
        }
        if (pendingCount == 0 && unpaidWithTokens == 0) {
          return const SizedBox.shrink();
        }
        final text = pendingCount > 0 && unpaidWithTokens > 0
            ? '$pendingCount members have pending dues · '
                  '$unpaidWithTokens unpaid with tokens'
            : pendingCount > 0
            ? '$pendingCount members have pending mess dues'
            : '$unpaidWithTokens unpaid members already have tokens';
        return Container(
          width: double.infinity,
          margin: const EdgeInsets.fromLTRB(12, 4, 12, 0),
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: const Color(0xFFFFF1E6),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: const Color(0xFFF59E0B).withValues(alpha: 0.5)),
          ),
          child: Row(
            children: [
              const Icon(
                Icons.currency_rupee_rounded,
                size: 20,
                color: Color(0xFF9A3412),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  text,
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: Color(0xFF9A3412),
                  ),
                ),
              ),
            ],
          ),
        );
      },
      orElse: () => const SizedBox.shrink(),
    );
  }
}
